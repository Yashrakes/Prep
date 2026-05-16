
## Count-Min Sketch & Top-K Videos — Deep Dive

### What is Count-Min Sketch?

It's a **probabilistic data structure** used to estimate the frequency of elements in a data stream. Think of it as a "lossy frequency counter" — it trades **a little accuracy** for **massive memory savings**.

It belongs to the family of **sketch algorithms** (like HyperLogLog, Bloom Filter) — all designed for the same tradeoff: approximate answers at scale.

---

### The Core Idea — How It Works

#### Structure

A 2D array of `d` rows × `w` columns, all initialized to 0. Each row has its own **independent hash function**.

```
d = depth (number of hash functions) → controls error probability
w = width (number of counters per row) → controls error magnitude
```

#### On Update (increment frequency of element `x`)

For each row `i` from 0 to d-1:

1. Compute `h_i(x) % w` → get column index
2. Increment `table[i][h_i(x) % w]`

So one element touches exactly `d` cells — one per row.

#### On Query (estimate frequency of `x`)

For each row `i`, read `table[i][h_i(x) % w]` Return `min(all d values)`

Why minimum? Because **hash collisions only inflate counts** — two different elements can map to the same cell, but no element ever _decreases_ another's count. So the **minimum across all rows** gives the least-inflated estimate.

```
True count ≤ Estimated count ≤ True count + ε·N
```

where ε = error factor, N = total stream size

---

### Mathematical Guarantees

Given:

- `w = ⌈e / ε⌉` (e ≈ 2.718)
- `d = ⌈ln(1/δ)⌉`

You get:

- **Error bound**: Estimate ≤ true count + ε·N, with probability ≥ 1 − δ
- **Memory**: O(d × w) = O((1/ε) × log(1/δ))

**Example**: ε = 0.001, δ = 0.01 → w = 2719, d = 5 → ~13,595 counters Compare to exact counting of 10M unique URLs → 10M counters. **~740x memory reduction.**

---

### Top-K Videos — System Design Integration

#### Problem Statement

> YouTube/Netflix/TikTok wants real-time "Trending Now" — top K most-watched videos in the last X minutes/hours.

#### Naive Approach & Why It Fails

```
HashMap<videoId, count> exact = new ConcurrentHashMap<>();
```

- At 1M req/sec, you'd have 100M+ unique video IDs in memory
- Lock contention on hot videos (viral clips get hammered)
- No windowing — counts grow unbounded
- Can't shard easily (global counter = single bottleneck)

#### CMS-Based Architecture

```
┌─────────┐    ┌──────────────┐    ┌─────────────────────────┐
│ API GW  │───▶│ Kafka Topic  │───▶│  Stream Processor       │
│ (views) │    │ (view events)│    │  (Flink / Kafka Streams)│
└─────────┘    └──────────────┘    └────────────┬────────────┘
                                                │
                              ┌─────────────────▼──────────────────┐
                              │         CMS per time window         │
                              │  [CMS-5min] [CMS-1hr] [CMS-24hr]   │
                              └─────────────────┬──────────────────┘
                                                │
                              ┌─────────────────▼──────────────────┐
                              │      Min-Heap (size K)              │
                              │  tracks top-K candidates            │
                              └─────────────────┬──────────────────┘
                                                │
                              ┌─────────────────▼──────────────────┐
                              │      Redis / Cache Layer            │
                              │  stores top-K result every 30s      │
                              └────────────────────────────────────┘
```

#### Step-by-Step Flow

**1. Event Ingestion**

```
User watches video → API Gateway → Kafka (partitioned by videoId)
```

Partitioning by videoId means all events for the same video go to the same partition → same CMS shard.

**2. Stream Processing (per partition)**

java

```java
// Pseudocode — Flink operator
for each event e:
    cms.update(e.videoId)              // O(d) — constant time
    long estimate = cms.query(e.videoId)
    minHeap.offer(new VideoCount(e.videoId, estimate))
    if minHeap.size() > K:
        minHeap.poll()                 // evict smallest
```

**3. Time Windowing — Sliding vs Tumbling**

|Window Type|CMS Strategy|Use Case|
|---|---|---|
|Tumbling (fixed)|New CMS every window|"Top K last hour" exact windows|
|Sliding|Multiple CMS instances (ring buffer)|"Top K in last 60 min, updated every 5 min"|
|Decay-based|Age old counts with multiplier|Continuous trending score|

For sliding windows, a common trick: maintain `n` CMS instances for sub-windows, merge by adding corresponding cells. Mergeability is a key CMS property — just element-wise add the matrices.

**4. Global Aggregation** Each partition has a local top-K. A coordinator merges them:

```
global_top_K = merge(partition_1_top_K, partition_2_top_K, ...)
```

For merging CMS across shards: simply add the matrices cell-by-cell (same hash functions required — seed them with the same value).

**5. Serving Layer** Top-K result written to Redis every 30 seconds. Read latency: sub-millisecond. Clients poll this, not the raw CMS.

---

### CMS + Min-Heap — The Classic Combo

These two always go together for Top-K:

```
CMS  → answers "how frequent is X?" in O(d) time
Min-Heap (size K) → answers "is X in top K?" in O(log K) time
```

**Why not Max-Heap?** You want to efficiently evict the _smallest_ element when a new candidate arrives. Min-Heap gives you O(1) peek at the current Kth-largest, O(log K) insertion.

java

```java
class TopKTracker {
    CountMinSketch cms;
    PriorityQueue<VideoCount> minHeap; // min-heap, size K

    void record(String videoId) {
        cms.update(videoId);
        long freq = cms.estimate(videoId);

        if (minHeap.size() < K) {
            minHeap.offer(new VideoCount(videoId, freq));
        } else if (freq > minHeap.peek().count) {
            minHeap.poll();
            minHeap.offer(new VideoCount(videoId, freq));
        }
    }
}
```

---

### Pros

- **Memory**: O(d·w) regardless of distinct element count — fixed size, predictable
- **Speed**: O(d) update and query — essentially O(1) since d is a constant (typically 5–10)
- **Mergeable**: Add two CMS matrices cell-by-cell — perfect for distributed aggregation
- **No false negatives**: Never underestimates — the minimum is always ≥ true count
- **Cache-friendly**: Simple 2D array — great CPU cache locality
- **Tunable**: You control ε and δ independently — tune for your accuracy/memory tradeoff

---

### Cons & Tradeoffs

|Limitation|Detail|Mitigation|
|---|---|---|
|Only overestimates|Collisions inflate counts, never deflate|Accept it — use for ranking not exact billing|
|No deletions natively|Decrementing causes negative counts, breaks guarantees|Use **Count-Min-CU** (conservative update) or separate delete log|
|Hot key amplification|A viral video with millions of hits causes its column to be a "collision attractor" for other videos in the same bucket|Wider sketch (increase w) or separate exact counter for known hot keys|
|Hash function quality matters|Bad hash → uneven distribution → more collisions|Use MurmurHash3 or xxHash — never use Java's default .hashCode()|
|No per-element deletion for sliding windows|Can't "undo" old events|Use separate CMS per time bucket, discard old buckets|
|Approximate Top-K|An element near the Kth boundary might be wrong|Good enough for "trending" use case — not for billing/analytics|

---

### CMS vs Alternatives

| Approach                       | Memory           | Accuracy                    | Deletions | Merge         |
| ------------------------------ | ---------------- | --------------------------- | --------- | ------------- |
| **Exact HashMap**              | O(n)             | Perfect                     | ✅         | ✅ (expensive) |
| **Count-Min Sketch**           | O(1/ε · log 1/δ) | Approximate (overestimates) | ❌ native  | ✅ trivial     |
| **Count Sketch**               | Similar          | Unbiased (over + under)     | ✅         | ✅             |
| **Lossy Counting**             | O(1/ε)           | Exact for freq ≥ εN         | ❌         | ❌             |
| **HeavyHitter (Space Saving)** | O(K)             | Exact top-K candidates      | ✅         | ❌             |

**Space-Saving algorithm** is actually CMS's main competitor for Top-K — it guarantees all true top-K are in its result set. CMS doesn't. But Space-Saving can't be merged across nodes, which kills it in distributed settings.

---

### Interview Talking Points — Say These Out Loud

**On accuracy:**

> "CMS gives a probabilistic upper bound. For a trending feed, a ±0.1% error on view counts is completely acceptable — we're not billing on this data. I'd use exact counters only for revenue-critical paths."

**On distributed merging:**

> "Because CMS is linearly mergeable — you just add the matrices — each Kafka partition maintains its own local CMS, and a coordinator aggregates them. No global locking, no coordination overhead during hot path."

**On time windows:**

> "For a sliding 1-hour window, I'd maintain 12 CMS instances of 5 minutes each. When a new 5-min bucket starts, I drop the oldest and merge the remaining 11. O(d·w) merge cost, not O(events)."

**On hot key problem:**

> "If a video goes viral — say 10M views/minute — its column in the CMS becomes a collision magnet. I'd use a hybrid: a small exact counter HashMap for the top-100 known hot videos (identified by a previous Top-K pass), and CMS for the long tail."

**On hash functions:**

> "I'd seed the same MurmurHash3 differently per row. Never Java's .hashCode() — it has terrible distribution for strings and will cluster collisions."

---

### Production Systems That Use This

- **Twitter**: Trending hashtags — CMS + sliding window
- **LinkedIn**: "Trending in your network" feed
- **Cloudflare**: DDoS detection — identify IPs with anomalous request rates
- **Redis**: `redis-cli` has a built-in approximate Top-K via `RedisBloom` module using CMS under the hood
- **Apache Flink / Spark Streaming**: Both have built-in CMS implementations

---

### One-Line Summary for Interview

> "Count-Min Sketch is a fixed-size 2D counter array where each element is hashed into one cell per row. We update all rows on write and take the minimum on read to cancel out collision noise. For Top-K, we pair it with a size-K min-heap — CMS handles frequency estimation in O(1), the heap tracks the current K leaders. It's the go-to for distributed trending systems because it's mergeable, memory-bounded, and tunable."