## The Core Problem Top K Videos Solves

On the surface tracking trending videos seems simple — count views, show top videos. But consider the real constraints:

```
YouTube scale reality:
────────────────────────────────────────────────
Daily video views:         5 billion+
Views per second:          58,000+ sustained (peak: 200K+)
Total videos:              800 million+
Trending windows needed:   Last hour, 24h, 7d, 30d, all-time
Categories:                20+ (music, gaming, news, sports, etc.)
Regions:                   200+ countries/languages
Update frequency:          Real-time (views reflected <10 seconds)

Requirements:
→ Show "Top 100 trending videos in last 24 hours" in <50ms
→ Update view counts in real-time (58K updates/second)
→ Support multiple time windows simultaneously
→ Support category filters (top gaming videos in last hour)
→ Support regional filters (top videos in Japan)
→ Handle viral spikes (1M views/minute on single video)
→ Never lose view counts (durability)
→ Query historical trends for analytics
```

This combination of **massive write throughput (58K/sec) + sub-50ms read latency + multiple time windows + categorical/regional filtering + real-time updates** is what forces this specific architecture.

---

## Why Redis Sorted Sets for Real-Time Top K?

### The Top K Query Problem

```
NAIVE APPROACH:
════════════════════════════════════════════════════════

"Show me top 100 videos by view count in last 24 hours"

SQL (PostgreSQL/MySQL):
────────────────────────────────────────────────
SELECT video_id, COUNT(*) as views
FROM video_views
WHERE timestamp > NOW() - INTERVAL '24 hours'
GROUP BY video_id
ORDER BY views DESC
LIMIT 100;

At YouTube scale:
- 24 hours × 58,000 views/second = 5 billion rows
- Scan 5 billion rows
- GROUP BY on 5 billion rows (expensive aggregation)
- Sort entire result set
- Query time: 30+ seconds (unacceptable)

Even with perfect indexes:
- Index on timestamp helps with WHERE
- Still must scan 5B rows for COUNT
- Aggregation still expensive
- 10+ seconds minimum

This doesn't work.
```

### Redis Sorted Set Solution

```
REDIS SORTED SET:
════════════════════════════════════════════════════════

Sorted Set properties:
- Members: video_id (unique)
- Score: view_count
- Automatically sorted by score
- O(log N) insert/update
- O(log N + K) range queries

Schema:
────────────────────────────────────────────────
Key:   top_videos:24h
Type:  Sorted Set
Score: view_count
Member: video_id

ZADD top_videos:24h 1500000 "video_123"  (1.5M views)
ZADD top_videos:24h 2300000 "video_456"  (2.3M views)
ZADD top_videos:24h 890000  "video_789"  (890K views)


Query: "Top 100 videos"
────────────────────────────────────────────────
ZREVRANGE top_videos:24h 0 99 WITHSCORES

Returns:
[
  ("video_456", 2300000),
  ("video_123", 1500000),
  ("video_789", 890000),
  ...
]

Query time: <5ms
Even with millions of videos in sorted set


Increment view count:
────────────────────────────────────────────────
ZINCRBY top_videos:24h 1 "video_123"

Atomic increment
Automatically maintains sorted order
Time: <1ms

58,000 increments/second: handled easily by Redis
(Redis handles 100K+ ops/second per instance)
```

### Why Sorted Set Is Perfect for Top K

```
SORTED SET INTERNALS:
════════════════════════════════════════════════════════

Redis implements Sorted Set using two structures:

1. Skip List (for ordered operations):
────────────────────────────────────────────────
Level 3: video_456 ──────────────────────→ video_789
Level 2: video_456 ──────→ video_123 ────→ video_789
Level 1: video_456 → video_123 → video_789 → ...

Sorted by score (view count)
O(log N) traversal
O(log N) insertion/update


2. Hash Table (for O(1) score lookup):
────────────────────────────────────────────────
"video_123" → 1500000
"video_456" → 2300000
"video_789" → 890000

O(1) lookup of specific video's score


Operations:
────────────────────────────────────────────────
ZINCRBY video_123 +1
1. Hash table: lookup current score O(1)
2. Skip list: remove node O(log N)
3. Skip list: insert with new score O(log N)
Total: O(log N)

ZREVRANGE 0 99
1. Skip list: traverse from highest score
2. Return top 100
Total: O(log N + 100) ≈ O(log N)

At 10 million videos:
log2(10,000,000) ≈ 23 operations
Query time: microseconds
```

---

## Why ClickHouse/BigQuery for Analytics?

### The Historical Analytics Problem

```
REQUIREMENTS:
════════════════════════════════════════════════════════

Business needs:
- "Show me view count trend for video_123 over last 6 months"
- "Which videos had the most view growth week-over-week?"
- "What's the average views-per-video by category and month?"
- "Predict next week's trending videos using historical patterns"

Redis cannot answer these:
- Redis Sorted Set only has CURRENT view count
- No historical data (unless you keep separate sets per day)
- No complex aggregations (growth, averages, percentiles)
- Not designed for analytical queries
```

### ClickHouse Columnar Storage

```
CLICKHOUSE SCHEMA:
════════════════════════════════════════════════════════

Video_Views table:
────────────────────────────────────────────────────────────────────────
timestamp           │ video_id  │ user_id  │ category │ region │ device  │ duration_sec
───────────────────────────────────────────────────────────────────────────────────────────
2024-02-26 10:00:00 │ video_123 │ user_001 │ music    │ US     │ mobile  │ 180
2024-02-26 10:00:01 │ video_456 │ user_002 │ gaming   │ UK     │ desktop │ 320
2024-02-26 10:00:02 │ video_123 │ user_003 │ music    │ JP     │ mobile  │ 145

Table engine: MergeTree
Partition by: toYYYYMM(timestamp)  (monthly partitions)
Order by: (video_id, timestamp)
TTL: timestamp + INTERVAL 2 YEAR  (auto-delete after 2 years)


Query: "Video view trend over 6 months"
────────────────────────────────────────────────
SELECT 
  toStartOfDay(timestamp) as date,
  COUNT(*) as views
FROM video_views
WHERE video_id = 'video_123'
AND timestamp >= NOW() - INTERVAL 6 MONTH
GROUP BY date
ORDER BY date;

ClickHouse optimizations:
- Partition pruning (only scans 6 monthly partitions)
- Columnar storage (only reads timestamp, video_id columns)
- Compressed storage (30-50x compression on timestamp/video_id)
- Parallel processing (all CPU cores used)

Query time: <500ms on billions of rows


Query: "Top 100 videos with highest view growth this week"
────────────────────────────────────────────────
WITH this_week AS (
  SELECT video_id, COUNT(*) as views_this_week
  FROM video_views
  WHERE timestamp >= NOW() - INTERVAL 7 DAY
  GROUP BY video_id
),
last_week AS (
  SELECT video_id, COUNT(*) as views_last_week
  FROM video_views
  WHERE timestamp >= NOW() - INTERVAL 14 DAY
  AND timestamp < NOW() - INTERVAL 7 DAY
  GROUP BY video_id
)
SELECT 
  t.video_id,
  t.views_this_week,
  l.views_last_week,
  (t.views_this_week - l.views_last_week) as growth
FROM this_week t
LEFT JOIN last_week l ON t.video_id = l.video_id
ORDER BY growth DESC
LIMIT 100;

Complex analytical query
Runs in 2-5 seconds on billions of rows
Impossible in Redis
```

### Why Not PostgreSQL for Analytics?

```
POSTGRESQL PROBLEMS AT THIS SCALE:
════════════════════════════════════════════════════════

Video_Views table (row-oriented):
────────────────────────────────────────────────
Row 1: [timestamp, video_id, user_id, category, region, device, duration_sec]
Row 2: [timestamp, video_id, user_id, category, region, device, duration_sec]
...
5 billion rows

Query: "Top videos in last 24 hours"
SELECT video_id, COUNT(*) FROM video_views
WHERE timestamp > NOW() - INTERVAL '24 hours'
GROUP BY video_id
ORDER BY COUNT(*) DESC
LIMIT 100;

Problems:
────────────────────────────────────────────────
→ Row storage: Must read entire rows (all 7 columns)
→ Even though query only needs video_id
→ 5B rows × ~100 bytes/row = 500GB scanned
→ B-tree index on timestamp helps with WHERE
→ But GROUP BY still scans billions of rows
→ PostgreSQL aggregation not optimized for this
→ Query time: 30+ seconds minimum
→ Vacuum overhead from constant INSERT load
→ Horizontal scaling difficult (sharding complex)


ClickHouse advantages:
────────────────────────────────────────────────
✓ Columnar storage (only read video_id column)
✓ Compression (50x smaller than row storage)
✓ Vectorized execution (SIMD processing)
✓ Native partitioning (auto-prune old partitions)
✓ Distributed by design (horizontal scaling easy)
✓ Optimized for append-only workloads
✓ Query time: 1-5 seconds (vs 30+ seconds PostgreSQL)
```

---

## The Multiple Time Window Problem

### Maintaining Separate Sorted Sets

```
REDIS SCHEMA FOR MULTIPLE WINDOWS:
════════════════════════════════════════════════════════

Different time windows:
────────────────────────────────────────────────
top_videos:1h   → last 1 hour
top_videos:24h  → last 24 hours
top_videos:7d   → last 7 days
top_videos:30d  → last 30 days
top_videos:all  → all-time

Each maintained separately


Category breakdown:
────────────────────────────────────────────────
top_videos:24h:music
top_videos:24h:gaming
top_videos:24h:news
top_videos:24h:sports
...

Regional breakdown:
────────────────────────────────────────────────
top_videos:24h:US
top_videos:24h:UK
top_videos:24h:JP
...

Combined:
────────────────────────────────────────────────
top_videos:24h:music:US
top_videos:24h:gaming:UK
...

Total keys: 
5 windows × 20 categories × 200 regions = 20,000+ keys

Each key: 100KB - 10MB (depends on video count)
Total Redis memory: ~200GB
Manageable for Redis cluster
```

### Stream Processing for Window Updates

```
HOW TO MAINTAIN MULTIPLE TIME WINDOWS:
════════════════════════════════════════════════════════

Problem: Cannot just ZINCRBY on all windows
→ 5 windows × 20 categories × 200 regions = 20K increments per view
→ 58K views/sec × 20K increments = 1.16 BILLION ops/second
→ Impossible

Solution: Stream processing with Apache Flink
────────────────────────────────────────────────

Video view event:
{
  video_id: 'video_123',
  user_id: 'user_001',
  category: 'music',
  region: 'US',
  timestamp: 1708945200
}
        │
        ▼
Kafka topic: video_views
(raw events buffered)
        │
        ▼
Apache Flink consumer:
- Tumbling windows (1h, 24h, 7d, 30d)
- Keyed by: (video_id, category, region)
- Aggregation: COUNT per window

Flink maintains state:
{
  "video_123:music:US": {
    "1h_window": 5000 views,
    "24h_window": 45000 views,
    "7d_window": 230000 views,
    "30d_window": 890000 views
  }
}
        │
        ▼
Every 10 seconds, Flink outputs aggregates:
        │
        ▼
Batch update to Redis:
ZADD top_videos:1h:music:US 5000 "video_123"
ZADD top_videos:24h:music:US 45000 "video_123"
ZADD top_videos:7d:music:US 230000 "video_123"
ZADD top_videos:30d:music:US 890000 "video_123"

4 Redis commands (vs 20K individual increments)
Batched across all videos
1000 videos × 4 = 4000 commands every 10 seconds
= 400 commands/second (manageable)
```

---

## The Hot Key Problem

### What is the Hot Key Problem?

```
VIRAL VIDEO SCENARIO:
════════════════════════════════════════════════════════

Taylor Swift releases new music video
Receives 1,000,000 views in first minute
= 16,667 views/second
ALL hitting same video_id

Redis cluster (sharded by key):
────────────────────────────────────────────────
Key: top_videos:24h
→ Hash("top_videos:24h") % nodes = Node 3
→ ALL updates go to Node 3

Node 3 workload:
- 16,667 ZINCRBY commands/second (just for this one video)
- Plus normal 58K views/second spread across all videos
- Total on Node 3: 20K+ ops/second

Node 3 overloaded:
- CPU saturated
- Memory bandwidth saturated
- Latency spikes from <1ms to 100ms+
- Other videos on Node 3 affected
- Hot key problem


Why does this happen?
────────────────────────────────────────────────
Redis shards by KEY, not by member
All operations on top_videos:24h go to same node
Even though it contains millions of different video_ids
```

### Solution: Replicate Hot Keys

```
REPLICATION STRATEGY:
════════════════════════════════════════════════════════

Primary sorted set:
────────────────────────────────────────────────
top_videos:24h → Node 3 (original)


Replicas:
────────────────────────────────────────────────
top_videos:24h:replica1 → Node 1
top_videos:24h:replica2 → Node 5
top_videos:24h:replica3 → Node 7

All four contain same data
Updated in parallel


Write path (distribute load):
────────────────────────────────────────────────
View event for video_123
        │
        ▼
Hash video_id to determine replica:
replica_num = hash(video_id) % 4

If replica_num = 2:
  ZINCRBY top_videos:24h:replica2 1 "video_123"

16,667 views/second on video_123:
→ All go to replica2 (Node 5)
→ Node 5 handles this load
→ Node 3, 1, 7 unaffected


Read path (load balance):
────────────────────────────────────────────────
User requests: "Top 100 videos"
        │
        ▼
Round-robin or random selection:
replica_num = random() % 4

Query from top_videos:24h:replica2
ZREVRANGE top_videos:24h:replica2 0 99

All 4 replicas have same rankings
Reads distributed evenly across 4 nodes
No single node bottleneck


Background reconciliation:
────────────────────────────────────────────────
Every 60 seconds:
- Merge all replicas into primary
- ZUNIONSTORE to combine scores
- Trim to top 10,000 videos (discard long tail)
- Redistribute to replicas

Ensures replicas stay synchronized
Prevents memory explosion from long tail
```

---

## Complete Schema Architecture

```
REDIS SCHEMA:
════════════════════════════════════════════════════════

Top videos sorted sets:
────────────────────────────────────────────────
Key:   top_videos:24h
Type:  Sorted Set
Score: view_count (last 24 hours)
Member: video_id

ZADD top_videos:24h 2300000 "video_123"
ZADD top_videos:24h 1800000 "video_456"
...
ZCARD top_videos:24h  → total videos tracked
TTL: None (maintained by stream processor)


Category-specific:
────────────────────────────────────────────────
top_videos:24h:music
top_videos:24h:gaming
top_videos:24h:news
...


Regional:
────────────────────────────────────────────────
top_videos:24h:US
top_videos:24h:UK
top_videos:24h:JP
...


Combined filters:
────────────────────────────────────────────────
top_videos:24h:music:US
top_videos:24h:gaming:JP
...


Hot key replicas:
────────────────────────────────────────────────
top_videos:24h:replica0  → Node 1
top_videos:24h:replica1  → Node 3
top_videos:24h:replica2  → Node 5
top_videos:24h:replica3  → Node 7


Video metadata cache:
────────────────────────────────────────────────
Key:   video:video_123
Type:  Hash
Value: {
  title: "Amazing Video",
  channel: "Cool Creator",
  thumbnail: "https://...",
  duration: 320,
  category: "music"
}
TTL: 1 hour

After fetching top 100 video IDs:
MGET video:id1 video:id2 ... video:id100
→ Batch fetch metadata


CLICKHOUSE SCHEMA:
════════════════════════════════════════════════════════

Video_Views table (raw events):
────────────────────────────────────────────────────────────────────────
timestamp           │ video_id  │ user_id  │ session_id │ category │ region │ device  │ watch_time_sec │ completed
───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
2024-02-26 10:00:00 │ video_123 │ user_001 │ sess_abc   │ music    │ US     │ mobile  │ 180            │ true
2024-02-26 10:00:01 │ video_456 │ user_002 │ sess_def   │ gaming   │ UK     │ desktop │ 320            │ false
2024-02-26 10:00:02 │ video_123 │ user_003 │ sess_ghi   │ music    │ JP     │ mobile  │ 145            │ true

Table engine: MergeTree
Partition by: toYYYYMM(timestamp)
Order by: (video_id, timestamp)
Sampling: user_id
TTL: timestamp + INTERVAL 2 YEAR

Indexes:
  Primary: (video_id, timestamp)
  Skipping index on category
  Skipping index on region


Video_Aggregates_Hourly (materialized view):
────────────────────────────────────────────────────────────────────────
hour                │ video_id  │ category │ region │ views │ total_watch_time │ avg_completion
───────────────────────────────────────────────────────────────────────────────────────────────────
2024-02-26 10:00:00 │ video_123 │ music    │ US     │ 50000 │ 9000000          │ 0.85
2024-02-26 10:00:00 │ video_456 │ gaming   │ UK     │ 30000 │ 9600000          │ 0.92

Created from video_views:
CREATE MATERIALIZED VIEW video_aggregates_hourly
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(hour)
ORDER BY (hour, video_id, category, region)
AS SELECT
  toStartOfHour(timestamp) as hour,
  video_id,
  category,
  region,
  count() as views,
  sum(watch_time_sec) as total_watch_time,
  avg(completed) as avg_completion
FROM video_views
GROUP BY hour, video_id, category, region

Benefits:
→ Pre-aggregated hourly data
→ Queries on hourly buckets 100x faster
→ Storage: 1 row per video per hour (vs millions of raw views)


KAFKA SCHEMA:
════════════════════════════════════════════════════════

Topic: video_views
Partitions: 64 (distributed by video_id)
Replication: 3
Retention: 7 days

Message:
{
  "event_id": "evt_abc123",
  "video_id": "video_123",
  "user_id": "user_001",
  "session_id": "sess_xyz",
  "timestamp": 1708945200123,
  "category": "music",
  "region": "US",
  "device": "mobile",
  "watch_time_sec": 180,
  "completed": true,
  "quality": "1080p",
  "buffering_count": 2
}

Consumers:
1. Flink (real-time aggregation → Redis)
2. ClickHouse consumer (raw event storage)
3. ML recommendation service (user behavior)
4. Analytics service (business metrics)
```

---

## Complete Database Flow

```
FLOW 1: User Watches Video (View Event)
════════════════════════════════════════════════════════

User watches video_123 on YouTube
        │
        ▼
Video player sends view event:
{
  video_id: 'video_123',
  user_id: 'user_001',
  category: 'music',
  region: 'US',
  timestamp: 1708945200,
  watch_time_sec: 180
}
        │
        ▼
STEP 1: Publish to Kafka
────────────────────────────────────────────────
Producer.send(
  topic="video_views",
  key=video_id,  ← ensures same video → same partition
  value=event
)

Kafka buffers event durably
Acknowledges to client
Time: <5ms


STEP 2: Flink consumes and aggregates
────────────────────────────────────────────────
Flink streaming job (runs continuously):

stream
  .keyBy(event => (event.video_id, event.category, event.region))
  .window(TumblingEventTimeWindows.of(Time.hours(24)))
  .aggregate(new ViewCountAggregator())
  .addSink(new RedisSink())

Flink maintains windowed state:
{
  "video_123:music:US": {
    "24h_count": 45001,  ← incremented
    "7d_count": 230001,
    ...
  }
}

Every 10 seconds, Flink emits aggregated counts


STEP 3: Update Redis sorted sets
────────────────────────────────────────────────
Batch update:
ZADD top_videos:24h 45001 "video_123"
ZADD top_videos:24h:music 45001 "video_123"
ZADD top_videos:24h:US 45001 "video_123"
ZADD top_videos:24h:music:US 45001 "video_123"

Determine replica:
replica_num = hash("video_123") % 4 = 2

ZADD top_videos:24h:replica2 45001 "video_123"

Time: <10ms for batch


STEP 4: ClickHouse consumer writes raw event
────────────────────────────────────────────────
ClickHouse Kafka consumer:
Batches events (10,000 events or 5 seconds)

INSERT INTO video_views VALUES
  (event1),
  (event2),
  ...
  (event10000)

Batch insert to ClickHouse
Time: ~100ms for 10K rows

Materialized view automatically updates:
video_aggregates_hourly gets new row


STEP 5: ML service updates recommendations
────────────────────────────────────────────────
ML consumer processes view event:
- Update user profile (watched music video)
- Update video-to-video similarity
- Trigger recommendation refresh

Asynchronous processing
```

```
FLOW 2: User Requests Trending Videos
════════════════════════════════════════════════════════

User opens "Trending" tab in YouTube app
        │
        ▼
GET /api/trending?window=24h&category=music&region=US&limit=50
        │
        ▼
STEP 1: Construct Redis key
────────────────────────────────────────────────
window = "24h"
category = "music"
region = "US"

key = f"top_videos:{window}:{category}:{region}"
→ "top_videos:24h:music:US"


STEP 2: Select replica (load balancing)
────────────────────────────────────────────────
replica_num = random() % 4  ← random load distribution
key_with_replica = f"{key}:replica{replica_num}"
→ "top_videos:24h:music:US:replica2"


STEP 3: Fetch top K from Redis
────────────────────────────────────────────────
ZREVRANGE top_videos:24h:music:US:replica2 0 49 WITHSCORES

Returns:
[
  ("video_789", 2500000),
  ("video_123", 2300000),
  ("video_456", 1800000),
  ...
  (50 total videos with view counts)
]

Query time: <5ms


STEP 4: Fetch video metadata (batch)
────────────────────────────────────────────────
video_ids = [video_789, video_123, video_456, ...]

Check Redis cache:
MGET video:video_789 video:video_123 video:video_456 ...

Cache hits: 45 videos (90%)
Cache misses: 5 videos (10%)

For cache misses, query main database (MySQL/PostgreSQL):
SELECT video_id, title, channel, thumbnail, duration
FROM videos
WHERE video_id IN ('video_xxx', ...)

Backfill Redis cache:
HSET video:video_xxx title "..." channel "..." ...
EXPIRE video:video_xxx 3600

Time: <10ms (mostly cache hits)


STEP 5: Return to client
────────────────────────────────────────────────
Response:
{
  trending: [
    {
      video_id: 'video_789',
      title: 'Amazing Song 2024',
      channel: 'Music Channel',
      thumbnail: 'https://...',
      views: 2500000,
      rank: 1
    },
    {
      video_id: 'video_123',
      title: 'Best Music Video',
      channel: 'Cool Artist',
      thumbnail: 'https://...',
      views: 2300000,
      rank: 2
    },
    ...
  ]
}

Total latency: 5+10 = 15ms
Well under 50ms target ✓
```

```
FLOW 3: Analytics Query (Historical Trends)
════════════════════════════════════════════════════════

Business analyst: "Show me view trends for video_123 over last 6 months"
        │
        ▼
Query ClickHouse:
────────────────────────────────────────────────
SELECT 
  toStartOfDay(hour) as date,
  SUM(views) as daily_views
FROM video_aggregates_hourly
WHERE video_id = 'video_123'
AND hour >= NOW() - INTERVAL 6 MONTH
GROUP BY date
ORDER BY date;

ClickHouse execution:
1. Partition pruning (only scan 6 monthly partitions)
2. Primary key filter (video_id = 'video_123')
3. Column scanning (only hour, views columns)
4. Parallel aggregation (all CPU cores)
5. Compressed data (30x smaller than raw)

Data scanned:
- 6 months × 30 days × 24 hours = 4,320 hourly rows
- vs 6 months × 2M views/day = 360M raw view events
- 83,000x fewer rows to scan

Query time: <200ms

Returns:
date       │ daily_views
──────────────────────────
2024-09-01 │ 1,500,000
2024-09-02 │ 1,450,000
2024-09-03 │ 2,300,000  ← spike (maybe trending)
...
```

```
FLOW 4: Window Expiration (Data Cleanup)
════════════════════════════════════════════════════════

Background job runs every hour:
        │
        ▼
Clean up 24h window:
────────────────────────────────────────────────
Current time: 2024-02-26 10:00:00
24h window cutoff: 2024-02-25 10:00:00

Query ClickHouse for videos to remove:
SELECT DISTINCT video_id
FROM video_views
WHERE timestamp < NOW() - INTERVAL 24 HOUR
AND timestamp >= NOW() - INTERVAL 25 HOUR;

These videos no longer in 24h window
Remove from Redis:
ZREM top_videos:24h video_xxx
ZREM top_videos:24h video_yyy
...

Or simpler: Let Flink handle this
Flink tumbling window naturally excludes old data
Emits new counts without expired events


Clean up ClickHouse (TTL automatic):
────────────────────────────────────────────────
ClickHouse TTL policy:
ALTER TABLE video_views
MODIFY TTL timestamp + INTERVAL 2 YEAR;

Every day at 3 AM:
ClickHouse identifies partitions older than 2 years
Drops entire partitions
No manual cleanup needed
```

```
FLOW 5: Viral Video Handling
════════════════════════════════════════════════════════

Taylor Swift releases music video
1 million views in first minute (16,667 views/second)
        │
        ▼
STEP 1: Kafka handles spike
────────────────────────────────────────────────
16,667 events/second published to Kafka
Kafka topic has 64 partitions
video_id hashed to same partition

Partition 23 receives all Taylor Swift video events
Other partitions unaffected

Kafka buffers events durably
No data loss


STEP 2: Flink processes spike
────────────────────────────────────────────────
Flink consumer on partition 23:
Processes 16,667 events/second (within Flink capacity)

Aggregates in memory:
{
  "video_taylor:music:US": {
    "1h_count": 1000000,  ← grows rapidly
    "24h_count": 1000000,
    ...
  }
}

Every 10 seconds, emits aggregated count:
Instead of 16,667 × 10 = 166,670 Redis commands
Emits 1 batch update with new total


STEP 3: Redis handles concentrated writes
────────────────────────────────────────────────
Replica selection: hash("video_taylor") % 4 = 1

All updates go to replica1
ZADD top_videos:24h:replica1 1000000 "video_taylor"

Replica1 on Node 3 handles this load
Other replicas (on other nodes) unaffected

Single node can handle 100K+ ops/second
16,667/10 seconds = 1,667 ops/second
Well within capacity ✓


STEP 4: Read load distribution
────────────────────────────────────────────────
Millions of users checking trending:
"Is Taylor's video #1 yet?"

Reads distributed across all 4 replicas:
- 25% hit replica0 (Node 1)
- 25% hit replica1 (Node 3)
- 25% hit replica2 (Node 5)
- 25% hit replica3 (Node 7)

No single node overwhelmed
All replicas have same rankings
Users see consistent results
```

---

## Tradeoffs vs Other Databases

```
┌──────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                          │ THIS ARCH    │ POSTGRES ALL │ MONGO ALL    │ ELASTICSEARCH│
├──────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Real-time top K (<50ms)  │ Redis ✓      │ Seconds ✗    │ Seconds ✗    │ 100ms+       │
│ Write throughput         │ 100K+/sec ✓  │ 10K/sec ✗    │ 50K/sec      │ 50K/sec      │
│ Auto-sorted by score     │ Native ✓     │ ORDER BY     │ Sort         │ Sort         │
│ Multiple time windows    │ Multiple keys│ Time query   │ Time query   │ Time query   │
│ Hot key handling         │ Replicas ✓   │ N/A          │ N/A          │ N/A          │
│ Historical analytics     │ ClickHouse ✓ │ PostgreSQL✓  │ Aggregation  │ Aggregation✓ │
│ Complex aggregations     │ ClickHouse ✓ │ PostgreSQL✓  │ Limited      │ Good ✓       │
│ Storage efficiency       │ High ✓       │ Medium       │ Medium       │ High ✓       │
│ Operational complexity   │ HIGH         │ LOW          │ MEDIUM       │ HIGH         │
│ Cost at YouTube scale    │ HIGH         │ Impossible   │ Very HIGH    │ Very HIGH    │
└──────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **Redis Sorted Sets store real-time top K videos because the skip list + hash table hybrid structure gives O(log N) insertions via ZINCRBY for 58K view increments per second and O(log N + K) range queries via ZREVRANGE returning top 100 videos in under 5ms versus PostgreSQL's "GROUP BY video_id ORDER BY COUNT(*) DESC" taking 30+ seconds to scan and aggregate 5 billion rows even with perfect indexes — maintaining separate sorted sets per time window (1h/24h/7d/30d) and per category/region combination creates 20K+ Redis keys but Apache Flink stream processing with tumbling windows aggregates raw Kafka events in memory and batch-updates Redis every 10 seconds avoiding the 1.16 billion Redis operations/second that would result from incrementing all combinations on every view — ClickHouse columnar storage handles historical analytics queries like "video view trends over 6 months" in 200ms by reading only timestamp and video_id columns compressed 30-50x and scanning pre-aggregated hourly buckets (4,320 rows) instead of 360 million raw view events, while monthly partitioning and automatic TTL policies delete data older than 2 years by dropping entire partitions instantly — the hot key problem when viral videos like Taylor Swift get 16,667 views/second is solved by replicating each sorted set across 4 Redis nodes and hash-distributing writes by video_id so one replica handles the viral video's concentrated writes while other replicas stay available, and reads round-robin across all replicas to distribute the "check if it's #1 yet" query load, with background reconciliation every 60 seconds merging replicas and trimming to top 10K videos to prevent memory explosion from long tail.**