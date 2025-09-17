# The Dual Write Problem: A Complete Guide

## What is the Dual Write Problem?
- The Dual Write Problem is a fundamental challenge in distributed systems that occurs when a component needs to persist a change across two different systems simultaneously, such as writing to a database and publishing an event to a message broker like Kafka. The core challenge lies in ensuring both operations succeed together or fail together, maintaining consistency across both systems.

---
## The Traditional Approach: Two-Phase Commit (2PC)

### How 2PC Works
- The first solution that comes to mind is the Two-Phase Commit protocol, a classical distributed transaction approach. In 2PC, there's a central coordinator that orchestrates the transaction across multiple participants through two distinct phases.
- In the **Prepare Phase**, the coordinator sends a "Prepare" message to all participant services and waits for acknowledgments. Each participant performs all the work needed for the transaction but doesn't commit yet. They respond with either "Yes" (ready to commit) or "No" (cannot commit).
- In the **Commit Phase**, if all participants voted "Yes," the coordinator sends a "Commit" message to all participants. If any participant voted "No," the coordinator sends an "Abort" message instead. Participants then either commit or rollback their local transactions accordingly.

### Why 2PC Fails for Dual Write Scenarios
- Despite its theoretical elegance, 2PC is not a viable solution for the Dual Write Problem in modern distributed systems for several critical reasons.
	- **Heterogeneous System Incompatibility**: Dual write scenarios often involve heterogeneous systems where components may not support 2PC. Many message queues and brokers don't implement the 2PC protocol, making it impossible to include them in a distributed transaction.
	- **Performance and Latency Concerns**
	- **Coordinator Failure Risk**
	- **Blocking Protocol Nature**: 2PC is a blocking protocol, meaning that if any participant becomes unavailable, the entire transaction is blocked until that participant recovers or times out. This severely impacts system availability.

---
# Event-Driven Architecture Patterns: The Modern Solution

## Pattern 1: Transactional Outbox Pattern

### Core Concept
- Instead of writing to both the database and publishing an event simultaneously, the pattern writes both the business data and the event information to the same database within a single transaction.
- The pattern introduces an "outbox" table in the same database where business data is stored. When a business operation occurs, the application writes the business data to its respective table and simultaneously writes event information to the outbox table, all within a single database transaction. This ensures that both the business data and the event record are consistent with each other.
- A separate background process, often called a "poller," continuously reads from the outbox table and publishes these events to the message broker. Once events are successfully published, they're removed from the outbox table.

![[Pasted image 20250522170736.png]]

### Key Challenges and Solutions
- **Poller Service Complexity**: Implementing a reliable poller service requires careful consideration of failure scenarios, retry logic, and monitoring. The poller must handle broker outages, network failures, and its own restarts gracefully.
- **Event Publishing Delays**: There's an inherent delay between when business data is written and when events are published. This delay depends on the poller's frequency and the current load. For time-sensitive operations, this delay might be problematic.
- **Idempotency Concerns**: The poller might publish the same event multiple times due to failures or restarts. 
	- To handle this, Kafka producers should enable idempotency by setting `enable.idempotency=true`. 
	- Kafka then assigns a unique producer ID to each producer and adds monotonically increasing sequence numbers to messages. 
	- Duplicate messages with the same producer ID and sequence number are automatically discarded.
- **Multiple Poller Coordination**: If multiple poller instances run for scalability, they might pick up and publish the same events, creating duplicates. Since different pollers get different producer IDs, Kafka's built-in idempotency won't help. 
	- Consumer-side idempotency handling becomes necessary, typically implemented by maintaining a cache of processed event IDs.
- **Event Ordering Challenges**: When multiple pollers process events in parallel, events might be published out of order. Kafka only guarantees ordering within a single partition when there's one producer. With multiple producers and partitions, ordering isn't guaranteed. 
	- For use cases requiring strict ordering, Kafka Streams can be employed to process events in order while maintaining parallelism.
- **Failed Event Handling**: When the poller cannot publish events to the message broker, these events shouldn't be lost. 
	- Implement exponential backoff with jitter for retry mechanisms. 
	- After exhausting retries, move failed events to a separate failed events table or mark them with a failed status in the outbox table for manual intervention.
- **Outbox Table Growth**: The outbox table can grow rapidly in high-throughput systems. 
	- Implement regular cleanup processes that remove successfully published events, either immediately after successful publication or through batch cleanup jobs during low-traffic periods.

---
## Pattern 2: Listen to Yourself Pattern

### Core Concept
- The Listen to Yourself Pattern is a variation of the Transactional Outbox Pattern that further embraces eventual consistency. 
- In this pattern, the application doesn't directly write business data to the database. Instead, it only writes events to the outbox table. 
- The application then listens to its own published events and updates the database based on these events.
- This pattern transforms the traditional "write then notify" flow into a "notify then write" flow, making the event the single source of truth for what operations should be performed.

![[Pasted image 20250522171214.png]]


### Implementation Flow
Using our order creation example:
1. **Event-Only Write**: When processing an order creation request, the application writes only an "OrderCreationRequested" event to the outbox table within a transaction.
2. **Event Publishing**: The poller publishes this event to the message broker as in the standard outbox pattern.
3. **Self-Consumption**: The same application (or a component within it) consumes the "OrderCreationRequested" event and then creates the order record in the database.
4. **Confirmation Event**: Optionally, the application can publish an "OrderCreated" event after successfully processing the original event.

### Unique Challenges
- **Read-Before-Write Inconsistency**: This pattern introduces a significant challenge. If a GET request arrives after the event is written to the outbox but before it's processed and the database is updated, the GET request won't find the expected data in the database.
- **Write-Through Cache Solution**: To address this, implement a write-through cache strategy. 
	- When writing the initial event, also write the expected data to a cache. 
	- When GET requests arrive, they first check the cache before querying the database. This ensures that even if the event hasn't been processed yet, the GET request can be fulfilled from the cache.
	- The cache entry should include metadata about whether the data is "committed" (exists in the database) or "pending" (only exists as an event). This allows for more sophisticated read strategies based on consistency requirements.

---
## Pattern 3: Transactional Log Tailing (Change Data Capture)

### Core Concept
- The Transactional Log Tailing pattern, also known as Change Data Capture (CDC), eliminates the need for an outbox table entirely. Instead, it leverages the database's transaction log, which already contains a record of all changes made to the database.
- CDC tools like **Debezium** monitor the database's transaction log (such as MySQL's binlog or PostgreSQL's WAL) and automatically publish events to a message broker whenever changes are detected. This approach treats the database's transaction log as an event stream.

### Implementation Flow
1. **Normal Database Operations**: The application performs standard database operations without any special event-related code. It simply inserts, updates, or deletes records as needed.
2. **Log Monitoring**: A CDC tool continuously monitors the database's transaction log for changes.
3. **Event Generation**: When changes are detected, the CDC tool automatically generates corresponding events and publishes them to the message broker.
4. **Event Filtering**: The CDC tool can be configured to filter events based on table names, operation types, or other criteria to ensure only relevant events are published.

![[Pasted image 20250522171545.png]]


### Key Challenges
- **Event Duplication**: CDC tools might publish the same event multiple times due to restarts, network issues, or log replay scenarios. Consumer-side idempotency handling is essential to prevent duplicate processing.
- **Ordering Complexities**: When multiple concurrent transactions are committed, the CDC tool might publish events in a different order than the transactions were originally submitted. This is because the order in the transaction log might differ from the order of transaction initiation. Use Kafka Streams or similar technologies when strict ordering is required.
- **Latency Dependencies**: The event publishing latency depends on how quickly the database writes to its transaction log and how frequently the CDC tool polls the log. Some databases use batch writes to their logs, introducing additional latency.
- **Unwanted Event Noise**: The transaction log contains all database changes, including administrative operations, temporary data, or operations you don't want to publish as business events. Implement comprehensive filtering logic to capture only the relevant business events.
- **Schema Evolution Challenges**: When database schemas change, the CDC tool must be updated to handle the new schema. This coupling between database schema and event schema can complicate deployments and migrations.

---
# Pattern Comparison and Selection Guide

### When to Choose Transactional Outbox
Choose the Transactional Outbox Pattern when you need fine-grained control over what events are published and when. This pattern works well when:
- You need to publish business events that don't directly correspond to database row changes
- You want to enrich events with additional context or computed data
- You need to publish events selectively based on complex business rules
- You're working with legacy systems where installing CDC tools isn't feasible
- You need to ensure events are published in a specific business order rather than database commit order

### When to Choose Listen to Yourself
The Listen to Yourself Pattern is ideal for systems that can tolerate higher eventual consistency and want to embrace event-driven architecture fully:
- You're building a new system and can design it to be event-first from the ground up
- You want to decouple command processing from state persistence
- You need to support multiple read models or projections from the same events
- You're implementing Event Sourcing patterns where events are the primary source of truth

### When to Choose CDC
Transactional Log Tailing is best for:
- Existing systems where you want to add event publishing without modifying application code
- Data integration scenarios where you need to replicate all database changes to other systems
- Systems with high write volumes where the overhead of outbox tables would be significant
- When you need to capture changes made by multiple applications or direct database access
---