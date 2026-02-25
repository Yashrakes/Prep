
---

## The Core Problem Rate Limiting Solves

On the surface rate limiting seems simple — count requests, block if over limit. But consider the real constraints:

```
API Gateway scale reality:
────────────────────────────────────────────────
API requests:              100,000+ requests/second
Users:                     10 million+
Rate limit rules:          1,000+ different limits
Example limits:
  - Free tier: 100 req/hour per user
  - Basic tier: 1,000 req/hour per user
  - Premium: 10,000 req/hour per user
  - Per endpoint: /api/search limited to 10 req/min
  - Global: 100K req/sec across all users

Requirements:
→ Check limit <1ms (can't slow down API)
→ Accurate counting (no "off by N" errors)
→ Distributed across multiple servers (no single point)
→ Handle race conditions (concurrent requests)
→ Support multiple algorithms (fixed window, sliding window, token bucket)
→ Per-user, per-IP, per-API-key granularity
→ Dynamic rule updates (no restart needed)
→ Fail open (if rate limiter down, allow requests)
```

This combination of **sub-millisecond latency + accurate distributed counting + atomic operations + multiple algorithms + high availability** is what forces this specific architecture.

---

## Why Redis for Rate Limiting?

### The Speed Requirement

```
LATENCY BUDGET:
════════════════════════════════════════════════════════

API request flow:
User → Load Balancer → API Gateway → Rate Limiter → Backend Service
                                         ↑
                                    Must be <1ms

If rate limiter takes 50ms:
- User experiences 50ms added latency on EVERY request
- 100K requests/sec × 50ms = 5,000 concurrent checks
- Unacceptable

Target: <1ms per rate limit check
Only in-memory databases can achieve this
Redis is PERFECT
```

### Redis Data Structures for Rate Limiting

```
ALGORITHM 1: Fixed Window Counter (simplest)
════════════════════════════════════════════════════════

Limit: 100 requests per minute

Redis schema:
────────────────────────────────────────────────
Key:   rate_limit:user_001:2024-02-26:10:30
Value: 45  (request count in this minute)
TTL:   60 seconds

When request arrives:
INCR rate_limit:user_001:2024-02-26:10:30
EXPIRE rate_limit:user_001:2024-02-26:10:30 60

Response:
IF value <= 100: Allow
IF value > 100: Reject (429 Too Many Requests)


Atomic operation:
────────────────────────────────────────────────
count = INCR rate_limit:user_001:2024-02-26:10:30

Single Redis command
Atomic (no race condition)
Time: <1ms


Problem with fixed window:
────────────────────────────────────────────────
Burst at window boundary:

10:29:59 → 50 requests
10:30:00 → 100 requests (new window)

Total: 150 requests in 1 second (50 + 100)
But limit is 100 requests per minute!

This is the "boundary burst" problem
```

```
ALGORITHM 2: Sliding Window Log (accurate)
════════════════════════════════════════════════════════

Limit: 100 requests per minute

Redis schema:
────────────────────────────────────────────────
Key:   rate_limit:user_001:log
Type:  Sorted Set
Score: Unix timestamp (with millisecond precision)
Member: request_id (unique)

ZADD rate_limit:user_001:log 1708945200.123 "req_abc"
ZADD rate_limit:user_001:log 1708945200.456 "req_xyz"
...


When request arrives:
────────────────────────────────────────────────
current_time = NOW()
window_start = current_time - 60 seconds

-- Remove old entries (outside window)
ZREMRANGEBYSCORE rate_limit:user_001:log -inf window_start

-- Count requests in window
count = ZCARD rate_limit:user_001:log

-- Add new request
ZADD rate_limit:user_001:log current_time request_id

-- Set expiration (cleanup)
EXPIRE rate_limit:user_001:log 60

IF count < 100: Allow
ELSE: Reject


Lua script (atomic):
────────────────────────────────────────────────
local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

-- Remove old entries
redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window)

-- Count current requests
local count = redis.call('ZCARD', key)

if count < limit then
  -- Add new request
  redis.call('ZADD', key, now, now .. ':' .. math.random())
  redis.call('EXPIRE', key, window)
  return 1  -- Allow
else
  return 0  -- Reject
end


Pros:
────────────────────────────────────────────────
✓ Accurate (no boundary burst)
✓ True sliding window
✓ Per-request granularity

Cons:
────────────────────────────────────────────────
✗ Memory intensive (stores every request)
✗ 100 requests × 10M users = 1 billion entries
✗ Cleanup overhead (ZREMRANGEBYSCORE on every request)
```

```
ALGORITHM 3: Sliding Window Counter (hybrid, optimal)
════════════════════════════════════════════════════════

Combines accuracy of sliding window with efficiency of counter

Limit: 100 requests per minute

Redis schema:
────────────────────────────────────────────────
Two keys per user:
rate_limit:user_001:current_minute   (count in current minute)
rate_limit:user_001:previous_minute  (count in previous minute)


When request arrives at 10:30:45 (45 seconds into minute):
────────────────────────────────────────────────
current_count = GET rate_limit:user_001:2024-02-26:10:30
previous_count = GET rate_limit:user_001:2024-02-26:10:29

-- Calculate weighted count
elapsed_in_current = 45 seconds
weight_current = 45 / 60 = 0.75
weight_previous = 1 - 0.75 = 0.25

estimated_count = (previous_count × 0.25) + (current_count × 0.75)

If estimated_count < 100:
  INCR rate_limit:user_001:2024-02-26:10:30
  Allow
Else:
  Reject


Lua script (atomic):
────────────────────────────────────────────────
local current_key = KEYS[1]
local previous_key = KEYS[2]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

local current_minute = math.floor(now / window)
local previous_minute = current_minute - 1
local elapsed = now % window

local current_count = tonumber(redis.call('GET', current_key) or '0')
local previous_count = tonumber(redis.call('GET', previous_key) or '0')

local weight = elapsed / window
local estimated_count = (previous_count * (1 - weight)) + current_count

if estimated_count < limit then
  redis.call('INCR', current_key)
  redis.call('EXPIRE', current_key, window * 2)
  return 1
else
  return 0
end


Pros:
────────────────────────────────────────────────
✓ Accurate (minimal boundary burst)
✓ Memory efficient (only 2 counters per user)
✓ Fast (simple arithmetic)
✓ Best of both worlds

This is what most production systems use
```

```
ALGORITHM 4: Token Bucket (rate smoothing)
════════════════════════════════════════════════════════

Concept: User has bucket of tokens, requests consume tokens
Tokens refill at constant rate

Limit: 100 tokens, refill rate 10 tokens/minute

Redis schema:
────────────────────────────────────────────────
Key:   rate_limit:user_001:bucket
Type:  Hash

HSET rate_limit:user_001:bucket
  tokens "100"
  last_refill "1708945200"


When request arrives:
────────────────────────────────────────────────
Lua script:
local key = KEYS[1]
local capacity = tonumber(ARGV[1])  -- 100
local refill_rate = tonumber(ARGV[2])  -- 10 per minute
local now = tonumber(ARGV[3])

local bucket = redis.call('HGETALL', key)
local tokens = tonumber(bucket[2] or capacity)
local last_refill = tonumber(bucket[4] or now)

-- Calculate tokens to add
local time_passed = now - last_refill
local tokens_to_add = math.floor(time_passed * refill_rate / 60)
tokens = math.min(capacity, tokens + tokens_to_add)

if tokens >= 1 then
  -- Consume one token
  tokens = tokens - 1
  redis.call('HSET', key, 'tokens', tokens, 'last_refill', now)
  return 1  -- Allow
else
  return 0  -- Reject
end


Pros:
────────────────────────────────────────────────
✓ Smooth rate limiting (no bursts)
✓ Allows burst up to bucket capacity
✓ Good for APIs with variable request costs

Cons:
────────────────────────────────────────────────
✗ More complex calculation
✗ Slightly slower (more Redis operations)
```

---

## Why PostgreSQL for Configuration?

### What Configuration Data Looks Like

```
RATE LIMIT RULES:
════════════════════════════════════════════════════════

{
  rule_id: "rule_001",
  name: "Free Tier - Search API",
  resource: "/api/search",
  subject_type: "user",  // user, ip, api_key
  limit: 100,
  window: 3600,  // 1 hour in seconds
  algorithm: "sliding_window_counter",
  tier: "free",
  action: "reject",  // reject or throttle
  response_code: 429,
  response_message: "Rate limit exceeded. Upgrade to Premium.",
  created_at: "2024-01-01",
  enabled: true,
  priority: 1  // lower number = higher priority
}
```

### PostgreSQL Schema

```
POSTGRESQL SCHEMA:
════════════════════════════════════════════════════════

Rate_Limit_Rules table:
────────────────────────────────────────────────────────────────────────────────
rule_id │ name             │ resource      │ limit │ window │ algorithm       │ enabled
───────────────────────────────────────────────────────────────────────────────────────────
rule_001│ Free-Search      │ /api/search   │ 100   │ 3600   │ sliding_window  │ true
rule_002│ Premium-Search   │ /api/search   │ 10000 │ 3600   │ sliding_window  │ true
rule_003│ Global-Limit     │ *             │ 100000│ 60     │ fixed_window    │ true

Columns:
  subject_type: ENUM('user', 'ip', 'api_key')
  action: ENUM('reject', 'throttle', 'log')
  priority: INTEGER (for rule ordering)

Indexes:
  PRIMARY KEY (rule_id)
  INDEX (resource, enabled)
  INDEX (tier)


User_Tiers table:
────────────────────────────────────────────────
user_id  │ tier     │ upgraded_at
─────────────────────────────────────────────────
user_001 │ free     │ NULL
user_002 │ premium  │ 2024-02-01
user_003 │ enterprise│ 2024-01-15

Foreign keys:
  tier REFERENCES tiers(name)


Tiers table:
────────────────────────────────────────────────────────────────────────
tier_name    │ display_name │ price │ limits
────────────────────────────────────────────────────────────────────────────────
free         │ Free         │ 0     │ {"search": 100, "api": 1000}
premium      │ Premium      │ 49    │ {"search": 10000, "api": 50000}
enterprise   │ Enterprise   │ 999   │ {"search": 1000000, "api": unlimited}
```

### Why PostgreSQL for Config?

```
CONFIGURATION QUERIES:
════════════════════════════════════════════════════════

Query 1: Get applicable rate limit for request
────────────────────────────────────────────────
SELECT r.rule_id, r.limit, r.window, r.algorithm
FROM rate_limit_rules r
JOIN user_tiers ut ON r.tier = ut.tier
WHERE r.resource IN ('/api/search', '*')
AND r.subject_type = 'user'
AND ut.user_id = 'user_001'
AND r.enabled = true
ORDER BY r.priority ASC
LIMIT 1;

→ JOIN with user_tiers
→ Multiple WHERE conditions
→ Priority-based selection
→ Natural in SQL


Query 2: Get user's remaining quota
────────────────────────────────────────────────
SELECT 
  r.resource,
  r.limit as total_limit,
  r.window,
  -- Current usage fetched from Redis separately
  (r.limit - current_usage) as remaining
FROM rate_limit_rules r
JOIN user_tiers ut ON r.tier = ut.tier
WHERE ut.user_id = 'user_001'
AND r.enabled = true;

→ Calculation logic
→ JOIN for user context


Query 3: Audit log (which rules triggered)
────────────────────────────────────────────────
Rate_Limit_Events table:
event_id │ rule_id  │ user_id  │ timestamp           │ action
──────────────────────────────────────────────────────────────────
evt_001  │ rule_001 │ user_001 │ 2024-02-26 10:00:00 │ rejected
evt_002  │ rule_001 │ user_002 │ 2024-02-26 10:01:00 │ rejected

SELECT COUNT(*) as violations, user_id
FROM rate_limit_events
WHERE rule_id = 'rule_001'
AND timestamp > NOW() - INTERVAL '1 day'
GROUP BY user_id
ORDER BY violations DESC
LIMIT 10;

→ Aggregation
→ Historical analysis
→ Business intelligence


WHY NOT REDIS FOR CONFIG:
────────────────────────────────────────────────
✗ No JOINs (user_tiers + rules)
✗ No complex queries (priority ordering)
✗ No durability guarantee (Redis is cache)
✗ No audit trail
✗ No schema validation
✗ No relational integrity

PostgreSQL is correct choice for configuration
```

---

## Distributed Rate Limiting Challenge

### The Race Condition Problem

```
NAIVE APPROACH (BROKEN):
════════════════════════════════════════════════════════

Three API servers, all checking same user's rate limit:

Server 1:                  Server 2:                  Server 3:
  │                          │                          │
  ├─ GET count              ├─ GET count              ├─ GET count
  │  → returns 99           │  → returns 99           │  → returns 99
  │                          │                          │
  ├─ Check: 99 < 100 ✓      ├─ Check: 99 < 100 ✓      ├─ Check: 99 < 100 ✓
  │                          │                          │
  ├─ SET count 100          ├─ SET count 100          ├─ SET count 100
  │                          │                          │
  └─ Allow                  └─ Allow                  └─ Allow

Result: 3 requests allowed, but limit was 100
Final count: 100 (should be 102)
Lost 2 requests in race condition!

At 100K requests/second, this happens constantly
Rate limiter becomes inaccurate
```

### Solution 1: Atomic INCR

```
ATOMIC INCREMENT:
════════════════════════════════════════════════════════

All servers use INCR (atomic):

Server 1:                  Server 2:                  Server 3:
  │                          │                          │
  ├─ INCR count             ├─ INCR count             ├─ INCR count
  │  → returns 100          │  → returns 101          │  → returns 102
  │                          │                          │
  ├─ Check: 100 <= 100 ✓    ├─ Check: 101 > 100 ✗     ├─ Check: 102 > 100 ✗
  │                          │                          │
  └─ Allow                  └─ Reject                 └─ Reject

Result: Only 1 request allowed (correct!)
Final count: 102
Accurate rate limiting

INCR is atomic in Redis
No race condition possible
```

### Solution 2: Lua Scripts (Complex Logic)

```
LUA SCRIPT FOR SLIDING WINDOW:
════════════════════════════════════════════════════════

Problem: Sliding window requires multiple operations:
1. Remove old entries (ZREMRANGEBYSCORE)
2. Count remaining (ZCARD)
3. Check against limit
4. Add new entry (ZADD)

If these are separate commands:
→ Race conditions between steps
→ Another server could sneak in

Solution: Lua script (atomic)
────────────────────────────────────────────────
Lua script in Redis executes atomically
No other command can run during script execution
All operations appear as single atomic unit

EVAL """
local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

-- All these operations are atomic (no interleaving)
redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window)
local count = redis.call('ZCARD', key)

if count < limit then
  redis.call('ZADD', key, now, now)
  redis.call('EXPIRE', key, window)
  return {1, count + 1}  -- Allow, new count
else
  return {0, count}  -- Reject, current count
end
""" 1 rate_limit:user_001 1708945200 60 100

Benefits:
────────────────────────────────────────────────
✓ Atomic (no race conditions)
✓ Single network round trip
✓ Server-side execution (fast)
✓ Complex logic possible
```

---

## High Availability Architecture

### Redis Cluster with Replication

```
PRODUCTION REDIS SETUP:
════════════════════════════════════════════════════════

Redis Cluster (3 masters + 3 replicas):
────────────────────────────────────────────────

Master 1 (Slots 0-5461)          Replica 1
  │                               │
  └───── Replication ─────────────┘

Master 2 (Slots 5462-10922)      Replica 2
  │                               │
  └───── Replication ─────────────┘

Master 3 (Slots 10923-16383)     Replica 3
  │                               │
  └───── Replication ─────────────┘


Key distribution:
────────────────────────────────────────────────
hash(rate_limit:user_001) % 16384 = Slot 8432
→ Routed to Master 2

hash(rate_limit:user_002) % 16384 = Slot 1234
→ Routed to Master 1

Automatic sharding across masters
Each user's rate limit on specific master


Failover:
────────────────────────────────────────────────
Master 2 crashes
        │
        ▼
Redis Sentinel detects failure (within 30 seconds)
        │
        ▼
Promote Replica 2 → New Master 2
        │
        ▼
All clients redirect to new Master 2
        │
        ▼
Service continues with zero data loss

Rate limiter stays available (HA achieved)
```

### Fail Open Strategy

```
CIRCUIT BREAKER PATTERN:
════════════════════════════════════════════════════════

If Redis is down/slow:
────────────────────────────────────────────────

Without circuit breaker:
Every request tries Redis
Redis is down
All requests timeout (5 seconds each)
API becomes unusable
Cascading failure


With circuit breaker:
────────────────────────────────────────────────
Pseudocode:

if circuit_breaker.is_open():
  # Fail open: Allow request without rate limiting
  log_warning("Rate limiter bypassed - Redis unavailable")
  return ALLOW

try:
  result = redis.check_rate_limit(user_id)
  circuit_breaker.record_success()
  return result
except RedisTimeout:
  circuit_breaker.record_failure()
  if circuit_breaker.failure_threshold_reached():
    circuit_breaker.open()
  # Fail open: Allow request
  return ALLOW


Circuit breaker states:
────────────────────────────────────────────────
CLOSED (normal):
→ All requests go to Redis
→ Track failures

OPEN (Redis down):
→ All requests bypass Redis (fail open)
→ Don't overload dead Redis
→ Try to reconnect every 30 seconds

HALF_OPEN (testing recovery):
→ Send small % of requests to Redis
→ If succeed: Close circuit (back to normal)
→ If fail: Open circuit again


Why fail open (not fail closed):
────────────────────────────────────────────────
Fail open: Allow requests when rate limiter down
→ Better UX (API works)
→ Temporary over-limit acceptable
→ Graceful degradation

Fail closed: Reject all requests when rate limiter down
→ Terrible UX (API completely down)
→ Worse than no rate limiter!

Most APIs choose fail open
```

---

## Complete Architecture

```
REQUEST FLOW:
════════════════════════════════════════════════════════

User request:
GET /api/search?q=python

        │
        ▼
┌────────────────────┐
│   Load Balancer    │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│   API Gateway      │
│  (Rate Limiter)    │
└─────────┬──────────┘
          │
          ├──────────┐
          │          │
          ▼          ▼
    ┌─────────┐  ┌────────────┐
    │  Redis  │  │ PostgreSQL │
    │ (counts)│  │ (config)   │
    └─────────┘  └────────────┘
          │
          ▼
┌────────────────────┐
│  Backend Service   │
│   (/api/search)    │
└────────────────────┘


Detailed flow:
────────────────────────────────────────────────

1. Request arrives at API Gateway
        │
        ▼
2. Extract rate limit key
   - user_id from JWT token
   - IP address from headers
   - API key from header
   Key: rate_limit:user_001:/api/search:2024-02-26:10:30


3. Fetch rate limit rule from PostgreSQL (cached)
   Cache key: rate_limit_config:/api/search:free_tier
   TTL: 5 minutes
   
   If cached: Use cached rule
   If not: Query PostgreSQL, cache result


4. Check rate limit in Redis
   ────────────────────────────────────────────
   Lua script execution:
   
   result = EVAL sliding_window_script 
             1 
             rate_limit:user_001:/api/search
             1708945200 
             60 
             100
   
   Returns: {allow: true, count: 45, remaining: 55}
   
   Time: <1ms


5. Decision
   ────────────────────────────────────────────
   If allow = true:
     - Add headers to response:
       X-RateLimit-Limit: 100
       X-RateLimit-Remaining: 55
       X-RateLimit-Reset: 1708945260
     - Forward request to backend
     
   If allow = false:
     - Return 429 Too Many Requests
     - Headers:
       X-RateLimit-Limit: 100
       X-RateLimit-Remaining: 0
       X-RateLimit-Reset: 1708945260
       Retry-After: 15  (seconds)
     - Body: {"error": "Rate limit exceeded"}


6. Log to PostgreSQL (async)
   ────────────────────────────────────────────
   Background job inserts rate limit events:
   
   INSERT INTO rate_limit_events
   (user_id, rule_id, action, timestamp)
   VALUES ('user_001', 'rule_001', 'rejected', NOW());
   
   Used for analytics and billing
```

---

## Advanced Patterns

### Per-Endpoint Rate Limits

```
HIERARCHICAL RATE LIMITS:
════════════════════════════════════════════════════════

User has multiple limits:
1. Global: 10,000 requests/hour (any endpoint)
2. Search API: 100 requests/hour
3. Upload API: 10 requests/hour

Check all three:
────────────────────────────────────────────────
keys = [
  "rate_limit:user_001:global",
  "rate_limit:user_001:/api/search",
  "rate_limit:user_001:/api/upload"
]

For each key:
  Check limit
  If any limit exceeded → Reject

All three must pass for request to be allowed


Lua script (check multiple limits atomically):
────────────────────────────────────────────────
local keys = KEYS
local limits = ARGV

for i, key in ipairs(keys) do
  local limit = tonumber(limits[i])
  local count = redis.call('INCR', key)
  redis.call('EXPIRE', key, 3600)
  
  if count > limit then
    -- Decrement all keys (rollback)
    for j = 1, i do
      redis.call('DECR', keys[j])
    end
    return {0, key}  -- Reject, which limit hit
  end
end

return {1}  -- Allow
```

### Cost-Based Rate Limiting

```
VARIABLE COST PER REQUEST:
════════════════════════════════════════════════════════

Different requests have different costs:
- Simple GET: 1 token
- Complex search: 5 tokens
- AI inference: 50 tokens
- Video processing: 100 tokens

User has 1,000 tokens/hour budget

Redis token bucket:
────────────────────────────────────────────────
Key:   rate_limit:user_001:tokens
Value: 850  (remaining tokens)

When expensive request arrives:
cost = 50 tokens

Lua script:
local key = KEYS[1]
local cost = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])

local tokens = tonumber(redis.call('GET', key) or capacity)

if tokens >= cost then
  redis.call('DECRBY', key, cost)
  return {1, tokens - cost}  -- Allow
else
  return {0, tokens}  -- Reject
end

EVAL script 1 rate_limit:user_001:tokens 50 1000

Response headers:
X-RateLimit-Limit: 1000 (tokens)
X-RateLimit-Remaining: 800
X-RateLimit-Cost: 50 (this request)
```

### Distributed Rate Limiting Across Data Centers

```
MULTI-REGION CHALLENGE:
════════════════════════════════════════════════════════

User makes requests to different regions:
- 50 requests → US-East
- 30 requests → EU-West
- 20 requests → Asia-Pacific

Limit: 100 requests/hour globally

Problem: Each region has separate Redis
→ US-East Redis: 50 requests
→ EU-West Redis: 30 requests
→ Asia-Pacific Redis: 20 requests
→ Total: 100 (at limit)

But no region knows about others!
User could send 100 to each region → 300 total!


Solution 1: Centralized Redis (simple, slow)
────────────────────────────────────────────────
Single Redis cluster in one region
All regions check this Redis

Pros:
✓ Accurate
✓ Simple

Cons:
✗ Cross-region latency (100-300ms)
✗ Single point of failure


Solution 2: Gossip Protocol (complex, accurate)
────────────────────────────────────────────────
Each region maintains local count
Periodically sync counts between regions

Every 10 seconds:
US-East broadcasts: "user_001 has 50 requests"
EU-West broadcasts: "user_001 has 30 requests"
Asia-Pacific broadcasts: "user_001 has 20 requests"

Each region calculates global: 50 + 30 + 20 = 100

Pros:
✓ Low latency (local checks)
✓ Eventually accurate

Cons:
✗ Complex implementation
✗ Temporary over-limit possible (during sync delay)


Solution 3: Regional Quotas (pragmatic)
────────────────────────────────────────────────
Split global limit across regions

Global limit: 100 requests/hour
Split: 
- US-East: 40 requests/hour
- EU-West: 30 requests/hour
- Asia-Pacific: 30 requests/hour

Each region enforces its quota locally

Pros:
✓ Simple
✓ Fast (local Redis)
✓ No cross-region communication

Cons:
✗ Inflexible (user might use only one region)
✗ Under-utilization (if user uses only one region)

Most companies use this pragmatic approach
```

---

## Schema Architecture Summary

```
REDIS KEYS:
════════════════════════════════════════════════════════

Fixed Window:
────────────────────────────────────────────────
rate_limit:user_001:2024-02-26:10:30  → 45
rate_limit:user_002:2024-02-26:10:30  → 12
TTL: 60 seconds


Sliding Window Log:
────────────────────────────────────────────────
rate_limit:user_001:log  (Sorted Set)
  1708945200.123 → req_abc
  1708945200.456 → req_xyz
  1708945201.789 → req_def
TTL: 3600 seconds


Sliding Window Counter:
────────────────────────────────────────────────
rate_limit:user_001:2024-02-26:10:30  → 45
rate_limit:user_001:2024-02-26:10:29  → 98
TTL: 120 seconds (2 windows)


Token Bucket:
────────────────────────────────────────────────
rate_limit:user_001:bucket  (Hash)
  tokens → 85
  last_refill → 1708945200
TTL: 3600 seconds


Configuration Cache:
────────────────────────────────────────────────
rate_limit_config:/api/search:free  (Hash)
  limit → 100
  window → 3600
  algorithm → sliding_window
TTL: 300 seconds (5 minutes)


POSTGRESQL TABLES:
════════════════════════════════════════════════════════

rate_limit_rules:
  - rule_id (PK)
  - resource, tier, limit, window, algorithm
  
user_tiers:
  - user_id (PK), tier, upgraded_at
  
rate_limit_events (audit):
  - event_id (PK), user_id, rule_id, action, timestamp
```

---

## Tradeoffs vs Other Databases

```
┌───────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                           │ THIS ARCH    │ POSTGRES ALL │ MEMCACHED    │ DYNAMODB     │
├───────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Latency (<1ms)            │ Redis ✓      │ 10ms+ ✗      │ Memcached✓   │ 5-10ms       │
│ Atomic operations         │ INCR+Lua ✓   │ Transactions │ NO ✗         │ Conditional✓ │
│ TTL auto-expiration       │ Native ✓     │ Manual ✗     │ Native ✓     │ Native ✓     │
│ Sorted sets (sliding)     │ Native ✓     │ Manual       │ NO ✗         │ NO ✗         │
│ Lua scripts               │ Native ✓     │ NO ✗         │ NO ✗         │ NO ✗         │
│ High availability         │ Cluster ✓    │ Replication  │ Manual       │ Native ✓     │
│ Config queries            │ PostgreSQL✓  │ PostgreSQL✓  │ NO ✗         │ Limited      │
│ Audit trail               │ PostgreSQL✓  │ PostgreSQL✓  │ NO ✗         │ Possible     │
│ Cost at scale             │ LOW ✓        │ MEDIUM       │ LOW ✓        │ HIGH         │
│ Operational complexity    │ MEDIUM       │ LOW          │ LOW          │ LOW (managed)│
└───────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **Redis stores rate limit counters and timestamps because sub-millisecond in-memory operations are mandatory when checking limits on 100K requests/second and atomic INCR commands prevent race conditions where three concurrent requests at count=99 would all see 99, increment to 100, and all pass when limit is 100 (losing 2 requests), while Lua scripts enable complex sliding window algorithms to execute atomically without interleaving from other requests — automatic TTL expiration handles cleanup where fixed window counters expire after 60 seconds and sliding window logs expire after 3600 seconds without background jobs scanning millions of keys like PostgreSQL's "DELETE FROM rate_limits WHERE timestamp < NOW() - INTERVAL '1 hour'" would attempt — sorted sets implement sliding window logs storing (timestamp, request_id) pairs allowing ZREMRANGEBYSCORE to remove expired entries and ZCARD to count remaining requests in O(log N + M) time versus PostgreSQL's full table scans on billions of rows even with timestamp indexes — PostgreSQL stores configuration rules and user tier mappings because queries like "get applicable rate limit WHERE resource='/api/search' AND tier=user.tier AND enabled=true ORDER BY priority" require JOINs and complex conditionals that key-value stores cannot express, while audit logs tracking which users hit which limits enable analytics queries like "COUNT violations per user in last day GROUP BY user_id" that inform billing and abuse detection — Redis Cluster with 3 masters and 3 replicas provides high availability where automatic failover promotes replicas to masters within 30 seconds when crashes occur, and circuit breaker pattern implements fail-open behavior that bypasses rate limiting when Redis is unavailable rather than making the entire API unavailable since temporary over-limit requests are better than a completely dead API — hybrid sliding window counter algorithm combines fixed window efficiency (two integer counters per user) with sliding window accuracy (weighted average eliminates boundary burst problem where 100 requests at 10:29:59 plus 100 at 10:30:00 gives 200 requests in one second) making it the production standard over memory-intensive sliding window logs that store every request timestamp separately.**