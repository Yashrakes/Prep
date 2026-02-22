
---

## The Core Problem Facebook News Feed Solves

On the surface a news feed seems simple — show friend's posts. But consider the real constraints:

```
Facebook scale reality:
────────────────────────────────────────────────
Active users:              3 billion+
Average friends per user:  338
Power users (influencers): 5,000+ friends/followers
Post creation rate:        4,000+ posts/second globally
Feed refresh rate:         User opens app → must show feed <500ms
Feed size needed:          Show ~50 most relevant posts
Celebrity post fan-out:    1 post → notify 100 million followers

Requirements:
→ User posts → all 338 friends see it in their feed instantly
→ Kim Kardashian posts → 364 million followers see it (don't crash system)
→ User opens app → feed loads in <500ms even if they have 5,000 friends
→ Ranking algorithm considers: recency, engagement, relationship strength
→ User sees own post immediately (consistency)
→ Edit post → update shows in all feeds within seconds
→ Delete post → remove from all feeds immediately
→ Handle celebrity follows without breaking fan-out system
```

This combination of **write amplification (1 post → 338 writes) + celebrity fan-out (1 post → 100M writes) + sub-500ms read latency + complex ranking + immediate consistency for self** is what forces this multi-database architecture.

---

## Why Cassandra for Posts Storage?

### The Write Amplification Problem

```
THE SCALING MATH:
════════════════════════════════════════════════════════

4,000 posts/second created globally
Average 338 friends per user
Each post must be added to 338 friend feeds

Naive calculation:
4,000 posts/sec × 338 friends = 1,352,000 feed writes/second

But that's just average. Consider spiky traffic:
- Peak hours: 10,000 posts/second
- Celebrity posts: 1 post → 100,000,000 feed updates
- Viral moments (Taylor Swift drops album): 1,000,000 posts/minute

This is an append-heavy workload
Writes dominate reads by 100:1
```

### Why Cassandra Handles This

```
CASSANDRA WRITE PATH:
════════════════════════════════════════════════════════

Post created:
{
  post_id: 'post_abc123',
  author_id: 'user_001',
  content: 'Just had the best coffee!',
  timestamp: 1708945200,
  likes_count: 0,
  comments_count: 0
}

Write to Cassandra:
────────────────────────────────────────────────
1. Append to commit log (sequential write to disk)
2. Write to MemTable (in-memory sorted structure)
3. Acknowledge write immediately
4. Later: flush MemTable to SSTable (background)

Write latency: <1ms
Throughput: 100,000+ writes/second per node

Why this works:
────────────────────────────────────────────────
✓ No random disk seeks (commit log is sequential)
✓ No index updates blocking writes
✓ No locks (partition key isolation)
✓ Writes don't block reads
✓ Horizontal scaling: add nodes → more write capacity
✓ Tunable consistency (can use ONE for speed)


Compare to PostgreSQL:
────────────────────────────────────────────────
PostgreSQL write path:
1. Acquire row lock
2. Update B-tree index
3. Write to WAL (write-ahead log)
4. Update table pages
5. Vacuum old versions (MVCC overhead)

Write latency: 5-10ms
Throughput: ~10,000 writes/second (single node)
At 1.3M writes/sec needed: would need 130 PostgreSQL servers
Unmanageable
```

### Cassandra Partitioning Strategy

```
POSTS TABLE PARTITIONING:
════════════════════════════════════════════════════════

Partition key: author_id
Clustering key: timestamp (descending)

CREATE TABLE posts (
  author_id UUID,
  post_id UUID,
  timestamp BIGINT,
  content TEXT,
  likes_count COUNTER,
  PRIMARY KEY ((author_id), timestamp, post_id)
) WITH CLUSTERING ORDER BY (timestamp DESC);

Data distribution:
────────────────────────────────────────────────
Node 1: hash(user_001) → stores all user_001's posts
Node 2: hash(user_002) → stores all user_002's posts
Node 3: hash(user_003) → stores all user_003's posts

Query optimization:
────────────────────────────────────────────────
"Show me user_001's last 20 posts"

SELECT * FROM posts
WHERE author_id = 'user_001'
ORDER BY timestamp DESC
LIMIT 20;

→ Goes directly to Node 1 (partition key routing)
→ Data already sorted by timestamp (clustering key)
→ Read is O(1) partition lookup + O(20) sequential read
→ Latency: <5ms


Why not partition by post_id?
────────────────────────────────────────────────
If partitioned by post_id:
→ User's posts scattered across all nodes
→ "Show user's last 20 posts" requires querying ALL nodes
→ Scatter-gather query (slow)
→ Partition by author_id keeps user's data together
```

---

## Why Redis for Pre-Computed Feeds?

### The Feed Generation Problem

```
NAIVE APPROACH (Fan-Out on Read):
════════════════════════════════════════════════════════

User opens app → generate feed on-the-fly:

1. Get user's friends: [friend_001, friend_002, ..., friend_338]
2. For each friend, get their recent posts:
   SELECT * FROM posts WHERE author_id = friend_001 LIMIT 10
   SELECT * FROM posts WHERE author_id = friend_002 LIMIT 10
   ...
   (338 queries to Cassandra)
3. Merge all posts (338 × 10 = 3,380 posts)
4. Rank by algorithm (engagement, recency, relationship)
5. Return top 50

Problems:
────────────────────────────────────────────────
→ 338 queries to Cassandra per feed load
→ Each query: 5ms → total: 338 × 5ms = 1,690ms
→ User waits 1.7 seconds (unacceptable, target <500ms)
→ 1 billion users opening app → 338 billion queries/day
→ Cassandra overwhelmed


CELEBRITY PROBLEM:
────────────────────────────────────────────────
User follows Taylor Swift (364M followers)
User opens app → must query Taylor's posts
364M users query same partition (celebrity's posts)
→ Hot partition problem
→ Partition overloaded
→ Query latency spikes to seconds
→ System degraded
```

### Redis Pre-Computed Feed Solution

```
FAN-OUT ON WRITE:
════════════════════════════════════════════════════════

User posts → immediately write to ALL friends' feeds:

User_001 creates post_abc123
        │
        ▼
Get user_001's friends from Graph DB:
[friend_001, friend_002, ..., friend_338]
        │
        ▼
For each friend, add post to their Redis feed:

ZADD feed:friend_001 1708945200 'post_abc123'
ZADD feed:friend_002 1708945200 'post_abc123'
...
ZADD feed:friend_338 1708945200 'post_abc123'

Score = timestamp (for sorting by recency)

Each ZADD: <1ms
338 writes in parallel: <50ms total


User opens app:
────────────────────────────────────────────────
GET /feed

Backend:
ZREVRANGE feed:user_123 0 49 WITHSCORES
→ Returns: top 50 post IDs sorted by score (timestamp)
→ Latency: <5ms

Fetch post details (batch):
MGET post:post_abc123 post:post_def456 ...
→ Returns: all 50 posts in one round trip
→ Latency: <10ms

Total feed load: <20ms (vs 1,690ms naive approach)
85x faster!
```

---

## Why Graph Database for Social Connections?

### The Friend Graph Problem

```
SOCIAL GRAPH CHARACTERISTICS:
════════════════════════════════════════════════════════

User_001's social connections:
- Friends: [user_002, user_003, ..., user_339]  (338 friends)
- Followers: [user_400, user_401, ...]  (500 followers)
- Following: [user_500, user_501, ...]  (400 following)
- Close friends: [user_002, user_005]  (2 close friends)
- Blocked: [user_999]

Queries needed:
────────────────────────────────────────────────
1. "Get user_001's friends" → O(1) lookup
2. "Are user_001 and user_002 friends?" → O(1) check
3. "Get mutual friends of user_001 and user_002" → graph traversal
4. "Friend suggestions: friends of friends who aren't my friends" → 2-hop traversal
5. "How many hops between user_001 and user_999?" → breadth-first search
6. "Get user_001's friends who live in SF and like hiking" → graph + property filter

These are GRAPH problems
Not relational problems
Not document problems
Pure graph traversal
```

### Neo4j Graph Database Solution

```
GRAPH SCHEMA:
════════════════════════════════════════════════════════

Nodes: Users
────────────────────────────────────────────────
(:User {
  user_id: 'user_001',
  name: 'Alice',
  city: 'San Francisco',
  interests: ['hiking', 'coffee']
})

Relationships: Connections
────────────────────────────────────────────────
(user_001)-[:FRIEND]->(user_002)
(user_001)-[:FOLLOWS]->(user_500)
(user_001)-[:BLOCKS]->(user_999)
(user_001)-[:CLOSE_FRIEND {since: '2020-01-15'}]->(user_002)


Query: "Get user_001's friends"
────────────────────────────────────────────────
MATCH (u:User {user_id: 'user_001'})-[:FRIEND]->(friend)
RETURN friend.user_id

Graph traversal: O(degree) = O(338)
Time: <5ms


Query: "Friend suggestions (friends of friends)"
────────────────────────────────────────────────
MATCH (u:User {user_id: 'user_001'})-[:FRIEND]->(friend)
      -[:FRIEND]->(suggestion)
WHERE NOT (u)-[:FRIEND]->(suggestion)
AND NOT (u)-[:BLOCKS]->(suggestion)
AND u <> suggestion
RETURN DISTINCT suggestion.user_id, COUNT(*) as mutual_friends
ORDER BY mutual_friends DESC
LIMIT 10

This is a 2-hop traversal with filtering
In SQL: multiple self-joins (complex, slow)
In Graph DB: natural traversal (simple, fast)
Time: <20ms


Query: "Mutual friends"
────────────────────────────────────────────────
MATCH (u1:User {user_id: 'user_001'})-[:FRIEND]->(mutual)
      <-[:FRIEND]-(u2:User {user_id: 'user_002'})
RETURN mutual.user_id

Two-way traversal
SQL would need self-join on friends table
Graph DB: native operation
Time: <10ms
```

### Why Not PostgreSQL for Social Graph?

```
POSTGRESQL ATTEMPT:
════════════════════════════════════════════════════════

Friends table:
────────────────────────────────────────────────
user_id_1 │ user_id_2 │ relationship │ created_at
──────────────────────────────────────────────────────
user_001  │ user_002  │ friend       │ 2020-01-15
user_001  │ user_003  │ friend       │ 2020-03-20
user_002  │ user_001  │ friend       │ 2020-01-15


Query: "Get user_001's friends"
────────────────────────────────────────────────
SELECT user_id_2 FROM friends WHERE user_id_1 = 'user_001'
UNION
SELECT user_id_1 FROM friends WHERE user_id_2 = 'user_001'

Problem: Friendship is bidirectional, stored twice
Or: need UNION to check both directions


Query: "Friends of friends (2-hop)"
────────────────────────────────────────────────
SELECT DISTINCT f2.user_id_2
FROM friends f1
JOIN friends f2 ON f1.user_id_2 = f2.user_id_1
WHERE f1.user_id_1 = 'user_001'
AND f2.user_id_2 NOT IN (
  SELECT user_id_2 FROM friends WHERE user_id_1 = 'user_001'
)

→ Self-join (expensive)
→ Subquery for exclusion (expensive)
→ At 338 friends with 338 friends each: 114K rows scanned
→ Query time: 100-500ms (vs Neo4j 20ms)


Query: "How many hops to user_999?" (6 degrees of separation)
────────────────────────────────────────────────
WITH RECURSIVE path AS (
  SELECT user_id_1, user_id_2, 1 as hops
  FROM friends
  WHERE user_id_1 = 'user_001'
  
  UNION
  
  SELECT p.user_id_1, f.user_id_2, p.hops + 1
  FROM path p
  JOIN friends f ON p.user_id_2 = f.user_id_1
  WHERE p.hops < 6
)
SELECT MIN(hops) FROM path WHERE user_id_2 = 'user_999';

→ Recursive CTE (slow on large graphs)
→ Joins grow exponentially
→ Query time: seconds (unacceptable)
→ Graph DB: breadth-first search (native algorithm)
→ Graph DB time: <50ms


GRAPH DB WINS FOR:
────────────────────────────────────────────────
✓ Multi-hop traversals (friends of friends of friends)
✓ Path finding (shortest path between users)
✓ Community detection (find clusters of friends)
✓ Influence analysis (who has most connections)
✓ Recommendation algorithms (collaborative filtering)

All these are O(joins^N) in SQL
All these are O(traversal) in Graph DB
```

---

## The Hybrid Fan-Out Strategy

### Fan-Out on Write (Most Users)

```
REGULAR USER POSTS (338 friends):
════════════════════════════════════════════════════════

User_001 creates post
        │
        ▼
1. Write to Cassandra (source of truth):
INSERT INTO posts (author_id, post_id, content, timestamp)
VALUES ('user_001', 'post_abc', 'Coffee is great!', 1708945200)
        │
        ▼
2. Query Graph DB for friends:
MATCH (u:User {user_id: 'user_001'})-[:FRIEND]->(friend)
RETURN friend.user_id

Returns: [friend_001, friend_002, ..., friend_338]
        │
        ▼
3. Fan-out to all friends' Redis feeds (parallel):
ZADD feed:friend_001 1708945200 'post_abc'
ZADD feed:friend_002 1708945200 'post_abc'
...
ZADD feed:friend_338 1708945200 'post_abc'

338 Redis writes in parallel: <50ms
        │
        ▼
4. Also add to own feed (immediate consistency):
ZADD feed:user_001 1708945200 'post_abc'

User_001 refreshes → sees own post immediately


Total latency: <100ms
User sees "Post published" instantly
All 338 friends see post in their feed within 100ms
```

### Fan-Out on Read (Celebrities)

```
CELEBRITY POSTS (100M followers):
════════════════════════════════════════════════════════

Kim Kardashian creates post
        │
        ▼
Problem with fan-out on write:
100,000,000 × ZADD commands = 100M writes
Even at 100K writes/sec: 1000 seconds = 16 minutes
Unacceptable delay


Solution: Fan-out on READ
────────────────────────────────────────────────
1. Mark user as celebrity (followers > 1M threshold)
   
2. When celebrity posts:
   - Write to Cassandra (source of truth)
   - Do NOT fan-out to followers
   - Only add to celebrity's own feed
   
3. When follower opens app:
   - Generate feed hybrid:
   
   a) Get pre-computed feed from Redis (friends):
      ZREVRANGE feed:user_123 0 49
      
   b) Get list of celebrities user follows:
      MATCH (u:User {user_id: 'user_123'})-[:FOLLOWS]->(celeb:User)
      WHERE celeb.follower_count > 1000000
      RETURN celeb.user_id
      
   c) Fetch celebrity's recent posts (Cassandra):
      SELECT * FROM posts
      WHERE author_id IN ('kim_kardashian', 'taylor_swift')
      AND timestamp > NOW() - 7 DAYS
      ORDER BY timestamp DESC
      LIMIT 20
      
   d) Merge + rank:
      - Friends' posts (from Redis)
      - Celebrities' posts (from Cassandra)
      - Sort by ranking algorithm
      - Return top 50


Result:
────────────────────────────────────────────────
Fan-out on write: 0 writes (no fan-out)
Feed generation: 5ms (Redis) + 10ms (Cassandra) + 5ms (merge)
                = 20ms total
Still sub-500ms target ✓
No 16-minute delay ✓
```

---

## Complete Schema Architecture

```
CASSANDRA SCHEMA:
════════════════════════════════════════════════════════

Posts table:
────────────────────────────────────────────────────────────────────────────────
author_id │ timestamp  │ post_id    │ content                │ likes_count │ comments_count │ media_urls
───────────────────────────────────────────────────────────────────────────────────────────────────────────
user_001  │ 1708945200 │ post_abc   │ Just had best coffee!  │ 42          │ 5              │ [url1]
user_001  │ 1708945100 │ post_xyz   │ Morning run complete   │ 23          │ 2              │ []
user_002  │ 1708945300 │ post_def   │ New project launched!  │ 156         │ 34             │ [url2, url3]

PRIMARY KEY ((author_id), timestamp, post_id)
CLUSTERING ORDER BY (timestamp DESC)

Indexes:
  Secondary index on post_id (for single post lookup)
  
TTL: Posts older than 5 years auto-deleted


Likes table (counter column type):
────────────────────────────────────────────────
post_id    │ likes_count
───────────────────────────
post_abc   │ 42
post_xyz   │ 23

UPDATE likes SET likes_count = likes_count + 1
WHERE post_id = 'post_abc';

Atomic increment (no race conditions)


Comments table:
────────────────────────────────────────────────────────────────────────
post_id  │ timestamp  │ comment_id │ author_id │ content
────────────────────────────────────────────────────────────────────────────
post_abc │ 1708945250 │ cmt_001    │ user_002  │ Great coffee spot!
post_abc │ 1708945300 │ cmt_002    │ user_003  │ Where is this?

PRIMARY KEY ((post_id), timestamp, comment_id)


REDIS SCHEMA:
════════════════════════════════════════════════════════

User feed (sorted set):
────────────────────────────────────────────────
Key:   feed:user_001
Type:  Sorted Set
Score: Ranking score (combination of timestamp + engagement)
Value: post_id

ZADD feed:user_001 1708945200 'post_abc'
ZADD feed:user_001 1708945150 'post_xyz'
ZADD feed:user_001 1708945100 'post_def'

ZREVRANGE feed:user_001 0 49  ← top 50 posts
TTL: 7 days (older posts pruned)


Post cache (hash):
────────────────────────────────────────────────
Key:   post:post_abc
Type:  Hash
Value: Post details

HSET post:post_abc
  author_id "user_001"
  content "Just had best coffee!"
  timestamp "1708945200"
  likes_count "42"
  
HGETALL post:post_abc  ← fetch post details
TTL: 24 hours


Ranking score cache:
────────────────────────────────────────────────
Key:   score:post_abc:user_001
Value: Personalized ranking score for this user

SET score:post_abc:user_001 "0.87"
TTL: 1 hour

Recomputed periodically by ML model


Engagement metrics (for ranking):
────────────────────────────────────────────────
Key:   engagement:post_abc
Type:  Hash

HINCRBY engagement:post_abc likes 1
HINCRBY engagement:post_abc comments 1
HINCRBY engagement:post_abc shares 1

Used by ranking algorithm


GRAPH DATABASE SCHEMA (Neo4j):
════════════════════════════════════════════════════════

User nodes:
────────────────────────────────────────────────
(:User {
  user_id: 'user_001',
  name: 'Alice Johnson',
  city: 'San Francisco',
  interests: ['hiking', 'coffee', 'photography'],
  follower_count: 338,
  is_celebrity: false
})

(:User {
  user_id: 'kim_kardashian',
  name: 'Kim Kardashian',
  follower_count: 364000000,
  is_celebrity: true
})


Relationship types:
────────────────────────────────────────────────
(user_001)-[:FRIEND {since: '2020-01-15'}]->(user_002)
(user_001)-[:FOLLOWS]->(kim_kardashian)
(user_001)-[:BLOCKS]->(user_999)
(user_001)-[:CLOSE_FRIEND]->(user_002)

(:User)-[:FRIEND]->(:User)        // Symmetric relationship
(:User)-[:FOLLOWS]->(:User)       // Asymmetric (Twitter-style)
(:User)-[:BLOCKS]->(:User)        // Block user
(:User)-[:CLOSE_FRIEND]->(:User)  // Inner circle


Indexes:
────────────────────────────────────────────────
CREATE INDEX ON :User(user_id)
CREATE INDEX ON :User(is_celebrity)
CREATE INDEX ON :User(city)
```

---

## Complete Database Flow

```
FLOW 1: User Creates Post
════════════════════════════════════════════════════════

User_001 clicks "Post" with content: "Just had best coffee!"
        │
        ▼
POST /api/posts
{
  user_id: 'user_001',
  content: 'Just had best coffee!',
  media_urls: ['https://cdn.fb.com/img123.jpg']
}
        │
        ▼
STEP 1: Write to Cassandra (source of truth)
────────────────────────────────────────────────
post_id = generate_uuid()  // post_abc
timestamp = NOW()  // 1708945200

INSERT INTO posts (author_id, timestamp, post_id, content, media_urls)
VALUES ('user_001', 1708945200, 'post_abc', 'Just had best coffee!', 
        ['https://cdn.fb.com/img123.jpg']);

Cassandra write: <1ms


STEP 2: Check if user is celebrity
────────────────────────────────────────────────
Query Graph DB:
MATCH (u:User {user_id: 'user_001'})
RETURN u.follower_count, u.is_celebrity

Result: follower_count = 338, is_celebrity = false
→ Use fan-out on write


STEP 3: Get user's friends from Graph DB
────────────────────────────────────────────────
MATCH (u:User {user_id: 'user_001'})-[:FRIEND]->(friend)
RETURN friend.user_id

Result: [friend_001, friend_002, ..., friend_338]
Query time: <5ms


STEP 4: Fan-out to friends' feeds (Redis, parallel)
────────────────────────────────────────────────
Pipeline to Redis (batched for efficiency):

pipeline = redis.pipeline()
for friend_id in friends:
  pipeline.zadd(f'feed:{friend_id}', {post_id: timestamp})
  pipeline.expire(f'feed:{friend_id}', 604800)  # 7 days
pipeline.execute()

338 writes batched into one round trip
Time: <30ms


STEP 5: Add to own feed (immediate consistency)
────────────────────────────────────────────────
ZADD feed:user_001 1708945200 'post_abc'

User_001 sees own post immediately


STEP 6: Cache post details in Redis
────────────────────────────────────────────────
HSET post:post_abc
  author_id "user_001"
  content "Just had best coffee!"
  timestamp "1708945200"
  likes_count "0"
  comments_count "0"
  media_urls '["https://cdn.fb.com/img123.jpg"]'
  
EXPIRE post:post_abc 86400  # 24 hours

Other users can fetch quickly


STEP 7: Publish event to Kafka (async notifications)
────────────────────────────────────────────────
Producer.send(
  topic="post_events",
  key=user_id,
  value={
    "event_type": "POST_CREATED",
    "post_id": "post_abc",
    "author_id": "user_001",
    "timestamp": 1708945200
  }
)

Downstream consumers:
- Notification service (notify friends)
- ML ranking service (compute personalized scores)
- Analytics service (track user activity)


Response to user:
{
  success: true,
  post_id: 'post_abc',
  message: 'Post published!'
}

Total latency: <50ms
```

```
FLOW 2: User Opens Feed
════════════════════════════════════════════════════════

User_002 opens Facebook app
        │
        ▼
GET /api/feed?user_id=user_002
        │
        ▼
STEP 1: Check if user follows any celebrities
────────────────────────────────────────────────
Query Graph DB:
MATCH (u:User {user_id: 'user_002'})-[:FOLLOWS]->(celeb:User)
WHERE celeb.is_celebrity = true
RETURN celeb.user_id

Result: ['kim_kardashian', 'taylor_swift']
Query time: <5ms


STEP 2: Get pre-computed feed from Redis
────────────────────────────────────────────────
ZREVRANGE feed:user_002 0 49 WITHSCORES

Returns:
[
  (post_abc, 1708945200),
  (post_xyz, 1708945100),
  (post_def, 1708945000),
  ...
]  (50 post IDs with scores)

Query time: <5ms


STEP 3: Fetch celebrity posts (fan-out on read)
────────────────────────────────────────────────
Query Cassandra for each celebrity:

SELECT post_id, timestamp, content, likes_count
FROM posts
WHERE author_id = 'kim_kardashian'
AND timestamp > 1708858800  # last 24 hours
ORDER BY timestamp DESC
LIMIT 10

UNION

SELECT post_id, timestamp, content, likes_count
FROM posts
WHERE author_id = 'taylor_swift'
AND timestamp > 1708858800
ORDER BY timestamp DESC
LIMIT 10

Result: 20 celebrity posts
Query time: <10ms (parallel)


STEP 4: Merge and rank posts
────────────────────────────────────────────────
Combined posts: 50 (friends) + 20 (celebrities) = 70 posts

Ranking algorithm (simplified):
────────────────────────────────────────────────
For each post:
  base_score = timestamp  (recency)
  
  engagement_score = likes_count * 0.3 
                   + comments_count * 0.5
                   + shares_count * 0.7
  
  relationship_score = query_graph_db(
    "strength of user_002's connection to post author"
  )
  
  final_score = base_score * 0.5 
              + engagement_score * 0.3
              + relationship_score * 0.2

Sort by final_score descending
Return top 50

Ranking time: <20ms


STEP 5: Fetch post details (batch)
────────────────────────────────────────────────
Top 50 post IDs: [post_abc, post_xyz, ...]

Check Redis cache first:
MGET post:post_abc post:post_xyz post:post_def ...

Cache hits: 45 posts (90%)
Cache misses: 5 posts (10%)

For cache misses, query Cassandra:
SELECT * FROM posts WHERE post_id IN ('post_miss1', 'post_miss2', ...)

Backfill Redis:
HSET post:post_miss1 ... 

Total fetch time: <15ms


STEP 6: Hydrate with user data
────────────────────────────────────────────────
For each post, need author info:
- Name
- Profile pic URL
- Verification status

Batch query user service (microservice):
GET /api/users/batch?ids=user_001,user_002,user_003

Returns: User details for all authors

Time: <10ms


STEP 7: Return feed to client
────────────────────────────────────────────────
Response:
{
  posts: [
    {
      post_id: 'post_abc',
      author: {user_id: 'user_001', name: 'Alice', ...},
      content: 'Just had best coffee!',
      timestamp: 1708945200,
      likes_count: 42,
      comments_count: 5,
      media_urls: [...]
    },
    ...
  ],
  next_cursor: 'cursor_xyz'  // for pagination
}

Total latency: 5+5+10+20+15+10 = 65ms
Well under 500ms target ✓
```

```
FLOW 3: User Likes Post
════════════════════════════════════════════════════════

User_002 clicks "Like" on post_abc
        │
        ▼
POST /api/posts/post_abc/like
{
  user_id: 'user_002'
}
        │
        ▼
STEP 1: Increment likes in Cassandra
────────────────────────────────────────────────
UPDATE posts
SET likes_count = likes_count + 1
WHERE author_id = 'user_001'
AND post_id = 'post_abc';

Cassandra counter: atomic increment
No race conditions
Time: <5ms


STEP 2: Update Redis cache
────────────────────────────────────────────────
HINCRBY post:post_abc likes_count 1

Keep cache consistent
Time: <1ms


STEP 3: Record like in likes table
────────────────────────────────────────────────
INSERT INTO likes (post_id, user_id, timestamp)
VALUES ('post_abc', 'user_002', NOW());

For "Show who liked this" feature
Time: <5ms


STEP 4: Publish event to Kafka
────────────────────────────────────────────────
Producer.send(
  topic="engagement_events",
  value={
    "event_type": "POST_LIKED",
    "post_id": "post_abc",
    "user_id": "user_002",
    "timestamp": NOW()
  }
)

Downstream consumers:
- Notification service (notify post author)
- ML ranking service (update engagement signals)
- Analytics service


Response to user:
{
  success: true,
  new_likes_count: 43
}

Total latency: <15ms
User sees heart icon fill immediately
```

```
FLOW 4: Celebrity Posts (Fan-Out on Read)
════════════════════════════════════════════════════════

Kim Kardashian posts (364M followers)
        │
        ▼
STEP 1: Write to Cassandra
────────────────────────────────────────────────
INSERT INTO posts (author_id, post_id, content, timestamp)
VALUES ('kim_kardashian', 'post_celeb', 'New product launch!', NOW());

Time: <1ms


STEP 2: Add to own feed only (no fan-out)
────────────────────────────────────────────────
ZADD feed:kim_kardashian 1708945200 'post_celeb'

NO fan-out to 364M followers
Avoids 364M Redis writes
Time: <1ms


STEP 3: Publish event
────────────────────────────────────────────────
Kafka event: POST_CREATED (celebrity)

Notification service batches:
- Send push to followers in batches (controlled rate)
- 364M pushes over 10 minutes (600K/second)


STEP 4: Follower opens app
────────────────────────────────────────────────
User_002 (follows Kim) opens app

Feed generation:
1. Get pre-computed feed (friends): 50 posts from Redis
2. Check: Does user follow celebrities?
   → YES: kim_kardashian
3. Fetch Kim's recent posts:
   SELECT * FROM posts
   WHERE author_id = 'kim_kardashian'
   AND timestamp > NOW() - 24 HOURS
   LIMIT 10
4. Merge: 50 friend posts + 10 Kim posts
5. Rank: Kim's post likely scores high (viral engagement)
6. Return top 50

User sees Kim's post in feed
Total latency: still <100ms ✓
No 16-minute delay ✓
```

```
FLOW 5: Edit Post (Consistency Challenge)
════════════════════════════════════════════════════════

User_001 edits post_abc:
"Just had best coffee!" → "Just had the BEST coffee ever! ☕"
        │
        ▼
PUT /api/posts/post_abc
{
  content: 'Just had the BEST coffee ever! ☕'
}
        │
        ▼
STEP 1: Update in Cassandra
────────────────────────────────────────────────
UPDATE posts
SET content = 'Just had the BEST coffee ever! ☕',
    edited_at = NOW()
WHERE author_id = 'user_001'
AND post_id = 'post_abc';


STEP 2: Invalidate Redis cache
────────────────────────────────────────────────
DEL post:post_abc

Next fetch will reload from Cassandra


STEP 3: Publish edit event to Kafka
────────────────────────────────────────────────
Kafka event: POST_EDITED

Consumers:
- Real-time sync service (WebSocket push to viewing users)
- Cache invalidation service


STEP 4: Real-time update to viewing users
────────────────────────────────────────────────
WebSocket broadcast:
"Post post_abc updated, please refresh"

Users currently viewing post see update in real-time
```

```
FLOW 6: Delete Post (Cascade)
════════════════════════════════════════════════════════

User_001 deletes post_abc
        │
        ▼
DELETE /api/posts/post_abc
        │
        ▼
STEP 1: Mark as deleted in Cassandra (soft delete)
────────────────────────────────────────────────
UPDATE posts
SET deleted = true,
    deleted_at = NOW()
WHERE author_id = 'user_001'
AND post_id = 'post_abc';

Keep for audit trail
Don't show in feeds


STEP 2: Remove from all feeds (Redis)
────────────────────────────────────────────────
Get friends:
MATCH (u:User {user_id: 'user_001'})-[:FRIEND]->(friend)
RETURN friend.user_id

For each friend:
ZREM feed:friend_001 'post_abc'
ZREM feed:friend_002 'post_abc'
...

338 removes in parallel: <30ms


STEP 3: Delete from Redis cache
────────────────────────────────────────────────
DEL post:post_abc
DEL engagement:post_abc


STEP 4: Cascade delete likes and comments
────────────────────────────────────────────────
DELETE FROM likes WHERE post_id = 'post_abc';
DELETE FROM comments WHERE post_id = 'post_abc';


STEP 5: Publish delete event
────────────────────────────────────────────────
Kafka: POST_DELETED

Downstream consumers handle cleanup
```

---

## Tradeoffs vs Other Databases

```
┌────────────────────────┬───────────────┬──────────────┬──────────────┬──────────────┐
│                        │ THIS ARCH     │ POSTGRES ALL │ MONGO ALL    │ MYSQL ALL    │
├────────────────────────┼───────────────┼──────────────┼──────────────┼──────────────┤
│ Write throughput       │ 1M+/sec ✓     │ 10K/sec ✗    │ 50K/sec      │ 10K/sec ✗    │
│ Read latency (feed)    │ <50ms ✓       │ 500ms+ ✗     │ 100ms        │ 500ms+ ✗     │
│ Graph queries          │ Neo4j ✓       │ Recursive ✗  │ Manual ✗     │ Recursive ✗  │
│ Pre-computed feeds     │ Redis ✓       │ Materialize  │ Manual       │ Materialize  │
│ Celebrity fan-out      │ Hybrid ✓      │ Impossible ✗ │ Impossible ✗ │ Impossible ✗ │
│ Horizontal scaling     │ Native ✓      │ Sharding     │ Native ✓     │ Sharding     │
│ Operational complexity │ HIGH          │ LOW          │ MEDIUM       │ LOW          │
│ Cost at FB scale       │ HIGH          │ Impossible   │ HIGH         │ Impossible   │
│ Consistency            │ Eventual      │ Strong ✓     │ Eventual     │ Strong ✓     │
└────────────────────────┴───────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **Cassandra stores posts partitioned by author_id because Facebook's write-heavy workload (4,000 posts/second creating 1.3M fan-out writes/second) requires append-only sequential writes that Cassandra handles at 100K+ writes/second per node versus PostgreSQL's 10K/second bottlenecked by B-tree index maintenance and row locking — Redis stores pre-computed feeds as sorted sets (ZADD with timestamp scores) so opening the app queries one key returning 50 post IDs in 5ms instead of querying Cassandra 338 times for each friend's posts taking 1,690ms, enabling sub-500ms feed loads even with 5,000 friends — Graph databases optimize social connections because queries like "friend suggestions from friends-of-friends who aren't my friends" are 2-hop graph traversals that Neo4j completes in 20ms using native adjacency lists versus PostgreSQL's self-join recursive CTEs taking 500ms scanning 114K rows — the hybrid fan-out strategy fans out on write for regular users (338 Redis writes in 50ms) but fans out on read for celebrities (zero writes, fetch celebrity posts on-demand from Cassandra during feed generation) preventing Kim Kardashian's 364 million followers from causing 16-minute write delays, while ensuring regular users' friends see posts instantly within 100ms through parallel Redis fan-out batched into single pipeline commands — immediate self-consistency is achieved by synchronously writing to the author's own Redis feed before returning success so users see their posts instantly, while friend propagation happens asynchronously in parallel without blocking the response, and post edits/deletes cascade to all 338 friend feeds through Redis ZREM commands completing in 30ms total maintaining eventual consistency across the social graph.**

Would you like me to explain how the ML ranking algorithm computes personalized feed scores using collaborative filtering and recency decay, or how the real-time notification system uses WebSockets and Redis Pub/Sub to push updates to users currently viewing a post when someone likes or comments on it?
 