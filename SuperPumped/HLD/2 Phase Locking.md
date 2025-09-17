- what is 2 Phase Locking
- why is it used
- explain everything one should know about this topic from interview perspective, include the following topics as well, feel free to make use of diagrams if it makes the understanding easier
- finish of with interview questions and answers

00:00 - Introduction
00:41 - Basic 2PL
08:56 - Deadlock, 1st Issues with Basic 2PL
14:30 - Timeout, Deadlock Prevention Strategy
16:30 - WFG(Wait for Graph), Deadlock Prevention Strategy
21:42 - Conservative 2PL, Deadlock Prevention Strategy
24:52 - TimeStamp, Deadlock Prevention Strategy
29:14 - Cascading Abort, 2nd Issue with Basic 2PL
32:37 - Rigorous or (Strong Strict 2PL), Cascading Abort Prevention Strategy
34:45 - Example of Each

---
# Introduction
- Two-Phase Locking (2PL) is a concurrency control protocol that ensures serializability of transactions in database systems. The core idea is that transactions acquire locks on data items before accessing them and release locks after they're done, but in a specific two-phase manner:
	1. **Growing Phase (Phase 1)**: The transaction acquires all the locks it needs but cannot release any locks.
	2. **Shrinking Phase (Phase 2)**: The transaction releases locks and cannot acquire any new locks.
- The key rule is: **once a transaction releases a lock, it cannot acquire any more locks**.
- This protocol divides transaction execution into these two distinct phases, hence the name "Two-Phase Locking."

![[Pasted image 20250429103902.png]]


### Types of Locks
- **Shared Lock (S-lock)**: Used for read operations. Multiple transactions can hold shared locks on the same data item simultaneously.
- **Exclusive Lock (X-lock)**: Used for write operations. Only one transaction can hold an exclusive lock on a data item at a time.

### Lock Compatibility Matrix
||S-lock|X-lock|
|---|---|---|
|S-lock|Yes|No|
|X-lock|No|No|

### Lock Point
- The point where a transaction acquires its final lock is called the **lock point**. This moment separates the growing phase from the shrinking phase and is critical in understanding 2PL.

---
# Deadlock and Prevention Strategies

- Deadlock occurs when two or more transactions are waiting indefinitely for each other to release locks. This creates a circular wait condition where no transaction can proceed.
### Deadlock Example:
```
Transaction T1:
1. Acquire lock on A
2. Acquire lock on B (waiting for T2)

Transaction T2:
1. Acquire lock on B
2. Acquire lock on A (waiting for T1)
```
- Both transactions are now waiting for each other, creating a deadlock.

---

## Deadlock Prevention Strategies

### 1. Timeout
- **How it works**: If a transaction waits for a lock longer than a specified timeout period, the system assumes a deadlock might have occurred and aborts the transaction.
- **Advantages**:
	- Simple to implement
	- Low overhead
- **Disadvantages**:
	- Might abort transactions unnecessarily
	- Difficult to determine optimal timeout value

### 2. Wait-For Graph (WFG)
- A Wait-For Graph is a directed graph where:
	- Nodes represent transactions
	- An edge from Ti to Tj means Ti is waiting for Tj to release a lock
- The system periodically checks for cycles in the WFG. If a cycle is found, it indicates a deadlock.
- **How to resolve**: Break the deadlock by aborting one of the transactions in the cycle. Selection strategies include:
	- Youngest transaction (least invested work)
	- Transaction with fewest locks
	- Transaction that would be least expensive to roll back
- **Advantages**:
	- Precise deadlock detection
	- Only aborts transactions when necessary
- **Disadvantages**:
	- Higher overhead due to graph maintenance
	- Deadlock detection is not instantaneous

### 3. Conservative 2PL
- Also known as **Static 2PL** or **Predeclaration Protocol**.
- **How it works**:
	- Transactions must declare all data items they will access before execution
	- The transaction acquires all necessary locks at the beginning
	- If any lock cannot be acquired, the transaction doesn't begin and waits
- **Advantages**:
	- Completely prevents deadlocks (no "wait-for" condition can occur)
	- Simple implementation
- **Disadvantages**:
	- Poor concurrency as locks are held longer than necessary
	- Requires knowing all data items in advance, which is often impractical
	- Reduces system throughput

### 4. Timestamp-Based Prevention
- **How it works**: Each transaction is assigned a unique timestamp when it begins. When lock conflicts occur, decisions are made based on timestamps:
	- **Wait-Die**: If an older transaction (smaller timestamp) requests a lock held by a younger transaction, it waits. If a younger transaction requests a lock held by an older transaction, it "dies" (aborts and restarts with the same timestamp).
	- **Wound-Wait**: If an older transaction requests a lock held by a younger transaction, it "wounds" (aborts) the younger one. If a younger transaction requests a lock held by an older transaction, it waits.
- **Advantages**:
	- Prevents deadlocks
	- Avoids starvation by using the original timestamp after restart
- **Disadvantages**:
	- May cause unnecessary aborts
	- Overhead of timestamp management
---
# Cascading Abort: Second Issue with Basic 2PL

- Cascading aborts occur when the abort of one transaction forces the abort of other dependent transactions. This happens when:
	1. Transaction T1 writes to data item X
	2. Transaction T2 reads X after T1's write but before T1 commits (this is possible since T1 has released the lock as it is in shrinking phase)
	3. T1 aborts (rolls back)
	4. T2 must also abort because it read an invalid value of X
- This creates a domino effect where one abort triggers multiple aborts, severely affecting database performance.

![[Pasted image 20250429103636.png]]

---
## Rigorous 2PL (Strong Strict 2PL) - Cascading Abort Prevention Strategy

![[Pasted image 20250429103829.png]]

- Rigorous 2PL is a stricter version of the basic 2PL protocol that prevents cascading aborts.
- **How it works**:
	- All locks (both shared and exclusive) are held until after the transaction either commits or aborts
	- No locks are released in the middle of a transaction
- **Advantages**:
	- Completely prevents cascading aborts
	- Produces strict schedules (easier recovery)
	- Ensures recoverability
- **Disadvantages**:
	- Reduced concurrency compared to basic 2PL
	- Locks are held longer

---
### Comparison

![[Pasted image 20250429104350.png]]

---

