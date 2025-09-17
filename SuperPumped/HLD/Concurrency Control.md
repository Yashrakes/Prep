# Introduction to Concurrency Control

## Key Challenges in Concurrency:

1. **Data Consistency** - Ensuring data remains valid despite multiple simultaneous operations
2. **Resource Utilization** - Maximizing throughput while maintaining correctness
3. **Deadlock Prevention** - Avoiding situations where processes are indefinitely blocked

---
## SYNCHRONIZED
- Synchronization is a fundamental concept in concurrent programming that helps control access to shared resources. In languages like Java, the `synchronized` keyword creates critical sections where only one thread can execute at a time.
- When a thread enters a synchronized block or method, it acquires a lock (also called a monitor) on the specified object. Other threads attempting to enter synchronized blocks protected by the same lock must wait until the first thread releases it.

---
## TRANSACTIONS
- A transaction is a logical unit of work that must be completed in its entirety or not at all. Transactions are crucial for maintaining database consistency.

### Key Properties of Transactions (ACID):
1. **Atomicity** - A transaction is all or nothing; either all operations complete successfully or none do
2. **Consistency** - A transaction transforms the database from one valid state to another
3. **Isolation** - Concurrent transactions shouldn't interfere with each other
4. **Durability** - Once committed, transaction effects persist even through system failures

---
## DATABASE LOCKING (Shared and Exclusive Locking)

- Database locking is a mechanism used to control concurrent access to data. It prevents multiple transactions from accessing or modifying the same data simultaneously in ways that could cause inconsistencies.

### Types of Locks:
1. **Shared (S) Lock** (Read Lock):
    - Multiple transactions can acquire shared locks on the same data
    - Allows concurrent reads but prevents writes while active
    - Used for read operations
2. **Exclusive (X) Lock** (Write Lock):
    - Only one transaction can hold an exclusive lock
    - Prevents both reads and writes from other transactions
    - Used for write operations
---
# Isolation Property and Isolation Levels

- The isolation property of ACID ensures that concurrent transactions execute as if they were running one after another (serially). However, perfect isolation can severely limit performance, so databases offer different isolation levels that trade some isolation guarantees for improved performance.

## Problems in Concurrent Transactions:
#### 1. DIRTY Read Problem
- A dirty read occurs when a transaction reads data that has been modified by another transaction that has not yet committed. If the modifying transaction rolls back, the reading transaction has seen data that never "officially" existed.
#### 2. NON-REPEATABLE Read Problem
- A non-repeatable read occurs when a transaction reads the same row twice and gets different results because another transaction has modified the row between the reads. This breaks the consistency of the reading transaction's view of the data.
#### 3. PHANTOM Read Problem
- A phantom read occurs when a transaction retrieves a set of rows twice, and the second retrieval finds new rows ("phantoms") that weren't visible in the first retrieval. This happens because another transaction inserted new rows that match the query criteria.

---
## Isolation Levels:

#### 1. READ UNCOMMITTED
- Transactions can see uncommitted changes made by other transactions
- No locking mechanisms are used.
- **Use Cases**:
	- Reporting scenarios where approximate results are acceptable
	- When maximum concurrency is needed and data consistency is less critical

#### 2. READ COMMITTED
- This is the **default isolation level** in many database systems (including PostgreSQL and SQL Server).
- Transactions only see committed changes from other transactions
- Read operations acquire and release locks immediately
- Write operations hold locks until the transaction completes

#### 3. REPEATABLE READ
- This level ensures that if a transaction reads a row, it will continue to see the same data for that row throughout the transaction.
- Read locks are held until transaction completion
- Prevents other transactions from modifying any rows that have been read
  
#### 4. SERIALIZABLE
- This is the highest isolation level, providing complete isolation from other transactions.
- Transactions execute as if they were running one after another
- Read ranges are locked, preventing insertions within query ranges
- Maximum protection against concurrency anomalies

![[Pasted image 20250427190741.png]]

#### Locking Strategies
![[Pasted image 20250427190824.png]]


---
# Concurrency Control Approaches

## Optimistic Concurrency Control (OCC)

- Optimistic concurrency control assumes that conflicts between transactions are rare. It allows transactions to proceed **without locking resources** and only checks for conflicts at commit time.
#### Key Phases of OCC:
1. **Read Phase**:
    - Transaction reads data and keeps track of read/write sets
    - No locks are acquired during this phase
2. **Validation Phase**:
    - Before committing, the system checks if any concurrent transaction has modified the data
    - Validates that no conflicts have occurred
3. **Write/Commit Phase**:
    - If validation succeeds, changes are made permanent
    - If validation fails, the transaction is aborted and must be restarted

#### Implementation Methods:
1. **Timestamp-based**: Each transaction is assigned a timestamp, and the system ensures order of execution matches timestamp order
2. **Version-based**: Each data item has a version number that increments with each update

#### Example of Version-based OCC:

```sql
-- Read the current state and version
SELECT balance, version FROM accounts WHERE id = 123;
-- Got balance = 1000, version = 5

-- Later, try to update with version check
UPDATE accounts 
SET balance = 900, version = version + 1 
WHERE id = 123 AND version = 5;

-- If no rows updated, version has changed, so transaction fails
IF @@ROWCOUNT = 0
    THROW 50000, 'Concurrency conflict detected', 1;
```

#### Benefits of OCC:
- No locking overhead for read operations
- Good performance when conflicts are rare
- No deadlocks
- Works well in distributed systems

#### Drawbacks of OCC:
- Wasted work if transactions are aborted
- Performance degrades under high contention
- Complex to implement correctly

---
## Pessimistic Concurrency Control (PCC)

- Pessimistic concurrency control assumes that conflicts are likely to occur and uses locks to prevent conflicts before they happen.

#### Key Characteristics:
1. **Lock Acquisition**:
    - Transactions acquire locks before accessing data
    - Locks prevent other transactions from accessing the same data in conflicting ways
2. **Lock Duration**:
    - Locks are held until the transaction completes (commits or aborts)
    - Longer lock duration increases correctness but reduces concurrency

#### Implementation Methods:
1. **Two-Phase Locking (2PL)**: The most common method of pessimistic concurrency control
    - Growing phase: Transaction acquires locks but cannot release any
    - Shrinking phase: Transaction releases locks but cannot acquire new ones
2. **Strict Two-Phase Locking (Strict 2PL)**:
    - All locks are held until transaction commits or aborts
    - Prevents cascading aborts

#### Example of Pessimistic Locking:
```sql
BEGIN TRANSACTION;

-- Acquire exclusive lock
SELECT * FROM accounts WITH (UPDLOCK, HOLDLOCK) WHERE id = 123;

-- Perform update
UPDATE accounts SET balance = balance - 100 WHERE id = 123;

-- Locks released only upon commit or rollback
COMMIT;
```

#### Benefits of PCC:
- Prevents conflicts before they occur
- Guarantees consistency even under high contention
- Familiar model that's well-understood

#### Drawbacks of PCC:
- Lower concurrency due to lock contention
- **Potential for deadlocks**
- Higher overhead for read operations

---
## OCC vs. PCC Choice Factors:
![[Pasted image 20250427190500.png]]

---
## Database-Specific Implementation Details:

1. **PostgreSQL**:
    - Uses Multi-Version Concurrency Control (MVCC)
    - Provides all standard isolation levels
    - Default: READ COMMITTED
2. **MySQL/InnoDB**:
    - Uses a combination of MVCC and traditional locking
    - Default: REPEATABLE READ
3. **SQL Server**:
    - Offers both pessimistic (default) and optimistic concurrency control
    - Default: READ COMMITTED
4. **Oracle**:
    - Uses MVCC with statement-level read consistency
    - Default: READ COMMITTED (called "READ CONSISTENCY" in Oracle)
---
# Multi-Version Concurrency Control (MVCC) - A Detailed Explanation

- MVCC is a concurrency control method used by most modern database systems that creates multiple versions of data to allow concurrent transactions to access the database without excessive locking.

### Core Concepts of MVCC
**1. Version Management**
- When a transaction modifies a row in the database:
	- The original version of the row is preserved
	- A new version of the row is created with the changes
	- Each version has metadata about which transactions can see it
- This approach means that different transactions can simultaneously see different versions of the same data, based on when they started.

**2. Transaction Snapshots**
- When a transaction begins:
	- It gets a "snapshot" of the database at that point in time
	- The snapshot defines which versions of rows are visible to this transaction
	- The transaction always sees a consistent view of data based on this snapshot

**3. Version Visibility Rules**
- For a version to be visible to a transaction:
	- It must have been committed before the transaction's snapshot was taken
	- It must not have been deleted by a transaction that committed before the snapshot was taken
	- If multiple versions exist, the transaction sees the most recent one that meets these criteria

### MVCC Implementation Example (PostgreSQL)
- In PostgreSQL, each row version (called a tuple) contains:
	- **xmin**: The ID of the transaction that created this version
	- **xmax**: The ID of the transaction that deleted/updated this version (or null if still valid)
	- **The actual data** of the row.
- Let's see how this works with a concrete example:
- Imagine a table with a single row where a value = 10:
1. **Initial state**:
    ```
    value = 10, xmin = 50, xmax = null
    ```
2. **Transaction 100 updates the value to 20**:
    ```
    value = 10, xmin = 50, xmax = 100  (old version)
    value = 20, xmin = 100, xmax = null (new version)
    ```
3. **Transaction 200 starts and reads the value**:
    - If Transaction 100 has committed: sees value = 20
    - If Transaction 100 is still running: sees value = 10
    - If Transaction 100 rolled back: sees value = 10 and the new version is removed
4. **Transaction 100 commits successfully**:
    - Future transactions will see value = 20
    - Existing transactions behave according to their isolation level:
        - READ COMMITTED: will see value = 20 in subsequent reads
        - REPEATABLE READ/SERIALIZABLE: will continue to see value = 10

### Key Differences from Traditional Locking
- In traditional locking systems:
	- Readers block writers (shared locks prevent exclusive locks)
	- Writers block readers (exclusive locks prevent shared locks)
- In MVCC:
	- Readers never block writers (they see appropriate versions)
	- Writers never block readers (they create new versions)
	- Writers still block other writers on the same data

### Advantages of MVCC
1. **Improved Read Concurrency**: Readers don't block writers and vice versa
2. **Consistent Snapshots**: Each transaction sees a consistent view of the database
3. **Reduced Lock Contention**: Fewer locks means better scalability
4. **No Read Locks**: Read operations don't acquire locks, improving performance

### Disadvantages of MVCC
1. **Storage Overhead**: Multiple versions require more storage
2. **Cleanup Complexity**: Garbage collection of old versions adds complexity
3. **Implementation Complexity**: More complex than traditional locking schemes
4. **Write Conflicts**: Still requires conflict resolution for concurrent writes

### MVCC and Isolation Levels
Each isolation level has a specific implementation in MVCC systems:
1. **READ UNCOMMITTED**: Rarely implemented distinctly in MVCC systems; usually identical to READ COMMITTED
2. **READ COMMITTED**: Each SQL statement sees data committed before it began
3. **REPEATABLE READ**: The entire transaction sees data committed before it began
4. **SERIALIZABLE**: Adds additional checks to detect write-write, read-write conflicts that would violate serializability

---
# Interview Questions

### **Q: Explain the difference between optimistic and pessimistic concurrency control.**

Pessimistic concurrency control assumes conflicts will occur frequently and prevents them proactively:

- Acquires locks before accessing data
- Prevents other transactions from accessing the data in conflicting ways
- Guarantees no conflicts but reduces concurrency
- Works well in high-contention environments
- Example: Two-phase locking in traditional databases

Optimistic concurrency control assumes conflicts are rare and handles them reactively:

- Allows transactions to proceed without locking
- Checks for conflicts only at commit time
- Aborts and restarts transactions if conflicts are detected
- Provides higher concurrency but wastes work when conflicts occur
- Works well in low-contention environments
- Example: Version-based concurrency control in many web applications

The fundamental difference is in philosophy: pessimistic approaches prevent conflicts before they happen, while optimistic approaches detect conflicts after they happen and resolve them.

---
### **Q: What isolation level would you choose for a report-generation system that needs to read consistent data but doesn't need to be absolutely up-to-date?**

For a report-generation system that needs consistent data but not necessarily the most current data, I would choose REPEATABLE READ isolation level.

Rationale:

- REPEATABLE READ ensures that if the report reads the same data multiple times during its execution, it will always see the same values
- This prevents non-repeatable reads, which could lead to inconsistent report results
- It's less restrictive than SERIALIZABLE, so it provides better performance
- It's more consistent than READ COMMITTED, which could show different data for the same query within a transaction
- Reports typically need internal consistency more than they need absolute freshness

In an MVCC system like PostgreSQL, this would create a snapshot of the database at the start of the report generation transaction, ensuring consistent results throughout the entire report without blocking writers.

---
### Scenario-Based Questions

### **Q: You're designing a banking system where account balances must be consistent. What isolation level and concurrency control approach would you use for fund transfers?**

For a banking system handling fund transfers, I would implement:

1. **Isolation Level**: SERIALIZABLE
    - Prevents all concurrency anomalies (dirty reads, non-repeatable reads, phantom reads)
    - Ensures complete transaction isolation
    - Critical for financial integrity where every dollar must be accounted for
2. **Concurrency Control**: Pessimistic with two-phase locking
    - Acquires exclusive locks on both source and destination accounts before any modification
    - Holds locks until transaction completion
    - Prevents race conditions that could lead to incorrect balances
3. **Implementation Details**:
    - Sort account IDs and always lock in the same order to prevent deadlocks
    - Use stored procedures for transfers to ensure consistent locking behavior
    - Implement appropriate timeouts to prevent indefinite blocking
    - Include robust error handling and logging
4. **Additional Safety Measures**:
    - Maintain a separate transaction log for auditing
    - Implement reconciliation processes
    - Use constraints to prevent negative balances

While this approach reduces concurrency, the data integrity guarantees are worth the performance trade-off for financial transactions where correctness is non-negotiable.

---
### **Q: Your e-commerce website shows product inventory to customers. During checkout, you need to verify that items are still in stock. What concurrency control approach would you use?**

For an e-commerce inventory system, I would implement:

1. **Separation of Concerns**:
    - Display inventory: READ COMMITTED isolation with MVCC
    - Checkout process: Optimistic concurrency control
2. **Display Phase**:
    - Show current approximate inventory to users browsing products
    - Accept that this view may be slightly out of date
    - No locks needed, maximizing read performance
3. **Checkout Process**:
    - When user adds items to cart: No inventory reservation yet
    - When user initiates checkout: Verify items are in stock using current data
    - At final payment confirmation: Use optimistic concurrency control
        
        sql
        
        ```sql
        BEGIN TRANSACTION;
        -- Get current inventory with version/timestamp
        SELECT inventory, version FROM products WHERE id = ?;
        
        -- Business logic to check if enough inventory
        
        -- Try to update with version check
        UPDATE products 
        SET inventory = inventory - ?, version = version + 1
        WHERE id = ? AND version = ?;
        
        -- If 0 rows affected, conflict occurred
        IF @@ROWCOUNT = 0 THEN
          ROLLBACK;
          -- Handle conflict (notify user, retry, etc.)
        ELSE
          COMMIT;
        END IF;
        ```
        
4. **Additional Strategies**:
    - Implement short-term (e.g., 15-minute) cart reservations for high-demand items
    - Use inventory buffers for popular products
    - Implement graceful conflict resolution (suggest alternatives, waitlists)

This approach maximizes concurrency while still preventing overselling, and handles the common e-commerce pattern where many users browse but relatively few complete purchases.

---
### **Q: You're building a collaborative document editing system. How would you handle concurrent edits to the same document?**

For a collaborative document editing system, I would implement:

1. **Operational Transformation (OT) or Conflict-free Replicated Data Types (CRDTs)**:
    - Instead of traditional database transactions, use specialized algorithms designed for collaborative editing
    - Each edit is represented as an operation that can be transformed against other operations
    - Operations are designed to be commutative and idempotent
2. **Implementation Approach**:
    - Each user works on a local copy of the document
    - Edits are transmitted as operations (not complete document states)
    - Server maintains the canonical document state and transformation history
    - When server receives an operation:
        1. Transform it against all concurrent operations
        2. Apply it to the canonical document
        3. Broadcast the transformed operation to all other clients
    - When client receives an operation:
        1. Transform it against any pending local operations
        2. Apply it to the local document model
        3. Update the UI
3. **Versioning and Conflict Resolution**:
    - Use vector clocks to track causal relationships between edits
    - Implement automatic merging for non-conflicting concurrent edits
    - Provide UI for resolving conflicts that can't be merged automatically
    - Maintain an edit history for auditing and rollbacks
4. **Practical Considerations**:
    - Use WebSockets or similar for real-time communication
    - Implement presence indicators showing who's editing
    - Provide cursor/selection tracking to show others' positions
    - Consider section-based locking for large documents

This approach avoids traditional database locking entirely, instead using algorithms specifically designed for collaborative editing scenarios that maximize concurrent work while ensuring consistency.

---
## Advanced Technical Questions

**Q: How does MVCC handle the phantom read problem? Does it prevent phantom reads by default?**

MVCC alone does not prevent phantom reads by default. Here's why and how it's addressed:

**Why MVCC Doesn't Prevent Phantom Reads**:

- MVCC provides row-level versioning, but phantom reads involve rows that don't exist yet when the transaction starts
- When a transaction reads a range of data (e.g., `WHERE price < 100`), MVCC ensures it sees consistent versions of existing rows
- However, if another transaction inserts new rows that match the predicate, these "phantom" rows might appear in subsequent range queries

**How Different Databases Handle This**:

1. **PostgreSQL**:
    - At REPEATABLE READ: Does not prevent phantom reads by default
    - At SERIALIZABLE: Uses predicate locks and detects read-write conflicts on ranges
    - If a serialization anomaly is detected, one transaction will be aborted with a serialization failure
2. **Oracle**:
    - Uses statement-level read consistency, which doesn't prevent phantom reads
    - Only prevents phantom reads at SERIALIZABLE isolation
3. **SQL Server**:
    - In SNAPSHOT isolation: Does not prevent phantom reads
    - In SERIALIZABLE: Uses key-range locks to prevent phantom reads

**Advanced MVCC Techniques to Prevent Phantom Reads**:

1. **Predicate Locking**: Conceptually locks all rows that match a predicate, including future rows
    - Theoretical but rarely implemented directly due to performance issues
2. **Index-Range Locking**: Locks ranges in indexes rather than individual rows
    - More practical implementation of predicate locking concept
3. **Serialization Graph Testing**: Tracks dependencies between transactions and aborts those that would create cycles in the dependency graph
    - Used in PostgreSQL's SERIALIZABLE isolation
4. **First-Committer-Wins Strategy**: If two transactions would create a serialization anomaly, the first to commit succeeds and the second is aborted
    - Implementation detail of serializable snapshot isolation

The bottom line is that MVCC needs additional mechanisms beyond basic versioning to handle phantom reads, typically only activated at SERIALIZABLE isolation level, with corresponding performance impacts.

---
### **Q: What is the difference between two-phase locking (2PL) and two-phase commit (2PC)?**

Two-phase locking (2PL) and two-phase commit (2PC) are distinct concepts that address different aspects of transaction processing:

**Two-Phase Locking (2PL)**:

- **Purpose**: Concurrency control protocol to ensure serializability within a single database
- **Participants**: A single transaction acquiring multiple locks
- **Phases**:
    1. **Growing Phase**: Transaction acquires locks but doesn't release any
    2. **Shrinking Phase**: Transaction releases locks but doesn't acquire new ones
- **Variants**:
    - **Strict 2PL**: Holds all locks until commit/abort
    - **Strong Strict 2PL (Rigorous)**: Releases write locks only at commit/abort
- **Goal**: Prevent concurrency anomalies
- **Scope**: Local to a single database system

**Two-Phase Commit (2PC)**:

- **Purpose**: Atomic commitment protocol to ensure transaction atomicity across multiple systems
- **Participants**: Multiple database systems (or resource managers) participating in a distributed transaction
- **Phases**:
    1. **Prepare Phase**: Coordinator asks all participants if they can commit
    2. **Commit Phase**: If all say yes, coordinator tells all to commit; if any says no, all abort
- **Key Components**:
    - **Coordinator**: Orchestrates the commit process
    - **Participants**: Individual systems involved in the transaction
    - **Transaction Manager**: Tracks the global transaction state
- **Goal**: Ensure all-or-nothing property across distributed systems
- **Scope**: Spans multiple independent systems

**Key Differences**:

1. **Problem Addressed**: 2PL addresses isolation, 2PC addresses atomicity
2. **Scale**: 2PL is local to one database, 2PC works across distributed systems
3. **Timing**: 2PL operates throughout transaction execution, 2PC activates at commit time
4. **Failure Handling**: 2PL handles deadlocks, 2PC handles system failures

**Relationship**: In a distributed database using traditional locking, both would be used together:

- 2PL ensures serializability within each database node
- 2PC ensures atomicity across all participating database nodes

---
### **Q: How would you implement deadlock detection and resolution in a database system?**

A comprehensive deadlock detection and resolution system for a database would include:

**1. Deadlock Detection Algorithm**:

- Construct a wait-for graph where:
    - Nodes represent transactions
    - Directed edges represent "transaction A is waiting for transaction B"
- Periodically scan for cycles in the graph (typically every few seconds)
- A cycle indicates a deadlock situation

**2. Implementation Details**:

```
function detectDeadlocks():
    // Build wait-for graph from current lock state
    graph = new DirectedGraph()
    
    for each transaction T:
        for each lock L held by T:
            for each transaction W waiting for lock L:
                graph.addEdge(W, T)  // W is waiting for T
    
    // Check for cycles using depth-first search
    cycles = findCycles(graph)
    
    for each cycle in cycles:
        resolveDeadlock(cycle)
```

**3. Deadlock Resolution Strategy**:

- Select a victim transaction to abort based on criteria such as:
    - Transaction age (youngest first)
    - Transaction complexity (least work done)
    - Priority (lowest priority first)
    - Number of locks held (fewest locks)
    - Cost of rollback (lowest cost)
- Roll back the victim transaction entirely
- Release all locks held by the victim
- Notify the application about the deadlock
- Allow the victim to restart

**4. Deadlock Prevention Measures**:

- Timeout mechanism: Abort transactions that wait longer than a threshold
- Lock ordering: Always acquire locks in a consistent global order
- Lock escalation: Reduce lock granularity when a transaction holds many locks
- Preemptive scheduling: Allow lock requests to preempt existing locks based on priority

**5. Monitoring and Tuning**:

- Track deadlock frequency and patterns
- Identify transactions commonly involved in deadlocks
- Adjust deadlock detection interval based on system load
- Log detailed information about each deadlock for analysis

**6. Advanced Techniques**:

- Incremental deadlock detection for large systems
- Distributed deadlock detection for clustered databases
- Wait-die and wound-wait schemes for prioritized transactions

**Key Considerations**:

- Balance detection frequency against overhead
- Ensure deterministic victim selection for predictable behavior
- Provide clear feedback to applications about deadlocks
- Consider application-level retry logic for deadlocked transactions

This approach provides reliable deadlock detection while minimizing the performance impact of the detection process itself.

---
## System Design Scenarios

### **Q: Design a ticket booking system that prevents overselling while maintaining high concurrency.**

**Ticket Booking System Design**

**1. Core Architecture**:

- Microservices architecture with dedicated services for:
    - Inventory management
    - Reservation processing
    - Payment processing
    - User management
- Event-driven architecture with message queues for asynchronous operations
- Read replicas for browsing and reporting

**2. Data Model**:

```
Events
  - event_id, name, venue, date, etc.

Seats
  - seat_id, event_id, section, row, number, price, status

Reservations
  - reservation_id, user_id, created_at, expires_at, status

ReservationItems
  - reservation_item_id, reservation_id, seat_id

Orders
  - order_id, user_id, reservation_id, status, payment_status
```

**3. Concurrency Control Strategy**:

**For Browsing (Read-Heavy)**:

- READ COMMITTED isolation level
- MVCC for maximum read concurrency
- Cached seat maps with TTLs of 5-10 seconds
- Clearly indicate "X people looking at these seats" for popular sections

**For Temporary Reservations**:

- Optimistic concurrency control with version numbers
- Time-limited reservations (typically 5-15 minutes)
- Status field in Seats table (Available, Reserved, Sold)

sql

```sql
UPDATE Seats SET 
  status = 'Reserved', 
  version = version + 1,
  reserved_at = NOW(),
  reservation_id = ?
WHERE seat_id IN (?, ?, ?) 
  AND status = 'Available' 
  AND version = ?
```

**For Final Booking (Purchase)**:

- Pessimistic locking during payment processing
- SERIALIZABLE isolation level for the final transaction
- Two-phase commit if payment is handled by an external system

**4. Handling High-Demand Events**:

- Virtual waiting room for extremely high-demand events
- Queue system to control concurrency
- Batch processing of reservation requests
- Rate limiting per user
- Gradual release of inventory in waves

**5. Resilience Mechanisms**:

- Automatic reservation expiration using a scheduled job
- Idempotent APIs for retry safety
- Distributed cache for session storage
- Circuit breakers for dependent services

**6. Additional Features**:

- Real-time seat status using WebSockets
- Conflict resolution UI ("Sorry, these seats were just taken. Here are alternatives...")
- Analytics to identify booking patterns and optimize performance
- Fraud detection system to prevent scalping

This design achieves high concurrency while preventing overselling by using a combination of optimistic concurrency for browsing and temporary reservations, with more strict controls for final purchases.

---
### **Q: How would you design a high-throughput financial trading system with consistent order execution?**

**High-Throughput Financial Trading System Design**

**1. System Architecture**:

- **Core Components**:
    - Order Gateway: Validates and normalizes incoming orders
    - Matching Engine: Matches buy/sell orders
    - Risk Management System: Pre-trade risk checks
    - Market Data Processor: Handles price feeds
    - Settlement System: Post-trade processing
    - Historical Data Store: Analytics and compliance
- **Physical Architecture**:
    - Co-location with exchanges for minimal latency
    - Multi-region deployment for regulatory compliance
    - Dedicated networks with redundant paths

**2. Concurrency Control Strategy**:

- **Order Processing Pipeline**:
    - Single-threaded matching engine per instrument for deterministic execution
    - LMAX Disruptor pattern for inter-thread communication
    - Command pattern with sequenced execution
- **Isolation Levels**:
    - SERIALIZABLE for order execution and matching
    - REPEATABLE READ for risk calculations
    - READ COMMITTED for reporting and analytics
- **Memory Model**:
    - In-memory order books with disk-based backup
    - Memory-mapped files for persistence
    - Lock-free data structures where possible

**3. Data Consistency Approach**:

- **Write Path**:
    - Write-ahead logging (WAL) for durability
    - Synchronous replication to backup instances
    - Checkpointing for recovery
- **Read Path**:
    - MVCC for read-heavy operations
    - Snapshots for consistent point-in-time views
    - Materialized views for complex analytics

**4. Performance Optimization**:

- **Low Latency Techniques**:
    - Custom memory allocators to avoid garbage collection
    - CPU pinning and NUMA awareness
    - Kernel bypass networking (DPDK)
    - Hardware acceleration for critical paths
- **High Throughput Techniques**:
    - Batching for efficiency at high volumes
    - Predictive scaling based on market volatility
    - Data partitioning by instrument
    - Tiered storage based on access patterns

**5. Failure Handling**:

- **Fault Tolerance**:
    - Active-active deployment with deterministic processing
    - Automatic failover with session migration
    - Circuit breakers for external dependencies
    - Chaos engineering practices
- **Recovery Mechanisms**:
    - Point-in-time recovery capabilities
    - Transaction replay from logs
    - State reconstruction from snapshots
    - Reconciliation processes

**6. Monitoring and Control**:

- **Real-time Monitoring**:
    - Nanosecond-precision latency tracking
    - Order flow visualization
    - Circuit breaker status dashboard
    - Risk exposure heat maps
- **Operational Controls**:
    - Kill switches by market/instrument
    - Trading velocity controls
    - Position limits enforcement
    - Market condition-based throttling

This design balances the need for extremely low latency and high throughput with the strict consistency requirements of financial transactions, using a combination of in-memory processing, careful concurrency control, and robust durability mechanisms.

---

### **Q: Design a social media feed system that handles concurrent updates and reads efficiently.**

**Social Media Feed System Design**

**1. System Architecture**:

- **Microservices**:
    - Post Service: Handles creation and storage of posts
    - Graph Service: Manages follower relationships
    - Feed Service: Generates and serves personalized feeds
    - Notification Service: Alerts users to relevant activity
    - Media Service: Handles photos, videos, and other media
- **Data Stores**:
    - Posts: Distributed document store (MongoDB/Cassandra)
    - Relationships: Graph database (Neo4j) or specialized service
    - Feeds: In-memory caches with database backup
    - Media: Object storage with CDN integration

**2. Feed Generation Strategies**:

- **Hybrid Push-Pull Model**:
    - Push: Fan-out writes to pre-computed feeds for high-value users
    - Pull: On-demand feed generation for casual users
    - Adaptive approach based on user activity levels

**3. Concurrency Control Approach**:

- **For Posts (Write Path)**:
    - Append-only model for base content
    - Optimistic concurrency for edits with version stamps
    - READ COMMITTED isolation for most operations
    - Eventually consistent model across regions
- **For Feeds (Read Path)**:
    - MVCC with READ COMMITTED for maximum read throughput
    - Cache-based architecture with stale-while-revalidate pattern
    - Pagination tokens for consistent paging during updates
    - Separate request paths for initial load vs. refresh

**4. Data Flow and Processing**:

- **Post Creation Flow**:
    1. User creates post → Post Service stores post
    2. Post ID published to event bus
    3. Fan-out service consumes events
    4. For each follower:
        - Add post ID to feed cache if high-activity user
        - Mark feed cache as stale if low-activity user
    5. Notification service determines which followers to notify
- **Feed Reading Flow**:
    1. User requests feed → Feed Service receives request
    2. Check user-specific feed cache
    3. If valid: Return cached feed items
    4. If stale/missing:
        - Query Graph Service for followed accounts
        - Query Post Service for recent posts from those accounts
        - Apply ranking algorithms
        - Cache results
        - Return to user

**5. Handling Edge Cases**:

- **Viral Content**:
    - Content popularity detection
    - Dynamic caching policies for viral posts
    - CDN integration for media
    - Circuit breakers to protect backend services
- **Consistency Challenges**:
    - Accept eventual consistency for follower counts
    - Use logical timestamps for ordering posts
    - Implement reconciliation processes for lost events
    - Provide "refresh" option for users to force consistency

**6. Performance Optimizations**:

- **Caching Strategy**:
    - Multi-level caching (application, distributed cache, CDN)
    - Content-aware cache expiration
    - Predictive prefetching for active users
    - Partial cache invalidation
- **Read Optimizations**:
    - Denormalized data structures for feeds
    - Feed windowing (only maintain recent content in hot storage)
    - Background refreshing of stale caches
    - Read replicas for high-traffic regions
- **Write Optimizations**:
    - Batched writes to databases
    - Asynchronous processing of non-critical updates
    - Write-behind caches
    - Database sharding by user ID

This design prioritizes read performance (as feed reads vastly outnumber posts) while ensuring sufficient consistency for a good user experience. The hybrid push-pull model adapts to user behavior patterns, and the eventually consistent approach allows for high scalability while maintaining apparent correctness from the user perspective.

---
### **Q: In mvcc, whats the point of maintaining snaphot if all transactions are not able to see latest data?**

The question you've asked gets right to the heart of the purpose of MVCC! The key insight is that MVCC isn't about preventing transactions from seeing the latest data—it's about ensuring each transaction sees a _consistent_ view of the database throughout its execution.

#### Why Snapshots Matter in MVCC

In MVCC, the snapshot mechanism serves several critical purposes:
##### 1. Consistent Read View
- When a transaction starts, it needs a consistent point-in-time view of the database that doesn't change as other transactions make modifications. This consistency is crucial for making correct decisions based on the data.
- Think about writing a report that calculates monthly sales totals. If you started with January data showing $100,000 in sales, then halfway through your calculation another transaction updated January to $95,000, your report would contain inconsistent data. The snapshot ensures you see a consistent state throughout your transaction.

##### 2. Non-Blocking Reads
Without snapshots, reading data would require acquiring locks that block writers, severely limiting concurrency. With MVCC snapshots:
- Transaction A can read data
- Transaction B can simultaneously write to that same data
- Transaction A continues seeing its consistent snapshot
- New transactions can see Transaction B's changes once committed

##### 3. Isolation Level Implementation
Different isolation levels are largely implemented through how snapshots work:
- In READ COMMITTED: A new snapshot is taken at the start of each statement
- In REPEATABLE READ: One snapshot is taken at the start of the transaction
- In SERIALIZABLE: The snapshot mechanism is enhanced with additional checks for serialization anomalies

#### Real-World Analogy
Think of database snapshots like taking a photograph of a whiteboard in a meeting room:
- When you enter the room, you take a photo of the whiteboard (your snapshot)
- Other people may come in and modify the whiteboard (concurrent transactions)
- You can still refer to your photo to make consistent decisions
- When you finish your work and leave the room, you can take a new photo if needed (start a new transaction)

#### Is "Not Seeing Latest Data" Actually a Problem?
What might seem like a limitation—not always seeing the latest data—is actually a _feature_ that enables both consistency and concurrency. Here's why:
1. **Transaction Correctness**: For a single transaction to execute correctly, it needs data that doesn't shift under its feet.
2. **Serializable Behavior**: The goal of transaction isolation is to make concurrent transactions appear as if they ran one after another (serially). If every transaction always saw the latest changes, that would violate this principle.
3. **Read-Write Concurrency**: By allowing readers to see a consistent snapshot while writers create new versions, MVCC dramatically increases throughput compared to lock-based systems.

---
