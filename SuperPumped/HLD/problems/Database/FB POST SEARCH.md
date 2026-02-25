
## The Core Problem Facebook Post Search Solves

On the surface post search seems simple — find posts matching keywords. But consider the real constraints:

```
Facebook scale reality:
────────────────────────────────────────────────
Active users:              3 billion+
Posts per day:             350 million+ (4,000 posts/second)
Total posts:               Trillions (accumulated over 20 years)
Search queries:            Billions per day
Average query:             "recipe chocolate cake" (3-5 keywords)

Requirements:
→ Search across trillions of posts (<1 second response)
→ Full-text search (find "chocolate" in post content)
→ Fuzzy matching ("reciepe" → "recipe")
→ Phrase search ("New York City" as exact phrase)
→ Privacy enforcement (only show posts user can see)
→ Ranking by relevance + recency (recent popular posts first)
→ Filter by author, date range, tags, location
→ Autocomplete suggestions ("choc..." → "chocolate cake")
→ Handle typos and synonyms ("car" also matches "automobile")
→ Real-time indexing (new posts searchable within seconds)
```

This combination of **massive scale + full-text search + privacy filtering + complex ranking + real-time indexing** is what forces Elasticsearch as the primary database.

---

## Why Elasticsearch for Post Search?

### The Full-Text Search Problem

```
NAIVE SQL APPROACH (DOESN'T WORK):
════════════════════════════════════════════════════════

Posts table (PostgreSQL):
post_id │ user_id │ content
────────────────────────────────────────────────────────────────────
1       │ user_01 │ "Just made chocolate cake for my birthday!"
2       │ user_02 │ "Best recipe for chocolate brownies"
3       │ user_03 │ "Cake decorating tips"

Query: "chocolate cake recipe"

SQL LIKE approach:
SELECT * FROM posts
WHERE content LIKE '%chocolate%'
AND content LIKE '%cake%'
AND content LIKE '%recipe%';

Problems at Facebook scale:
────────────────────────────────────────────────
→ LIKE '%keyword%' requires full table scan
→ No index can help (wildcard at beginning)
→ 1 trillion posts × 500 bytes avg = 500TB to scan
→ Query takes hours (target: <1 second)
→ No relevance ranking (all matches equal)
→ No fuzzy matching (typos fail)
→ No phrase matching
→ Cannot handle synonyms


PostgreSQL Full-Text Search (GIN index):
────────────────────────────────────────────────
CREATE INDEX idx_content_fts ON posts 
USING GIN (to_tsvector('english', content));

SELECT * FROM posts
WHERE to_tsvector('english', content) @@ 
      to_tsquery('english', 'chocolate & cake & recipe');

Better, but still problems:
────────────────────────────────────────────────
→ GIN index grows to 50-100GB per billion posts
→ Index rebuild takes days after bulk inserts
→ Limited ranking (basic relevance only)
→ Poor fuzzy matching
→ Cannot handle complex scoring (recency + relevance)
→ Slow aggregations (faceted search)

At 1 trillion posts:
→ Index size: 50-100 TERABYTES
→ Unmanageable in single PostgreSQL instance
→ Sharding is extremely complex
```

### How Elasticsearch Solves This

```
ELASTICSEARCH INVERTED INDEX:
════════════════════════════════════════════════════════

Post 1: "Just made chocolate cake for my birthday!"
Post 2: "Best recipe for chocolate brownies"
Post 3: "Cake decorating tips"

After analysis (tokenization, lowercase, stemming):
────────────────────────────────────────────────

Inverted index:
Term           → Document IDs (with positions)
─────────────────────────────────────────────────────────
chocolate      → [1: position 2, 2: position 3]
cake           → [1: position 3, 3: position 0]
recipe         → [2: position 1]
made           → [1: position 1]
birthday       → [1: position 6]
brownie        → [2: position 4]
decorate       → [3: position 1]  (stemmed from "decorating")


Query: "chocolate cake recipe"
────────────────────────────────────────────────
Elasticsearch processes query:
1. Tokenize: ["chocolate", "cake", "recipe"]
2. Lookup each term in inverted index
3. Find matching documents:
   - chocolate → [1, 2]
   - cake → [1, 3]
   - recipe → [2]
4. Calculate relevance scores (TF-IDF)
5. Combine scores and rank

Results (ranked):
1. Post 1 (score: 8.5) - has "chocolate" AND "cake"
2. Post 2 (score: 6.2) - has "chocolate" AND "recipe"
3. Post 3 (score: 2.1) - has "cake" only

Query time: <100ms (even on billions of posts)
```

### Elasticsearch Core Advantages

```
1. INVERTED INDEX STRUCTURE:
════════════════════════════════════════════════════════

Traditional database:
Document → Terms (slow to search)

Inverted index:
Term → Documents (fast to search)

Lookup is O(1) hash table lookup
No table scan needed


2. DISTRIBUTED BY DESIGN:
────────────────────────────────────────────────

Elasticsearch cluster (5 nodes):
────────────────────────────────────────────────
Node 1: Posts 0-200M     (Shard 0)
Node 2: Posts 200M-400M  (Shard 1)
Node 3: Posts 400M-600M  (Shard 2)
Node 4: Posts 600M-800M  (Shard 3)
Node 5: Posts 800M-1B    (Shard 4)

Query: "chocolate cake"
→ Broadcast to all 5 shards (parallel)
→ Each shard searches its subset
→ Aggregate results
→ Total time: ~100ms (vs sequential PostgreSQL taking hours)


3. BUILT-IN TEXT ANALYSIS:
────────────────────────────────────────────────

Analyzers process text:
- Tokenization: "chocolate-cake" → ["chocolate", "cake"]
- Lowercase: "Chocolate" → "chocolate"
- Stemming: "running" → "run", "runs" → "run"
- Stopwords: Remove "the", "a", "is"
- Synonyms: "car" → ["car", "automobile", "vehicle"]
- Phonetic: "Steven" → matches "Stephen"

Example:
Input: "The BEST chocolate CAKES!"
After analysis: ["best", "chocol", "cake"]
(stemmed, lowercased, stopwords removed)


4. SCORING AND RANKING:
────────────────────────────────────────────────

TF-IDF scoring:
Term Frequency (TF): How often term appears in document
Inverse Document Frequency (IDF): How rare term is overall

"chocolate cake" in post:
- "chocolate" appears 3 times in this post (high TF)
- "chocolate" appears in 10% of posts (medium IDF)
- "cake" appears 1 time in this post (low TF)
- "cake" appears in 30% of posts (low IDF)

Score = TF × IDF
Post with "chocolate" 3 times ranks higher


5. FUZZY MATCHING:
────────────────────────────────────────────────

Query: "reciepe" (typo)

Elasticsearch fuzzy search (Levenshtein distance):
Edit distance = 2
Matches: "recipe" (2 character swaps)

Returns results for "recipe" automatically


6. PHRASE SEARCH:
────────────────────────────────────────────────

Query: "New York City" (exact phrase)

Inverted index with positions:
Post 1: "I love New York City" 
        Positions: New=3, York=4, City=5

Post 2: "York is far from New City"
        Positions: York=0, New=4, City=5

Elasticsearch checks positions:
Post 1: New(3), York(4), City(5) → consecutive ✓
Post 2: New(4), York(0), City(5) → not consecutive ✗

Only Post 1 returned


7. FACETED SEARCH (AGGREGATIONS):
────────────────────────────────────────────────

Query: "chocolate cake" 
+ Show counts by year, author, tags

Aggregations:
{
  "aggregations": {
    "posts_by_year": {
      "2024": 15000,
      "2023": 12000,
      "2022": 8000
    },
    "top_authors": {
      "chef_john": 500,
      "foodie_sarah": 300
    },
    "tags": {
      "recipe": 10000,
      "dessert": 8000,
      "baking": 6000
    }
  }
}

Computed in <100ms (parallel across shards)
PostgreSQL would need multiple GROUP BY queries (slow)
```

---

## Why PostgreSQL for Metadata?

### What Metadata Looks Like

```
POST METADATA (NOT SEARCHABLE TEXT):
════════════════════════════════════════════════════════

{
  post_id: "post_123",
  user_id: "user_456",
  created_at: "2024-02-26T10:00:00Z",
  updated_at: "2024-02-26T10:05:00Z",
  privacy: "friends",  // public, friends, only_me, custom
  allowed_users: ["user_789", "user_012"],  // if custom
  likes_count: 42,
  comments_count: 15,
  shares_count: 8,
  is_deleted: false,
  flagged: false,
  location: {
    lat: 37.7749,
    lng: -122.4194,
    name: "San Francisco, CA"
  }
}
```

This is structured, relational data.

### PostgreSQL Schema

```
POSTGRESQL SCHEMA (sharded by user_id):
════════════════════════════════════════════════════════

Posts table:
────────────────────────────────────────────────────────────────────────────────
post_id │ user_id │ created_at          │ privacy │ likes   │ comments │ is_deleted
───────────────────────────────────────────────────────────────────────────────────────────
post_123│ user_456│ 2024-02-26 10:00:00 │ friends │ 42      │ 15       │ false
post_124│ user_456│ 2024-02-25 15:30:00 │ public  │ 156     │ 48       │ false

Indexes:
  PRIMARY KEY (post_id)
  INDEX (user_id, created_at DESC)  ← user's timeline
  INDEX (created_at) WHERE is_deleted = false  ← recent posts


Post_Privacy table:
────────────────────────────────────────────────────────────────────────
post_id  │ privacy_type │ allowed_user_id
────────────────────────────────────────────────────────────────────────────────
post_125 │ custom       │ user_789
post_125 │ custom       │ user_012
post_126 │ friends_except│ user_999  (excluded)

For complex privacy rules


User_Friends table (social graph):
────────────────────────────────────────────────
user_id  │ friend_id │ friendship_status
────────────────────────────────────────────────────────
user_456 │ user_789  │ confirmed
user_456 │ user_012  │ confirmed
user_789 │ user_456  │ confirmed

Bidirectional friendship
```

### Complex Queries PostgreSQL Handles

```
QUERY 1: Privacy check
════════════════════════════════════════════════════════

"Can user_001 see post_123?"

WITH post_info AS (
  SELECT user_id, privacy 
  FROM posts 
  WHERE post_id = 'post_123'
)
SELECT 
  CASE
    WHEN privacy = 'public' THEN true
    WHEN privacy = 'only_me' AND user_id = 'user_001' THEN true
    WHEN privacy = 'friends' AND EXISTS (
      SELECT 1 FROM user_friends
      WHERE user_id = post_info.user_id
      AND friend_id = 'user_001'
      AND friendship_status = 'confirmed'
    ) THEN true
    WHEN privacy = 'custom' AND EXISTS (
      SELECT 1 FROM post_privacy
      WHERE post_id = 'post_123'
      AND allowed_user_id = 'user_001'
    ) THEN true
    ELSE false
  END as can_see
FROM post_info;

→ Complex conditional logic
→ Subqueries
→ JOINs with friend graph
→ Natural in SQL
→ Impossible in Elasticsearch


QUERY 2: Get user's visible posts
════════════════════════════════════════════════════════

"Get all post_ids user_001 can search"
(Used to pre-filter Elasticsearch results)

WITH user_friends AS (
  SELECT friend_id 
  FROM user_friends 
  WHERE user_id = 'user_001' 
  AND friendship_status = 'confirmed'
)
SELECT p.post_id
FROM posts p
WHERE p.is_deleted = false
AND (
  p.privacy = 'public'
  OR (p.privacy = 'friends' AND p.user_id IN (SELECT friend_id FROM user_friends))
  OR p.user_id = 'user_001'
  OR EXISTS (
    SELECT 1 FROM post_privacy pp
    WHERE pp.post_id = p.post_id
    AND pp.allowed_user_id = 'user_001'
  )
);

→ Returns list of visible post_ids
→ Used as filter in Elasticsearch query
→ Ensures privacy
```

---

## The Hybrid Architecture

### Why Both Elasticsearch and PostgreSQL?

```
SEPARATION OF CONCERNS:
════════════════════════════════════════════════════════

Elasticsearch: Search and ranking
────────────────────────────────────────────────
✓ Full-text search (inverted index)
✓ Fuzzy matching
✓ Phrase search
✓ Relevance scoring (TF-IDF)
✓ Faceted search (aggregations)
✓ Fast search across billions of documents

✗ No complex relational queries
✗ No JOINs
✗ No ACID transactions
✗ Eventual consistency (not strong)
✗ Poor for frequently updated counters (likes, comments)


PostgreSQL: Metadata and relationships
────────────────────────────────────────────────
✓ ACID transactions (critical for likes, comments)
✓ JOINs (social graph, privacy rules)
✓ Complex WHERE clauses
✓ Foreign keys (referential integrity)
✓ Aggregations (COUNT, SUM, AVG)
✓ Strong consistency

✗ Poor full-text search
✗ Slow at billion-row scans
✗ Cannot handle fuzzy matching
✗ No relevance ranking


Together they complement each other perfectly
```

---

## Complete Schema Architecture

```
ELASTICSEARCH INDEX:
════════════════════════════════════════════════════════

Index: facebook_posts
────────────────────────────────────────────────

Document structure:
{
  "post_id": "post_123",
  "user_id": "user_456",
  "author_name": "John Smith",  // denormalized for display
  "content": "Just made the best chocolate cake ever! Recipe: ...",
  "created_at": "2024-02-26T10:00:00Z",
  "privacy": "friends",
  "tags": ["recipe", "chocolate", "cake", "dessert"],
  "location": {
    "lat": 37.7749,
    "lon": -122.4194,
    "name": "San Francisco, CA"
  },
  "media_type": "photo",
  "media_urls": ["https://cdn.fb.com/img123.jpg"],
  "likes_count": 42,
  "comments_count": 15,
  "engagement_score": 57  // likes + comments for ranking
}

Mapping (schema):
────────────────────────────────────────────────
{
  "mappings": {
    "properties": {
      "post_id": {"type": "keyword"},  // exact match only
      "user_id": {"type": "keyword"},
      "author_name": {"type": "text"},  // analyzed (searchable)
      "content": {
        "type": "text",
        "analyzer": "english",  // stemming, stopwords
        "fields": {
          "exact": {"type": "keyword"}  // for exact phrase
        }
      },
      "created_at": {"type": "date"},
      "privacy": {"type": "keyword"},
      "tags": {"type": "keyword"},  // exact match for filtering
      "location": {"type": "geo_point"},  // for geo queries
      "engagement_score": {"type": "integer"}
    }
  }
}


Sharding strategy:
────────────────────────────────────────────────
Shard by time ranges (for data lifecycle):

Shard 0: Posts from 2024-01-01 to 2024-01-31
Shard 1: Posts from 2024-02-01 to 2024-02-28
Shard 2: Posts from 2024-03-01 to 2024-03-31
...

Benefits:
✓ Recent posts (hot data) on fast SSD nodes
✓ Old posts (cold data) on cheaper storage
✓ Easy to archive (drop entire shard)
✓ Query optimization (only search relevant shards)


POSTGRESQL SCHEMA:
════════════════════════════════════════════════════════

Posts table (sharded by user_id):
────────────────────────────────────────────────────────────────────────────────
post_id  │ user_id  │ created_at          │ updated_at │ privacy  │ likes │ comments │ shares
───────────────────────────────────────────────────────────────────────────────────────────────
post_123 │ user_456 │ 2024-02-26 10:00:00 │ ...        │ friends  │ 42    │ 15       │ 8

Indexes:
  PRIMARY KEY (post_id)
  INDEX (user_id, created_at DESC)
  INDEX (created_at) WHERE privacy = 'public'


Post_Privacy table:
────────────────────────────────────────────────
post_id  │ allowed_user_id │ excluded_user_id
────────────────────────────────────────────────────────
post_125 │ user_789        │ NULL
post_126 │ NULL            │ user_999  (friends except)


Post_Content table (optional - for change tracking):
────────────────────────────────────────────────────────────────────────
post_id  │ version │ content                          │ edited_at
────────────────────────────────────────────────────────────────────────────────
post_123 │ 1       │ "Just made chocolate cake!"      │ 2024-02-26 10:00:00
post_123 │ 2       │ "Just made the best choco..."    │ 2024-02-26 10:05:00

For edit history


User_Friends table:
────────────────────────────────────────────────
user_id  │ friend_id │ status     │ created_at
────────────────────────────────────────────────────────
user_456 │ user_789  │ confirmed  │ 2023-01-15
```

---

## Complete Search Flow

```
FLOW 1: User Searches for "chocolate cake recipe"
════════════════════════════════════════════════════════

User: Types "chocolate cake recipe" in search box
        │
        ▼
STEP 1: Get user's social graph (PostgreSQL)
────────────────────────────────────────────────
SELECT friend_id 
FROM user_friends 
WHERE user_id = 'user_001' 
AND status = 'confirmed';

Returns: [user_789, user_012, user_345, ... ] (500 friends)

Time: <5ms (indexed query)


STEP 2: Build privacy filter
────────────────────────────────────────────────
Construct Elasticsearch filter:
- Privacy = "public" (anyone can see)
- OR (Privacy = "friends" AND user_id IN [friend_list])
- OR (user_id = "user_001") (own posts)

This filter ensures user only sees posts they're allowed to see


STEP 3: Query Elasticsearch
────────────────────────────────────────────────
POST /facebook_posts/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "multi_match": {
            "query": "chocolate cake recipe",
            "fields": ["content^2", "tags"],  // content 2x weight
            "type": "best_fields",
            "fuzziness": "AUTO"  // handle typos
          }
        }
      ],
      "filter": [
        {
          "bool": {
            "should": [
              {"term": {"privacy": "public"}},
              {
                "bool": {
                  "must": [
                    {"term": {"privacy": "friends"}},
                    {"terms": {"user_id": ["user_789", "user_012", ...]}}
                  ]
                }
              },
              {"term": {"user_id": "user_001"}}
            ]
          }
        },
        {"range": {"created_at": {"gte": "now-2y"}}}  // last 2 years
      ]
    }
  },
  "sort": [
    {
      "_score": {"order": "desc"},  // relevance first
      "created_at": {"order": "desc"}  // then recency
    }
  ],
  "from": 0,
  "size": 20,  // pagination
  "highlight": {
    "fields": {
      "content": {}  // highlight matching terms
    }
  }
}


STEP 4: Elasticsearch processes query
────────────────────────────────────────────────
Elasticsearch (across 5 shards, parallel):

Shard 0 (2024-01 posts):
  - Lookup "chocolate" → [post_1, post_5, post_9, ...]
  - Lookup "cake" → [post_1, post_7, post_9, ...]
  - Lookup "recipe" → [post_1, post_3, post_9, ...]
  - Intersect: [post_1, post_9]
  - Apply privacy filter
  - Calculate scores
  - Return top 20

Shard 1 (2024-02 posts):
  - Same process
  - Return top 20

... (Shards 2, 3, 4)

Coordinator node:
  - Receives 20 results from each shard (100 total)
  - Merge and re-rank
  - Return final top 20

Query time: ~150ms


STEP 5: Fetch full metadata from PostgreSQL
────────────────────────────────────────────────
Elasticsearch returns:
[
  {post_id: "post_123", score: 8.5, ...},
  {post_id: "post_456", score: 7.2, ...},
  ...
]

Fetch complete data:
SELECT p.post_id, p.user_id, u.name, u.avatar_url,
       p.created_at, p.likes_count, p.comments_count
FROM posts p
JOIN users u ON p.user_id = u.user_id
WHERE p.post_id IN ('post_123', 'post_456', ...)
ORDER BY 
  CASE p.post_id
    WHEN 'post_123' THEN 1
    WHEN 'post_456' THEN 2
    ...
  END;

Maintains Elasticsearch ranking order

Time: <20ms (indexed lookup)


STEP 6: Return results to user
────────────────────────────────────────────────
Response:
{
  "results": [
    {
      "post_id": "post_123",
      "author": "Chef John",
      "avatar": "https://...",
      "content": "Just made the best <em>chocolate cake</em> ever! Here's my <em>recipe</em>: ...",
      "created_at": "2 hours ago",
      "likes": 42,
      "comments": 15,
      "media": ["https://cdn.fb.com/img123.jpg"]
    },
    ...
  ],
  "total": 15420,
  "took": 185  // milliseconds
}

Total latency: 5 + 150 + 20 + 10 = 185ms
Well under 1 second ✓
```

```
FLOW 2: New Post Created (Real-Time Indexing)
════════════════════════════════════════════════════════

User creates post: "Amazing chocolate cake I baked today!"
        │
        ▼
STEP 1: Write to PostgreSQL (source of truth)
────────────────────────────────────────────────
BEGIN TRANSACTION;

INSERT INTO posts (post_id, user_id, created_at, privacy, likes_count, comments_count)
VALUES ('post_new', 'user_456', NOW(), 'friends', 0, 0);

INSERT INTO post_content (post_id, version, content)
VALUES ('post_new', 1, 'Amazing chocolate cake I baked today!');

COMMIT;

Time: <10ms


STEP 2: Publish to message queue (Kafka)
────────────────────────────────────────────────
Producer.send(
  topic="post_indexing",
  key=post_id,
  value={
    "action": "INDEX",
    "post_id": "post_new",
    "user_id": "user_456",
    "content": "Amazing chocolate cake I baked today!",
    "created_at": "2024-02-26T10:00:00Z",
    "privacy": "friends",
    "tags": ["baking", "dessert"]
  }
)

Kafka buffers event
Asynchronous (doesn't block user)


STEP 3: Indexing consumer processes event
────────────────────────────────────────────────
Consumer reads from Kafka
        │
        ▼
POST /facebook_posts/_doc/post_new
{
  "post_id": "post_new",
  "user_id": "user_456",
  "content": "Amazing chocolate cake I baked today!",
  "created_at": "2024-02-26T10:00:00Z",
  "privacy": "friends",
  "tags": ["baking", "dessert"],
  "engagement_score": 0
}

Elasticsearch:
1. Analyzes content: ["amaz", "chocol", "cake", "bake", "today"]
2. Updates inverted index:
   - "chocol" → [..., post_new]
   - "cake" → [..., post_new]
   - "bake" → [..., post_new]
3. Stores document

Time: <50ms


STEP 4: Post now searchable
────────────────────────────────────────────────
Total time from creation to searchable: <500ms
User searches "chocolate cake" → sees new post


Refresh interval:
────────────────────────────────────────────────
Elasticsearch refreshes index every 1 second (default)
New posts visible in search within 1 second
Configurable (can be near real-time at <100ms with tradeoffs)
```

```
FLOW 3: User Likes Post (Update Engagement Score)
════════════════════════════════════════════════════════

User likes post_123
        │
        ▼
STEP 1: Update PostgreSQL (ACID transaction)
────────────────────────────────────────────────
BEGIN TRANSACTION;

UPDATE posts
SET likes_count = likes_count + 1
WHERE post_id = 'post_123';

INSERT INTO post_likes (post_id, user_id, liked_at)
VALUES ('post_123', 'user_001', NOW());

COMMIT;

Atomic increment
Time: <5ms


STEP 2: Update Elasticsearch (async)
────────────────────────────────────────────────
POST /facebook_posts/_update/post_123
{
  "script": {
    "source": "ctx._source.likes_count += 1; ctx._source.engagement_score = ctx._source.likes_count + ctx._source.comments_count",
    "lang": "painless"
  }
}

Atomic update script
Recalculates engagement_score
Affects future search ranking

Time: <10ms


Why async update to Elasticsearch:
────────────────────────────────────────────────
✓ User sees like immediately (PostgreSQL is source)
✓ Engagement score update doesn't block user
✓ Eventually consistent (acceptable for ranking)
✓ Kafka can batch updates (efficiency)


Batching strategy:
────────────────────────────────────────────────
Instead of updating Elasticsearch on every like:
- Collect likes for 10 seconds
- post_123: +15 likes
- post_456: +8 likes
- Bulk update:
  POST /_bulk
  {"update": {"_id": "post_123"}}
  {"script": {"source": "ctx._source.likes_count += 15; ..."}}
  {"update": {"_id": "post_456"}}
  {"script": {"source": "ctx._source.likes_count += 8; ..."}}

Reduces Elasticsearch load
Acceptable staleness for ranking
```

```
FLOW 4: User Edits Post (Reindex)
════════════════════════════════════════════════════════

User edits post_123 content:
"Amazing chocolate cake" → "Amazing chocolate cake with vanilla frosting"
        │
        ▼
STEP 1: Update PostgreSQL
────────────────────────────────────────────────
BEGIN TRANSACTION;

UPDATE posts
SET updated_at = NOW()
WHERE post_id = 'post_123';

INSERT INTO post_content (post_id, version, content, edited_at)
VALUES ('post_123', 2, 'Amazing chocolate cake with vanilla frosting', NOW());

COMMIT;

Edit history preserved


STEP 2: Reindex in Elasticsearch
────────────────────────────────────────────────
POST /facebook_posts/_update/post_123
{
  "doc": {
    "content": "Amazing chocolate cake with vanilla frosting",
    "updated_at": "2024-02-26T10:05:00Z"
  }
}

Elasticsearch:
1. Removes old inverted index entries
2. Analyzes new content: ["amaz", "chocol", "cake", "vanilla", "frost"]
3. Updates inverted index:
   - "vanilla" → [..., post_123]  (new term)
   - "frost" → [..., post_123]  (new term)
4. Updates document

Now searchable by "vanilla frosting"
```

---

## Advanced Features

### Custom Ranking Function

```
COMBINING RELEVANCE + RECENCY + ENGAGEMENT:
════════════════════════════════════════════════════════

Problem: 
- Relevant old post (3 years old, perfect match)
- Less relevant recent post (1 day old, partial match)

Which should rank higher?

Solution: Function score query
────────────────────────────────────────────────
{
  "query": {
    "function_score": {
      "query": {
        "multi_match": {
          "query": "chocolate cake",
          "fields": ["content", "tags"]
        }
      },
      "functions": [
        {
          "field_value_factor": {
            "field": "engagement_score",
            "factor": 0.1,
            "modifier": "log1p"  // log(1 + engagement_score)
          }
        },
        {
          "gauss": {
            "created_at": {
              "origin": "now",
              "scale": "7d",  // posts from last week get boost
              "decay": 0.5  // older posts decay exponentially
            }
          }
        }
      ],
      "score_mode": "multiply",
      "boost_mode": "multiply"
    }
  }
}

Final score = text_relevance × engagement_boost × recency_decay

Example:
Post A: Relevance 10, engagement 100, age 1 day
  → 10 × log(101) × 1.0 = 46.4

Post B: Relevance 15, engagement 10, age 30 days
  → 15 × log(11) × 0.2 = 7.2

Post A ranks higher (recent + popular)
```

### Autocomplete Suggestions

```
SEARCH-AS-YOU-TYPE:
════════════════════════════════════════════════════════

User types: "choc..."

Elasticsearch query:
────────────────────────────────────────────────
{
  "suggest": {
    "text": "choc",
    "completion": {
      "field": "content.suggest",
      "size": 5,
      "fuzzy": {
        "fuzziness": 1
      }
    }
  }
}

Returns:
[
  "chocolate",
  "chocolate cake",
  "chocolate recipe",
  "chocolate brownies",
  "chocolate chip cookies"
]

Powered by FST (Finite State Transducer)
Time: <10ms


Mapping for autocomplete:
────────────────────────────────────────────────
{
  "content": {
    "type": "text",
    "fields": {
      "suggest": {
        "type": "completion"
      }
    }
  }
}

Stores prefix tree
Optimized for prefix matching
```

### Privacy-Aware Search Optimization

```
FRIEND GRAPH CACHE:
════════════════════════════════════════════════════════

Problem:
Query PostgreSQL for friend list on every search
500 friends × 1B users = slow

Solution: Cache friend graph in Redis
────────────────────────────────────────────────
Key:   friends:user_001
Type:  Set
Value: {user_789, user_012, user_345, ...}
TTL:   1 hour

SET friends:user_001 {member_list}

On search:
friends = SMEMBERS friends:user_001  (<1ms)
Use in Elasticsearch filter

Update on friend add/remove:
SADD friends:user_001 user_999
SREM friends:user_001 user_888


Alternative: Elasticsearch-only approach
────────────────────────────────────────────────
Store privacy rules in Elasticsearch document:
{
  "post_id": "post_123",
  "visibility": {
    "type": "friends",
    "allowed_users": ["user_789", "user_012", ...]
  }
}

Filter:
{"terms": {"visibility.allowed_users": "user_001"}}

Pros:
✓ Single database (simpler)
✓ No cross-database join

Cons:
✗ Denormalization (friend list in every post)
✗ Large documents (500 friends × 1M posts)
✗ Update complexity (friend add → update all posts)

PostgreSQL + Redis cache is better approach
```

---

## Data Lifecycle Management

### Archiving Old Posts

```
TIME-BASED SHARDING:
════════════════════════════════════════════════════════

Elasticsearch indices by month:
────────────────────────────────────────────────
facebook_posts_2024_02  (current, hot data)
facebook_posts_2024_01  (recent, warm data)
facebook_posts_2023_12  (older, cold data)
...
facebook_posts_2020_01  (very old, frozen)


Index Lifecycle Management (ILM):
────────────────────────────────────────────────
Hot phase (0-30 days):
- High-performance SSD nodes
- Full indexing + search
- Replicas: 2 (high availability)

Warm phase (30-365 days):
- Standard SSD nodes
- Read-only (no updates)
- Replicas: 1

Cold phase (1-3 years):
- Cheap HDD storage
- Searchable snapshot
- Replicas: 0 (restore from snapshot if needed)

Frozen phase (3+ years):
- Archive to S3
- Remove from Elasticsearch cluster
- Restore on-demand for historical searches

Delete phase (5+ years):
- Permanent deletion
- Compliance with data retention policies


Query optimization:
────────────────────────────────────────────────
User searches "chocolate cake"

Default: Search last 2 years only
- facebook_posts_2024_*
- facebook_posts_2023_*

If user selects "All time":
- Search all indices (slower, but acceptable)
- Older indices return results from cold storage
```

---

## System Diagram

```
                    ┌─────────────────┐
                    │  User Browser   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  Load Balancer  │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │   API Gateway   │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
  │ PostgreSQL   │   │Elasticsearch │   │    Redis     │
  │              │   │              │   │              │
  │ - Posts      │   │ - Inverted   │   │ - Friend     │
  │ - Privacy    │   │   index      │   │   graph      │
  │ - Friends    │   │ - Search     │   │   cache      │
  │ - Counters   │   │   results    │   │              │
  └──────────────┘   └──────────────┘   └──────────────┘
         │                   ▲
         │                   │
         └───────┬───────────┘
                 │
         ┌───────▼────────┐
         │     Kafka      │
         │  (indexing     │
         │   pipeline)    │
         └────────────────┘
```

---

## Tradeoffs vs Other Databases

```
┌───────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                           │ THIS ARCH    │ POSTGRES ALL │ SOLR         │ ALGOLIA      │
├───────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Full-text search speed    │ Elastic ✓    │ Slow ✗       │ Elastic ✓    │ Fast ✓       │
│ Fuzzy matching            │ Native ✓     │ Limited      │ Good ✓       │ Excellent ✓  │
│ Privacy queries (JOINs)   │ PostgreSQL✓  │ PostgreSQL✓  │ Manual ✗     │ Manual ✗     │
│ ACID transactions         │ PostgreSQL✓  │ PostgreSQL✓  │ NO ✗         │ NO ✗         │
│ Real-time indexing        │ <1s ✓        │ N/A          │ <1s ✓        │ <100ms ✓     │
│ Custom ranking functions  │ Native ✓     │ Manual       │ Good ✓       │ Limited      │
│ Aggregations (facets)     │ Excellent ✓  │ Slow         │ Good ✓       │ Good ✓       │
│ Geo queries               │ Native ✓     │ PostGIS ✓    │ Good ✓       │ Excellent ✓  │
│ Horizontal scaling        │ Native ✓     │ Sharding     │ Native ✓     │ Managed ✓    │
│ Cost at FB scale          │ HIGH         │ Impossible   │ HIGH         │ VERY HIGH    │
│ Self-hosted               │ Yes ✓        │ Yes ✓        │ Yes ✓        │ NO (SaaS)    │
│ Operational complexity    │ HIGH         │ MEDIUM       │ HIGH         │ LOW (managed)│
└───────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **Elasticsearch stores post content with inverted indices because full-text search queries like "chocolate cake recipe" require O(1) term lookups in hash tables mapping "chocolate"→[post_ids] rather than PostgreSQL's LIKE '%chocolate%' causing full table scans of trillions of posts taking hours, while built-in text analysis with tokenization, stemming, and fuzzy matching handles typos ("reciepe"→"recipe" with Levenshtein edit distance 2) and phrase searches ("New York City" matching consecutive position-indexed terms) that SQL cannot express — PostgreSQL stores post metadata and social graph because privacy queries like "can user_001 see this post WHERE privacy='friends' AND EXISTS friend relationship" require JOINs across user_friends table and conditional logic checking custom privacy rules that Elasticsearch's document model cannot express, while ACID transactions ensure likes_count increments don't have race conditions and referential integrity prevents posts referencing deleted users — time-based sharding creates monthly Elasticsearch indices (facebook_posts_2024_02) enabling Index Lifecycle Management where hot recent posts live on fast SSD nodes with 2 replicas, warm 30-365 day posts move to cheaper storage with 1 replica, cold 1-3 year posts become read-only searchable snapshots on HDD, and frozen 3+ year posts archive to S3 and restore on-demand reducing storage costs by 90% — custom function_score queries combine TF-IDF text relevance × log(engagement_score) × exponential_time_decay(created_at) ranking recent popular posts higher than old obscure posts even with perfect keyword matches, implementing the "best results first" experience users expect — real-time indexing through Kafka pipelines where new posts write to PostgreSQL immediately returning to user, asynchronously publish to Kafka topic, and consumer batch-indexes to Elasticsearch within 1 second making posts searchable without blocking the creation API, while like/comment updates batch every 10 seconds updating engagement_score used in ranking formulas — privacy filtering pre-fetches user's 500-friend graph from PostgreSQL (or Redis cache <1ms) and injects as Elasticsearch boolean filter requiring (privacy='public' OR (privacy='friends' AND user_id IN friend_list) OR user_id=self) evaluated during search before relevance scoring, ensuring users never see posts they shouldn't access regardless of keyword match quality.**