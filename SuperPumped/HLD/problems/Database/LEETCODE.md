
## The Core Problem LeetCode Solves

On the surface a coding platform seems simple — store problems, run code, show rankings. But consider the real constraints:

```
LeetCode scale reality:
────────────────────────────────────────────────
Active users:              20 million+
Daily submissions:         500,000+ (6 submissions/second sustained)
Peak submissions:          2,000 submissions/second (during contests)
Total problems:            3,000+
Test cases per problem:    50-200 (some have 1,000+)
Code execution time:       0.1s - 10s per test case
Leaderboard updates:       Real-time (sub-second)

Requirements:
→ Store problem descriptions, constraints, examples
→ Execute untrusted user code safely (sandbox isolation)
→ Run code against 100+ test cases per submission
→ Store billions of historical submissions for analytics
→ Update user rankings instantly after submission
→ Show global leaderboard (<100ms query)
→ Support multiple languages (Python, Java, C++, Go, Rust, etc.)
→ Handle contest spikes (10,000 users submitting simultaneously)
→ Prevent cheating (plagiarism detection needs historical code)
→ Query: "Show me all my Python submissions for problem #42"
```

This combination of **relational problem data + massive time-series submission writes + code execution isolation + real-time ranking updates + historical code search** is what forces this multi-database architecture.

---

## Why PostgreSQL for Problems?

### The Problem Data Structure

```
WHAT IS A "PROBLEM"?
════════════════════════════════════════════════════════

Problem #42: "Two Sum"
{
  problem_id: 42,
  title: "Two Sum",
  difficulty: "Easy",
  description: "Given an array of integers...",
  constraints: "2 <= nums.length <= 10^4",
  examples: [
    {input: "[2,7,11,15], target=9", output: "[0,1]"},
    {input: "[3,2,4], target=6", output: "[1,2]"}
  ],
  test_cases: [
    {input: "[2,7,11,15], 9", expected: "[0,1]"},
    {input: "[3,2,4], 6", expected: "[1,2]"},
    ... (100+ test cases)
  ],
  hints: ["Use a hash map", "One-pass solution exists"],
  tags: ["Array", "Hash Table"],
  companies: ["Amazon", "Google", "Microsoft"],
  acceptance_rate: 0.48,
  related_problems: [1, 15, 167],
  solution_code: {...},
  editorial: "Approach 1: Brute Force...",
  created_at: "2018-01-15",
  updated_at: "2024-02-20"
}
```

This is highly relational data with complex structure.

### Why PostgreSQL Is Perfect

```
POSTGRESQL SCHEMA:
════════════════════════════════════════════════════════

Problems table:
────────────────────────────────────────────────────────────────────────────────
problem_id │ title     │ difficulty │ description        │ constraints │ acceptance_rate
───────────────────────────────────────────────────────────────────────────────────────────
42         │ Two Sum   │ Easy       │ Given an array...  │ 2 <= n...   │ 0.48
15         │ 3Sum      │ Medium     │ Given an integer...│ 3 <= n...   │ 0.31
1          │ Add Two...│ Medium     │ You are given...   │ 1 <= l...   │ 0.39

Additional columns:
  slug: VARCHAR (URL-friendly: "two-sum")
  created_at: TIMESTAMP
  updated_at: TIMESTAMP
  is_premium: BOOLEAN (locked for free users)


Test_Cases table:
────────────────────────────────────────────────────────────────────────
test_case_id │ problem_id │ input                │ expected_output │ is_hidden
────────────────────────────────────────────────────────────────────────────────
1001         │ 42         │ "[2,7,11,15]\n9"     │ "[0,1]"         │ false
1002         │ 42         │ "[3,2,4]\n6"         │ "[1,2]"         │ false
1003         │ 42         │ "[100000,...]\n..." │ "[999,1000]"    │ true

is_hidden = true → not shown to user, only used for judging


Problem_Tags (many-to-many):
────────────────────────────────────────────────
problem_id │ tag_name
──────────────────────────────────────────────
42         │ Array
42         │ Hash Table
15         │ Array
15         │ Two Pointers


Problem_Companies (many-to-many):
────────────────────────────────────────────────
problem_id │ company_name │ frequency
────────────────────────────────────────────────────
42         │ Amazon       │ 150
42         │ Google       │ 89
15         │ Microsoft    │ 120


Related_Problems:
────────────────────────────────────────────────
problem_id │ related_id │ relationship_type
──────────────────────────────────────────────────────
42         │ 167        │ similar
42         │ 1          │ follow_up
```

### Complex Queries PostgreSQL Handles Naturally

```
QUERY 1: Find problems by multiple criteria
════════════════════════════════════════════════════════

"Show me all Medium difficulty problems
 tagged 'Array' and 'Hash Table'
 asked by Amazon
 with acceptance rate > 30%"

SELECT p.problem_id, p.title, p.acceptance_rate
FROM problems p
JOIN problem_tags pt1 ON p.problem_id = pt1.problem_id
JOIN problem_tags pt2 ON p.problem_id = pt2.problem_id
JOIN problem_companies pc ON p.problem_id = pc.problem_id
WHERE p.difficulty = 'Medium'
AND pt1.tag_name = 'Array'
AND pt2.tag_name = 'Hash Table'
AND pc.company_name = 'Amazon'
AND p.acceptance_rate > 0.30
ORDER BY pc.frequency DESC;

→ Multiple JOINs
→ Complex filtering
→ Natural in SQL
→ <50ms with proper indexes


QUERY 2: Recommendation engine
────────────────────────────────────────────────

"User just solved problem #42, what should they try next?"

SELECT rp.related_id, p.title, p.difficulty
FROM related_problems rp
JOIN problems p ON rp.related_id = p.problem_id
WHERE rp.problem_id = 42
AND rp.relationship_type = 'follow_up'
AND p.difficulty IN (
  SELECT difficulty FROM problems WHERE problem_id = 42
  UNION
  SELECT CASE 
    WHEN difficulty = 'Easy' THEN 'Medium'
    WHEN difficulty = 'Medium' THEN 'Hard'
    ELSE 'Hard'
  END
)
ORDER BY p.acceptance_rate DESC
LIMIT 5;

→ Subqueries, CASE statements
→ Complex logic
→ Impossible in NoSQL without application code
```

### Why Not MongoDB for Problems?

```
MONGODB ATTEMPT:
════════════════════════════════════════════════════════

Store problem as document:
{
  _id: 42,
  title: "Two Sum",
  difficulty: "Easy",
  test_cases: [
    {input: "...", expected: "..."},
    ... (100+ test cases embedded)
  ],
  tags: ["Array", "Hash Table"],
  companies: [{name: "Amazon", frequency: 150}, ...]
}

Pros:
✓ Flexible schema
✓ Single document fetch

Cons:
✗ Cannot query efficiently: "problems with tags X AND Y"
   → Must fetch all, filter in app
✗ Cannot JOIN with submissions, user stats
✗ Document size explosion (100+ test cases per problem)
   → 16MB document limit risk
✗ No enforced relationships (foreign keys)
✗ Updating acceptance rate requires full document rewrite


When MongoDB works:
────────────────────────────────────────────────
→ Simple key-value lookups
→ No complex queries needed
→ Schema changes frequently

LeetCode needs relational queries
PostgreSQL is the right choice
```

---

## Why Cassandra for Submissions?

### The Submission Write Pattern

```
SUBMISSION CHARACTERISTICS:
════════════════════════════════════════════════════════

Write pattern:
- 6 submissions/second sustained
- 2,000 submissions/second during contests
- Pure append (never update old submissions)
- Time-series data (timestamp critical)

Read pattern:
- "Show user's submission history" (by user_id, time)
- "Show all submissions for problem #42" (by problem_id, time)
- Rarely query old submissions (>1 month)
- Recent submissions queried frequently

Data volume:
- 500,000 submissions/day × 365 = 182M submissions/year
- Each submission: ~10KB (code, result, metadata)
- Total: 1.8TB/year

This is a WRITE-HEAVY time-series workload
Perfect for Cassandra
```

### Cassandra Schema Design

```
CASSANDRA SCHEMA:
════════════════════════════════════════════════════════

Submissions_By_User table:
────────────────────────────────────────────────────────────────────────────────
user_id │ submission_time        │ submission_id │ problem_id │ language │ result │ runtime │ code
───────────────────────────────────────────────────────────────────────────────────────────────────────────────
user_001│ 2024-02-26 10:00:00   │ sub_abc       │ 42         │ python   │ AC     │ 52ms    │ "def..."
user_001│ 2024-02-26 09:55:00   │ sub_xyz       │ 42         │ python   │ WA     │ N/A     │ "def..."
user_001│ 2024-02-25 15:30:00   │ sub_def       │ 15         │ java     │ AC     │ 38ms    │ "class..."

PRIMARY KEY ((user_id), submission_time, submission_id)
CLUSTERING ORDER BY (submission_time DESC, submission_id DESC)

Why this schema:
────────────────────────────────────────────────
Partition key: user_id
→ All user's submissions in same partition
→ Efficient query: "show user's history"

Clustering key: submission_time (DESC)
→ Submissions sorted by time within partition
→ Most recent first
→ Efficient query: "show last 20 submissions"


Submissions_By_Problem table:
────────────────────────────────────────────────────────────────────────────────
problem_id │ submission_time      │ submission_id │ user_id  │ language │ result │ runtime
───────────────────────────────────────────────────────────────────────────────────────────────
42         │ 2024-02-26 10:00:00 │ sub_abc       │ user_001 │ python   │ AC     │ 52ms
42         │ 2024-02-26 09:58:00 │ sub_ghi       │ user_002 │ java     │ TLE    │ N/A
42         │ 2024-02-26 09:55:00 │ sub_xyz       │ user_001 │ python   │ WA     │ N/A

PRIMARY KEY ((problem_id), submission_time, submission_id)

Why duplicate data (denormalization):
────────────────────────────────────────────────
Cassandra has no JOINs
Must query by partition key
Different queries need different partition keys
→ Duplicate data in multiple tables
→ Each table optimized for its query pattern

Storage is cheap
Write throughput is cheap (Cassandra strength)
Duplication is acceptable tradeoff
```

### Why Cassandra Handles This Better Than PostgreSQL

```
POSTGRESQL PROBLEMS:
════════════════════════════════════════════════════════

Submissions table (single table):
────────────────────────────────────────────────
submission_id │ user_id  │ problem_id │ submitted_at        │ result │ code
───────────────────────────────────────────────────────────────────────────────
sub_abc       │ user_001 │ 42         │ 2024-02-26 10:00:00 │ AC     │ "def..."
sub_xyz       │ user_001 │ 42         │ 2024-02-26 09:55:00 │ WA     │ "def..."
...
(182 million rows after 1 year)


Query: "Show user's last 20 submissions"
SELECT * FROM submissions
WHERE user_id = 'user_001'
ORDER BY submitted_at DESC
LIMIT 20;

Problems:
────────────────────────────────────────────────
→ B-tree index on (user_id, submitted_at) required
→ Index size grows linearly with data
→ After 182M rows: index is 50GB+
→ Query requires index scan + sort
→ Latency: 50-200ms (acceptable but not ideal)

At 2,000 submissions/second during contest:
→ 2,000 INSERTs/second
→ Each INSERT updates B-tree index
→ Index maintenance becomes bottleneck
→ Vacuum lag (MVCC overhead)
→ Write latency spikes to 500ms+
→ Users see "Submission delayed" errors


CASSANDRA SOLUTION:
════════════════════════════════════════════════════════

Writes:
────────────────────────────────────────────────
Write to Cassandra:
1. Append to commit log (sequential disk write)
2. Write to MemTable (in-memory)
3. Acknowledge immediately

Write latency: <5ms (even at 2K writes/sec)
No index maintenance
No locks
No vacuum


Reads:
────────────────────────────────────────────────
SELECT * FROM submissions_by_user
WHERE user_id = 'user_001'
ORDER BY submission_time DESC
LIMIT 20;

Cassandra internals:
1. Hash user_id to find partition (Node 3)
2. Go directly to Node 3
3. Read from SSTable (data already sorted by submission_time)
4. Return top 20

Read latency: <10ms
No index needed (data naturally sorted by clustering key)
Scales horizontally (add more nodes)
```

---

## Why Redis Sorted Sets for Leaderboard?

### The Real-Time Ranking Problem

```
REQUIREMENTS:
════════════════════════════════════════════════════════

Global leaderboard:
- Rank users by total problems solved
- Update instantly on submission acceptance
- Show top 100 users (<50ms)
- Show user's rank: "You are #12,345 out of 20M users"

Contest leaderboard:
- Rank users by problems solved + time penalty
- Update in real-time (thousands watching)
- Must be correct (no stale data)
- Sub-second updates during contest


Naive SQL approach:
────────────────────────────────────────────────
Users table:
user_id │ problems_solved │ total_runtime
─────────────────────────────────────────────────
user_001│ 450             │ 123456
user_002│ 892             │ 98765
...
(20 million rows)

Query: "Top 100 users"
SELECT user_id, problems_solved, total_runtime
FROM users
ORDER BY problems_solved DESC, total_runtime ASC
LIMIT 100;

→ Full table scan or full index scan
→ Sort 20M rows
→ Query time: 5-10 seconds
→ Unacceptable


Query: "What is user_001's rank?"
SELECT COUNT(*) + 1 as rank
FROM users
WHERE problems_solved > (
  SELECT problems_solved FROM users WHERE user_id = 'user_001'
)
OR (problems_solved = (
  SELECT problems_solved FROM users WHERE user_id = 'user_001'
)
AND total_runtime < (
  SELECT total_runtime FROM users WHERE user_id = 'user_001'
));

→ Subqueries
→ Scan millions of rows
→ Query time: 3-5 seconds
→ Impossible at scale
```

### Redis Sorted Set Solution

```
REDIS SORTED SET FOR LEADERBOARD:
════════════════════════════════════════════════════════

Global leaderboard:
────────────────────────────────────────────────
Key:   leaderboard:global
Type:  Sorted Set
Score: problems_solved × 10^9 - total_runtime
Member: user_id

ZADD leaderboard:global 450000123456 "user_001"
     ^^^^ score = 450 × 10^9 - 123456

Why this scoring formula:
- Primary: problems_solved (higher is better)
- Tiebreaker: total_runtime (lower is better)
- Combined into single score for sorting


Update on submission acceptance:
────────────────────────────────────────────────
User solves new problem in 52ms

ZINCRBY leaderboard:global 999999948 "user_001"
                          ^^^^ 10^9 - 52

New score: 451000123404
(451 problems, total runtime 123456 + 52 = 123508)

Atomic operation
Automatically re-sorts
Time: <1ms


Query: "Top 100 users"
────────────────────────────────────────────────
ZREVRANGE leaderboard:global 0 99 WITHSCORES

Returns:
[
  ("user_top1", 892000098765),
  ("user_top2", 891000234567),
  ...
  ("user_top100", 450000999999)
]

Query time: <5ms
O(log N + 100) = O(log 20M + 100) ≈ 24 operations


Query: "What is user_001's rank?"
────────────────────────────────────────────────
ZREVRANK leaderboard:global "user_001"

Returns: 12344 (zero-indexed, so rank is 12,345)

Query time: <2ms
O(log N) = O(log 20M) ≈ 24 skip list hops
```

### Contest Leaderboard (Separate Sorted Set)

```
CONTEST LEADERBOARD:
════════════════════════════════════════════════════════

Key:   contest:123:leaderboard
Score: (problems_solved × 10^12) + time_penalty

time_penalty = sum of (submission_time - contest_start + 20min × wrong_attempts)

User solves 3 problems:
Problem 1: Solved at 10 min, 0 wrong attempts
  → penalty = 10

Problem 2: Solved at 25 min, 2 wrong attempts
  → penalty = 25 + (20 × 2) = 65

Problem 3: Solved at 45 min, 1 wrong attempt
  → penalty = 45 + (20 × 1) = 65

Total penalty: 10 + 65 + 65 = 140 minutes

Score: (3 × 10^12) + 140 = 3000000000140

ZADD contest:123:leaderboard 3000000000140 "user_001"


Real-time updates:
────────────────────────────────────────────────
10,000 users watching contest leaderboard
Poll every 5 seconds: "Show me current standings"

ZREVRANGE contest:123:leaderboard 0 49 WITHSCORES

50 queries/second (10K users / 200)
Redis handles 100K+ queries/second easily
<5ms latency per query
No database bottleneck
```

---

## Code Execution Isolation

### The Untrusted Code Problem

````
SECURITY THREAT:
════════════════════════════════════════════════════════

User submits malicious code:

Python submission:
```python
import os
os.system("rm -rf /")  # Delete server files
```

C++ submission:
```cpp
while(true) { fork(); }  // Fork bomb
```

Java submission:
```java
new File("/etc/passwd").delete();  // Delete system files
```

Without isolation:
→ User code runs on judge server
→ Can access file system
→ Can consume all CPU/memory
→ Can attack other users' code
→ Server compromised
````

### Containerized Execution Solution

```
DOCKER CONTAINER SANDBOX:
════════════════════════════════════════════════════════

Execution flow:
────────────────────────────────────────────────

User submits code
        │
        ▼
Submission Service:
1. Save code to Cassandra (async)
2. Publish to Kafka topic: "code_execution"
3. Return submission_id to user
4. User sees: "Submission queued..."
        │
        ▼
Execution Worker (consumer):
5. Consume from Kafka
6. Create isolated Docker container:
   docker run --rm \
     --cpus=1 \          # CPU limit
     --memory=256m \     # Memory limit
     --network=none \    # No network access
     --read-only \       # Read-only filesystem
     --user=nobody \     # Non-root user
     --timeout=10s \     # 10 second max
     judge-python:latest

7. Mount code and test cases (read-only)
8. Execute: python solution.py < input.txt
9. Capture stdout, stderr, exit code
10. Kill container after 10 seconds
11. Compare output with expected
12. Publish result to Kafka: "code_results"
        │
        ▼
Result Service:
9. Update submission in Cassandra (result: AC/WA/TLE)
10. Update leaderboard in Redis (if AC)
11. Push notification to user via WebSocket


Security guarantees:
────────────────────────────────────────────────
✓ Isolated filesystem (cannot access server files)
✓ No network (cannot attack other systems)
✓ Resource limits (cannot consume all CPU/memory)
✓ Timeout (cannot run forever)
✓ Ephemeral (container destroyed after execution)
✓ Read-only code mount (cannot modify test cases)
```

---

## Test Case Storage (Blob Storage)

### The Large Dataset Problem

```
PROBLEM WITH DATABASE STORAGE:
════════════════════════════════════════════════════════

Some problems have massive test cases:

Problem #1000: "Process Large Dataset"
Test case #150:
- Input: 100MB file (1 million integers)
- Expected output: 50MB file

Storing in PostgreSQL/Cassandra:
────────────────────────────────────────────────
→ 100MB BLOB in database row
→ Fetched into memory on every submission
→ Database bloat (1000 problems × 100 cases × 10MB avg = 1TB)
→ Slow queries (transferring 100MB over network)
→ Expensive (database storage costs 10x vs object storage)


Large test cases per problem:
────────────────────────────────────────────────
Graph problems: 10,000 node graph → 5MB adjacency list
String problems: 1 million character string → 1MB
Array problems: 100,000 element array → 500KB

Total test case storage needed: 10TB+
```

### S3/Blob Storage Solution

```
BLOB STORAGE ARCHITECTURE:
════════════════════════════════════════════════════════

PostgreSQL (metadata only):
────────────────────────────────────────────────
test_case_id │ problem_id │ s3_key                        │ size_bytes │ is_hidden
───────────────────────────────────────────────────────────────────────────────────────
1001         │ 42         │ "test-cases/42/input_1.txt"   │ 150        │ false
1002         │ 42         │ "test-cases/42/input_2.txt"   │ 180        │ false
1003         │ 1000       │ "test-cases/1000/large_1.txt" │ 104857600  │ true


S3 bucket structure:
────────────────────────────────────────────────
s3://leetcode-test-cases/
  test-cases/
    42/
      input_1.txt     (150 bytes)
      expected_1.txt  (20 bytes)
      input_2.txt     (180 bytes)
      expected_2.txt  (25 bytes)
    1000/
      large_1.txt     (100 MB)
      large_expected_1.txt (50 MB)


Execution worker flow:
────────────────────────────────────────────────
1. Receive submission: problem_id = 1000

2. Query PostgreSQL for test case metadata:
   SELECT s3_key FROM test_cases WHERE problem_id = 1000

3. Download test cases from S3 (parallel):
   aws s3 cp s3://leetcode-test-cases/test-cases/1000/large_1.txt /tmp/
   
4. Mount test case in Docker container:
   docker run -v /tmp/large_1.txt:/input.txt:ro ...

5. Execute code with test case

6. Clean up (delete local test case copy)


Benefits:
────────────────────────────────────────────────
✓ Cheap storage ($0.023/GB/month vs $0.10+ for database)
✓ No database bloat
✓ Parallel downloads (faster than database fetch)
✓ CDN integration (CloudFront caches frequently used test cases)
✓ Versioning (S3 versioning for test case updates)
✓ Durability (S3 11-nines durability vs database backups)
```

---

## Complete Schema Architecture

```
POSTGRESQL SCHEMA:
════════════════════════════════════════════════════════

Problems:
────────────────────────────────────────────────────────────────────────────────
problem_id │ slug      │ title     │ difficulty │ acceptance_rate │ is_premium │ created_at
───────────────────────────────────────────────────────────────────────────────────────────────
42         │ two-sum   │ Two Sum   │ Easy       │ 0.48            │ false      │ 2018-01-15
15         │ 3sum      │ 3Sum      │ Medium     │ 0.31            │ false      │ 2018-02-01

Indexes:
  PRIMARY KEY (problem_id)
  UNIQUE (slug)
  INDEX (difficulty)
  INDEX (acceptance_rate)


Test_Cases:
────────────────────────────────────────────────────────────────────────
test_case_id │ problem_id │ s3_input_key   │ s3_expected_key │ is_hidden
────────────────────────────────────────────────────────────────────────────────
1001         │ 42         │ "tc/42/in1"    │ "tc/42/exp1"    │ false
1002         │ 42         │ "tc/42/in2"    │ "tc/42/exp2"    │ true

Indexes:
  PRIMARY KEY (test_case_id)
  INDEX (problem_id)


User_Problems (track solved problems):
────────────────────────────────────────────────────────────────────────
user_id  │ problem_id │ status      │ language │ best_runtime │ last_attempted
────────────────────────────────────────────────────────────────────────────────────
user_001 │ 42         │ solved      │ python   │ 52ms         │ 2024-02-26
user_001 │ 15         │ attempted   │ java     │ NULL         │ 2024-02-25

Indexes:
  PRIMARY KEY (user_id, problem_id)
  INDEX (user_id, status)


CASSANDRA SCHEMA:
════════════════════════════════════════════════════════

Submissions_By_User:
────────────────────────────────────────────────────────────────────────────────
user_id │ submission_time      │ submission_id │ problem_id │ language │ result │ runtime │ memory │ code
───────────────────────────────────────────────────────────────────────────────────────────────────────────────
user_001│ 2024-02-26 10:00:00 │ sub_abc       │ 42         │ python   │ AC     │ 52ms    │ 14MB   │ "def..."
user_001│ 2024-02-26 09:55:00 │ sub_xyz       │ 42         │ python   │ WA     │ N/A     │ N/A    │ "def..."

PRIMARY KEY ((user_id), submission_time, submission_id)
CLUSTERING ORDER BY (submission_time DESC)
TTL: 90 days (old submissions auto-deleted)


Submissions_By_Problem:
────────────────────────────────────────────────────────────────────────────────
problem_id │ submission_time    │ submission_id │ user_id  │ result │ runtime
───────────────────────────────────────────────────────────────────────────────────────
42         │ 2024-02-26 10:00:00│ sub_abc       │ user_001 │ AC     │ 52ms
42         │ 2024-02-26 09:58:00│ sub_ghi       │ user_002 │ TLE    │ N/A

PRIMARY KEY ((problem_id), submission_time, submission_id)

Purpose: Analytics on problem difficulty (acceptance rates)


REDIS SCHEMA:
════════════════════════════════════════════════════════

Global leaderboard:
────────────────────────────────────────────────
Key:   leaderboard:global
Type:  Sorted Set
Score: (problems_solved × 10^9) - total_runtime_ms

ZADD leaderboard:global 450000123456 "user_001"


Contest leaderboard:
────────────────────────────────────────────────
Key:   contest:123:leaderboard
Score: (problems_solved × 10^12) + time_penalty_minutes

ZADD contest:123:leaderboard 3000000000140 "user_001"


User stats cache:
────────────────────────────────────────────────
Key:   user:user_001:stats
Type:  Hash

HSET user:user_001:stats
  problems_solved "450"
  easy_solved "150"
  medium_solved "200"
  hard_solved "100"
  acceptance_rate "0.65"
  ranking "12345"

TTL: 1 hour (refresh periodically)


Submission result cache (temporary):
────────────────────────────────────────────────
Key:   submission:sub_abc:result
Value: "PENDING" | "AC" | "WA" | "TLE" | "RE"
TTL:   60 seconds

WebSocket polling uses this
Avoids hammering Cassandra for result
```

---

## Complete Database Flow

```
FLOW 1: User Submits Code
════════════════════════════════════════════════════════

User writes solution and clicks "Submit"
        │
        ▼
POST /api/submit
{
  problem_id: 42,
  language: "python",
  code: "def twoSum(nums, target):\n  ..."
}
        │
        ▼
STEP 1: Validate and create submission
────────────────────────────────────────────────
Generate submission_id: sub_abc123
Current time: 2024-02-26 10:00:00

Write to Cassandra (async):
INSERT INTO submissions_by_user
(user_id, submission_time, submission_id, problem_id, language, code, result)
VALUES ('user_001', '2024-02-26 10:00:00', 'sub_abc123', 42, 'python', '...', 'PENDING');

Time: <5ms (write to commit log + MemTable)


STEP 2: Mark result as pending in Redis
────────────────────────────────────────────────
SET submission:sub_abc123:result "PENDING" EX 60


STEP 3: Publish to Kafka
────────────────────────────────────────────────
Producer.send(
  topic="code_execution",
  key=submission_id,
  value={
    submission_id: "sub_abc123",
    user_id: "user_001",
    problem_id: 42,
    language: "python",
    code: "def twoSum...",
    timestamp: 1708945200
  }
)

Kafka buffers durably
Returns immediately


STEP 4: Return to user
────────────────────────────────────────────────
Response:
{
  submission_id: "sub_abc123",
  status: "PENDING",
  message: "Your submission is being judged..."
}

User sees: "Running..."

Total API latency: <20ms
Code execution happens asynchronously
```

```
FLOW 2: Code Execution (Async)
════════════════════════════════════════════════════════

Execution worker (Kafka consumer) picks up submission
        │
        ▼
STEP 1: Fetch test cases
────────────────────────────────────────────────
Query PostgreSQL:
SELECT s3_input_key, s3_expected_key, is_hidden
FROM test_cases
WHERE problem_id = 42
ORDER BY test_case_id;

Returns: 150 test cases

Download from S3 (parallel, 10 at a time):
aws s3 cp s3://bucket/tc/42/in1 /tmp/tc1_in
aws s3 cp s3://bucket/tc/42/exp1 /tmp/tc1_exp
...

Time: ~500ms for all test cases


STEP 2: Create Docker container
────────────────────────────────────────────────
docker create --name judge_sub_abc123 \
  --cpus=1 \
  --memory=256m \
  --network=none \
  --read-only \
  --user=nobody \
  judge-python:3.11


STEP 3: Mount code and test cases
────────────────────────────────────────────────
docker cp /tmp/solution.py judge_sub_abc123:/solution.py
docker cp /tmp/tc1_in judge_sub_abc123:/input.txt


STEP 4: Execute code
────────────────────────────────────────────────
start_time = now()

docker start -a judge_sub_abc123 \
  timeout 10s python /solution.py < /input.txt > /output.txt

end_time = now()
runtime = end_time - start_time

Capture:
- stdout → actual_output
- stderr → error_messages
- exit_code


STEP 5: Compare output
────────────────────────────────────────────────
expected = read("/tmp/tc1_exp")
actual = read_from_container("/output.txt")

if actual == expected:
  result = "AC" (Accepted)
elif timeout:
  result = "TLE" (Time Limit Exceeded)
elif runtime_error:
  result = "RE" (Runtime Error)
else:
  result = "WA" (Wrong Answer)


STEP 6: Repeat for all test cases
────────────────────────────────────────────────
For test cases 2-150:
  - Destroy old container
  - Create new container
  - Run code with next test case
  - Check result

Overall result:
- If ALL test cases AC → Final result: AC
- If ANY test case fails → Final result: First failure (WA/TLE/RE)

Total execution time: 5-30 seconds (depends on # test cases)


STEP 7: Publish result
────────────────────────────────────────────────
Producer.send(
  topic="code_results",
  key=submission_id,
  value={
    submission_id: "sub_abc123",
    user_id: "user_001",
    problem_id: 42,
    result: "AC",
    runtime: "52ms",
    memory: "14MB",
    test_cases_passed: 150,
    timestamp: now()
  }
)
```

```
FLOW 3: Result Processing
════════════════════════════════════════════════════════

Result consumer receives AC result
        │
        ▼
STEP 1: Update Cassandra
────────────────────────────────────────────────
UPDATE submissions_by_user
SET result = 'AC',
    runtime = '52ms',
    memory = '14MB'
WHERE user_id = 'user_001'
AND submission_time = '2024-02-26 10:00:00'
AND submission_id = 'sub_abc123';


STEP 2: Update PostgreSQL (user stats)
────────────────────────────────────────────────
```

Continue

21:40

```
STEP 2: Update PostgreSQL (user stats)
────────────────────────────────────────────────
Check if first AC for this problem:
SELECT status FROM user_problems
WHERE user_id = 'user_001' AND problem_id = 42;

If not exists or status = 'attempted':
  -- First solve!
  INSERT INTO user_problems (user_id, problem_id, status, language, best_runtime, last_attempted)
  VALUES ('user_001', 42, 'solved', 'python', '52ms', NOW())
  ON CONFLICT (user_id, problem_id) 
  DO UPDATE SET 
    status = 'solved',
    best_runtime = LEAST(EXCLUDED.best_runtime, user_problems.best_runtime),
    last_attempted = NOW();
  
  -- Increment user's solved count
  UPDATE users
  SET problems_solved = problems_solved + 1,
      easy_solved = easy_solved + 1  -- if problem.difficulty = 'Easy'
  WHERE user_id = 'user_001';


STEP 3: Update Redis leaderboard
────────────────────────────────────────────────
If first solve (new problem solved):
  
  ZINCRBY leaderboard:global 999999948 "user_001"
         ^^^^ score increment = 10^9 - runtime_ms
         
  New score reflects:
  - One more problem solved (10^9 added)
  - Total runtime increased by 52ms (52 subtracted)
  
  Leaderboard automatically re-sorts
  User's rank updates instantly


STEP 4: Update Redis cache
────────────────────────────────────────────────
SET submission:sub_abc123:result "AC" EX 60

HSET user:user_001:stats
  problems_solved "451"  ← incremented
  easy_solved "151"
  acceptance_rate "0.66"  ← recalculated

EXPIRE user:user_001:stats 3600


STEP 5: Notify user via WebSocket
────────────────────────────────────────────────
WebSocket broadcast to user_001:
{
  type: "SUBMISSION_RESULT",
  submission_id: "sub_abc123",
  result: "AC",
  runtime: "52ms",
  memory: "14MB",
  message: "Accepted! 🎉",
  new_rank: 12344,
  rank_change: +1
}

User sees green checkmark instantly
Confetti animation plays
Rank updated in sidebar


STEP 6: Award achievements (if applicable)
────────────────────────────────────────────────
Check milestones:
IF problems_solved = 100:
  INSERT INTO user_achievements (user_id, achievement_id, earned_at)
  VALUES ('user_001', 'century_club', NOW());
  
  Notify: "Achievement Unlocked: Century Club! 🏆"

IF first solve in new category:
  Award: "Array Master Badge"

Total result processing: <100ms
```

```
FLOW 4: User Views Leaderboard
════════════════════════════════════════════════════════

User opens "Leaderboard" page
        │
        ▼
GET /api/leaderboard?page=1
        │
        ▼
STEP 1: Fetch top 100 from Redis
────────────────────────────────────────────────
ZREVRANGE leaderboard:global 0 99 WITHSCORES

Returns:
[
  ("user_top1", 892000098765),
  ("user_top2", 891000234567),
  ("user_top3", 890000456789),
  ...
  ("user_top100", 450000999888)
]

Query time: <5ms


STEP 2: Decode scores
────────────────────────────────────────────────
For each user:
  score = 892000098765
  problems_solved = score / 10^9 = 892
  total_runtime = 10^9 - (score % 10^9) = 98765ms


STEP 3: Fetch user details (batch)
────────────────────────────────────────────────
user_ids = ["user_top1", "user_top2", ..., "user_top100"]

Query PostgreSQL:
SELECT user_id, username, avatar_url, country
FROM users
WHERE user_id IN (...100 user_ids...);

Time: <20ms (indexed lookup)


STEP 4: Return combined data
────────────────────────────────────────────────
Response:
{
  leaderboard: [
    {
      rank: 1,
      user_id: "user_top1",
      username: "CodeMaster",
      avatar: "https://...",
      country: "US",
      problems_solved: 892,
      total_runtime: "98.8s"
    },
    ...
  ],
  total_users: 20000000,
  last_updated: "2024-02-26T10:05:00Z"
}

Total latency: <30ms


For user's own rank:
────────────────────────────────────────────────
ZREVRANK leaderboard:global "user_001"
→ Returns: 12344

ZSCORE leaderboard:global "user_001"
→ Returns: 451000123404

User sees: "Your rank: #12,345 / 20,000,000"
```

```
FLOW 5: Contest Real-Time Updates
════════════════════════════════════════════════════════

Weekly contest starts: 10,000 participants
        │
        ▼
Contest initialization:
────────────────────────────────────────────────
Create contest leaderboard:
Key: contest:123:leaderboard

For each registered user:
ZADD contest:123:leaderboard 0 "user_001"
ZADD contest:123:leaderboard 0 "user_002"
...

All start with score 0


User solves problem during contest:
────────────────────────────────────────────────
User_001 solves Problem A at t=15min (1 wrong attempt)
        │
        ▼
STEP 1: Calculate penalty
────────────────────────────────────────────────
time_penalty = submission_time - contest_start + (20 × wrong_attempts)
             = 15 + (20 × 1)
             = 35 minutes

problems_solved = 1
score = (1 × 10^12) + 35 = 1000000000035


STEP 2: Update contest leaderboard
────────────────────────────────────────────────
ZADD contest:123:leaderboard 1000000000035 "user_001"

Atomic operation
Instant rank update


STEP 3: Broadcast to viewers
────────────────────────────────────────────────
Pub/Sub channel: contest:123:updates

PUBLISH contest:123:updates '{
  type: "SOLVE",
  user: "user_001",
  problem: "A",
  time: 15,
  new_rank: 245
}'

All 10,000 participants subscribed to channel
See update instantly: "user_001 solved Problem A!"


STEP 4: Leaderboard polling
────────────────────────────────────────────────
Users' browsers poll every 5 seconds:
GET /api/contest/123/leaderboard?top=50

ZREVRANGE contest:123:leaderboard 0 49 WITHSCORES

Returns top 50 in <5ms
10,000 users × 0.2 req/sec = 2,000 req/sec
Redis handles easily


Final standings:
────────────────────────────────────────────────
After 90 minutes, contest ends

Freeze leaderboard:
SET contest:123:frozen "true"

No more score updates accepted

Generate final results:
ZREVRANGE contest:123:leaderboard 0 9999 WITHSCORES

Top 3 users awarded:
- Virtual prize money
- Badge: "Contest Winner 🥇"
- Profile highlight

Leaderboard archived to PostgreSQL for history
```

```
FLOW 6: Plagiarism Detection (Background Job)
════════════════════════════════════════════════════════

Background service runs hourly:
        │
        ▼
STEP 1: Fetch recent AC submissions
────────────────────────────────────────────────
Query Cassandra:
SELECT submission_id, user_id, problem_id, code
FROM submissions_by_problem
WHERE problem_id = 42
AND submission_time > NOW() - 1 HOUR
AND result = 'AC';

Returns: 5,000 submissions for problem #42


STEP 2: Normalize code
────────────────────────────────────────────────
For each submission:
  - Remove comments
  - Remove whitespace
  - Normalize variable names (a → var1, b → var2)
  - Generate AST (Abstract Syntax Tree)
  - Hash AST structure


STEP 3: Compare submissions
────────────────────────────────────────────────
Use MinHash LSH (Locality Sensitive Hashing):

For each code:
  shingles = generate_shingles(code, k=5)
  minhash = MinHash(shingles)
  
Insert into LSH index:
  lsh.insert(submission_id, minhash)

Query similar submissions:
  candidates = lsh.query(minhash, threshold=0.9)

If similarity > 90%:
  Flag for manual review


STEP 4: Store in PostgreSQL
────────────────────────────────────────────────
INSERT INTO plagiarism_cases
(submission_id_1, submission_id_2, similarity_score, status)
VALUES ('sub_abc', 'sub_xyz', 0.95, 'pending_review');

Human moderators review flagged cases
```

```
FLOW 7: Analytics Query (Business Intelligence)
════════════════════════════════════════════════════════

Product team: "What's our daily active users trend?"
        │
        ▼
Query Cassandra with Spark:
────────────────────────────────────────────────
SELECT 
  toDate(submission_time) as date,
  COUNT(DISTINCT user_id) as dau
FROM submissions_by_problem
WHERE submission_time >= '2024-01-01'
AND submission_time < '2024-03-01'
GROUP BY toDate(submission_time)
ORDER BY date;

Spark distributes query across Cassandra cluster
Processes 50M submissions
Returns daily aggregates

Time: 2-5 minutes (acceptable for analytics)


Advanced analytics:
────────────────────────────────────────────────
"Which problems have highest WA rate?"

SELECT 
  problem_id,
  COUNT(*) as total_submissions,
  SUM(CASE WHEN result = 'WA' THEN 1 ELSE 0 END) as wrong_answers,
  (wrong_answers::float / total_submissions) as wa_rate
FROM submissions_by_problem
WHERE submission_time >= NOW() - 30 DAYS
GROUP BY problem_id
HAVING total_submissions > 1000
ORDER BY wa_rate DESC
LIMIT 20;

Identifies problems that are confusing
Product team can improve problem statements

Time: 5-10 minutes


"Language popularity by problem difficulty"
────────────────────────────────────────────────
WITH submissions AS (
  SELECT s.problem_id, s.language, p.difficulty
  FROM submissions_by_problem s
  JOIN problems p ON s.problem_id = p.problem_id
  WHERE s.submission_time >= NOW() - 30 DAYS
)
SELECT 
  difficulty,
  language,
  COUNT(*) as count
FROM submissions
GROUP BY difficulty, language
ORDER BY difficulty, count DESC;

Results:
Easy problems: Python most popular (60%)
Hard problems: C++ most popular (45%)

Informs language support priorities
```

---

## Tradeoffs vs Other Databases

```
┌───────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                           │ THIS ARCH    │ POSTGRES ALL │ MONGO ALL    │ MYSQL ALL    │
├───────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Problem metadata queries  │ PostgreSQL✓  │ PostgreSQL✓  │ MongoDB ✓    │ MySQL ✓      │
│ Submission write throughput│ Cassandra✓  │ 2K/sec max✗  │ 50K/sec      │ 2K/sec max✗  │
│ Historical submission query│ Cassandra✓  │ Slow (>1yr)  │ Slow         │ Slow (>1yr)  │
│ Leaderboard updates       │ Redis ✓      │ Seconds ✗    │ Seconds ✗    │ Seconds ✗    │
│ Real-time rank query      │ Redis ✓      │ Impossible✗  │ Impossible✗  │ Impossible✗  │
│ Complex problem queries   │ PostgreSQL✓  │ PostgreSQL✓  │ Limited      │ MySQL ✓      │
│ Time-series partitioning  │ Cassandra✓   │ Manual       │ Manual       │ Manual       │
│ Horizontal scaling        │ Native ✓     │ Sharding     │ Native ✓     │ Sharding     │
│ Operational complexity    │ HIGH         │ LOW ✓        │ MEDIUM       │ LOW ✓        │
│ Cost at LeetCode scale    │ MEDIUM       │ HIGH         │ HIGH         │ HIGH         │
└───────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## Alternative Architectures (Why NOT Used)

```
ALTERNATIVE 1: All PostgreSQL
════════════════════════════════════════════════════════

Single PostgreSQL database:
- Problems (works fine)
- Submissions (problems at scale)
- Leaderboard (materialized view)

Problems:
────────────────────────────────────────────────
✗ 2,000 submissions/sec during contest overwhelms single DB
✗ 182M submissions/year makes queries slow
✗ Leaderboard materialized view refresh takes minutes
✗ Calculating rank requires COUNT(*) over millions
✗ Vacuum lag from constant inserts
✗ Single point of failure

Works for: Small coding platforms (<10K users)


ALTERNATIVE 2: All MongoDB
════════════════════════════════════════════════════════

MongoDB for everything:
- Problems as documents (works)
- Submissions as documents (works)
- Leaderboard aggregation pipeline (slow)

Problems:
────────────────────────────────────────────────
✗ Cannot efficiently query: "problems with tags X AND Y"
✗ Aggregation pipeline for leaderboard takes seconds
✗ No efficient way to get user's rank
✗ Eventual consistency during contests
✗ Sharding by problem_id scatters user's submissions

Works for: When queries are simple key-value lookups


ALTERNATIVE 3: All Cassandra
════════════════════════════════════════════════════════

Cassandra for everything:
- Problems (awkward - no JOINs)
- Submissions (perfect)
- Leaderboard (impossible)

Problems:
────────────────────────────────────────────────
✗ Cannot do complex problem queries (tags, companies, related)
✗ No JOINs (must denormalize heavily)
✗ Leaderboard requires scanning all users (impossible)
✗ No sorted queries across partitions

Works for: Pure time-series append workloads only
```

---

## Why This Hybrid Is Optimal

```
ARCHITECTURAL PRINCIPLES:
════════════════════════════════════════════════════════

1. Match Database to Access Pattern
────────────────────────────────────────────────
Problems: Relational, complex queries → PostgreSQL
Submissions: Time-series, append-heavy → Cassandra
Leaderboard: Sorted rankings, real-time → Redis


2. Separate Read and Write Paths
────────────────────────────────────────────────
Write path:
  User submits → Cassandra (async)
  
Read path:
  User views history → Cassandra (sorted by time)
  User views leaderboard → Redis (pre-computed)


3. Pre-compute Expensive Operations
────────────────────────────────────────────────
Don't calculate rank on every request:
  ✗ SELECT COUNT(*) FROM users WHERE score > my_score
  
Pre-compute in Redis Sorted Set:
  ✓ ZREVRANK leaderboard:global user_id


4. Use Right Tool for Each Job
────────────────────────────────────────────────
Test cases: Large files → S3 (not database)
Code execution: Untrusted code → Docker (isolated)
Real-time updates: WebSocket + Pub/Sub → Redis
Analytics: Large aggregations → Spark + Cassandra
```

---

## System Diagram

```
                        ┌─────────────────────┐
                        │   User Browser      │
                        │   (React App)       │
                        └──────────┬──────────┘
                                   │
                ┌──────────────────┼──────────────────┐
                │                  │                  │
                ▼                  ▼                  ▼
        ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
        │ Problem API  │  │ Submit API   │  │ Leaderboard  │
        └──────┬───────┘  └──────┬───────┘  │     API      │
               │                 │           └──────┬───────┘
               │                 │                  │
               ▼                 ▼                  ▼
        ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
        │ PostgreSQL  │   │   Kafka     │   │    Redis    │
        │             │   │  (queue)    │   │  (sorted    │
        │ - Problems  │   └──────┬──────┘   │   sets)     │
        │ - Test cases│          │          └─────────────┘
        │ - User stats│          │
        └─────────────┘          │
                                 ▼
                        ┌─────────────────┐
                        │ Execution Worker│
                        │   (Kafka →)     │
                        └────────┬────────┘
                                 │
                    ┌────────────┼────────────┐
                    ▼            ▼            ▼
              ┌──────────┐ ┌──────────┐ ┌──────────┐
              │ Docker 1 │ │ Docker 2 │ │ Docker N │
              │ (Python) │ │ (Java)   │ │ (C++)    │
              └──────────┘ └──────────┘ └──────────┘
                    │            │            │
                    └────────────┼────────────┘
                                 ▼
                        ┌─────────────────┐
                        │     Kafka       │
                        │  (results →)    │
                        └────────┬────────┘
                                 │
                    ┌────────────┼────────────┐
                    ▼            ▼            ▼
              ┌──────────┐ ┌──────────┐ ┌──────────┐
              │Cassandra │ │PostgreSQL│ │  Redis   │
              │(update   │ │(update   │ │(update   │
              │submissn) │ │ stats)   │ │rank)     │
              └──────────┘ └──────────┘ └──────────┘
```

---

## One Line Summary

> **PostgreSQL stores problems and user stats because complex queries like "show Medium difficulty problems tagged 'Array' AND 'Hash Table' asked by Amazon with acceptance > 30%" require JOINs across problem_tags and problem_companies tables that NoSQL databases cannot express efficiently, while test case metadata lives in PostgreSQL but actual large test files (100MB inputs) are stored in S3 to avoid database bloat and leverage cheaper object storage at $0.023/GB/month versus $0.10+ for database storage — Cassandra stores submissions partitioned by user_id with clustering on submission_time because the append-heavy workload (2,000 submissions/second during contests) needs sequential writes to commit log that complete in <5ms versus PostgreSQL's B-tree index maintenance causing 500ms write latency spikes, and querying "show user's last 20 submissions" retrieves pre-sorted data from a single partition in 10ms versus PostgreSQL scanning 182 million rows even with indexes — Redis Sorted Sets maintain the global leaderboard because ZREVRANK returns user's rank among 20 million users in <2ms through O(log N) skip list traversal versus PostgreSQL's "SELECT COUNT(*) WHERE score > my_score" taking 5+ seconds to scan millions of rows, and ZINCRBY atomically updates rankings in <1ms when users solve problems versus recalculating the entire leaderboard materialized view taking minutes — code execution happens in isolated Docker containers with CPU/memory limits, read-only filesystems, and network disabled to prevent malicious code from accessing server files or consuming all resources, with untrusted submissions queued in Kafka and processed asynchronously by worker pools that mount test cases from S3, execute code with 10-second timeouts, compare outputs, and publish results back through Kafka to update Cassandra (submission history), PostgreSQL (user stats), and Redis (leaderboard) in parallel without blocking the submission API which returns immediately after queuing — this architecture handles 500K daily submissions across 3,000 problems with sub-50ms leaderboard queries, sub-second submission acknowledgments, and horizontal scaling by adding Cassandra nodes for write capacity, Redis replicas for read capacity, and Docker workers for execution parallelism.**