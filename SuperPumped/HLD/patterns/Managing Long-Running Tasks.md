# Managing Long-Running Tasks Pattern

## Overview
Pattern for handling operations that take too long for synchronous processing by splitting them into immediate acknowledgment and asynchronous background execution.

---

## When to Use This Pattern
- **Video encoding/transcoding** - Can take minutes to hours
- **Report generation** - Complex queries and formatting
- **Bulk operations** - Processing thousands of records
- **Image processing** - Resizing, compression, filters
- **Email sending** - Marketing campaigns, bulk notifications
- **Data exports/imports** - Large CSV/Excel files
- **PDF generation** - Multi-page documents with charts
- **Machine learning inference** - Model predictions on large datasets
- **Any task taking > 2-3 seconds**

---

## Core Architecture

### Basic Flow
```
1. Client → API Server: POST /process-video {videoUrl}
2. API Server validates request
3. API Server → Queue: Push job with job_id
4. API Server → Client: Return {job_id: "abc123"} (instant)
5. Worker pulls job from Queue
6. Worker processes video
7. Worker updates job status → Database
8. Client polls: GET /jobs/abc123/status
9. Client receives: {status: "completed", result: "outputUrl"}
```

### Components

**1. API/Web Server**:
- Validates incoming requests
- Generates unique job_id
- Pushes job to message queue
- Returns job_id immediately (< 100ms)
- Provides status endpoint for polling

**2. Message Queue**:
- Stores pending jobs
- Ensures reliable delivery
- Provides job persistence
- Options: Redis, Kafka, RabbitMQ, AWS SQS

**3. Worker Pool**:
- Continuously polls queue for jobs
- Processes jobs independently
- Updates job status
- Handles retries and failures
- Scales independently from API servers

**4. Job Status Store**:
- Tracks job state (queued → processing → completed/failed)
- Stores progress updates
- Contains results or error messages
- Usually database or Redis

---

## When NOT to Use Async Processing

**CRITICAL**: Don't rush to add queues. This is a common over-engineering mistake.

### Prefer Synchronous Processing When:
- **Task duration < 2-3 seconds** - User can wait
- **User needs immediate result** - Can't proceed without it
- **Simple use case** - Don't add complexity unnecessarily
- **Low traffic** - Don't need independent scaling

### Why Synchronous is Better:
- **Simpler architecture** - Fewer moving parts
- **Clearer backpressure** - Server load limits requests naturally
- **Better UX** - User gets result immediately
- **Easier debugging** - Direct request-response flow
- **Fewer failure modes** - No queue failures, worker crashes, etc.

### Use Async Processing When:
- **Task duration > 3-5 seconds** - User shouldn't wait
- **Heavy CPU/memory** - Don't want to block web server
- **Traffic spikes** - Queue smooths out load
- **Can parallelize** - Multiple workers process simultaneously
- **External dependencies** - API calls with variable latency

---

## Implementation Details

### Job Queue Technologies

**Redis (List-based Queue)**:
```
How it works:
- LPUSH to add job to queue
- BRPOP to fetch job (blocking)
- Simple and fast
```

**When to use**:
- Simple job queues
- Don't need job persistence after processing
- Low volume (< 10K jobs/sec)

**Pros**:
- Very fast (in-memory)
- Simple to implement
- Built-in blocking operations

**Cons**:
- Not durable (can lose jobs on crash)
- Limited visibility into queue
- No built-in retry mechanism

**Include in design**:
- "Use Redis Lists for job queue - simple and fast"
- "Workers use BRPOP for blocking dequeue"
- "Acknowledge jobs by removing from queue"

---

**Kafka**:
```
How it works:
- Produce message to topic
- Consumer groups read from topic
- Messages persisted to disk
- Can replay messages
```

**When to use**:
- Need durability and replay
- High throughput (100K+ jobs/sec)
- Jobs are also events for other systems
- Need audit trail

**Pros**:
- Durable (persists to disk)
- High throughput
- Can replay/reprocess
- Multiple consumers

**Cons**:
- More complex to operate
- Overkill for simple queues
- Higher latency than Redis

**Include in design**:
- "Use Kafka for job queue - need durability and replay capability"
- "Workers join consumer group for load balancing"
- "Can reprocess failed jobs by resetting offset"

---

**RabbitMQ**:
```
How it works:
- Publish message to exchange
- Exchange routes to queues
- Consumers subscribe to queues
- ACK/NACK mechanism
```

**When to use**:
- Need complex routing
- Priority queues
- Want built-in retry/DLQ
- Moderate throughput

**Pros**:
- Flexible routing
- Built-in reliability features
- Good management UI
- Priority queues

**Cons**:
- More complex than Redis
- Lower throughput than Kafka
- Another technology to learn

**Include in design**:
- "Use RabbitMQ for job queue - need priority handling"
- "Set up dead letter exchange for failed jobs"
- "Workers ACK jobs after successful processing"

---

**AWS SQS**:
```
How it works:
- SendMessage to add job
- ReceiveMessage to fetch (long polling)
- DeleteMessage to remove
- Visibility timeout mechanism
```

**When to use**:
- AWS infrastructure
- Want managed service
- Don't want to operate queue

**Pros**:
- Fully managed
- Auto-scaling
- Built-in retry and DLQ
- Pay per use

**Cons**:
- AWS vendor lock-in
- Higher latency than Redis
- Limited visibility into queue internals

**Include in design**:
- "Use SQS for job queue - fully managed, no ops burden"
- "Set visibility timeout to 5 minutes"
- "Configure DLQ for jobs that fail 3 times"

---

### Job Status Tracking

**Schema Design**:
```
jobs table:
- job_id (PK)
- user_id
- job_type (video_encoding, report_gen, etc.)
- status (queued, processing, completed, failed)
- progress (0-100)
- created_at
- started_at
- completed_at
- input_data (JSON)
- result (JSON or URL)
- error_message
```

**State Transitions**:
```
queued → processing → completed
                    → failed
                    
Can also add: paused, cancelled
```

**Status Endpoint**:
```
GET /api/jobs/{job_id}/status

Response:
{
  "job_id": "abc123",
  "status": "processing",
  "progress": 45,
  "created_at": "2024-01-15T10:00:00Z",
  "estimated_completion": "2024-01-15T10:05:00Z"
}
```

**Include in design**:
- "Job status stored in database for persistence"
- "Workers update status and progress as they process"
- "Client polls status endpoint every 2-5 seconds"
- "Return 404 for unknown job_id"

---

### Worker Pool Design

**Worker Structure**:
```
Basic worker loop:
1. Connect to queue
2. While running:
   a. Pull job from queue (blocking)
   b. Update status to 'processing'
   c. Execute job logic
   d. Update status to 'completed' or 'failed'
   e. ACK/remove from queue
3. Handle shutdown gracefully
```

**Scaling Workers**:
- **Horizontal**: Add more worker instances
- **Vertical**: Increase resources per worker
- **Auto-scaling**: Scale based on queue depth

**Include in design**:
- "Worker pool scales independently from API servers"
- "Each worker processes one job at a time"
- "Workers deployed as separate service/containers"
- "Auto-scale workers based on queue length > 100"

**Worker Failure Handling**:
- Worker crashes mid-processing
- Job stays in queue (timeout-based retry)
- Or use visibility timeout (SQS)
- Or use NACK to requeue (RabbitMQ)

---

### Retry Mechanisms

**Retry Strategy**:
```
1. Attempt job processing
2. If fails, check retry count
3. If retries < max (e.g., 3):
   - Increment retry count
   - Wait with exponential backoff
   - Retry: 1s, 2s, 4s, 8s...
4. If retries >= max:
   - Move to Dead Letter Queue
   - Mark job as permanently failed
   - Alert operations team
```

**Exponential Backoff**:
```
retry_delay = base_delay * (2 ^ retry_count)
Examples:
- Retry 1: wait 1s
- Retry 2: wait 2s
- Retry 3: wait 4s
- Retry 4: wait 8s
```

**When to Retry**:
- Transient network failures
- Temporary service unavailability
- Rate limit errors

**When NOT to Retry**:
- Invalid input data (permanent failure)
- Authentication errors
- Resource not found

**Include in design**:
- "Implement retry with exponential backoff"
- "Max 3 retries before moving to DLQ"
- "Different retry policies per job type if needed"
- "Don't retry on validation errors"

---

### Dead Letter Queue (DLQ)

**Purpose**:
- Store jobs that repeatedly fail
- Prevent infinite retry loops
- Allow manual investigation

**When Job Moves to DLQ**:
- After N failed retry attempts (e.g., 3)
- On permanent failure (bad input data)
- When max processing time exceeded

**DLQ Processing**:
- Separate monitoring and alerting
- Manual review of failed jobs
- Fix issues (code bugs, data problems)
- Reprocess manually or in batch

**Include in design**:
- "After 3 failed retries, move job to DLQ"
- "DLQ has separate queue for manual review"
- "Alert ops team when DLQ depth > threshold"
- "Ops can manually reprocess from DLQ after fixing issues"

---

## Complete Design Example: Video Transcoding Service

### Requirements:
- Users upload videos
- System transcodes to multiple formats
- Transcoding takes 2-10 minutes
- Users need to know when done

### Design:

**1. Upload Flow**:
```
Client → POST /api/videos/upload
API Server:
  - Validates video
  - Generates job_id
  - Stores metadata in DB
  - Pushes job to Kafka topic: "video-transcode"
  - Returns: {job_id: "xyz", status_url: "/api/jobs/xyz"}
```

**2. Queue**:
```
Technology: Kafka
Topic: video-transcode
Partitions: 10 (for parallelism)
Retention: 7 days (can replay if needed)
```

**3. Worker Pool**:
```
Workers: 20 instances
Each worker:
  - Subscribes to Kafka consumer group
  - Pulls video from S3
  - Transcodes using FFmpeg
  - Uploads results to S3
  - Updates job status in DB
Auto-scaling: Based on Kafka lag > 1000 messages
```

**4. Job Status**:
```
GET /api/jobs/xyz

Database schema:
- job_id, user_id, status, progress
- input_video_url, output_video_urls
- created_at, completed_at

Polling: Client polls every 5s
Eventually: Switch to WebSocket for push updates
```

**5. Retry Logic**:
```
Max retries: 3
Backoff: 2^n seconds
DLQ: video-transcode-dlq
Alert when DLQ > 10 jobs
```

**6. Monitoring**:
```
Metrics:
- Queue depth (Kafka lag)
- Processing time per job
- Success/failure rate
- DLQ depth
- Worker CPU/memory usage

Alerts:
- Queue depth > 1000
- DLQ depth > 10
- Worker error rate > 5%
```

---

## Common Challenges & Solutions

### Challenge 1: How Does Client Know Job is Done?

**Option A: Polling** (most common)
```
Client polls: GET /jobs/{id}/status every 3-5 seconds
Server returns current status
When status = 'completed', stop polling
```

**Option B: Webhook Callback**
```
Client provides callback URL on job creation
Worker calls webhook when job completes
Client gets push notification
```

**Option C: WebSocket/SSE**
```
Client opens WebSocket connection
Server pushes updates when status changes
More complex but better UX
```

**Recommendation**: Start with polling, upgrade to push if needed

---

### Challenge 2: Job Priority

**Problem**: Some jobs more important than others

**Solution**:
- **Multiple queues**: high-priority, low-priority
- **Priority queue**: RabbitMQ native support
- **Weighted workers**: 70% process high, 30% process low

**Include in design**:
- "Use two queues: priority-jobs and normal-jobs"
- "Workers process priority queue first"
- "Ensures critical jobs processed faster"

---

### Challenge 3: Long-Running Job Progress

**Problem**: 30-minute job, user wants progress updates

**Solution**:
- Worker updates progress field periodically
- Store in Redis for fast access
- Client polls progress every 5-10 seconds

```
Worker pseudocode:
total_steps = 100
for step in 1..100:
  process_step()
  progress = (step / total_steps) * 100
  update_job_progress(job_id, progress)
```

**Include in design**:
- "Workers update progress percentage in Redis"
- "Client polls progress every 5s to show progress bar"

---

### Challenge 4: Idempotency

**Problem**: Job processed twice (retry, duplicate)

**Solution**:
- Use unique job_id as idempotency key
- Check if job already completed before processing
- Use database constraints (unique job_id)

```
Worker logic:
1. Check if job_id already in completed state
2. If yes, skip processing
3. If no, proceed with processing
```

**Include in design**:
- "Jobs are idempotent using job_id as key"
- "Workers check completion status before processing"
- "Duplicate jobs safely ignored"

---

### Challenge 5: Job Timeouts

**Problem**: Worker crashes, job stuck in 'processing'

**Solution**:
- Set visibility timeout (SQS) or heartbeat mechanism
- Job becomes visible again after timeout
- Another worker can pick it up

```
Visibility timeout: 10 minutes
If worker doesn't complete in 10 min, job re-queued
New worker picks it up
```

**Include in design**:
- "Set visibility timeout to 2x expected processing time"
- "If worker crashes, job automatically requeued"
- "Max processing time: 30 minutes, then move to DLQ"

---

## Scaling Considerations

### Scaling Queue
- **Redis**: Vertical scaling, or use Redis Cluster
- **Kafka**: Add partitions, more consumers
- **RabbitMQ**: Add queue replicas
- **SQS**: Auto-scales (managed)

### Scaling Workers
- Monitor queue depth
- Auto-scale: if queue_depth > threshold, add workers
- Different worker types for different job types
- Use spot instances (AWS) for cost savings

### Backpressure Handling
- When queue too full, reject new jobs
- Return 503 Service Unavailable
- Or implement rate limiting on job submission

**Include in design**:
- "Auto-scale workers when queue depth > 100"
- "If queue depth > 10K, return 503 to clients"
- "Prevents queue from growing indefinitely"

---

## Interview Tips

### Always Mention
1. **Synchronous first**: "For short tasks < 3s, keep it synchronous"
2. **Trade-offs**: Discuss complexity vs benefits
3. **Job status tracking**: How client knows when done
4. **Retry and failure handling**: Don't forget DLQ

### Deep Dive Topics
- **Queue choice**: Redis vs Kafka vs RabbitMQ - know trade-offs
- **Retry strategy**: Exponential backoff, max retries
- **Monitoring**: Queue depth, processing time, error rates
- **Scaling**: Workers scale independently from API

### Red Flags to Avoid
- Adding queue without justification
- Not discussing job status tracking
- Forgetting retry and DLQ
- No monitoring/alerting mentioned

### Good Answers Include
- "Let's use async for video transcoding - it takes 5+ minutes"
- "I'd use Kafka for durability and ability to replay failed jobs"
- "Workers update progress in Redis, client polls every 5 seconds"
- "After 3 retries with exponential backoff, move to DLQ"
- "Monitor queue depth and auto-scale workers when it grows"

---

## Example Use Cases

### Video Transcoding
- **Why async**: Takes 5-30 minutes
- **Queue**: Kafka (durable, can replay)
- **Workers**: FFmpeg-based, high CPU
- **Status**: Progress updates every 10%

### Report Generation
- **Why async**: Complex queries, takes 30s - 5min
- **Queue**: RabbitMQ (priority for urgent reports)
- **Workers**: Database-heavy queries
- **Status**: Polling until complete

### Bulk Email Sending
- **Why async**: 10K emails takes time
- **Queue**: SQS (managed, reliable)
- **Workers**: Send via SendGrid/SES API
- **Status**: Count of sent/failed emails

### Image Resizing
- **Synchronous**: If < 1 second
- **Async**: If batch of 100s of images
- **Queue**: Redis (fast, simple)
- **Workers**: ImageMagick/Sharp processing
