
## The Core Problem Google Docs Solves

Before picking any database, you need to understand what makes Google Docs _hard_. It's not just storing text — any database can do that. The real challenge is: **what happens when Alice and Bob are both editing the same paragraph at the same time?**

Imagine a document contains the word `"Hello"`. Alice deletes the `"H"` at position 0, and Bob inserts `"!"` at position 5 — simultaneously. If you apply these operations naively, in the wrong order, you get garbage. This is called the **concurrent edit problem**, and it's the entire reason this architecture exists.

---

## Why MongoDB for Documents?

MongoDB is a _document-oriented_ database, which means it stores data as flexible JSON-like objects (called BSON). This is a natural fit for documents because:

A Google Doc's content isn't a neat table of rows and columns — it's a rich, nested structure with paragraphs, headings, images, comments, and formatting metadata. In a relational database like MySQL, you'd have to split this across dozens of tables and reassemble it with complex JOINs every time someone opens a document. In MongoDB, the entire document content lives in one place as a single object.

Also, **schema flexibility matters here**. Different documents have wildly different structures. A simple text note looks nothing like a document with embedded tables, charts, or code blocks. MongoDB lets you store these without a rigid pre-defined schema.

> Think of it this way: SQL databases are like spreadsheets — great for uniform, structured data. MongoDB is like a filing cabinet — each folder (document) can contain whatever you need.

Why _not_ another NoSQL option? Cassandra is optimized for write-heavy, distributed time-series data — overkill here and harder to query. DynamoDB would work but is AWS-locked. MongoDB gives you flexibility, rich querying, and a mature ecosystem.

---

## Why PostgreSQL for Operational Transforms?

This is the most interesting choice. You might ask — _"if we're already using MongoDB, why bring in a second database just for operations?"_

Here's the key insight: **operations have relational, transactional requirements that document databases aren't designed for.**

An "operation" (insert character at position 5, delete 3 characters starting at position 2, etc.) must be:

**Ordered** — operations have a strict sequence. Operation #42 must come after #41, always. PostgreSQL gives you sequences and transactional guarantees that make this rock-solid.

**Consistent under concurrent writes** — when two users submit operations at the same moment, PostgreSQL's ACID transactions ensure only one wins the race to write, preventing duplicates or gaps in the operation log.

**Queryable in complex ways** — "give me all operations on doc X between timestamp A and B by user Y" — this is exactly what SQL excels at.

MongoDB could store operations too, but its eventual consistency model and looser transaction support (especially in older versions) made it a riskier choice for something that _must_ be perfectly ordered. PostgreSQL's row-level locking and serializable transactions are battle-tested for exactly this kind of workload.

---

## Why WebSockets + Redis for Real-Time Sync?

Regular HTTP is _request-response_ — your browser asks, the server answers, connection closes. That model breaks completely for real-time collaboration, because the server needs to _push_ changes to all connected users the instant they happen.

**WebSockets** solve this by keeping a persistent, bidirectional connection open between your browser and the server. When Bob types a character, the server immediately pushes that operation to Alice's browser over her WebSocket connection — no polling, no delay.

But here's the scaling problem: Google Docs has millions of concurrent users. You can't run this on a single server. When you have 50 servers, and Alice is connected to Server A while Bob is connected to Server B, how does Server A know to forward Bob's edit to Alice?

**Redis Pub/Sub** is the message broker that solves this. Every server subscribes to a "channel" for each active document. When Bob's server receives his edit, it publishes the operation to Redis on the channel `doc:abc123`. Every other server subscribed to that channel (including Alice's server) instantly receives it and pushes it to their connected clients.

> Redis works here because it's entirely **in-memory** — reads and writes happen in microseconds, which is essential when you're forwarding keystrokes in real time.

---

## Understanding the Schema Design

Now let's look at why the schema is structured the way it is.

### The Documents Table

```
Documents:
- doc_id         → uniquely identifies the document
- owner_id       → who created/owns it
- content_version → which version of the content this represents
- current_content → the actual text/structure of the document right now
```

The `current_content` field is essentially a **snapshot** — the materialized result of applying all operations up to `content_version`. You store this so that when someone opens a document, you don't have to replay _every single operation since the document was created_ to reconstruct the current state. You start from the snapshot and only replay recent operations.

The `content_version` is the bridge between the two tables. It tells you: "this snapshot reflects all operations up to version N."

### The Operations Table

```
Operations:
- doc_id       → which document this operation belongs to
- operation_id → the sequence number of this operation (monotonically increasing)
- type         → what kind of operation (insert, delete, format, etc.)
- position     → where in the document this operation applies
- text         → what text was inserted (for insert operations)
- timestamp    → when it happened (for debugging, history, conflict resolution)
- user_id      → who made this change (for showing cursors, attribution)
```

The **composite primary key** of `(doc_id, operation_id)` is a deliberate design choice. It means operations are globally unique per document, and you can efficiently query "all operations for document X after operation #500" — which is exactly what a newly reconnected client needs to catch up.

---

## How the Issues Are Actually Solved

**Concurrent Edits with Operational Transformation** is the heart of the system. When Alice and Bob both submit operation #42, the server detects the conflict (same `operation_id` attempted twice) and _transforms_ one operation relative to the other. For example, if Alice inserted a character at position 5 and Bob deleted at position 3, Bob's delete effectively shifts Alice's insertion point — so Alice's operation gets transformed to position 4 before being applied. This ensures both operations are eventually applied correctly for everyone.

**Vector Clocks** augment the simple `timestamp` for conflict resolution. A vector clock is a small data structure that tracks "how many operations each user has seen." This is more reliable than wall-clock timestamps because clocks on distributed servers can drift. If two operations genuinely conflict (both trying to modify the exact same character), vector clocks let the system determine a _causal_ ordering — did Alice's edit happen _before_ or _after_ Bob's, from each user's perspective?

**Periodic Snapshots** solve the performance problem of operation log growth. If a document has been edited 500,000 times, you don't want to replay all 500,000 operations every time someone opens it. Periodically, the system takes the current `current_content`, saves it as the new snapshot, and advances `content_version` — effectively compressing history. Older operations can then be archived or deleted.

---

## The Big Picture

The reason this architecture uses _two_ databases rather than one is that different parts of the problem have genuinely different characteristics. Document content is flexible and hierarchical (→ MongoDB). Operation logs are sequential, transactional, and relational (→ PostgreSQL). Real-time messaging is ephemeral and latency-sensitive (→ Redis). Forcing all three into a single database would mean compromising on at least one of these requirements — and in a system like Google Docs, any compromise is felt by millions of users immediately.

Would you like me to go deeper on any specific part — for instance, how Operational Transformation actually works mathematically, or how the snapshot + operation-log pattern compares to event sourcing in general software architecture?



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


