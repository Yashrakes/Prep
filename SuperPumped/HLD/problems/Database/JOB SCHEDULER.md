
## The Core Problem a Job Scheduler Solves

On the surface a job scheduler seems simple — run tasks at specified times. But consider the real constraints:

```
Airbnb scale job scheduler:
────────────────────────────────────────────────
Scheduled jobs:              10,000,000+ (send booking confirmations,
                                          payment processing,
                                          data pipelines, etc.)
Jobs per second at peak:     50,000 executions/second
Execution types:             One-time, recurring (cron),
                            delayed, retry with backoff
Requirements:
→ Never execute same job twice (idempotency)
→ Distributed workers pick up jobs atomically (no race)
→ Jobs execute at EXACTLY scheduled time (not 30s late)
→ Failed jobs retry with exponential backoff
→ Query "show me failed jobs from yesterday" in <1 second
→ Keep execution logs for 90 days for debugging
→ Handle worker crashes mid-execution gracefully
→ Support priority queues (critical jobs first)
```

This combination of **precise timing + atomic job claiming + massive write throughput + long-term audit trail** is what forces this three-component architecture.

---

## Why PostgreSQL for Jobs Table?

### What Job Metadata Actually Needs

```
A job definition looks like:
────────────────────────────────────────────────
{
  job_id:          "job_12345",
  job_type:        "send_email",
  schedule_time:   "2024-02-26 15:30:00",  ← exact execution time
  status:          "PENDING",
  priority:        5,                      ← higher = more important
  execution_params: {
    user_id:       "user_001",
    email_template: "booking_confirmation",
    data: {...}
  },
  max_retries:     3,
  retry_backoff:   "exponential",
  timeout_seconds: 300,
  created_at:      "2024-02-26 10:00:00",
  created_by:      "booking_service"
}
```

Job data has specific characteristics that PostgreSQL handles perfectly:

```
JOBS TABLE REQUIREMENTS:
────────────────────────────────────────────────

Complex queries needed:
→ "Find all FAILED jobs from yesterday that haven't been retried"
→ "Show me pending jobs for user_001 grouped by job_type"
→ "Which jobs are stuck in RUNNING state for >1 hour?"
→ "Generate report: job success rate by type this week"

PostgreSQL handles these naturally:
SELECT job_type,
       COUNT(*) as total,
       SUM(CASE WHEN status='SUCCESS' THEN 1 ELSE 0 END) as success,
       (success::float / total * 100) as success_rate
FROM jobs
WHERE created_at > NOW() - INTERVAL '7 days'
GROUP BY job_type
ORDER BY success_rate DESC

→ Natural SQL with aggregations
→ Indexes on status, created_at, job_type
→ Runs in milliseconds even with millions of jobs
```

### Why Partitioning Is Critical

```
Without partitioning:
────────────────────────────────────────────────
Jobs table after 1 year:
50,000 jobs/sec × 86,400 sec/day × 365 days
= 1.5 TRILLION rows

Query: "Show failed jobs from yesterday"
→ Scans entire 1.5T row table
→ Indexes help but still slow
→ Vacuum/autovacuum cannot keep up
→ Query takes 30+ seconds
→ System unusable

With partitioning (by scheduled time):
────────────────────────────────────────────────
CREATE TABLE jobs (
  job_id UUID PRIMARY KEY,
  schedule_time TIMESTAMP NOT NULL,
  status VARCHAR(20),
  ...
) PARTITION BY RANGE (schedule_time);

CREATE TABLE jobs_2024_02_26
  PARTITION OF jobs
  FOR VALUES FROM ('2024-02-26') TO ('2024-02-27');

CREATE TABLE jobs_2024_02_27
  PARTITION OF jobs
  FOR VALUES FROM ('2024-02-27') TO ('2024-02-28');

Query: "Show failed jobs from yesterday"
WHERE schedule_time BETWEEN '2024-02-25' AND '2024-02-26'
→ PostgreSQL uses partition pruning
→ Only scans jobs_2024_02_25 partition
→ ~4M rows instead of 1.5T rows
→ Query completes in <100ms

Old partition cleanup:
DROP TABLE jobs_2023_11_26  ← entire partition deleted
→ Instant cleanup (vs DELETE which scans every row)
→ Retention policy implemented via partition management
```

---

## Why Redis Sorted Sets for the Queue?

### The Fundamental Problem with PostgreSQL as Queue

```
TRYING TO USE POSTGRESQL AS JOB QUEUE:
────────────────────────────────────────────────

Workers poll for jobs every 100ms:
SELECT * FROM jobs
WHERE status = 'PENDING'
AND schedule_time <= NOW()
ORDER BY priority DESC, schedule_time ASC
LIMIT 1
FOR UPDATE SKIP LOCKED;

Problems at 50,000 jobs/second scale:
────────────────────────────────────────────────
→ 100 workers × 10 polls/sec = 1000 SELECT queries/sec
→ Index on (status, schedule_time, priority) needed
→ Index bloat from constant status updates
→ Row locking contention even with SKIP LOCKED
→ Each poll query scans thousands of PENDING rows
→ Vacuum lag from constant INSERT/UPDATE/DELETE
→ Query latency spikes from 5ms to 500ms under load
→ Jobs executed late (missed SLA)
```

### Why Redis Sorted Set Is Perfect

```
Redis Sorted Set for job queue:
────────────────────────────────────────────────

Key: jobs:queue:priority:5
Score: Unix timestamp (schedule_time)
Member: job_id

ZADD jobs:queue:priority:5 1708956600 "job_12345"
     ^score=schedule time   ^member=job_id

Workers fetch jobs:
ZPOPMIN jobs:queue:priority:5 1  ← atomic pop lowest score
→ Returns job scheduled earliest
→ Automatically removed from queue
→ O(log N) operation
→ ~100 microseconds total
→ No locking needed (single-threaded Redis = atomic)
→ No index maintenance
→ No vacuum overhead
```

### How Priority Works with Multiple Sorted Sets

```
Priority 10 (critical): jobs:queue:priority:10
Priority 5  (normal):   jobs:queue:priority:5
Priority 1  (low):      jobs:queue:priority:1

Worker fetches jobs in priority order:
────────────────────────────────────────────────
STEP 1: Try critical queue first
result = ZPOPMIN jobs:queue:priority:10 1
IF result:
  execute job
  return

STEP 2: Try normal queue
result = ZPOPMIN jobs:queue:priority:5 1
IF result:
  execute job
  return

STEP 3: Try low priority queue
result = ZPOPMIN jobs:queue:priority:1 1
IF result:
  execute job
  return

STEP 4: No jobs ready
sleep 100ms
goto STEP 1

Critical jobs always execute first
Lower priority jobs only run when high priority empty
Natural prioritization with zero complex logic
```

---

## Why Redis for Distributed Locks?

### The Double Execution Problem

```
Without locks:
────────────────────────────────────────────────
T+0ms:  Worker A pops job_12345 from queue
T+1ms:  Worker A starts executing job_12345
T+50ms: Worker A crashes mid-execution
T+51ms: Job_12345 still in queue? NO (already popped)
T+52ms: Job never completes, no one retries it
        → LOST JOB

OR worse:

T+0ms:  Worker A pops job_12345
T+1ms:  Worker A executes job (sends email)
T+50ms: Worker A crashes BEFORE marking job complete
T+60ms: Retry mechanism re-queues job_12345
T+61ms: Worker B pops job_12345
T+62ms: Worker B executes job (sends email AGAIN)
        → DUPLICATE EMAIL TO CUSTOMER
```

### Redis Distributed Lock Solution

```
Redis lock prevents duplicate execution:
────────────────────────────────────────────────

Worker A pops job_12345 from queue
        │
        ▼
STEP 1: Acquire lock atomically
SET lock:job:job_12345 "worker_A_uuid" NX EX 300
    ^key               ^value           ^only if not exists
                                        ^expire in 300 seconds

If SET returns 1 → lock acquired ✓
If SET returns 0 → another worker already has it ✗
        │
        ▼ (lock acquired)
STEP 2: Execute job
Send email...
        │
        ▼ (job complete)
STEP 3: Release lock
DEL lock:job:job_12345

Worker A crashes scenario:
────────────────────────────────────────────────
T+0:    Worker A acquires lock (EX 300)
T+1:    Worker A starts job
T+50:   Worker A crashes
T+300:  Lock auto-expires (TTL reached)
T+301:  Worker B can now acquire lock
T+302:  Worker B executes job successfully
        → No lost job
        → No duplicate execution
        → Automatic recovery
```

### The Redlock Algorithm for Multiple Redis Instances

```
Single Redis failure scenario:
────────────────────────────────────────────────
Worker A acquires lock on Redis Instance 1
Redis Instance 1 crashes before replicating
Worker B connects to Redis Instance 2
Redis Instance 2 doesn't know about lock
Worker B acquires "same" lock
→ Both workers execute job
→ DUPLICATE EXECUTION

Redlock solution (5 Redis instances):
────────────────────────────────────────────────
Worker must acquire lock on MAJORITY (3 of 5):

SET lock:job:12345 "worker_A" NX EX 300 on Redis 1 → success
SET lock:job:12345 "worker_A" NX EX 300 on Redis 2 → success
SET lock:job:12345 "worker_A" NX EX 300 on Redis 3 → success

Acquired 3/5 → majority → SAFE to proceed

Even if Redis 1 and 2 crash:
Worker B tries to acquire:
Redis 1: DOWN
Redis 2: DOWN
Redis 3: lock already held by worker_A → fail
Redis 4: success
Redis 5: success

Only 2/5 acquired → NO majority → cannot proceed
→ No duplicate execution possible
→ Fault tolerant
```

---

## Complete Schema Architecture

```
POSTGRESQL SCHEMA:
════════════════════════════════════════════════

Jobs Table (partitioned by schedule_time):
────────────────────────────────────────────────────────────────────────────────
job_id    │ schedule_time       │ status  │ priority │ job_type      │ execution_params
────────────────────────────────────────────────────────────────────────────────────────
job_12345 │ 2024-02-26 15:30:00 │ PENDING │ 5        │ send_email    │ {user:"u1",...}
job_12346 │ 2024-02-26 15:31:00 │ RUNNING │ 10       │ process_payment│ {amount:100}
job_12347 │ 2024-02-26 15:29:00 │ SUCCESS │ 5        │ send_email    │ {user:"u2",...}
job_12348 │ 2024-02-26 15:32:00 │ FAILED  │ 1        │ data_pipeline │ {table:"orders"}

Additional columns:
  max_retries:     INT
  current_retries: INT
  retry_backoff:   VARCHAR  ← 'exponential', 'linear'
  timeout_seconds: INT
  created_at:      TIMESTAMP
  updated_at:      TIMESTAMP
  created_by:      VARCHAR  ← which service created this job

Indexes:
  PRIMARY KEY (job_id, schedule_time)  ← composite for partitioning
  INDEX idx_status_schedule (status, schedule_time, priority)
  INDEX idx_created_at (created_at)
  INDEX idx_job_type (job_type)


Job_Executions Table (audit trail):
────────────────────────────────────────────────────────────────────────────────
execution_id │ job_id    │ worker_id │ start_time          │ end_time            │ status  │ error_msg │ logs
────────────────────────────────────────────────────────────────────────────────────────────────────────────────
exec_001     │ job_12345 │ worker_A  │ 2024-02-26 15:30:00 │ 2024-02-26 15:30:05 │ SUCCESS │ NULL      │ "Email sent"
exec_002     │ job_12348 │ worker_B  │ 2024-02-26 15:32:00 │ 2024-02-26 15:32:10 │ FAILED  │ "Timeout" │ "DB query..."
exec_003     │ job_12348 │ worker_C  │ 2024-02-26 15:37:00 │ 2024-02-26 15:37:08 │ SUCCESS │ NULL      │ "Retry success"

Purpose:
→ Full audit trail of every execution attempt
→ Debugging failed jobs (logs stored here)
→ Performance monitoring (end_time - start_time)
→ Worker health tracking (which workers failing)
→ Compliance/regulatory requirements


REDIS SCHEMA:
════════════════════════════════════════════════

Job Queue (Sorted Sets):
────────────────────────────────────────────────
Key:    jobs:queue:priority:10
Type:   Sorted Set
Score:  Unix timestamp (schedule_time)
Member: job_id

ZADD jobs:queue:priority:10 1708956600 "job_12345"
ZADD jobs:queue:priority:10 1708956660 "job_12346"

Key:    jobs:queue:priority:5
ZADD jobs:queue:priority:5 1708956700 "job_12347"

Key:    jobs:queue:priority:1
ZADD jobs:queue:priority:1 1708956800 "job_12348"


Distributed Locks:
────────────────────────────────────────────────
Key:   lock:job:job_12345
Type:  String
Value: worker_uuid (who holds the lock)
TTL:   300 seconds (auto-expire for crash recovery)

SET lock:job:job_12345 "worker_A_uuid_123" NX EX 300


Job Metadata Cache (avoid PostgreSQL reads):
────────────────────────────────────────────────
Key:   job:meta:job_12345
Type:  Hash
Value: {
  job_type: "send_email",
  priority: 5,
  max_retries: 3,
  current_retries: 0,
  execution_params: "{...json...}"
}
TTL:   3600 seconds (1 hour)

HGETALL job:meta:job_12345


Worker Heartbeat (track alive workers):
────────────────────────────────────────────────
Key:   worker:heartbeat:worker_A
Type:  String
Value: timestamp of last heartbeat
TTL:   30 seconds

SET worker:heartbeat:worker_A "1708956789" EX 30
```

---

## Complete Database Flow

```
FLOW 1: Job Creation (API receives request)
═══════════════════════════════════════════════════════

Client: "Schedule email for user_001 at 3:30 PM"
            │
            ▼
Job Scheduler API:
{
  job_type: "send_email",
  schedule_time: "2024-02-26 15:30:00",
  priority: 5,
  execution_params: {user_id: "user_001", ...}
}
            │
            ├─────────────────────────────────────────┐
            │                                         │
            ▼                                         ▼
    POSTGRESQL (source of truth)              REDIS (queue)
    ─────────────────────────────             ──────────────────────
    INSERT INTO jobs                          ZADD jobs:queue:priority:5
    (job_id, schedule_time,                   1708956600 "job_12345"
     status, priority, job_type,
     execution_params, max_retries,           HSET job:meta:job_12345
     created_at)                                job_type "send_email"
    VALUES                                       priority 5
    ('job_12345',                                max_retries 3
     '2024-02-26 15:30:00',                      execution_params "{...}"
     'PENDING', 5, 'send_email',
     '{...}', 3, NOW())                        TTL: 3600 seconds
    
    Returns job_id to client
    Total: ~2-5ms

Why both databases?
────────────────────────────────────────────────
PostgreSQL = permanent record, complex queries
Redis = fast queue for workers to poll
```

```
FLOW 2: Worker Picks Up Job
═══════════════════════════════════════════════════════

Worker daemon loop (runs every 100ms):
            │
            ▼
STEP 1: Check priority queues (high to low)
────────────────────────────────────────────────
ZPOPMIN jobs:queue:priority:10 1
→ Returns: NULL (no critical jobs)

ZPOPMIN jobs:queue:priority:5 1
→ Returns: (job_12345, 1708956600)
→ Job popped atomically
→ Other workers cannot see this job
            │
            ▼
STEP 2: Check if job time has arrived
────────────────────────────────────────────────
current_time = 1708956599 (one second before scheduled)
job_schedule = 1708956600

IF current_time < job_schedule:
  Sleep until job_schedule
  (ensures exact execution time)

Wait 1 second...
            │
            ▼
STEP 3: Acquire distributed lock
────────────────────────────────────────────────
REDIS:
SET lock:job:job_12345 "worker_A_uuid" NX EX 300

Result: 1 → lock acquired ✓
(If result: 0 → another worker beat us, skip this job)
            │
            ▼
STEP 4: Fetch job metadata
────────────────────────────────────────────────
REDIS cache first:
HGETALL job:meta:job_12345
→ Returns: {job_type:"send_email", params:"{...}"}

If cache miss:
Query PostgreSQL:
SELECT * FROM jobs WHERE job_id = 'job_12345'
Store in Redis for next time
            │
            ▼
STEP 5: Mark job as RUNNING in PostgreSQL
────────────────────────────────────────────────
POSTGRESQL:
UPDATE jobs
SET status = 'RUNNING',
    updated_at = NOW()
WHERE job_id = 'job_12345'

INSERT INTO job_executions
(execution_id, job_id, worker_id, start_time, status)
VALUES ('exec_001', 'job_12345', 'worker_A', NOW(), 'RUNNING')
            │
            ▼
STEP 6: Execute the actual job
────────────────────────────────────────────────
Worker calls job handler:
send_email_handler(params)
→ Sends email via SendGrid API
→ Takes 2 seconds
            │
   ┌────────┴─────────┐
   │                  │
SUCCESS              FAILURE
   │                  │
   ▼                  ▼
```

```
FLOW 3A: Job Succeeds
═══════════════════════════════════════════════════════

Job completes successfully
            │
            ▼
STEP 1: Update PostgreSQL
────────────────────────────────────────────────
UPDATE jobs
SET status = 'SUCCESS',
    updated_at = NOW()
WHERE job_id = 'job_12345'

UPDATE job_executions
SET status = 'SUCCESS',
    end_time = NOW(),
    logs = 'Email sent successfully to user@example.com'
WHERE execution_id = 'exec_001'
            │
            ▼
STEP 2: Release lock
────────────────────────────────────────────────
REDIS:
DEL lock:job:job_12345
            │
            ▼
STEP 3: Clean up metadata cache
────────────────────────────────────────────────
DEL job:meta:job_12345
(will be recreated if job retries)
            │
            ▼
Worker goes back to polling for next job
```

```
FLOW 3B: Job Fails
═══════════════════════════════════════════════════════

Job execution throws exception
            │
            ▼
STEP 1: Record failure in PostgreSQL
────────────────────────────────────────────────
UPDATE jobs
SET status = 'FAILED',
    current_retries = current_retries + 1,
    updated_at = NOW()
WHERE job_id = 'job_12345'

UPDATE job_executions
SET status = 'FAILED',
    end_time = NOW(),
    error_msg = 'SendGrid API timeout',
    logs = 'Full stack trace...'
WHERE execution_id = 'exec_001'
            │
            ▼
STEP 2: Check retry policy
────────────────────────────────────────────────
SELECT max_retries, current_retries, retry_backoff
FROM jobs
WHERE job_id = 'job_12345'

Returns: max_retries=3, current_retries=1, backoff='exponential'

current_retries (1) < max_retries (3)?
YES → retry this job
            │
            ▼
STEP 3: Calculate backoff delay
────────────────────────────────────────────────
Exponential backoff formula:
delay = base_delay * (2 ^ current_retries)
      = 60 seconds * (2 ^ 1)
      = 120 seconds

New schedule_time = NOW() + 120 seconds
            │
            ▼
STEP 4: Re-queue job in Redis
────────────────────────────────────────────────
ZADD jobs:queue:priority:5
     (NOW() + 120) "job_12345"

Job will be picked up again in 2 minutes
            │
            ▼
STEP 5: Release lock
────────────────────────────────────────────────
DEL lock:job:job_12345
            │
            ▼
If current_retries >= max_retries:
→ Don't re-queue
→ Status stays FAILED permanently
→ Alert operations team
```

```
FLOW 4: Worker Crashes Mid-Execution
═══════════════════════════════════════════════════════

Worker A executing job_12345
            │
            ▼
T+0:    Acquired lock (EX 300)
T+1:    Started execution
T+50:   Worker A CRASHES (power failure)
        Lock still exists in Redis
        Job status = RUNNING in PostgreSQL
            │
            ▼
T+60:   Monitoring service detects stale job
────────────────────────────────────────────────
SELECT * FROM jobs
WHERE status = 'RUNNING'
AND updated_at < NOW() - INTERVAL '5 minutes'

Finds job_12345 stuck in RUNNING
            │
            ▼
T+61:   Check if lock still exists
────────────────────────────────────────────────
GET lock:job:job_12345
→ Returns: "worker_A_uuid"

Check worker heartbeat:
GET worker:heartbeat:worker_A
→ Returns: NULL (TTL expired, worker dead)
            │
            ▼
T+62:   Force release lock
────────────────────────────────────────────────
DEL lock:job:job_12345

UPDATE jobs
SET status = 'PENDING'
WHERE job_id = 'job_12345'

Re-queue in Redis:
ZADD jobs:queue:priority:5
     NOW() "job_12345"
            │
            ▼
T+63:   Healthy Worker B picks up job
T+64:   Job executes successfully
        System self-healed
```

```
FLOW 5: Scheduled Recurring Job (Cron-style)
═══════════════════════════════════════════════════════

Cron job definition:
"Run data pipeline every day at 2 AM"
            │
            ▼
Job created with:
schedule_pattern = "0 2 * * *"  ← cron syntax
next_run_time = "2024-02-27 02:00:00"
            │
            ▼
T = 2024-02-27 02:00:00
Job executes (flows 2-3 above)
            │
            ▼
After completion:
────────────────────────────────────────────────
Cron Scheduler Service:
Calculate next_run_time from pattern
"0 2 * * *" → next is 2024-02-28 02:00:00

INSERT INTO jobs (job_id, schedule_time, ...)
VALUES ('job_new_uuid', '2024-02-28 02:00:00', ...)

ZADD jobs:queue:priority:1
     (timestamp of 2024-02-28 02:00:00) "job_new_uuid"

Repeats forever until job disabled
```

---

## Tradeoffs vs Other Databases

```
┌────────────────────────┬──────────────┬────────────────┬──────────────────┐
│                        │ THIS ARCH    │ POSTGRES ONLY  │ RABBITMQ         │
├────────────────────────┼──────────────┼────────────────┼──────────────────┤
│ Queue latency          │ <1ms (Redis) │ 5-50ms (SQL)   │ <5ms             │
│ Atomic job claiming    │ Built-in ✓   │ SKIP LOCKED ✓  │ Built-in ✓       │
│ Priority support       │ Multi-queue✓ │ ORDER BY ✓     │ Priority queue✓  │
│ Exact timing           │ Sleep+poll✓  │ Polling only   │ Delayed queue ✓  │
│ Complex queries        │ PostgreSQL✓  │ PostgreSQL ✓   │ NO ✗             │
│ Audit trail            │ PostgreSQL✓  │ PostgreSQL ✓   │ External DB      │
│ Distributed locking    │ Redis ✓      │ pg_advisory ✓  │ NO               │
│ Crash recovery         │ Lock TTL ✓   │ Manual         │ Requeue ✓        │
│ Operational complexity │ MEDIUM       │ LOW ✓          │ MEDIUM           │
│ At 50K jobs/sec        │ Handles ✓    │ Struggles ✗    │ Handles ✓        │
└────────────────────────┴──────────────┴────────────────┴──────────────────┘
```

---

## One Line Summary

> **PostgreSQL owns job metadata and execution history because partitioning by schedule_time keeps queries on yesterday's failed jobs fast even with trillions of historical jobs and SQL naturally handles complex analytics that no queue can express, Redis Sorted Sets own the job queue because ZPOPMIN gives sub-millisecond atomic job claiming with automatic priority ordering that PostgreSQL polling cannot match at 50K jobs/second, and Redis distributed locks with TTL-based auto-expiry ensure no job ever executes twice even when workers crash mid-execution — together they form a fault-tolerant scheduler where PostgreSQL is truth and Redis is speed.**