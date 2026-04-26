
## The Core Problem Bitly Solves

On the surface URL shortening seems simple — map short code to long URL. But consider the real constraints:

```
Bitly scale reality:
────────────────────────────────────────────────
Daily shortened URLs:      500 million+
Daily redirects (clicks):  10 billion+ (20,000 clicks/second sustained)
Peak redirects:            100,000+ clicks/second
Total URLs in system:      50 billion+
Read/Write ratio:          99:1 (1% creates, 99% redirects)

Requirements:
→ Short codes must be globally unique (no collisions)
→ Short codes must be short (6-7 characters ideal)
→ Redirect latency <50ms (critical user experience)
→ High availability (links can't break)
→ Click analytics (track every click)
→ Custom short codes (bit.ly/mycampaign)
→ Link expiration (temporary URLs)
→ Geographic analytics (where are clicks coming from)
→ Real-time click counting
```

This combination of **unique ID generation + extreme read-heavy workload + low-latency redirects + massive analytics writes** is what forces this specific architecture.

---

## Why PostgreSQL/MySQL for Primary Storage?

### The Short Code Uniqueness Problem

```
CORE REQUIREMENT: GLOBALLY UNIQUE SHORT CODES
════════════════════════════════════════════════════════

When user creates bit.ly/abc123:
- Must check if "abc123" already exists
- If exists: reject or generate new code
- If not: insert and claim it
- No two users can have same short code

This requires:
1. Unique constraint enforcement
2. Atomic check-and-insert
3. ACID transactions

PostgreSQL/MySQL are PERFECT for this
```

### PostgreSQL Schema

```
POSTGRESQL SCHEMA:
════════════════════════════════════════════════════════

URLs table:
────────────────────────────────────────────────────────────────────────────────
short_code │ long_url                        │ user_id  │ created_at          │ expires_at │ clicks
───────────────────────────────────────────────────────────────────────────────────────────────────────────────────
abc123     │ https://example.com/very/lon... │ user_001 │ 2024-02-26 10:00:00 │ NULL       │ 15420
xyz789     │ https://another-site.com/pa...  │ user_002 │ 2024-02-26 10:05:00 │ 2024-03-26 │ 892
custom1    │ https://mysite.com/campaign     │ user_003 │ 2024-02-26 10:10:00 │ NULL       │ 50231

Columns:
  short_code: VARCHAR(10) PRIMARY KEY  ← 6-10 characters
  long_url: TEXT NOT NULL  ← can be very long (2000+ chars)
  user_id: UUID REFERENCES users(user_id)
  created_at: TIMESTAMP WITH TIME ZONE DEFAULT NOW()
  expires_at: TIMESTAMP WITH TIME ZONE  ← NULL = never expires
  clicks: BIGINT DEFAULT 0  ← cached count (not real-time)
  is_active: BOOLEAN DEFAULT true
  is_custom: BOOLEAN DEFAULT false  ← user-chosen vs generated

Indexes:
  PRIMARY KEY (short_code)  ← Unique constraint + fast lookup
  INDEX (user_id, created_at DESC)  ← "Show my recent short URLs"
  INDEX (created_at) WHERE expires_at IS NOT NULL  ← Cleanup job
  
Constraints:
  UNIQUE (short_code)  ← Critical: prevents duplicates
  CHECK (LENGTH(short_code) >= 6 AND LENGTH(short_code) <= 10)


Users table:
────────────────────────────────────────────────────────────────────────
user_id │ email              │ plan      │ created_at          │ links_created
────────────────────────────────────────────────────────────────────────────────
user_001│ john@company.com   │ premium   │ 2023-01-15 00:00:00 │ 1523
user_002│ alice@startup.com  │ free      │ 2023-06-20 00:00:00 │ 45

Tracks user quotas:
- Free tier: 100 links/month
- Premium: Unlimited + custom short codes + analytics


Custom_Domains table (for custom branded links):
────────────────────────────────────────────────────────────────────────
domain_id │ user_id  │ domain           │ verified │ created_at
────────────────────────────────────────────────────────────────────────────────
dom_001   │ user_003 │ go.mycompany.com │ true     │ 2024-01-01 00:00:00

Enables: go.mycompany.com/campaign instead of bit.ly/abc123
```

### Why ACID Transactions Are Critical

```
SHORT CODE CREATION RACE CONDITION:
════════════════════════════════════════════════════════

WITHOUT TRANSACTIONS (BROKEN):
────────────────────────────────────────────────

User A:                        User B:
  │                              │
  ├─ Generate: "abc123"         ├─ Generate: "abc123"
  │                              │
  ├─ Check exists:              ├─ Check exists:
  │  SELECT FROM urls           │  SELECT FROM urls
  │  WHERE short_code='abc123'  │  WHERE short_code='abc123'
  │  → Returns 0 rows           │  → Returns 0 rows
  │                              │
  ├─ Insert:                    ├─ Insert:
  │  INSERT INTO urls           │  INSERT INTO urls
  │  VALUES ('abc123', ...)     │  VALUES ('abc123', ...)
  │  → SUCCESS                  │  → SUCCESS (COLLISION!)
  │                              │
  └─ Returns bit.ly/abc123      └─ Returns bit.ly/abc123

RESULT: Both users get same short code!
User A: bit.ly/abc123 → https://user-a-site.com
User B: bit.ly/abc123 → https://user-b-site.com

Later INSERT overwrites earlier
DISASTER: One user's link is broken


WITH UNIQUE CONSTRAINT (CORRECT):
────────────────────────────────────────────────

User A:                        User B:
  │                              │
  ├─ Generate: "abc123"         ├─ Generate: "abc123"
  │                              │
  ├─ INSERT INTO urls           ├─ INSERT INTO urls
  │  VALUES ('abc123', ...)     │  VALUES ('abc123', ...)
  │  → SUCCESS                  │  → ERROR: duplicate key
  │                              │
  │                              ├─ Retry with new code
  │                              ├─ Generate: "abc124"
  │                              ├─ INSERT INTO urls
  │                              │  VALUES ('abc124', ...)
  │                              │  → SUCCESS
  │                              │
  └─ Returns bit.ly/abc123      └─ Returns bit.ly/abc124

RESULT: Both users get unique codes
Unique constraint prevents collision
Database enforces integrity
```

### Short Code Generation Strategies

```
STRATEGY 1: Base62 Encoding of Auto-Increment ID
════════════════════════════════════════════════════════

Auto-increment sequence in PostgreSQL:
CREATE SEQUENCE url_id_seq START 1;

When creating short URL:
────────────────────────────────────────────────
next_id = nextval('url_id_seq')  → 123456789

Base62 encode (62 characters: a-z, A-Z, 0-9):
base62_chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

function base62_encode(num):
  result = ""
  while num > 0:
    remainder = num % 62
    result = base62_chars[remainder] + result
    num = num // 62
  return result

base62_encode(123456789) = "8M0kX"  (5 characters)

short_code = "8M0kX"

INSERT INTO urls (short_code, long_url, ...)
VALUES ('8M0kX', 'https://example.com/...', ...);

Returns: bit.ly/8M0kX


Pros:
────────────────────────────────────────────────
✓ Guaranteed unique (sequence never repeats)
✓ Predictable length (ID 1B = 6 chars)
✓ No collision checks needed
✓ Sequential (good for B-tree index)
✓ Simple implementation

Cons:
────────────────────────────────────────────────
✗ Predictable (8M0kX → 8M0kY → 8M0kZ)
✗ Can guess total URLs created
✗ No custom codes


STRATEGY 2: Random Generation with Collision Check
════════════════════════════════════════════════════════

Generate random 6-character code:
────────────────────────────────────────────────
function generate_random_code(length=6):
  chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
  code = ""
  for i in range(length):
    code += random.choice(chars)
  return code

code = generate_random_code()  → "a3X9zK"

Attempt insert:
INSERT INTO urls (short_code, long_url, ...)
VALUES ('a3X9zK', 'https://example.com/...', ...)
ON CONFLICT (short_code) DO NOTHING
RETURNING short_code;

If returns NULL (collision):
  → Retry with new random code
  → Usually succeeds on first try (low collision rate)


Collision probability:
────────────────────────────────────────────────
62^6 = 56 billion possible codes
With 50 billion URLs in system:
Collision rate ≈ 50B / 56B ≈ 89%

Wait, that's high! Need 7 characters:
62^7 = 3.5 trillion possible codes
Collision rate ≈ 50B / 3.5T ≈ 1.4%

Or 8 characters:
62^8 = 218 trillion possible codes
Collision rate ≈ 50B / 218T ≈ 0.02%

Use 7 characters for safety


Pros:
────────────────────────────────────────────────
✓ Unpredictable (secure)
✓ No central sequence (distributed generation)
✓ Can handle custom codes (same mechanism)

Cons:
────────────────────────────────────────────────
✗ Collision checks needed (retry logic)
✗ Slightly slower (rare retries)


STRATEGY 3: Hash-based (MD5/SHA with truncation)
════════════════════════════════════════════════════════

Hash the long URL + timestamp:
────────────────────────────────────────────────
input = long_url + str(timestamp)
hash = md5(input)  → "3e25960a79dbc69b674cd4ec67a72c62"

Take first 6 characters: "3e2596"

Check collision:
INSERT ... ON CONFLICT DO NOTHING

If collision:
  → Increment counter and re-hash
  → hash = md5(long_url + timestamp + "1")


Pros:
────────────────────────────────────────────────
✓ Deterministic (same input = same code)
✓ Can deduplicate (same URL = same short code)

Cons:
────────────────────────────────────────────────
✗ Higher collision rate (birthday paradox)
✗ Security concerns (hash reveals info)
✗ Doesn't work for custom codes

Most production systems use Strategy 1 or 2
```

---

## Why Redis for Caching?

### The Read-Heavy Problem

```
READ/WRITE RATIO:
════════════════════════════════════════════════════════

Typical Bitly workload:
- Creates: 500M/day = 5,787 writes/second
- Redirects: 10B/day = 115,740 reads/second
- Ratio: 115,740 / 5,787 = 20:1 reads to writes
- Some popular links: 1M+ clicks (extremely read-heavy)

Without caching:
────────────────────────────────────────────────
Every redirect hits PostgreSQL:
SELECT long_url FROM urls WHERE short_code = 'abc123';

115,740 queries/second on PostgreSQL
Even with perfect indexes: 5-10ms per query
Database CPU saturated
Slow redirects (50ms+)
Poor user experience


With Redis caching:
────────────────────────────────────────────────
99% of redirects served from Redis (<1ms)
1% cache miss hits PostgreSQL (5-10ms)
Database load reduced 99%
Fast redirects (<10ms average)
Excellent user experience
```

### Redis Schema

```
REDIS SCHEMA:
════════════════════════════════════════════════════════

Short code to long URL mapping:
────────────────────────────────────────────────
Key:   url:abc123
Value: "https://example.com/very/long/path/..."
TTL:   3600 seconds (1 hour)

SET url:abc123 "https://example.com/..." EX 3600

Lookup:
GET url:abc123
→ Returns long URL instantly (<1ms)


Click counter (real-time):
────────────────────────────────────────────────
Key:   clicks:abc123
Value: 15420  (total clicks)
TTL:   None (permanent)

INCR clicks:abc123
→ Atomic increment
→ Returns new count: 15421


Recent clicks (for real-time dashboard):
────────────────────────────────────────────────
Key:   recent_clicks:abc123
Type:  List
Value: [timestamp1, timestamp2, ...]
TTL:   86400 (24 hours)

LPUSH recent_clicks:abc123 1708945200
LTRIM recent_clicks:abc123 0 999  ← Keep last 1000 clicks

Dashboard queries:
LRANGE recent_clicks:abc123 0 99  ← Last 100 clicks
→ Calculate clicks per minute


User's recent short URLs (for dashboard):
────────────────────────────────────────────────
Key:   user:user_001:urls
Type:  Sorted Set
Score: Timestamp (for ordering)

ZADD user:user_001:urls 1708945200 "abc123"
ZADD user:user_001:urls 1708945300 "xyz789"

User dashboard:
ZREVRANGE user:user_001:urls 0 19  ← 20 most recent
→ Returns short codes
→ Batch fetch details from PostgreSQL
```

### Cache Strategies

```
CACHE-ASIDE PATTERN (LAZY LOADING):
════════════════════════════════════════════════════════

When redirect request arrives:
────────────────────────────────────────────────

STEP 1: Check Redis cache
long_url = GET url:abc123

If cache hit:
  → Return long_url immediately
  → Redirect user
  → Total time: <5ms

If cache miss:
  → Go to STEP 2


STEP 2: Query PostgreSQL
SELECT long_url FROM urls 
WHERE short_code = 'abc123' 
AND (expires_at IS NULL OR expires_at > NOW())
AND is_active = true;

Returns: https://example.com/...
Time: 5-10ms


STEP 3: Store in Redis (cache warm-up)
SET url:abc123 "https://example.com/..." EX 3600

Future requests will hit cache


STEP 4: Return to user
Redirect to long URL
Total time: 10-20ms (first request)


Cache eviction policy:
────────────────────────────────────────────────
TTL: 1 hour (3600 seconds)
- Popular links stay cached (frequent access refreshes)
- Unpopular links evict after 1 hour
- Reduces memory usage

Memory estimation:
- 50 billion URLs total
- Average long URL: 200 bytes
- If cache all: 50B × 200B = 10 TB (too much!)
- Cache hot 1%: 500M × 200B = 100 GB (manageable)


CACHE-THROUGH PATTERN (WRITE-THROUGH):
════════════════════════════════════════════════════════

When creating short URL:
────────────────────────────────────────────────

STEP 1: Insert into PostgreSQL
INSERT INTO urls (short_code, long_url, ...)
VALUES ('abc123', 'https://example.com/...', ...);


STEP 2: Immediately cache in Redis
SET url:abc123 "https://example.com/..." EX 3600

Benefit: First redirect is fast (no cache miss)


CACHE INVALIDATION:
════════════════════════════════════════════════════════

When user updates/deletes short URL:
────────────────────────────────────────────────

UPDATE urls SET is_active = false WHERE short_code = 'abc123';
DEL url:abc123  ← Invalidate cache

Next redirect:
→ Cache miss
→ PostgreSQL query returns no results (is_active = false)
→ Return 404 Not Found
```

---

## Why Cassandra for Analytics?

### The Analytics Write Volume Problem

```
ANALYTICS REQUIREMENTS:
════════════════════════════════════════════════════════

Every click generates analytics record:
- 10 billion clicks per day
- 115,740 writes/second sustained
- Peak: 500,000 writes/second

Data points per click:
- Timestamp (when)
- Short code (which link)
- IP address (anonymized for privacy)
- User agent (browser/device)
- Referrer (where they came from)
- Geographic location (country, city)

Storage per record: ~500 bytes
Daily storage: 10B × 500B = 5 TB/day
Annual storage: 5TB × 365 = 1.8 PB/year

This is MASSIVE write throughput + time-series data
Perfect for Cassandra
```

### Cassandra Schema

```
CASSANDRA SCHEMA:
════════════════════════════════════════════════════════

Click_Analytics table:
────────────────────────────────────────────────────────────────────────────────
short_code │ timestamp           │ click_id   │ ip_hash │ user_agent      │ country │ city      │ referrer
───────────────────────────────────────────────────────────────────────────────────────────────────────────────
abc123     │ 2024-02-26 10:00:00 │ clk_001    │ hash123 │ Mozilla/5.0...  │ US      │ New York  │ google.com
abc123     │ 2024-02-26 10:00:01 │ clk_002    │ hash456 │ Chrome/...      │ UK      │ London    │ facebook.com
abc123     │ 2024-02-26 10:00:02 │ clk_003    │ hash789 │ Safari/...      │ JP      │ Tokyo     │ twitter.com

PRIMARY KEY ((short_code), timestamp, click_id)
CLUSTERING ORDER BY (timestamp DESC, click_id DESC)

Why this partition key:
────────────────────────────────────────────────
Partition by short_code:
→ All clicks for abc123 in same partition
→ Efficient query: "Show clicks for abc123"
→ Natural data locality

Clustering by timestamp DESC:
→ Most recent clicks first
→ Efficient query: "Show last 1000 clicks"
→ Time-ordered within partition


Click_Aggregates_Daily table (materialized view):
────────────────────────────────────────────────────────────────────────
short_code │ date       │ total_clicks │ unique_visitors │ top_country │ top_referrer
───────────────────────────────────────────────────────────────────────────────────────
abc123     │ 2024-02-26 │ 15420        │ 12341           │ US          │ google.com
abc123     │ 2024-02-25 │ 14532        │ 11823           │ US          │ facebook.com

PRIMARY KEY ((short_code), date)

Pre-aggregated daily stats
Fast dashboard queries


Click_Aggregates_Hourly table:
────────────────────────────────────────────────────────────────────────
short_code │ hour_bucket         │ clicks │ unique_ips
────────────────────────────────────────────────────────────────────────────────
abc123     │ 2024-02-26 10:00:00 │ 523    │ 421
abc123     │ 2024-02-26 11:00:00 │ 612    │ 501

Real-time analytics (updated every minute)
```

### Why Cassandra Handles This Better Than PostgreSQL

```
POSTGRESQL ATTEMPT:
════════════════════════════════════════════════════════

Click_Analytics table:
────────────────────────────────────────────────
click_id │ short_code │ timestamp           │ ip_hash │ user_agent │ country
──────────────────────────────────────────────────────────────────────────────
clk_001  │ abc123     │ 2024-02-26 10:00:00 │ hash123 │ Mozilla... │ US
...
(10 billion rows per day)

Problems at 115K writes/second:
────────────────────────────────────────────────
→ B-tree index on timestamp requires updates
→ Each INSERT updates multiple indexes
→ Autovacuum cannot keep up (MVCC overhead)
→ Table bloat grows rapidly
→ Query performance degrades over time
→ Partitioning helps but complex to manage
→ Need 100+ PostgreSQL shards for this volume


Query: "Show last 1000 clicks for abc123"
SELECT * FROM click_analytics
WHERE short_code = 'abc123'
ORDER BY timestamp DESC
LIMIT 1000;

→ Index scan on (short_code, timestamp)
→ At billions of rows: 100ms+ query time
→ Aggregations (COUNT, SUM) very slow


CASSANDRA SOLUTION:
════════════════════════════════════════════════════════

Writes:
────────────────────────────────────────────────
INSERT INTO click_analytics (short_code, timestamp, ...)
VALUES ('abc123', NOW(), ...);

Cassandra write path:
1. Append to commit log (sequential disk write)
2. Write to MemTable (in-memory)
3. Acknowledge immediately (<1ms)
4. Later: flush to SSTable (background)

Write throughput: 100,000+ writes/second per node
No index maintenance
No locks
Horizontally scalable (add nodes → more capacity)


Reads:
────────────────────────────────────────────────
SELECT * FROM click_analytics
WHERE short_code = 'abc123'
AND timestamp > '2024-02-26 00:00:00'
ORDER BY timestamp DESC
LIMIT 1000;

Cassandra execution:
1. Hash 'abc123' → find partition (Node 3)
2. Go to Node 3
3. Read from SSTable (data already sorted by timestamp)
4. Return top 1000

Query time: <50ms
Scales linearly with cluster size


Time-series data compaction:
────────────────────────────────────────────────
Old click data (>1 year) rarely accessed
Cassandra TTL:

INSERT INTO click_analytics (...) USING TTL 31536000;
-- Auto-delete after 1 year (365 days)

Or aggressive compaction for old data
Reduces storage costs
```

---

## Complete Flow

```
FLOW 1: Create Short URL
════════════════════════════════════════════════════════

User: "Shorten https://example.com/very/long/path/to/resource?param1=value&param2=value"
        │
        ▼
POST /api/shorten
{
  long_url: "https://example.com/very/long/path/to/resource?param1=value&param2=value",
  custom_code: null,  // or "mycampaign" for custom
  user_id: "user_001"
}
        │
        ▼
STEP 1: Validate long URL
────────────────────────────────────────────────
- Check valid format (http:// or https://)
- Check not already a short URL (prevent recursion)
- Check not malicious (blocklist check)
- Check length < 2000 characters


STEP 2: Check user quota
────────────────────────────────────────────────
SELECT plan, links_created FROM users WHERE user_id = 'user_001';

Returns: plan = "free", links_created = 45

Check: 45 < 100 (free tier limit) → ✓ Allowed


STEP 3: Generate short code
────────────────────────────────────────────────
If custom_code provided:
  short_code = "mycampaign"
  -- User must have premium plan
  -- Check if already taken
Else:
  next_id = nextval('url_id_seq')  → 123456790
  short_code = base62_encode(next_id)  → "8M0kY"


STEP 4: Insert into PostgreSQL
────────────────────────────────────────────────
BEGIN TRANSACTION;

INSERT INTO urls (short_code, long_url, user_id, created_at, is_custom)
VALUES ('8M0kY', 'https://example.com/...', 'user_001', NOW(), false)
ON CONFLICT (short_code) DO NOTHING
RETURNING short_code;

If returns NULL (collision - rare):
  → Retry with new code
  
UPDATE users 
SET links_created = links_created + 1
WHERE user_id = 'user_001';

COMMIT;

Time: <10ms


STEP 5: Cache in Redis (write-through)
────────────────────────────────────────────────
SET url:8M0kY "https://example.com/..." EX 3600

Initialize click counter:
SET clicks:8M0kY 0

Add to user's recent URLs:
ZADD user:user_001:urls 1708945200 "8M0kY"


STEP 6: Return to user
────────────────────────────────────────────────
Response:
{
  short_url: "https://bit.ly/8M0kY",
  long_url: "https://example.com/...",
  created_at: "2024-02-26T10:00:00Z",
  qr_code: "https://api.qr.ly/8M0kY.png"
}

Total latency: <50ms
```

```
FLOW 2: Redirect (Click on Short URL)
════════════════════════════════════════════════════════

User clicks: https://bit.ly/abc123
        │
        ▼
GET /abc123
        │
        ▼
STEP 1: Check Redis cache
────────────────────────────────────────────────
long_url = GET url:abc123

Cache hit: Returns "https://example.com/..." immediately
Cache miss: Go to STEP 2


STEP 2: Query PostgreSQL (if cache miss)
────────────────────────────────────────────────
SELECT long_url, is_active, expires_at
FROM urls
WHERE short_code = 'abc123';

Returns:
- long_url: "https://example.com/..."
- is_active: true
- expires_at: NULL

If is_active = false OR expires_at < NOW():
  → Return 404 Not Found
  → "This link has expired or been disabled"


STEP 3: Cache in Redis
────────────────────────────────────────────────
SET url:abc123 "https://example.com/..." EX 3600

Future requests will hit cache


STEP 4: Increment click counter (async)
────────────────────────────────────────────────
INCR clicks:abc123  → Returns 15421

Don't wait for this (non-blocking)


STEP 5: Record analytics (async)
────────────────────────────────────────────────
Publish to message queue (Kafka):
{
  "short_code": "abc123",
  "timestamp": "2024-02-26T10:00:00.123Z",
  "ip": "203.0.113.45",
  "user_agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
  "referrer": "https://google.com"
}

Background consumer writes to Cassandra (async)
Doesn't block redirect


STEP 6: Redirect user
────────────────────────────────────────────────
HTTP/1.1 301 Moved Permanently
Location: https://example.com/...
Cache-Control: public, max-age=3600

Browser redirects user to long URL

Total latency: 5-20ms
User barely notices redirect
```

```
FLOW 3: Analytics Background Processing
════════════════════════════════════════════════════════

Kafka consumer (runs continuously):
        │
        ▼
STEP 1: Consume click event from Kafka
────────────────────────────────────────────────
Event:
{
  short_code: "abc123",
  timestamp: "2024-02-26T10:00:00.123Z",
  ip: "203.0.113.45",
  user_agent: "Mozilla/5.0...",
  referrer: "https://google.com"
}


STEP 2: Enrich with geographic data
────────────────────────────────────────────────
IP geolocation lookup (MaxMind GeoIP):
ip_to_location("203.0.113.45")

Returns:
- country: "US"
- city: "New York"
- latitude: 40.7128
- longitude: -74.0060


STEP 3: Parse user agent
────────────────────────────────────────────────
parse_user_agent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/121.0...")

Returns:
- browser: "Chrome"
- version: "121.0"
- os: "Windows"
- device: "Desktop"


STEP 4: Write to Cassandra (batch)
────────────────────────────────────────────────
INSERT INTO click_analytics
(short_code, timestamp, click_id, ip_hash, user_agent, country, city, referrer, browser, os, device)
VALUES
('abc123', '2024-02-26 10:00:00.123', 'clk_12345', hash('203.0.113.45'), 
 'Mozilla/5.0...', 'US', 'New York', 'google.com', 'Chrome', 'Windows', 'Desktop');

Batch 1000 clicks every 5 seconds:
→ Reduces write amplification
→ 1000 INSERTs in single batch
→ Time: <50ms


STEP 5: Update hourly aggregates
────────────────────────────────────────────────
UPDATE click_aggregates_hourly
SET clicks = clicks + 1,
    unique_ips = unique_ips + 1  -- if new IP
WHERE short_code = 'abc123'
AND hour_bucket = '2024-02-26 10:00:00';

Real-time dashboard shows updated count


STEP 6: Update PostgreSQL click count (periodic)
────────────────────────────────────────────────
Every 1 minute, sync Redis → PostgreSQL:

clicks = GET clicks:abc123  → 15421

UPDATE urls
SET clicks = 15421
WHERE short_code = 'abc123';

Keeps PostgreSQL count reasonably accurate
Not real-time (acceptable tradeoff)
```

```
FLOW 4: View Analytics Dashboard
════════════════════════════════════════════════════════

User: "Show me analytics for bit.ly/abc123"
        │
        ▼
GET /api/analytics/abc123?period=7d
        │
        ▼
STEP 1: Fetch basic stats from PostgreSQL
────────────────────────────────────────────────
SELECT short_code, long_url, created_at, clicks
FROM urls
WHERE short_code = 'abc123'
AND user_id = 'user_001';  -- Security check

Returns:
- Created: 2024-02-20
- Total clicks: 15420


STEP 2: Fetch daily breakdown from Cassandra
────────────────────────────────────────────────
SELECT date, total_clicks, unique_visitors, top_country, top_referrer
FROM click_aggregates_daily
WHERE short_code = 'abc123'
AND date >= '2024-02-20'
AND date < '2024-02-27'
ORDER BY date DESC;

Returns last 7 days:
[
  {date: "2024-02-26", clicks: 2341, unique: 1923, country: "US", referrer: "google.com"},
  {date: "2024-02-25", clicks: 2156, unique: 1802, country: "US", referrer: "facebook.com"},
  ...
]


STEP 3: Fetch geographic breakdown
────────────────────────────────────────────────
SELECT country, COUNT(*) as clicks
FROM click_analytics
WHERE short_code = 'abc123'
AND timestamp >= '2024-02-20'
AND timestamp < '2024-02-27'
GROUP BY country
ORDER BY clicks DESC
LIMIT 10;

Returns top countries:
[
  {country: "US", clicks: 8234},
  {country: "UK", clicks: 2341},
  {country: "CA", clicks: 1523},
  ...
]


STEP 4: Fetch referrer breakdown
────────────────────────────────────────────────
SELECT referrer, COUNT(*) as clicks
FROM click_analytics
WHERE short_code = 'abc123'
AND timestamp >= '2024-02-20'
GROUP BY referrer
ORDER BY clicks DESC
LIMIT 10;

Returns top referrers:
[
  {referrer: "google.com", clicks: 5234},
  {referrer: "facebook.com", clicks: 3412},
  {referrer: "twitter.com", clicks: 2341},
  ...
]


STEP 5: Return combined analytics
────────────────────────────────────────────────
Response:
{
  short_code: "abc123",
  long_url: "https://example.com/...",
  created_at: "2024-02-20T10:00:00Z",
  total_clicks: 15420,
  daily_breakdown: [...],
  top_countries: [...],
  top_referrers: [...],
  device_breakdown: {desktop: 60%, mobile: 35%, tablet: 5%}
}

Total query time: <200ms
Dashboard renders charts and graphs
```

```
FLOW 5: Link Expiration (Background Job)
════════════════════════════════════════════════════════

Cron job runs every hour:
        │
        ▼
STEP 1: Find expired links
────────────────────────────────────────────────
SELECT short_code FROM urls
WHERE expires_at IS NOT NULL
AND expires_at < NOW()
AND is_active = true
LIMIT 10000;

Returns: [abc123, xyz789, ...]


STEP 2: Deactivate links
────────────────────────────────────────────────
UPDATE urls
SET is_active = false
WHERE short_code IN ('abc123', 'xyz789', ...);


STEP 3: Invalidate cache
────────────────────────────────────────────────
For each short_code:
  DEL url:abc123
  DEL url:xyz789

Next redirect attempt:
→ Cache miss
→ PostgreSQL returns is_active = false
→ 404 Not Found response
```

---

## System Architecture Diagram

```
                    ┌──────────────────┐
                    │  User Browser    │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │  CDN / Load      │
                    │   Balancer       │
                    └────────┬─────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
  │  API Server  │   │  API Server  │   │  API Server  │
  │  (Create)    │   │  (Redirect)  │   │ (Analytics)  │
  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘
         │                   │                   │
   ┌─────┼───────────────────┼───────────────────┼─────┐
   │     │                   │                   │     │
   │     ▼                   ▼                   │     │
   │ ┌────────────┐   ┌─────────────┐          │     │
   │ │PostgreSQL  │   │    Redis    │          │     │
   │ │            │   │             │          │     │
   │ │ -URLs      │   │ -Cache      │          │     │
   │ │ -Users     │   │ -Counters   │          │     │
   │ └────────────┘   └─────────────┘          │     │
   │                                            │     │
   └────────────────────┬───────────────────────┘     │
                        │                             │
                   ┌────▼──────┐                      │
                   │   Kafka   │                      │
                   │ (events)  │                      │
                   └────┬──────┘                      │
                        │                             │
                        ▼                             ▼
                ┌───────────────┐            ┌────────────────┐
                │   Consumer    │            │   Cassandra    │
                │  (Analytics)  │──────────→ │   (Analytics)  │
                └───────────────┘            └────────────────┘
```

---

## Tradeoffs vs Other Databases

```
┌──────────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                              │ THIS ARCH    │ POSTGRES ALL │ MONGO ALL    │ DYNAMODB ALL │
├──────────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Unique constraint (no dups)  │ PostgreSQL✓  │ PostgreSQL✓  │ Unique Index │ Conditional✓ │
│ Read caching (99% hits)      │ Redis ✓      │ pg_cache     │ App cache    │ DAX ✓        │
│ Analytics write throughput   │ Cassandra✓   │ 10K/sec ✗    │ 50K/sec      │ Variable ✓   │
│ Time-series queries          │ Cassandra✓   │ Partitioning │ Time-series  │ GSI ✓        │
│ Click counter (atomic)       │ Redis ✓      │ UPDATE ✓     │ Increment ✓  │ Atomic ✓     │
│ TTL auto-expiration          │ PostgreSQL✓  │ Manual job   │ Native ✓     │ Native ✓     │
│ Horizontal scaling           │ Sharding     │ Manual shard │ Native ✓     │ Native ✓     │
│ Operational complexity       │ HIGH         │ MEDIUM       │ MEDIUM       │ LOW (managed)│
│ Cost at Bitly scale          │ HIGH         │ VERY HIGH    │ HIGH         │ MEDIUM       │
└──────────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **PostgreSQL stores URL mappings with UNIQUE constraint on short_code preventing duplicate short links through database-level enforcement where simultaneous INSERT attempts by two users generating same random code ("abc123") cause one to succeed and other to fail with duplicate key error forcing retry with new code, while auto-increment sequence strategy (nextval → base62_encode) guarantees collision-free codes and B-tree indexes on (user_id, created_at) enable fast "show my recent links" queries in <10ms — Redis caches short_code→long_url mappings with 1-hour TTL serving 99% of redirect requests in <1ms versus PostgreSQL's 5-10ms eliminating database load from 115K reads/second workload, while atomic INCR operations maintain real-time click counters updated on every redirect without race conditions and ZADD sorted sets track user's recent URLs ordered by timestamp for instant dashboard rendering — Cassandra stores click analytics partitioned by short_code with clustering on timestamp DESC enabling append-heavy workload (115K writes/second, 500K peak) through commit log sequential writes completing in <1ms and time-ordered queries "last 1000 clicks for abc123" reading from single partition's pre-sorted SSTable in <50ms, with TTL-based expiration (USING TTL 31536000) auto-deleting year-old data reducing storage from 1.8 PB/year to manageable levels — base62 encoding converts auto-increment IDs (123456789) to 6-character codes (8M0kX) using charset "0-9a-zA-Z" giving 62^6=56 billion unique codes preventing collisions better than random generation which needs 7 characters (62^7=3.5 trillion) to achieve <2% collision rate at 50 billion URLs, while custom codes require premium plan and duplicate checks via ON CONFLICT DO NOTHING — async analytics pipeline publishes click events to Kafka immediately after redirect without blocking user (non-blocking INCR + async Kafka.send) then background consumers batch 1000 events every 5 seconds enriching with IP geolocation and user-agent parsing before writing to Cassandra, periodically syncing Redis click counters to PostgreSQL (every 1 minute) for durability while keeping redirects fast through cache-aside pattern where cache misses query PostgreSQL then warm cache (SET with EX) for future hits.**
