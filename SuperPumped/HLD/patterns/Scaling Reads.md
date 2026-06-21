# Scaling Reads Pattern

## Overview
Pattern for handling high-volume read traffic as applications grow from hundreds to millions of users. Read traffic typically grows much faster than write traffic (10:1 to 100:1 ratio).

---

## When to Use This Pattern
- **Social media feeds** - Millions viewing posts, few posting
- **E-commerce product pages** - Many browse, few purchase
- **News websites** - Heavy read traffic
- **Video streaming metadata** - View counts, recommendations
- **Search/browse features** - Product catalogs, user listings
- **Analytics dashboards** - Read-heavy reporting
- **Content delivery** - Blogs, documentation, media

---

## Why Reads Become a Bottleneck

### Typical Read-to-Write Ratios

**Instagram Example**:
```
User opens app:
- Load 20 photos → 20 reads for image metadata
- Each photo → read user info, likes count, comments
- Total: ~100 database queries
- User posts 1 photo per day → 1 write

Read:Write ratio = 100:1 (per day even higher)
```

**E-commerce Example**:
```
100 users browse product page → 100 reads
1 user purchases → 1 write

Read:Write ratio = 100:1
```

**News Website**:
```
1 million page views → 1 million+ reads
10 new articles published → 10 writes

Read:Write ratio = 100,000:1
```

---

## Natural Progression of Solutions

### Level 1: Optimize Within Single Database
### Level 2: Add Read Replicas (Horizontal Scaling)
### Level 3: Add External Caching (Redis, CDN)

---

## Level 1: Database Read Optimization

### 1. Indexing

**How It Works**:
- B-tree index for faster lookups
- Trade-off: Faster reads, slower writes
- Index on frequently queried columns

**Without Index**:
```
SELECT * FROM products WHERE category = 'electronics';

Full table scan: O(n) - checks every row
1 million products → 1 million rows scanned
Query time: 2 seconds
```

**With Index**:
```
CREATE INDEX idx_category ON products(category);

SELECT * FROM products WHERE category = 'electronics';

Index seek: O(log n) - binary tree traversal
1 million products → ~20 comparisons
Query time: 10ms
```

**When to Use**:
- Columns in WHERE clauses
- Columns in JOIN conditions
- Columns in ORDER BY
- Foreign keys

**What to Index**:
```
Common patterns:
- user_id (for user-specific queries)
- created_at (for time-range queries)
- status (for filtering active/inactive)
- category_id (for category browsing)

Composite indexes for multiple columns:
CREATE INDEX idx_user_created ON posts(user_id, created_at);
-- Good for: WHERE user_id = X ORDER BY created_at
```

**Index Trade-offs**:
- **Pros**: Dramatically faster reads (1000x improvement possible)
- **Cons**: Slower writes (index must be updated), more storage

**Include in Design**:
- "Create index on user_id for fast user queries"
- "Composite index on (category, price) for filtering"
- "Indexes speed up reads by 100-1000x"
- "Monitor slow query log to identify missing indexes"

---

### 2. Denormalization

**How It Works**:
- Store redundant data to avoid JOINs
- Pre-compute expensive aggregations
- Trade-off: Duplicate data, faster reads, complex writes

**Normalized (Multiple JOINs)**:
```
Tables:
- users (id, name)
- posts (id, user_id, content)
- likes (post_id, user_id)

Query for feed:
SELECT p.content, u.name, COUNT(l.id) as like_count
FROM posts p
JOIN users u ON p.user_id = u.id
LEFT JOIN likes l ON p.id = l.post_id
GROUP BY p.id
ORDER BY p.created_at DESC
LIMIT 20;

-- 3 JOINs + aggregation = slow
```

**Denormalized (No JOINs)**:
```
posts table:
- id
- user_id
- user_name (denormalized from users)
- content
- like_count (denormalized from likes)
- created_at

Query for feed:
SELECT id, user_name, content, like_count
FROM posts
ORDER BY created_at DESC
LIMIT 20;

-- Single table, no JOINs = very fast
```

**What to Denormalize**:
- User names in posts (avoid JOIN with users)
- Aggregate counts (likes, comments, views)
- Frequently accessed related data

**Synchronization**:
```
When user changes name:
1. Update users table
2. Update all posts with that user_id (denormalized name)
3. Use async job if too many posts

When post gets liked:
1. Insert into likes table
2. Increment like_count in posts table (atomic)
```

**Trade-offs**:
- **Pros**: Much faster reads, single-table queries
- **Cons**: Data duplication, complex update logic, consistency challenges

**Include in Design**:
- "Denormalize user_name into posts table to avoid JOIN"
- "Store like_count in posts for fast access"
- "Update denormalized data asynchronously for non-critical fields"
- "Accept eventual consistency for counts"

---

### 3. Query Optimization

**Techniques**:

**Select Only Needed Columns**:
```
Bad:  SELECT * FROM products;  -- Fetches all columns
Good: SELECT id, name, price FROM products;  -- Only needed data
```

**Limit Results**:
```
Bad:  SELECT * FROM posts ORDER BY created_at DESC;  -- All posts
Good: SELECT * FROM posts ORDER BY created_at DESC LIMIT 20;  -- Paginated
```

**Avoid N+1 Queries**:
```
Bad (N+1 problem):
posts = SELECT * FROM posts LIMIT 20;
for each post:
    user = SELECT * FROM users WHERE id = post.user_id;
-- 1 query for posts + 20 queries for users = 21 queries

Good (JOIN or IN clause):
SELECT p.*, u.name FROM posts p
JOIN users u ON p.user_id = u.id
LIMIT 20;
-- Single query with JOIN

Or:
posts = SELECT * FROM posts LIMIT 20;
user_ids = [post.user_id for post in posts];
users = SELECT * FROM users WHERE id IN (user_ids);
-- 2 queries total
```

**Use Covering Indexes**:
```
Query: SELECT user_id, created_at FROM posts WHERE status = 'published';

CREATE INDEX idx_covering ON posts(status, user_id, created_at);

-- Index contains all needed columns, no table access needed
```

**Include in Design**:
- "Optimize queries to select only needed columns"
- "Use LIMIT for pagination, avoid fetching all results"
- "Avoid N+1 queries with JOINs or IN clauses"
- "Use EXPLAIN to analyze query plans"

---

## Level 2: Horizontal Scaling with Read Replicas

### How Read Replicas Work

**Architecture**:
```
Primary Database (Master)
    ↓ (replication)
    ├→ Read Replica 1
    ├→ Read Replica 2
    └→ Read Replica 3

Writes → Primary only
Reads  → Load balanced across replicas
```

**Replication Process**:
```
1. Write goes to Primary
2. Primary commits to its WAL (Write-Ahead Log)
3. Primary sends changes to Replicas
4. Replicas apply changes to their data
5. Replicas acknowledge (async or sync)
```

**Read Path**:
```
Client → Read request
Load Balancer → Route to any replica (round-robin)
Replica → Return data
```

**Write Path**:
```
Client → Write request
App Server → Route to Primary only
Primary → Write + replicate to replicas
```

---

### Implementation in Design

**Configuration**:
```
Application layer:
- Connection pool for Primary (writes)
- Connection pool for Replicas (reads)

routing logic:
if operation = INSERT/UPDATE/DELETE:
    route to Primary
else if operation = SELECT:
    route to Replica (round-robin)
```

**Number of Replicas**:
- Start with 2-3 replicas
- Monitor read load on each replica
- Add more as traffic grows
- 5-10 replicas common for high-traffic apps

**When to Use**:
- Read:Write ratio > 10:1
- Database CPU maxed out on reads
- Simple scaling without sharding

**Pros**:
- Horizontal scaling for reads
- Simple to implement
- Built-in to most databases (MySQL, PostgreSQL)
- Improves availability (failover to replica)

**Cons**:
- Replication lag (eventual consistency)
- Writes still bottleneck on single primary
- More complex application routing

---

### Replication Lag

**What It Is**:
- Time between write to primary and visibility on replica
- Usually 10ms - 1 second
- Can be higher during load or network issues

**Implications**:
```
1. User posts comment (write to primary)
2. User refreshes page (read from replica)
3. Comment not yet replicated → user doesn't see own comment!

This is "read-your-own-writes" problem
```

**Solutions**:

**1. Read from Primary for Recent Writes**:
```
After user posts comment:
- Set flag or timestamp in session
- For next N seconds, route that user's reads to primary
- After lag period, resume reading from replicas
```

**2. Check Replication Position**:
```
Primary returns: log_position = 12345
App stores this in session
For next read, query replica with: replication_position >= 12345
If replica not caught up, wait or read from primary
```

**3. Accept Eventual Consistency**:
```
For some features (view counts, trending), eventual consistency OK
Show message: "Posted! May take a moment to appear"
```

**Include in Design**:
- "Use 3 read replicas for horizontal scaling"
- "Route reads to replicas, writes to primary"
- "Replication lag ~100ms, acceptable for most reads"
- "Read from primary for user's own writes to avoid consistency issues"
- "Monitor lag, alert if > 1 second"

---

## Level 3: External Caching

### 1. Application-Level Caching (Redis, Memcached)

**How It Works**:
- Store frequently accessed data in memory (Redis)
- Check cache before database
- Much faster: RAM access ~1ms vs DB ~10-100ms

**Architecture**:
```
Client → App Server → Check Redis cache
                       ↓ (cache hit)
                       Return cached data (1ms)
                       ↓ (cache miss)
                       Query database (50ms)
                       Store result in cache
                       Return data
```

**Cache-Aside Pattern** (most common):
```
function getProduct(productId):
    // Check cache first
    data = redis.get("product:" + productId)
    
    if data != null:
        // Cache hit
        return data
    
    // Cache miss - query database
    data = db.query("SELECT * FROM products WHERE id = " + productId)
    
    // Store in cache with expiration
    redis.setex("product:" + productId, 3600, data)  // 1 hour TTL
    
    return data
```

**What to Cache**:
- **Hot data**: Frequently accessed items (popular products, trending posts)
- **Expensive queries**: Complex JOINs, aggregations
- **Rarely changing**: Product info, user profiles
- **Not user-specific**: Same data for many users

**What NOT to Cache**:
- Frequently changing data (stock prices, live scores)
- User-specific data with low reuse
- Small datasets (overhead not worth it)

**TTL (Time-To-Live)**:
```
Static content: 1 day - 1 week
Semi-static: 1 hour - 1 day
Dynamic: 1 minute - 1 hour

Examples:
- Product details: 1 hour
- User profile: 15 minutes
- Hot items list: 5 minutes
```

---

### Cache Invalidation Strategies

**1. Time-Based (TTL)**:
```
redis.setex("product:123", 3600, data)  // Expires in 1 hour

Pros: Simple, automatic cleanup
Cons: Stale data until expiration
```

**2. Event-Based (Active Invalidation)**:
```
When product updated:
1. Update database
2. Delete from cache: redis.del("product:123")
3. Next read will cache miss → fetch fresh data

Pros: Always fresh data
Cons: More complex, need to track all cache keys
```

**3. Write-Through**:
```
When product updated:
1. Update database
2. Update cache: redis.set("product:123", new_data)

Pros: Cache always up-to-date
Cons: Every write = 2 operations
```

**4. Hybrid**:
```
Use TTL + event-based:
- Default TTL for safety (1 hour)
- Invalidate on updates for freshness
- Best of both worlds
```

**Include in Design**:
- "Cache hot products in Redis with 1-hour TTL"
- "Cache-aside pattern: check cache, then database"
- "Invalidate cache on product updates"
- "Reduces database load by 70-90%"

---

### Cache Challenges & Solutions

**Challenge 1: Cache Stampede (Thundering Herd)**

**Problem**:
```
Popular item expires from cache
100 concurrent requests → all cache miss
All 100 query database simultaneously
Database overloaded
```

**Solution - Stale-While-Revalidate**:
```
1. Cache entry has soft + hard expiration
2. After soft expiry, return stale data
3. Single background request refreshes cache
4. Other requests get stale data immediately

Entry: {data: "...", soft_exp: T+1hr, hard_exp: T+2hr}

At T+1hr:
- First request triggers async refresh, returns stale data
- Other requests return stale data
- Background refresh updates cache
```

**Solution - Probabilistic Early Expiration**:
```
Refresh cache before expiration with probability
Probability increases as expiration nears

Spreads cache refreshes over time
Avoids all keys expiring simultaneously
```

---

**Challenge 2: Hot Key Problem**

**Problem**:
```
Single key (celebrity profile) gets millions of requests
Single Redis instance can't handle load
Becomes bottleneck
```

**Solution - Local In-Memory Cache**:
```
Architecture:
Client → App Server (local cache) → Redis → Database

function getCelebrity(id):
    // Check local cache (in-app-server memory)
    if localCache.has(id):
        return localCache.get(id)
    
    // Check Redis
    data = redis.get(id)
    
    // Store in local cache for 1 minute
    localCache.set(id, data, 60)
    
    return data

Now: Millions of requests served from app-server memory
Only occasional requests go to Redis
```

**Solution - Cache Replication**:
```
Replicate hot keys across multiple Redis instances
Load balance reads across replicas
```

---

**Challenge 3: Cache vs DB Consistency**

**Problem**:
```
Race condition:
Thread A: Read DB (v1) → slow network
Thread B: Update DB (v2) → Update cache (v2)
Thread A: Write cache (v1) ← stale data!

Cache now has old data v1, DB has v2
```

**Solution - Delete Instead of Update**:
```
When updating:
1. Update database
2. DELETE from cache (don't update)
3. Next read = cache miss → fetch fresh data from DB

Avoids race conditions
```

---

### 2. Content Delivery Network (CDN)

**How It Works**:
- Distributed edge servers worldwide
- Cache static content (images, videos, CSS, JS)
- Serve from nearest geographic location
- Reduces latency + origin server load

**Architecture**:
```
User (USA) → CDN Edge (USA) → Return cached image (50ms)
                            ↓ (cache miss)
                            Origin Server (Europe) → Return image (200ms)
                            Store in CDN cache
```

**What to Cache on CDN**:
- **Static assets**: Images, videos, CSS, JS, fonts
- **API responses**: Product listings, public profiles
- **Generated content**: Thumbnails, resized images

**Cache Control Headers**:
```
HTTP Response Headers:
Cache-Control: public, max-age=86400
-- Cache for 1 day (86400 seconds)
-- "public" = can be cached by CDN

Cache-Control: private, max-age=3600
-- Cache for 1 hour
-- "private" = user-specific, cache only in browser

Cache-Control: no-cache
-- Always revalidate with origin

ETag: "33a64df5"
-- Used for conditional requests
-- If-None-Match: "33a64df5" → 304 Not Modified
```

**Purging CDN Cache**:
```
When content updated:
- API call to CDN provider to purge/invalidate
- CDN fetches fresh content on next request

Purge strategies:
- Purge specific URL: /products/123.jpg
- Purge by pattern: /products/*
- Versioned URLs: /products/123.jpg?v=2 (new URL = new cache)
```

**Include in Design**:
- "Use CDN (CloudFlare, CloudFront) for images and static assets"
- "Cache-Control: max-age=86400 for product images"
- "Reduces origin server load by 80%"
- "Improves latency: CDN edge <50ms vs origin >200ms"
- "Purge CDN cache on content updates"

---

## Complete Design Example: Social Media Feed

### Requirements:
- 1 million users
- Each user loads feed → 100 database queries
- Read:Write ratio = 100:1
- Need fast page loads (<200ms)

### Design:

**Level 1: Database Optimization**

```
1. Indexing:
CREATE INDEX idx_user_created ON posts(user_id, created_at);
-- For user timeline queries

CREATE INDEX idx_created ON posts(created_at);
-- For global feed

2. Denormalization:
posts table:
- Add user_name (avoid JOIN with users)
- Add like_count (avoid COUNT aggregation)
- Add comment_count (avoid COUNT aggregation)

3. Query Optimization:
SELECT id, content, user_name, like_count, created_at
FROM posts
WHERE user_id IN (following_user_ids)
ORDER BY created_at DESC
LIMIT 20;
```

**Level 2: Read Replicas**

```
Setup:
- 1 Primary database (writes)
- 5 Read replicas (reads)

Routing:
- Feed queries → Read replicas
- Post creation, likes → Primary

Handles replication lag:
- After user posts, read from primary for 5 seconds
- Then route to replicas
```

**Level 3: Caching**

```
Redis Caching:

1. User Feed Cache:
Key: "feed:user:{user_id}"
Value: JSON array of post IDs
TTL: 5 minutes

function getUserFeed(userId):
    feed = redis.get("feed:user:" + userId)
    if feed:
        return feed
    
    feed = db.query(...)  // Read from replica
    redis.setex("feed:user:" + userId, 300, feed)
    return feed

2. Post Detail Cache:
Key: "post:{post_id}"
Value: Post JSON
TTL: 1 hour

3. Hot Posts Cache:
Key: "hot:posts"
Value: Trending posts
TTL: 2 minutes

Invalidation:
- New post → Invalidate user's followers' feeds
- Like/comment → Invalidate post detail cache
```

**CDN**:

```
- All images: https://cdn.example.com/images/{id}.jpg
- Cache-Control: max-age=604800 (1 week)
- Versioned URLs for profile pics: /avatar/{user_id}/v{version}.jpg
```

**Results**:
```
Before:
- Every feed load → 100 DB queries → 2 seconds

After:
- 90% cache hit rate
- Cache hit: Redis → 10ms
- Cache miss: Replica → 100ms
- Images from CDN: 50ms
- Total page load: <200ms

Database load:
- Before: 100M queries/day
- After: 10M queries/day (90% reduction)
```

---

## Monitoring & Metrics

### Key Metrics to Track:

**Database**:
- Query latency (p50, p95, p99)
- Slow query count
- Connection pool usage
- Replication lag

**Cache**:
- Hit rate (cache hits / total requests)
- Miss rate
- Eviction rate
- Memory usage

**Application**:
- Page load time
- API response time
- Error rate

**Alerting**:
```
- Database CPU > 80% → Consider more replicas
- Replication lag > 1 second → Investigate
- Cache hit rate < 70% → Improve caching strategy
- Slow queries > 1 second → Add indexes or optimize
```

---

## Interview Tips

### Always Mention
1. **Natural progression**: "Start with indexes, then replicas, then caching"
2. **Read:Write ratio**: "Understand the ratio to justify investment"
3. **Specific numbers**: "10ms cache vs 100ms database vs 1s cold query"
4. **Monitoring**: "Track cache hit rate, replication lag, query latency"

### Deep Dive Topics
- **Cache invalidation**: "Two hard problems in CS: naming and cache invalidation"
- **Replication lag**: Read-your-own-writes problem and solutions
- **Hot keys**: How to handle celebrity profiles or viral content
- **N+1 queries**: Common performance killer

### Red Flags to Avoid
- Jumping to caching without database optimization
- Not discussing cache invalidation
- Ignoring replication lag implications
- Over-complicating with premature optimization

### Good Answers Include
- "Instagram has 100:1 read:write ratio, justifies heavy read optimization"
- "Index on (user_id, created_at) for efficient feed queries"
- "3 read replicas, route reads there, writes to primary"
- "Cache hot posts in Redis with 5-min TTL, invalidate on updates"
- "CDN for images reduces origin load by 80%"
- "Monitor cache hit rate - target >80% for hot data"

---

## Example Use Cases Summary

| Use Case | Read:Write | Solution | Key Technique |
|----------|------------|----------|---------------|
| Social Feed | 100:1 | All levels | Denormalization + Replicas + Redis + CDN |
| E-commerce Product | 1000:1 | All levels | Aggressive caching, CDN for images |
| News Website | 10000:1 | Mostly caching | Heavy CDN, Redis for articles |
| User Profile Edit | 10:1 | Database optimization | Indexes, occasional cache |
| Analytics Dashboard | 100:1 | Replicas + Cache | Pre-computed aggregations |
| Video Metadata | 10000:1 | All levels | CDN for thumbnails, Redis for metadata |
