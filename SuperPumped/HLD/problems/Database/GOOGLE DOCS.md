
## The Core Problem Google Docs Solves

On the surface collaborative editing seems simple — store text, let multiple people edit. But consider the real constraints:

```
Google Docs reality:
────────────────────────────────────────────────
Users editing simultaneously:    50+ (in one doc)
Character operations per second: 1,000+ (during active typing)
Document size:                   100+ pages (millions of characters)
Network latency:                 50-500ms between users
Requirements:
→ Every user sees same final document (consistency)
→ Every keystroke appears instantly on all screens (<100ms)
→ No character duplication or loss (no "hello" becoming "hheelllloo")
→ Works even when users are offline then reconnect
→ Can replay full document history for any point in time
→ Handle conflicts when two users type at exact same position
```

This combination of **millisecond latency + perfect consistency + conflict resolution + offline support + full history** is what forces this specific architecture.

---

## Why MongoDB for Documents?

### The Flexible Schema Problem

```
What a Google Doc actually contains:
════════════════════════════════════════════════════════

NOT just plain text:
{
  "content": "Hello world"
}

ACTUAL structure:
{
  "doc_id": "doc_123",
  "owner_id": "user_001",
  "content_version": 8472,
  "content": {
    "blocks": [
      {
        "type": "heading",
        "level": 1,
        "text": "Project Proposal",
        "formatting": {"bold": true, "color": "#333"}
      },
      {
        "type": "paragraph",
        "text": "This proposal outlines...",
        "formatting": {"fontSize": 12},
        "comments": [
          {"user": "alice", "text": "Great start!", "position": 5}
        ]
      },
      {
        "type": "image",
        "url": "https://cdn.../image.jpg",
        "width": 600,
        "caption": "Architecture diagram",
        "position": {"x": 100, "y": 200}
      },
      {
        "type": "table",
        "rows": 5,
        "cols": 3,
        "cells": [...]
      },
      {
        "type": "code_block",
        "language": "python",
        "content": "def hello():\n  print('world')"
      }
    ]
  },
  "metadata": {
    "created_at": "2024-01-15",
    "last_modified": "2024-02-26",
    "permissions": {
      "public": false,
      "collaborators": ["user_002", "user_003"]
    },
    "settings": {
      "page_size": "letter",
      "margins": {"top": 1, "bottom": 1},
      "line_spacing": 1.5
    }
  }
}
```

Every document has **completely different structure**. One doc is pure text. Another has tables. Another has embedded charts. Another has LaTeX equations.

### Why MongoDB Handles This Perfectly

```
TRYING TO USE POSTGRESQL FOR DOCUMENT STORAGE:
════════════════════════════════════════════════════════

Schema design nightmare:
────────────────────────────────────────────────

Documents table:
- doc_id, owner_id, created_at...

Paragraphs table:
- paragraph_id, doc_id, order, text, formatting...

Images table:
- image_id, doc_id, url, width, height, position...

Tables table:
- table_id, doc_id, rows, cols...

Table_Cells table:
- cell_id, table_id, row, col, content...

Comments table:
- comment_id, doc_id, user_id, text, position...

Formatting table:
- format_id, paragraph_id, start_pos, end_pos, style...


To render ONE document:
────────────────────────────────────────────────
SELECT * FROM documents WHERE doc_id = ?
SELECT * FROM paragraphs WHERE doc_id = ? ORDER BY order
SELECT * FROM images WHERE doc_id = ?
SELECT * FROM tables WHERE doc_id = ?
FOR EACH table:
  SELECT * FROM table_cells WHERE table_id = ?
SELECT * FROM comments WHERE doc_id = ?
SELECT * FROM formatting WHERE paragraph_id IN (...)

→ 10+ SQL queries
→ Complex joins
→ Slow (100-500ms)
→ Adding new content type (like LaTeX) requires schema migration
→ Rigid structure doesn't fit evolving product needs


MONGODB APPROACH:
════════════════════════════════════════════════════════

db.documents.findOne({doc_id: "doc_123"})

→ ONE query
→ Entire document structure returned
→ No joins needed
→ Response in <10ms
→ Adding new content type? Just add it to the JSON
→ No schema migration
→ Flexible for product evolution

Example - adding LaTeX support:
────────────────────────────────────────────────
PostgreSQL: ALTER TABLE, add latex_blocks table, migrate data
MongoDB: Just start inserting {type: "latex", formula: "..."}

This is why document-oriented databases exist
```

### MongoDB Read Pattern for Documents

```
User opens document:
════════════════════════════════════════════════════════

db.documents.findOne(
  {doc_id: "doc_123"},
  {
    content: 1,           // Fetch full content
    content_version: 1,   // Current version number
    metadata: 1           // Permissions, settings
  }
)

Returns entire document in ONE network round-trip
~5-10ms total

Compare to PostgreSQL: 10+ queries, 100-500ms
```

---

## Why PostgreSQL for Operational Transforms?

### What Is an Operation?

```
User types one character:
════════════════════════════════════════════════════════

Operation generated:
{
  "doc_id": "doc_123",
  "operation_id": 8473,          ← sequential ID
  "type": "INSERT",
  "position": 42,                ← character position in doc
  "text": "a",                   ← the character typed
  "timestamp": 1708956789.123,
  "user_id": "alice",
  "client_version": 8472         ← what version alice had
}

This operation must be:
→ Stored permanently (audit trail)
→ Broadcast to all connected users
→ Applied to document in correct order
→ Transformable against concurrent operations
```

### Why PostgreSQL for Operations (Not MongoDB)

```
OPERATIONS TABLE REQUIREMENTS:
════════════════════════════════════════════════════════

1. Strict ordering (CRITICAL):
────────────────────────────────────────────────
Operation 8472 must come before 8473
Operation 8473 must come before 8474
Any gap in sequence = corruption

PostgreSQL:
CREATE SEQUENCE operation_id_seq;
CREATE TABLE operations (
  doc_id UUID,
  operation_id BIGINT PRIMARY KEY DEFAULT nextval('operation_id_seq'),
  ...
);

→ SERIAL/SEQUENCE guarantees no gaps
→ ACID transactions prevent race conditions
→ Two operations cannot get same ID

MongoDB:
ObjectId or auto-increment field
→ Not guaranteed sequential in distributed setup
→ No native sequence with gap-free guarantee
→ Possible race conditions in high concurrency


2. Transactional consistency:
────────────────────────────────────────────────
When storing operation AND updating document version:

BEGIN TRANSACTION;
  INSERT INTO operations (doc_id, operation_id, type, ...)
  VALUES ('doc_123', 8473, 'INSERT', ...);
  
  UPDATE documents SET content_version = 8473
  WHERE doc_id = 'doc_123';
COMMIT;

If commit fails:
→ BOTH operations rolled back
→ No inconsistent state
→ Document version matches last operation

MongoDB (before v4.0, and limited even now):
→ No multi-document transactions (or limited)
→ Operation could be stored but version not updated
→ Document becomes inconsistent
→ Difficult to detect and recover


3. Range queries by operation ID:
────────────────────────────────────────────────
"Give me all operations for doc_123 after version 8000"

SELECT * FROM operations
WHERE doc_id = 'doc_123'
AND operation_id > 8000
ORDER BY operation_id ASC;

→ Uses composite index (doc_id, operation_id)
→ Efficient range scan
→ Guaranteed order

MongoDB:
→ Can do this but PostgreSQL optimizes better
→ B-tree index on sequential integers is highly efficient


4. Conflict resolution requires exact ordering:
────────────────────────────────────────────────
When Alice and Bob both insert at position 42:

Alice's operation_id: 8472
Bob's operation_id: 8473

System must apply 8472 first, then transform 8473
Any ordering ambiguity = document corruption

PostgreSQL guarantees this with ACID + sequences
MongoDB has weaker guarantees (eventual consistency)
```

---

## Why WebSockets + Redis Pub/Sub for Real-Time?

### The Instant Update Problem

```
Traditional HTTP approach (WRONG):
════════════════════════════════════════════════════════

Alice types "a"
        │
        ▼
POST /api/document/doc_123/insert
{position: 42, text: "a"}
        │
        ▼
Server saves to database
        │
        ▼
Bob polls: GET /api/document/doc_123/changes?after=8471
        │
        ▼
Bob receives Alice's change
        │
        ▼
Bob's screen updates

Problems:
→ Bob polls every 100ms → 10 HTTP requests/second
→ Latency: 100-500ms between Alice typing and Bob seeing
→ 100 users polling = 1000 requests/second on server
→ Most polls return "no changes" (wasted)
→ Not real-time (feels laggy)
```

### WebSockets Solve the Push Problem

```
WebSocket approach (CORRECT):
════════════════════════════════════════════════════════

Connection established ONCE:
Alice browser ←──WebSocket──→ Server
Bob browser ←──WebSocket──→ Server

Alice types "a"
        │
        ▼
Sent over WebSocket (already open)
{type: "INSERT", position: 42, text: "a"}
        │
        ▼
Server processes immediately
        │
        ▼
Server pushes to Bob over his WebSocket
        │
        ▼
Bob's screen updates instantly

Benefits:
→ No polling (persistent connection)
→ Latency: 10-50ms (much faster)
→ Server can PUSH to clients
→ Efficient (one connection, many messages)
→ Real-time feel
```

### Redis Pub/Sub for Multi-Server Scale

```
Single server problem:
════════════════════════════════════════════════════════

Alice connects to Server A
Bob connects to Server A
Alice types → Server A → broadcasts to Bob ✓ WORKS

BUT at scale:
Alice connects to Server A
Bob connects to Server B
Alice types → Server A → ??? → Server B needs to know
                                but they don't communicate


Redis Pub/Sub solution:
════════════════════════════════════════════════════════

      Alice                           Bob
        │                             │
   WebSocket                     WebSocket
        │                             │
        ▼                             ▼
  ┌──────────┐                 ┌──────────┐
  │ Server A │                 │ Server B │
  └────┬─────┘                 └────┬─────┘
       │                             │
       │  PUBLISH                    │  SUBSCRIBE
       │  doc:doc_123:ops            │  doc:doc_123:ops
       │                             │
       └────────┬───────────┬────────┘
                │           │
                ▼           ▼
         ┌──────────────────────┐
         │    REDIS PUB/SUB     │
         │                      │
         │  Channel:            │
         │  doc:doc_123:ops     │
         └──────────────────────┘

Flow:
────────────────────────────────────────────────
1. Alice types "a" → Server A
2. Server A: PUBLISH doc:doc_123:ops '{"type":"INSERT",...}'
3. Redis broadcasts to ALL subscribers
4. Server B receives broadcast (subscribed to doc:doc_123:ops)
5. Server B pushes to Bob over WebSocket
6. Bob sees Alice's change INSTANTLY

This works across 1000 servers
```

---

## Complete Schema Architecture

```
MONGODB SCHEMA (Document storage):
════════════════════════════════════════════════════════

Collection: documents
────────────────────────────────────────────────────────
{
  _id: ObjectId("..."),
  doc_id: "doc_123",                    // UUID
  owner_id: "user_001",
  content_version: 8472,                // Last applied operation ID
  current_content: {
    blocks: [
      {type: "paragraph", text: "Hello world", ...},
      {type: "image", url: "...", ...}
    ]
  },
  metadata: {
    created_at: ISODate("2024-01-15"),
    last_modified: ISODate("2024-02-26"),
    collaborators: ["user_002", "user_003"],
    permissions: {...}
  }
}

Indexes:
────────────────────────────────────────────────
db.documents.createIndex({doc_id: 1}, {unique: true})
db.documents.createIndex({owner_id: 1})


POSTGRESQL SCHEMA (Operations log):
════════════════════════════════════════════════════════

Operations table:
────────────────────────────────────────────────────────
doc_id       │ operation_id │ type   │ position │ text │ timestamp           │ user_id
─────────────────────────────────────────────────────────────────────────────────────────
doc_123      │ 8470         │ INSERT │ 41       │ "H"  │ 2024-02-26 10:00:00 │ alice
doc_123      │ 8471         │ INSERT │ 42       │ "e"  │ 2024-02-26 10:00:01 │ alice
doc_123      │ 8472         │ INSERT │ 43       │ "l"  │ 2024-02-26 10:00:02 │ alice
doc_123      │ 8473         │ DELETE │ 42       │ NULL │ 2024-02-26 10:00:05 │ bob
doc_123      │ 8474         │ INSERT │ 42       │ "a"  │ 2024-02-26 10:00:06 │ bob

Additional columns:
  client_version:  BIGINT  // What version client had when operation created
  server_version:  BIGINT  // What it became after transformation
  transformed:     BOOLEAN // Whether this was transformed against concurrent ops
  
Primary key: (doc_id, operation_id)
Indexes:
  - PRIMARY KEY (doc_id, operation_id)
  - INDEX (doc_id, timestamp) for history queries
  - INDEX (user_id) for user activity tracking


Documents table (in PostgreSQL, for version tracking):
────────────────────────────────────────────────────────
doc_id      │ content_version │ last_snapshot_at
────────────────────────────────────────────────────────
doc_123     │ 8474            │ 2024-02-26 09:00:00

Purpose: Track current version for each document
Ensures operation_id sequence matches document state


REDIS SCHEMA (Real-time pubsub):
════════════════════════════════════════════════════════

Pub/Sub Channels:
────────────────────────────────────────────────
Channel: doc:doc_123:ops

Message format:
{
  "operation_id": 8473,
  "type": "DELETE",
  "position": 42,
  "user_id": "bob",
  "timestamp": 1708956789.123
}

Active users set (for presence):
────────────────────────────────────────────────
Key: doc:doc_123:users
Type: SET
Members: ["alice", "bob", "charlie"]
TTL: 60 seconds (refreshed by heartbeat)

SADD doc:doc_123:users "alice"
EXPIRE doc:doc_123:users 60


Operation buffer (recent ops for new joiners):
────────────────────────────────────────────────
Key: doc:doc_123:recent_ops
Type: LIST
Value: [op_8472, op_8473, op_8474, ...]
TTL: 300 seconds (5 minutes)

LPUSH doc:doc_123:recent_ops '{"operation_id": 8473, ...}'
LTRIM doc:doc_123:recent_ops 0 999  // Keep last 1000 ops
EXPIRE doc:doc_123:recent_ops 300
```

---

## Complete Database Flow

```
FLOW 1: User Opens Document
════════════════════════════════════════════════════════

Alice opens doc_123
        │
        ▼
STEP 1: Fetch document from MongoDB
────────────────────────────────────────────────
db.documents.findOne({doc_id: "doc_123"})

Returns:
{
  doc_id: "doc_123",
  content_version: 8472,
  current_content: {...full document...}
}

Time: ~5ms


STEP 2: Establish WebSocket connection
────────────────────────────────────────────────
Browser ←──WebSocket──→ Server A

Server A subscribes to Redis Pub/Sub:
SUBSCRIBE doc:doc_123:ops

Server A adds Alice to active users:
SADD doc:doc_123:users "alice"
EXPIRE doc:doc_123:users 60


STEP 3: Check for missed operations
────────────────────────────────────────────────
(In case document loaded from stale MongoDB replica)

PostgreSQL query:
SELECT * FROM operations
WHERE doc_id = 'doc_123'
AND operation_id > 8472
ORDER BY operation_id ASC;

If any operations exist:
→ Apply them to Alice's local document
→ Update her version to latest

Time: ~2ms


Total: Alice sees document in <10ms
```

```
FLOW 2: Alice Types a Character
════════════════════════════════════════════════════════

Alice types "a" at position 42
        │
        ▼
STEP 1: Client generates operation
────────────────────────────────────────────────
{
  doc_id: "doc_123",
  type: "INSERT",
  position: 42,
  text: "a",
  client_version: 8472,  // Version Alice has locally
  user_id: "alice"
}

Alice's screen updates IMMEDIATELY (optimistic update)
        │
        ▼
STEP 2: Send operation via WebSocket
────────────────────────────────────────────────
WebSocket message to Server A
Time: ~10ms (network latency)


STEP 3: Server stores operation in PostgreSQL
────────────────────────────────────────────────
BEGIN TRANSACTION;

-- Get next operation ID
SELECT nextval('operation_id_seq');  // Returns 8473

-- Check for conflicts
SELECT operation_id FROM operations
WHERE doc_id = 'doc_123'
AND operation_id > 8472
ORDER BY operation_id ASC;

-- If concurrent operations exist, transform Alice's operation
-- (Operational Transformation algorithm)
-- Example: If Bob inserted at position 42 first (op 8473),
--          Alice's position shifts to 43

INSERT INTO operations
(doc_id, operation_id, type, position, text, user_id, timestamp, client_version)
VALUES
('doc_123', 8473, 'INSERT', 42, 'a', 'alice', NOW(), 8472);

UPDATE documents SET content_version = 8473
WHERE doc_id = 'doc_123';

COMMIT;

Time: ~3ms


STEP 4: Broadcast via Redis Pub/Sub
────────────────────────────────────────────────
Server A publishes:
PUBLISH doc:doc_123:ops '{
  "operation_id": 8473,
  "type": "INSERT",
  "position": 42,
  "text": "a",
  "user_id": "alice"
}'

Redis broadcasts to ALL subscribers
(Server A, Server B, Server C...)
        │
        ▼
STEP 5: Other servers receive broadcast
────────────────────────────────────────────────
Server B (where Bob is connected) receives message

Server B → Bob via WebSocket:
"Alice inserted 'a' at position 42"

Bob's screen updates: "Hello" → "Hallo"

Total time from Alice typing to Bob seeing: ~15-30ms
```

```
FLOW 3: Concurrent Edits (Conflict Resolution)
════════════════════════════════════════════════════════

Initial document: "Hello"
Position:          01234

At EXACTLY same time:
Alice inserts "X" at position 2 (between "He" and "llo")
Bob inserts "Y" at position 2 (same spot!)

Timeline:
────────────────────────────────────────────────
T+0ms:  Alice types "X", position 2
        Alice's client version: 8472
        Optimistic: Alice sees "HeXllo"

T+0ms:  Bob types "Y", position 2
        Bob's client version: 8472
        Optimistic: Bob sees "HeYllo"

T+10ms: Alice's operation arrives at Server A
        Server A acquires PostgreSQL transaction lock

T+11ms: Bob's operation arrives at Server B
        Server B tries to acquire PostgreSQL transaction lock
        BLOCKED (Server A has it)

T+13ms: Server A completes:
        INSERT INTO operations (...) VALUES
        (..., operation_id=8473, type='INSERT', pos=2, text='X');
        
        COMMIT;
        
        Server A publishes to Redis:
        PUBLISH doc:doc_123:ops '{"op_id": 8473, ...}'

T+14ms: Server B's transaction unblocks
        Checks for concurrent operations:
        SELECT * FROM operations
        WHERE doc_id='doc_123' AND operation_id > 8472;
        
        Finds operation 8473 (Alice's "X" at position 2)
        
        OPERATIONAL TRANSFORMATION:
        Bob wanted position 2
        But Alice already inserted at position 2 (op 8473)
        Bob's operation transforms to position 3
        
        INSERT INTO operations (...) VALUES
        (..., operation_id=8474, type='INSERT', pos=3, text='Y');
        
        COMMIT;
        
        Server B publishes:
        PUBLISH doc:doc_123:ops '{"op_id": 8474, ...}'

T+15ms: All clients receive both operations via Pub/Sub:
        1. Apply op 8473: "Hello" → "HeXllo" (Alice's X at pos 2)
        2. Apply op 8474: "HeXllo" → "HeXYllo" (Bob's Y at pos 3)

Final result for EVERYONE: "HeXYllo"
Alice sees: "HeXllo" → "HeXYllo"  (her X stayed, Bob's Y added after)
Bob sees:   "HeYllo" → "HeXYllo"  (his Y moved one position right)

Perfect consistency through Operational Transformation!
```

```
FLOW 4: Periodic Snapshot (Performance Optimization)
════════════════════════════════════════════════════════

Problem: Document has 1,000,000 operations
New user joins → must replay all 1M operations?
Takes 10+ seconds to render document

Solution: Periodic snapshots
────────────────────────────────────────────────

Every 1000 operations (or every 10 minutes):

STEP 1: Background worker queries latest operations
────────────────────────────────────────────────
SELECT * FROM operations
WHERE doc_id = 'doc_123'
AND operation_id > last_snapshot_version
ORDER BY operation_id ASC;


STEP 2: Apply all operations to snapshot
────────────────────────────────────────────────
Start with: last_snapshot_content
Apply op 8001, 8002, ..., 9000
Result: new_snapshot_content


STEP 3: Save snapshot to MongoDB
────────────────────────────────────────────────
db.documents.updateOne(
  {doc_id: "doc_123"},
  {$set: {
    current_content: new_snapshot_content,
    content_version: 9000,
    last_snapshot_at: new Date()
  }}
)


STEP 4: Archive old operations
────────────────────────────────────────────────
Operations before 8000 can be moved to cold storage:
- Move to S3
- Or keep in PostgreSQL but in separate partition
- Or compress heavily

Active table only keeps recent operations


Now when new user joins:
────────────────────────────────────────────────
1. Load snapshot (version 9000) from MongoDB
2. Replay only operations 9001-9500 from PostgreSQL
3. User sees document in <100ms instead of 10+ seconds
```

---

## Tradeoffs vs Other Databases

```
┌──────────────────────┬───────────────────┬─────────────────┬───────────────────┐
│                      │ THIS ARCHITECTURE │ POSTGRESQL ONLY │ MONGODB ONLY      │
├──────────────────────┼───────────────────┼─────────────────┼───────────────────┤
│ Document flexibility │ MongoDB ✓         │ JSONB possible  │ MongoDB ✓         │
│ Operation ordering   │ PostgreSQL ✓      │ PostgreSQL ✓    │ Weak guarantees ✗ │
│ ACID transactions    │ PostgreSQL ✓      │ PostgreSQL ✓    │ Limited           │
│ Read performance     │ <10ms ✓           │ 100ms (joins)   │ <10ms ✓           │
│ Real-time sync       │ Redis ✓           │ LISTEN/NOTIFY   │ Change Streams    │
│ Complexity           │ HIGH              │ MEDIUM          │ MEDIUM            │
│ Consistency          │ STRONG ✓          │ STRONG ✓        │ EVENTUAL          │
│ Offline support      │ Replay ops ✓      │ Replay ops ✓    │ Difficult         │
└──────────────────────┴───────────────────┴─────────────────┴───────────────────┘
```

---

## One Line Summary

> **MongoDB stores flexible document content because every Google Doc has wildly different structure (text, images, tables, code blocks) that would require 10+ joined tables in PostgreSQL causing 500ms load times instead of MongoDB's single 5ms query, PostgreSQL stores the sequential operation log because its ACID transactions and gap-free sequences guarantee perfect ordering critical for Operational Transformation to resolve conflicts when Alice and Bob type at position 42 simultaneously, and WebSockets + Redis Pub/Sub deliver operations in 15-30ms by maintaining persistent connections and broadcasting across servers so when Alice types on Server A, Bob on Server B sees it instantly without polling — together they achieve Google Docs' magic of seeing everyone's cursor move in real-time while guaranteeing everyone sees identical final text even under concurrent edits.**


