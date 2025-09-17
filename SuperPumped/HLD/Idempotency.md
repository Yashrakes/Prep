# Idempotent POST APIs: A Comprehensive Guide

## What is Idempotency?
- Idempotency is a mathematical and computer science property where applying an operation multiple times produces the same result as applying it once. In the context of APIs, an idempotent endpoint ensures that making the same request multiple times has the same effect as making it once.
- This property is essential for building reliable systems, particularly when:
	- Network failures might cause clients to retry requests
	- Users might accidentally submit the same form twice
	- Mobile applications operate in unstable network environments
---
## Idempotency in HTTP Methods

- By default, certain HTTP methods already have idempotent properties:
	- **Naturally idempotent**: GET, HEAD, OPTIONS, TRACE (read-only operations)
	- **Designed to be idempotent**: PUT, DELETE
	- **Not inherently idempotent**: POST (typically creates new resources)
- The challenge lies in making POST requests idempotent, since they're conventionally used for resource creation and other state-changing operations.
---
## The Idempotent POST Flow

![[Pasted image 20250412164716.png]]

---
## Handling Sequential Requests

![[Pasted image 20250412164812.png]]

---
## Handling Parallel Requests

![[Pasted image 20250412164846.png]]

### Handling Clients During Lock Acquisition

> In the parallel request scenario, when request 1 acquires the lock and request 2 is waiting, what happens to the client thread for request 2?

- This depends on the implementation approach. There are several common patterns:
	1. **Synchronous Waiting**: The server holds the connection open, and the client thread remains in a waiting state until the lock is acquired. This is simple but consumes server resources and can lead to connection timeouts if the first request takes too long.
	2. **Retry-After Response**: The server immediately returns a 409 Conflict or 429 Too Many Requests status code with a `Retry-After` header suggesting when to retry. The client is responsible for implementing the retry logic.
	3. **Asynchronous Processing**: The server accepts both requests, queues the second one internally, and returns a 202 Accepted status to both clients with a status check endpoint. Clients can poll this endpoint to determine when processing completes.
	4. **WebHook Callback**: Similar to the asynchronous approach, but instead of having clients poll, the server pushes a notification to a client-provided callback URL when processing completes.
- For web applications, **option 2 (Retry-After)** is most common as it works well with standard HTTP clients and doesn't tie up server resources. For microservices, option 3 or 4 is often preferred as they better handle longer-running operations.
- The lock waiting period should always have a reasonable timeout to prevent deadlocks if the first request never completes or releases the lock.

---
## Key Components of Idempotent POST Systems

### 1. Idempotency Key Generation
- The client must generate a unique key for each logical operation. Common approaches include:
	- UUID/GUID generation
	- Hash of request payload + timestamp
	- Client-side transaction IDs
- The key must be unique to the specific operation intent, not just the request content.

### 2. Storage and Lifecycle Management
- The server needs a system to store idempotency keys and associated responses:
- Each key should have a Time-To-Live (TTL) after which it expires. Common TTL periods:
	- Short-lived operations: 15 minutes to 1 hour
	- Business transactions: 24-48 hours
	- Financial operations: Up to 7 days

![[Pasted image 20250412164917.png]]

### 3. Concurrency Control Mechanisms
![[Pasted image 20250412164958.png]]

### 4. System Architecture for Idempotent APIs
![[Pasted image 20250412165018.png]]

---

# Key Interview Considerations

## 1. Durability vs. Performance Trade-offs
- Storage options for idempotency mechanisms vary in durability and performance:
	- In-memory storage provides the fastest performance but is lost on server restarts
	- Distributed caches like Redis offer a balance of speed and resilience
	- Database storage provides the highest durability but with higher latency
- For critical operations like payments, higher durability is essential, while for less critical operations, performance might be prioritized.
---
## 2. Expiration Policies
Idempotency keys cannot be stored forever, so a well-designed system needs policies for:
- How long to keep keys (typically hours to days depending on the business context)
- What to do when a key expires but a client retries (usually treat as a new request)
- Whether to use different expiration times for different operation types.
---
## 3. Failure Recovery Strategies
Robust idempotent systems plan for various failure scenarios:
- Server crashes during processing
- Database failures during state updates
- Network partitions in distributed systems
- Partial completion of complex operations.
---
## 4. Idempotency Guarantees
Different systems offer different levels of idempotency guarantees:
- Strict idempotency: Exactly the same effect and response
- Result idempotency: Same end state but potentially different responses
- Semantic idempotency: Equivalent business outcome but technical details may differ.
---
## 5. Scalability Considerations
As systems grow, idempotency mechanisms must scale:
- Sharding of idempotency storage across multiple nodes
- Replication of idempotency data for fault tolerance
- Performance impact as idempotency dataset grows.
---

### ✅ Solution: **Persist the Key Across Page Refresh**

To handle this, you need to store the idempotency key in **persistent client-side storage** like:

| Option           | Use When                                   |
| ---------------- | ------------------------------------------ |
| `localStorage`   | Persistent even after tab close or refresh |
| `sessionStorage` | Lasts for the duration of a tab session    |
| `IndexedDB`      | For larger or more structured data         |
| URL parameter    | For deep linking use cases                 |
| Cookie           | If working in server-rendered app          |
## 💡 Ideal Client-Side Flow

| Step                                        | Description                                                       |
| ------------------------------------------- | ----------------------------------------------------------------- |
| 1. **User initiates operation**             | e.g., clicks “Pay ₹500”                                           |
| 2. **Check for existing idempotency key**   | Look in `localStorage` (or session/local DB)                      |
| 3. **If not found, generate**               | Use `UUID` (e.g., `crypto.randomUUID()` or `uuid.v4()`), store it |
| 4. **Send request with the key**            | Attach to HTTP request header like: `"Idempotency-Key": key`      |
| 5. **On success, clear the key**            | Remove from storage to avoid reusing it                           |
| 6. **On failure, retry with same key**      | Retry on 5xx, network error etc. with _same_ key                  |
| 7. **If user cancels / finishes operation** | Clear key manually or via timeout logic                           |
## 🛠 Ideal Server-Side Flow

|Step|Description|
|---|---|
|1. **Receive request with Idempotency-Key**|Validate it’s present and well-formed|
|2. **Check DB or cache for that key**|Redis or DB table|
|3. **If found**|Return **cached result** (don’t reprocess)|
|4. **If not found**|Proceed with operation, then store result alongside key|
|5. **Store response (optional)**|So same response can be returned on retries|
|6. **Expire old keys**|TTL in Redis or archived in DB after X hours/days|