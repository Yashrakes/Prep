
# Index
- [[#Cache Aside (Lazy Loading)]]
- [[#Read Through Cache]]
- [[#Write Around Cache]]
- [[#Write Through Cache]]
- [[#Write Back (or Write Behind) Cache]]
- [[#Cache Invalidation]]
- [[#Interview Questions]]

---
# Intro

- Distributed caching is a technique used in modern distributed systems to improve performance, reduce latency, and decrease the load on backend data sources. 
- Unlike a local cache that exists on a single server, a distributed cache spans multiple servers, allowing applications in a distributed environment to access a shared cache. 
- This approach is particularly valuable in microservices architectures, cloud-based applications, and any system where data needs to be accessed quickly across multiple nodes.

---
# Cache Aside (Lazy Loading)

 - In a Cache Aside pattern, the application is responsible for managing the interaction between the cache and the data store. When data is requested, the application first checks the cache; if the data isn't found (a cache miss), it retrieves the data from the database and then populates the cache with this data for future requests.

**Sequence Diagram:**

```
Client        Application        Cache        Database
   |               |               |              |
   | Request Data  |               |              |
   |-------------->|               |              |
   |               | Check Cache   |              |
   |               |-------------->|              |
   |               | Cache Miss    |              |
   |               |<--------------|              |
   |               | Query Data    |              |
   |               |----------------------------->|
   |               |               |   Return Data|
   |               |<-----------------------------|
   |               | Update Cache  |              |
   |               |-------------->|              |
   |               |  Confirmation |              |
   |               |<--------------|              |
   | Return Data   |               |              |
   |<--------------|               |              |
```

**Pros:**
- Only requested data is cached, which uses cache memory efficiently
- The cache is updated only when needed, reducing unnecessary write operations
- Resilient to cache failures; the application can still function by accessing the database directly
- Works well for read-heavy workloads where the same data is accessed frequently

**Cons:**
- Initial requests for data will always be slower due to the cache miss scenario
- Data inconsistency can occur if the database is updated by other processes without updating the cache
- Requires implementation of cache expiration or invalidation strategies to handle data freshness
- The application code manages cache logic, increasing complexity

---
# Read Through Cache

- Read Through is similar to Cache Aside but with a key difference: the cache itself is responsible for loading data from the database when a cache miss occurs, not the application.

**Sequence Diagram:**
```
Client        Application        Cache        Database
   |               |               |              |
   | Request Data  |               |              |
   |-------------->|               |              |
   |               | Check Cache   |              |
   |               |-------------->|              |
   |               |               | Cache Miss   |
   |               |               |------------->|
   |               |               | Query Data   |
   |               |               |------------->|
   |               |               |   Return Data|
   |               |               |<-------------|
   |               | Return Data   |              |
   |               |<--------------|              |
   | Return Data   |               |              |
   |<--------------|               |              |
```

**Pros:**
- Simplifies application code as the cache management is abstracted away
- Ensures a consistent approach to loading data into the cache
- Reduces the chances of cache inconsistencies
- Works well with cache providers that have built-in read-through capabilities

**Cons:**
- Still experiences slower performance on initial requests due to cache misses
- Limited control over what data gets cached and how it's structured
- May not be suitable for applications that need custom cache warming strategies
- Potentially less efficient than Cache Aside for certain access patterns
for read heavy system always prefer read through cache in compare to cache aside , 
---
# Write Around Cache

- In a Write Around pattern, write operations go directly to the database, bypassing the cache. The cache is only updated when data is read (typically through a Cache Aside or Read Through approach).

**Sequence Diagram:**
```
Client        Application        Cache        Database
   |               |               |              |
   | Write Data    |               |              |
   |-------------->|               |              |
   |               | Write Data    |              |
   |               |----------------------------->|
   |               |               |  Confirmation|
   |               |<-----------------------------|
   | Confirmation  |               |              |
   |<--------------|               |              |
   |               |               |              |
   | Request Data  |               |              |
   |-------------->|               |              |
   |               | Check Cache   |              |
   |               |-------------->|              |
   |               | Cache Miss    |              |
   |               |<--------------|              |
   |               | Query Data    |              |
   |               |----------------------------->|
   |               |               |   Return Data|
   |               |<-----------------------------|
   |               | Update Cache  |              |
   |               |-------------->|              |
   |               |  Confirmation |              |
   |               |<--------------|              |
   | Return Data   |               |              |
   |<--------------|               |              |
```

**Pros:**
- Prevents the cache from being flooded with write-intensive data that might not be read
- Efficiently handles write-heavy workloads with infrequent reads
- Reduces cache churn from data that gets written but rarely read
- Good for systems with large volumes of transient data

**Cons:**
- Initial reads will always experience higher latency due to cache misses
- Can lead to higher read latency in read-after-write scenarios
- May result in more database read operations when data is frequently updated then read
- Cache and database can become inconsistent until the cache is updated on read

---
# Write Through Cache

- In a Write Through caching strategy, data is written to both the cache and the database in the same operation. The write is considered complete only when both the cache and database have been updated.

**Sequence Diagram:**
```
Client        Application        Cache        Database
   |               |               |              |
   | Write Data    |               |              |
   |-------------->|               |              |
   |               | Update Cache  |              |
   |               |-------------->|              |
   |               | Confirmation  |              |
   |               |<--------------|              |
   |               | Write Data    |              |
   |               |----------------------------->|
   |               |               |  Confirmation|
   |               |<-----------------------------|
   | Confirmation  |               |              |
   |<--------------|               |              |
   |               |               |              |
   | Request Data  |               |              |
   |-------------->|               |              |
   |               | Check Cache   |              |
   |               |-------------->|              |
   |               | Return Data   |              |
   |               |<--------------|              |
```

**Pros:**
- Ensures cache and database consistency since they're updated together
- Provides fast reads for recently written data (no cache miss)
- Simplifies read operations as the cache is always up-to-date
- Good for systems where data is read shortly after being written

**Cons:**
- Higher latency for write operations since they need to update two systems
- Inefficient use of cache space if written data is rarely read
- Increased load on the cache system for write-heavy workloads
- Cache must be highly available or write operations will fail

---
# Write Back (or Write Behind) Cache

- In a Write Back strategy, data is written only to the cache initially. The cache then asynchronously writes the data to the database after some delay, often in batches to optimize database operations.

**Sequence Diagram:**
```
Client        Application        Cache        Database
   |               |               |              |
   | Write Data    |               |              |
   |-------------->|               |              |
   |               | Update Cache  |              |
   |               |-------------->|              |
   |               | Confirmation  |              |
   |               |<--------------|              |
   | Confirmation  |               |              |
   |<--------------|               |              |
   |               |               |              |
   |               |               | Async Write  |
   |               |               |------------->|
   |               |               |  Confirmation|
   |               |               |<-------------|
   |               |               |              |
   | Request Data  |               |              |
   |-------------->|               |              |
   |               | Check Cache   |              |
   |               |-------------->|              |
   |               | Return Data   |              |
   |               |<--------------|              |
   | Return Data   |               |              |
   |<--------------|               |              |
```

**Pros:**
- Fastest write performance since it only needs to update the cache initially
- Can batch database writes to reduce database load and improve efficiency
- Handles write-heavy workloads exceptionally well
- Buffers database from write request spikes

**Cons:**
- Risk of data loss if the cache fails before data is persisted to the database
- Temporary data inconsistency between cache and database
- More complex implementation, especially for handling failures
- Requires sufficient cache durability to prevent data loss
---
# Cache Invalidation

Cache invalidation ensures that the cached data remains consistent with the source of truth (usually a database). This is critical when data changes.

### Time-Based Invalidation (TTL)
Data in the cache expires after a set time-to-live (TTL).
**Pros:**
- Simple to implement
- Works well for data that changes predictably
- Requires no special events or coordination

**Cons:**
- Can lead to serving stale data until expiration
- May cause unnecessary cache misses if TTL is too short.
---
### Event-Based Invalidation
The cache is updated or invalidated when specific events occur (like data updates).

**Sequence Diagram:**
```
Client        Application A      Cache        Application B     Database
   |               |               |              |               |
   |               |               |              | Update Data   |
   |               |               |              |-------------->|
   |               |               |              | Confirmation  |
   |               |               |              |<--------------|
   |               |               |              | Invalidate    |
   |               |               |<-------------|               |
   |               |               | Confirmation |               |
   |               |               |------------->|               |
   | Request Data  |               |              |               |
   |-------------->|               |              |               |
   |               | Check Cache   |              |               |
   |               |-------------->|              |               |
   |               | Cache Miss    |              |               |
   |               |<--------------|              |               |
   |               | Query Data    |              |               |
   |               |--------------------------------------------->|
   |               |               |              |  Return Data  |
   |               |<------------------------------------------ --|
   |               | Update Cache  |              |               |
   |               |-------------->|              |               |
   | Return Data   |               |              |               |
   |<--------------|               |              |               |
```

**Pros:**
- Ensures data consistency
- Reduces serving of stale data
- Efficient for frequently accessed but infrequently updated data

**Cons:**
- More complex to implement
- Requires coordination between systems
- Can lead to "thundering herd" problems if many cached items expire simultaneously.
---

# Interview Questions
## Q1: When would you choose Write Back caching over Write Through?
**A:** I would choose Write Back caching when:
1. Write performance is critical
2. The system experiences high write volumes
3. Some level of data loss is acceptable (or mitigated through cache durability)
4. We want to reduce database load by batching write operations
5. The application can tolerate temporary inconsistency between the cache and database.

Write Through would be preferred when data consistency is more important than write performance.

---
## Q2: How would you handle cache invalidation in a microservices architecture?
**A:** In a microservices architecture, cache invalidation can be handled through:
1. Event-driven approach: Services publish events when data changes, and interested services subscribe to invalidate their caches
2. Message queues or pub/sub systems to propagate invalidation events
3. Time-to-live (TTL) as a fallback strategy
4. Distributed cache systems with built-in consistency protocols
5. Cache keys that incorporate version information
6. Eventual consistency patterns that tolerate temporary inconsistencies.

The specific approach depends on consistency requirements, the microservices communication pattern, and the distributed cache technology used.

---
## Q3: What is the "thundering herd" problem in caching and how would you mitigate it?
**A:** The "thundering herd" problem occurs when many cache entries expire simultaneously or a popular cache entry expires, causing numerous clients to request the same data from the database at once, overwhelming it.
To mitigate this:
1. Implement staggered expiration times by adding jitter to TTLs
2. Use cache stampede prevention techniques like probabilistic early recomputation
3. Implement distributed locks or semaphores to ensure only one process refreshes expired data
4. Use background refresh mechanisms that update cache entries before they expire
5. Implement request coalescing, where multiple identical requests are combined into one database query

---
## Q4: How would you design a caching strategy for a system with eventual consistency requirements?
**A:** For a system with eventual consistency requirements:
1. Use Cache Aside or Write Back patterns which naturally support eventual consistency
2. Implement appropriate TTL values based on how long inconsistency can be tolerated
3. Consider event-based cache invalidation for critical updates
4. Include version numbers or timestamps with cached data to detect staleness
5. Implement background refresh processes for frequently accessed data
6. Use conflict resolution strategies when conflicting updates occur
7. Consider using CRDTs (Conflict-free Replicated Data Types) for certain data structures
8. Monitor consistency metrics to ensure eventual consistency SLAs are met
---
## Q5: How do you determine the appropriate TTL for cached data?
**A:** The appropriate TTL should be determined by:
1. The volatility of the data (how frequently it changes)
2. Tolerance for stale data in the specific use case
3. Read vs. write patterns for the data
4. Business requirements for data freshness
5. Resource constraints of the cache
6. Database load considerations
I would start with a conservative TTL, measure cache hit rates and freshness metrics, and then adjust. Critical data might have shorter TTLs (minutes or seconds), while relatively static data could have longer TTLs (hours or days).

---
## Q6: What caching strategy would you use for a high-traffic e-commerce product catalog?
**A:** For a high-traffic e-commerce product catalog, I would implement:
1. Cache Aside pattern for most product data since it's read-heavy
2. Longer TTLs for static product information (descriptions, images)
3. Shorter TTLs or event-based invalidation for dynamic data (prices, inventory)
4. Prewarming of cache for popular products
5. Consider Read Through for standardized data access patterns
6. Write Around for inventory updates (bypassing cache on writes)
7. A layered caching approach with CDN for images and static content
8. Partitioning of the cache by product categories to improve hit rates

This approach optimizes for read performance while ensuring reasonable freshness of critical data like prices and inventory.

---
|Feature|               Read-Through|              Cache-Aside|

|Simplicity for reads|✅ Easier|❌ App must handle logic|
|Automatic population|✅ Yes|❌ No|
|Write consistency control|❌ Harder|✅ App controls it|
|Best for read-heavy?|✅ Yes|⚠️ Sometimes|


1. building an e-commerce product service where the data is updated rarely but read frequently, what'd you use? use cache-aside, can pre-warm the cache with frequently accessed data to reduce cold starts, also can use TTL or manual invatidation (admin-updated) to prevent stale reads. 2. Designing a leaderboard for a game that updates scores often. abstract the cache management to avoid boilerplate code in the app layer ? use read-through to completely transfer the caching logic to cache layer,

2. building an e-commerce product service where the data is updated rarely but read frequently, what'd you use? use cache-aside, can pre-warm the cache with frequently accessed data to reduce cold starts, also can use TTL or manual invatidation (admin-updated) to prevent stale reads. 2. Designing a leaderboard for a game that updates scores often. abstract the cache management to avoid boilerplate code in the app layer ? use read-through to completely transfer the caching logic to cache layer,



3. Logging user events into a DB but don't need those logs for immediate reads, what to use? use write around and write directly to the cache, since the logs are rarely used, we don't pollute the cache and cache this way will take in relevant fields only. note that reads might be slow but the cache stays clean. 4. building stock trading system, consistency is critical in orders and updates, what to use? write-through so that write updates both the cache and the DB in a single.atomic operation


so how does distributed caching work?
when the data is cached, the client library typically hashes the key associated with the data to determine twhich cache node will store it. for reliability, the cache system replicates the cached data across multiple nodes. so if node A stores the data, it might also be copied to another node like B as a backup. to get the data from the cache, application provides the key to the client library, which the library uses to find and query the node which has the data, if the data is present, it's returned to the application. if not, the data is fetched from the primary data source and can be cached for future use. 
next make sure how to handle cache invalidation and cache eviction
 sample flow: you as the user will provide this:
cacheClient.set("user:123", userData);
now this key will be used by the server to generate a hash and find the corresponding hash cache node using consistent hashing for the given data and stored there. now if there is a get request with the same key, this key will be used again and then  the respective data cache will be fetched and returned.
in this, we have a cache pool (hosted on independent servers, and a cache client which manages the access to this pool)
so now the appserver first requests the cache client to give it a cache, for this it uses consistent hashing technique (note that each server should be allotted the same cache everytime otherwise the reasoning behind caches would fail..)