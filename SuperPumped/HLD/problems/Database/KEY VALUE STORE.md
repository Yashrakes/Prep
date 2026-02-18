
## What Problem Does a Key-Value Store Solve?

```
Simplest possible database interface:

PUT("user:001", "{name: Alex, age: 25}")
GET("user:001") → "{name: Alex, age: 25}"
DELETE("user:001")

No SQL. No joins. No schemas.
Just keys mapping to values.

Why would anyone want this?
→ Maximum speed
→ Maximum simplicity
→ Maximum scalability
```

The challenge is building something that is:

```
FAST     → microsecond reads and writes
DURABLE  → survives crashes without losing data
SCALABLE → handles billions of keys across machines
RELIABLE → works even when nodes fail
```

These four requirements pull in **opposite directions**. Making it fast hurts durability. Making it durable hurts speed. This tension is what drives every architectural decision.

---

## The Three Layer Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│          LAYER 1: IN-MEMORY (Redis-like)                    │
│          Hash Table                                         │
│          Purpose: Microsecond reads and writes              │
│          Trade-off: Lost on crash                           │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│          LAYER 2: PERSISTENT (RocksDB/LevelDB)              │
│          LSM Tree                                           │
│          Purpose: Survive crashes, store overflow           │
│          Trade-off: Slower than memory                      │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│          LAYER 3: DISTRIBUTED (Consistent Hashing)          │
│          Partitioning across nodes                          │
│          Purpose: Scale beyond single machine               │
│          Trade-off: Network latency, complexity             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

Each layer solves what the layer above cannot handle alone.

---

## Layer 1: Why In-Memory Hash Table?

### The Hash Table Choice

A hash table gives O(1) lookup — regardless of whether you have 1 key or 1 billion keys, finding a value takes the same time.

```
How it works internally:
────────────────────────────────────────────────
Key: "user:001"
        │
        ▼
Hash function: hash("user:001") = 4829173
        │
        ▼
Array index: 4829173 % array_size = slot 47
        │
        ▼
Value stored at slot 47: "{name: Alex}"
        │
        ▼
Retrieved in ONE memory access
~100 nanoseconds total
```

Why not alternatives?

```
B-TREE (what PostgreSQL uses internally):
→ O(log N) lookup
→ At 1 billion keys: 30 comparisons minimum
→ Fine for disk, wasteful for memory
→ Designed for range queries we don't need

SKIP LIST (what Redis actually uses for sorted sets):
→ O(log N) lookup
→ Better for ordered data
→ Overkill for pure key-value lookup

HASH TABLE wins for pure key-value because:
→ O(1) lookup always
→ Simple implementation
→ Cache-friendly memory layout
→ No ordering overhead we don't need
```

### Why Not Just Keep Everything In Memory?

```
Problem 1: Memory is expensive
────────────────────────────────────────────────
1TB RAM costs ~$5,000
1TB SSD costs ~$100
50x cost difference

At Twitter scale (hundreds of TB of data)
All-memory = $500,000+ in RAM alone
With disk persistence = $10,000

Problem 2: Memory is volatile
────────────────────────────────────────────────
Server crashes → everything gone
Power outage → everything gone
Restart for updates → everything gone

Memory alone is NOT a database
It is a cache
```

This is why you need Layer 2.

---

## Layer 2: Why RocksDB/LevelDB and the LSM Tree?

### What Is an LSM Tree?

LSM stands for **Log Structured Merge Tree**. It is the most important data structure in modern databases that most engineers never learn.

```
The Core Insight of LSM Tree:
────────────────────────────────────────────────
Sequential disk writes are 100x faster
than random disk writes

Hard drive:  Sequential 500MB/s vs Random 1MB/s
SSD:         Sequential 3GB/s   vs Random 100MB/s

LSM Tree converts ALL writes into sequential writes
This is why RocksDB can handle millions of writes/second
```

### How LSM Tree Works Step by Step

```
WRITE PATH:
────────────────────────────────────────────────

Step 1: Write comes in
PUT("user:001", "Alex")

Step 2: Write to WAL (Write Ahead Log) first
Append to WAL file sequentially:
[timestamp] PUT user:001 = Alex
→ Sequential write = fast
→ Crash recovery guaranteed

Step 3: Write to MemTable (in-memory sorted structure)
MemTable: {
  "user:001": "Alex",
  "user:002": "Bob",
  "user:003": "Carol"
}
→ Sorted by key
→ Fast writes

Step 4: MemTable fills up (say 64MB)
→ Flush to disk as SSTable (Sorted String Table)
→ Sequential write to disk
→ Immutable once written

Disk now has:
SSTable L0-1: {user:001→Alex, user:003→Carol}
SSTable L0-2: {user:002→Bob,  user:004→Dave}
```

```
READ PATH:
────────────────────────────────────────────────

GET("user:001")

Step 1: Check MemTable first (in memory, fastest)
Found? Return immediately

Step 2: Check Bloom Filter
"Does user:001 exist in SSTable L0-1?"
Bloom filter: YES (probabilistic, no false negatives)
→ Avoids unnecessary disk reads

Step 3: Check SSTables from newest to oldest
L0-1 → L0-2 → L1 → L2
First match wins (newest value is correct)
```

```
COMPACTION (background process):
────────────────────────────────────────────────

Problem: Multiple SSTables build up over time
Reading requires checking many files
Gets slower over time

Solution: Compaction merges SSTables:

Before:
L0: [user:001→Alex][user:001→NewAlex][user:001→DELETE]

After compaction:
L1: [user:001 → DELETED]

→ Removes duplicates
→ Applies deletes
→ Fewer files to search
→ Reads get faster again
```

### Why Not B-Tree for Persistence?

```
B-TREE writes (what PostgreSQL/MySQL use):
────────────────────────────────────────────────
PUT("user:001", "Alex")
→ Find correct leaf node (random disk seek)
→ Insert into sorted position (random write)
→ Possibly rebalance tree (more random writes)

Random writes on SSD: ~100MB/s

LSM TREE writes:
────────────────────────────────────────────────
PUT("user:001", "Alex")
→ Append to WAL (sequential)
→ Write to MemTable (memory)
→ Flush MemTable to SSTable (sequential)

Sequential writes on SSD: ~3GB/s

LSM Tree is 30x faster for writes
This is why every modern key-value store
(RocksDB, LevelDB, Cassandra, HBase)
uses LSM Trees
```

---

## Layer 3: Why Consistent Hashing for Distribution?

### The Partitioning Problem

```
You have 1 billion keys
One machine cannot hold them all
You need to split across N machines

Naive approach: hash(key) % N
────────────────────────────────────────────────
Key "user:001" → hash = 4829173
4829173 % 4 nodes = Node 1

Works fine until you add a 5th node:
4829173 % 5 nodes = Node 3  ← DIFFERENT NODE!

Adding one node remaps ~80% of all keys
Every GET goes to wrong node
Must move 80% of data
System becomes unavailable during move
Catastrophic at scale
```

### How Consistent Hashing Solves This

```
Visualize a ring from 0 to 2^32:
────────────────────────────────────────────────

            0
           /  \
    Node A      Node D
    (0-90)      (270-360)
         \      /
    Node B    Node C
    (90-180) (180-270)

Each node owns a range of the ring
Keys are placed on ring by hash value
Each key belongs to the next node clockwise

hash("user:001") = 120 → falls between 90-180 → Node B
hash("user:002") = 200 → falls between 180-270 → Node C
hash("user:003") = 50  → falls between 0-90 → Node A
```

```
Adding Node E between B and C:
────────────────────────────────────────────────

Before: Node B owns 90-180
After:  Node B owns 90-135
        Node E owns 135-180

Only keys in range 135-180 move (25% of Node B's keys)
Overall: only ~20% of keys remapped
vs naive hashing: 80% remapped

System stays available
Minimal data movement
```

### Why Virtual Nodes?

```
Problem with basic consistent hashing:
────────────────────────────────────────────────
Node A: owns 0-90    = 90 units = 25% of data
Node B: owns 90-180  = 90 units = 25% of data
Node C: owns 180-270 = 90 units = 25% of data
Node D: owns 270-360 = 90 units = 25% of data

Looks balanced. But in practice:
hash("user:*") keys cluster around 100-150
Node B gets 60% of traffic
Other nodes sit idle
Uneven load distribution
```

```
Solution: Virtual Nodes (VNodes)
────────────────────────────────────────────────
Instead of each physical node owning ONE range
Each physical node owns MANY small ranges

Node A owns: 10-20, 80-90, 150-160, 240-250
Node B owns: 20-30, 90-100, 160-170, 250-260
Node C owns: 30-40, 100-110, 170-180, 260-270
Node D owns: 40-50, 110-120, 180-190, 270-280

Traffic hotspot at 100-150?
Spread across ALL nodes via their vnodes
Much more even distribution
```

---

## The Complete Schema Architecture

```
DATA STRUCTURE LAYER:
────────────────────────────────────────────────

In-Memory Hash Table:
┌─────────────────────────────────────────┐
│ Slot 0:  → "user:003" → "{name:Carol}"  │
│ Slot 1:  → null                         │
│ Slot 2:  → "user:001" → "{name:Alex}"   │
│ ...                                     │
│ Slot 47: → "session:x" → "token_abc"   │
│ ...                                     │
└─────────────────────────────────────────┘
O(1) lookup, O(1) insert

LSM Tree on Disk:
┌─────────────────────────────────────────┐
│ MemTable (in memory, sorted):           │
│   user:001 → Alex                       │
│   user:002 → Bob                        │
│                                         │
│ L0 SSTables (newest):                   │
│   [user:001→Alex2][user:003→Carol]      │
│                                         │
│ L1 SSTables (older, larger):            │
│   [user:001→Alex][user:002→Bob]         │
│                                         │
│ L2 SSTables (oldest, largest):          │
│   [session:x→token][user:004→Dave]      │
└─────────────────────────────────────────┘

Partition Table:
┌─────────────────────────────────────────┐
│ hash("user:001") = 4829173              │
│ 4829173 % ring_position = 120           │
│ Ring position 120 → Node B              │
│ Node B address → 192.168.1.2:6379       │
└─────────────────────────────────────────┘
```

---

## Persistence: AOF + RDB Snapshots

### Why Two Persistence Mechanisms?

```
RDB (Redis Database Snapshot):
────────────────────────────────────────────────
What: Complete memory dump to disk
When: Every 5 minutes (configurable)
How:  Fork process, write entire dataset

ADVANTAGES:
→ Compact single file
→ Fast restart (load one file)
→ Good for backups

DISADVANTAGES:
→ Can lose up to 5 minutes of data
→ Fork is expensive on large datasets
→ Not suitable alone for critical data


AOF (Append Only File):
────────────────────────────────────────────────
What: Log every write operation
When: Every write (or every second)
How:  Append command to log file

ADVANTAGES:
→ Maximum durability (lose at most 1 second)
→ Human readable log
→ Can replay to any point in time

DISADVANTAGES:
→ Large file size over time
→ Slower restart (replay all operations)
→ AOF rewrite needed periodically


COMBINED APPROACH:
────────────────────────────────────────────────
RDB: Safety net every 5 minutes
AOF: Granular recovery between snapshots

Crash recovery:
1. Load latest RDB snapshot (fast bulk load)
2. Replay AOF entries since snapshot (small delta)
3. System restored with minimal data loss
```

---

## Replication: Master-Slave and Quorum

### Master-Slave Replication

```
WRITE flow:
────────────────────────────────────────────────
Client → PUT("user:001", "Alex")
              │
              ▼
         MASTER NODE
         Writes locally
              │
              ├──→ Slave Node 1 (async replication)
              ├──→ Slave Node 2 (async replication)
              └──→ Slave Node 3 (async replication)
              │
         Returns OK to client
         (doesn't wait for slaves)

READ flow:
────────────────────────────────────────────────
Client → GET("user:001")
              │
              ▼
    Any Slave Node (load balanced)
    Returns value
    Master not involved
    Reads scale horizontally
```

### Quorum Writes for Strong Consistency

```
Problem with pure master-slave:
────────────────────────────────────────────────
Master writes, returns OK
Master crashes before replicating
Data lost forever
Slave promoted to master has stale data

Solution: Quorum (used by DynamoDB, Cassandra):
────────────────────────────────────────────────
N = total replicas (say 3)
W = write quorum (say 2)
R = read quorum (say 2)

Rule: W + R > N guarantees consistency

WRITE:
Client → PUT("user:001", "Alex")
Must write to W=2 nodes before returning OK
If only 1 node confirms → return error
Client knows write is safe on 2 nodes

READ:
Client → GET("user:001")
Must read from R=2 nodes
Compare versions
Return most recent value
```

```
Quorum configurations:
────────────────────────────────────────────────
N=3, W=3, R=1  → Strong consistency, slow writes
N=3, W=1, R=3  → Strong consistency, slow reads
N=3, W=2, R=2  → Balanced (most common)
N=3, W=1, R=1  → Weak consistency, maximum speed
```

---

## Complete Flow: All Components Connected

```
CLIENT REQUEST: PUT("user:001", "Alex")
═══════════════════════════════════════════════════════

STEP 1: CLIENT FINDS CORRECT NODE
────────────────────────────────────────────────
Client hashes key:
hash("user:001") = 4829173
Ring position = 120
→ Belongs to Node B
Client connects to Node B directly


STEP 2: NODE B RECEIVES WRITE
────────────────────────────────────────────────
Node B (Master for this key range):

a) Writes to WAL immediately:
   [timestamp] PUT user:001 = Alex
   → Sequential disk write
   → Crash safety guaranteed

b) Writes to MemTable:
   MemTable["user:001"] = "Alex"
   → In-memory write
   → Sub-millisecond

c) Updates Hash Table:
   hash_table[slot_47] = {key:"user:001", val:"Alex"}
   → O(1) in-memory update


STEP 3: REPLICATION TO SLAVE NODES
────────────────────────────────────────────────
Node B replicates to Node C and Node D:

Node B → Node C: "PUT user:001 = Alex"
Node B → Node D: "PUT user:001 = Alex"

Quorum W=2:
Node C confirms → quorum reached (1+1=2)
Node B returns OK to client
Node D replication continues async


STEP 4: CLIENT RECEIVES CONFIRMATION
────────────────────────────────────────────────
Client: write confirmed
Total time: ~1-2ms
Data safe on 2 nodes


STEP 5: BACKGROUND PERSISTENCE
────────────────────────────────────────────────
Every 1 second:
AOF writer appends to append-only file:
"PUT user:001 Alex 1708901234"

Every 5 minutes:
RDB snapshot fork:
→ Child process dumps entire MemTable to disk
→ Creates new snapshot file
→ Old snapshot deleted after new one complete


STEP 6: MEMTABLE FLUSH (when full)
────────────────────────────────────────────────
MemTable reaches 64MB threshold:
→ Frozen (becomes Immutable MemTable)
→ New MemTable created for incoming writes
→ Immutable MemTable flushed to L0 SSTable
→ Sequential write to disk
→ Bloom filter created for SSTable
→ Immutable MemTable discarded from memory


STEP 7: COMPACTION (background)
────────────────────────────────────────────────
L0 has 4 SSTables:
Background thread merges them:
→ Reads all L0 SSTables
→ Merges and deduplicates
→ Writes single sorted L1 SSTable
→ Deletes old L0 SSTables
→ Reads get faster
```

```
CLIENT REQUEST: GET("user:001")
═══════════════════════════════════════════════════════

STEP 1: FIND CORRECT NODE
────────────────────────────────────────────────
hash("user:001") = 4829173 → Node B
Connect to Node B (or its slave)


STEP 2: CHECK MEMORY FIRST
────────────────────────────────────────────────
Check Hash Table:
hash_table[slot_47] → found "Alex"
Return immediately
~100 nanoseconds total
Cache HIT = most common case


STEP 3: IF NOT IN MEMORY (cache miss)
────────────────────────────────────────────────
Check MemTable → not found
Check Bloom Filter for L0-SSTable-1:
"Is user:001 in this file?"
→ YES (bloom filter says check)
Read L0-SSTable-1 from disk
→ Found "Alex"
Return value
~1-5ms total


STEP 4: WORST CASE (key in deep levels)
────────────────────────────────────────────────
Not in Hash Table
Not in MemTable
Not in L0 SSTables
Check L1 → not found
Check L2 → FOUND
Return value
~10-20ms total
(rare, only for cold/old data)
```

---

## Tradeoffs vs Other Approaches

```
┌──────────────────┬────────────────┬────────────────┬───────────────┐
│                  │ THIS ARCH      │ PURE MEMORY    │ PURE DISK     │
│                  │ (Redis+RocksDB)│ (Memcached)    │ (PostgreSQL)  │
├──────────────────┼────────────────┼────────────────┼───────────────┤
│ Read speed       │ ~100ns-5ms     │ ~100ns         │ ~10-50ms      │
│ Write speed      │ ~1-2ms         │ ~100ns         │ ~5-20ms       │
│ Durability       │ HIGH           │ NONE           │ VERY HIGH     │
│ Memory cost      │ MEDIUM         │ VERY HIGH      │ LOW           │
│ Scale            │ HORIZONTAL     │ HORIZONTAL     │ LIMITED       │
│ Consistency      │ TUNABLE        │ NONE           │ STRONG        │
│ Complexity       │ HIGH           │ LOW            │ LOW           │
└──────────────────┴────────────────┴────────────────┴───────────────┘
```

---

## One Line Summary

> **The key-value store uses three layers because each solves what the others cannot — the Hash Table gives O(1) speed in memory, the LSM Tree converts that memory into durable sequential disk writes 30x faster than B-trees, and Consistent Hashing with virtual nodes scales both across machines while keeping only 20% of keys moving when the cluster grows. AOF plus RDB gives you both granular recovery and fast restarts, while quorum writes ensure no data is lost even when nodes crash mid-write.**