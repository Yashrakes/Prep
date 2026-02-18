
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