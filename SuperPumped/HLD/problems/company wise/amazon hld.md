
## Worldwide Capacity Planning (WWCP) - Vibe Team

---

## 📅 Interview Timeline

**Target Role**: SDE-2, Amazon Hyderabad  
**Team**: Worldwide Capacity Planning (WWCP)  
**Product**: Vibe - Workforce Management Platform  
**Problems to Complete**: 7

---

## 🎯 Strategic Problem Selection

These 7 problems are selected based on:

1. ✅ Amazon's most recent SDE-2 interview questions (2024-2025)
2. ✅ Maximum coverage of HLD concepts
3. ✅ Direct alignment with WWCP/Vibe JD requirements
4. ✅ Each problem covers a different pattern/concept

---

## 📝 Problems Checklist

### Problem 1: Design a Job Scheduler (Hard)

- [ ] **Problem Solved**
- [ ] **Reviewed Trade-offs**
- [ ] **Mock Interview Practice**
- [ ] **Whiteboard Sketched**

**Source**: Hello Interview (Hard)

#### Why This Problem?

- **Direct JD Alignment**: Your team builds scheduling systems for thousands of associates across hundreds of sites
- **Real-world Application**: Generate work schedules, handle real-time adjustments
- **Amazon Frequency**: ⭐⭐⭐⭐⭐ (Very Common)

#### Key Concepts to Cover

- [ ] Distributed task scheduling
- [ ] Priority queues & task allocation
- [ ] Worker pool management
- [ ] Task retry & failure handling
- [ ] Distributed locking mechanisms
- [ ] State management & persistence
- [ ] Monitoring & alerting

#### Critical Trade-offs

##### 1. **Push vs Pull Model**

|Aspect|Push Model|Pull Model|
|---|---|---|
|**Design**|Scheduler pushes tasks to workers|Workers pull tasks from queue|
|**Pros**|Lower latency, immediate execution|Better fault tolerance, self-balancing|
|**Cons**|Needs worker state tracking|Slight delay, polling overhead|
|**Use When**|Low worker count, urgent tasks|High worker count, varying capacity|

##### 2. **Centralized vs Distributed Scheduler**

|Aspect|Centralized|Distributed|
|---|---|---|
|**Coordination**|Single coordinator|Multiple coordinators|
|**Complexity**|Simpler|Complex (consensus needed)|
|**Scalability**|Limited by single node|Highly scalable|
|**SPOF**|Yes (needs hot standby)|No|
|**Use When**|Small-medium scale|Large scale, high availability|

##### 3. **Database Choice for Task Queue**

|Database|Pros|Cons|Use Case|
|---|---|---|---|
|**Redis**|Very fast, pub-sub support|Limited persistence, memory-bound|Short-lived tasks, caching|
|**PostgreSQL**|ACID, complex queries, reliable|Slower than NoSQL|Critical tasks, need transactions|
|**DynamoDB**|Scalable, low latency, managed|No complex queries|High throughput, simple queries|
|**Kafka**|High throughput, replay capability|Complex setup, overkill for simple tasks|Event streaming, audit logs|

##### 4. **Task Priority Handling**

- **Simple Priority Queue**: Fast but can starve low-priority tasks
- **Weighted Fair Queuing**: Balanced but complex
- **Deadline-based**: Good for SLA but needs careful tuning
- **Multi-queue with Priority Levels**: Simple to implement, predictable

##### 5. **Failure Handling Strategies**

|Strategy|Latency Impact|Resource Usage|Reliability|
|---|---|---|---|
|**Immediate Retry**|Low|High (thundering herd risk)|Medium|
|**Exponential Backoff**|Medium|Medium|High|
|**Dead Letter Queue**|N/A|Low|High (with manual intervention)|
|**Circuit Breaker**|Variable|Low|High|

##### 6. **Consistency vs Availability**

- **Strong Consistency**: Guarantees no duplicate execution, slower
- **Eventual Consistency**: Faster, risk of duplicate execution (need idempotency)
- **At-least-once delivery**: Simple, requires idempotent tasks
- **Exactly-once delivery**: Complex, expensive, rarely truly needed

#### Amazon-Specific Considerations

- Use **SQS** for task queue (standard vs FIFO trade-offs)
- **Lambda** for worker execution vs EC2 for long-running tasks
- **Step Functions** for complex workflows
- **DynamoDB** for state management
- **CloudWatch** for monitoring and auto-scaling triggers

#### JD Connection

> "Generate work schedules for thousands of associates across hundreds of sites, and monitor/mitigate Service Level (SL) risks in real-time"

---

### Problem 2: Design a Rate Limiter (Medium)

- [x] **Problem Solved**
- [x] **Reviewed Trade-offs**
- [x] **Mock Interview Practice**
- [x] **Whiteboard Sketched**

**Source**: Hello Interview (Medium)

#### Why This Problem?

- **Essential Pattern**: Rate limiting is in every large-scale system
- **JD Connection**: Prevent system overload, manage capacity constraints
- **Amazon Frequency**: ⭐⭐⭐⭐⭐ (Extremely Common)

#### Key Concepts to Cover

- [x] Token bucket algorithm
- [x] Leaky bucket algorithm
- [x] Fixed window counter
- [x] Sliding window log
- [x] Sliding window counter
- [x] Distributed rate limiting
- [x] Redis integration

#### Critical Trade-offs

##### 1. **Rate Limiting Algorithms**

|Algorithm|Memory|Accuracy|Burst Handling|Complexity|
|---|---|---|---|---|
|**Token Bucket**|O(1) per user|High|Allows controlled bursts|Low|
|**Leaky Bucket**|O(1) per user|High|Smooths traffic|Low|
|**Fixed Window**|O(1) per user|Low (boundary issues)|No|Very Low|
|**Sliding Window Log**|O(N) (N=requests)|Very High|Yes|High|
|**Sliding Window Counter**|O(1) per user|High|Yes|Medium|

**Recommendation for Amazon scale**: Token Bucket or Sliding Window Counter

##### 2. **Storage Location**

|Location|Latency|Consistency|Cost|Use Case|
|---|---|---|---|---|
|**In-Memory (local)**|<1ms|No (per instance)|Free|Single server|
|**Redis (centralized)**|1-5ms|Yes|Medium|Distributed, strict limits|
|**Redis Cluster**|2-10ms|Yes|High|Very high scale|
|**DynamoDB**|5-20ms|Yes|Medium|AWS native, serverless|

##### 3. **Single vs Multi-tier Rate Limiting**

- **Single Tier**: Simpler, one limit per user/IP
- **Multi-tier**: User + IP + API endpoint + Global limits (more robust)
- **Trade-off**: Complexity vs granular control

##### 4. **Synchronous vs Asynchronous Enforcement**

|Approach|Latency|Accuracy|UX Impact|
|---|---|---|---|
|**Synchronous**|Adds to request time|100% accurate|Immediate feedback|
|**Asynchronous**|No impact|May have slight delay|Better performance|

##### 5. **Rate Limit Exceeded Response**

- **HTTP 429 (Too Many Requests)**: Standard, with Retry-After header
- **Queueing**: Better UX but adds complexity
- **Degraded Service**: Serve cached/stale data
- **Dynamic Pricing**: Charge more for burst capacity (business decision)

##### 6. **Distributed Counter Consistency**

|Approach|Accuracy|Performance|Complexity|
|---|---|---|---|
|**Exact Count (locks)**|100%|Slow (contention)|High|
|**Eventual Consistency**|~95-99%|Fast|Medium|
|**Loose Counting**|~90%|Very Fast|Low|

**Recommendation**: Use eventual consistency with slight over-provisioning (set limit to 90% of actual capacity)

#### Implementation Strategies

##### Option 1: Redis with Sorted Sets (Sliding Window Log)

```
Pros: Accurate, handles bursts well
Cons: Memory intensive for high traffic
Best for: Medium scale, need accuracy
```

##### Option 2: Redis with Token Bucket

```
Pros: Efficient, handles bursts, low memory
Cons: Requires atomic operations
Best for: High scale, allow controlled bursts
```

##### Option 3: DynamoDB with Conditional Writes

```
Pros: Serverless, scalable, AWS native
Cons: Higher latency than Redis
Best for: AWS-heavy, serverless architecture
```

#### Amazon-Specific Considerations

- **API Gateway**: Built-in rate limiting (simpler for API endpoints)
- **WAF**: For IP-based rate limiting
- **ElastiCache (Redis)**: For custom logic
- **Lambda@Edge**: For CDN-level rate limiting

---

### Problem 3: Design Ad Click Aggregator (Hard)

- [ ] **Problem Solved**
- [ ] **Reviewed Trade-offs**
- [ ] **Mock Interview Practice**
- [ ] **Whiteboard Sketched**

**Source**: Hello Interview (Hard)

#### Why This Problem?

- **Direct JD Alignment**: Real-time analytics, handling millions of events (like forecast generation)
- **Hot Topic**: Stream processing heavily tested at Amazon
- **Amazon Frequency**: ⭐⭐⭐⭐ (Very Common)

#### Key Concepts to Cover

- [ ] Stream processing (Kafka, Kinesis)
- [ ] Time-series data management
- [ ] Aggregation pipelines
- [ ] Real-time analytics
- [ ] Lambda architecture (batch + stream)
- [ ] Kappa architecture (stream only)
- [ ] Data deduplication
- [ ] Late-arriving data handling

#### Critical Trade-offs

##### 1. **Lambda vs Kappa Architecture**

|Architecture|Complexity|Latency|Accuracy|Reprocessing|
|---|---|---|---|---|
|**Lambda**|High (2 pipelines)|Batch: High, Stream: Low|High (batch corrects)|Easy|
|**Kappa**|Medium (1 pipeline)|Low|Medium|Hard (full replay)|

**Lambda**: Batch layer (historical accuracy) + Speed layer (real-time)  
**Kappa**: Stream-only with replayable log

**Recommendation**: Lambda for ad aggregation (accuracy matters for billing)

##### 2. **Aggregation Window Strategies**

|Window Type|Use Case|Complexity|Accuracy|
|---|---|---|---|
|**Tumbling**|Fixed intervals (every 5 min)|Low|High|
|**Sliding**|Overlapping windows|Medium|High|
|**Session**|User activity bursts|High|Variable|
|**Global**|All-time aggregates|Low|High|

**Tumbling Windows**: Best for ad clicks (hourly, daily reports)

##### 3. **Late Data Handling**

|Strategy|Accuracy|Complexity|Resource Usage|
|---|---|---|---|
|**Drop**|Low|Very Low|Low|
|**Watermark + Grace Period**|High|Medium|Medium|
|**Reprocess**|Perfect|High|High|

**Recommendation**: Watermark with 10-15 min grace period

##### 4. **Storage for Time-Series Data**

|Database|Write Speed|Query Speed|Cost|Best For|
|---|---|---|---|---|
|**ClickHouse**|Very High|Very High|Medium|OLAP, analytics|
|**TimescaleDB**|High|High|Low|Time-series + SQL|
|**DynamoDB**|Very High|Medium (with indexes)|Medium|AWS native, key-value|
|**Redshift**|Medium|Very High (OLAP)|High|Data warehouse|
|**Druid**|Very High|Very High|High|Real-time analytics|

**Recommendation for Amazon**: DynamoDB for hot data, S3 + Athena/Redshift for cold data

##### 5. **Exactly-Once vs At-Least-Once Processing**

|Guarantee|Complexity|Performance|Risk|
|---|---|---|---|
|**At-most-once**|Low|Highest|Data loss|
|**At-least-once**|Medium|High|Duplicates|
|**Exactly-once**|Very High|Lower|None (but expensive)|

**Recommendation**: At-least-once + idempotent aggregation (use deduplication keys)

##### 6. **Aggregation Strategy**

|Level|Latency|Accuracy|Query Performance|
|---|---|---|---|
|**Pre-aggregate at ingestion**|Lowest|Medium|Highest|
|**Micro-batching (5s-1min)**|Low|High|High|
|**On-demand aggregation**|High|Highest|Lowest|
|**Hybrid (pre-agg + refinement)**|Medium|High|High|

**Recommendation**: Micro-batching with pre-aggregation for common queries

##### 7. **Partitioning Strategy**

- **By Time**: Good for range queries, natural data lifecycle
- **By Ad ID**: Good for per-ad queries, but can create hot partitions
- **By User ID**: Good for user analytics
- **Composite (Time + Ad ID)**: Best balance for ad click aggregator

##### 8. **Hot vs Cold Data Tiering**

|Tier|Storage|Query Latency|Cost|Data Age|
|---|---|---|---|---|
|**Hot**|In-memory/SSD|<10ms|Very High|Last hour|
|**Warm**|SSD|10-100ms|High|Last 24h|
|**Cold**|HDD/S3|100ms-1s|Low|>24h|
|**Archive**|Glacier|Minutes|Very Low|>90 days|

#### Stream Processing Trade-offs

##### Kafka vs Kinesis vs SQS

|Feature|Kafka|Kinesis|SQS|
|---|---|---|---|
|**Throughput**|Very High|High|Medium|
|**Ordering**|Per partition|Per shard|FIFO queues only|
|**Retention**|Configurable (unlimited)|365 days max|14 days|
|**Replay**|Yes|Yes|No|
|**Cost**|Medium (self-managed)|High|Low|
|**Complexity**|High|Low (managed)|Very Low|

**Recommendation for Amazon**: Kinesis (AWS native, managed, good balance)

#### Data Deduplication Strategies

1. **Bloom Filter**: Fast, space-efficient, but probabilistic
2. **Redis Set**: Exact, but memory intensive
3. **Database Unique Constraint**: Exact, but slower
4. **Request ID + Time Window**: Balance of accuracy and performance

#### Amazon-Specific Considerations

- **Kinesis Data Streams**: For ingestion
- **Kinesis Data Analytics**: For real-time processing
- **Lambda**: For lightweight transformations
- **EMR/Spark**: For complex aggregations
- **DynamoDB Streams**: For change data capture
- **S3**: For long-term storage
- **Athena**: For ad-hoc queries on S3
- **QuickSight**: For dashboards

#### JD Connection

> "Systems generate millions of weekly forecasts" and "monitor/mitigate Service Level (SL) risks in real-time"

---

### Problem 4: Design Uber (Hard)

- [ ] **Problem Solved**
- [ ] **Reviewed Trade-offs**
- [ ] **Mock Interview Practice**
- [ ] **Whiteboard Sketched**

**Source**: Hello Interview (Hard)

#### Why This Problem?

- **JD Connection**: Real-time matching/optimization (associate-to-shift assignment under volatility)
- **Comprehensive**: Covers multiple system design patterns
- **Amazon Frequency**: ⭐⭐⭐⭐ (Common - geospatial problems)

#### Key Concepts to Cover

- [ ] Geospatial indexing & search
- [ ] Real-time location tracking
- [ ] WebSockets / Server-Sent Events
- [ ] Matching algorithms
- [ ] Distributed transactions
- [ ] Payment processing
- [ ] Notification systems
- [ ] Maps integration
- [ ] Surge pricing

#### Critical Trade-offs

##### 1. **Geospatial Data Structure**

|Structure|Query Speed|Update Speed|Memory|Accuracy|
|---|---|---|---|---|
|**Grid/Geohash**|Fast|Fast|Low|Medium (fixed grid)|
|**QuadTree**|Very Fast|Medium|Medium|High (adaptive)|
|**R-Tree**|Fast|Slow|Medium|Very High|
|**S2 Geometry**|Very Fast|Fast|Medium|Very High (Google's lib)|

**Recommendation**: Geohash for simplicity, S2 for production (what Google/Uber use)

##### 2. **Location Update Frequency**

|Frequency|Accuracy|Battery Drain|Server Load|Use Case|
|---|---|---|---|---|
|**Every 1s**|Perfect|Very High|Very High|Active ride only|
|**Every 5s**|High|High|High|Nearby drivers|
|**Every 30s**|Medium|Medium|Medium|Idle drivers|
|**Event-driven**|Variable|Low|Low|Significant movements only|

**Recommendation**: Adaptive - frequent during ride, less when idle

##### 3. **Matching Algorithm Strategy**

|Strategy|Speed|Fairness|Driver Utilization|
|---|---|---|---|
|**Nearest Driver**|Fastest|Low|Low (drivers cluster)|
|**Best ETA**|Fast|Medium|Medium|
|**Global Optimization**|Slow|High|High (but complex)|
|**Auction-based**|Medium|High|Very High|

**Recommendation**: Best ETA with timeout fallback to nearest

##### 4. **Communication Protocol**

|Protocol|Real-time|Overhead|Mobile Battery|Use Case|
|---|---|---|---|---|
|**Polling**|No|Very High|Very High|Don't use|
|**Long Polling**|Nearly|High|High|Fallback only|
|**WebSocket**|Yes|Low|Medium|Active trip tracking|
|**SSE**|Yes|Low|Low|One-way updates|
|**Push Notifications**|Nearly|Very Low|Low|Occasional updates|

**Recommendation**: WebSocket for active rides, SSE for location updates, Push for arrival notifications

##### 5. **Consistency Model for Ride State**

|Model|Complexity|Race Conditions|User Experience|
|---|---|---|---|
|**Strong Consistency**|High|None|Can have delays|
|**Eventual Consistency**|Low|Possible|Fast but risky|
|**Leader-based (Paxos/Raft)**|Very High|None|Slow|
|**Optimistic Locking**|Medium|Detects conflicts|Fast, handles races|

**Recommendation**: Optimistic locking with version numbers (prevent double-booking)

##### 6. **Database Choice**

|Database|Geospatial|Transactions|Scalability|Use Case|
|---|---|---|---|---|
|**PostgreSQL + PostGIS**|Excellent|ACID|Medium|Ride state, payments|
|**MongoDB**|Good|Limited|High|Driver locations|
|**Redis + GeoHash**|Good|No|Very High|Real-time matching|
|**DynamoDB**|No (manual)|Limited|Very High|User data|
|**Cassandra**|No|No|Extreme|High write loads|

**Recommendation**: PostgreSQL for rides/payments, Redis for matching, MongoDB for locations

##### 7. **Payment Processing**

|Approach|Latency|Reliability|Cost|Complexity|
|---|---|---|---|---|
|**Synchronous**|High|Low (blocks on failure)|Medium|Low|
|**Asynchronous (queue)**|Low|High|Medium|Medium|
|**Two-phase Commit**|Very High|High|High|Very High|
|**Saga Pattern**|Medium|High|Medium|High|

**Recommendation**: Saga pattern (async with compensating transactions)

##### 8. **Surge Pricing Calculation**

|Method|Accuracy|Latency|Fairness|
|---|---|---|---|
|**Real-time ML Model**|High|Medium|Variable|
|**Rule-based (supply/demand ratio)**|Medium|Very Low|High|
|**Historical + Real-time**|Very High|Low|High|
|**Auction-based**|Perfect|High|Variable|

**Recommendation**: Historical baseline + real-time adjustment

##### 9. **Map Service Integration**

|Service|Cost|Accuracy|Features|Mobile SDK|
|---|---|---|---|---|
|**Google Maps**|High|Excellent|Rich|Yes|
|**Mapbox**|Medium|Excellent|Customizable|Yes|
|**OpenStreetMap**|Free|Good|Limited|Yes|
|**HERE Maps**|Medium|Excellent|Good|Yes|

**Recommendation**: Mapbox (cost-effective, customizable)

##### 10. **Scaling Strategy**

- **Geo-sharding**: Partition by city/region (most data is geographically localized)
- **Service Sharding**: Separate services (matching, payments, notifications)
- **Read Replicas**: For driver locations and map data
- **Caching**: Heavily cache map tiles, static content

#### Advanced Considerations

##### Fraud Detection

- **Real-time**: Rules engine (if location jumping impossible, flag it)
- **Batch**: ML models on historical patterns
- **Trade-off**: False positives vs fraud loss

##### ETA Accuracy

- **Simple**: Distance / average speed
- **Medium**: Historical traffic patterns
- **Advanced**: ML model with real-time traffic data
- **Trade-off**: Accuracy vs computation cost

##### Driver-Rider Matching Race Conditions

**Problem**: Multiple riders requesting same driver **Solution**:

1. **Optimistic Locking**: Driver has a version number, first successful update wins
2. **Queue-based**: Driver picks from queue (slower but fair)
3. **Reservation System**: Driver temporarily locked during offer (5-10s)

**Recommendation**: Optimistic locking with 10-second reservation window

#### Amazon-Specific Considerations

- **AppSync**: For real-time GraphQL subscriptions
- **IoT Core**: For high-volume location updates
- **Location Service**: AWS native geospatial service
- **DynamoDB**: For user/driver profiles
- **RDS Aurora**: For ride transactions
- **ElastiCache**: For real-time matching cache
- **SNS/SQS**: For notifications and async processing
- **Step Functions**: For ride state machine orchestration

#### JD Connection

> "Adapt to demand and supply volatility" and "optimize CS workforce for customer experience"

---

### Problem 5: Design a Distributed Cache (Hard)

- [ ] **Problem Solved**
- [ ] **Reviewed Trade-offs**
- [ ] **Mock Interview Practice**
- [ ] **Whiteboard Sketched**

**Source**: Hello Interview (Hard)

#### Why This Problem?

- **Fundamental**: Used in virtually every large-scale system
- **JD Connection**: Low-latency data access for forecasting systems
- **Amazon Frequency**: ⭐⭐⭐⭐⭐ (Extremely Common - Asked across all levels)

#### Key Concepts to Cover

- [ ] Cache eviction policies (LRU, LFU, FIFO)
- [ ] Consistent hashing
- [ ] Cache sharding strategies
- [ ] Replication & high availability
- [ ] Cache invalidation strategies
- [ ] Write-through vs Write-back
- [ ] Cache stampede prevention
- [ ] Hot key problem

#### Critical Trade-offs

##### 1. **Eviction Policies**

|Policy|Hit Rate|Implementation|Memory Efficiency|Use Case|
|---|---|---|---|---|
|**LRU**|High|Medium (doubly-linked list + hash)|Good|General purpose|
|**LFU**|Very High|Complex (min-heap)|Medium|Predictable access patterns|
|**FIFO**|Low|Simple (queue)|Good|Time-based data|
|**Random**|Low|Very Simple|Good|Evenly distributed access|
|**TTL-based**|Variable|Simple|Excellent|Time-sensitive data|
|**ARC (Adaptive)**|Very High|Very Complex|Good|Variable workloads|

**Recommendation**: LRU for most cases, LFU for hot data workloads

##### 2. **Write Strategies**

|Strategy|Consistency|Write Latency|Read Latency|Complexity|
|---|---|---|---|---|
|**Write-through**|Strong|High|Low|Low|
|**Write-back**|Eventual|Low|Low|High (data loss risk)|
|**Write-around**|Eventual|Medium|High (first read)|Low|
|**Refresh-ahead**|Eventual|Low|Very Low|High|

**Write-through**: Write to cache AND database synchronously  
**Write-back**: Write to cache, async write to database  
**Write-around**: Write to database only, read populates cache

**Recommendation**: Write-through for critical data, write-back for high throughput

##### 3. **Cache Invalidation Strategies**

|Strategy|Consistency|Complexity|Scalability|Staleness Risk|
|---|---|---|---|---|
|**TTL**|Weak|Very Low|Excellent|High (time-based)|
|**Event-driven**|Strong|Medium|Good|Low|
|**Write-through**|Strong|Low|Good|None|
|**Polling**|Medium|Low|Poor|Medium|
|**Version-based**|Strong|Medium|Excellent|Low|

**Famous Quote**: _"There are only two hard things in Computer Science: cache invalidation and naming things."_

**Recommendation**: TTL + Event-driven for critical updates

##### 4. **Sharding Strategy**

|Strategy|Load Distribution|Hot Spot Risk|Resharding Ease|Use Case|
|---|---|---|---|---|
|**Hash-based**|Even|Low|Hard (full reshard)|Static cluster|
|**Consistent Hashing**|Good|Medium|Easy (minimal movement)|Dynamic cluster|
|**Range-based**|Uneven|High|Easy|Ordered data|
|**Virtual Nodes**|Excellent|Very Low|Very Easy|Production systems|

**Consistent Hashing Details**:

- Add/remove nodes: Only K/N keys need to move (K=total keys, N=nodes)
- Virtual nodes (vnodes): Each physical node has 100-500 virtual nodes for better distribution
- **Recommendation**: Consistent hashing with 150-200 vnodes per node

##### 5. **Replication Strategy**

|Strategy|Availability|Consistency|Write Latency|Read Latency|
|---|---|---|---|---|
|**No Replication**|Low|Strong|Low|Low|
|**Master-Slave (async)**|Medium|Eventual|Low|Low|
|**Master-Slave (sync)**|Medium|Strong|High|Low|
|**Master-Master**|High|Eventual (conflict risk)|Medium|Low|
|**Multi-Master (Raft)**|Very High|Strong|High|Low|

**Recommendation**: Async master-slave with 2 replicas (balance of availability and cost)

##### 6. **Consistency Models**

|Model|Guarantees|Performance|Use Case|
|---|---|---|---|
|**Strong**|Always latest value|Slow|Financial data|
|**Eventual**|Eventually consistent|Fast|Social media feeds|
|**Read-after-write**|Your writes visible to you|Medium|User profiles|
|**Monotonic Reads**|No backward time travel|Medium|Session data|
|**Causal**|Related events ordered|Medium|Comment threads|

**Recommendation for Cache**: Eventual consistency (cache is secondary storage)

##### 7. **Handling Cache Failures**

##### Cache Stampede (Thundering Herd)

**Problem**: Cache expires, thousands of requests hit database simultaneously

**Solutions**:

|Solution|Effectiveness|Complexity|Latency Impact|
|---|---|---|---|
|**Lock-based**|High|Medium|High (queuing)|
|**Probabilistic Early Expiration**|Medium|Low|Low|
|**Always Async Refresh**|High|High|None|
|**Request Coalescing**|Very High|Medium|Low|

**Recommendation**: Probabilistic early expiration + request coalescing

```
# Probabilistic Early Expiration
expiry_time = now + ttl * (1 - beta * random(0,1))
beta = 1 means up to 100% early refresh
```

##### 8. **Hot Key Problem**

**Problem**: One key gets 90% of traffic (e.g., celebrity tweet)

**Solutions**:

|Solution|Effectiveness|Complexity|Cost|
|---|---|---|---|
|**Local Caching**|High|Low|Low (memory)|
|**Replication**|Very High|Medium|High (bandwidth)|
|**Load Balancing**|Medium|Medium|Medium|
|**Rate Limiting**|High|Low|Low (may impact UX)|

**Recommendation**: Local cache + replication for top 0.1% keys

##### 9. **Memory Management**

|Approach|Memory Efficiency|Performance|Complexity|
|---|---|---|---|
|**Fixed Size Cache**|Low|High|Very Low|
|**Dynamic Sizing**|High|Medium|Medium|
|**Compression**|Very High|Low (CPU cost)|Medium|
|**Tiered Storage**|High|Medium|High|

**Recommendation**: Fixed size with LRU (simplicity wins)

##### 10. **Cold Start Problem**

**Problem**: Empty cache on restart = all requests hit database

**Solutions**:

1. **Cache Warming**: Pre-populate cache before serving traffic
2. **Gradual Traffic Ramp**: Route 10% → 50% → 100% traffic
3. **Persistent Cache**: Write to disk, reload on restart (Redis AOF/RDB)
4. **Layered TTL**: Short TTL initially, increase after warm-up

**Recommendation**: Persistent cache (Redis RDB snapshots every 5 minutes)

#### Data Structures Inside Cache

##### Redis Data Types Trade-offs

|Type|Use Case|Memory|Operations|Complexity|
|---|---|---|---|---|
|**String**|Simple key-value|Low|O(1)|Very Low|
|**Hash**|Objects with fields|Medium|O(1) per field|Low|
|**List**|Queues, timelines|Medium|O(1) at ends, O(N) middle|Low|
|**Set**|Unique items|Medium|O(1) add/remove|Low|
|**Sorted Set**|Leaderboards, time-series|High|O(log N)|Medium|
|**Bitmap**|Boolean flags|Very Low|O(1)|Low|
|**HyperLogLog**|Cardinality estimation|Very Low|O(1)|Low|

#### Advanced: Multi-Level Caching

|Level|Technology|Latency|Size|Hit Rate|Cost|
|---|---|---|---|---|---|
|**L1: Browser**|LocalStorage|<1ms|~10MB|Low|Free|
|**L2: CDN**|CloudFront|10-50ms|~TB|Medium|Low|
|**L3: Application**|In-memory HashMap|<1ms|~GB|Medium|Free (RAM)|
|**L4: Distributed**|Redis/Memcached|1-5ms|~TB|High|Medium|
|**L5: Database**|Query Cache|5-20ms|~GB|N/A|Free|

**Strategy**: Check L1 → L2 → L3 → L4 → Database, populate on way back

#### Metrics to Monitor

- **Hit Rate**: (Cache Hits / Total Requests) * 100
    - **Good**: >80%, **Excellent**: >95%
- **Miss Rate**: 100 - Hit Rate
- **Eviction Rate**: How often are items evicted?
    - **High eviction rate** = Need more cache memory
- **Latency**: p50, p95, p99
- **Memory Usage**: % utilization, evictions due to memory pressure
- **Network**: Bandwidth, packet loss

#### Amazon-Specific Considerations

- **ElastiCache (Redis)**: Managed, cluster mode for sharding
- **ElastiCache (Memcached)**: Simpler, no persistence
- **DynamoDB DAX**: DynamoDB-specific cache (microsecond latency)
- **CloudFront**: CDN for static content
- **API Gateway Caching**: For API responses

#### JD Connection

> "Scale systems to handle massive data" and "optimize for scale, latency, and resource usage"

---

### Problem 6: Design a Web Crawler (Hard)

- [ ] **Problem Solved**
- [ ] **Reviewed Trade-offs**
- [ ] **Mock Interview Practice**
- [ ] **Whiteboard Sketched**

**Source**: Hello Interview (Hard)

#### Why This Problem?

- **Pattern Match**: Distributed data collection and processing
- **Comprehensive**: Covers queuing, deduplication, politeness, scaling
- **Amazon Frequency**: ⭐⭐⭐⭐ (Common for distributed systems roles)

#### Key Concepts to Cover

- [ ] URL frontier (priority queue)
- [ ] Crawling strategies (BFS vs DFS)
- [ ] Robots.txt compliance
- [ ] Politeness policies
- [ ] URL deduplication
- [ ] Content deduplication
- [ ] DNS resolution & caching
- [ ] Distributed crawling
- [ ] Fault tolerance

#### Critical Trade-offs

##### 1. **Crawling Strategy**

|Strategy|Coverage|Speed|Resource Usage|Use Case|
|---|---|---|---|---|
|**BFS (Breadth-First)**|Wide|Medium|High (memory)|Discover new sites|
|**DFS (Depth-First)**|Deep|Fast|Low|Deep site crawling|
|**Priority-based**|Targeted|Variable|Medium|Important pages first|
|**Focused**|Narrow|Fast|Low|Topic-specific|

**Recommendation**: Priority-based BFS (balance of coverage and importance)

##### 2. **URL Frontier Design**

|Design|Politeness|Priority|Complexity|Throughput|
|---|---|---|---|---|
|**Single Queue**|No|No|Very Low|Very High|
|**Per-Host Queue**|Yes|No|Low|High|
|**Priority Queue**|No|Yes|Medium|Medium|
|**Hybrid (Priority + Per-Host)**|Yes|Yes|High|Medium|

**Recommendation**: Hybrid design

```
Front Queues: Priority-based (prioritizer assigns priority)
Back Queues: Per-host queues (ensures politeness)
```

##### 3. **URL Deduplication**

|Method|Accuracy|Memory|Speed|False Positive|
|---|---|---|---|---|
|**HashSet (in-memory)**|100%|Very High|Very Fast|0%|
|**Database**|100%|Low|Slow|0%|
|**Bloom Filter**|~99%|Very Low|Very Fast|<1%|
|**Checksum + DB**|100%|Low|Medium|0%|

**Recommendation**: Bloom filter for fast check + Database for persistence

**Math**: 1 billion URLs, 0.1% false positive rate

- Bloom filter: ~1.2 GB memory
- HashSet: ~40-80 GB memory

##### 4. **Content Deduplication**

|Method|Accuracy|Speed|Use Case|
|---|---|---|---|
|**Full MD5 Hash**|100%|Medium|Exact duplicates|
|**Simhash**|~95%|Fast|Near-duplicates (mirrors)|
|**MinHash**|~90%|Very Fast|Similar content|
|**Shingles**|Variable|Medium|Partial duplicates|

**Recommendation**: Simhash (64-bit fingerprint, Hamming distance for similarity)

##### 5. **DNS Resolution**

|Strategy|Latency|Cost|Reliability|
|---|---|---|---|
|**Per-Request**|High (100-500ms)|Free|High|
|**Local Cache**|Low (<1ms cache hit)|Free|Medium|
|**Dedicated DNS Service**|Very Low|Medium|Very High|

**Recommendation**: Local cache with 5-minute TTL + background refresh

**Optimization**: Batch DNS lookups (resolve multiple domains in parallel)

##### 6. **Politeness Policy**

|Policy|Web Server Load|Crawl Speed|Complexity|
|---|---|---|---|
|**Fixed Delay (1s)**|Low|Slow|Very Low|
|**Adaptive (based on response time)**|Optimal|Fast|Medium|
|**robots.txt**|Low (compliant)|Variable|Low|
|**No Policy**|High (risk of ban)|Very Fast|None|

**Recommendation**: robots.txt + adaptive delay (start at 1s, adjust based on server response)

**Standard**:

- 1 request/second per host minimum
- Respect `Crawl-delay` in robots.txt
- Check `Disallow` rules

##### 7. **Distributed Crawling**

|Approach|Coordination|Efficiency|Fault Tolerance|
|---|---|---|---|
|**Centralized**|Simple|High|Low (SPOF)|
|**Decentralized**|Complex|Medium|High|
|**Hybrid (partitioned)**|Medium|High|High|

**Recommendation**: Hash-based partitioning by hostname

```
crawler_id = hash(hostname) % num_crawlers
Each crawler owns a subset of hosts
```

**Advantage**: No cross-crawler coordination needed, natural deduplication

##### 8. **Storage Strategy**

|Layer|Storage|Purpose|Size|Speed|
|---|---|---|---|---|
|**URL Queue**|Redis|Frontier|~GB|Very Fast|
|**URL Seen**|Bloom Filter + DB|Deduplication|~GB|Fast|
|**Content**|Blob Storage (S3)|Raw HTML|~TB-PB|Medium|
|**Metadata**|Database|URL info, status|~TB|Medium|
|**Indexed Data**|Search Engine (ES)|Searchable|~TB|Fast|

##### 9. **Content Freshness**

|Strategy|Accuracy|Efficiency|Complexity|
|---|---|---|---|
|**Recrawl All Periodically**|Low|Low|Very Low|
|**Recrawl by Change Frequency**|Medium|Medium|Low|
|**Recrawl by Importance**|High|Medium|Medium|
|**ML-based Prediction**|Very High|High|High|

**Recommendation**: Hybrid approach

- Important pages: Daily
- Medium importance: Weekly
- Low importance: Monthly
- Use HTTP `If-Modified-Since` to avoid re-downloading unchanged content

##### 10. **Handling Dynamic Content**

|Content Type|Strategy|Complexity|Resource Cost|
|---|---|---|---|
|**Static HTML**|Simple GET|Very Low|Very Low|
|**JavaScript-rendered**|Headless browser (Puppeteer)|High|Very High|
|**Infinite Scroll**|Scroll simulation|High|High|
|**AJAX**|XHR interception|Medium|Medium|

**Trade-off**: JavaScript rendering is 10-100x slower and more expensive **Recommendation**: Heuristic to detect JS-heavy sites, render only those

#### Advanced: Crawler Architecture Components

##### 1. **URL Frontier**

```
[Prioritizer] → [Front Queues] → [Selector] → [Back Queues] → [Workers]
                     ↓                              ↓
                  Priority                     Per-Host
```

##### 2. **Robots.txt Cache**

- Cache for 24 hours
- Parallel fetch during initial crawl
- Respect `Crawl-delay` directive

##### 3. **URL Normalization**

Before deduplication, normalize URLs:

- Convert to lowercase: `HTTP` → `http`
- Remove trailing slash: `/page/` → `/page`
- Sort query parameters: `?b=2&a=1` → `?a=1&b=2`
- Remove fragments: `#section` → ``
- Remove default ports: `:80`, `:443`

##### 4. **Error Handling**

|Error Type|Retry Strategy|Impact|
|---|---|---|
|**DNS Failure**|Retry 3 times, exponential backoff|Skip host temporarily|
|**Connection Timeout**|Retry 2 times|Mark URL as slow|
|**404 Not Found**|Don't retry|Mark URL as dead|
|**500 Server Error**|Retry after 1 hour|Temporary issue|
|**429 Rate Limited**|Exponential backoff|Respect server|
|**Malformed HTML**|Try to parse anyway|Log for analysis|

#### Scalability Considerations

##### Throughput Calculations

**Assumption**: Crawl 1 billion pages per month

```
1 billion pages / 30 days = 33M pages/day
33M pages / 86,400 seconds = ~385 pages/second
```

**With 1 second politeness per host**:

- Need at least 385 different hosts being crawled simultaneously
- With 10 threads per machine: ~40 crawler machines
- With overhead and retries: ~60-80 machines

##### Network Bandwidth

**Assumption**: Average page size 100KB

```
385 pages/s * 100KB = 38.5 MB/s = ~308 Mbps
```

##### Storage

```
1 billion pages * 100KB = 100TB raw HTML
Compressed (gzip, ~5x): ~20TB
Indexed: ~50TB (with metadata and indexes)
```

#### Amazon-Specific Considerations

- **EC2 Auto Scaling**: Scale crawler fleet based on queue depth
- **SQS**: For URL frontier (distributed queue)
- **S3**: For storing crawled content
- **DynamoDB**: For URL deduplication tracking
- **Lambda**: For lightweight HTML parsing
- **CloudWatch**: For monitoring crawl rate, errors
- **Step Functions**: For orchestrating crawl workflows

#### JD Connection

> "Build systems that adapt to demand" and "massive scale"

---

### Problem 7: Design YouTube / Video Streaming Platform (Hard)

- [ ] **Problem Solved**
- [ ] **Reviewed Trade-offs**
- [ ] **Mock Interview Practice**
- [ ] **Whiteboard Sketched**

**Source**: Hello Interview (Hard)

#### Why This Problem?

- **Comprehensive System**: Covers CDN, encoding, storage, recommendations, real-time updates
- **JD Connection**: High-throughput data processing and serving
- **Amazon Loves This**: Prime Video questions are common (domain expertise)
- **Amazon Frequency**: ⭐⭐⭐⭐⭐ (Very Common - Tests multiple concepts)

#### Key Concepts to Cover

- [ ] Video upload & processing pipeline
- [ ] Adaptive bitrate streaming (ABR)
- [ ] Content Delivery Network (CDN)
- [ ] Video encoding & transcoding
- [ ] Storage optimization
- [ ] Recommendation system
- [ ] Real-time view counter
- [ ] Comment system
- [ ] Search & discovery

#### Critical Trade-offs

##### 1. **Video Upload Strategy**

|Strategy|UX|Reliability|Complexity|Cost|
|---|---|---|---|---|
|**Single POST**|Poor (long wait)|Low (network fails)|Very Low|Low|
|**Chunked Upload**|Good (progress bar)|High (resume-able)|Medium|Low|
|**Multipart Upload**|Good|Very High|Medium|Medium|
|**Resumable (Tus Protocol)**|Excellent|Very High|High|Medium|

**Recommendation**: Chunked multipart upload (balance of UX and complexity)

**Details**:

- 5-10 MB chunks
- Upload chunks in parallel (5-10 at a time)
- S3 Multipart Upload API: Handle 5GB-5TB files
- Store chunk checksums for validation

##### 2. **Video Processing Pipeline**

|Approach|Latency|Cost|Scalability|Flexibility|
|---|---|---|---|---|
|**Synchronous**|High (blocks user)|Medium|Low|Low|
|**Async (Queue)**|Low|Medium|High|Medium|
|**Streaming Pipeline**|Very Low (progressive)|High|Very High|High|
|**Hybrid**|Medium|Medium|High|High|

**Recommendation**: Async queue-based pipeline

**Pipeline Stages**:

```
Upload → Queue → Transcoding → Quality Check → CDN Distribution → Notification
```

##### 3. **Video Encoding**

|Codec|Quality|Compression|CPU Cost|Browser Support|
|---|---|---|---|---|
|**H.264 (AVC)**|Good|Good|Medium|Universal|
|**H.265 (HEVC)**|Excellent|Excellent|Very High|Limited|
|**VP9**|Excellent|Excellent|High|Good|
|**AV1**|Best|Best|Extreme|Emerging|

**Recommendation**: H.264 (universal support) + VP9 (for bandwidth-constrained)

**Multiple Resolutions**:

- 4K: 3840x2160 (20-40 Mbps)
- 1080p: 1920x1080 (5-8 Mbps)
- 720p: 1280x720 (2.5-5 Mbps)
- 480p: 854x480 (1-1.5 Mbps)
- 360p: 640x360 (0.5-1 Mbps)

##### 4. **Adaptive Bitrate Streaming (ABR)**

|Protocol|Latency|Compatibility|Features|Use Case|
|---|---|---|---|---|
|**HLS (Apple)**|Medium (6-30s)|Excellent|Good|Mobile, Safari|
|**DASH**|Low (2-10s)|Good|Excellent|Modern browsers|
|**Smooth Streaming (MS)**|Medium|Good|Medium|Legacy Windows|
|**WebRTC**|Very Low (<1s)|Good|Limited|Live streaming|

**Recommendation**: HLS for VOD (Video on Demand), WebRTC for live streaming

**HLS Details**:

- Master playlist (.m3u8) lists all quality variants
- Each variant has its own playlist
- Client auto-switches based on bandwidth
- Typical segment length: 6-10 seconds

##### 5. **Storage Strategy**

|Tier|Storage Type|Cost/GB|Latency|Use Case|
|---|---|---|---|---|
|**Hot**|SSD (S3 Standard)|$0.023|10-50ms|Recent/popular videos|
|**Warm**|S3 Intelligent-Tiering|$0.023-0.0125|10-50ms|Auto-optimization|
|**Cold**|S3 Glacier Instant Retrieval|$0.004|10-50ms|Old but accessible|
|**Archive**|S3 Glacier Deep Archive|$0.00099|12 hours|Rarely accessed|

**Recommendation**: S3 Intelligent-Tiering (automatic optimization based on access patterns)

**Storage Optimization**:

- Delete duplicate uploads (content-based deduplication using MD5/SHA256)
- Store only popular resolutions for old videos (e.g., drop 4K after 1 year)
- Lazy encoding: Generate high-res only when requested

##### 6. **CDN Strategy**

|Approach|Latency|Cost|Cache Hit Rate|Global Reach|
|---|---|---|---|---|
|**Single Origin**|High (far users)|Low|N/A|Poor|
|**CDN (CloudFront)**|Low|Medium|High (80-90%)|Excellent|
|**Multi-CDN**|Very Low|High|Very High|Best|
|**Origin Shield**|Low|Medium|Very High|Excellent|

**Recommendation**: CDN with Origin Shield

**CDN Benefits**:

- Reduced origin load: 80-95% requests served from edge
- Lower latency: ~50-200ms → ~10-50ms
- Better availability: CDN absorbs traffic spikes

**Origin Shield**: Extra cache layer between CDN edge and origin

- Further reduces origin load (95%+ edge hit rate)
- Cost: +10-20% but saves origin bandwidth

##### 7. **Video Recommendation System**

|Approach|Accuracy|Latency|Cost|Freshness|
|---|---|---|---|---|
|**Collaborative Filtering**|High|Low|Medium|Medium|
|**Content-based**|Medium|Low|Low|High|
|**Deep Learning (DNN)**|Very High|Medium|High|High|
|**Hybrid**|Very High|Medium|High|High|

**Recommendation**: Hybrid (content-based for new videos, collaborative for established)

**Two-stage Ranking**:

1. **Candidate Generation**: Retrieve ~1000 candidates (fast, simple model)
2. **Ranking**: Re-rank top 50 with complex model (accurate, expensive)

**Features**:

- User history: Watch time, likes, searches
- Video metadata: Title, tags, category, upload time
- Social: Trending, friends watching
- Context: Time of day, device type

##### 8. **Real-time View Counter**

|Approach|Accuracy|Latency|Cost|Scalability|
|---|---|---|---|---|
|**Write to DB per view**|100%|High|Very High|Poor|
|**Batch writes (every 10s)**|~99%|Low|Medium|High|
|**Approximate counter (HyperLogLog)**|~98%|Very Low|Very Low|Very High|
|**Hybrid (exact + approx)**|High|Low|Medium|High|

**Recommendation**: Hybrid approach

- First 1000 views: Exact (every view to DB)
- 1000-100K views: Batch (every 10 seconds)
- 100K+ views: Approximate (HyperLogLog, update every minute)

**Why**: Users notice exact counts at low views, but "1.2M views" vs "1.23M views" doesn't matter

##### 9. **Comment System**

|Approach|Scalability|Ordering|Moderation|Real-time|
|---|---|---|---|---|
|**SQL (tree structure)**|Low|Easy|Easy|No|
|**NoSQL (denormalized)**|High|Complex|Medium|No|
|**Dedicated service (Disqus-like)**|Very High|Easy|Easy|Yes|

**Recommendation**: NoSQL with comment threads denormalized

**Schema**:

```json
{
  "video_id": "abc123",
  "comment_id": "xyz789",
  "user_id": "user456",
  "text": "Great video!",
  "likes": 42,
  "parent_id": null,  // null for top-level, comment_id for replies
  "created_at": "2024-01-01T12:00:00Z"
}
```

**Pagination**: Cursor-based (better than offset for real-time updates)

##### 10. **Live Streaming**

|Protocol|Latency|Cost|Quality|Use Case|
|---|---|---|---|---|
|**RTMP**|3-10s|Low|Good|Ingestion|
|**HLS**|10-30s|Low|Excellent|Playback (mobile)|
|**WebRTC**|<1s|High|Good|Ultra-low latency|
|**Low-latency HLS**|2-6s|Medium|Excellent|Balance|

**Recommendation**: RTMP ingestion → Low-latency HLS for playback

**Live Streaming Pipeline**:

```
Streamer (OBS) → RTMP Server → Transcoder → CDN → HLS Viewers
```

**Challenges**:

- **Buffering**: Need 2-3 segments buffered (12-18s for HLS)
- **Sync**: Chat and video must be in sync
- **Scale**: Popular streams can have 1M+ concurrent viewers

##### 11. **Search & Discovery**

|Approach|Relevance|Speed|Cost|Complexity|
|---|---|---|---|---|
|**SQL LIKE**|Poor|Slow|Low|Very Low|
|**Full-text index (PostgreSQL)**|Medium|Medium|Low|Low|
|**Elasticsearch**|High|Fast|Medium|Medium|
|**ML-based (BERT)**|Very High|Medium|High|High|

**Recommendation**: Elasticsearch with ML re-ranking

**Indexing**:

- Title, description, tags, transcript (from speech-to-text)
- Boost: Recent uploads, view count, engagement
- Personalization: User watch history, location

##### 12. **Copyright Detection (Content ID)**

|Approach|Accuracy|Cost|Latency|
|---|---|---|---|
|**Manual Review**|High|Very High|Days|
|**Fingerprinting (MD5)**|Perfect (exact match)|Low|Real-time|
|**Perceptual Hashing**|High (near match)|Medium|Real-time|
|**Audio Fingerprinting**|Very High|Medium|Seconds|

**Recommendation**: Audio fingerprinting + video perceptual hashing

**Process**:

1. Extract audio fingerprint (e.g., Chromaprint)
2. Compare against database of copyrighted content
3. If match: Block, monetize (share revenue), or track

#### Advanced: System Components

##### Upload Flow

```
Client → Load Balancer → Upload Service → S3 Multipart
                                       ↓
                                   Metadata DB
                                       ↓
                                 Processing Queue (SQS)
                                       ↓
                              Transcoding Workers (EC2)
                                       ↓
                                  CDN (CloudFront)
```

##### Playback Flow

```
Client → CDN (cache hit? Yes → Return)
              ↓ No
         Origin Server → S3 → CDN → Client
```

##### Database Schema

**Videos Table** (PostgreSQL):

```sql
video_id (PK), user_id, title, description, 
upload_date, duration, view_count, like_count, 
status (processing/ready/deleted)
```

**Video Variants** (NoSQL - DynamoDB):

```
video_id (PK), resolution (SK), codec, url, 
bitrate, file_size
```

**User Interactions** (NoSQL):

```
user_id (PK), video_id (SK), watched_percentage, 
liked, subscribed, watched_at
```

#### Monitoring & Metrics

##### Video Metrics

- **Upload success rate**: Target >99.5%
- **Processing latency**: p50 < 5 min, p99 < 15 min
- **CDN cache hit rate**: Target >85%
- **Video start time**: p50 < 2s, p95 < 5s
- **Buffering ratio**: <0.5% of playback time
- **Video quality index (VQI)**: Combines bitrate, resolution, buffering

##### Infrastructure Metrics

- **Origin requests**: Should be <10% of total (90%+ CDN cache hit)
- **Transcoding queue depth**: <1000 videos
- **Database query latency**: p95 < 50ms
- **API latency**: p95 < 200ms

#### Cost Optimization

##### Typical Cost Breakdown (YouTube-scale)

- **CDN & Bandwidth**: 40-50%
- **Storage**: 20-25%
- **Compute (Transcoding)**: 15-20%
- **Databases & Misc**: 10-15%

##### Optimization Strategies

1. **Lazy Encoding**: Don't generate all resolutions upfront
    
    - Encode 720p immediately
    - Generate 1080p/4K when requested >10 times
    - **Savings**: 30-40% transcoding cost
2. **Intelligent Caching**:
    
    - Cache popular videos in more edge locations
    - Unpopular: Fewer edge locations (regional only)
    - **Savings**: 20% CDN cost
3. **Storage Tiering**:
    
    - Auto-move old videos to Glacier
    - Regenerate from source if requested (rare)
    - **Savings**: 80% storage cost for old content
4. **Compression**:
    
    - Use AV1 for new uploads (50% bandwidth savings)
    - Worth the higher encoding cost for popular videos
    - **Savings**: 20-30% bandwidth cost

#### Scalability Numbers

**YouTube Scale (hypothetical)**:

- **Videos uploaded**: 500 hours/minute = 720,000 hours/day
- **Storage needed**: ~1 PB/day raw, ~200 TB after compression/deduplication
- **Watch time**: 1 billion hours/day
- **Bandwidth**: ~5-10 Pbps (petabits per second)
- **Servers**: 100,000+ servers globally (CDN + origin + processing)

#### Amazon-Specific Considerations

- **S3**: Video storage with lifecycle policies
- **MediaConvert**: Managed video transcoding (vs EC2 with FFmpeg)
- **CloudFront**: CDN with Origin Shield
- **ElastiCache**: For metadata caching (video info, user prefs)
- **OpenSearch**: For video search
- **Kinesis**: For real-time view count aggregation
- **SageMaker**: For recommendation models
- **Rekognition**: For video content analysis (thumbnails, moderation)
- **Transcribe**: For automatic captions
- **Lambda**: For lightweight processing (webhooks, notifications)

#### JD Connection

> "Systems that adapt to demand and supply volatility" and "optimize for scale, latency, and resource usage"

---

## 📊 Comprehensive Coverage Matrix

|Concept|Problem 1|Problem 2|Problem 3|Problem 4|Problem 5|Problem 6|Problem 7|
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
|**Database Design**|✅|⚪|✅|✅|⚪|✅|✅|
|**SQL vs NoSQL**|✅|⚪|✅|✅|⚪|✅|✅|
|**Sharding**|✅|⚪|✅|✅|✅|✅|✅|
|**Replication**|✅|⚪|✅|✅|✅|⚪|✅|
|**Caching**|✅|✅|✅|✅|✅✅|✅|✅|
|**Load Balancing**|✅|✅|✅|✅|✅|✅|✅|
|**Message Queues**|✅✅|⚪|✅✅|✅|⚪|✅|✅|
|**API Design**|✅|✅✅|✅|✅|✅|✅|✅|
|**Rate Limiting**|✅|✅✅|⚪|✅|⚪|✅|✅|
|**CDN**|⚪|⚪|⚪|⚪|✅|⚪|✅✅|
|**Real-time Processing**|✅|⚪|✅✅|✅|⚪|⚪|✅|
|**Stream Processing**|⚪|⚪|✅✅|⚪|⚪|⚪|✅|
|**Consistency Models**|✅|✅|✅|✅✅|✅✅|⚪|✅|
|**Distributed Locks**|✅✅|⚪|⚪|✅|⚪|⚪|⚪|
|**Geospatial**|⚪|⚪|⚪|✅✅|⚪|⚪|⚪|
|**WebSockets**|⚪|⚪|⚪|✅✅|⚪|⚪|✅|
|**Monitoring**|✅|✅|✅|✅|✅|✅|✅|
|**Fault Tolerance**|✅✅|✅|✅|✅|✅|✅✅|✅|
|**Security**|✅|✅|⚪|✅|⚪|⚪|✅|
|**Search**|⚪|⚪|⚪|⚪|⚪|✅|✅✅|
|**Recommendation**|⚪|⚪|⚪|⚪|⚪|⚪|✅✅|

**Legend**: ⚪ Not Covered | ✅ Covered | ✅✅ Primary Focus

---

## 🎯 Amazon Interview Best Practices

### Before the Interview

- [ ] **Research Amazon Leadership Principles** - System design decisions should align with these
- [ ] **Study AWS Services** - Familiarize with S3, EC2, RDS, DynamoDB, SQS, SNS, Lambda, etc.
- [ ] **Practice Drawing** - Get comfortable with whiteboard/virtual diagramming
- [ ] **Time Management** - Practice 45-minute mock interviews
- [ ] **Review Your Resume** - Be ready to discuss projects using HLD concepts

### During the Interview

#### 1. Clarify Requirements (5-10 minutes)

**Functional Requirements:**

- What are the core features?
- Who are the users?
- What's the scale? (DAU, MAU, requests/second)

**Non-Functional Requirements:**

- Latency requirements? (p50, p95, p99)
- Consistency vs Availability trade-offs?
- Read-heavy or write-heavy?
- Peak load vs average load?

**Constraints:**

- Budget constraints?
- Geographic distribution?
- Compliance requirements?

**Amazon Interviewer Expectation**: _"We want candidates to ask clarifying questions and narrow down the scope. The problem is intentionally vague."_

#### 2. High-Level Design (15-20 minutes)

- [ ] Start with a simple box diagram (client, server, database)
- [ ] Identify major components
- [ ] Draw data flow
- [ ] Call out key decisions (e.g., "I'm choosing NoSQL here because...")
- [ ] Estimate scale (back-of-envelope calculations)

**Back-of-Envelope Calculation Example:**

```
100M DAU, each user makes 10 requests/day
= 1B requests/day
= 1B / 86,400 seconds
= ~11,500 QPS (queries per second)
Peak (3x): ~35,000 QPS
```

#### 3. Deep Dive (15-20 minutes)

**Interviewer will pick a component to drill into:**

- How do you handle failures?
- How do you scale this component?
- What's your database schema?
- How do you ensure data consistency?

**Pro Tip**: If not directed, choose your strongest area to deep-dive

#### 4. Discuss Trade-offs (Throughout)

**Always discuss trade-offs explicitly:**

- "I'm choosing X over Y because..."
- "The advantage of this approach is... but the downside is..."
- "For this use case, I prioritize [availability] over [consistency] because..."

**Example Trade-off Discussion:**

```
"For the caching layer, I'd use Redis over Memcached because:
✅ Redis supports more data structures (sorted sets for leaderboards)
✅ Redis has persistence options (we can recover cache on restart)
❌ Redis is slightly more expensive
❌ Memcached has lower latency for simple key-value

Given our use case needs persistence and we have budget, Redis is the better choice."
```

#### 5. Think Aloud

- **Don't go silent** - Verbalize your thought process
- **Explain your reasoning** - "I'm considering approach A vs B..."
- **Ask for feedback** - "Does this approach make sense to you?"
- **Be open to hints** - Interviewers often nudge you in the right direction

### Common Pitfalls to Avoid

❌ **Jumping to solutions** without clarifying requirements  
❌ **Over-engineering** simple problems  
❌ **Under-engineering** at scale (ignoring bottlenecks)  
❌ **Not discussing trade-offs**  
❌ **Ignoring monitoring and alerting**  
❌ **Forgetting about failure scenarios**  
❌ **Not considering cost** (important at Amazon!)

### What Interviewers Look For

✅ **Structured thinking** - Methodical approach, not random  
✅ **Trade-off awareness** - Understand pros/cons of each decision  
✅ **Scalability mindset** - Design for growth  
✅ **Practical experience** - Real-world considerations  
✅ **Communication** - Clear, concise explanations  
✅ **Customer obsession** - How does design improve customer experience?

---

## 🔑 Key Trade-offs to Remember

### 1. CAP Theorem

|Property|Choose When|Example|
|---|---|---|
|**Consistency + Availability**|Impossible with partitions|N/A|
|**Consistency + Partition Tolerance**|Data correctness critical|Banking systems|
|**Availability + Partition Tolerance**|Uptime critical|Social media feeds|

**Reality**: You get ~2.5 out of 3 (tunable consistency)

### 2. SQL vs NoSQL

|Factor|SQL (PostgreSQL)|NoSQL (DynamoDB)|
|---|---|---|
|**Schema**|Fixed, strong typing|Flexible|
|**Transactions**|Full ACID|Limited|
|**Joins**|Excellent|Poor (denormalize)|
|**Scalability**|Vertical (limited)|Horizontal (unlimited)|
|**Use Case**|Complex queries, transactions|Simple queries, massive scale|

### 3. Synchronous vs Asynchronous

|Aspect|Synchronous|Asynchronous|
|---|---|---|
|**Latency**|User waits|User doesn't wait|
|**Complexity**|Simple|Complex (queues, workers)|
|**Reliability**|Immediate failure|Retry logic needed|
|**Use Case**|Critical path (login)|Background tasks (emails)|

### 4. Push vs Pull

|Model|Push (WebSocket)|Pull (Polling)|
|---|---|---|
|**Latency**|Real-time|Delayed|
|**Server Load**|Constant connections|Periodic spikes|
|**Client Battery**|Medium drain|High drain|
|**Use Case**|Chat, live updates|Less frequent updates|

### 5. Monolith vs Microservices

|Factor|Monolith|Microservices|
|---|---|---|
|**Complexity**|Low|High|
|**Deployment**|All-or-nothing|Independent|
|**Scaling**|Scale entire app|Scale services independently|
|**Team Size**|Small team|Large team|
|**Latency**|Low (in-process)|Higher (network calls)|

**Recommendation**: Start monolith, split when needed

### 6. Vertical vs Horizontal Scaling

|Type|Vertical (Scale Up)|Horizontal (Scale Out)|
|---|---|---|
|**Limit**|Hardware limit (~96 cores, 1TB RAM)|Unlimited|
|**Cost**|Exponential|Linear|
|**Complexity**|Very Low|High (load balancing, sharding)|
|**Downtime**|Yes (during upgrade)|No (rolling updates)|

### 7. Strong vs Eventual Consistency

|Consistency|Latency|Availability|Use Case|
|---|---|---|---|
|**Strong**|Higher|Lower|Financial transactions|
|**Eventual**|Lower|Higher|Social media, caching|
|**Causal**|Medium|Medium|Messaging|

### 8. Normalization vs Denormalization

|Approach|Storage|Write Speed|Read Speed|Consistency|
|---|---|---|---|---|
|**Normalized**|Efficient|Fast|Slow (joins)|Easy|
|**Denormalized**|Redundant|Slow|Very Fast|Hard|

### 9. Client-Side vs Server-Side Rendering

|Rendering|Initial Load|SEO|Server Load|Use Case|
|---|---|---|---|---|
|**Client-Side (SPA)**|Slow|Poor|Low|Web apps|
|**Server-Side**|Fast|Excellent|High|Content sites|
|**Hybrid (Next.js)**|Fast|Excellent|Medium|Best of both|

---

## 📚 Essential Resources

### System Design Fundamentals

- [ ] [Designing Data-Intensive Applications](https://www.amazon.com/Designing-Data-Intensive-Applications-Reliable-Maintainable/dp/1449373321) - Martin Kleppmann
- [ ] [System Design Primer (GitHub)](https://github.com/donnemartin/system-design-primer)
- [ ] [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)

### Amazon-Specific

- [ ] [Amazon Builders' Library](https://aws.amazon.com/builders-library/) - Real-world Amazon system designs
- [ ] [Amazon Leadership Principles](https://www.amazon.jobs/content/en/our-workplace/leadership-principles)
- [ ] [Amazon SDE-2 Interview Prep Guide](https://amazon.jobs/content/en/how-we-hire/sde-ii-interview-prep)

### Practice Platforms

- [ ] [Hello Interview](https://www.hellointerview.com/practice) - Guided practice
- [ ] [System Design Interview (Book)](https://www.amazon.com/System-Design-Interview-insiders-Second/dp/B08CMF2CQF)
- [ ] [Exponent](https://www.tryexponent.com/) - Mock interviews

### AWS Services Cheat Sheet

```
Compute: EC2, Lambda, ECS, EKS
Storage: S3, EBS, EFS, Glacier
Database: RDS, DynamoDB, ElastiCache, Aurora
Networking: VPC, CloudFront, Route 53, API Gateway
Analytics: Kinesis, EMR, Athena, Redshift
ML: SageMaker, Rekognition, Transcribe
Messaging: SQS, SNS, EventBridge
Monitoring: CloudWatch, X-Ray
```

---

## 🎓 JD-Specific Deep Dives

### Your Team: Worldwide Capacity Planning (WWCP)

#### Core Problems You'll Solve

1. **Forecasting at Scale**
    
    - Millions of weekly forecasts
    - Time-series data at global scale
    - ML model serving and A/B testing
2. **Scheduling Optimization**
    
    - Constraint satisfaction problems
    - Resource allocation algorithms
    - Real-time schedule adjustments
3. **Real-time Monitoring**
    
    - Service Level (SL) risk detection
    - Anomaly detection
    - Alert routing and escalation

#### Relevant System Patterns

##### 1. Time-Series Forecasting System

**Components:**

- Data ingestion pipeline (historical metrics)
- Feature store (pre-computed features)
- Model training pipeline (offline)
- Model serving (online predictions)
- Feedback loop (actual vs predicted)

**Technologies:**

- **Data Lake**: S3 + Athena
- **Feature Store**: DynamoDB or Redis
- **Model Training**: SageMaker
- **Model Serving**: Lambda or SageMaker endpoints
- **Monitoring**: CloudWatch + custom metrics

##### 2. Real-time Event Processing

**For monitoring SL risks:**

```
Contact Volume Stream → Kinesis → Lambda → Anomaly Detection
                                          ↓
                                    Alert if threshold exceeded
                                          ↓
                                    SNS → On-call engineer
```

##### 3. Optimization Engine

**Scheduling optimization:**

- Input: Forecast, constraints, associate preferences
- Algorithm: Mixed Integer Programming (MIP) or Genetic Algorithm
- Output: Optimized schedule
- Scale: Thousands of associates, hundreds of sites

**Trade-off**: Optimal solution (slow) vs Good-enough solution (fast)

#### Questions You Might Get

**Q: "Design the forecasting system that generates millions of forecasts weekly."**

**Key Points to Cover:**

- Batch processing pipeline (not real-time, can take hours)
- Parallel processing (forecast different regions/queues independently)
- Model versioning and A/B testing
- Monitoring forecast accuracy (MAPE, MAE, RMSE)
- Handling special events (Prime Day, holidays)

**Q: "How would you detect and mitigate SL risks in real-time?"**

**Key Points to Cover:**

- Real-time metrics pipeline (current volume, handle time, available agents)
- Anomaly detection (statistical or ML-based)
- Alerting system (threshold-based + predictive)
- Mitigation strategies (move agents, overtime, queue priority)
- False positive vs false negative trade-off

**Q: "Design a system to generate schedules for thousands of associates."**

**Key Points to Cover:**

- Constraint definition (work rules, preferences, coverage)
- Optimization objective (cost, fairness, coverage)
- Scalability (parallel scheduling by site/team)
- Agent experience (shift preferences, time-off requests)
- Real-time adjustments (sick calls, over/under staffing)

---

## ✅ Final Preparation Checklist

### Week 1-2: Concepts

- [ ] Complete all 9 system design concept guides
- [ ] Watch Amazon's HLD prep videos
- [ ] Read AWS Well-Architected Framework
- [ ] Study Amazon Leadership Principles

### Week 3-4: Practice

- [ ] Solve all 7 problems independently
- [ ] Draw diagrams for each problem
- [ ] Write out trade-offs for each decision
- [ ] Time yourself (45 minutes per problem)

### Week 5-6: Mock Interviews

- [ ] 3-5 mock interviews with peers or coaches
- [ ] Practice thinking aloud
- [ ] Get feedback on communication
- [ ] Iterate on weak areas

### Final Week

- [ ] Review all 7 problems
- [ ] Review trade-offs document
- [ ] Practice with timer (40 minutes to leave buffer)
- [ ] Get good sleep before interview

### Day Before Interview

- [ ] Review your notes (this document)
- [ ] Watch 1-2 system design videos
- [ ] Prepare questions to ask interviewer
- [ ] Test video/whiteboard setup
- [ ] Relax and stay confident

---

## 💪 Confidence Builders

### You've Got This Because:

✅ **Strategically Selected**: These 7 problems cover 95%+ of HLD concepts  
✅ **Amazon-Aligned**: Based on real interview data from 2024-2025  
✅ **JD-Relevant**: Direct connection to WWCP team challenges  
✅ **Comprehensive**: Each problem covers unique patterns

### Remember:

- **Perfect designs don't exist** - There are only trade-offs
- **Interviewers want you to succeed** - They're evaluating potential, not perfection
- **Communication matters more** than "right" answers
- **Your experience is valuable** - Relate problems to what you've built

---

## 🎯 Interview Day Mindset

### What Interviewers Really Want

1. **Can this person build scalable systems?** ✅
2. **Do they understand trade-offs?** ✅
3. **Can they communicate clearly?** ✅
4. **Will they be a good teammate?** ✅
5. **Do they align with Amazon's principles?** ✅

### Your Strengths

- **Domain Knowledge**: Capacity planning, forecasting, optimization
- **Preparation**: 7 comprehensive problems solved
- **Structured Thinking**: This document is your blueprint
- **Trade-off Awareness**: You know the pros/cons of every decision

---

## 📞 Questions to Ask Interviewer

### About the Team

- "What are the most challenging technical problems your team is solving?"
- "How does the team balance innovation with operational excellence?"
- "What does a typical sprint look like for your team?"

### About the Role

- "What would my first 90 days look like?"
- "What are the key projects I'd be working on?"
- "How does the team collaborate with other Amazon teams?"

### About Growth

- "What opportunities are there for technical leadership?"
- "How does Amazon support learning and skill development?"
- "What does career progression look like for SDE-2s on your team?"

---

## 🚀 You're Ready!

You have:

- ✅ 7 strategically selected problems
- ✅ Comprehensive trade-offs for each problem
- ✅ Amazon-specific considerations
- ✅ JD-aligned deep dives
- ✅ Interview best practices
- ✅ A clear preparation plan

**Now go ace that interview!** 💪

---

_Created for Amazon SDE-2 HLD Interview Preparation_  
_Team: Worldwide Capacity Planning (WWCP) - Vibe_  
_Location: Hyderabad_

**Good luck! You've got this! 🎯🚀**