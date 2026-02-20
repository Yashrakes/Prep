
## The Core Problem Ad Analytics Solves

On the surface ad click tracking seems simple — log clicks, count them. But consider the real constraints:

```
Google Ads / Facebook Ads reality:
────────────────────────────────────────────────
Ad impressions per day:      100+ billion
Ad clicks per day:           10+ billion  
Click events per second:     115,000+ sustained (peak: 500K+)
Query patterns:
→ "Show me clicks per campaign for last hour" (<1 second response)
→ "Show me cost per click by country for last 30 days" (<5 second response)
→ "Which ad performed best on mobile vs desktop yesterday?"
→ "Hourly trend for campaign X over last 6 months"
→ "Top 100 ads by conversion rate, grouped by device, OS, country"

Requirements:
→ Ingest 115K clicks/second without data loss
→ Query billions of rows in <5 seconds
→ Aggregate across time, campaigns, countries simultaneously
→ Store 3 years of history = trillions of rows
→ Real-time dashboard updates every 10 seconds
→ Cost-efficient storage (not $1M/month in database costs)
```

This combination of **massive write throughput + analytical queries on trillions of rows + real-time aggregation + multi-dimensional grouping + long retention** is what forces this specific architecture.

---

## Why ClickHouse/Druid (Columnar Storage)?

### The Analytical Query Problem

```
TYPICAL AD ANALYTICS QUERY:
════════════════════════════════════════════════════════

"Show total cost and click count per campaign for last 7 days"

SELECT campaign_id,
       COUNT(*) as clicks,
       SUM(cost) as total_cost
FROM ad_clicks
WHERE timestamp >= NOW() - INTERVAL '7 days'
GROUP BY campaign_id
ORDER BY total_cost DESC
LIMIT 100;

This query touches:
7 days × 10 billion clicks/day = 70 billion rows

Must read these columns:
- timestamp (for filtering)
- campaign_id (for grouping)
- cost (for summing)

Does NOT need these columns:
- user_id
- ad_id
- country
- device
- browser
- ip_address
- referrer
- ... (50+ other tracking columns)
```

### Why Row-Oriented Databases (PostgreSQL/MySQL) Fail Here

```
ROW-ORIENTED STORAGE (PostgreSQL):
════════════════════════════════════════════════════════

Data stored as complete rows on disk:

Row 1: [timestamp: 2024-02-26 10:00:00, ad_id: 123, user_id: 456, campaign_id: 789, cost: 0.50, country: US, device: mobile, ...]
Row 2: [timestamp: 2024-02-26 10:00:01, ad_id: 124, user_id: 457, campaign_id: 790, cost: 0.75, country: UK, device: desktop, ...]
Row 3: [timestamp: 2024-02-26 10:00:02, ad_id: 125, user_id: 458, campaign_id: 789, cost: 0.50, country: CA, device: tablet, ...]
...

To run our query, PostgreSQL must:
────────────────────────────────────────────────
1. Read ALL 70 billion rows from disk (complete rows)
2. Each row = ~500 bytes
3. Total data read: 70B × 500 bytes = 35 TERABYTES
4. Even at 1GB/sec disk speed: 35,000 seconds = 9.7 HOURS
5. Filter WHERE timestamp (discard 55+ columns per row)
6. Extract campaign_id and cost
7. Group and aggregate

Problems:
→ Must read entire rows even though only need 3 columns
→ Wasted I/O reading 50+ columns we don't need
→ Query takes HOURS
→ Indexes help but still scan gigabytes
→ VACUUM overhead from constant inserts
→ Write throughput: ~10K inserts/sec max (vs 115K needed)


COLUMN-ORIENTED STORAGE (ClickHouse):
════════════════════════════════════════════════════════

Data stored by COLUMN, not by row:

timestamp column:      [2024-02-26 10:00:00, 2024-02-26 10:00:01, 2024-02-26 10:00:02, ...]
ad_id column:          [123, 124, 125, ...]
user_id column:        [456, 457, 458, ...]
campaign_id column:    [789, 790, 789, ...]
cost column:           [0.50, 0.75, 0.50, ...]
country column:        [US, UK, CA, ...]
...

To run our query, ClickHouse:
────────────────────────────────────────────────
1. Read ONLY 3 columns: timestamp, campaign_id, cost
2. Each column compressed heavily
3. timestamp: delta encoding + LZ4 (50:1 compression)
4. campaign_id: dictionary encoding (integers repeated)
5. cost: double-delta encoding

Total data read: 70B rows × 3 columns × ~5 bytes = 1 TERABYTE
(vs 35 terabytes in PostgreSQL)

At 1GB/sec: 1,000 seconds = 17 minutes
But ClickHouse reads at 10GB/sec with parallel processing
Real time: ~2 seconds

Why 35x less data?
→ Only read columns needed
→ Aggressive compression on columnar data
→ Vectorized execution (SIMD)
```

---

## How Columnar Compression Works (The Magic)

```
EXAMPLE: Storing timestamps
════════════════════════════════════════════════════════

RAW DATA (1 million rows):
────────────────────────────────────────────────
2024-02-26 10:00:00  (8 bytes as Unix timestamp: 1708945200)
2024-02-26 10:00:01  (8 bytes: 1708945201)
2024-02-26 10:00:02  (8 bytes: 1708945202)
2024-02-26 10:00:03  (8 bytes: 1708945203)
...
(1 million rows × 8 bytes = 8MB raw)


ROW STORAGE (PostgreSQL):
────────────────────────────────────────────────
Each row stores full timestamp + all other columns
Cannot compress well (mixed data types)
Compression: ~2:1
Size: ~4MB for timestamps in 1M rows


COLUMNAR STORAGE (ClickHouse):
────────────────────────────────────────────────
All timestamps stored together:
[1708945200, 1708945201, 1708945202, 1708945203, ...]

Delta encoding:
Store first value: 1708945200
Store differences: [+1, +1, +1, +1, +1, ...]

Run-length encoding on deltas:
"1,000,000 times +1"

Compressed size: ~20KB (400:1 compression!)

Same principle for campaign_id:
────────────────────────────────────────────────
Raw: [789, 789, 790, 789, 789, 791, 789, 789, ...]

Dictionary encoding:
Build dictionary: {789: 0, 790: 1, 791: 2}
Store indices: [0, 0, 1, 0, 0, 2, 0, 0, ...]

Run-length: [(0, count: 1000), (1, count: 500), ...]

Compression: 50:1 typical
```

---

## Why ClickHouse Over Other Columnar Databases?

```
┌──────────────────────┬─────────────┬───────────────┬─────────────────┐
│                      │ CLICKHOUSE  │ DRUID         │ POSTGRESQL      │
├──────────────────────┼─────────────┼───────────────┼─────────────────┤
│ Write throughput     │ 1M+ rows/s ✓│ 500K rows/s ✓ │ 10K rows/s ✗    │
│ Compression ratio    │ 30-50x ✓    │ 20-30x ✓      │ 2-3x ✗          │
│ Query latency (70B)  │ 1-5s ✓      │ 2-10s ✓       │ Hours ✗         │
│ SQL compatibility    │ Full SQL ✓  │ SQL-like      │ Full SQL ✓      │
│ Real-time ingestion  │ Yes ✓       │ Yes ✓         │ Yes             │
│ Horizontal scaling   │ Sharding ✓  │ Native ✓      │ Limited         │
│ Materialized views   │ Native ✓    │ Rollups ✓     │ Limited         │
│ Time partitioning    │ Native ✓    │ Native ✓      │ Manual          │
│ Operational cost     │ MEDIUM      │ HIGH          │ LOW             │
│ Learning curve       │ MEDIUM      │ HIGH          │ LOW             │
└──────────────────────┴─────────────┴───────────────┴─────────────────┘
```

### ClickHouse vs Druid: When to Choose What

```
CLICKHOUSE:
════════════════════════════════════════════════════════
Better when:
✓ Need full SQL (JOINs, subqueries, CTEs)
✓ Team already knows SQL
✓ Ad-hoc queries (users write custom reports)
✓ Cost-sensitive (cheaper to operate)
✓ Simpler operational model

Example: Internal analytics team at startup/mid-size company


DRUID:
════════════════════════════════════════════════════════
Better when:
✓ Extreme scale (petabytes, millions of events/sec)
✓ Fixed dashboard queries (pre-defined rollups)
✓ Need sub-second latency on complex aggregations
✓ Real-time alerting on streaming data
✓ Have dedicated ops team for complex system

Example: Netflix user analytics, Airbnb pricing analytics
```

---

## Why Kafka for Stream Processing?

### The Buffering Problem

```
WITHOUT KAFKA:
════════════════════════════════════════════════════════

Ad server generates click event:
{
  "timestamp": 1708945200,
  "ad_id": 123,
  "user_id": 456,
  "campaign_id": 789,
  "cost": 0.50
}
        │
        ▼
Direct INSERT to ClickHouse:
INSERT INTO ad_clicks VALUES (...)

Problems:
────────────────────────────────────────────────
→ Peak traffic: 500K clicks/second
→ ClickHouse optimized for BATCH inserts, not single row
→ Each INSERT has overhead (network, parsing, locking)
→ At 500K individual INSERTs/sec, ClickHouse overwhelmed
→ Write latency spikes from <10ms to seconds
→ Queries slow down (competing with writes)
→ If ClickHouse is down/slow → clicks LOST forever
→ No replay capability


WITH KAFKA:
════════════════════════════════════════════════════════

Ad server → Kafka topic "ad_clicks"
        │
        │ Kafka buffers events durably
        │
        ▼
Consumer batches events:
Collect 10,000 clicks
Every 5 seconds
        │
        ▼
Batch INSERT to ClickHouse:
INSERT INTO ad_clicks VALUES
  (click1),
  (click2),
  ...
  (click10000)

Benefits:
────────────────────────────────────────────────
✓ Kafka handles 500K events/sec easily (millions possible)
✓ ClickHouse receives 2000 batches/sec of 10K rows each
   (vs 500K individual inserts)
✓ Batch inserts 100x more efficient
✓ If ClickHouse slow/down, Kafka buffers
✓ Can replay from Kafka if ClickHouse needs rebuild
✓ Multiple consumers can read same stream
✓ Decouples ad servers from ClickHouse
```

---

## Why Redis for Cache?

### The Dashboard Query Problem

```
DASHBOARD: "Show clicks per campaign for last hour"
════════════════════════════════════════════════════════

WITHOUT REDIS CACHE:
────────────────────────────────────────────────
User refreshes dashboard every 10 seconds
        │
        ▼
Query ClickHouse:
SELECT campaign_id, COUNT(*), SUM(cost)
FROM ad_clicks
WHERE timestamp > NOW() - INTERVAL '1 hour'
GROUP BY campaign_id

Scans: 1 hour × 115K clicks/sec = ~400 million rows
Query time: 2 seconds
        │
        ▼
100 users watching dashboards:
100 × 6 refreshes/minute = 600 queries/minute
600 queries × 2 seconds = 1200 seconds of ClickHouse CPU/min
ClickHouse overwhelmed
Dashboard becomes slow
Real-time queries suffer


WITH REDIS CACHE:
────────────────────────────────────────────────
Stream processor (consuming Kafka):
Every 10 seconds:
1. Read 10 seconds of clicks from Kafka
2. Aggregate in memory:
   {campaign_789: {clicks: 5000, cost: 2500}, ...}
3. Merge into Redis:
   
HINCRBY campaign:789:clicks:hour 5000
HINCRBYFLOAT campaign:789:cost:hour 2500.0

4. Set TTL = 1 hour

Dashboard query:
GET campaign:*:clicks:hour  → Redis
GET campaign:*:cost:hour    → Redis

Response time: <5ms (vs 2000ms)
ClickHouse load: Zero for dashboard queries
        │
        ▼
100 users = 600 queries/min to Redis
Redis handles 100K+ queries/sec easily
No impact on ClickHouse
```

---

## Complete Schema Architecture

```
CLICKHOUSE SCHEMA:
════════════════════════════════════════════════════════

Ad_Clicks table (raw events):
────────────────────────────────────────────────────────────────────────────────
timestamp    │ ad_id │ user_id │ campaign_id │ cost │ country │ device │ os │ browser │ ...
────────────────────────────────────────────────────────────────────────────────────────────
2024-02-26   │ 123   │ 456     │ 789         │ 0.50 │ US      │ mobile │ iOS│ Safari  │ ...
10:00:00
2024-02-26   │ 124   │ 457     │ 790         │ 0.75 │ UK      │ desktop│ Win│ Chrome  │ ...
10:00:01

Table engine: MergeTree
Partition by: toYYYYMM(timestamp)  ← monthly partitions
Order by: (campaign_id, timestamp) ← sort key for fast queries
TTL: timestamp + INTERVAL 3 YEAR   ← auto-delete old data

Indexes:
  Primary: (campaign_id, timestamp)
  Bloom filter: (user_id)          ← for "show user's click history"
  MinMax: (cost)                   ← skip partitions outside cost range


Ad_Clicks_Hourly (materialized view - pre-aggregated):
────────────────────────────────────────────────────────────────────────
hour         │ campaign_id │ ad_id │ device  │ country │ clicks │ total_cost
────────────────────────────────────────────────────────────────────────────────
2024-02-26   │ 789         │ 123   │ mobile  │ US      │ 5000   │ 2500.00
10:00:00
2024-02-26   │ 789         │ 124   │ desktop │ UK      │ 3000   │ 2250.00
10:00:00

Created automatically from Ad_Clicks:
CREATE MATERIALIZED VIEW ad_clicks_hourly
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(hour)
ORDER BY (hour, campaign_id, ad_id, device, country)
AS SELECT
  toStartOfHour(timestamp) as hour,
  campaign_id,
  ad_id,
  device,
  country,
  count() as clicks,
  sum(cost) as total_cost
FROM ad_clicks
GROUP BY hour, campaign_id, ad_id, device, country

Benefits:
→ Queries on hourly data scan 24 rows instead of 400M
→ 16,000,000x fewer rows to scan
→ Query latency: <50ms instead of 2 seconds


KAFKA SCHEMA:
════════════════════════════════════════════════════════

Topic: ad_clicks
Partitions: 32 (distributed by campaign_id hash)
Retention: 7 days (for replay)
Replication: 3 (durability)

Message format (JSON or Avro):
{
  "timestamp": 1708945200123,      ← millisecond precision
  "event_id": "uuid-123-456",      ← deduplication
  "ad_id": 123,
  "user_id": 456,
  "campaign_id": 789,
  "cost": 0.50,
  "country": "US",
  "device": "mobile",
  "os": "iOS",
  "browser": "Safari",
  "ip": "203.0.113.45",
  "referrer": "https://example.com",
  "conversion": false,
  "click_position": 3
}

Partitioning by campaign_id ensures:
→ All clicks for campaign X go to same partition
→ Maintains order per campaign
→ Enables efficient per-campaign processing


REDIS SCHEMA:
════════════════════════════════════════════════════════

Real-time aggregations (TTL: 1 hour):
────────────────────────────────────────────────
HASH campaign:789:metrics:hour
  clicks        "5000"
  cost          "2500.00"
  conversions   "45"
  ctr           "0.009"   ← click-through rate

HASH campaign:789:breakdown:hour:device
  mobile        "3000"
  desktop       "1800"
  tablet        "200"

HASH campaign:789:breakdown:hour:country
  US            "2500"
  UK            "1500"
  CA            "1000"


Recent click IDs (for deduplication):
────────────────────────────────────────────────
SET recent_clicks:campaign_789
    ["uuid-123", "uuid-456", "uuid-789", ...]
TTL: 5 minutes

SADD recent_clicks:campaign_789 "uuid-123"
EXPIRE recent_clicks:campaign_789 300

Prevents duplicate click counting from retries


Top ads by performance (sorted set):
────────────────────────────────────────────────
ZADD top_ads:by_clicks 5000 "ad_123"
ZADD top_ads:by_clicks 4500 "ad_124"
ZADD top_ads:by_clicks 4000 "ad_125"

ZREVRANGE top_ads:by_clicks 0 9  ← top 10 ads
TTL: 1 hour


Campaign budget tracking (prevent overspend):
────────────────────────────────────────────────
HASH campaign:789:budget
  daily_limit    "10000.00"
  spent_today    "7543.21"
  remaining      "2456.79"

Atomic increment on each click:
HINCRBYFLOAT campaign:789:budget spent_today 0.50

If spent >= limit → reject new clicks
```

---

## Complete Database Flow

```
FLOW 1: Ad Click Event Ingestion
════════════════════════════════════════════════════════

User clicks ad on website
        │
        ▼
Ad server generates click event:
{
  "event_id": "uuid-abc-123",
  "timestamp": 1708945200123,
  "campaign_id": 789,
  "ad_id": 123,
  "user_id": 456,
  "cost": 0.50,
  "device": "mobile",
  "country": "US"
}
        │
        ▼
STEP 1: Check Redis deduplication
────────────────────────────────────────────────
SADD recent_clicks:campaign_789 "uuid-abc-123"

Result: 1 (new member added) → proceed
Result: 0 (already exists) → reject duplicate
        │
        ▼ (not duplicate)
STEP 2: Check budget in Redis
────────────────────────────────────────────────
HGET campaign:789:budget remaining
→ Returns: "2456.79"

0.50 <= 2456.79? YES → proceed
        │
        ▼
STEP 3: Publish to Kafka
────────────────────────────────────────────────
Producer.send(
  topic="ad_clicks",
  key=campaign_id,    ← partition by campaign
  value=json_event
)

Kafka acknowledges: Message stored durably
        │
        ▼
STEP 4: Update Redis real-time metrics
────────────────────────────────────────────────
MULTI  ← start Redis transaction

HINCRBY campaign:789:metrics:hour clicks 1
HINCRBYFLOAT campaign:789:metrics:hour cost 0.50
HINCRBYFLOAT campaign:789:budget spent_today 0.50
HINCRBY campaign:789:breakdown:hour:device mobile 1
HINCRBY campaign:789:breakdown:hour:country US 1

EXPIRE campaign:789:metrics:hour 3600
EXPIRE campaign:789:breakdown:hour:device 3600
EXPIRE campaign:789:breakdown:hour:country 3600

EXEC  ← execute atomically

Dashboard sees update immediately (<10ms latency)
        │
        ▼
STEP 5: Kafka consumer batches events
────────────────────────────────────────────────
Consumer accumulates events:
- Read from Kafka continuously
- Batch 10,000 events OR 5 seconds (whichever first)

Batch ready: 10,000 clicks collected
        │
        ▼
STEP 6: Batch insert to ClickHouse
────────────────────────────────────────────────
INSERT INTO ad_clicks FORMAT JSONEachRow
{"timestamp":1708945200123,"ad_id":123,...}
{"timestamp":1708945200124,"ad_id":124,...}
...
(10,000 rows)

ClickHouse:
→ Parses JSON in parallel (all CPU cores)
→ Compresses columns
→ Writes to MergeTree
→ Acknowledges

Time: ~100ms for 10,000 row batch
Throughput: 100,000 rows/second per consumer
With 10 consumers: 1M rows/second


STEP 7: ClickHouse materializes hourly aggregates
────────────────────────────────────────────────
Background process (automatic):

New data inserted → triggers materialized view
ad_clicks_hourly automatically updated:

Aggregates new 10,000 rows into hourly buckets:
hour: 2024-02-26 10:00:00
campaign_id: 789
→ clicks += 10,000
→ total_cost += 5000.00

Happens asynchronously, no query blocking
```

```
FLOW 2: Dashboard Query (Real-Time)
════════════════════════════════════════════════════════

User opens dashboard: "Show campaign performance last hour"
        │
        ▼
STEP 1: Check Redis cache first
────────────────────────────────────────────────
Keys: campaign:*:metrics:hour

HGETALL campaign:789:metrics:hour
→ Returns: {clicks: 5000, cost: 2500.00, conversions: 45}

HGETALL campaign:790:metrics:hour
→ Returns: {clicks: 3000, cost: 2250.00, conversions: 30}

Response time: <5ms
        │
        ▼
Dashboard shows:
Campaign 789: 5000 clicks, $2500 spent, 45 conversions
Campaign 790: 3000 clicks, $2250 spent, 30 conversions

Updates every 10 seconds (Redis real-time data)
```

```
FLOW 3: Analytics Query (Historical)
════════════════════════════════════════════════════════

User runs report: "Show campaign performance by device, last 30 days"
        │
        ▼
STEP 1: Query ClickHouse materialized view
────────────────────────────────────────────────
SELECT 
  campaign_id,
  device,
  SUM(clicks) as total_clicks,
  SUM(total_cost) as total_cost,
  (total_cost / total_clicks) as cpc
FROM ad_clicks_hourly
WHERE hour >= NOW() - INTERVAL 30 DAY
GROUP BY campaign_id, device
ORDER BY total_clicks DESC
LIMIT 100;

Data scanned:
30 days × 24 hours = 720 hourly aggregates per campaign
vs 30 days × 10B clicks/day = 300 billion raw rows

ClickHouse processes:
→ Reads only aggregated data (720 rows vs 300B)
→ Columnar compression
→ Parallel processing across CPU cores
→ SIMD vectorization

Query time: ~500ms
        │
        ▼
Returns:
campaign_id │ device  │ total_clicks │ total_cost │ cpc
────────────────────────────────────────────────────────
789         │ mobile  │ 5,000,000    │ 2,500,000  │ 0.50
789         │ desktop │ 3,000,000    │ 2,250,000  │ 0.75
790         │ mobile  │ 4,500,000    │ 3,375,000  │ 0.75
```

```
FLOW 4: Ad-Hoc Drill-Down Query
════════════════════════════════════════════════════════

User: "Show me all clicks for campaign 789 from iOS users in California yesterday"
        │
        ▼
This query needs RAW data (not pre-aggregated):
────────────────────────────────────────────────
SELECT 
  timestamp,
  ad_id,
  user_id,
  cost
FROM ad_clicks
WHERE campaign_id = 789
AND toDate(timestamp) = yesterday()
AND os = 'iOS'
AND state = 'CA'
ORDER BY timestamp DESC
LIMIT 1000;

Data scanned:
→ Partition pruning: Only yesterday's partition (not all history)
→ Primary key filter: campaign_id = 789 (only that campaign)
→ Column selection: Only timestamp, ad_id, user_id, cost

Yesterday's data: ~10 billion clicks total
Campaign 789: ~100 million clicks
iOS in CA: ~5 million clicks (filtered in ClickHouse)

ClickHouse:
→ Partition pruning: Skips all partitions except Feb-2024
→ Primary key range: Skips all campaigns except 789
→ Bloom filter: Quickly skips blocks without iOS
→ Vectorized filtering: SIMD processes millions of rows/sec

Query time: ~2 seconds
        │
        ▼
Returns 1000 most recent matching clicks
```

```
FLOW 5: Alerting on Anomalies
════════════════════════════════════════════════════════

Alert rule: "Notify if campaign cost exceeds $10,000/hour"
        │
        ▼
Stream processor (Kafka consumer) monitors in real-time:
────────────────────────────────────────────────

Accumulator in memory:
{
  campaign_789: {
    hour_start: 1708945200,
    clicks: 15000,
    cost: 7500.00
  }
}

New click arrives: campaign 789, cost 0.50
cost += 0.50 → 7500.50

New click: cost 0.75
cost += 0.75 → 7501.25

...time passes...

New click: cost 0.50
cost += 0.50 → 10000.75  ← THRESHOLD EXCEEDED!
        │
        ▼
STEP 1: Verify with ClickHouse (avoid false positive)
────────────────────────────────────────────────
SELECT SUM(cost)
FROM ad_clicks
WHERE campaign_id = 789
AND timestamp >= toStartOfHour(NOW())

Returns: 10,001.25  (confirms threshold exceeded)
        │
        ▼
STEP 2: Check Redis alert cooldown
────────────────────────────────────────────────
GET alert:campaign_789:cost_exceeded
→ Returns: NULL (no recent alert)

SET alert:campaign_789:cost_exceeded "1" EX 3600
(prevent re-alerting for 1 hour)
        │
        ▼
STEP 3: Send alert
────────────────────────────────────────────────
POST to Slack webhook:
"🚨 Campaign 789 exceeded $10K/hour limit!
Current spend: $10,001.25
Clicks: 15,000
CPC: $0.67"

POST to PagerDuty API:
Create incident for on-call engineer
        │
        ▼
STEP 4: Auto-pause campaign (optional)
────────────────────────────────────────────────
UPDATE Redis:
HSET campaign:789:budget remaining 0.00

Future clicks rejected (budget check fails)
Campaign effectively paused
```

```
FLOW 6: Data Retention & Cleanup
════════════════════════════════════════════════════════

ClickHouse TTL policy (automatic):
────────────────────────────────────────────────
Every night at 2 AM:

STEP 1: Identify old partitions
────────────────────────────────────────────────
TTL rule: DELETE WHERE timestamp < NOW() - INTERVAL 3 YEAR

Find partitions: 202201, 202202, ... 202112 (3 years old)
        │
        ▼
STEP 2: Delete entire partitions
────────────────────────────────────────────────
ALTER TABLE ad_clicks DROP PARTITION '202101'

→ Deletes entire partition file
→ Instant (no row-by-row deletion)
→ Frees disk space immediately
→ No vacuum/compaction needed


STEP 3: Cleanup Kafka (automatic)
────────────────────────────────────────────────
Kafka retention: 7 days
After 7 days: segments auto-deleted
No manual cleanup needed


STEP 4: Redis TTL expires automatically
────────────────────────────────────────────────
Keys with TTL: auto-expire when TTL reaches 0
Memory freed automatically
No manual cleanup needed
```

---

## Tradeoffs vs Other Databases

```
┌─────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                     │ CLICKHOUSE   │ POSTGRESQL   │ ELASTICSEARCH│ CASSANDRA    │
├─────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Write throughput    │ 1M+ rows/s ✓ │ 10K rows/s ✗ │ 100K rows/s  │ 500K rows/s ✓│
│ Analytical queries  │ Seconds ✓    │ Hours ✗      │ Seconds ✓    │ Slow ✗       │
│ Aggregations        │ Native ✓     │ Native ✓     │ Good ✓       │ Manual ✗     │
│ Compression         │ 30-50x ✓     │ 2-3x ✗       │ 5-10x        │ 3-5x         │
│ SQL support         │ Full SQL ✓   │ Full SQL ✓   │ Limited      │ CQL (basic)  │
│ Join queries        │ Yes ✓        │ Yes ✓        │ No ✗         │ No ✗         │
│ Real-time ingestion │ Kafka ✓      │ Slow         │ Good ✓       │ Native ✓     │
│ Storage cost        │ LOW ✓        │ HIGH ✗       │ MEDIUM       │ MEDIUM       │
│ Operational cost    │ MEDIUM       │ LOW          │ HIGH         │ HIGH         │
│ Time partitioning   │ Native ✓     │ Manual       │ Indices      │ Native ✓     │
│ TTL/retention       │ Native ✓     │ Manual ✗     │ ILM ✓        │ Native ✓     │
└─────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **ClickHouse stores ad click events in columnar format because analytical queries like "show cost per campaign for last 30 days" only need 3 columns (timestamp, campaign_id, cost) out of 50+ tracked fields, and columnar storage reads just those 3 columns compressed 30-50x smaller giving 2-second query times on 70 billion rows versus PostgreSQL's row storage that must read all 50 columns in 35 terabytes taking 9+ hours — Kafka buffers the 115,000 clicks/second stream so ClickHouse receives efficient 10,000-row batches every 5 seconds instead of overwhelming individual inserts, enabling replay if ClickHouse crashes and decoupling ad servers from database availability — Redis caches real-time hourly aggregates updated by stream processors every 10 seconds so dashboards showing "clicks in last hour" query Redis in 5ms instead of scanning 400 million rows in ClickHouse for 2 seconds, and materialized views pre-aggregate hourly rollups reducing 30-day queries from scanning 300 billion raw rows to 720 hourly summaries giving sub-second response on complex multi-dimensional analytics that would timeout in traditional row-oriented databases.**