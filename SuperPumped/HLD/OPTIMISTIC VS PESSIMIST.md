# Optimistic vs Pessimistic Locking in Distributed Systems

---

## The Core Problem: Concurrent Access

In any distributed system, multiple processes/services can try to read and modify the same data simultaneously. Without coordination, you get **race conditions**, **lost updates**, and **data corruption**.

**Example:** Two users simultaneously try to book the last seat on a flight. Without locking, both see "1 seat available", both proceed, and you've overbooked.

---

## Pessimistic Locking

### Philosophy

_"Something WILL go wrong, so I'll lock the resource before touching it."_

You assume conflict is likely and prevent it upfront by acquiring a lock before reading or writing data. No one else can touch that resource until you're done.

### How It Works (Flow)

```
Client A                    Database/Lock Manager           Client B
   |                               |                           |
   |--- LOCK row (SELECT FOR UPDATE)|                          |
   |                               |--- Lock granted -------->|
   |                               |                           |
   |--- READ data ----------------->|                          |
   |                               |                           |
   |                               |<-- Client B tries LOCK --|
   |                               |--- BLOCKED / WAIT ------->|
   |--- DO business logic          |                           |
   |--- WRITE updated data -------->|                          |
   |--- COMMIT / RELEASE LOCK ----->|                          |
   |                               |--- Lock granted -------->|
   |                               |<-- Client B can now read--|
```

### In SQL (PostgreSQL Example)

sql

````sql
BEGIN;

-- Acquires an exclusive row-level lock
SELECT * FROM inventory
WHERE product_id = 42
FOR UPDATE;  -- <-- This is the pessimistic lock

-- Now safely update
UPDATE inventory
SET quantity = quantity - 1
WHERE product_id = 42;

COMMIT; -- Lock released here
```

Other variants:
- `FOR SHARE` — allows others to read, blocks writes
- `FOR UPDATE NOWAIT` — fails immediately if lock unavailable (no blocking)
- `FOR UPDATE SKIP LOCKED` — skips locked rows (useful for job queues)

### In Distributed Systems (Beyond Single DB)

A single DB `SELECT FOR UPDATE` only works within one database. In distributed systems you need external lock managers:

**Redis-based distributed lock (Redlock):**
```
1. Client gets current timestamp T1
2. Client tries to SET lock_key "client_id" NX PX 30000
   (NX = only if not exists, PX = 30000ms TTL)
3. If SET succeeds → lock acquired
4. Do your work
5. DELETE lock_key (only if value == client_id)
````

python

````python
# Using redis-py
import redis
import uuid

r = redis.Redis()
lock_id = str(uuid.uuid4())

# Acquire lock
acquired = r.set("lock:product:42", lock_id, nx=True, px=30000)

if acquired:
    try:
        # Do critical work
        process_inventory_update(product_id=42)
    finally:
        # Release only if we own the lock (atomic via Lua script)
        lua_script = """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        else
            return 0
        end
        """
        r.eval(lua_script, 1, "lock:product:42", lock_id)
```

**ZooKeeper-based locking:**
- Create ephemeral sequential node `/locks/resource-0000001`
- Watch the node with the next lower sequence number
- If yours is lowest → you hold the lock
- When that node is deleted → you acquire the lock
- Ephemeral nodes auto-delete if client disconnects (prevents deadlock)

---


````

## Optimistic Locking

### Philosophy
*"Conflicts are RARE, so let's not lock at all. We'll detect and handle conflicts at write time."*

You read freely, do your work, and when you go to write, you verify nothing changed while you were working. If it did change, you retry or fail.

### How It Works (Flow)
```
Client A                    Database                    Client B
   |                           |                           |
   |--- READ row (version=5) -->|                          |
   |                           |<-- READ row (version=5) --|
   |                           |                           |
   |--- Do business logic      |--- Do business logic      |
   |                           |                           |
   |--- UPDATE WHERE version=5 |                           |
   |    SET version=6 -------->|                           |
   |                           |--- Rows updated: 1 ------>|
   |   ✅ SUCCESS              |                           |
   |                           |                           |
   |                           |<-- UPDATE WHERE version=5 |
   |                           |    SET version=6 ---------|
   |                           |--- Rows updated: 0 ------->
   |                           |   ❌ CONFLICT! Retry?    |
### In SQL

sql

```sql
-- Step 1: Read with version
SELECT id, quantity, version
FROM inventory
WHERE product_id = 42;
-- Returns: quantity=10, version=5

-- Step 2: Application does business logic

-- Step 3: Conditional update
UPDATE inventory
SET quantity = 9,
    version = version + 1   -- bump version
WHERE product_id = 42
  AND version = 5;          -- only update if nothing changed

-- Check rows affected:
-- 1 row → SUCCESS (we got the update)
-- 0 rows → CONFLICT (someone else changed it, retry or fail)
```

### Using Timestamps Instead of Version Numbers

sql

```sql
UPDATE inventory
SET quantity = 9,
    updated_at = NOW()
WHERE product_id = 42
  AND updated_at = '2024-01-15 10:30:00.123';
```

Timestamps are less reliable than version integers because of clock skew in distributed systems. Prefer integer versions.

### In Application Code (Java/JPA Example)

java

```java
@Entity
public class Inventory {
    @Id
    private Long productId;
    private int quantity;

    @Version  // JPA handles version checking automatically
    private Long version;
}

// JPA will automatically add WHERE version=? to UPDATE
// and throw OptimisticLockException if rows affected = 0
inventoryRepository.save(updatedInventory);
```

### In Distributed Systems (CAS — Compare and Swap)

Many distributed datastores implement this natively:

**DynamoDB Conditional Writes:**

python

```python
dynamodb.update_item(
    TableName='Inventory',
    Key={'product_id': {'N': '42'}},
    UpdateExpression='SET quantity = :new_qty',
    ConditionExpression='version = :expected_version',
    ExpressionAttributeValues={
        ':new_qty': {'N': '9'},
        ':expected_version': {'N': '5'}
    }
)
# Throws ConditionalCheckFailedException if version mismatch
```

**Redis CAS with WATCH/MULTI/EXEC:**

python

```python
with r.pipeline() as pipe:
    while True:
        try:
            pipe.watch('inventory:42')
            current = pipe.get('inventory:42')
            
            pipe.multi()  # Start transaction
            pipe.set('inventory:42', int(current) - 1)
            pipe.execute()  # Fails if watched key changed
            break
        except redis.WatchError:
            continue  # Retry on conflict
```

**etcd CAS:**

python

````python
# Only update if current value matches expected
etcd_client.replace(
    key='/inventory/42',
    initial_value='10',
    new_value='9'
)
```

---

## How Real Production Systems Work

### E-Commerce (Amazon/Shopify style)

**Inventory reservation uses pessimistic locking:**
```
User clicks "Buy Now"
    → Lock inventory row (SELECT FOR UPDATE)
    → Check quantity > 0
    → Create pending order
    → Decrement inventory
    → Commit → Lock released
    → Payment async (saga pattern)
```
Why pessimistic here? Because inventory is scarce, contention is HIGH during flash sales. Optimistic would cause massive retry storms.

**Order status updates use optimistic locking:**
```
Order state: PENDING → CONFIRMED → SHIPPED → DELIVERED
    → Read order (get version)
    → Apply state transition
    → UPDATE WHERE version = N
    → If conflict → re-read and retry
````

Why optimistic here? Order status conflicts are rare. Most of the time only one system is updating a given order at a time.

### Banking Systems

**Debit/Credit transactions use pessimistic locking:**

sql

````sql
BEGIN;
SELECT balance FROM accounts WHERE id = 123 FOR UPDATE;
-- Verify sufficient funds
UPDATE accounts SET balance = balance - 500 WHERE id = 123;
COMMIT;
```

**Audit logs / read-heavy analytics use optimistic locking** or MVCC (Multi-Version Concurrency Control).

### Ticket Booking (IRCTC, BookMyShow)

Hybrid approach:
```
Phase 1 - Seat Selection (Optimistic):
    Show available seats (no lock)
    User picks seat
    
Phase 2 - Hold/Reserve (Pessimistic, short window):
    Lock seat for 10 minutes
    User completes payment
    
Phase 3 - Confirm or Release:
    Payment success → Confirm booking
    Timeout/Failure → Release lock
```

### Microservices with Saga Pattern
```
Order Service           Inventory Service       Payment Service
     |                        |                       |
     |--- Reserve Items ------>|                       |
     |                        |-- Optimistic lock     |
     |                        |-- Check & reserve     |
     |<-- Reserved OK ---------|                       |
     |--- Charge Payment -------------------------------->|
     |                        |                       |-- Process
     |<-- Payment Failed ----------------------------------|
     |--- Compensate: Release Items --->|               |
     |                        |-- Release reservation  |
```

---

## MVCC — What Real Databases Actually Use

Most modern databases (PostgreSQL, MySQL InnoDB, Oracle) use **Multi-Version Concurrency Control (MVCC)** under the hood. It's a form of optimistic locking at the DB engine level.
```
Timeline:
T=1: Row inserted: {id:1, val:"A", txn_id:100}
T=2: Transaction 200 reads → sees "A" (snapshot from T=2)
T=3: Transaction 150 updates to "B" → creates new version: {val:"B", txn_id:150}
T=4: Transaction 200 reads again → still sees "A" (its snapshot)
T=5: Transaction 200 tries to write → detects conflict with 150
```

MVCC gives readers never blocking writers and writers never blocking readers, which is why PostgreSQL is so performant under read-heavy loads.

---

## Tradeoffs: Deep Comparison

| Dimension | Pessimistic Locking | Optimistic Locking |
|---|---|---|
| **Concurrency** | Low — serializes access | High — parallel reads |
| **Throughput** | Lower under high load | Higher under low contention |
| **Latency** | Higher (wait for locks) | Lower (no waiting) |
| **Deadlock Risk** | YES — must handle | NO deadlocks |
| **Starvation Risk** | YES — long queues | Lower |
| **Retry Complexity** | Simple (just wait) | Complex (retry logic needed) |
| **Network Overhead** | Lock acquire/release calls | Extra version checks |
| **Best Scenario** | High contention, scarce resources | Low contention, read-heavy |
| **Failure Mode** | Deadlock, timeout | Retry storm under high load |
| **DB Support** | Native (`FOR UPDATE`) | Via version column or CAS |

---

## When to Use What — Decision Framework

### Use Pessimistic Locking When:

- **Contention is high** — many concurrent users on the same resource
- **Data is scarce** — last N seats, limited inventory, unique resources
- **Operations are short** — lock duration is milliseconds, not seconds
- **You cannot tolerate retries** — financial transactions, idempotency is hard
- **Write-heavy workloads** — most operations modify data
- **External systems are involved** — you need to hold state across multiple calls

Real examples: seat booking, bank transfers, stock trading, job queues, flash sale inventory

### Use Optimistic Locking When:

- **Contention is low** — conflicts are rare in practice
- **Read-heavy workloads** — most operations are reads
- **Long-lived operations** — user fills a form for 2 minutes (can't hold a lock that long)
- **Distributed systems without a central lock manager** — CAS is cheaper
- **You can tolerate retries** — idempotent operations
- **Scalability is critical** — optimistic scales horizontally better

Real examples: CMS document editing, user profile updates, configuration changes, order status updates, social media posts

### Decision Tree
```
Is conflict rate > 20-30%?
├── YES → Pessimistic
└── NO → Is operation long (> 1 second)?
    ├── YES → Optimistic (can't hold lock that long)
    └── NO → Is data scarce/finite?
        ├── YES → Pessimistic
        └── NO → Optimistic
```

---

## Complete Service + Database Flow

### Pessimistic Flow (Microservice → DB)
```
[Client Request]
      ↓
[Load Balancer]
      ↓
[Service Instance]
      ↓
[Connection Pool] → Acquire DB Connection
      ↓
[BEGIN TRANSACTION]
      ↓
[SELECT ... FOR UPDATE] ← Row lock acquired here
      ↓
[Business Logic Validation]
      ↓
[UPDATE / INSERT / DELETE]
      ↓
[COMMIT] ← Row lock released here
      ↓
[Release Connection to Pool]
      ↓
[Return Response to Client]

Failure paths:
- Lock wait timeout → ROLLBACK → Return 409/503
- Deadlock detected → ROLLBACK → Retry or 500
- Connection pool exhausted → Queue or 503
```

### Optimistic Flow (Microservice → DB)
```
[Client Request]
      ↓
[Service Instance]
      ↓
[SELECT with version] (no lock, fast)
      ↓
[Business Logic — can take time]
      ↓
[UPDATE WHERE id=X AND version=N]
      ↓
[Check rows_affected]
├── rows_affected = 1 → SUCCESS → Return 200
└── rows_affected = 0 → CONFLICT
    ↓
    [Retry strategy]
    ├── Exponential backoff + re-read + retry
    ├── Max retries exceeded → Return 409 Conflict
    └── Dead letter queue for async processing
````

### Retry Strategy for Optimistic Locking

python

````python
import time
import random

def update_with_retry(entity_id, new_value, max_retries=3):
    for attempt in range(max_retries):
        # Re-read on every attempt
        row = db.query("SELECT * FROM items WHERE id = %s", entity_id)
        
        rows_updated = db.execute("""
            UPDATE items
            SET value = %s, version = version + 1
            WHERE id = %s AND version = %s
        """, (new_value, entity_id, row.version))
        
        if rows_updated == 1:
            return {"status": "success"}
        
        # Exponential backoff with jitter
        wait = (2 ** attempt) * 0.1 + random.uniform(0, 0.05)
        time.sleep(wait)
    
    raise ConflictException(f"Could not update after {max_retries} attempts")
```

---

## Deadlock Deep Dive (Pessimistic Problem)

Deadlock happens when two transactions each hold a lock the other needs:
```
Transaction A:           Transaction B:
LOCK row 1               LOCK row 2
    |                        |
    |--- Wait for row 2      |--- Wait for row 1
    |         ↑______________↑
    |              DEADLOCK!
```

**Prevention strategies:**

1. **Always acquire locks in the same order** — If all transactions lock row 1 before row 2, deadlock is impossible
2. **Lock timeout** — `SET lock_timeout = '5s'` — fail fast rather than waiting forever
3. **Deadlock detection** — PostgreSQL detects cycles and kills one transaction automatically
4. **Use `NOWAIT`** — Fail immediately rather than waiting: `SELECT FOR UPDATE NOWAIT`

---

## Interview Questions + Answers

---

### Foundational Questions

**Q1: What is the difference between optimistic and pessimistic locking?**

Pessimistic locking prevents conflicts by acquiring an exclusive lock before reading/modifying data. No other transaction can modify that resource until the lock is released. Optimistic locking assumes conflicts are rare — it doesn't lock, instead it validates at write time that nothing changed (usually via a version number), and retries or fails if a conflict is detected.

---

**Q2: When would you choose optimistic over pessimistic locking?**

Choose optimistic when: read-to-write ratio is high, conflicts are rare (< 10-20%), operations are long-lived (can't hold a DB lock for minutes), or scalability is critical. Choose pessimistic when contention is high, resources are scarce (last ticket/seat), or you're doing financial transactions where retries are expensive or dangerous.

---

**Q3: What is a version number in optimistic locking and why is it better than a timestamp?**

A version number is an integer incremented on every write. The UPDATE checks `WHERE version = expected_version`. Timestamps can have clock skew in distributed systems — two machines may report the same millisecond timestamp, causing a false no-conflict result. Integers are monotonically increasing and unambiguous.

---

**Q4: How do you handle the ABA problem in optimistic locking?**

The ABA problem: value goes A → B → A, so a reader thinks nothing changed when actually it did change twice. Using a monotonically incrementing version number (not value-based comparison) solves this — even if value returns to A, the version number (e.g., 7) is different from the original (e.g., 5).

---

**Q5: What is SELECT FOR UPDATE and how does it work?**

It's a SQL statement that reads rows while acquiring an exclusive lock on them simultaneously. Other transactions trying to read those rows with `FOR UPDATE` or modify them will block until the first transaction commits or rolls back. It's the primary mechanism for pessimistic locking in relational databases.

---

### Intermediate Questions

**Q6: How do you implement distributed locking when your data is spread across multiple databases or services?**

Options include: Redis-based locks (Redlock algorithm), ZooKeeper/Consul for coordination, database-backed lock tables, or CAS operations in distributed stores like DynamoDB or etcd. The key requirement is the lock must be atomic (acquired and released atomically), have a TTL (to prevent leaked locks), and the release must be idempotent and only done by the owner.

---

**Q7: What is the Redlock algorithm and what are its criticisms?**

Redlock acquires locks on N (usually 5) independent Redis nodes. The lock is considered acquired if you get it on the majority (> N/2). The clock-based TTL is used for safety. Martin Kleppmann criticized it because it relies on timing assumptions — if a process pauses (GC, network partition), the lock TTL can expire while the process thinks it still holds the lock, causing two processes to think they hold the lock simultaneously. For true safety, you need fencing tokens (monotonically increasing numbers checked by the storage system on every write).

---

**Q8: What are fencing tokens and why are they important?**

When you acquire a distributed lock, the lock service returns a monotonically increasing fencing token. Every write to the storage system includes this token. The storage system rejects writes with tokens older than the last seen token. This ensures that even if a process holds a stale lock (expired but process doesn't know), its writes are rejected because a newer lock holder has a higher token. This is the safe way to implement distributed locking.

---

**Q9: How does MVCC relate to optimistic locking?**

MVCC (Multi-Version Concurrency Control) is a database-level implementation of optimistic concurrency. Each transaction sees a snapshot of the database at the time it started. Multiple versions of rows coexist. Reads never block writes and writes never block reads. When a write occurs, it creates a new row version. Conflicts are detected at commit time, not at read time. This is how PostgreSQL and InnoDB achieve high concurrency.

---

**Q10: Explain the difference between row-level, page-level, and table-level locking.**

Table-level locks entire table — maximum contention, used for DDL operations or full-table writes. Page-level locks a storage page (usually 8KB) containing multiple rows — rare in modern systems. Row-level locks only the specific rows being modified — maximum concurrency, used by default in PostgreSQL/InnoDB for DML. Row-level has higher overhead per lock but much better concurrency.

---

**Q11: How does a connection pool interact with pessimistic locking?**

Connection pool size directly limits concurrency for pessimistic locking. If your pool has 20 connections and 20 transactions are all waiting for locks, you've exhausted your pool — new requests queue or fail with timeout. This is called the "pool starvation" problem. You must size your pool carefully, use lock timeouts aggressively, and ensure transactions are short. Optimistic locking is friendlier to connection pools because connections are held for less time.

---

**Q12: What is a retry storm and how do you prevent it?**

A retry storm is when many clients simultaneously get optimistic locking conflicts and all retry at the same time, causing another wave of conflicts. Prevention: exponential backoff with random jitter (each client waits a different random duration before retrying), circuit breakers (stop retrying when failure rate is too high), and backpressure (shed load upstream when contention is detected).

---

### Advanced Questions

**Q13: How would you implement optimistic locking in a microservices architecture without a shared database?**

Use event sourcing + CQRS. Each service owns its data. Optimistic locking is implemented via event version numbers — when a command arrives, it includes the expected version of the aggregate. The service checks current aggregate version against expected version. If mismatch → reject with conflict error. Alternatively, use a saga with compensating transactions — if a downstream service rejects due to version conflict, upstream services run compensation.

---

**Q14: Design a distributed inventory system for a flash sale (10,000 users competing for 100 items).**

This is a classic high contention scenario. Pure optimistic locking would create a retry storm. The solution is layered: use Redis atomic DECR for fast inventory check and decrement (Redis is single-threaded, so atomic), use a queue to serialize purchase requests (only 100 get through), persist the confirmed purchases to the database with pessimistic locking for the financial record, and use rate limiting + queue depth monitoring to shed excess load early.

---

**Q15: What is the "lost update" problem and how does each locking strategy handle it?**

Lost update: T1 reads value 10, T2 reads value 10, T1 writes 9, T2 writes 9 — T1's update is lost. Pessimistic locking prevents it entirely — T2 blocks until T1 commits, then reads the updated value 9. Optimistic locking detects it — T2's `WHERE version = N` fails because T1 already changed the version, forcing T2 to retry with the fresh value.

---

**Q16: How does Cassandra handle concurrent writes without traditional locking?**

Cassandra uses "last write wins" (LWW) based on client-supplied timestamps. For stronger consistency, it offers Lightweight Transactions (LWT) using Paxos consensus — `INSERT IF NOT EXISTS` or `UPDATE IF condition` — which implements compare-and-swap. This is expensive (4 round trips) so it's used sparingly. Most Cassandra systems use eventual consistency with application-level conflict resolution instead.

---

**Q17: What's the difference between read locks (shared) and write locks (exclusive)?**

A shared/read lock allows multiple transactions to read simultaneously but blocks writers. An exclusive/write lock blocks all other readers and writers. This is often expressed as a readers-writer lock. `SELECT FOR SHARE` in PostgreSQL acquires a shared lock — multiple readers can share it, but it blocks `FOR UPDATE` writers. `SELECT FOR UPDATE` acquires an exclusive lock — blocks everyone. The general rule: readers share, writers exclude.

---

### Follow-Up / Scenario Questions

**Q18: Your service is experiencing frequent deadlocks. How do you diagnose and fix it?**

Diagnosis: query `pg_stat_activity` and `pg_locks` in PostgreSQL; check deadlock logs; enable `log_lock_waits`. Fix: standardize the order in which tables/rows are locked across all code paths; reduce transaction scope (do less work per transaction); use `SELECT FOR UPDATE SKIP LOCKED` for queue patterns; add lock timeouts; consider switching hotspot operations to optimistic locking.

---

**Q19: A user complains that they keep getting "conflict" errors when updating their profile. What's happening?**

This is unexpected contention on a resource that should have low conflict. Investigate whether multiple backend services are updating the same user record concurrently (e.g., sync service + user service). Check if a background job is touching user records without proper version handling. Review if the retry logic is correct and the version column is being read fresh on each attempt. Consider whether this is actually a single user or a bot/test script hammering the endpoint.

---

**Q20: How would you add optimistic locking to a service that currently doesn't have it?**

Add a `version` integer column to the table with `DEFAULT 0`. Update all INSERT statements to include `version = 0`. Update all UPDATE statements to add `WHERE version = :expected AND version = version + 1`. Wrap the execution in retry logic in the application. For API responses, include the version in the response payload so clients can send it back on updates. Ensure backward compatibility during deployment by making version optional initially, then required.

---

## Quick Reference Cheat Sheet
```
┌────────────────────────────────────────────────────────────┐
│  HIGH CONTENTION    →  PESSIMISTIC  (lock first, work safe)│
│  LOW CONTENTION     →  OPTIMISTIC   (work fast, retry rare)│
│  LONG OPERATIONS    →  OPTIMISTIC   (can't hold lock long) │
│  SCARCE RESOURCES   →  PESSIMISTIC  (must guarantee count) │
│  DISTRIBUTED + CAS  →  OPTIMISTIC   (cheaper, no lock mgr) │
│  FINANCIAL TX       →  PESSIMISTIC  (correctness > speed)  │
└────────────────────────────────────────────────────────────┘
````

---