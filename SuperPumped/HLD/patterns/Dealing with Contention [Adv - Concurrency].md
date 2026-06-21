# Dealing with Contention Pattern

## Overview
Pattern for handling scenarios where multiple users/processes try to access and modify the same resource simultaneously. Prevents race conditions and ensures data consistency.

---

## When to Use This Pattern
- **Booking systems** - Last concert ticket, hotel room
- **Inventory management** - E-commerce stock levels
- **Auction systems** - Multiple bidders on same item
- **Seat selection** - Movie theaters, flights
- **Financial transactions** - Account balances, transfers
- **Collaborative editing** - Multiple users editing same document
- **Like/vote counters** - Preventing double-counting
- **Coupon redemption** - Limited use codes

---

## Common Race Condition Examples

### Example 1: E-commerce Inventory
```
Initial state: 1 item in stock

User A reads: stock = 1
User B reads: stock = 1
User A buys: stock = 1 - 1 = 0
User B buys: stock = 1 - 1 = 0

Result: Both purchases succeed, sold 2 items when only 1 exists!
```

### Example 2: Bank Account Transfer
```
Initial: Account balance = $100

Transaction A: Withdraw $80
Transaction B: Withdraw $80

Without locking:
A reads: $100, checks if 100 >= 80 ✓
B reads: $100, checks if 100 >= 80 ✓
A writes: $100 - $80 = $20
B writes: $100 - $80 = $20

Result: Balance = $20, but should be -$60 or one should fail!
```

### Example 3: Like Counter
```
Initial: post_likes = 100

User A clicks like: read 100, write 101
User B clicks like: read 100, write 101

Result: likes = 101, but should be 102 (lost update)
```

---

## Solution Approaches

## 1. Database-Level Solutions (Single Database)

### Pessimistic Locking (Exclusive Locks)

**How It Works**:
- Lock row/resource BEFORE reading
- Hold lock during entire transaction
- Other transactions wait for lock release
- Guarantees no conflicts

**SQL Implementation**:
```
BEGIN TRANSACTION;

SELECT stock FROM products 
WHERE product_id = 123 
FOR UPDATE;  -- Locks the row

-- Check if stock > 0
-- If yes, decrement stock
UPDATE products SET stock = stock - 1 
WHERE product_id = 123;

COMMIT;  -- Releases lock
```

**When to Use**:
- High contention expected
- Critical consistency requirements
- Short transaction duration
- Read-modify-write pattern

**How It Reduces Contention**:
- **Serializes access**: Only one transaction can modify the resource at a time
- **Prevents wasted work**: No retries needed, first transaction wins
- **Eliminates race conditions**: Lock ensures exclusive access to data
- **Database-managed queuing**: DB handles waiting transactions efficiently
- **Short critical section**: Fast operations minimize lock hold time

**Pros**:
- Prevents all conflicts
- Simpler logic (no retry needed)
- Data consistency guaranteed

**Cons**:
- Lower throughput (serialized access)
- Potential deadlocks
- Lock contention under high load
- Blocked transactions wait

**Include in Design**:
- "Use SELECT FOR UPDATE to lock inventory row"
- "Ensures only one user can purchase at a time"
- "Keep transaction short to minimize lock duration"
- "Add timeout to prevent indefinite waiting"

**Deadlock Prevention**:
- Always acquire locks in same order
- Set lock timeout
- Implement deadlock detection and retry

---

### Optimistic Locking (Version-Based)

**How It Works**:
- Read data with version number
- Modify data locally
- Write back ONLY if version unchanged
- If version changed, someone else modified → retry

**SQL Implementation**:
```
-- Read with version
SELECT stock, version FROM products 
WHERE product_id = 123;

-- stock = 5, version = 10

-- Try to update with version check
UPDATE products 
SET stock = 4, version = 11
WHERE product_id = 123 AND version = 10;

-- If rows_affected = 0, version changed → conflict → retry
-- If rows_affected = 1, success
```

**When to Use**:
- Low to moderate contention
- Can tolerate retries
- Long-running transactions
- Better concurrency than pessimistic

**How It Reduces Contention**:
- **Allows concurrent reads**: Multiple transactions can read simultaneously
- **No blocking**: Transactions don't wait for each other during read phase
- **Fail-fast detection**: Version check immediately identifies conflicts
- **Optimistic assumption**: Assumes conflicts are rare, maximizes throughput
- **Database doesn't hold locks**: Reduces pressure on lock manager

**Pros**:
- Higher concurrency
- No lock contention
- No deadlocks
- Better for read-heavy workloads

**Cons**:
- Requires retry logic
- More complex application code
- Can cause retry storms under high contention
- Wasted work if many retries

**Include in Design**:
- "Use version column for optimistic locking"
- "Client retries with exponential backoff if conflict"
- "Works well for low contention scenarios"
- "Max 3 retries before returning error"

**Retry Strategy**:
```
max_retries = 3
for attempt in 1..max_retries:
    try to update with version check
    if success: break
    if conflict: 
        sleep(exponential_backoff)
        re-read current state
    if attempt = max_retries:
        return error to user
```

---

### Atomic Operations

**How It Works**:
- Single atomic database operation
- No read-modify-write pattern
- Database ensures atomicity

**SQL Implementation**:
```
-- Direct atomic decrement
UPDATE products 
SET stock = stock - 1 
WHERE product_id = 123 AND stock > 0;

-- Check rows_affected
-- If 0, stock was 0 (failed)
-- If 1, success
```

**When to Use**:
- Simple increment/decrement
- Single column update
- No complex business logic needed

**How It Reduces Contention**:
- **Single operation**: No read-modify-write gap where conflicts occur
- **Database-level atomicity**: DB engine handles concurrency internally using latches
- **No transaction overhead**: No BEGIN/COMMIT needed, faster execution
- **Row-level execution**: Modern DBs use row versioning, minimal blocking
- **Shortest critical section**: Single UPDATE is fastest possible operation

**Pros**:
- Simplest approach
- Best performance
- No explicit locking
- Database handles concurrency

**Cons**:
- Limited to simple operations
- Can't check multiple conditions
- Less control over logic

**Include in Design**:
- "Use atomic UPDATE for stock decrement"
- "Single query, no transaction needed"
- "Simplest and most performant approach"
- "Check affected rows to know if succeeded"

---

### Database Transactions with Isolation Levels

**Isolation Levels** (from weakest to strongest):

**Read Uncommitted**:
- Can read uncommitted changes (dirty reads)
- Almost never used
- **Contention impact**: Minimal locks, maximum contention/conflicts

**Read Committed** (default in most DBs):
- Only see committed changes
- Non-repeatable reads possible
- **Contention impact**: Short read locks, moderate contention

**Repeatable Read**:
- Same read query returns same results within transaction
- Phantom reads possible
- **Contention impact**: Read locks held longer, more contention on reads

**Serializable** (strongest):
- Full isolation, as if transactions run serially
- Highest consistency, lowest concurrency
- **Contention impact**: Maximum locking, highest contention but zero conflicts

**Include in Design**:
- "Use Serializable isolation for critical transfers"
- "Use Repeatable Read for booking systems"
- "Trade-off: higher isolation = lower concurrency"

**Example - Bank Transfer**:
```
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;

SELECT balance FROM accounts WHERE id = A FOR UPDATE;
SELECT balance FROM accounts WHERE id = B FOR UPDATE;

-- Check A has sufficient funds
-- Deduct from A, add to B

UPDATE accounts SET balance = balance - 100 WHERE id = A;
UPDATE accounts SET balance = balance + 100 WHERE id = B;

COMMIT;
```

---

## 2. Application-Level Solutions

### In-Memory Locks (Single Server)

**How It Works**:
- Use mutex/semaphore in application
- Lock before accessing resource
- Release after operation

**When to Use**:
- Single server deployment
- Not distributed system
- Low traffic

**How It Reduces Contention**:
- **In-memory speed**: Nanosecond-level lock acquisition vs millisecond DB locks
- **No network overhead**: Lock is local to process, no network calls
- **OS-level primitives**: Uses CPU instructions (CAS) for lock management
- **Shared memory**: All threads see the same lock state instantly

**Pros**:
- Very fast (in-memory)
- Simple to implement

**Cons**:
- Only works on single server
- Doesn't scale horizontally
- Lost on server restart

**Not recommended for distributed systems**

---

### Distributed Locks

**How It Works**:
- Acquire lock from distributed system (Redis, Zookeeper)
- Hold lock during operation
- Release lock when done
- Other processes wait for lock

**Redis Implementation (Redlock)**:
```
SET resource_name my_unique_id NX PX 30000

NX = only set if not exists (acquire lock)
PX 30000 = expire after 30 seconds (auto-release)
my_unique_id = identifier to verify ownership

-- Perform operation

-- Release lock (if still owner)
if GET resource_name == my_unique_id:
    DEL resource_name
```

**When to Use**:
- Distributed system (multiple servers)
- Need coordination across servers
- Can't use database-level locks

**How It Reduces Contention**:
- **Cross-server coordination**: Prevents multiple servers from accessing same resource
- **Centralized arbitration**: Single Redis instance decides who gets the lock
- **Fast lock checks**: In-memory Redis operations are very fast (~1ms)
- **Auto-expiry**: Stuck locks don't block forever, system self-heals
- **Reduces DB pressure**: Moves lock management away from database

**Pros**:
- Works across multiple servers
- Fast (Redis is in-memory)
- Auto-expiry prevents stuck locks

**Cons**:
- Complex to implement correctly
- Network failures can cause issues
- Redlock has controversy (not 100% safe)
- Additional dependency (Redis, Zookeeper)

**Include in Design**:
- "Use Redis distributed lock for cross-server coordination"
- "Set lock timeout to prevent stuck locks"
- "Implement retry with exponential backoff"
- "Not for critical financial transactions (use DB instead)"

**Challenges**:
- **Lock timeout too short**: Process still running when lock expires
- **Lock timeout too long**: Stuck locks block others
- **Network partition**: Can create split-brain scenarios

---

### Two-Phase Commit (2PC)

**How It Works**:
- Coordinator asks all participants: "Can you commit?"
- All respond yes/no
- If all yes, coordinator says "Commit"
- If any no, coordinator says "Abort"

**Phases**:
```
Phase 1 - Prepare:
Coordinator → All participants: "Prepare to commit"
Participants → Coordinator: "Yes" or "No"

Phase 2 - Commit/Abort:
If all yes:
    Coordinator → All: "Commit"
Else:
    Coordinator → All: "Abort"
```

**When to Use**:
- Multiple databases involved
- All-or-nothing requirement
- Can tolerate blocking

**How It Reduces Contention**:
- **Coordination across systems**: Prevents partial updates across multiple databases
- **Locks on all participants**: Each DB locks its own resources during prepare phase
- **Atomic commitment**: All succeed or all fail, no inconsistent states
- **NOTE**: Doesn't actually reduce contention, it extends it across systems
- **Trade-off**: Sacrifices performance for consistency

**Pros**:
- Strong consistency across systems
- All-or-nothing guarantee

**Cons**:
- Blocking protocol (participants wait)
- Single point of failure (coordinator)
- Poor performance (high latency)
- Rarely used in practice

**Include in Design**:
- "Use 2PC for distributed transactions across databases"
- "Coordinator ensures all-or-nothing commit"
- "Be aware: blocking protocol, avoid if possible"

**Avoid 2PC when possible - prefer eventual consistency or sagas**

---

### Queue-Based Serialization

**How It Works**:
- All requests for resource go through single queue
- Single worker processes queue sequentially
- Eliminates contention by serialization

**Architecture**:
```
User A → Request → Queue → Single Worker → Resource
User B → Request → ↓
User C → Request → ↓
```

**When to Use**:
- High contention on specific resource
- Order of operations matters
- Can tolerate async processing

**How It Reduces Contention**:
- **Eliminates contention entirely**: Only one worker accesses resource, no conflicts possible
- **Sequential processing**: Inherently serialized, no race conditions
- **Moves contention upstream**: Contention happens in queue (writes), not on resource
- **Backpressure handling**: Queue depth signals system load
- **Predictable performance**: No lock waits, retries, or deadlocks

**Pros**:
- No locking needed
- Predictable ordering
- Simple to reason about

**Cons**:
- Bottleneck (single worker)
- Higher latency (async)
- Queue can grow large

**Include in Design**:
- "Serialize all inventory updates through queue"
- "Single worker processes sequentially"
- "Eliminates race conditions completely"
- "Trade latency for correctness"

---

## 3. Advanced Patterns

### Compare-and-Swap (CAS)

**How It Works**:
- Atomic operation: update only if current value matches expected
- Hardware-level support in many systems

**Pseudocode**:
```
expected = 5
new_value = 4
success = CAS(stock, expected, new_value)

-- Updates stock to 4 ONLY if current value is 5
-- Returns true/false
```

**When to Use**:
- Single-field updates
- Lock-free algorithms
- High-performance requirements

**How It Reduces Contention**:
- **Lock-free**: No blocking, all threads can attempt updates concurrently
- **Hardware-level atomicity**: CPU instruction ensures atomic swap
- **Immediate conflict detection**: Know instantly if update succeeded
- **No lock overhead**: No lock acquisition/release, just compare-and-swap
- **Optimistic concurrency**: Assumes success, retries on failure

**Pros**:
- Lock-free
- High performance
- No deadlocks

**Cons**:
- Complex logic
- Retry storms possible
- Limited to simple updates

---

### Saga Pattern (for Distributed Transactions)

**How It Works**:
- Break transaction into multiple steps
- Each step has compensating action
- If any step fails, run compensations to undo

**Example - E-commerce Order**:
```
Steps:
1. Reserve inventory → Compensation: Release inventory
2. Charge payment → Compensation: Refund payment
3. Create shipment → Compensation: Cancel shipment

If step 2 fails:
- Run compensation for step 1 (release inventory)
- Order fails gracefully
```

**When to Use**:
- Distributed systems
- Can't use 2PC
- Eventually consistent acceptable

**How It Reduces Contention**:
- **Non-blocking**: Each step commits independently, no long-held locks
- **Distributed load**: Each service handles its own part, no single bottleneck
- **Eventual consistency**: Allows concurrent processing without coordination
- **Asynchronous**: Steps don't block each other, higher throughput
- **NOTE**: Doesn't prevent contention on individual steps, but avoids cross-service locking

**Pros**:
- Non-blocking
- Better availability than 2PC
- Scales well

**Cons**:
- Complex to implement
- Eventually consistent (not immediate)
- Need to design compensations

**Include in Design**:
- "Use Saga pattern for order processing across services"
- "Each step has compensation transaction"
- "Choreography: events trigger next steps"
- "Orchestration: central coordinator manages flow"

---

## How to Choose the Right Approach

### Decision Tree:

**1. Single Database?**
- Yes → Use database-level solutions
- No → Need distributed coordination

**2. If Single Database:**

**High Contention (many conflicts)**:
- Use Pessimistic Locking (SELECT FOR UPDATE)
- Example: Concert ticket booking

**Low-Moderate Contention**:
- Use Optimistic Locking (version column)
- Example: User profile updates

**Simple increment/decrement**:
- Use Atomic Operations
- Example: Like counters, view counts

**Critical financial transactions**:
- Use Serializable Isolation + Pessimistic Locks
- Example: Bank transfers

**3. If Distributed System:**

**Can use eventual consistency**:
- Use Saga Pattern
- Example: Order processing

**Need strong consistency**:
- Avoid if possible (use single DB)
- If required: Distributed locks (Redis) or 2PC
- Be aware of complexity and failure modes

**High contention on specific resource**:
- Queue-based serialization
- Example: Flash sale on single product

---

## Complete Design Example: Concert Ticket Booking

### Requirements:
- 1000 tickets available
- 10,000 users trying to book simultaneously
- No overselling allowed
- Fast response time

### Design:

**Approach 1: Pessimistic Locking (Recommended)**

```
1. User clicks "Book"
2. API Server:

BEGIN TRANSACTION;

SELECT available_tickets FROM events 
WHERE event_id = 123 FOR UPDATE;

-- Locks the row

IF available_tickets > 0:
    UPDATE events 
    SET available_tickets = available_tickets - 1
    WHERE event_id = 123;
    
    INSERT INTO bookings (user_id, event_id, ticket_num);
    
    COMMIT;
    return success
ELSE:
    ROLLBACK;
    return sold_out

-- Keep transaction < 100ms
```

**Why this approach**:
- High contention (10K users, 1K tickets)
- Critical consistency (no overselling)
- Short transaction (fast DB operation)
- Simple logic (no retry complexity)

**Trade-off**:
- Serialized access (lower throughput)
- Users wait in queue
- But guarantees correctness

---

**Approach 2: Optimistic Locking + Retry**

```
1. User clicks "Book"
2. API Server:

max_retries = 3
for attempt in 1..max_retries:
    SELECT available_tickets, version 
    FROM events WHERE event_id = 123;
    
    IF available_tickets > 0:
        UPDATE events 
        SET available_tickets = available_tickets - 1,
            version = version + 1
        WHERE event_id = 123 AND version = current_version;
        
        IF rows_affected = 1:
            INSERT INTO bookings...;
            return success
        ELSE:
            -- Conflict, retry
            sleep(exponential_backoff)
    ELSE:
        return sold_out

return conflict_error (too many retries)
```

**Why might choose this**:
- Better concurrency than pessimistic
- Works for moderate contention

**Why might NOT choose**:
- Retry storms possible with 10K users
- More complex code
- Wasted work on conflicts

**For this scenario**: Pessimistic is better

---

**Approach 3: Queue-Based (Alternative)**

```
Architecture:
Users → Load Balancer → API Servers → Redis Queue → Single Booking Worker

Flow:
1. User clicks "Book"
2. API Server:
   - Push request to Redis queue
   - Return: "In queue, position: 1523"
3. Single Worker:
   - Process queue sequentially
   - Check availability
   - Book if available
   - Notify user (WebSocket/polling)

Guarantees:
- No race conditions (sequential processing)
- Fair queue (FIFO)
```

**Why might choose this**:
- Eliminates contention completely
- Fair ordering
- Can show queue position to users

**Why might NOT choose**:
- Higher latency (async)
- More complex (queue, workers, notifications)
- Single worker bottleneck

**For this scenario**: Overkill, but mention as option

---

## Common Interview Scenarios

### Scenario 1: "Design a system to handle flash sales"

**Answer**:
- "High contention on limited inventory"
- "Use pessimistic locking on inventory row"
- "Keep transaction under 100ms"
- "Scale horizontally: database handles serialization"
- "Monitor lock wait times"
- "Alternative: Queue all requests, process sequentially"

### Scenario 2: "Users can edit their profile, prevent lost updates"

**Answer**:
- "Low contention (user edits own profile)"
- "Use optimistic locking with version column"
- "If version conflict, show user: 'Profile changed, please refresh'"
- "Rare conflicts, simple retry on client side"

### Scenario 3: "Design auction bidding system"

**Answer**:
- "Multiple users bid on same item simultaneously"
- "Use pessimistic locking on auction item row"
- "Transaction: lock item, validate bid > current_bid, update"
- "Keep transaction short"
- "Alternative: Queue bids, single worker processes sequentially"

---

## Key Considerations

### When You Separate Data Into Multiple Databases

**CRITICAL Interview Point**:
- Single database handles contention well (ACID properties)
- When you shard/partition across multiple DBs, you lose ACID guarantees
- Distributed transactions are complex and slow (2PC)
- Prefer keeping contested data in single database

**What You're Giving Up**:
- Atomic transactions across shards
- Immediate consistency
- Simple locking mechanisms
- Database-managed concurrency

**What You Gain**:
- Horizontal scalability
- Higher throughput for independent operations

**Interviewer wants to know**: Do you understand this trade-off?

---

## Interview Tips

### Always Mention
1. **Start simple**: "Single database with atomic operations or pessimistic locking"
2. **Justify complexity**: Only add distributed locks/2PC if really needed
3. **Trade-offs**: Consistency vs availability vs performance
4. **Test**: "Would do load testing to validate approach"

### Deep Dive Topics
- **Isolation levels**: Know difference between Read Committed, Repeatable Read, Serializable
- **Deadlocks**: How they happen, how to prevent
- **Distributed locks**: Complexity, failure modes
- **CAP theorem**: Can't have all three (Consistency, Availability, Partition tolerance)

### Red Flags to Avoid
- Jumping to distributed locks without justification
- Not discussing isolation levels
- Ignoring deadlock possibility
- Premature sharding/distribution

### Good Answers Include
- "I'd use SELECT FOR UPDATE for high-contention booking"
- "Optimistic locking works here because conflicts are rare"
- "Keep contended data in single database to avoid distributed transaction complexity"
- "Use atomic UPDATE for simple counter increments"
- "Monitor lock wait times and conflict rates to validate approach"

---

## Example Use Cases Summary

| Use Case | Contention Level | Best Approach | Why |
|----------|------------------|---------------|-----|
| Concert tickets | Very High | Pessimistic lock | High contention, critical consistency |
| User profile edit | Very Low | Optimistic lock | Rare conflicts, can retry |
| Like counter | Medium | Atomic operation | Simple increment, performance critical |
| Bank transfer | N/A | Serializable + Pessimistic | Critical consistency, no errors allowed |
| Flash sale | Very High | Pessimistic or Queue | Limited inventory, high concurrent access |
| Auction bidding | High | Pessimistic lock | Need immediate consistency, fair ordering |
| Inventory across warehouses | Medium | Saga pattern | Distributed data, eventual consistency OK |
