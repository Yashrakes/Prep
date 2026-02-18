



### "Why Redis Sorted Sets specifically? Why not just a SQL table with an ORDER BY?"

This is a classic warm-up. The real answer has two layers.

The surface answer is performance: a Redis Sorted Set (`ZREVRANGE`, `ZINCRBY`) operates in **O(log N)** time, and since the entire structure lives in memory, you're talking about microsecond-level operations. A SQL `ORDER BY views DESC LIMIT 10` requires either a full table scan or a maintained index — and under write-heavy load (millions of `UPDATE` calls per second), index maintenance becomes a bottleneck that SQL databases aren't designed for.

The deeper answer is about **atomic increment semantics**. When a view event arrives, you call `ZINCRBY top_videos:24h 1 vid:abc123`. This is a single atomic operation — it increments the score and re-positions the member in the sorted set simultaneously. In SQL, you'd need a `SELECT → compute → UPDATE` cycle, which creates race conditions under concurrent writes unless you add locking, which destroys your throughput. This is the answer that separates SDE2 candidates from junior ones.

A good interviewer might follow up: _"What does Redis lose compared to SQL?"_ — and you should confidently say: **durability guarantees, complex joins, and storage capacity**. Redis is RAM-bound, so you can't store your entire historical view dataset there. That's precisely why ClickHouse exists in this design.

---

### "Walk me through what happens when a video goes viral — say it gets 10 million views in one hour."

This is testing whether you understand the **hot key problem**, which is one of the most important distributed systems concepts at this level.

Here's the scenario: every one of those 10 million view events calls `ZINCRBY top_videos:24h 1 vid:viral`. In a standard Redis cluster, every key lives on exactly one node. So you've just routed 10 million writes to a single Redis node, which becomes saturated — it can handle roughly 100k ops/sec, so you're already 100x over capacity.

The solution you should describe is **write sharding combined with read merging**. Instead of writing to one key, you scatter writes across multiple keys based on the video ID's hash:

```
shard_id = hash(video_id) % 10
ZINCRBY top_videos:24h:shard_{shard_id} delta vid:viral
```

This spreads the write load across 10 nodes. But now you have a read problem — the true score for a video is the _sum_ across all shards. So on the read path, you use Redis's `ZUNIONSTORE` command to merge all shard keys into a single temporary result set (with `aggregate=SUM`), which you cache with a short TTL (say, 60 seconds). The key insight to articulate is that **reads are much less frequent than writes** in this system — thousands of reads per second versus millions of writes — so the merge cost on reads is acceptable.

---

### "How do you handle the 24-hour vs 7-day vs all-time windows? Can you use one data structure for all three?"

This tests whether you understand **time-series data modeling** and the cost of on-the-fly aggregation.

A naive answer is: just store all views and do `WHERE event_time > NOW() - INTERVAL 24 HOUR` at query time. This sounds clean, but at billions of rows, that's a full or partial table scan happening on every trending page load. Completely unacceptable.

The right mental model is **pre-aggregated buckets**. Think of it like a clock: you store a view count for each video for each hour. The 24-hour score is the sum of the last 24 hourly buckets. The 7-day score is the sum of the last 168 hourly buckets, or more efficiently, the last 7 daily buckets. You're trading a small amount of extra storage for dramatically cheaper reads.

In Redis, this looks like keys of the form `view_bucket:{video_id}:{YYYY-MM-DD-HH}`, each just an integer counter. In ClickHouse, you implement this as **Materialized Views** — `video_views_hourly` and `video_views_daily` — which are updated incrementally as raw events arrive, meaning the aggregation happens once at write time and reads are instant.

The architectural principle here is: **compute at write time, not read time**, when reads are latency-sensitive and writes can tolerate slightly more work. This is a trade-off worth stating explicitly in an interview.

---

### "How do you prevent a video from gaming the trending algorithm with fake views?"

This question seems product-oriented, but it's really testing your **schema design intuition** — specifically whether you model the right data in the first place.

The key is that your schema should capture _not just view count_ but the signals that distinguish real engagement from bot traffic. Your raw events table stores `user_id`, `session_id`, `view_duration`, `is_unique`, and `platform`. This lets you compute a **weighted score** rather than a raw view count:

```
score = raw_views × engagement_ratio × unique_viewer_ratio × time_decay
```

The engagement ratio (watch time / video duration) is low for bots that immediately close the video. The unique viewer ratio (distinct user IDs / total views) is low when the same few accounts send thousands of requests. The time decay penalizes sudden artificial spikes.

The schema design implication is that you need to store enough granular data to compute these signals — which is why the raw `video_views` table in ClickHouse keeps `view_duration`, `user_id`, and `is_unique` even though they consume more storage. An interviewer will appreciate that you can connect the business requirement (fraud prevention) back to specific schema field choices.

---

### "Why ClickHouse (or BigQuery) and not just Postgres?"

This is asking you to demonstrate awareness of **OLAP vs OLTP** systems at a level beyond just knowing the acronyms.

Postgres is a row-oriented store — when you query `SELECT sum(views) FROM events WHERE video_id = 'x'`, it reads the entire row (including all columns you don't need like `user_agent`, `country_code`, etc.) for every matching record. At 100 billion rows, this is extraordinarily wasteful.

ClickHouse is **columnar**: it stores all `views` values contiguously on disk, so an aggregation query reads only the columns it needs, and those columns compress extremely well because similar values are adjacent (e.g., all view counts are small integers). The result is 10-100x faster aggregation on large datasets. You can typically query 1 billion rows in under a second in ClickHouse on reasonably modern hardware.

The trade-off to mention: ClickHouse doesn't support arbitrary updates or deletes efficiently (it's designed for append-only workloads), and it has less mature support for complex transactions. That's fine for an event log, but it means you use `ReplacingMergeTree` with a version column for your metadata table (where you _do_ need upserts), and you don't store anything there that needs frequent point-updates.

---

### "What happens if your Flink job falls behind — say there's a 10-minute processing lag? How does your schema handle out-of-order events?"

This is a senior-leaning question, and answering it well will genuinely impress your interviewer.

Flink uses **event-time processing with watermarks**. The event time is the timestamp embedded in the Kafka message (when the view _actually happened_), not the time Flink processes it. Watermarks tell Flink "I'm confident I've seen all events up to time T," which allows it to close windows and emit results correctly even when events arrive late.

Your schema supports this because the `video_views` table in ClickHouse stores `event_time` from the event itself, not an ingestion timestamp. This means even if events arrive 10 minutes late, they're inserted into the correct time slot. The Materialized Views recompute correctly because ClickHouse's `SummingMergeTree` engine accumulates values whenever new data arrives for a given `(video_id, hour)` partition.

The Redis sorted sets, however, are the "lossy" part of the design. Once you've written a score to Redis, you can't easily retroactively correct it for late-arriving events. This is an acceptable trade-off because Redis is the _display_ layer — slight inaccuracies in real-time ranking are tolerable. The source of truth for accurate counts is always ClickHouse, and a periodic reconciliation job (say, every 15 minutes) can sync Redis scores from ClickHouse if precision matters.

---

## The One Trade-off Table You Should Have In Your Head

Understanding when to reach for each layer is what truly signals SDE2-level thinking:

|Question|Redis Sorted Set|ClickHouse|
|---|---|---|
|"What are the top 10 right now?"|✅ Perfect fit|Too slow for p99 latency|
|"How did this video trend over 30 days?"|❌ Not designed for it|✅ Perfect fit|
|"Total views in Germany yesterday?"|❌ No country dimension|✅ Trivial GROUP BY|
|"Increment a view count atomically?"|✅ O(log N), sub-ms|❌ OLAP, not for point writes|
|"Historical audit of top-10 at 3pm last Friday?"|❌ TTL expired|✅ `trending_snapshots` table|

---

## The Answer Pattern That Wins Every Time

Whenever you answer a design question, use this three-part structure that senior engineers naturally think in: **state the requirement → identify the constraint → justify the choice**. For example: _"The requirement is sub-10ms reads for the trending list. The constraint is that we have millions of writes per second. This rules out SQL because index maintenance under write pressure degrades read latency. Redis Sorted Sets are the right fit because they're in-memory, writes and reads are both O(log N), and ZINCRBY gives us atomic score updates without any locking overhead."_

That pattern — requirement, constraint, justification — shows the interviewer that you don't just know _what_ to use, you know _why_, and you understand what you're giving up. That is the core of SDE2-level system design thinking.

why is ther ebillions of record suppose there is a video x whata re we storing in redis and how are they updating the exising record and getting new record

01:04

Great question — this is actually the thing most people get confused about when they first see this design. Let me untangle it completely.

## First, Let's Separate Two Very Different Things

There are **two completely separate databases** being discussed, and they store fundamentally different things. The confusion comes from mixing them up.

**Redis** stores almost nothing — just a tiny scoreboard. **ClickHouse** is where the billions of records actually live. Let's go through each one carefully.

---

## What Redis Actually Stores for Video X

Imagine a Redis Sorted Set as a **leaderboard in a video game**. It has exactly two pieces of information per entry: a name (the player/video) and a score. That's it. There is no history, no timestamps, no individual events. Just a name and a number.

For your entire platform — say 10 million videos — the Redis key `top_videos:24h` looks like this internally:

```
top_videos:24h (Sorted Set)
─────────────────────────────────────────
vid:abc123   →  score: 4,812,930
vid:xyz789   →  score: 3,201,445
vid:qwe456   →  score: 891,002
... (maybe top 10,000 videos, nothing more)
```

That's it. For Video X, Redis stores **exactly one number** — its current view count score. Not 4 billion rows. Just one floating point number. The entire Redis sorted set for 10,000 videos fits comfortably in a few megabytes of RAM.

---

## So Where Do the Billions of Records Come From?

They come from **ClickHouse**, and they represent raw events — one row per view. Think about what happens when someone watches a video on YouTube:

Every single time **any** user clicks play on **any** video, one row gets appended to the `video_views` table in ClickHouse. So if your platform has 500 million users watching an average of 10 videos a day, that's 5 billion rows _per day_ being added. That's where the billions come from — it's not about one video, it's about every view event across your entire platform over time.

For Video X specifically, ClickHouse might have something like this:

sql

```sql
-- These are individual rows in video_views for just Video X
video_id      | event_time          | user_id    | view_duration | country
---------------------------------------------------------------------------
vid:9xQ3kp2Z  | 2024-01-15 14:00:01 | user:00001 | 312           | IN
vid:9xQ3kp2Z  | 2024-01-15 14:00:03 | user:00002 | 89            | US
vid:9xQ3kp2Z  | 2024-01-15 14:00:04 | user:00003 | 601           | BR
... (potentially millions of rows just for this one video)
```

Each row is a separate human watching Video X at a specific moment. This is your audit trail, your analytics source of truth, your fraud detection data. You never query this directly for trending — it's too slow. But it's the raw material everything else is built from.

---

## Now, How Does Redis Get Updated?

This is the heart of your question, and the answer involves understanding **where the counting actually happens**.

Redis doesn't receive individual view events. If it did, you'd have millions of `ZINCRBY` calls per second hitting one key, which would destroy performance. Instead, the flow works in stages — think of it like a funnel.

**Stage 1 — The event fires.** A user watches Video X. The app server publishes one small event to Kafka:

json

```json
{ "video_id": "vid:9xQ3kp2Z", "user_id": "user:99999", 
  "event_time": 1720000000, "view_duration": 245 }
```

Kafka is just a durable queue — it holds these events until someone processes them. It doesn't update Redis or ClickHouse itself.

**Stage 2 — Flink batches and aggregates.** Flink reads from Kafka continuously and uses a **time window** (say, every 5 seconds) to count how many views each video received in that tiny interval. Instead of saying "Video X got a view" 50,000 times, it says "Video X got 50,000 views in the last 5 seconds." One number. This is the aggregation step.

python

```python
# Flink computes this every 5 seconds across all videos
aggregated = {
    "vid:9xQ3kp2Z": 50_000,   # Video X got 50k views in 5 sec
    "vid:abc123":   12_300,   
    "vid:xyz789":   8_100,
}
```

**Stage 3 — Redis gets one batched update.** Flink then calls `ZINCRBY` once per video per flush cycle — not once per user view. So instead of 50,000 Redis calls, you make 1:

bash

```bash
# This single command adds 50,000 to Video X's current score
ZINCRBY top_videos:24h 50000 vid:9xQ3kp2Z
```

Redis takes the existing score (say, 4,762,930) and simply adds 50,000 to it, making it 4,812,930. It then automatically re-sorts Video X's position in the leaderboard. This is the "updating the existing record" part — there's no insert, no delete, no lookup. Just an atomic add-to-existing-number.

---

## How Do "New" Records Get Added to Redis?

When a brand new video is uploaded and gets its very first view, `ZINCRBY` handles this gracefully — if the member doesn't exist yet in the sorted set, Redis creates it with a score of 0 and then adds your delta. So `ZINCRBY top_videos:24h 1 vid:brand_new_video` on a key that doesn't exist yet will create it with a score of 1. There's no separate "insert" step. `ZINCRBY` is both the insert and the update operation depending on whether the member exists.

---

## The Full Picture — One Mental Model

Think of it this way: ClickHouse is like a **security camera recording every single thing that happens** — every view, every click, every second of watch time, forever. It's comprehensive but slow to search through.

Redis is like the **scoreboard on the wall of the venue** — it only shows the current standings and gets updated every few seconds. It has no memory of how it got to those numbers. It's fast to read but stores almost nothing.

Flink is the **person watching the security footage** and periodically walking over to update the scoreboard. It's the bridge between the raw history and the live display.

The billions of records exist because ClickHouse is the camera — it never throws anything away. Redis is just the scoreboard — it only holds one number per video, right now. These two systems serve completely different purposes, and that's exactly why you need both.



---

## You've Actually Caught a Contradiction

If Flink is batching 50,000 individual view events into one number every 5 seconds, and then writing that one number to Redis — then yes, for _that single flush_, you're making maybe a few thousand `ZINCRBY` calls (one per video that had views in that window). Not 10 million. So where does the "10 million writes" concern even come from?

The answer is that **10 million writes isn't describing one flush from one Flink job**. It's describing the total write pressure on Redis when you zoom out and think about what's actually happening at platform scale. Let's build this up carefully.

---

## What "Writes Per Second to Redis" Actually Means at Scale

Imagine your platform has 10 million _active_ videos at any given moment — meaning videos that are receiving views right now. Flink is running a flush every 5 seconds. So every 5 seconds, Flink has a batch of aggregated counts, one per active video, and it writes each one to Redis as a `ZINCRBY`.

That means every 5 seconds you're making up to 10 million `ZINCRBY` calls to Redis. That works out to **2 million Redis writes per second** sustained, just from this one Flink job. And if you have separate jobs for the 24h window, the 7d window, and the all-time window, you're multiplying that further. _This_ is the 10 million writes problem — it's not about individual user events bypassing Flink, it's about the sheer volume of aggregated updates across all videos across all windows happening simultaneously.

---

## But Wait — Does Every Video Really Need to Be Written Every 5 Seconds?

No, and this is where good schema design thinking kicks in. Most of those 10 million videos aren't trending. Maybe only 100,000 videos are receiving significant view traffic at any given moment. The other 9.9 million have zero or near-zero views in that 5-second window. So a well-designed Flink job only emits a `ZINCRBY` for videos that actually _had_ views in that window — zero-count videos produce no write. This brings the realistic write pressure way down.

So the honest answer is: the "billions of writes" concern is somewhat **worst-case thinking**, and it's meant to make you design defensively rather than describe what happens on a quiet Tuesday afternoon. The hot key problem, though, is very real even with batching — because a viral video might accumulate 500,000 views in a 5-second window, meaning that one video's key in Redis is getting hammered with extremely high-value increments from multiple Flink parallel tasks simultaneously. Even if it's one write per task, if you have 100 parallel Flink workers all flushing to the same Redis key at the same time, you get contention.

---

## The Deeper Point: Batching Reduces Write _Count_, Not Write _Contention_


---

## The Document Made a Logical Error

The document said: "10 million view events each call `ZINCRBY`... routing 10 million writes to a single Redis node." But you've correctly identified the contradiction — if we're batching in Flink, then individual view events _never_ touch Redis directly. So that scenario is simply impossible in this architecture. The hot key justification as written was describing a system _without_ Flink, which is not what we're building.

So let's ask the real question honestly: **after batching, does the hot key problem still exist at all?**

---

## After Batching, What Does the Write Pattern Actually Look Like?

Let's trace through a concrete example with a viral video. Say the video gets 2 million views in one hour. Flink is flushing every 5 seconds, and let's say you have 20 parallel Flink worker tasks all processing different partitions of your Kafka topic.

Within one 5-second window, those 2 million views per hour works out to roughly 2,778 views every 5 seconds. Those events are spread across 20 Kafka partitions, so each Flink worker sees about 139 events for this video in its window. When the flush happens, each of those 20 workers independently computes its local count and then calls `ZINCRBY top_videos:24h 139 vid:viral`. So in that 5-second window, Redis receives **20 writes** to the same key — not 2,778, and certainly not 2 million.

20 writes per 5 seconds to one key is **completely trivial** for Redis. There is no hot key problem here. You were right to be skeptical.

---

## So Does the Hot Key Problem Ever Actually Appear?

Yes, but only under specific conditions that are worth understanding precisely, because this is the kind of nuance an interviewer actually wants to hear.

The first condition is **extreme parallelism at massive scale**. If your Flink cluster has 500 parallel workers (which is realistic for a truly planet-scale system like YouTube), and each one flushes to the same Redis key simultaneously, you have 500 concurrent `ZINCRBY` calls hitting one key every 5 seconds. Redis processes commands single-threaded per key, so these 500 calls queue up. At that level, you start to see microsecond-level queuing latency. It's still not a crisis, but it's measurable.

The second condition is **very aggressive flush intervals**. If you reduce the flush window from 5 seconds to 100 milliseconds to get more real-time trending updates, and you have 500 workers, now you're firing 500 writes to one key every 100ms — that's 5,000 writes per second to a single key. Redis can handle about 100,000 simple ops/second, so you're using 5% of one node's capacity on just this one viral video. Multiply by 10 videos all going viral simultaneously, and you're genuinely starting to feel it.

The third condition is the **read side**, which people often forget. The hot key problem in Redis is not just about writes — it's also about reads. If your trending page is getting 100,000 requests per second all calling `ZREVRANGE top_videos:24h 0 9`, that's 100,000 reads hitting the same single Redis node that hosts that key. This is actually the more realistic bottleneck than the write side, and it's where the "replicate across multiple nodes" solution applies.

---

## What the Document Should Have Said

The honest framing for an interview is this: batching via Flink largely eliminates the write-side hot key problem for most practical scales. The residual concerns are, first, read-side pressure on the sorted set key from high-traffic trending pages, and second, write contention only emerging at very high Flink parallelism with very short flush windows. The sharding solution is a forward-looking design choice for extreme scale, not something you'd need on day one.

If an interviewer asks you about hot keys in this system and you've already introduced Flink batching, the right answer is exactly what you intuited: _"Batching significantly reduces write pressure because we're collapsing millions of events into a handful of increments per flush cycle. The more realistic concern is read-side hotness on the sorted set, which we can address with read replicas or local caching of the top-K result."_

That answer demonstrates better systems thinking than the document did, because it shows you can trace the actual data flow and reason about where bottlenecks genuinely occur — rather than just pattern-matching to "high traffic = hot key problem" without checking whether the architecture already handles it.

how bar ew e putting for same video oid to diffenrt shrads

01:17

This is a really important question because it exposes a genuine contradiction in the sharding design. Let me work through it carefully.

---

## You've Found the Core Tension

The whole point of a sorted set is that **one video has one score**. If Video X has 4 million views, that number lives as a single entry in a single sorted set on a single Redis node. The moment you split writes across shards, you're saying "Video X's score is partially on shard 0, partially on shard 3, partially on shard 7" — which means at any given moment, **no single shard knows the true score of Video X**. You've traded correctness for write distribution.

So the question is: is that trade-off worth it, and how do you reconstruct the true score when you need it?

---

## How the Sharded Write Actually Works

When Flink flushes a batch, instead of writing the full delta to one key, it hashes the video ID to pick a shard:

python

```python
shard_id = hash("vid:9xQ3kp2Z") % 10  # always gives same shard for same video

# This video always goes to, say, shard_4
ZINCRBY top_videos:24h:shard_4  2778  vid:9xQ3kp2Z
```

Notice the critical detail here — **the same video always goes to the same shard** because the hash of a fixed video ID is always the same number. `hash("vid:9xQ3kp2Z") % 10` will always produce 4, every single flush, forever. So you're not randomly scattering a video's score across all shards. You're consistently assigning each video to exactly one shard, but _different videos go to different shards_. This is the key insight you were probably missing.

Think of it like assigning students to classrooms. Student A always goes to Room 4, Student B always goes to Room 7. Each room handles a different subset of students. No room gets all students, but each student always has exactly one room. The hash function is the assignment rule.

---

## So What Problem Does This Actually Solve?

It solves the **write distribution problem across videos**, not within a single video. If you have 10 million active videos all being flushed simultaneously, without sharding every single `ZINCRBY` call goes to the one Redis node that hosts `top_videos:24h`. With sharding, those 10 million writes get spread across 10 nodes, each handling 1 million writes. Each node is 10x less loaded.

For any individual video, the write path is identical to the non-sharded version — one `ZINCRBY` call per flush, going to one consistent shard. The video's score on its shard is always accurate and up to date.

---

## Then How Do You Read the Top 10?

This is where the complexity comes back. Since each video lives on exactly one shard, and different videos live on different shards, the true global top 10 is scattered across all 10 shards. You can't ask any single shard "who's number one?" because it only sees a tenth of the videos.

The solution is `ZUNIONSTORE`, which Redis provides natively:

bash

```bash
# Merge all shards into one temporary sorted set
ZUNIONSTORE top_videos:24h:merged 10 \
    top_videos:24h:shard_0 \
    top_videos:24h:shard_1 \
    ... \
    top_videos:24h:shard_9 \
    AGGREGATE SUM

# Now read from the merged result
ZREVRANGE top_videos:24h:merged 0 9 WITHSCORES

# Cache this merged result for 60 seconds so you don't merge on every request
EXPIRE top_videos:24h:merged 60
```

The `ZUNIONSTORE` command goes across all shards, takes each video's score from whichever shard it lives on, and produces one unified sorted set. Since each video only exists on one shard, `AGGREGATE SUM` here is equivalent to just taking the one existing score — there's nothing to sum because there's no duplication.

---

## Pause Here — Does This Design Actually Make Sense?

Honestly, at this point you should be asking yourself: if each video still lives on exactly one shard, and we still need to merge all shards to read the top 10, have we actually solved the hot key problem or just moved it around?

The answer is: **yes, we've genuinely solved the write pressure problem**, but the read problem is now worse than before, because `ZUNIONSTORE` is an expensive operation that touches all 10 shards. This is a real trade-off and a mature interviewer will expect you to acknowledge it. The typical mitigation is to cache the merged result aggressively — if you cache `top_videos:24h:merged` for 60 seconds, you're doing one expensive merge per minute instead of on every request. Given that trending lists don't need millisecond freshness, this is usually acceptable.



**◆**  **Architecture Overview**

**The trending system is built on three tiers: a Redis hot-path for real-time sorted scoring, a stream-processing layer (Flink/Spark) for windowed aggregation, and an analytical store (ClickHouse/BigQuery) for deep historical queries.**

  

|   |   |   |
|---|---|---|
|**Tier**|**Technology**|**Responsibility**|
|Hot Path|Redis Sorted Sets|Real-time top-K ranking, sub-ms reads|
|Stream Layer|Apache Flink / Spark Streaming|Windowed view-count aggregation from Kafka|
|Analytics Store|ClickHouse / BigQuery|Historical trends, ad-hoc queries, dashboards|
|Event Bus|Apache Kafka|Durable view-event queue, fan-out to consumers|
|Object Store|S3 / GCS|Cold snapshot backups of sorted sets|

  

**1.  REDIS SORTED-SET SCHEMA**

  

**▸**  **1.1  Key Naming Convention**

**Each time-window has a dedicated sorted set. Separating windows avoids O(N) re-aggregation at query time and allows independent TTL management.**

  

|   |
|---|
|# Pattern:  top_videos:{window}[:{shard_id}]<br><br>  <br><br>top_videos:24h          # rolling 24-hour window (TTL = 26 h)<br><br>top_videos:7d           # rolling 7-day  window (TTL = 8 d)<br><br>top_videos:30d          # rolling 30-day window (TTL = 32 d)<br><br>top_videos:alltime      # cumulative all-time   (no TTL)<br><br>  <br><br># Sharded hot-path replicas (anti hot-key pattern)<br><br>top_videos:24h:shard_0  ..  top_videos:24h:shard_9<br><br>  <br><br># Category-scoped (optional partition)<br><br>top_videos:24h:cat:gaming<br><br>top_videos:24h:cat:music|

  

**▸**  **1.2  Sorted-Set Schema & Key Fields**

|   |   |   |   |
|---|---|---|---|
|**Field**|**Type**|**Example**|**Description**|
|member (key)|String|vid:9xQ3kp2Z|Unique video identifier – prefixed for namespacing|
|score|Float64|4812930.0|Cumulative weighted view count for the window|
|TTL on key|Seconds|93600 (26 h)|Auto-expiry enforced via EXPIRE on the sorted-set key|
|ZREVRANK|Derived|0 = #1 trending|Server-side rank; no extra storage required|

  

**▸**  **1.3  Hash – Per-Video Metadata Cache**

**Alongside each sorted set, a companion Hash stores metadata needed to render the trending list without a DB round-trip.**

  

|   |
|---|
|# Key: video_meta:{video_id}<br><br>HSET video_meta:vid:9xQ3kp2Z \<br><br>  title          "How to Build Redis Clusters"       \<br><br>  channel_id     "ch:UC_abc123"                       \<br><br>  thumbnail_url  "https://cdn.example.com/t/9xQ3.jpg" \<br><br>  duration_sec   612                                   \<br><br>  uploaded_at    1720000000                            \<br><br>  category       "tech"                                \<br><br>  view_count_24h 4812930                               \<br><br>  like_count     238410<br><br>  <br><br># TTL matches its parent sorted-set window<br><br>EXPIRE video_meta:vid:9xQ3kp2Z 93600|

  

**▸**  **1.4  Example Redis Queries**

  

|   |   |
|---|---|
|**Use Case**|**Command**|
|Top 10 trending (24 h)|ZREVRANGE top_videos:24h 0 9 WITHSCORES|
|Rank of a specific video|ZREVRANK top_videos:24h vid:9xQ3kp2Z|
|Score of a video|ZSCORE top_videos:24h vid:9xQ3kp2Z|
|Increment view count|ZINCRBY top_videos:24h 1 vid:9xQ3kp2Z|
|Remove expired entry|ZREM top_videos:24h vid:9xQ3kp2Z|
|Count videos in set|ZCARD top_videos:24h|
|Top 50 with metadata (pipeline)|ZREVRANGE + HGETALL via pipeline|
|Videos ranked 100-200|ZREVRANGE top_videos:24h 99 199 WITHSCORES|

  

**▸**  **1.5  Pipelined Top-10 Fetch with Metadata**

  

|   |
|---|
|# Pseudo-code – Redis pipeline (Python redis-py)<br><br>pipe = redis.pipeline()<br><br>pipe.zrevrange('top_videos:24h', 0, 9, withscores=True)<br><br>results = pipe.execute()<br><br>  <br><br>video_ids, scores = zip(*results[0])   # [(vid_id, score), ...]<br><br>  <br><br># Batch-fetch metadata<br><br>pipe2 = redis.pipeline()<br><br>for vid_id in video_ids:<br><br>    pipe2.hgetall(f'video_meta:{vid_id}')<br><br>meta_list = pipe2.execute()            # [ {title, channel_id, ...}, ... ]<br><br>  <br><br># Merge results<br><br>trending = [<br><br>    {'rank': i+1, 'video_id': vid, 'score': sc, **meta}<br><br>    for i, (vid, sc, meta) in enumerate(zip(video_ids, scores, meta_list))<br><br>]|

  

**2.  CLICKHOUSE / BIGQUERY ANALYTICS SCHEMA**

  

**▸**  **2.1  Raw Events Table – video_views**

**Every view event lands in this table. ClickHouse's MergeTree family gives O(log N) primary-key lookups and columnar compression ratios of 10:1 on typical view data.**

  

|   |
|---|
|-- ClickHouse DDL<br><br>CREATE TABLE video_views<br><br>(<br><br>    video_id      String,               -- 'vid:9xQ3kp2Z'<br><br>    event_time    DateTime,             -- UTC, second precision<br><br>    view_duration UInt16,               -- seconds watched (0-32767)<br><br>    user_id       UInt64,               -- 0 = anonymous<br><br>    country_code  LowCardinality(String),<br><br>    platform      LowCardinality(String),  -- 'web'\|'ios'\|'android'\|'tv'<br><br>    source        LowCardinality(String),  -- 'homepage'\|'search'\|'external'<br><br>    session_id    UUID,<br><br>    is_unique     UInt8                 -- 0/1 flag set by dedup layer<br><br>)<br><br>ENGINE = MergeTree()<br><br>PARTITION BY toYYYYMM(event_time)<br><br>ORDER BY (video_id, event_time)<br><br>SETTINGS index_granularity = 8192;|

  

**▸**  **2.2  Pre-Aggregated Materialized Views**

**Materialized views maintain running view counts per window, updated incrementally as events arrive. This eliminates full-scan aggregation at query time.**

  

|   |
|---|
|-- Hourly aggregate: video_views_hourly<br><br>CREATE MATERIALIZED VIEW video_views_hourly<br><br>ENGINE = SummingMergeTree()<br><br>ORDER BY (video_id, hour)<br><br>POPULATE AS<br><br>SELECT<br><br>    video_id,<br><br>    toStartOfHour(event_time)  AS hour,<br><br>    countIf(is_unique = 1)     AS unique_views,<br><br>    count()                    AS total_views,<br><br>    avg(view_duration)         AS avg_watch_time<br><br>FROM video_views<br><br>GROUP BY video_id, hour;<br><br>  <br><br>-- Daily aggregate: video_views_daily<br><br>CREATE MATERIALIZED VIEW video_views_daily<br><br>ENGINE = SummingMergeTree()<br><br>ORDER BY (video_id, day)<br><br>POPULATE AS<br><br>SELECT<br><br>    video_id,<br><br>    toDate(event_time)         AS day,<br><br>    countIf(is_unique = 1)     AS unique_views,<br><br>    count()                    AS total_views<br><br>FROM video_views<br><br>GROUP BY video_id, day;|

  

**▸**  **2.3  Video Metadata Dimension Table**

  

|   |
|---|
|CREATE TABLE video_metadata<br><br>(<br><br>    video_id       String,<br><br>    title          String,<br><br>    channel_id     String,<br><br>    channel_name   String,<br><br>    category       LowCardinality(String),<br><br>    tags           Array(String),<br><br>    uploaded_at    DateTime,<br><br>    duration_sec   UInt32,<br><br>    thumbnail_url  String,<br><br>    language       LowCardinality(String),<br><br>    is_live        UInt8,<br><br>    _updated_at    DateTime DEFAULT now()<br><br>)<br><br>ENGINE = ReplacingMergeTree(_updated_at)<br><br>ORDER BY video_id;<br><br>  <br><br>-- ReplacingMergeTree ensures upsert semantics on video_id|

  

**▸**  **2.4  Trending Snapshot Table (Point-in-Time)**

**Periodically (every 5 min) the stream-processing job writes the current top-K into this table, enabling historical trend replay and A/B testing.**

  

|   |
|---|
|CREATE TABLE trending_snapshots<br><br>(<br><br>    snapshot_time  DateTime,<br><br>    window         LowCardinality(String),  -- '24h'\|'7d'\|'30d'\|'alltime'<br><br>    rank           UInt16,<br><br>    video_id       String,<br><br>    score          Float64,<br><br>    view_delta     Int64,    -- change in score since last snapshot<br><br>    rank_delta     Int16     -- change in rank (+/-)<br><br>)<br><br>ENGINE = MergeTree()<br><br>PARTITION BY (window, toDate(snapshot_time))<br><br>ORDER BY (window, snapshot_time, rank)<br><br>TTL snapshot_time + INTERVAL 90 DAY;|

  

**▸**  **2.5  Analytical Query Examples**

  

|   |   |
|---|---|
|**Query Goal**|**SQL**|
|Top 10 videos (24 h) with metadata|SELECT h.video_id, m.title, sum(h.total_views) AS views<br><br>FROM video_views_hourly h<br><br>JOIN video_metadata m USING(video_id)<br><br>WHERE h.hour >= now() - INTERVAL 24 HOUR<br><br>GROUP BY h.video_id, m.title<br><br>ORDER BY views DESC LIMIT 10;|
|Views per hour for one video|SELECT hour, total_views<br><br>FROM video_views_hourly<br><br>WHERE video_id = 'vid:9xQ3kp2Z'<br><br>  AND hour >= now() - INTERVAL 48 HOUR<br><br>ORDER BY hour;|
|7-day trending (daily rollup)|SELECT video_id, sum(total_views) AS views_7d<br><br>FROM video_views_daily<br><br>WHERE day >= today() - 7<br><br>GROUP BY video_id<br><br>ORDER BY views_7d DESC LIMIT 20;|
|Country breakdown for top video|SELECT country_code, count() AS views<br><br>FROM video_views<br><br>WHERE video_id = 'vid:9xQ3kp2Z'<br><br>  AND event_time >= now() - INTERVAL 24 HOUR<br><br>GROUP BY country_code<br><br>ORDER BY views DESC;|
|Rank trajectory over time|SELECT snapshot_time, rank, score<br><br>FROM trending_snapshots<br><br>WHERE video_id = 'vid:9xQ3kp2Z'<br><br>  AND window = '24h'<br><br>ORDER BY snapshot_time;|

  

**3.  STREAM PROCESSING PIPELINE**

  

**▸**  **3.1  Kafka Topic Schema (Avro)**

  

|   |
|---|
|// Avro schema – topic: view.events.v2<br><br>{<br><br>  "type": "record",<br><br>  "name": "ViewEvent",<br><br>  "fields": [<br><br>    { "name": "video_id",       "type": "string"  },<br><br>    { "name": "user_id",        "type": ["null","long"], "default": null },<br><br>    { "name": "session_id",     "type": "string"  },<br><br>    { "name": "event_time_ms",  "type": "long"    },  // Unix ms<br><br>    { "name": "view_duration",  "type": "int"     },  // seconds<br><br>    { "name": "country_code",   "type": "string"  },<br><br>    { "name": "platform",       "type": "string"  },<br><br>    { "name": "source",         "type": "string"  }<br><br>  ]<br><br>}<br><br>  <br><br># Partitioning strategy<br><br># Partition key = video_id → guarantees per-video ordering<br><br># Retention = 7 days (for replay / backfill)|

  

**▸**  **3.2  Flink Windowed Aggregation (Java-like pseudo-code)**

  

|   |
|---|
|DataStream<ViewEvent> events = env<br><br>    .addSource(new KafkaSource<>("view.events.v2"))<br><br>    .assignTimestampsAndWatermarks(<br><br>        WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(10)));<br><br>  <br><br>// ─── 24-hour sliding window ───────────────────────<br><br>events<br><br>  .keyBy(e -> e.videoId)<br><br>  .window(SlidingEventTimeWindows.of(Time.hours(24), Time.minutes(5)))<br><br>  .aggregate(new ViewCountAggregator())   // sum(views) per video<br><br>  .addSink(new RedisSortedSetSink("top_videos:24h"));<br><br>  <br><br>// ─── 7-day tumbling window ────────────────────────<br><br>events<br><br>  .keyBy(e -> e.videoId)<br><br>  .window(TumblingEventTimeWindows.of(Time.hours(1)))<br><br>  .aggregate(new ViewCountAggregator())<br><br>  .keyBy(r -> r.videoId)<br><br>  .window(TumblingEventTimeWindows.of(Time.days(7)))<br><br>  .reduce(new SumReducer())<br><br>  .addSink(new RedisSortedSetSink("top_videos:7d"));<br><br>  <br><br>// ─── Dual sink: Redis + ClickHouse ───────────────<br><br>events<br><br>  .addSink(new ClickHouseSink("video_views"));  // raw append|

  

**▸**  **3.3  Redis Sink – Atomic ZINCRBY Pattern**

  

|   |
|---|
|# Flink RedisSortedSetSink – per micro-batch flush<br><br>pipe = redis.pipeline(transaction=False)<br><br>  <br><br>for video_id, delta in aggregated_window.items():<br><br>    shard = hash(video_id) % NUM_SHARDS          # hot-key sharding<br><br>    key   = f'top_videos:24h:shard_{shard}'<br><br>    pipe.zincrby(key, delta, video_id)           # atomic increment<br><br>  <br><br># Also write to canonical key for reads<br><br>    pipe.zincrby('top_videos:24h', delta, video_id)<br><br>  <br><br>pipe.execute()                                   # single round-trip<br><br>  <br><br># Trim to top 10 000 to cap memory<br><br>redis.zremrangebyrank('top_videos:24h', 0, -(10_001))|

  

**4.  ISSUES & SOLUTIONS (WITH SCHEMA IMPACT)**

  

**▸**  **Issue 1  ·  Real-time Aggregation of Billions of Views**

**With 100k+ view events/sec a naive INCR + SORT on a single key becomes a bottleneck. The solution is two-phase aggregation.**

  

|   |
|---|
|Phase 1 – Edge counters (in memory, per app pod)<br><br>──────────────────────────────────────────────────<br><br>local_counts = defaultdict(int)   # {video_id: count}<br><br>  <br><br>def on_view(event):<br><br>    local_counts[event.video_id] += 1<br><br>    if len(local_counts) >= FLUSH_THRESHOLD or time_elapsed() > 5:<br><br>        flush_to_flink(local_counts)  # batch publish to Kafka<br><br>        local_counts.clear()<br><br>  <br><br>Phase 2 – Flink session window merges into Redis<br><br>──────────────────────────────────────────────────<br><br># ZINCRBY is O(log N) – use pipelines to batch<br><br># Flush every 5 seconds → latency acceptable for trending|

  

**▸**  **Issue 2  ·  Multiple Time Windows (24h, 7d, all-time)**

**Each window requires a separate data structure. The schema uses bucketed pre-aggregation with a ring-buffer pattern.**

|   |
|---|
|# Schema: per-hour buckets stored as sorted sets<br><br># Key pattern: view_bucket:{video_id}:{YYYY-MM-DD-HH}<br><br>  <br><br># On each view event<br><br>bucket_key = f'view_bucket:{video_id}:{datetime.utcnow().strftime("%Y-%m-%d-%H")}' <br><br>redis.incr(bucket_key)<br><br>redis.expire(bucket_key, 3600 * 32)   # keep 32 h of hourly buckets<br><br>  <br><br># 24h window score = sum of last 24 buckets<br><br>def get_24h_score(video_id):<br><br>    keys = [f'view_bucket:{video_id}:{h}' for h in last_24_hours()]<br><br>    return sum(int(redis.get(k) or 0) for k in keys)<br><br>  <br><br># 7d window = pre-aggregated in ClickHouse daily rollup<br><br># SELECT sum(total_views) FROM video_views_daily<br><br># WHERE video_id=? AND day >= today()-7<br><br>  <br><br># All-time = single ZSCORE read from top_videos:alltime|

  

**▸**  **Issue 3  ·  Hot-Key Problem in Distributed Redis**

**Viral videos hit millions of ZINCRBY calls/sec on the same key across all nodes. Solution: replication + read fan-out across shards.**

  

|   |
|---|
|# Write path – scatter across N shards<br><br>NUM_SHARDS = 10<br><br>  <br><br>def increment_view(video_id, delta=1):<br><br>    shard_id = xxhash(video_id) % NUM_SHARDS<br><br>    redis_cluster[shard_id].zincrby(<br><br>        f'top_videos:24h:shard_{shard_id}',<br><br>        delta,<br><br>        video_id<br><br>    )<br><br>  <br><br># Read path – merge-sort from all shards (ZUNIONSTORE)<br><br>def get_top10_24h():<br><br>    shard_keys = [f'top_videos:24h:shard_{i}' for i in range(NUM_SHARDS)]<br><br>    redis.zunionstore(<br><br>        'top_videos:24h:merged',<br><br>        shard_keys,<br><br>        aggregate='SUM'<br><br>    )<br><br>    redis.expire('top_videos:24h:merged', 60)   # short TTL on merged<br><br>    return redis.zrevrange('top_videos:24h:merged', 0, 9, withscores=True)<br><br>  <br><br># Use local read replicas to further distribute ZREVRANGE traffic|

  

**▸**  **Issue 4  ·  Score Fairness – Preventing View-Count Manipulation**

**Raw view counts can be gamed. A weighted score function factors in unique viewers, watch-time, and recency decay.**


|   |
|---|
|# Weighted score formula (computed in Flink before writing to Redis)<br><br>def compute_score(views, unique_viewers, avg_watch_sec,<br><br>                  duration_sec, hours_since_upload):<br><br>    # Engagement ratio (0-1)<br><br>    engagement = min(avg_watch_sec / max(duration_sec, 1), 1.0)<br><br>  <br><br>    # Unique-viewer ratio (penalises bot traffic)<br><br>    uv_ratio = unique_viewers / max(views, 1)<br><br>  <br><br>    # Time-decay: halve weight every 6 hours<br><br>    decay = 0.5 ** (hours_since_upload / 6.0)<br><br>  <br><br>    score = views * engagement * uv_ratio * decay<br><br>    return round(score, 2)<br><br>  <br><br># Write to Redis<br><br>redis.zadd('top_videos:24h', {video_id: score}, xx=False)|

  

**5.  COMPLETE SCHEMA REFERENCE**

  

**▸**  **5.1  Redis Key Inventory**

|   |   |   |   |
|---|---|---|---|
|**Key Pattern**|**Type**|**TTL**|**Purpose**|
|top_videos:24h|Sorted Set|26 h|Rolling 24-h trending rank|
|top_videos:7d|Sorted Set|8 d|Rolling 7-day trending rank|
|top_videos:30d|Sorted Set|32 d|Rolling 30-day trending rank|
|top_videos:alltime|Sorted Set|None|All-time cumulative rank|
|top_videos:24h:shard_{0-9}|Sorted Set|26 h|Write shards (hot-key mitigation)|
|top_videos:24h:cat:{category}|Sorted Set|26 h|Category-scoped ranking|
|video_meta:{video_id}|Hash|26 h|Metadata cache for rendering|
|view_bucket:{video_id}:{YYYY-MM-DD-HH}|String|32 h|Hourly view count bucket|
|dedup:{session_id}|String|3 h|View deduplication flag|

  

**▸**  **5.2  ClickHouse Table Inventory**
  
|   |   |   |   |
|---|---|---|---|
|**Table**|**Engine**|**Retention**|**Purpose**|
|video_views|MergeTree|2 years|Raw view events (immutable append log)|
|video_views_hourly|SummingMergeTree (MV)|6 months|Hourly pre-aggregated view counts|
|video_views_daily|SummingMergeTree (MV)|2 years|Daily pre-aggregated view counts|
|video_metadata|ReplacingMergeTree|Forever|Video dimension data (upsertable)|
|trending_snapshots|MergeTree|90 days|Periodic top-K snapshots for audit/replay|