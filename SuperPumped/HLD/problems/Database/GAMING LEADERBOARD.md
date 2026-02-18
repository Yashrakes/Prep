## The Core Problem a Leaderboard Solves

On the surface a leaderboard seems trivial — store scores, sort them, show ranks. But consider the real constraints:

```
Fortnite has 350 million registered players
Peak concurrent: 8.3 million players simultaneously
Every kill, every match completion → score update
Every score update → rank changes for potentially
                     millions of players above/below

Requirements:
→ Accept millions of score updates per second
→ Return player rank in under 10ms
→ Show top 100 globally in real time
→ Show players around YOUR rank (±5 positions)
→ Support regional leaderboards simultaneously
→ Keep historical records forever
→ Never show stale rank data
```

This combination of **massive concurrent writes + instant rank queries + historical persistence** is what forces this two-database architecture.

---

## Why Redis Sorted Sets for Real-Time Rankings?

### First, What Is a Redis Sorted Set?

A Sorted Set is Redis's most powerful data structure. It maintains a collection of members, each with a floating-point score, **always kept in sorted order automatically**.

```
Redis Sorted Set internals:
────────────────────────────────────────────────
Implemented using TWO data structures together:

1. Skip List (for ordered operations):
   → O(log N) rank queries
   → O(log N) range queries
   → O(log N) insertions

2. Hash Table (for O(1) score lookups):
   → "What is player X's score?" → instant
   → "What is player X's rank?" → O(log N)

Combined: best of both worlds
Fast lookups AND fast ordered operations
```

### Why Not SQL for Real-Time Rankings?

This is the core question. Let's destroy the SQL approach first:

```
NAIVE SQL APPROACH:
────────────────────────────────────────────────
Score update comes in:
UPDATE user_scores
SET score = score + 500
WHERE user_id = 'player_001'

Get player rank:
SELECT COUNT(*) + 1 as rank
FROM user_scores
WHERE score > (
  SELECT score FROM user_scores
  WHERE user_id = 'player_001'
)

Problems at scale:
→ 8M concurrent players updating scores
→ Each UPDATE acquires row lock
→ Each rank query does full table scan
→ 8M concurrent full table scans
→ Database melts completely
→ Rank query takes 30+ seconds
→ Players see wrong ranks
```

```
REDIS SORTED SET APPROACH:
────────────────────────────────────────────────
Score update:
ZINCRBY leaderboard:global 500 "player_001"
→ O(log N) operation
→ No locks
→ Completes in microseconds
→ Rank automatically maintained

Get player rank:
ZREVRANK leaderboard:global "player_001"
→ O(log N) operation
→ Returns rank instantly
→ Always accurate
→ ~100 microseconds total

At 8M concurrent players:
Redis handles 1M+ operations/second easily
Each operation microseconds
No locks, no contention, no degradation
```

---

## The Skip List: Why Redis Rank Queries Are O(log N)

Most engineers use Redis Sorted Sets without understanding why they're fast. Let me show you:

```
Skip List visualization (simplified):
────────────────────────────────────────────────

Level 3: player_A(100) ──────────────────────────→ player_E(900)
Level 2: player_A(100) ──────→ player_C(500) ────→ player_E(900)
Level 1: player_A(100) → player_B(300) → player_C(500) → player_D(700) → player_E(900)

Finding rank of player_D (score 700):
→ Start at Level 3: jump from 100 to 900 (too far)
→ Drop to Level 2: jump from 100 to 500 (ok)
→ Stay Level 2: jump from 500 to 900 (too far)
→ Drop to Level 1: move from 500 to 700 (found!)
→ Count nodes passed = rank

Total steps: 4 instead of 4 sequential
At 1 billion players: ~30 steps instead of 1 billion
This is O(log N) in action
```

---

## Why PostgreSQL for Persistence?

### What PostgreSQL Actually Stores Here

PostgreSQL is NOT the real-time leaderboard. Redis owns that job. PostgreSQL stores things Redis fundamentally cannot:

```
REDIS can store:
────────────────────────────────────────────────
Current score of every player    ✓
Current rank of every player     ✓
Top N players right now          ✓

REDIS cannot store:
────────────────────────────────────────────────
Player's rank last Tuesday       ✗ (no time travel)
Season 1 final rankings          ✗ (data gets cleared)
Score history over 6 months      ✗ (memory too expensive)
Which games contributed to score ✗ (no complex queries)
Player's personal best per game  ✗ (no relational data)
```

PostgreSQL fills every gap Redis has:

```
User_Scores Table:
──────────────────────────────────────────────────────────
user_id  │ game_id  │ score   │ rank  │ last_updated
─────────────────────────────────────────────────────────
player_1 │ fortnite │ 48500   │ 1     │ 2024-02-26 10:00
player_2 │ fortnite │ 47200   │ 2     │ 2024-02-26 09:58
player_1 │ cod      │ 31000   │ 5     │ 2024-02-26 09:45
player_3 │ fortnite │ 46800   │ 3     │ 2024-02-26 09:55
──────────────────────────────────────────────────────────

Historical_Rankings Table:
──────────────────────────────────────────────────────────
snapshot_id │ user_id  │ game_id  │ rank │ score │ snapshot_date
─────────────────────────────────────────────────────────────────
snap_001    │ player_1 │ fortnite │ 1    │ 48500 │ 2024-02-25
snap_001    │ player_2 │ fortnite │ 2    │ 47200 │ 2024-02-25
snap_002    │ player_1 │ fortnite │ 3    │ 45000 │ 2024-02-24
──────────────────────────────────────────────────────────────────
```

### Why Not MongoDB for Persistence?

```
PostgreSQL wins here because:
────────────────────────────────────────────────
Complex queries needed:
"Show me player_1's rank trajectory
 over the last 30 days
 compared to their friends
 broken down by game mode"

PostgreSQL:
SELECT h.snapshot_date,
       h.rank,
       h.score,
       g.game_name
FROM historical_rankings h
JOIN games g ON h.game_id = g.game_id
JOIN friendships f ON h.user_id = f.friend_id
WHERE f.user_id = 'player_1'
AND h.snapshot_date > NOW() - INTERVAL '30 days'
ORDER BY h.snapshot_date

→ Natural SQL with joins
→ Runs efficiently with indexes

MongoDB:
→ Multiple collection lookups
→ No native joins (use $lookup which is slow)
→ Aggregation pipeline becomes complex
→ Not designed for this relational query pattern
```

---

## Sharding Strategy: By Region and Game

### Why Sharding Is Necessary

```
Single global Redis instance:
────────────────────────────────────────────────
All 350M players in one sorted set
ZINCRBY on single instance
Single-threaded Redis processes all commands
Max throughput: ~1M ops/second

At 8M concurrent players each scoring:
8,000,000 updates/second needed
1,000,000 updates/second available
→ 8x overloaded
→ Queue builds up
→ Latency spikes from microseconds to seconds
→ Leaderboard feels broken
```

```
Sharded Redis instances:
────────────────────────────────────────────────
leaderboard:NA:fortnite      → Redis Instance 1
leaderboard:EU:fortnite      → Redis Instance 2
leaderboard:ASIA:fortnite    → Redis Instance 3
leaderboard:NA:cod           → Redis Instance 4
leaderboard:EU:cod           → Redis Instance 5
leaderboard:ASIA:cod         → Redis Instance 6
leaderboard:global:fortnite  → Redis Instance 7 (aggregated)

Each instance handles its region's players:
NA players  → Instance 1 only (2.5M players)
EU players  → Instance 2 only (2M players)
ASIA players→ Instance 3 only (3M players)

Each instance now at 25-35% capacity
Massive headroom for spikes
```

### The Global Leaderboard Problem

```
How do you combine regional shards
into one global leaderboard?
────────────────────────────────────────────────

Option 1: Merge all sorted sets (expensive)
ZUNIONSTORE leaderboard:global 3
  leaderboard:NA:fortnite
  leaderboard:EU:fortnite
  leaderboard:ASIA:fortnite

→ Runs every 60 seconds (not real-time)
→ Creates temporary global view
→ Expensive operation but only periodic
→ Acceptable for global rankings
→ Regional rankings always real-time

Option 2: Hierarchical aggregation
Top 1000 from each region
→ Merged into global top
→ Below top 1000: show regional rank
→ Most players never see true global rank
→ Efficient and practical
```

---

## Complete Schema Architecture

```
REDIS SCHEMA (Real-time layer):
════════════════════════════════════════════════

Key: "leaderboard:NA:fortnite"
Type: Sorted Set
────────────────────────────────────────────────
Member          │ Score
────────────────────────────────────────────────
"player_001"    │ 48500.0
"player_002"    │ 47200.0
"player_003"    │ 46800.0
"player_004"    │ 45100.0
... 2.5M more entries

Key: "leaderboard:global:fortnite"
Type: Sorted Set (merged, updated every 60s)
────────────────────────────────────────────────
"player_001"    │ 48500.0
"player_EU_007" │ 48100.0
"player_AS_023" │ 47900.0

Key: "player:profile:player_001"
Type: Hash (supplementary player data)
────────────────────────────────────────────────
username     → "ProGamer_X"
avatar_url   → "https://cdn.../avatar.jpg"
country      → "US"
last_seen    → "2024-02-26 10:00:00"

Key: "rank:cache:player_001:fortnite"
Type: String (cached rank to avoid recomputation)
────────────────────────────────────────────────
Value: "1"
TTL:  30 seconds (auto-expires, forces refresh)


POSTGRESQL SCHEMA (Persistence layer):
════════════════════════════════════════════════

User_Scores:
──────────────────────────────────────────────────────────
user_id (PK) │ game_id (PK) │ score  │ rank │ last_updated
─────────────────────────────────────────────────────────
player_001   │ fortnite     │ 48500  │ 1    │ 2024-02-26
player_002   │ fortnite     │ 47200  │ 2    │ 2024-02-26

Historical_Rankings:
──────────────────────────────────────────────────────────────────
snapshot_id │ user_id   │ game_id  │ rank │ score │ snapshot_date
──────────────────────────────────────────────────────────────────
snap_001    │ player_001│ fortnite │ 1    │ 48500 │ 2024-02-25
snap_001    │ player_002│ fortnite │ 2    │ 47200 │ 2024-02-25

Games:
──────────────────────────────
game_id  │ name     │ mode
──────────────────────────────
fortnite │ Fortnite │ battle
cod      │ COD      │ shooter
```

---

## Complete Database Flow

```
FLOW 1: Player Scores Points During Match
═══════════════════════════════════════════════════════

Player kills enemy → +500 points
            │
            ▼
Game Server sends score event:
{
  user_id: "player_001",
  game_id: "fortnite",
  region:  "NA",
  points:  500,
  timestamp: 1708901234
}
            │
            ▼
Score Update Service receives event
            │
            ├──────────────────────────────────────────────┐
            │                                              │
            ▼                                              ▼
    REDIS (immediate)                          MESSAGE QUEUE
    ZINCRBY                                    (async persistence)
    leaderboard:NA:fortnite                    {user_001, +500, ts}
    500 "player_001"
            │
            ▼
    Redis updates skip list
    New score: 48000 + 500 = 48500
    Rank automatically maintained
    ~100 microseconds total
            │
            ▼                              Queue consumer runs
    Player rank updated                    every 60 seconds
    in real time                                   │
                                                   ▼
                                          POSTGRESQL (batch)
                                          UPDATE user_scores
                                          SET score = 48500,
                                              last_updated = NOW()
                                          WHERE user_id = 'player_001'
                                          AND game_id = 'fortnite'
```

```
FLOW 2: Player Opens Leaderboard Screen
═══════════════════════════════════════════════════════

Player taps leaderboard button
            │
            ▼
App requests:
GET /leaderboard?game=fortnite&region=NA&user=player_001
            │
            ▼
Leaderboard Service handles request
            │
            ├─────────────────────────────────────────────┐
            │                                             │
            ▼                                             ▼
  FETCH TOP 100 PLAYERS                    FETCH PLAYER'S OWN RANK
  ZREVRANGE                                ZREVRANK
  leaderboard:NA:fortnite                  leaderboard:NA:fortnite
  0 99 WITHSCORES                          "player_001"
            │                                             │
            ▼                                             ▼
  Returns:                                 Returns: 1247
  [player_001: 48500                       (player is rank 1247)
   player_002: 47200                                     │
   player_003: 46800                                     ▼
   ...]                              FETCH PLAYERS AROUND RANK
            │                        ZREVRANGE
            │                        leaderboard:NA:fortnite
            │                        1242 1252 WITHSCORES
            │                        (±5 around player)
            │                                             │
            └──────────────────┬──────────────────────────┘
                               │
                               ▼
                    ENRICH WITH PLAYER PROFILES
                    Redis Hash lookup for each player:
                    HGETALL player:profile:player_001
                    HGETALL player:profile:player_002
                    ...
                    (batch pipeline, single round trip)
                               │
                               ▼
                    RESPONSE TO CLIENT:
                    {
                      top_100: [...players with names/avatars],
                      your_rank: 1247,
                      nearby_players: [rank 1242-1252],
                      your_score: 48500
                    }
                    Total time: ~5-10ms
```

```
FLOW 3: Match Ends - Final Score Submitted
═══════════════════════════════════════════════════════

Match completes
Final score: player_001 earned 5000 points
            │
            ▼
Score Service:
            │
            ├──→ REDIS: ZINCRBY leaderboard:NA:fortnite 5000 "player_001"
            │    Immediate rank update
            │
            ├──→ REDIS: DEL rank:cache:player_001:fortnite
            │    Invalidate cached rank
            │
            └──→ KAFKA: publish score_update event
                          │
                          ▼
                 Persistence Worker consumes:
                          │
                          ├──→ POSTGRESQL UPDATE user_scores
                          │    SET score = new_score,
                          │        rank = new_rank,
                          │        last_updated = NOW()
                          │
                          └──→ Check if snapshot needed
                               (every 24 hours)
                               If yes:
                               INSERT INTO historical_rankings
                               SELECT snapshot_id,
                                      user_id,
                                      rank,
                                      score,
                                      CURRENT_DATE
                               FROM user_scores
                               WHERE game_id = 'fortnite'
```

```
FLOW 4: Player Requests Historical Rankings
═══════════════════════════════════════════════════════

Player views "My Rank History" screen
            │
            ▼
GET /history?user=player_001&game=fortnite&days=30
            │
            ▼
History Service:
            │
            ▼
Check Redis cache first:
GET history:cache:player_001:fortnite:30d
            │
   HIT ─────┘──────────────────────────────→ Return cached
            │
   MISS     │
            ▼
POSTGRESQL query:
SELECT h.snapshot_date,
       h.rank,
       h.score,
       (h.rank - LAG(h.rank)
        OVER (ORDER BY h.snapshot_date)) as rank_change
FROM historical_rankings h
WHERE h.user_id = 'player_001'
AND h.game_id = 'fortnite'
AND h.snapshot_date > NOW() - INTERVAL '30 days'
ORDER BY h.snapshot_date
            │
            ▼
Store in Redis cache:
SET history:cache:player_001:fortnite:30d
    [result]
    EX 3600    ← cache for 1 hour
            │
            ▼
Return to client:
{
  history: [
    {date: "2024-02-25", rank: 1, score: 48500},
    {date: "2024-02-24", rank: 3, score: 45000},
    {date: "2024-02-23", rank: 7, score: 41000}
  ],
  trend: "improving"
}
```

---

## Tradeoffs vs Other Databases

```
┌──────────────────┬─────────────────┬──────────────────┬────────────────┐
│                  │ REDIS SORTED SET│ POSTGRESQL ONLY  │ MONGODB        │
├──────────────────┼─────────────────┼──────────────────┼────────────────┤
│ Rank query       │ O(log N) ~100μs │ O(N) full scan   │ O(N) slow      │
│ Score update     │ O(log N) ~100μs │ O(log N) + lock  │ O(log N)       │
│ Top 100 query    │ O(100) instant  │ needs index      │ needs index     │
│ Historical data  │ NO              │ YES ✓            │ YES            │
│ Complex queries  │ NO              │ YES ✓            │ LIMITED        │
│ Memory cost      │ HIGH            │ LOW              │ MEDIUM         │
│ Persistence      │ LIMITED         │ FULL ✓           │ FULL           │
│ Concurrent writes│ 1M+/sec ✓       │ ~50K/sec         │ ~100K/sec      │
│ Rank accuracy    │ ALWAYS EXACT ✓  │ DELAYED          │ DELAYED        │
└──────────────────┴─────────────────┴──────────────────┴────────────────┘
```

---

## One Line Summary

> **Redis Sorted Sets own the real-time leaderboard because their skip list structure gives O(log N) rank updates and queries at microsecond speed for millions of concurrent players, while PostgreSQL stores what Redis cannot — historical rankings, season snapshots, and complex analytics queries — with sharding by region and game preventing any single Redis instance from becoming a bottleneck, and lazy rank computation meaning PostgreSQL rank columns are eventual-consistency mirrors of Redis truth, never the source of real-time rank data.**