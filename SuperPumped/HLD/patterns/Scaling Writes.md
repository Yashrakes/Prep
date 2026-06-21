# Scaling Writes Pattern

## Overview
Pattern for handling high-volume write traffic as applications grow from hundreds to millions of writes per second. Single database servers and storage systems hit hard limits, requiring horizontal scaling strategies.

---

## When to Use This Pattern
- **Social media posts** - Millions of users posting simultaneously
- **IoT sensor data** - Thousands of devices sending telemetry
- **Log aggregation** - High-volume application logs
- **Analytics events** - User activity tracking, clickstream
- **Chat messages** - High-frequency messaging
- **Financial transactions** - Trading platforms, payment systems
- **Gaming leaderboards** - Real-time score updates

---

## Why Writes Become a Bottleneck

### Single Database Limits

**Physical Limits**:
```
Disk I/O: ~10,000 writes/second (HDD)
         ~100,000 writes/second (SSD)
CPU: Transaction processing overhead
Memory: Write buffer capacity
Network: Replication bandwidth

Real-world single database:
- 5,000-10,000 writes/second sustained
- Peaks can cause queue buildup
- Latency increases under load
```

**Scaling Problem**:
```
Read scaling: Add read replicas (easy)
Write scaling: All writes go to primary (hard)

Cannot add more primaries easily:
- Need to coordinate between them
- Distributed consensus is complex
```

---

## Solution Strategies

## 1. Horizontal Sharding (Database Partitioning)

### How It Works

**Concept**:
- Split data across multiple independent databases
- Each database (shard) handles subset of data
- Route writes to correct shard based on partition key

**Architecture**:
```
Application
    ↓
Shard Router (determines which shard)
    ├→ Shard 1 (users 0-999,999)
    ├→ Shard 2 (users 1M-1,999,999)
    ├→ Shard 3 (users 2M-2,999,999)
    └→ Shard 4 (users 3M-3,999,999)
```

---

### Sharding Strategies

**1. Hash-Based Sharding**:

**How It Works**:
```
shard_id = hash(user_id) % num_shards

Example:
hash(12345) = 78912
78912 % 4 = 0 → Shard 0

hash(67890) = 23456
23456 % 4 = 2 → Shard 2
```

**Pros**:
- Even distribution (good hash function)
- Simple algorithm
- Predictable shard location

**Cons**:
- Resharding is difficult (changing num_shards changes all mappings)
- Range queries span all shards
- Related data may split across shards

**When to Use**:
- Need even distribution
- Lookup by ID (not range queries)
- Number of shards relatively stable

**Include in Design**:
- "Use hash-based sharding on user_id"
- "Shard = hash(user_id) % num_shards"
- "Ensures even distribution across shards"
- "Trade-off: difficult to add shards later"

---

**2. Range-Based Sharding**:

**How It Works**:
```
Shard 0: user_id 0 - 999,999
Shard 1: user_id 1M - 1,999,999
Shard 2: user_id 2M - 2,999,999
Shard 3: user_id 3M+

Lookup:
if user_id < 1M: Shard 0
elif user_id < 2M: Shard 1
elif user_id < 3M: Shard 2
else: Shard 3
```

**Pros**:
- Range queries stay within one shard
- Easy to add new shards (just extend range)
- Related data stays together (sequential IDs)

**Cons**:
- Uneven distribution (hotspots)
- New users all go to latest shard
- Old data shards underutilized

**When to Use**:
- Need range queries (time-based, ID-based)
- Data has natural ranges
- Can tolerate uneven distribution

**Include in Design**:
- "Use range-based sharding on timestamp"
- "Recent data in hot shard, archive older shards"
- "Easy to add new time ranges"
- "Trade-off: hot shard gets all new writes"

---

**3. Geographic Sharding**:

**How It Works**:
```
Shard US-East: US users
Shard EU-West: European users
Shard Asia-Pacific: Asian users

Route based on user location
```

**Pros**:
- Low latency (data near users)
- Regulatory compliance (data residency)
- Natural partitioning

**Cons**:
- Uneven distribution (population varies)
- Cross-region queries complex
- Users moving regions

**When to Use**:
- Global application
- Latency sensitive
- Data residency requirements

**Include in Design**:
- "Shard by user geography for low latency"
- "EU users → EU database (GDPR compliance)"
- "Cross-region queries rare"

---

**4. Directory-Based (Lookup Table)**:

**How It Works**:
```
Shard Lookup Service (maps user_id → shard_id)

user_id 12345 → Shard 2
user_id 67890 → Shard 1

Can use hash, range, or custom logic
Flexible reassignment
```

**Pros**:
- Most flexible (can reassign anytime)
- Custom sharding logic
- Easy rebalancing

**Cons**:
- Extra lookup hop (latency)
- Lookup service is single point of failure
- Adds complexity

**When to Use**:
- Need flexibility in shard assignment
- Can cache lookup mappings
- Worth extra hop for flexibility

**Include in Design**:
- "Use directory-based sharding with lookup service"
- "Can rebalance users across shards without rehashing"
- "Cache shard mappings in app servers"
- "Trade-off: extra lookup hop"

---

### Choosing Partition Keys

**Critical Decision**: Partition key determines data distribution and query patterns

**Good Partition Keys**:

**Even Distribution**:
```
✓ user_id (hash-based)
  - Users distributed evenly
  - No hotspots

✓ random_uuid
  - Perfect distribution
  - But harder to route queries
```

**Related Data Together**:
```
✓ tenant_id (multi-tenant app)
  - All tenant data in one shard
  - Queries don't span shards

✓ user_id (social media)
  - User's posts in one shard
  - User feed queries efficient
```

**Bad Partition Keys**:

**Hotspots**:
```
✗ status (active/inactive)
  - All active users in one shard
  - Massive imbalance

✗ date (current date)
  - All new data goes to one shard
  - Today's shard overloaded
```

**Breaks Queries**:
```
✗ random_id (for social graph)
  - User A's data in Shard 1
  - User B's data in Shard 2
  - Cannot efficiently query "A's friends"
```

**Example: Social Media**

```
Good: Partition by user_id
- User's posts in one shard
- User's followers in one shard
- Queries like "show user's posts" → single shard

Bad: Partition by post_id
- User's posts scattered across shards
- "Show user's posts" → query all shards → slow
```

**Include in Design**:
- "Partition key: user_id (hash-based)"
- "Keeps user's data together"
- "Even distribution (good hash function)"
- "Most queries single-shard"

---

### Challenges with Sharding

**Challenge 1: Cross-Shard Queries**

**Problem**:
```
Query: "Find all posts with #technology"

With sharding by user_id:
- Hashtag can be on posts in any shard
- Must query all shards
- Aggregate results
```

**Solutions**:

**1. Scatter-Gather**:
```
1. Send query to all shards in parallel
2. Each shard returns results
3. Application aggregates and sorts
4. Return top N

Slow: latency = slowest shard
```

**2. Duplicate Data (Denormalization)**:
```
hashtag_posts table (separate, not sharded):
- Stores hashtag → post_id mappings
- Can be in different database
- Query this for hashtag search
```

**3. Search Index (Elasticsearch)**:
```
- Index all posts in Elasticsearch
- Elasticsearch handles sharding internally
- Query ES for hashtag search
- Fetch post details from database shards
```

**Include in Design**:
- "Cross-shard queries are expensive"
- "Use Elasticsearch for global search"
- "Partition key chosen to minimize cross-shard queries"

---

**Challenge 2: Distributed Transactions**

**Problem**:
```
Transfer money from User A to User B
User A in Shard 1
User B in Shard 2

Need atomic operation across shards
```

**Solutions**:

**1. Avoid When Possible**:
```
Design to avoid cross-shard transactions
Example: Keep user's wallet in same shard as user
```

**2. Use 2-Phase Commit (if must)**:
```
Coordinator:
1. Prepare phase: Ask both shards to prepare
2. Commit phase: If both ready, commit both

Issues: Blocking, slow, complex
```

**3. Saga Pattern**:
```
1. Deduct from User A (Shard 1)
2. Add to User B (Shard 2)
3. If step 2 fails, compensate step 1 (add back to A)

Eventually consistent
```

**Include in Design**:
- "Avoid distributed transactions by design"
- "Keep related data in same shard"
- "Use Saga pattern for cross-shard operations"
- "Accept eventual consistency where possible"

---

**Challenge 3: Resharding (Adding/Removing Shards)**

**Problem**:
```
Current: 4 shards (hash % 4)
New: 8 shards (hash % 8)

User 12345:
Old: hash(12345) % 4 = 1 → Shard 1
New: hash(12345) % 8 = 5 → Shard 5

Most data needs to move!
```

**Solutions**:

**1. Consistent Hashing**:
```
- Virtual nodes on hash ring
- Adding shard only affects adjacent ranges
- Rebalances ~1/N of data instead of all

Example:
4 shards → 8 shards
Only 50% of data moves (not 100%)
```

**2. Pre-Shard**:
```
- Start with 1024 virtual shards
- Map virtual → physical shards
- Physical shards: 4, but track 1024 virtual

Adding physical shard:
- Just remap some virtual shards
- Data movement is controlled
```

**3. Directory-Based**:
```
- Lookup service tracks user → shard
- Can remap users gradually
- No rehashing needed
```

**Include in Design**:
- "Use consistent hashing for easier resharding"
- "Virtual shards allow gradual rebalancing"
- "Plan for resharding from day 1"

---

**Challenge 4: Hotspots (Celebrity Problem)**

**Problem**:
```
Celebrity user with millions of followers
All their activity in one shard
That shard overloaded
```

**Solutions**:

**1. Further Partition Hot Keys**:
```
Normal users: Shard by user_id
Celebrity users: Shard followers across multiple shards

Celebrity followers table:
- Shard 1: Followers 0-999,999
- Shard 2: Followers 1M-1,999,999
```

**2. Read Replicas for Hot Shard**:
```
Hot shard gets dedicated read replicas
Writes still to one primary
Reads distributed
```

**3. Caching**:
```
Cache celebrity posts heavily
Most reads from cache
Reduces database load
```

**Include in Design**:
- "Detect hot shards via monitoring"
- "Add read replicas to hot shards"
- "Cache hot users aggressively"

---

## 2. Vertical Partitioning

### How It Works

**Concept**:
- Split different tables/data types into separate databases
- Not by rows (like sharding), but by columns/tables

**Example**:

**Monolithic Database**:
```
Single DB:
- users table
- posts table
- messages table
- analytics_events table
```

**Vertical Partitioning**:
```
User Service DB:
- users table

Social DB:
- posts table
- comments table

Messaging DB:
- messages table
- conversations table

Analytics DB:
- events table
```

**When to Use**:
- Different workload characteristics
- Different scaling needs
- Different access patterns

**Pros**:
- Scale independently
- Different database types (SQL vs NoSQL)
- Isolate failure domains

**Cons**:
- Joins across databases impossible
- Distributed transaction complexity
- More operational overhead

**Include in Design**:
- "Separate user data and analytics data"
- "Analytics DB optimized for writes (time-series DB)"
- "User DB optimized for reads"
- "Scale independently based on needs"

---

## 3. Write Queues (Buffering Burst Traffic)

### How It Works

**Concept**:
- Accept writes into fast queue
- Background workers drain queue to database
- Smooths out traffic spikes

**Architecture**:
```
Client Writes → Queue (Kafka/Redis) → Workers → Database
                 (10,000/s)           (1,000/s)  

Queue absorbs burst
Workers process at sustainable rate
```

**When to Use**:
- Bursty write traffic (flash sales, viral events)
- Can tolerate async writes (not immediate consistency)
- Protect database from overload

**Example: Analytics Events**:
```
User clicks button → Event to Kafka (instant)
Workers consume from Kafka → Write to database in batches

Benefits:
- User gets instant response
- Database not overloaded
- Can batch writes for efficiency
```

**Pros**:
- Handles traffic spikes
- Protects database
- Can batch for efficiency

**Cons**:
- Eventual consistency (delay)
- Queue can grow unbounded
- More complexity

**Include in Design**:
- "Use Kafka queue for analytics events"
- "Workers batch write to database"
- "Queue handles 10x burst traffic"
- "Trade-off: slight delay in data visibility"

---

## 4. Write Batching

### How It Works

**Concept**:
- Group multiple writes into single transaction
- Reduces per-write overhead
- Higher throughput

**Example**:

**Individual Writes**:
```
for each event:
    db.execute("INSERT INTO events VALUES (...)")

100 events = 100 database round-trips
```

**Batched Writes**:
```
events = collect 100 events
db.execute("INSERT INTO events VALUES (...), (...), ..., (...)")

100 events = 1 database round-trip
```

**Throughput Improvement**:
```
Individual: 1,000 writes/second
Batched (100 per batch): 10,000 writes/second

10x improvement
```

**When to Use**:
- High-volume writes
- Writes can be delayed slightly
- Not user-facing (analytics, logs)

**Trade-offs**:
- **Pros**: Higher throughput, lower overhead
- **Cons**: Slightly higher latency, partial failure handling

**Include in Design**:
- "Batch analytics events (100 per batch)"
- "Reduces database round-trips 100x"
- "Increases write throughput 10x"

---

## 5. Load Shedding (Graceful Degradation)

### How It Works

**Concept**:
- When overloaded, reject/drop low-priority requests
- Preserve capacity for high-priority requests
- Fail gracefully

**Priority Tiers**:
```
Tier 1 (Critical): Payment transactions, login
Tier 2 (Important): Posts, messages
Tier 3 (Optional): Analytics, metrics

Under load:
- Drop Tier 3
- If still overloaded, drop Tier 2
- Always serve Tier 1
```

**Implementation**:
```
function handleWrite(request):
    current_load = getSystemLoad()
    
    if current_load > 90% and request.priority == "low":
        return 503 Service Unavailable
    
    if current_load > 95% and request.priority == "medium":
        return 503 Service Unavailable
    
    // Process high priority or normal load
    processWrite(request)
```

**When to Use**:
- Traffic spikes exceed capacity
- Want to protect critical functionality
- Better to reject than crash

**Include in Design**:
- "Implement load shedding for non-critical writes"
- "Under 90% load, drop analytics events"
- "Preserve capacity for user posts and transactions"
- "Return 503, client can retry later"

---

## 6. Async Replication (Eventual Consistency)

### How It Works

**Synchronous Replication**:
```
1. Write to primary
2. Wait for replicas to acknowledge
3. Return success to client

Latency: 50ms + 30ms replication = 80ms
Throughput: Limited by replication
```

**Asynchronous Replication**:
```
1. Write to primary
2. Return success to client immediately
3. Replicate to replicas in background

Latency: 50ms
Throughput: Higher (not waiting)
```

**Trade-offs**:
- **Pros**: Higher write throughput, lower latency
- **Cons**: Replicas lag, potential data loss if primary crashes

**When to Use**:
- Can tolerate eventual consistency
- High write throughput needed
- Data loss acceptable (or mitigated other ways)

**Include in Design**:
- "Use async replication for high write throughput"
- "Accept ~100ms replication lag"
- "Trade-off: potential data loss if primary fails"
- "Mitigate with write-ahead log persistence"

---

## Complete Design Example: IoT Sensor Data

### Requirements:
- 1 million IoT devices
- Each sends data every 10 seconds
- 100,000 writes/second
- Need to store for analytics
- Can tolerate 1-minute delay

### Design:

**Level 1: Write Queue (Buffering)**:
```
Devices → Kafka Topic → Workers → Time-Series Database

Kafka:
- Topic: sensor-data
- Partitions: 100 (parallelism)
- Retention: 7 days (replay capability)

Benefits:
- Kafka handles 100K writes/second easily
- Devices get instant acknowledgment
- Backpressure protection
```

**Level 2: Batching**:
```
Workers:
- Consume from Kafka in batches
- Accumulate 1000 events
- Batch write to database

Before: 100,000 individual writes/second
After: 100 batch writes/second (1000 per batch)

Database load reduced 1000x
```

**Level 3: Sharding (Time-Series Database)**:
```
Use InfluxDB or TimescaleDB (built for time-series)

Sharding by time:
- Shard 1: Data from 2025-01-01 to 2025-01-31
- Shard 2: Data from 2025-02-01 to 2025-02-28
- Current month shard gets all writes
- Old shards read-only (can archive)

Benefits:
- Hot shard gets all writes (simple)
- Easy to add new month shard
- Old data can be archived/compressed
```

**Level 4: Vertical Partitioning**:
```
Separate by data type:

Real-time DB (Kafka + Redis):
- Last reading per device (fast lookup)
- TTL: 1 hour

Historical DB (InfluxDB):
- All historical data
- Optimized for analytics queries

Benefits:
- Different optimization strategies
- Fast real-time lookup
- Efficient historical analytics
```

**Results**:
```
Throughput: 100K writes/second sustained
Latency: <10ms for device (Kafka ack)
Storage: Time-series DB optimized (compression)
Queries: Fast on historical data (indexed by time, device_id)

Failure handling:
- Kafka retains data for replay
- Worker failures → other workers pick up
- Database temporary downtime → Kafka buffers
```

---

## Monitoring & Alerting

### Key Metrics:

**Write Throughput**:
```
- Writes per second
- Peak vs average
- By shard (detect hotspots)
```

**Queue Metrics**:
```
- Queue depth (how far behind)
- Consumer lag (Kafka)
- Enqueue/dequeue rates
```

**Database**:
```
- Write latency (p50, p95, p99)
- Transaction rate
- Lock contention
- Disk I/O utilization
```

**Sharding**:
```
- Distribution across shards (evenness)
- Hot shards (load imbalance)
- Cross-shard query rate
```

**Alerts**:
```
- Queue depth > 10,000 → Consumers falling behind
- Write latency p99 > 1 second → Database overloaded
- Shard imbalance > 2x → Hotspot detected
- Disk I/O > 80% → Near capacity
```

---

## Interview Tips

### Always Mention
1. **Start simple**: "Single database until you hit limits"
2. **Numbers**: "Single DB: ~10K writes/sec, need sharding at 50K+"
3. **Partition key**: Critical decision, discuss trade-offs
4. **Avoid distributed transactions**: Design to minimize cross-shard

### Deep Dive Topics
- **Partition key selection**: Hash vs Range vs Geographic
- **Resharding strategy**: Consistent hashing, virtual shards
- **Hotspot handling**: Detect and mitigate
- **Cross-shard queries**: Scatter-gather, search indexes

### Red Flags to Avoid
- Premature sharding (before hitting limits)
- Bad partition key (hotspots, breaks queries)
- Not discussing resharding strategy
- Ignoring cross-shard query complexity

### Good Answers Include
- "Single DB handles 10K writes/sec, currently at 2K, don't need sharding yet"
- "Shard by user_id for even distribution and single-shard user queries"
- "Use Kafka queue to buffer burst traffic, protect database"
- "Batch writes (1000/batch) to increase throughput 10x"
- "Monitor shard distribution, rebalance hot shards"
- "Use consistent hashing for easier resharding later"

---

## Example Use Cases Summary

| Use Case | Writes/Sec | Solution | Key Technique |
|----------|------------|----------|---------------|
| IoT Sensors | 100K | Queue + Batch + Time-sharding | Kafka buffering + TimescaleDB |
| Social Media Posts | 50K | Sharding by user_id | Hash-based sharding |
| Chat Messages | 200K | Sharding + Queue | Geographic shards + Kafka |
| Analytics Events | 500K | Queue + Batch | Kafka + batch writes |
| Financial Transactions | 10K | Careful sharding + Sync replication | Avoid distributed transactions |
| Logs | 1M | Queue + Time-sharding | Kafka + Elasticsearch |
