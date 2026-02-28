
## The Core Problem Tinder Solves

On the surface a dating app seems simple — show nearby people, let users swipe. But consider the real constraints:

```
Tinder scale reality:
────────────────────────────────────────────────
Active users:              75 million+
Daily swipes:              1.6 billion+ (21K swipes/second sustained)
Average swipes per user:   ~140 per day
Matches created:           26 million per day (300 matches/second)
Geographical distribution: Global (230+ countries)
Location updates:          Every app open, every background refresh

Requirements:
→ Show users within 5-50 miles based on user preference
→ Never show same profile twice (already swiped)
→ Never show profiles user doesn't match criteria (age, gender, distance)
→ Detect mutual match instantly (both swiped right)
→ Update recommendations when user changes location
→ Handle user in Tokyo getting recommendations, flying to NYC, getting new recommendations
→ ML model ranks profiles by likelihood of match
→ Load next card <100ms (instant swipe feel)
→ Store billions of swipes for analytics/ML training
```

This combination of **geospatial queries + preventing duplicates at billion-swipe scale + instant mutual match detection + real-time location-aware recommendations + ML-powered ranking** is what forces this specific architecture.

---

## Why PostgreSQL with PostGIS (Geo-Sharded)?

### The Geospatial Query Problem

```
CORE QUERY:
════════════════════════════════════════════════════════

"Show me all users within 10 miles of my current location
 who match my preferences (age 25-35, female, want relationship)
 and I match their preferences (age 28-40, male, want relationship)
 and I haven't already swiped on
 ordered by likelihood of mutual match"

This query runs:
- Every time user opens app (75M users)
- After every swipe (1.6B times/day)
- 21,000 times per second sustained
```

### Why PostGIS Extension in PostgreSQL?

```
POSTGIS SPATIAL INDEX:
════════════════════════════════════════════════════════

Users table with location:
────────────────────────────────────────────────
user_id  │ location           │ age │ gender │ preferences
─────────────────────────────────────────────────────────────
user_001 │ POINT(37.7749 -122.4194) │ 28  │ M      │ {...}
user_002 │ POINT(37.7849 -122.4094) │ 26  │ F      │ {...}
user_003 │ POINT(40.7128 -74.0060)  │ 30  │ F      │ {...}

location column type: GEOGRAPHY (PostGIS)


Create spatial index:
────────────────────────────────────────────────
CREATE INDEX idx_location ON users 
USING GIST (location);

GIST (Generalized Search Tree) = spatial index
Organizes points geographically
Uses bounding boxes and R-tree structure
[[
]]
Query: "Find users within 10 miles"
────────────────────────────────────────────────
SELECT user_id, name, age, photos
FROM users
WHERE ST_DWithin(
  location,
  ST_MakePoint(-122.4194, 37.7749)::geography,
  16093  -- 10 miles in meters
)
AND age BETWEEN 25 AND 35
AND gender = 'F'
AND user_id NOT IN (
  SELECT swiped_id FROM swipes WHERE swiper_id = 'user_001'
)
LIMIT 100;

PostGIS internals:
1. GIST index finds candidates in geographic bounding box
2. Precise distance calculation only on candidates
3. Filter by age, gender, etc.
4. Exclude already swiped

Query time: 10-50ms (depends on density)
```

### Why Not MongoDB Geospatial?

```
MONGODB GEOSPATIAL:
════════════════════════════════════════════════════════

MongoDB has 2dsphere index:

db.users.createIndex({ location: "2dsphere" })

db.users.find({
  location: {
    $near: {
      $geometry: { type: "Point", coordinates: [-122.4194, 37.7749] },
      $maxDistance: 16093  // 10 miles in meters
    }
  },
  age: { $gte: 25, $lte: 35 },
  gender: "F",
  user_id: { $nin: already_swiped_ids }
}).limit(100)


Pros:
✓ Geospatial queries work
✓ Fast at moderate scale
✓ Flexible schema (good for varying profile data)

Cons:
✗ Complex multi-field queries slower than PostgreSQL
✗ $nin (not in) with large arrays is very slow
✗ At 1.6B swipes, already_swiped_ids array becomes huge
✗ No efficient way to handle "bidirectional preference match"
✗ Weaker transaction support (needed for match creation)


When to use MongoDB for dating:
────────────────────────────────────────────────
→ Smaller scale (<1M users)
→ Simple preference matching
→ Don't need complex relational queries
→ Want flexible profile schema


Why PostgreSQL wins here:
────────────────────────────────────────────────
✓ PostGIS is industry-leading for geospatial
✓ Complex joins (preferences, exclusions) are natural
✓ NOT IN subquery optimized with indexes
✓ ACID transactions for match creation
✓ Mature, battle-tested at scale
✓ Can handle billions of rows efficiently
```

### Geo-Sharding Strategy

```
WHY SHARD BY GEOGRAPHY:
════════════════════════════════════════════════════════

Problem: 75M users worldwide in one database
→ Queries scan millions of rows even with indexes
→ Single database bottleneck

Solution: Shard by region
────────────────────────────────────────────────
Shard 1: North America
  users: 30M
  Database: postgres_na

Shard 2: Europe
  users: 20M
  Database: postgres_eu

Shard 3: Asia-Pacific
  users: 15M
  Database: postgres_apac

Shard 4: South America
  users: 5M
  Database: postgres_sa

Shard 5: Rest of World
  users: 5M
  Database: postgres_row


User in San Francisco:
────────────────────────────────────────────────
Location: (37.7749, -122.4194)
→ Route to postgres_na
→ Query only 30M users (not 75M)
→ Faster queries
→ Regional data isolation (GDPR compliance)


User travels Tokyo → NYC:
────────────────────────────────────────────────
1. User opens app in Tokyo
   → Query postgres_apac
   → Show Tokyo matches

2. User flies to NYC
   → Location update detected
   → Query postgres_na
   → Show NYC matches

3. Background job (optional):
   → Migrate user record from apac → na
   → For long-term stays
```

---

## Why Graph Database (Neo4j) for Matches?

### The Mutual Match Problem

```
WHAT IS A MATCH?
════════════════════════════════════════════════════════

Match occurs when:
User A swipes right on User B
AND
User B swipes right on User A

This is a BIDIRECTIONAL relationship
Perfect for graph representation
```

### Neo4j Graph Model

```
GRAPH SCHEMA:
════════════════════════════════════════════════════════

Nodes: Users
────────────────────────────────────────────────
(:User {
  user_id: 'user_001',
  name: 'Alice',
  age: 28
})

Relationships: Swipes and Matches
────────────────────────────────────────────────
(user_001)-[:SWIPED_RIGHT {timestamp: 1708945200}]->(user_002)
(user_002)-[:SWIPED_RIGHT {timestamp: 1708945300}]->(user_001)

When both exist:
(user_001)-[:MATCHED {timestamp: 1708945300}]-(user_002)


Detecting mutual match:
────────────────────────────────────────────────
User A swipes right on User B

Query: "Did User B already swipe right on User A?"
MATCH (a:User {user_id: 'user_001'})<-[:SWIPED_RIGHT]-(b:User {user_id: 'user_002'})
RETURN b

If exists → CREATE MATCH relationship
If not exists → Just record swipe, wait


Query: "Show me all my matches"
────────────────────────────────────────────────
MATCH (me:User {user_id: 'user_001'})-[:MATCHED]-(match)
RETURN match
ORDER BY match.timestamp DESC

Simple, fast graph traversal
```

### Why PostgreSQL Can Also Work for Matches

```
POSTGRESQL APPROACH (simpler at small-medium scale):
════════════════════════════════════════════════════════

Swipes table:
────────────────────────────────────────────────────────
swiper_id │ swiped_id │ action    │ timestamp
──────────────────────────────────────────────────────────
user_001  │ user_002  │ like      │ 2024-02-26 10:00:00
user_001  │ user_003  │ pass      │ 2024-02-26 10:00:05
user_002  │ user_001  │ like      │ 2024-02-26 10:05:00  ← MATCH!
user_003  │ user_001  │ pass      │ 2024-02-26 10:06:00

PRIMARY KEY (swiper_id, swiped_id)
INDEX (swiped_id, swiper_id, action)  ← critical for match detection


Detecting mutual match:
────────────────────────────────────────────────
User A swipes right on User B

Check if User B already swiped right on User A:
SELECT swiper_id FROM swipes
WHERE swiper_id = 'user_002'
AND swiped_id = 'user_001'
AND action = 'like'

If row exists → MATCH!
If no row → Just record swipe


Record swipe:
INSERT INTO swipes (swiper_id, swiped_id, action, timestamp)
VALUES ('user_001', 'user_002', 'like', NOW());


If match detected:
INSERT INTO matches (user1_id, user2_id, matched_at)
VALUES ('user_001', 'user_002', NOW());

Trigger notification service


Query: "Show me all my matches"
────────────────────────────────────────────────
SELECT 
  CASE 
    WHEN user1_id = 'user_001' THEN user2_id
    ELSE user1_id
  END as match_id,
  matched_at
FROM matches
WHERE user1_id = 'user_001' OR user2_id = 'user_001'
ORDER BY matched_at DESC;


WHEN TO USE POSTGRESQL VS GRAPH DB:
════════════════════════════════════════════════════════

Use PostgreSQL when:
────────────────────────────────────────────────
✓ Scale < 50M users
✓ Simple match logic (just bidirectional swipes)
✓ Want simpler operational model (one database)
✓ Team familiar with SQL


Use Graph DB (Neo4j) when:
────────────────────────────────────────────────
✓ Scale > 50M users
✓ Complex relationship queries needed:
  - "Friends of matches" (social graph integration)
  - "People in my extended network"
  - "Matches with mutual friends"
  - "Dating network clustering/communities"
✓ Need sophisticated graph algorithms
✓ Have ops team comfortable with Neo4j


TINDER'S CHOICE:
────────────────────────────────────────────────
At Tinder's scale (75M users, 26M matches/day):
→ PostgreSQL is sufficient for matches
→ Complexity of Neo4j not justified
→ Simpler to keep matches in same DB as profiles

At Facebook Dating scale (integrating 3B user social graph):
→ Neo4j makes sense
→ Leverage existing social connections
→ Complex graph traversals common
```

---

## Why Redis for Recommendations?

### The Real-Time Recommendation Problem

```
RECOMMENDATION PIPELINE:
════════════════════════════════════════════════════════

When user opens app, need to:
1. Fetch candidates (geo-query PostgreSQL)
2. Filter already swiped (exclude billions of swipes)
3. Rank by ML model (complex scoring)
4. Return top 100 profiles
5. Load instantly (<100ms total)

All of this must happen 21,000 times per second
```

### Redis Recommendation Queue

```
REDIS SCHEMA:
════════════════════════════════════════════════════════

Pre-computed recommendation queue:
────────────────────────────────────────────────
Key:   rec:user_001
Type:  List
Value: [user_789, user_456, user_123, ...]  (ordered by ML score)

LPUSH rec:user_001 "user_789"  ← highest score
LPUSH rec:user_001 "user_456"
LPUSH rec:user_001 "user_123"
...

LRANGE rec:user_001 0 9  ← get next 10 recommendations
TTL: 1 hour (refresh hourly)


When user swipes:
────────────────────────────────────────────────
User swipes on user_789 (top recommendation)

LPOP rec:user_001  ← remove from queue
→ Returns: "user_789"

Next card shows: user_456 (now at top)


Bloom filter for already seen:
────────────────────────────────────────────────
Key:   seen:user_001
Type:  String (Bloom filter encoded)

BF.ADD seen:user_001 "user_002"
BF.ADD seen:user_001 "user_003"
BF.ADD seen:user_001 "user_004"

BF.EXISTS seen:user_001 "user_002"
→ Returns: 1 (probably seen)

BF.EXISTS seen:user_001 "user_999"
→ Returns: 0 (definitely not seen)

Bloom filter properties:
- False positive rate: ~1% (acceptable)
- Space: 10KB for 100K seen users (vs 100KB for exact set)
- No false negatives (if says "not seen", definitely not seen)


ML score cache:
────────────────────────────────────────────────
Key:   score:user_001:user_789
Value: 0.87  (ML model predicted match probability)
TTL:   24 hours

Precomputed by ML service
Avoids expensive model inference on every swipe
```

### Background ML Recommendation Service

```
ML RECOMMENDATION FLOW:
════════════════════════════════════════════════════════

Background job runs every hour for each user:

1. Fetch candidates from PostgreSQL:
────────────────────────────────────────────────
SELECT user_id, profile_data, photos
FROM users
WHERE ST_DWithin(location, user_location, max_distance)
AND age BETWEEN min_age AND max_age
AND gender = preferred_gender
AND user_id NOT IN (SELECT swiped_id FROM swipes WHERE swiper_id = :user_id)
LIMIT 1000  ← get large candidate pool


2. Score each candidate with ML model:
────────────────────────────────────────────────
For each candidate:
  features = extract_features(user, candidate)
  # Features: photo attractiveness, bio similarity, 
  #           education match, distance, activity level
  
  score = ml_model.predict(features)
  # Returns: 0.0 - 1.0 (probability of mutual like)


3. Sort by score and write to Redis:
────────────────────────────────────────────────
sorted_candidates = sort_by_score(candidates)

DEL rec:user_001  # Clear old recommendations
for candidate in sorted_candidates[:100]:  # Top 100
  RPUSH rec:user_001 candidate.user_id

EXPIRE rec:user_001 3600  # 1 hour TTL


4. Cache individual scores:
────────────────────────────────────────────────
for candidate in sorted_candidates:
  SET score:user_001:candidate.user_id candidate.score EX 86400
```

---

## Complete Schema Architecture

```
POSTGRESQL SCHEMA (geo-sharded):
════════════════════════════════════════════════════════

Users table:
────────────────────────────────────────────────────────────────────────────────
user_id  │ name    │ age │ gender │ location               │ bio        │ photos     │ preferences
─────────────────────────────────────────────────────────────────────────────────────────────────────────────
user_001 │ Alice   │ 28  │ F      │ POINT(37.7749 -122.42) │ "Love..."  │ [url1,...] │ {"age_min":25,...}
user_002 │ Bob     │ 32  │ M      │ POINT(37.7849 -122.41) │ "Looking..." │[url2,...] │ {"age_min":23,...}
user_003 │ Charlie │ 26  │ M      │ POINT(40.7128 -74.006) │ "Outdoors..."│[url3,...] │ {"age_min":21,...}

Column types:
  location: GEOGRAPHY(POINT, 4326)  ← WGS84 coordinate system
  preferences: JSONB  ← flexible preference storage

Indexes:
  PRIMARY KEY (user_id)
  CREATE INDEX idx_location ON users USING GIST (location)
  CREATE INDEX idx_age_gender ON users (age, gender)
  CREATE INDEX idx_active ON users (last_active) WHERE active = true

Partitioning:
  Partition by location (geo-shard)
  Or by user_id hash for simpler sharding


Swipes table:
────────────────────────────────────────────────────────────────────────
swiper_id │ swiped_id │ action │ timestamp           │ session_id
────────────────────────────────────────────────────────────────────────────
user_001  │ user_002  │ like   │ 2024-02-26 10:00:00 │ sess_abc
user_001  │ user_003  │ pass   │ 2024-02-26 10:00:05 │ sess_abc
user_002  │ user_001  │ like   │ 2024-02-26 10:05:00 │ sess_def  ← MATCH!
user_003  │ user_001  │ pass   │ 2024-02-26 10:06:00 │ sess_ghi

PRIMARY KEY (swiper_id, swiped_id)
CREATE INDEX idx_reverse ON swipes (swiped_id, swiper_id, action)
  ← critical for fast match detection

Partitioning:
  Partition by timestamp (monthly partitions)
  Old swipes (>1 year) archived to cold storage


Matches table:
────────────────────────────────────────────────────────────────────────
match_id │ user1_id │ user2_id │ matched_at          │ conversation_started
────────────────────────────────────────────────────────────────────────────────
match_01 │ user_001 │ user_002 │ 2024-02-26 10:05:00 │ true
match_02 │ user_001 │ user_004 │ 2024-02-26 11:00:00 │ false

Indexes:
  PRIMARY KEY (match_id)
  INDEX (user1_id, matched_at DESC)
  INDEX (user2_id, matched_at DESC)
  INDEX (matched_at) WHERE conversation_started = false  ← unmessaged matches


Messages table:
────────────────────────────────────────────────────────────────────────
message_id │ match_id │ sender_id │ content      │ timestamp           │ read
────────────────────────────────────────────────────────────────────────────────
msg_001    │ match_01 │ user_001  │ "Hi there!"  │ 2024-02-26 10:06:00 │ true
msg_002    │ match_01 │ user_002  │ "Hey!"       │ 2024-02-26 10:07:00 │ true

Indexes:
  PRIMARY KEY (message_id)
  INDEX (match_id, timestamp)
  INDEX (sender_id, timestamp)


REDIS SCHEMA:
════════════════════════════════════════════════════════

Recommendation queue (per user):
────────────────────────────────────────────────
Key:   rec:user_001
Type:  List
Value: ["user_789", "user_456", "user_123", ...]

LLEN rec:user_001  → get queue size
LRANGE rec:user_001 0 9  → get next 10
LPOP rec:user_001  → remove after swipe
TTL: 3600 (1 hour)


Bloom filter (already seen profiles):
────────────────────────────────────────────────
Key:   seen:user_001
Type:  Bloom Filter (RedisBloom module)

BF.ADD seen:user_001 "user_002"
BF.EXISTS seen:user_001 "user_002"  → 1 (probably seen)
BF.EXISTS seen:user_001 "user_999"  → 0 (definitely not seen)

Size: ~10KB for 100K items (1% false positive rate)
TTL: 30 days


ML score cache:
────────────────────────────────────────────────
Key:   score:user_001:user_789
Value: "0.87"
TTL:   86400 (24 hours)


User session data:
────────────────────────────────────────────────
Key:   session:sess_abc
Type:  Hash

HSET session:sess_abc
  user_id "user_001"
  started_at "1708945200"
  swipe_count "15"
  match_count "2"
  
TTL: 3600 (1 hour)


Active users (online status):
────────────────────────────────────────────────
Key:   active:user_001
Value: "1"
TTL:   300 (5 minutes)

Refreshed on every action
Used for "Active now" badge


GRAPH DATABASE SCHEMA (Neo4j - if used):
════════════════════════════════════════════════════════

User nodes:
────────────────────────────────────────────────
(:User {
  user_id: 'user_001',
  name: 'Alice',
  age: 28,
  gender: 'F'
})


Swipe relationships:
────────────────────────────────────────────────
(user_001)-[:SWIPED_RIGHT {timestamp: 1708945200}]->(user_002)
(user_001)-[:SWIPED_LEFT {timestamp: 1708945205}]->(user_003)
(user_002)-[:SWIPED_RIGHT {timestamp: 1708945300}]->(user_001)


Match relationship (bidirectional):
────────────────────────────────────────────────
(user_001)-[:MATCHED {timestamp: 1708945300}]-(user_002)


Mutual friends (if integrated with social graph):
────────────────────────────────────────────────
(user_001)-[:FB_FRIEND]-(user_004)-[:FB_FRIEND]-(user_002)

Query: "Does my match have mutual friends?"
MATCH (me:User {user_id: 'user_001'})-[:MATCHED]-(match)
MATCH (me)-[:FB_FRIEND]-(mutual)-[:FB_FRIEND]-(match)
RETURN mutual
```

---

## Complete Database Flow

```
FLOW 1: User Opens App
════════════════════════════════════════════════════════

User_001 opens Tinder app in San Francisco
        │
        ▼
GET /api/recommendations?user_id=user_001&location=37.7749,-122.4194
        │
        ▼
STEP 1: Check Redis recommendation queue
────────────────────────────────────────────────
LLEN rec:user_001

Returns: 0 (queue empty or expired)
→ Need to generate recommendations


STEP 2: Fetch candidates from PostgreSQL (geo-shard NA)
────────────────────────────────────────────────
User preferences: age 25-35, female, max_distance 10 miles

SELECT user_id, name, age, photos, bio
FROM users
WHERE ST_DWithin(
  location,
  ST_MakePoint(-122.4194, 37.7749)::geography,
  16093  -- 10 miles in meters
)
AND age BETWEEN 25 AND 35
AND gender = 'F'
AND active = true
AND user_id NOT IN (
  SELECT swiped_id FROM swipes WHERE swiper_id = 'user_001'
)
LIMIT 1000;

Returns: 847 candidates
Query time: 20ms (PostGIS spatial index)


STEP 3: Filter using Bloom filter (Redis)
────────────────────────────────────────────────
For each candidate:
  BF.EXISTS seen:user_001 candidate.user_id
  
  If exists (probably seen): exclude
  If not exists: keep

Filtered candidates: 820 (27 excluded by Bloom filter)


STEP 4: Score with ML model
────────────────────────────────────────────────
Check cache first:
MGET score:user_001:cand1 score:user_001:cand2 ...

Cache hits: 200 candidates (24%)
Cache misses: 620 candidates (76%)

For cache misses, run ML model:
features = extract(user_001, candidate)
score = model.predict(features)  # 0.0 - 1.0

Batch scoring: 100ms for 620 candidates


STEP 5: Sort and store in Redis queue
────────────────────────────────────────────────
sorted_candidates = sort_by_score(candidates)[:100]

DEL rec:user_001
for candidate in sorted_candidates:
  RPUSH rec:user_001 candidate.user_id

EXPIRE rec:user_001 3600

Cache new scores:
for candidate in sorted_candidates:
  SET score:user_001:candidate.user_id score EX 86400


STEP 6: Fetch top 10 profiles
────────────────────────────────────────────────
top_10_ids = LRANGE rec:user_001 0 9

SELECT user_id, name, age, photos, bio, preferences
FROM users
WHERE user_id IN (top_10_ids)

Batch fetch: <5ms


Response to client:
{
  profiles: [
    {user_id: 'user_789', name: 'Sarah', age: 27, photos: [...], ...},
    {user_id: 'user_456', name: 'Emma', age: 29, photos: [...], ...},
    ...
  ]
}

Total time: 20+10+100+10+5 = 145ms
First load (cold cache) ✓

Subsequent loads (warm cache):
Redis queue fetch: <5ms
Profile details: <5ms
Total: <10ms ✓
```

```
FLOW 2: User Swipes Right (Likes Profile)
════════════════════════════════════════════════════════

User_001 swipes right on user_002
        │
        ▼
POST /api/swipe
{
  swiper_id: 'user_001',
  swiped_id: 'user_002',
  action: 'like',
  location: {lat: 37.7749, lng: -122.4194}
}
        │
        ▼
STEP 1: Record swipe in PostgreSQL
────────────────────────────────────────────────
INSERT INTO swipes (swiper_id, swiped_id, action, timestamp, session_id)
VALUES ('user_001', 'user_002', 'like', NOW(), 'sess_abc');

Time: <5ms


STEP 2: Check for mutual match (reverse lookup)
────────────────────────────────────────────────
SELECT swiper_id FROM swipes
WHERE swiper_id = 'user_002'
AND swiped_id = 'user_001'
AND action = 'like';

Uses index: idx_reverse (swiped_id, swiper_id, action)
Time: <2ms

Result: 1 row found → MATCH!
(user_002 already liked user_001)


STEP 3: Create match (if mutual)
────────────────────────────────────────────────
BEGIN TRANSACTION;

INSERT INTO matches (match_id, user1_id, user2_id, matched_at)
VALUES ('match_123', 'user_001', 'user_002', NOW());

COMMIT;


STEP 4: Update Bloom filter
────────────────────────────────────────────────
BF.ADD seen:user_001 "user_002"

Prevent showing user_002 again


STEP 5: Remove from recommendation queue
────────────────────────────────────────────────
LPOP rec:user_001

Next card will show user_456


STEP 6: Send match notification (if match occurred)
────────────────────────────────────────────────
Publish to Kafka:
topic: "match_events"
event: {
  type: "NEW_MATCH",
  match_id: "match_123",
  user1_id: "user_001",
  user2_id: "user_002",
  matched_at: 1708945200
}

Downstream consumers:
- Push notification service → "It's a Match! 💘"
- Analytics service → track match rate
- Email service → send match email


Response to client:
{
  action: 'like',
  match: true,  ← indicate mutual match
  match_id: 'match_123',
  profile: {user_id: 'user_002', name: 'Sarah', ...}
}

UI shows: "It's a Match!" animation

Total time: 5+2+3+1+1 = 12ms
```

```
FLOW 3: User Changes Location (travels)
════════════════════════════════════════════════════════

User_001 travels from San Francisco to New York
        │
        ▼
App detects location change
        │
        ▼
PUT /api/user/location
{
  user_id: 'user_001',
  location: {lat: 40.7128, lng: -74.0060}
}
        │
        ▼
STEP 1: Update location in PostgreSQL
────────────────────────────────────────────────
UPDATE users
SET location = ST_MakePoint(-74.0060, 40.7128)::geography,
    last_location_update = NOW()
WHERE user_id = 'user_001';


STEP 2: Determine new geo-shard (if cross-shard)
────────────────────────────────────────────────
Old location: San Francisco → NA shard
New location: New York → Still NA shard
→ No shard migration needed

If traveled to London:
→ Migrate from NA shard → EU shard
→ Background job copies user record
→ Update routing table


STEP 3: Invalidate old recommendation queue
────────────────────────────────────────────────
DEL rec:user_001
DEL seen:user_001  ← optional: allow re-seeing profiles in new city

User will get fresh recommendations based on NYC location


STEP 4: Background job generates new recommendations
────────────────────────────────────────────────
Fetch NYC candidates:
WHERE ST_DWithin(location, NYC_point, 16093)
...

Score with ML model
Store in rec:user_001

Next time user opens app:
→ Sees NYC profiles instantly
```

```
FLOW 4: User Receives Match Notification
════════════════════════════════════════════════════════

Match created (from Flow 2)
        │
        ▼
Kafka consumer (Notification Service)
        │
        ▼
STEP 1: Check if users have push enabled
────────────────────────────────────────────────
SELECT push_enabled, device_tokens
FROM user_settings
WHERE user_id IN ('user_001', 'user_002')

Both enabled ✓


STEP 2: Fetch match profile details
────────────────────────────────────────────────
SELECT name, photos[1] as photo
FROM users
WHERE user_id IN ('user_001', 'user_002')


STEP 3: Send push notifications (via FCM/APNS)
────────────────────────────────────────────────
To user_001:
"It's a Match! 💘 You and Sarah liked each other"

To user_002:
"It's a Match! 💘 You and Alice liked each other"


STEP 4: Update match UI state
────────────────────────────────────────────────
WebSocket broadcast to connected clients:
{
  type: 'NEW_MATCH',
  match_id: 'match_123',
  match_profile: {...}
}

If user has app open:
→ Match screen appears immediately
→ No refresh needed
```

```
FLOW 5: Background ML Recommendation Refresh
════════════════════════════════════════════════════════

Cron job runs every hour for each active user
        │
        ▼
For user_001:
────────────────────────────────────────────────

STEP 1: Check if refresh needed
────────────────────────────────────────────────
LLEN rec:user_001

Returns: 45 (still have recommendations)
Returns: 0 (queue empty) → refresh needed


STEP 2: Fetch fresh candidates
────────────────────────────────────────────────
Same geo-query as Flow 1
+ Exclude users added to Bloom filter in last hour


STEP 3: Run ML model (batch inference)
────────────────────────────────────────────────
Load ML model in memory
features = extract_batch(user_001, candidates)
scores = model.predict_batch(features)

Optimized: GPU inference for batch
Process 1000 candidates in 50ms


STEP 4: Update Redis queue
────────────────────────────────────────────────
sorted_candidates = sort_by_score(candidates)[:100]

DEL rec:user_001
for candidate in sorted_candidates:
  RPUSH rec:user_001 candidate.user_id

EXPIRE rec:user_001 3600


This ensures users always have fresh recommendations
Even if they swipe through entire queue
```

---

## Tradeoffs vs Other Databases

```
┌─────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                         │ THIS ARCH    │ MONGO ALL    │ DYNAMODB ALL │ REDIS ALL    │
├─────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Geospatial queries      │ PostGIS ✓    │ 2dsphere ✓   │ Limited      │ Geo ✓        │
│ Complex preference join │ PostgreSQL✓  │ Aggregation  │ Manual ✗     │ NO ✗         │
│ Match detection         │ PostgreSQL✓  │ Possible     │ Possible     │ NO ✗         │
│ Bloom filter            │ Redis ✓      │ Manual       │ Manual       │ Native ✓     │
│ ML score cache          │ Redis ✓      │ MongoDB      │ DynamoDB     │ Native ✓     │
│ ACID transactions       │ PostgreSQL✓  │ Limited      │ Limited      │ NO ✗         │
│ Geo-sharding            │ Native ✓     │ Native ✓     │ Native ✓     │ Manual       │
│ Billions of swipes      │ Partitioned✓ │ Sharded ✓    │ Native ✓     │ NO ✗         │
│ Operational complexity  │ MEDIUM       │ MEDIUM       │ LOW (managed)│ LOW          │
│ Cost at 75M users       │ MEDIUM       │ HIGH         │ VERY HIGH    │ MEDIUM       │
└─────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **PostgreSQL with PostGIS extension stores user profiles geo-sharded by region because geospatial queries "find users within 10 miles" use GIST spatial indexes giving 20ms response times on millions of users versus MongoDB's 2dsphere taking 50-100ms and lacking efficient complex joins for bidirectional preference matching ("I match their criteria AND they match mine"), while monthly partitioning of the swipes table keeps 1.6 billion daily swipes queryable by archiving old partitions to cold storage and the reverse index (swiped_id, swiper_id, action) enables 2ms mutual match detection when user A swipes right on user B who already swiped right on user A — Redis stores pre-computed recommendation queues as lists (LPUSH/LPOP) so each swipe removes the top card and shows the next in under 1ms versus generating recommendations on-demand by scoring 1000 candidates with the ML model taking 100ms, Bloom filters prevent showing already-seen profiles using only 10KB per 100K swipes with 1% false positive rate versus exact sets requiring 100KB and preventing the "user_id NOT IN (billions of swipes)" anti-join that would timeout in PostgreSQL, and ML score caching in Redis (score:user_X:user_Y keys with 24-hour TTL) avoids re-running expensive neural network inference on every swipe — geo-sharding by region (NA/EU/APAC databases) keeps 99% of queries single-shard since users only match within their geography and handles travel seamlessly by invalidating the recommendation queue and re-generating from the new shard when location changes by more than 50 miles, while Graph databases like Neo4j are overkill for simple bidirectional match detection that PostgreSQL's indexed reverse lookup handles in 2ms but become valuable at Facebook Dating scale when integrating social graphs to show "matches with mutual friends" through 2-hop graph traversals that would require expensive self-joins in SQL.**