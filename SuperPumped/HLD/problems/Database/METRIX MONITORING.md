# Why This Architecture for a Metrics Monitoring System?

Let me build your intuition from the ground up — starting with what makes metrics monitoring brutally hard at scale.

---

## The Core Problem Metrics Monitoring Solves

On the surface metrics monitoring seems simple — collect numbers, store them, show graphs. But consider the real constraints:

```
Netflix infrastructure:
────────────────────────────────────────────────
Servers monitored:          500,000+
Metrics per server:         200+ (cpu, memory, disk, network...)
Collection interval:        every 10 seconds
Total writes per second:    500,000 × 200 / 10
                          = 10,000,000 writes/second

Each write looks like:
{
  host: "server-001",
  region: "us-east-1",
  metric: "cpu_usage",
  value: 67.3,
  timestamp: 1708901234
}

Requirements:
→ Store 10M data points per second
→ Query "show cpu for last 6 hours" in <1 second
→ Alert within 30 seconds of threshold breach
→ Keep raw data for 15 days
→ Keep hourly rollups for 1 year
→ Never lose an alert even if system is degraded
```

This combination of **massive write throughput + time-based queries + alerting + long retention** is what forces this three-database architecture.

---

## Why InfluxDB/Prometheus for Metrics?

### The Fundamental Problem With General Databases

Before understanding why InfluxDB wins, understand why everything else loses:

```
TRYING TO USE POSTGRESQL FOR METRICS:
────────────────────────────────────────────────
Table: metrics
─────────────────────────────────────────────
id (PK) │ host      │ metric    │ value │ timestamp
─────────────────────────────────────────────
1       │ server-001│ cpu_usage │ 67.3  │ 2024-02-26 10:00:00
2       │ server-001│ cpu_usage │ 68.1  │ 2024-02-26 10:00:10
3       │ server-001│ cpu_usage │ 66.9  │ 2024-02-26 10:00:20
...
8,640,000,000 rows after 1 day (100k records enterd per second)

Problems:
→ B-tree index bloat from constant inserts
→ MVCC overhead per row
→ Vacuum process cannot keep up
→ Query "avg cpu last 6 hours" scans billions of rows
→ Storage overhead: 200 bytes per row × 8.6B rows = 1.7TB/day
→ System collapses within hours at this write rate
```

```
TRYING TO USE MONGODB FOR METRICS:
────────────────────────────────────────────────
Document per metric point:
{
  host: "server-001",
  metric: "cpu_usage",
  value: 67.3,
  timestamp: ISODate("2024-02-26T10:00:00Z")
}

Problems:
→ BSON overhead per document (~100 bytes metadata)
→ Index maintenance at 10M writes/second
→ WiredTiger compression helps but not enough
→ Time range queries not natively optimized
→ No built-in downsampling
→ No continuous aggregation
→ Retention policies must be hand-coded
```

### Why InfluxDB Is Purpose-Built for This

InfluxDB was designed with one insight: **time-series data has completely different characteristics from relational data**.

```
Time-series data characteristics:
────────────────────────────────────────────────
1. Always has a timestamp
2. Appended in time order (never random inserts)
3. Rarely updated (immutable once written)
4. Queried by time ranges almost exclusively
5. Recent data accessed more than old data
6. Old data can be compressed aggressively
7. Duplicate timestamps for same metric are rare

InfluxDB exploits ALL of these properties:
```

### InfluxDB Internal Architecture

```
HOW INFLUXDB STORES DATA:
────────────────────────────────────────────────

Time-Structured Merge Tree (TSM):
Similar to LSM Tree but optimized for timestamps

Write Path:
────────────────────────────────────────────────
Incoming metric point
        │
        ▼
WAL (Write Ahead Log)
Sequential append → fast
        │
        ▼
Cache (in-memory)
Groups by: measurement + tag set
        │
        ▼ (when cache full)
TSM File on disk
Sorted by: series key + timestamp
Compressed: timestamps delta-encoded
            values gorilla-compressed

WHY THIS IS BRILLIANT:
────────────────────────────────────────────────
cpu_usage for server-001 over 1 hour:
Raw timestamps: 1708900000, 1708900010, 1708900020...
Delta encoded: 1708900000, +10, +10, +10...
After compression: stores the base + "repeat +10"
90% compression ratio on timestamps alone

cpu values: 67.3, 68.1, 66.9, 67.5, 67.2...
Gorilla compression: XOR between consecutive values
Values close together compress to near zero
80% compression ratio on values

Result: 1TB of raw metrics → 50GB stored
```

### Prometheus vs InfluxDB

```
PROMETHEUS:
────────────────────────────────────────────────
Pull model: Prometheus SCRAPES targets every N seconds
Targets expose /metrics HTTP endpoint
Prometheus fetches from each target

Advantages:
→ Service discovery built in
→ Pull model means targets don't need Prometheus address
→ Excellent Kubernetes integration
→ PromQL is powerful query language
→ Built-in alerting (AlertManager)

Disadvantages:
→ Local storage only (not clustered natively)
→ Long-term storage needs external solution (Thanos/Cortex)
→ Pull model problematic for short-lived jobs
→ Single server write throughput limit


INFLUXDB:
────────────────────────────────────────────────
Push model: Agents PUSH metrics to InfluxDB
Telegraf agent collects and sends data

Advantages:
→ Clustered natively (InfluxDB Enterprise)
→ Better long-term storage
→ SQL-like query language (Flux)
→ Built-in downsampling and retention policies
→ Higher write throughput at scale

Disadvantages:
→ Push model requires agents everywhere
→ More complex to set up
→ Enterprise clustering is paid

WHEN TO USE WHICH:
→ Kubernetes/microservices: Prometheus
→ Infrastructure/IoT at massive scale: InfluxDB
→ Many companies use BOTH
```

---

## Why PostgreSQL for Alerts?

### What Alerts Actually Need

```
An alert rule looks like:
────────────────────────────────────────────────
{
  alert_id:    "alert_001",
  metric_name: "cpu_usage",
  condition:   "greater_than",
  threshold:   90.0,
  duration:    "5 minutes",    ← must breach for 5 min
  severity:    "critical",
  notify:      ["pagerduty", "slack", "#ops-channel"],
  cooldown:    "30 minutes",   ← don't re-alert for 30 min
  created_by:  "user_123",
  enabled:     true
}
```

Alert data has completely different characteristics from metrics:

```
ALERTS vs METRICS comparison:
────────────────────────────────────────────────

METRICS:
→ 10M writes per second
→ Immutable once written
→ Simple structure (timestamp + value)
→ Time range queries
→ Old data can be approximate

ALERTS:
→ Few writes (humans create alerts)
→ Frequently read (checked every 30 seconds)
→ Complex structure (conditions, notifications, history)
→ Relational queries (join alert with its history)
→ MUST be exact (missing an alert = outage)
→ Need transactions (don't fire alert twice)
→ Need audit trail (who changed this alert?)
```

### Why PostgreSQL Is Perfect for Alerts

```
Alert firing requires ACID transactions:
────────────────────────────────────────────────

Alert Evaluation Service checks threshold:
cpu_usage = 95% (above threshold of 90%)

Must atomically:
1. Mark alert as FIRING
2. Record firing timestamp
3. Trigger notification
4. Start cooldown timer

BEGIN TRANSACTION;
  UPDATE alerts
  SET status = 'FIRING',
      last_fired = NOW(),
      fire_count = fire_count + 1
  WHERE alert_id = 'alert_001'
  AND status != 'FIRING';  ← prevent double-fire

  INSERT INTO alert_history
  (alert_id, fired_at, metric_value, resolved_at)
  VALUES ('alert_001', NOW(), 95.0, NULL);

  INSERT INTO notifications_queue
  (alert_id, channels, message)
  VALUES ('alert_001', '["pagerduty","slack"]', 'CPU critical');
COMMIT;

If COMMIT fails → none of the above happened
No duplicate alerts
No ghost notifications
No inconsistent state
```

```
Complex alert queries PostgreSQL handles naturally:
────────────────────────────────────────────────

"Show me all critical alerts that fired
 more than 3 times this week
 on servers in us-east-1
 that haven't been acknowledged"

SELECT a.alert_id,
       a.metric_name,
       a.threshold,
       COUNT(ah.history_id) as fire_count,
       MAX(ah.fired_at) as last_fired
FROM alerts a
JOIN alert_history ah ON a.alert_id = ah.alert_id
JOIN servers s ON a.host = s.host
WHERE a.severity = 'critical'
AND ah.fired_at > NOW() - INTERVAL '7 days'
AND s.region = 'us-east-1'
AND a.acknowledged = false
GROUP BY a.alert_id
HAVING COUNT(ah.history_id) > 3
ORDER BY last_fired DESC

→ Natural SQL
→ Indexes on alert_id, fired_at, region
→ Runs in milliseconds
→ Impossible to express this cleanly in InfluxDB
```

---

## Why Redis for Aggregations?

### The Dashboard Query Problem

```
User opens monitoring dashboard:
────────────────────────────────────────────────
Requests:
"Show average CPU for all 500,000 servers
 grouped by region
 for the last 6 hours
 refreshed every 30 seconds"

Without Redis:
────────────────────────────────────────────────
Every 30 seconds:
Query InfluxDB:
SELECT mean(value)
FROM cpu_usage
WHERE time > NOW() - 6h
GROUP BY region, time(5m)

At 10M writes/second × 6 hours:
= 216 billion data points scanned
Even with compression: billions of points
Even with indexes: takes 10-30 seconds
Dashboard feels broken
100 engineers watching dashboards = 100 concurrent queries
InfluxDB melts

With Redis:
────────────────────────────────────────────────
Pre-computed aggregations stored in Redis:
GET dashboard:cpu:by_region:6h
→ Returns pre-built result in <1ms
→ 100 engineers = same Redis key hit 100 times
→ Redis serves all from memory
→ InfluxDB barely touched for dashboard queries
```

### What Redis Stores for Aggregations

```
Redis key structures:
────────────────────────────────────────────────

Current metric values (for alert evaluation):
HSET current:metrics:server-001
     cpu_usage    67.3
     memory_usage 45.2
     disk_io      234.5
TTL: 60 seconds (auto-expire if server stops reporting)


Pre-aggregated dashboard data:
SET agg:cpu:us-east-1:5m
    [{time:10:00, avg:67.3, max:89.1, min:45.2},
     {time:10:05, avg:68.1, max:91.3, min:44.8}...]
TTL: 5 minutes (refresh when expired)


Alert state cache (prevent re-evaluation):
SET alert:state:alert_001  "FIRING"
TTL: 30 seconds (re-evaluate after 30s)


Rate limiting for alert notifications:
SET alert:cooldown:alert_001  "1"
TTL: 1800 seconds (30 minute cooldown)
SETNX used for atomic check-and-set
```

---

## Complete Schema Architecture

```
INFLUXDB SCHEMA:
════════════════════════════════════════════════

Measurement: cpu_usage
────────────────────────────────────────────────
Tags (indexed, string):
  host:    "server-001"    ← server identifier
  region:  "us-east-1"    ← AWS region
  env:     "production"   ← environment
  service: "api-gateway"  ← what runs on server

Fields (not indexed, numeric):
  value:   67.3           ← actual CPU percentage
  user:    45.2           ← user space CPU
  system:  22.1           ← kernel CPU

Timestamp: 2024-02-26T10:00:00Z (nanosecond precision)

WHY tags vs fields matters:
────────────────────────────────────────────────
Tags are indexed → filter/group by tags fast
Fields are not indexed → only store values

GOOD: WHERE host = 'server-001' (tag filter, fast)
BAD:  WHERE value > 90 (field filter, slow scan)
Use tags for dimensions you filter/group by
Use fields for the actual measurements


Measurement: memory_usage
────────────────────────────────────────────────
Tags: host, region, env
Fields: used_bytes, free_bytes, percent_used


Measurement: http_requests
────────────────────────────────────────────────
Tags: host, region, endpoint, status_code
Fields: count, latency_ms, error_rate


POSTGRESQL SCHEMA:
════════════════════════════════════════════════

Alerts Table:
──────────────────────────────────────────────────────────────────────
alert_id │ metric_name │ condition │ threshold │ duration │ severity
──────────────────────────────────────────────────────────────────────
alert_001│ cpu_usage   │ gt        │ 90.0      │ 5m       │ critical
alert_002│ memory_usage│ gt        │ 85.0      │ 2m       │ warning
alert_003│ error_rate  │ gt        │ 0.05      │ 1m       │ critical

Additional columns:
  tags_filter:   '{"region": "us-east-1"}'  ← which servers
  notify:        '["pagerduty", "slack"]'
  cooldown_mins: 30
  enabled:       true
  created_by:    "user_123"
  created_at:    timestamp
  acknowledged:  false


Alert_History Table:
──────────────────────────────────────────────────────────────────
history_id │ alert_id  │ fired_at   │ resolved_at │ peak_value
──────────────────────────────────────────────────────────────────
hist_001   │ alert_001 │ 2024-02-26 │ 2024-02-26  │ 95.3
hist_002   │ alert_001 │ 2024-02-25 │ 2024-02-25  │ 97.1
hist_003   │ alert_002 │ 2024-02-26 │ null         │ 88.2


Notification_Log Table:
──────────────────────────────────────────────────────────────────
notif_id │ alert_id │ channel    │ sent_at    │ status
──────────────────────────────────────────────────────────────────
notif_001│ alert_001│ pagerduty  │ 2024-02-26 │ delivered
notif_002│ alert_001│ slack      │ 2024-02-26 │ delivered
```

---

## Data Retention: The Rollup Strategy

```
RAW DATA LIFECYCLE:
════════════════════════════════════════════════

Age 0-15 days: Raw data (10-second intervals)
────────────────────────────────────────────────
Every data point stored exactly as received
Full resolution for recent debugging
"What happened at 10:00:45 AM yesterday?" → answerable
Storage: ~50GB per day after compression


Age 15 days - 3 months: 5-minute rollups
────────────────────────────────────────────────
Continuous query runs every 5 minutes:

SELECT mean(value) as mean_value,
       max(value)  as max_value,
       min(value)  as min_value,
       count(value) as sample_count
INTO cpu_usage_5m
FROM cpu_usage
WHERE time > NOW() - 5m
GROUP BY host, region, time(5m)

Raw data deleted after 15 days
5-minute averages kept
"What was average CPU last month?" → answerable
Storage: 50GB × 3 = 150GB (vs 750GB raw)
96% size reduction from 10s to 5m granularity


Age 3 months - 1 year: 1-hour rollups
────────────────────────────────────────────────
Further aggregate 5m data into 1h:

SELECT mean(mean_value) as hourly_avg,
       max(max_value)   as hourly_max,
       min(min_value)   as hourly_min
INTO cpu_usage_1h
FROM cpu_usage_5m
GROUP BY host, region, time(1h)

5-minute data deleted after 3 months
"CPU trend over last 6 months?" → answerable
Storage: tiny (8760 hours × 500K servers × few bytes)


Age 1 year+: 1-day rollups or delete
────────────────────────────────────────────────
Daily summaries for compliance/capacity planning
Or delete entirely based on business requirements
```

---

## Complete Database Flow

```
FLOW 1: Metric Collection and Storage
═══════════════════════════════════════════════════════

Server generates metric every 10 seconds:
cpu_usage = 67.3%
            │
            ▼
Telegraf agent on server:
Batches metrics for 10 seconds
Sends batch to InfluxDB:
[
  {measurement:cpu_usage, tags:{host:server-001,region:us-east-1}, fields:{value:67.3}, time:T},
  {measurement:memory_usage, tags:{host:server-001}, fields:{value:45.2, free:54.8}, time:T},
  {measurement:disk_io, tags:{host:server-001}, fields:{read_bytes:234, write_bytes:156}, time:T}
]
            │
            ├──────────────────────────────────────────┐
            │                                          │
            ▼                                          ▼
     INFLUXDB                                      REDIS
     Write to WAL                            HSET current:metrics:server-001
     Write to Cache                               cpu_usage    67.3
     Acknowledge write                            memory_usage 45.2
     ~1ms total                               TTL: 60 seconds
                                              Used by alert evaluator
```

```
FLOW 2: Alert Evaluation (runs every 30 seconds)
═══════════════════════════════════════════════════════

Alert Evaluator Service wakes up:
            │
            ▼
STEP 1: Fetch all enabled alerts from PostgreSQL
SELECT * FROM alerts WHERE enabled = true
Returns: [alert_001: cpu > 90%, alert_002: memory > 85%...]
            │
            ▼
STEP 2: For each alert, check Redis first
GET alert:state:alert_001
→ "FIRING" → skip (already firing, in cooldown)
→ NULL → need to evaluate
            │
            ▼
STEP 3: Query InfluxDB for metric value
SELECT mean(value)
FROM cpu_usage
WHERE host =~ /us-east-1/    ← from alert tags_filter
AND time > NOW() - 5m        ← duration window

Returns: mean_cpu = 95.3%
            │
            ▼
STEP 4: Evaluate condition
95.3 > 90.0 (threshold) → BREACH DETECTED
            │
            ▼
STEP 5: PostgreSQL transaction (atomic)
BEGIN TRANSACTION;
  UPDATE alerts SET status='FIRING', last_fired=NOW()
  WHERE alert_id='alert_001' AND status!='FIRING';

  INSERT INTO alert_history
  (alert_id, fired_at, peak_value)
  VALUES ('alert_001', NOW(), 95.3);

  INSERT INTO notification_queue
  (alert_id, channels, message)
  VALUES ('alert_001', '["pagerduty","slack"]',
          'CPU critical: 95.3% on us-east-1');
COMMIT;
            │
            ▼
STEP 6: Update Redis state
SET alert:state:alert_001 "FIRING" EX 30
SET alert:cooldown:alert_001 "1" EX 1800
            │
            ▼
STEP 7: Send notifications
PagerDuty API call → incident created
Slack API call → message in #ops-channel
```

```
FLOW 3: Dashboard Query
═══════════════════════════════════════════════════════

Engineer opens dashboard:
"Show CPU by region, last 6 hours, 5min intervals"
            │
            ▼
Dashboard Service:
            │
            ▼
STEP 1: Check Redis cache
GET dashboard:cpu:by_region:6h:5m
            │
    HIT ────┘──────────────────────→ Return cached data
            │                        to dashboard instantly
    MISS    │                        (<1ms)
            ▼
STEP 2: Query InfluxDB
SELECT mean(value)
FROM cpu_usage
WHERE time > NOW() - 6h
GROUP BY region, time(5m)

InfluxDB reads from TSM files
Returns aggregated data (~500ms)
            │
            ▼
STEP 3: Store in Redis
SET dashboard:cpu:by_region:6h:5m [result]
EX 30  ← cache for 30 seconds
            │
            ▼
STEP 4: Return to dashboard
100 engineers all see same data
Redis serves 99 of them from cache
InfluxDB only queried once per 30 seconds
```

```
FLOW 4: Automated Rollup (data retention)
═══════════════════════════════════════════════════════

Every 5 minutes, Rollup Service runs:
            │
            ▼
STEP 1: Aggregate raw → 5min in InfluxDB
SELECT mean(value), max(value), min(value)
INTO cpu_usage_5m
FROM cpu_usage
WHERE time > NOW() - 10m AND time < NOW() - 5m
GROUP BY host, region, time(5m)
            │
            ▼
STEP 2: Check if raw data needs deletion
If oldest raw data > 15 days:
DELETE FROM cpu_usage
WHERE time < NOW() - 15d
            │
            ▼
Every hour, aggregate 5min → 1hour:
SELECT mean(mean_value), max(max_value)
INTO cpu_usage_1h
FROM cpu_usage_5m
WHERE time > NOW() - 2h AND time < NOW() - 1h
GROUP BY host, region, time(1h)
            │
            ▼
Every day, check 5min retention:
If 5min data > 3 months → delete
Keep 1hour data for 1 year
```

---

## Tradeoffs vs Other Databases

```
┌────────────────────┬──────────────────┬────────────────┬──────────────────┐
│                    │ INFLUXDB         │ POSTGRESQL     │ ELASTICSEARCH    │
├────────────────────┼──────────────────┼────────────────┼──────────────────┤
│ Write throughput   │ 10M+/sec ✓       │ ~50K/sec ✗     │ ~500K/sec        │
│ Time range queries │ Optimized ✓      │ Slow at scale  │ Good             │
│ Compression        │ 90%+ ✓           │ ~50%           │ ~70%             │
│ Downsampling       │ Built-in ✓       │ Manual         │ Manual           │
│ Retention policies │ Built-in ✓       │ Manual         │ ILM policies     │
│ SQL support        │ Limited          │ Full ✓         │ No               │
│ Alert queries      │ Poor             │ Excellent ✓    │ Possible         │
│ Operational cost   │ Medium           │ Low ✓          │ High             │
│ Learning curve     │ Medium           │ Low ✓          │ High             │
└────────────────────┴──────────────────┴────────────────┴──────────────────┘
```

---

## One Line Summary

> **InfluxDB owns raw metric storage because its TSM tree compresses time-series data by 90% and handles 10M writes/second that would destroy PostgreSQL's B-tree indexes, PostgreSQL owns alerts because firing an alert requires ACID transactions to prevent duplicate notifications and complex relational queries that InfluxDB cannot express, Redis owns aggregations because pre-computing dashboard views means 100 engineers watching dashboards hit Redis once instead of triggering 100 concurrent billion-row InfluxDB scans every 30 seconds, and the rollup pipeline from 10-second to 5-minute to 1-hour granularity means you keep full resolution for recent debugging while reducing storage by 99% for historical data.**
