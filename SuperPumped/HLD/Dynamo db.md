https://www.youtube.com/watch?v=2X2SO3Y-af8


## 🔑 Core Concepts

1. **Data Model**
    
    - **Tables** → Like in RDBMS, but without fixed schema.
        
    - **Items** → Equivalent to rows; each item is a collection of key-value pairs.
        
    - **Attributes** → Equivalent to columns, but flexible (each item can have different attributes).
        
    - **Primary Key** → Mandatory; uniquely identifies each item.
        
        - **Partition Key only (simple primary key)**
            
        - **Partition Key + Sort Key (composite primary key)**
            
2. **Indexes**
    
    - **Primary Index** (always exists): Based on table’s partition + optional sort key.
        
    - **Local Secondary Index (LSI)**: Same partition key as table but different sort key.
        
    - **Global Secondary Index (GSI)**: Different partition + sort key (flexible querying).
        

---

## ⚙️ How DynamoDB Works Internally

1. **Partitioning (Data Distribution)**
    
    - Every item’s partition key is hashed.
        
    - That hash decides which **partition (physical storage node)** the item goes to.
        
    - Partitions are spread across multiple servers for scalability.
        
    - Hot partitions can occur if partition keys are not well-distributed.
        
2. **Storage Engine**
    
    - Data is stored across **SSD-backed storage** with replication across multiple Availability Zones (AZs).
        
    - Ensures durability & high availability.
        
    - Data is eventually consistent by default, but you can request strong consistency (within the same region).
        
3. **Read/Write Model**
    
    - **WCU (Write Capacity Units)** → Throughput for writes.
        
    - **RCU (Read Capacity Units)** → Throughput for reads.
        
    - **On-Demand Mode** → You don’t pre-allocate RCUs/WCUs, pay per request.
        
    - **Provisioned Mode** → You reserve a certain throughput capacity.
        
4. **Consistency**
    
    - **Eventually Consistent Reads** → Fast, may not reflect the latest write immediately.
        
    - **Strongly Consistent Reads** → Always reflect the latest write (but can have slightly higher latency).
        
5. **Indexes & Querying**
    
    - You can only query efficiently by partition key (and sort key range).
        
    - For other access patterns, you need GSIs/LSIs.
        
    - Supports `Scan` (full table scan, costly) and `Query` (efficient, by key).
        
6. **Caching**
    
    - Optionally integrate with **DAX (DynamoDB Accelerator)** for in-memory caching, reducing read latency from ms → microseconds.
        

---

## 🔄 Write Path

1. Client sends a `PutItem` or `UpdateItem`.
    
2. DynamoDB hashes the partition key → finds partition.
    
3. The request goes through a **replication log (commit log)**.
    
4. Written to **3 AZs** before confirming success.
    
5. Optionally streams the change to **DynamoDB Streams** (for event-driven processing).
    

---

## 📖 Read Path

1. Client issues `GetItem` / `Query`.
    
2. Partition key is hashed → correct partition is located.
    
3. Data is fetched from the replica (eventually consistent) or the leader (strong consistent).
    
4. If DAX is enabled, data might be served directly from cache.
    

---

## ✨ Strengths

- Fully managed (no server ops).
    
- Scales to millions of requests/sec.
    
- High availability (multi-AZ replication).
    
- Predictable low latency.
    

## ⚠️ Limitations

- Limited querying (only via keys or indexes).
    
- Expensive for scans & large analytics workloads.
    
- Hot partition issue if partition keys are not well-distributed.






how does dynamo db will maintain partituion ketys , sort keys and global secondary indexes in instagram follower follwoing user case

Excellent question — this is a **classic DynamoDB + Instagram follow system** design problem and **very common in system-design interviews**.

I’ll explain:

1. **Access patterns (most important)**
    
2. **Partition key & sort key design**
    
3. **How GSI works**
    
4. **How DynamoDB internally distributes data**
    
5. **Common problems & solutions**
    

---

# 📸 Instagram Follow / Following – DynamoDB Design

## Core Access Patterns

We must design keys **only after** defining access patterns.

### Required Queries

1. Get all **followers** of a user
    
2. Get all users **a user follows**
    
3. Check if **A follows B**
    
4. Get **follower count / following count**
    
5. Paginated results (infinite scroll)
    

---

## Single Table Design (Recommended)

### Table: `UserConnections`

|PK (Partition Key)|SK (Sort Key)|Attributes|
|---|---|---|
|`USER#123`|`FOLLOWING#456`|createdAt|
|`USER#456`|`FOLLOWER#123`|createdAt|

👉 **One follow action = two writes**

---

## 1️⃣ Partition Key Strategy

### Partition Key = `USER#{userId}`

Why?

- All relationships for a user stay together
    
- Enables fast query per user
    
- DynamoDB hashes PK → distributes across partitions
    

Example:

`USER#123  → Partition A USER#456  → Partition C`

---

## 2️⃣ Sort Key Strategy

Sort key encodes relationship type + other user ID:

`FOLLOWING#targetUserId FOLLOWER#sourceUserId`

### Why?

- Enables `begins_with(SK, 'FOLLOWER#')`
    
- Enables range queries
    
- Enables pagination
    

---

## 3️⃣ Query Examples

### Get Followers of User 123

`PK = USER#123 SK begins_with FOLLOWER#`

### Get Following of User 123

`PK = USER#123 SK begins_with FOLLOWING#`

### Check if User 123 follows 456

`PK = USER#123 SK = FOLLOWING#456`

🔥 **O(1) lookup**

---

## 4️⃣ Global Secondary Index (GSI)

### Why GSI is Needed?

Sometimes you want **reverse access** or **sorting by time**.

### Example GSI: `GSI1`

|GSI1_PK|GSI1_SK|
|---|---|
|`FOLLOWER#123`|`createdAt`|

### Use Case

➡ Get **recent followers across the platform**  
➡ Analytics  
➡ Recommendations

---

## 5️⃣ DynamoDB Partition Internals (IMPORTANT)

### How DynamoDB Stores Data

- Hashes **Partition Key**
    
- Maps to **physical partitions**
    
- Sort Key only orders **within partition**
    

`Hash(USER#123) → Partition 7 Hash(USER#456) → Partition 21`

If a user has **millions of followers** → **hot partition risk**

---

## 6️⃣ Hot Partition Problem & Solution

### Problem

Celebrity user (e.g. user 1M followers)

`PK = USER#1`

→ Too many reads/writes on same partition

---

### ✅ Solution 1: Partition Key Sharding

`PK = USER#1#SHARD#1 PK = USER#1#SHARD#2 ...`

Shard chosen by:

`hash(followerId) % N`

Reads → query all shards + merge

---

### ✅ Solution 2: Fan-out on Write

- Precompute follower counts
    
- Cache in Redis
    
- Use DynamoDB Streams → Lambda
    

---

## 7️⃣ Maintaining Follower Counts

❌ Don’t count rows every time

### Use Counter Table

|PK|followersCount|followingCount|
|---|---|---|
|USER#123|1052|231|

Updated atomically using:

`ADD followersCount :1`

---

## 8️⃣ Strong vs Eventual Consistency

|Operation|Consistency|
|---|---|
|Follow/unfollow|Strong|
|Feed rendering|Eventual|
|Follower count|Eventual|

---

## 9️⃣ Unfollow Flow (Transactional Write)

Use **DynamoDB Transactions**:

1. Delete FOLLOWING row
    
2. Delete FOLLOWER row
    
3. Decrement counters
    

All succeed or none

---

## 10️⃣ Complete Follow Flow (End-to-End)

`User A follows User B  ↓ Write 2 items (A → FOLLOWING, B → FOLLOWER)  ↓ Update counter table  ↓ Emit event via DynamoDB Stream  ↓ Async feed / recommendation updates`

---

## 11️⃣ Why DynamoDB Fits Instagram Use Case

|Reason|Benefit|
|---|---|
|Key-value|Fast lookups|
|Partitioning|Massive scale|
|GSIs|Flexible access|
|No joins|Predictable latency|
|Streams|Event-driven architecture|

---

## 🎯 Interview-Ready Summary

> “In an Instagram-like follow system, DynamoDB uses the userId as the partition key to group relationships per user and sort keys to distinguish followers and following. GSIs provide alternate access patterns like time-based or reverse lookups. Hot partitions are mitigated via sharding and follower counts are maintained using atomic counters.”

---

If you want next:

- **Instagram feed generation using DynamoDB**
    
- **Twitter vs Instagram follow schema**
    
- **Exact DynamoDB query examples**
    
- **Comparison with Cassandra**
    

Just tell me 👍

tell all questions you mentioned

Perfect 👍  
Below is a **complete interview-style question list** for the **Instagram / Twitter follow–feed system using DynamoDB**, covering **schema design, scaling, GSIs, hot partitions, consistency, and trade-offs**.

You can treat this as a **revision sheet**.

---

# 📸 Instagram / Twitter Follow System – Interview Questions

---

## 1️⃣ Core Follow / Following Design

1. How would you design **followers and following** in DynamoDB?
    
2. What are the **primary access patterns** for a follow system?
    
3. Why is **single-table design** preferred for DynamoDB here?
    
4. How do you model a **many-to-many relationship** in DynamoDB?
    
5. Why do you write **two rows** per follow?
    
6. How do you check if **user A follows user B** efficiently?
    
7. How do you implement **unfollow** safely?
    
8. How do you prevent **duplicate follow entries**?
    

---

## 2️⃣ Partition Key & Sort Key

9. Why choose `userId` as the **partition key**?
    
10. What happens internally when DynamoDB hashes a partition key?
    
11. What is the role of the **sort key**?
    
12. Why prefix sort keys (`FOLLOWER#`, `FOLLOWING#`)?
    
13. How does `begins_with()` help in queries?
    
14. Can DynamoDB query without a sort key?
    
15. How do you support **pagination** in follower lists?
    
16. How would you sort followers by **time**?
    

---

## 3️⃣ Global Secondary Index (GSI)

17. What is a **GSI** and why is it needed?
    
18. When should you use a **GSI vs base table**?
    
19. How does DynamoDB maintain GSIs internally?
    
20. What happens if a GSI becomes a **hot index**?
    
21. Can GSIs have **eventual consistency only**?
    
22. How do you design a GSI for **recent followers**?
    
23. What are the **write costs** of GSIs?
    
24. How many GSIs are allowed per table?
    
25. Can GSIs be added after table creation?
    

---

## 4️⃣ Hot Partition & Scalability

26. What is a **hot partition** in DynamoDB?
    
27. Why are celebrity users problematic?
    
28. How does DynamoDB split partitions automatically?
    
29. Why doesn’t partition splitting fully solve hot keys?
    
30. How do you implement **partition key sharding**?
    
31. What is the trade-off of sharding?
    
32. How many shards should you create?
    
33. Can sharding affect read latency?
    
34. How does Instagram handle millions of followers?
    

---

## 5️⃣ Consistency & Transactions

35. Does DynamoDB support **strong consistency**?
    
36. When should you use **strong vs eventual consistency**?
    
37. How do you ensure **follow/unfollow atomicity**?
    
38. What are DynamoDB **transactions**?
    
39. What is the performance impact of transactions?
    
40. Can transactions span multiple tables?
    
41. What happens if one write fails in a transaction?
    

---

## 6️⃣ Counters (Follower Count)

42. Why should you NOT count followers using `COUNT(*)`?
    
43. How do you maintain follower counts efficiently?
    
44. What are **atomic counters** in DynamoDB?
    
45. How do you prevent counter inconsistency?
    
46. What happens if counter update fails?
    
47. Are counters strongly consistent?
    
48. Can counters become hot?
    

---

## 7️⃣ Streams & Event-Driven Design

49. What are **DynamoDB Streams**?
    
50. How does Instagram use streams?
    
51. What events would you publish on follow/unfollow?
    
52. How do streams help with **feed generation**?
    
53. Can streams be replayed?
    
54. What is at-least-once delivery?
    
55. How do you handle duplicate stream events?
    

---

## 8️⃣ Feed Generation (Very Important)

56. Push model vs Pull model for feeds?
    
57. Why does Instagram use **hybrid feed generation**?
    
58. How does follow data help in feed fan-out?
    
59. What is **fan-out on write**?
    
60. What is **fan-out on read**?
    
61. Which approach is better for celebrities?
    
62. How do you handle inactive followers?
    

---

## 9️⃣ Performance & Optimization

63. How do you cache followers/following?
    
64. When would you use Redis with DynamoDB?
    
65. How do you reduce read costs?
    
66. How does pagination reduce load?
    
67. What is the impact of large partitions?
    
68. How do you monitor hot keys?
    
69. What CloudWatch metrics are important?
    

---

## 🔟 Failure Scenarios

70. What if one of the two follow writes fails?
    
71. What if follower count update fails?
    
72. What if GSI write fails?
    
73. How do you handle partial failures?
    
74. How do you ensure idempotency?
    
75. What happens during region failure?
    
76. How do you do multi-region replication?
    

---

## 1️⃣1️⃣ Security & Abuse

77. How do you prevent follow spam?
    
78. How do you rate-limit follow requests?
    
79. How do you block users?
    
80. How does blocking affect follower tables?
    
81. How do you implement private accounts?
    

---

## 1️⃣2️⃣ Comparison Questions (Very Common)

82. DynamoDB vs Cassandra for followers?
    
83. DynamoDB vs SQL (why not joins)?
    
84. Why not store followers as an array?
    
85. Why not use graph databases?
    
86. How does Twitter’s model differ from Instagram?
    
87. How does LinkedIn follow model differ?
    

---

## 1️⃣3️⃣ Schema Evolution & Limits

88. What is the item size limit in DynamoDB?
    
89. What happens if a user has 10M followers?
    
90. How do you delete a user safely?
    
91. How do you archive old follow data?
    
92. Can you migrate schema without downtime?
    

---

## 🎯 Final Interview Power Question

93. **Design Instagram’s follow system using DynamoDB. Explain keys, GSIs, scaling, and failure handling.**
    

---

## 🧠 How to Answer in Interview (Tip)

Structure every answer as:

1. **Access pattern**
    
2. **Schema**
    
3. **Scaling issue**
    
4. **Solution & trade-off**
    

---

If you want next, I can:

- Answer **each of these questions one by one**
    
- Convert this into a **PDF revision sheet**
    
- Do a **mock interview Q&A**
    
- Compare **Instagram vs Twitter feed design**
    

Just tell me which one you want 👌

Answer each of these questions one by one

Got it 👍  
I’ll **answer ALL the questions one by one**, in **interview-ready but concise form**.  
You can read them sequentially or revise selectively.

I’ll keep answers **clear, structured, and practical**.

---

# 📸 Instagram / Twitter Follow System – Answers

---

## 1️⃣ Core Follow / Following Design

### 1. How would you design followers and following in DynamoDB?

Using a **single table** with:

- Partition Key = `USER#{userId}`
    
- Sort Key = `FOLLOWER#{otherUserId}` or `FOLLOWING#{otherUserId}`
    

Each follow creates **two records**.

---

### 2. What are the primary access patterns?

- Get followers of a user
    
- Get following of a user
    
- Check if A follows B
    
- Get follower/following count
    
- Paginate results
    

---

### 3. Why is single-table design preferred?

- DynamoDB is optimized for **access patterns**
    
- Avoids joins
    
- Enables predictable latency at scale
    

---

### 4. How do you model many-to-many relationships?

By **duplicating data**:

- A → FOLLOWING → B
    
- B → FOLLOWER → A
    

---

### 5. Why write two rows per follow?

Because DynamoDB doesn’t support joins.  
You must store data **both ways** for efficient reads.

---

### 6. How do you check if user A follows user B?

`PK = USER#A SK = FOLLOWING#B`

O(1) lookup.

---

### 7. How do you implement unfollow safely?

Using **DynamoDB Transactions**:

- Delete FOLLOWING row
    
- Delete FOLLOWER row
    
- Update counters
    

---

### 8. How do you prevent duplicate follows?

Use **conditional writes**:

`attribute_not_exists(PK)`

---

## 2️⃣ Partition Key & Sort Key

### 9. Why choose userId as partition key?

- Groups all relationships of a user
    
- Enables fast per-user queries
    

---

### 10. What happens internally to partition key?

DynamoDB:

- Hashes PK
    
- Maps it to a physical partition
    

---

### 11. Role of sort key?

- Orders items inside a partition
    
- Enables range queries & pagination
    

---

### 12. Why prefix sort keys?

To support:

`begins_with(SK, 'FOLLOWER#')`

---

### 13. How does begins_with help?

Allows filtering by relationship type without scanning.

---

### 14. Can DynamoDB query without sort key?

Yes, but:

- You lose filtering & ordering
    
- Pagination becomes limited
    

---

### 15. How do you support pagination?

Using:

- `LastEvaluatedKey`
    
- Sort key ordering
    

---

### 16. How to sort followers by time?

Include timestamp in SK or attribute + GSI.

---

## 3️⃣ Global Secondary Index (GSI)

### 17. What is a GSI?

An alternate index with **different PK/SK**.

---

### 18. When use GSI vs base table?

When access pattern **cannot be served** by base keys.

---

### 19. How does DynamoDB maintain GSIs?

- Every base table write
    
- Triggers async write to GSI
    

---

### 20. What if a GSI becomes hot?

It throttles independently → affects write throughput.

---

### 21. Are GSIs strongly consistent?

❌ No. GSIs are **eventually consistent only**.

---

### 22. GSI for recent followers?

`GSI_PK = FOLLOWER#{userId} GSI_SK = createdAt`

---

### 23. Write cost of GSIs?

Each GSI = **extra write cost**.

---

### 24. Max GSIs per table?

👉 **20 GSIs**

---

### 25. Can GSIs be added later?

✔ Yes (but backfilling takes time).

---

## 4️⃣ Hot Partition & Scaling

### 26. What is a hot partition?

Too many reads/writes on **same PK**.

---

### 27. Why celebrities cause issues?

Millions of followers → single PK overloaded.

---

### 28. How does DynamoDB split partitions?

Automatically when size or throughput exceeds limit.

---

### 29. Why splitting isn’t enough?

Hash of same PK still routes to same logical key.

---

### 30. How do you shard partition keys?

`USER#1#SHARD#3`

Shard = `hash(followerId) % N`

---

### 31. Trade-off of sharding?

- Reads require querying multiple shards
    
- Slightly higher latency
    

---

### 32. How many shards?

Based on:

- Write rate
    
- Follower growth  
    Typically 10–100.
    

---

### 33. Does sharding affect read latency?

Yes, but manageable with parallel queries.

---

### 34. How does Instagram handle scale?

- Sharding
    
- Caching
    
- Hybrid feed model
    

---

## 5️⃣ Consistency & Transactions

### 35. Does DynamoDB support strong consistency?

✔ Yes (base table only).

---

### 36. When use strong consistency?

- Follow/unfollow
    
- Authorization checks
    

---

### 37. How ensure atomic follow?

Using **transactions**.

---

### 38. What are DynamoDB transactions?

ACID operations across multiple items.

---

### 39. Transaction performance impact?

Higher latency & cost.

---

### 40. Can transactions span tables?

✔ Yes.

---

### 41. If one write fails?

Entire transaction rolls back.

---

## 6️⃣ Counters

### 42. Why not COUNT(*)?

Very expensive and slow.

---

### 43. How maintain follower count?

Using **atomic counters**.

---

### 44. What are atomic counters?

`ADD followersCount :1`

---

### 45. Prevent counter inconsistency?

Update inside same transaction.

---

### 46. What if counter update fails?

Transaction rollback.

---

### 47. Are counters strongly consistent?

Yes (base table).

---

### 48. Can counters be hot?

Yes → mitigate using sharded counters.

---

## 7️⃣ Streams & Events

### 49. What are DynamoDB Streams?

Change log of table updates.

---

### 50. How Instagram uses streams?

- Feed updates
    
- Analytics
    
- Recommendations
    

---

### 51. Events on follow/unfollow?

- FollowCreated
    
- FollowDeleted
    

---

### 52. Streams help feed generation how?

Trigger async fan-out.

---

### 53. Can streams be replayed?

✔ Yes (within retention).

---

### 54. What is at-least-once delivery?

Events may be delivered more than once.

---

### 55. Handling duplicates?

Use **idempotent consumers**.

---

## 8️⃣ Feed Generation

### 56. Push vs Pull feed?

- Push: precompute
    
- Pull: compute on read
    

---

### 57. Why hybrid model?

Balances latency and cost.

---

### 58. Role of follow data?

Determines which feeds to fan out.

---

### 59. Fan-out on write?

Push posts to followers’ feeds.

---

### 60. Fan-out on read?

Fetch posts from followed users at read time.

---

### 61. Which for celebrities?

Pull model.

---

### 62. Handling inactive followers?

Skip fan-out for inactive users.

---

## 9️⃣ Performance & Optimization

### 63. Cache followers?

Redis.

---

### 64. Redis + DynamoDB why?

Low-latency reads.

---

### 65. Reduce read costs?

Caching + pagination.

---

### 66. Pagination helps how?

Limits data per request.

---

### 67. Impact of large partitions?

Latency spikes.

---

### 68. Monitor hot keys?

CloudWatch throttling metrics.

---

### 69. Important metrics?

- ReadThrottleEvents
    
- WriteThrottleEvents
    
- Latency
    

---

## 🔟 Failures

### 70. One write fails?

Transaction rollback.

---

### 71. Counter fails?

Retry or reconcile async.

---

### 72. GSI write fails?

Base write succeeds, GSI lags.

---

### 73. Partial failures?

Use retries + idempotency.

---

### 74. Ensure idempotency?

Client-generated request IDs.

---

### 75. Region failure?

Multi-region replication.

---

### 76. Multi-region?

DynamoDB Global Tables.

---

## 1️⃣1️⃣ Security & Abuse

### 77. Prevent spam follows?

Rate limiting.

---

### 78. Rate-limit how?

API Gateway + Redis.

---

### 79. Blocking users?

Store block relationship.

---

### 80. Blocking effect?

Filter during queries.

---

### 81. Private accounts?

Approve follow requests before writing.

---

## 1️⃣2️⃣ Comparisons

### 82. DynamoDB vs Cassandra?

DynamoDB = managed, auto-scale.

---

### 83. DynamoDB vs SQL?

SQL doesn’t scale joins well.

---

### 84. Why not arrays?

Item size limit (400 KB).

---

### 85. Why not graph DB?

Too slow at Instagram scale.

---

### 86. Twitter vs Instagram?

Twitter more pull-based.

---

### 87. LinkedIn difference?

More graph traversal, fewer writes.

---

## 1️⃣3️⃣ Limits & Evolution

### 88. Item size limit?

400 KB.

---

### 89. 10M followers?

Shard aggressively + cache.

---

### 90. Delete user safely?

Soft delete + async cleanup.

---

### 91. Archive old follows?

Move to S3.

---

### 92. Schema migration?

Add GSIs gradually.

---

## 🎯 Final Design Question

### 93. Design Instagram follow system?

**Access patterns → schema → GSIs → sharding → counters → streams → failure handling**



Perfect — this is a **top-tier system-design topic** and often the **deciding question** in senior interviews.

Below is a **deep, production-level explanation of Celebrity Feed Generation**, exactly how **Instagram / Twitter** handle it at massive scale.

---

# 🌟 Celebrity Feed Generation — Deep Dive

## Problem Statement

Celebrity users have:

- Millions of followers
    
- Extremely high write amplification if we do **fan-out on write**
    
- Tight latency requirements for feed reads
    

### Naive approach (❌ BAD)

`Celebrity posts → push to 50M followers`

- Impossible to scale
    
- Huge cost
    
- DynamoDB write storms
    

---

## Core Feed Models Recap

|Model|Description|
|---|---|
|Fan-out on write|Push post to followers’ feeds|
|Fan-out on read|Pull posts at read time|
|Hybrid|Mix of both|

👉 **Celebrities ALWAYS use pull model**

---

## 🎯 High-Level Architecture

`Celebrity Post    ↓ Post Service    ↓ Posts Table (time-sorted)    ↓ Feed Read Service    ↓ Merge with user feed`

---

## 1️⃣ Data Model (DynamoDB)

### 📦 Posts Table

`PK = USER#<celebrityId> SK = POST#<timestamp>`

- Posts stored **only once**
    
- Sorted by time
    
- Highly cacheable
    

---

## 2️⃣ Feed Read Flow (MOST IMPORTANT)

### When a user opens feed:

`User opens feed  ↓ Fetch normal-user feed (push model)  ↓ Fetch celebrity posts (pull model)  ↓ Merge & rank  ↓ Return feed`

### Why?

- Normal users → cheap fan-out
    
- Celebrities → pull at read time
    

---

## 3️⃣ Identifying Celebrities

### How system knows someone is a celebrity:

- Follower count threshold (e.g., > 100k)
    
- Engagement metrics
    
- Precomputed flag
    

`isCelebrity = true`

---

## 4️⃣ Celebrity Feed Fetch Logic

### Step-by-step:

1. Get list of celebrities the user follows
    
2. Fetch **latest N posts per celebrity**
    
3. Merge posts by timestamp
    
4. Rank using relevance
    

### Optimization:

- Limit to **top K celebrities**
    
- Limit to **last X hours**
    

---

## 5️⃣ Caching Strategy (CRITICAL)

### What to Cache?

|Data|Cache|
|---|---|
|Celebrity posts|Redis|
|Celebrity IDs user follows|Redis|
|Feed fragments|Redis|

### Why?

- Celebrity posts are **read-heavy**
    
- Perfect for cache
    

---

## 6️⃣ Ranking & Personalization

Feed is NOT just time-sorted.

### Ranking signals:

- Recency
    
- User interaction history
    
- Like/comment probability
    
- Relationship strength
    

### Ranking Service:

- ML-based
    
- Runs asynchronously
    
- Produces feed scores
    

---

## 7️⃣ Feed Merge Algorithm (Conceptual)

`Feed = Merge(   normalFeed (push),   celebrityFeed (pull) )`

- Use priority queue
    
- Apply ranking score
    
- Trim to feed size
    

---

## 8️⃣ Throttling & Backpressure

### Protect system:

- Limit number of celebrity posts fetched per request
    
- Pagination
    
- Async loading (“load more”)
    

---

## 9️⃣ Handling Massive Followers

### Problem

Celebrity has 100M followers.

### Solution

- No fan-out
    
- No per-follower writes
    
- Single post write only
    

---

## 🔁 End-to-End Example

### Celebrity posts

`Write 1 item → Posts table`

### User opens feed

`Read 20 celebrity posts + cached feed`

No explosion of writes 🔥

---

## 10️⃣ Why This Scales

|Problem|Solution|
|---|---|
|Write explosion|Pull model|
|Hot partitions|Sharded followers|
|Read latency|Cache + limits|
|Cost|Minimal writes|

---

## ⚖️ Trade-offs

|Pros|Cons|
|---|---|
|Scales infinitely|Slightly higher read latency|
|Cheap writes|More complex read logic|
|Reliable|Needs caching|

---

## 🎯 Interview-Ready Summary (Say This)

> “Celebrity feed generation uses a pull-based model. Celebrity posts are stored once and fetched at read time by followers. Normal users still use fan-out on write. A hybrid model avoids write amplification while maintaining low feed latency through caching and ranking.”

---

## 🧠 One-Line Rule

> **Never fan-out writes for users with unbounded followers.**