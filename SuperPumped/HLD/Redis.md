# Real-World Use Cases: Understanding Redis Through Problem-Solving

### Use Case 1: Real-Time Gaming Leaderboard

- **The Problem**: A mobile game with millions of players needs to show real-time rankings. Players complete levels and earn points throughout the day. The leaderboard must show top 100 players globally and each player's current rank instantly.

- **Traditional Database Approach**: Store scores in a table with player_id and score columns. To get rankings, execute `SELECT * FROM scores ORDER BY score DESC LIMIT 100`. To find a specific player's rank, count how many players have higher scores. These queries become expensive with millions of players and frequent updates.

- **Redis Solution Using Sorted Sets**: Use Redis sorted set where player IDs are members and scores are the sort values. When player 'alice' scores 1500 points, execute `ZADD leaderboard 1500 alice`. To get top 10 players, use `ZREVRANGE leaderboard 0 9 WITHSCORES`. To find alice's rank, use `ZREVRANK leaderboard alice`.

- **Why This Works**: Sorted sets maintain order automatically using skip list data structure. Adding a new score takes O(log N) time, getting top N takes O(log N + N) time, and finding rank takes O(log N) time. Compare this to traditional database approach which requires full table scans for ranking queries.

- **Data Structure Example**: The leaderboard sorted set might contain: alice:1500, bob:1200, charlie:1800, diana:900. Redis automatically maintains order: charlie:1800, alice:1500, bob:1200, diana:900. Getting top 3 returns charlie, alice, bob instantly.

---
### Use Case 2: Social Media Activity Feeds

- **The Problem**: Users follow hundreds of other users. When they open the app, they need to see recent posts from people they follow, ordered by time. Traditional approach requires complex joins across users, followers, and posts tables for each feed request.

- **Redis Solution Using Lists**: Maintain a list for each user containing recent post IDs from people they follow. When someone posts, add that post ID to the front of all their followers' lists. User feed becomes a simple list retrieval operation.

- **Data Structure Example**: User alice follows bob and charlie. When bob posts (post_id: 123), add 123 to alice's feed list. When charlie posts (post_id: 124), add 124 to alice's feed list. Alice's feed list becomes: [124, 123]. Getting alice's feed retrieves this list and fetches post details.

- **Why Lists Work**: Lists provide O(1) insertion at head and efficient range retrieval. The trade-off is space complexity - each user's feed is precomputed and stored, but feed generation becomes instant rather than requiring complex queries.

---
### Use Case 3: Session Management for Web Applications

- **The Problem**: Web applications need to track user sessions across multiple servers. Traditional approach stores sessions in database or server memory, but database access is slow and server memory doesn't work with load balancers distributing users across servers.

- **Redis Solution Using Hashes**: Store each session as a hash with session ID as key. Session data includes user ID, login time, shopping cart contents, preferences. Set automatic expiration to handle logout and session cleanup.

- **Data Structure Example**: Session for user logged in as alice might be stored as hash `session:abc123` containing `{user_id: alice, login_time: 1642234567, cart: [item1, item2], theme: dark}`. Set expiration to 30 minutes, extending it on each user activity.

- **Why Hashes Work**: Hashes allow partial updates - you can modify just the cart contents without retrieving entire session. Automatic expiration handles cleanup without additional cleanup processes. Multiple servers can access same session data instantly.

---
### Use Case 4: Rate Limiting for API Protection

**The Problem**: APIs need protection from abuse - limiting users to specific number of requests per time window. Traditional approach requires database tracking of request counts per user per time period, which becomes expensive with high request volumes.

**Redis Solution Using Strings with Expiration**: For each user, maintain a counter with automatic expiration. Key format like `rate_limit:user123:hour:14` for requests in hour 14. Increment counter on each request, set expiration on first request. Reject requests when counter exceeds limit.

**Data Structure Example**: User alice makes requests during hour 14. First request sets `rate_limit:alice:hour:14 = 1` with 1-hour expiration. Subsequent requests increment: 2, 3, 4. When reaching limit (say 100), reject further requests. Key automatically expires after the hour.

**Why Strings with Expiration Work**: Atomic increment operations prevent race conditions. Automatic expiration eliminates need for cleanup processes. Simple counter approach scales better than database queries for high-volume APIs.

---
### Use Case 5: Geospatial Applications (Uber/Food Delivery)

**The Problem**: Find nearby drivers or restaurants within specific radius of user location. Traditional approach requires complex mathematical calculations on all locations in database, which becomes expensive with millions of locations.

**Redis Solution Using Geospatial Indexes**: Store locations using geospatial commands. Add locations with `GEOADD drivers 77.123 28.456 driver1`. Find nearby drivers with `GEORADIUS drivers 77.100 28.400 5 km`. Redis handles complex geographic calculations internally.

**Data Structure Example**: Store driver locations in geo index called 'drivers'. Add driver1 at longitude 77.123, latitude 28.456. When user at 77.100, 28.400 requests ride, query returns all drivers within 5km radius instantly, sorted by distance.

**Why Geospatial Indexes Work**: Redis uses geohash algorithm to convert coordinates into sortable strings, enabling efficient range queries. Complex geographic calculations are handled by Redis internally, eliminating need for application-level geographic math.

---
### Use Case 6: Real-Time Analytics Dashboard

**The Problem**: Display real-time metrics like current active users, page views per minute, error rates. Traditional approach requires complex aggregation queries on large datasets, which are too slow for real-time dashboards.

**Redis Solution Using Multiple Data Structures**: Use sets for unique visitor counting, strings for counters, sorted sets for time-series data. Combine different structures to build comprehensive analytics.

**Data Structure Example**: Track unique visitors using set `visitors:today`, adding user IDs. Count total page views using string counter `pageviews:today`. Track errors over time using sorted set `errors:timeline` with timestamps as scores. Dashboard queries these structures instantly.

**Why Multiple Structures Work**: Each structure optimizes for specific query pattern. Sets provide unique counting, strings provide fast counters, sorted sets provide time-based queries. Combining structures gives comprehensive analytics without complex database queries.

---
### Use Case 7: Pub/Sub for Real-Time Notifications

**The Problem**: Send real-time notifications to users - chat messages, stock price updates, game events. Traditional approach uses polling or complex message queuing systems, which either waste resources or add infrastructure complexity.

**Redis Solution Using Pub/Sub**: Publishers send messages to channels, subscribers listen to channels. When stock price changes, publish to 'stock:AAPL' channel. All users watching Apple stock receive update instantly.

**Data Structure Example**: User alice subscribes to 'stock:AAPL' and 'stock:GOOGL' channels. When Apple stock price changes, publish message to 'stock:AAPL'. Alice receives notification instantly without polling. Multiple users can subscribe to same channels.

**Why Pub/Sub Works**: Eliminates polling overhead and provides instant message delivery. Redis handles message routing internally. Scales well with many subscribers per channel. Simple pattern for real-time features.

---

## Essential Redis Interview Questions and Answers

### Fundamental Concepts

**Q: What makes Redis faster than traditional databases?**

Redis achieves speed through several technical optimizations. First, all data resides in RAM, eliminating disk I/O bottlenecks that plague traditional databases. RAM access takes nanoseconds while disk access takes milliseconds - roughly 100,000 times faster. Second, Redis uses a single-threaded execution model for data operations, eliminating lock contention and context switching overhead that multi-threaded systems suffer from. Third, Redis commands map directly to optimized data structure operations rather than requiring query parsing and execution planning like SQL databases. Finally, Redis uses specialized data structures like skip lists for sorted sets and hash tables for key lookups, providing O(1) or O(log N) operations instead of generic B-tree structures.

**Q: How does Redis handle concurrency without multiple threads?**

Redis uses an event-driven, single-threaded architecture for data operations. All commands execute sequentially, making each command naturally atomic. This eliminates race conditions that plague multi-threaded systems. For example, the INCR command reads, increments, and writes a value as one atomic operation - no other command can interfere. Redis handles concurrency by processing commands very quickly (microseconds each) and queuing incoming requests. While background tasks like persistence and replication use separate threads, the main data operations remain single-threaded, ensuring consistency without locks.

**Q: What happens when Redis runs out of memory?**

Redis provides several eviction policies when memory limit is reached. The 'noeviction' policy returns errors for new writes, preserving existing data. The 'allkeys-lru' policy removes least recently used keys regardless of expiration settings. The 'volatile-lru' policy only removes LRU keys that have expiration set. Random eviction policies exist for simpler implementation. The choice depends on your use case - cache scenarios often use LRU policies, while session stores might use 'noeviction' to preserve critical data. Redis also provides memory usage monitoring and warnings before limits are reached.

### Persistence and Durability

**Q: Explain Redis persistence options and trade-offs.**

Redis offers three persistence approaches. RDB creates point-in-time snapshots of the entire dataset at configured intervals. It's space-efficient and provides fast restart times but can lose data between snapshots. AOF logs every write command, providing better durability but larger files and slower restarts. Hybrid persistence combines both - RDB for base data and AOF for recent changes. The trade-off is between performance and durability. Pure in-memory (no persistence) offers maximum performance but loses all data on restart. RDB provides good balance for most use cases. AOF with 'always' sync provides maximum durability but impacts performance.

**Q: How does Redis replication work?**

Redis replication follows a master-slave model. When a slave connects to master, it requests synchronization. The master initiates a background save (RDB snapshot) while buffering new commands. It sends the RDB file to the slave, which loads it to establish baseline. The master then sends buffered commands and continues streaming new commands. This provides eventual consistency - slaves lag behind master by network latency plus processing time. Slaves can serve read queries, distributing read load. If master fails, a slave can be promoted to master, though this requires external coordination or Redis Sentinel for automatic failover.

**Q: What is Redis Clustering and how does it work?**

Redis Cluster distributes data across multiple nodes for horizontal scaling. It divides the keyspace into 16,384 hash slots using consistent hashing. Each node owns a subset of slots. When a client requests a key, Redis calculates which slot the key belongs to and redirects to the appropriate node. Cluster nodes communicate via gossip protocol to maintain cluster state and detect failures. When a master node fails, its slaves can be promoted to maintain availability. The trade-off is increased complexity and loss of multi-key transactions across nodes, but you gain horizontal scalability beyond single-node memory limits.

### Caching Strategies

**Q: How do you handle cache invalidation in Redis?**

Cache invalidation depends on your consistency requirements and update patterns. TTL-based expiration works well for data that becomes stale predictably - set expiration times based on how often underlying data changes. Write-through invalidation deletes cache entries when underlying data updates, maintaining strong consistency but requiring careful coordination. Write-behind invalidation updates cache immediately and database asynchronously, providing low latency but risking data loss. Event-driven invalidation uses pub/sub or message queues to notify cache of changes, scaling better but adding complexity. Cache-aside pattern lets application manage cache, providing flexibility but requiring more application logic.

**Q: When would you choose write-through vs write-behind caching?**

Write-through caching updates both cache and database synchronously, ensuring consistency but increasing write latency. Choose this when data consistency is critical and you can accept higher write latency. Write-behind caching updates cache immediately and database asynchronously, providing low write latency but risking data loss if cache fails. Choose this for high-throughput scenarios where eventual consistency is acceptable. Consider your failure scenarios - write-through survives cache failures but write-behind risks losing unflushed writes. Most web applications use write-through for critical data like user accounts and write-behind for less critical data like activity logs.

### Performance and Monitoring

**Q: How do you monitor Redis performance?**

Monitor key metrics through Redis INFO command. Memory metrics include used_memory, memory fragmentation ratio, and evicted keys. Performance metrics include instantaneous operations per second, total commands processed, and keyspace hits vs misses (cache hit ratio). Latency metrics track command execution times - use SLOWLOG to identify problematic commands. Persistence metrics monitor RDB save times and AOF rewrite status. Network metrics track connected clients and input/output bytes. Set up alerts for memory usage approaching limits, high latency commands, and low cache hit ratios. Use Redis monitoring tools like RedisInsight or integrate with monitoring systems like Prometheus.

**Q: What are common Redis performance bottlenecks and solutions?**

Memory fragmentation occurs when allocated memory becomes scattered. If fragmentation ratio exceeds 1.5, consider restarting Redis or enabling active defragmentation. Slow commands like KEYS, SORT on large datasets, or operations on huge data structures can block other operations. Use SCAN instead of KEYS, optimize data structure sizes, and monitor SLOWLOG. Network bandwidth can limit throughput, especially in replication scenarios - consider compression or reducing data size. CPU can become bottleneck with complex operations - consider read replicas to distribute load. Memory limits cause eviction - monitor memory usage and plan capacity appropriately.

### Data Structure Selection

**Q: When would you use each Redis data structure?**

Choose strings for simple key-value storage, counters, and binary data. Use lists for queues, activity feeds, and ordered collections where you need fast insertion at ends. Use sets for unique collections, tag systems, and set operations like intersections. Use sorted sets for leaderboards, time-series data, and range queries by score. Use hashes for object storage where you need partial field access. Use geospatial indexes for location-based queries. Use streams for message queuing with persistence and consumer groups. The key is matching data structure capabilities to your access patterns - sorted sets excel at ranking queries, lists excel at queue operations, sets excel at membership testing.

**Q: How do you implement distributed locking with Redis?**

Implement distributed locking using SET command with NX (not exists) and EX (expiration) options. Acquire lock by setting a key with unique identifier and expiration time. Release lock by checking if the key still contains your identifier before deletion (use Lua script for atomicity). Handle edge cases like lock timeout (set appropriate expiration), lock renewal (extend expiration for long operations), and deadlock prevention (always set expiration). For multi-node Redis, use Redlock algorithm which requires majority consensus across nodes. Consider that Redis locks are advisory - all participants must respect the locking protocol. Distributed locking is complex - ensure you need it before implementing.

### Scaling and Architecture

**Q: How do you scale Redis for high availability?**

Scale Redis through several approaches. Vertical scaling adds more memory and CPU to single instance - simple but limited. Read scaling uses master-slave replication where slaves handle read queries. Write scaling requires sharding data across multiple masters or using Redis Cluster. High availability uses Redis Sentinel for automatic failover or Redis Cluster for built-in fault tolerance. Consider your consistency requirements - async replication provides performance but risks data loss during failover. Implement proper monitoring and alerting. Plan for failure scenarios and test failover procedures. Remember that scaling complexity increases with advanced patterns - start simple and scale based on actual needs.

**Q: Redis vs Memcached - when to choose which?**

Choose Redis when you need rich data structures, persistence, pub/sub messaging, or complex operations like sorted sets and geospatial queries. Redis provides more features but uses more memory per key. Choose Memcached for simple key-value caching with minimal memory overhead and maximum throughput. Memcached is simpler to deploy and manage. Redis is better for complex caching scenarios, session storage, and real-time features. Memcached is better for pure caching with memory constraints. Consider that Redis can do everything Memcached does plus much more, but at higher resource cost. Most modern applications choose Redis for its versatility unless memory efficiency is critical.

### Common Use Cases

**Q: How would you implement rate limiting using Redis?**

Implement rate limiting using Redis strings with expiration. For each user and time window, maintain a counter key like 'rate_limit:user123:minute:45'. On each request, increment the counter atomically. If counter exceeds limit, reject request. Set expiration on first request to automatically clean up old counters. For sliding window rate limiting, use sorted sets with timestamps as scores. Add request timestamps and remove old entries beyond the window. For distributed rate limiting, ensure atomic operations across multiple application servers. Consider different limits for different users or API endpoints. Monitor rate limiting effectiveness and adjust limits based on abuse patterns.

**Q: How would you design a chat application using Redis?**

Design chat using Redis pub/sub for real-time messaging and other structures for persistence. Use pub/sub channels for each chat room - users subscribe to room channels and publish messages. Store chat history using lists or streams, with each room having its own list. Use sets to track active users in each room. Use hashes to store user profiles and online status. For private messaging, create channels based on user IDs. Handle message persistence by storing messages in database while using Redis for real-time delivery. Consider message ordering, delivery guarantees, and offline message handling. Scale by sharding rooms across Redis instances or using Redis Cluster.

This comprehensive guide covers the essential Redis knowledge needed for system design interviews. The real-world use cases demonstrate practical problem-solving approaches, while the Q&A section provides ready answers for common interview questions.

---
