
  

# Problem Statements for Each LLD Question

## 1. **Rate Limiter (Token Bucket)**

Design a rate limiter that allows a maximum number of requests per time window for API endpoints. Support per-user/per-IP limiting where each user can make N requests per second.

## 2. **Distributed Cache with TTL**

Design an in-memory cache system where each entry has a time-to-live (TTL). Expired entries should be automatically removed. Support concurrent get/put operations.

## 3. **Message Queue (Producer-Consumer)**

Design a bounded message queue where multiple producers can publish messages and multiple consumers can process them. Ensure messages are processed exactly once.

## 4. **Connection Pool**

Design a database/HTTP connection pool that maintains a fixed number of reusable connections. Multiple threads should be able to acquire and release connections safely.

## 5. **Pub-Sub System**

Design a publish-subscribe messaging system where subscribers can listen to topics and publishers can send messages to topics. Multiple subscribers should receive messages concurrently.

## 6. **Parking Lot (Thread-Safe)**

Design a multi-level parking lot system that supports different vehicle types (motorcycle, car, truck). Handle concurrent vehicle entry and exit operations safely.

## 7. **Task Scheduler**

Design a task scheduler that can schedule tasks to run after a delay or at recurring intervals. Support multiple concurrent task executions.

## 8. **Read-Write Lock Based File System**

Design a simple in-memory file system that allows multiple concurrent reads but exclusive writes. Support create, read, write, and append operations.

## 9. **Dining Philosophers**

Implement the classic dining philosophers problem where N philosophers must share N forks to eat. Prevent deadlocks and ensure fairness.

## 10. **Web Crawler**

Design a multi-threaded web crawler that crawls URLs while respecting politeness policies (delay between requests to same domain). Avoid visiting the same URL twice.

## Additional Common Problems:

## 11. **LRU Cache**

Design a thread-safe Least Recently Used cache with get and put operations in O(1) time. Support concurrent access from multiple threads.

## 12. **Ride Sharing System (Uber/Lyft)**

Design a ride-sharing system that matches riders with drivers. Handle concurrent ride requests, driver availability updates, and ride assignments.

## 13. **Stock Exchange/Order Matching System**

Design an order book for stock trading that matches buy and sell orders. Handle concurrent order placements, cancellations, and executions.

## 14. **Hotel Booking System**

Design a hotel room booking system that handles concurrent reservation requests. Prevent double-booking and handle room inventory updates.

## 15. **Elevator System**

Design an elevator control system for a building with multiple elevators. Handle concurrent floor requests and optimize elevator movements.

## 16. **Ticket Booking System (BookMyShow)**

Design a movie/event ticket booking system that handles concurrent seat selection and booking. Implement seat locking during checkout.

## 17. **Vending Machine**

Design a vending machine that handles concurrent product selection and payment processing. Manage inventory updates safely.

## 18. **ATM System**

Design an ATM system that handles concurrent transactions (withdraw, deposit, balance check) while maintaining account consistency.

## 19. **Logger System**

Design a thread-safe logging system that writes log messages to files. Support log rotation and concurrent writes from multiple threads.

## 20. **Semaphore-based Resource Pool**

Design a generic resource pool (e.g., thread pool, worker pool) using semaphores to limit concurrent access to N resources.

## 21. **Distributed ID Generator (Twitter Snowflake)**

Design a distributed unique ID generator that generates non-colliding IDs across multiple machines and threads.

## 22. **Notification Service**

Design a notification system that sends emails/SMS/push notifications. Handle retry logic, rate limiting, and concurrent notification sending.

## 23. **Leaderboard System**

Design a real-time leaderboard for games that handles concurrent score updates and rank queries efficiently.

## 24. **Food Delivery System (Swiggy/Zomato)**

Design a food delivery system that manages concurrent order placements, restaurant assignments, and delivery partner matching.

## 25. **Distributed Lock Service**

Design a distributed lock mechanism that allows processes to acquire and release locks for distributed coordination.


# Low-Level Design Problems with Multithreading Solutions

Here are comprehensive examples of LLD problems where multithreading is essential, along with Java implementations:

## 1. **Rate Limiter (Token Bucket)**

**Use Case**: API gateway limiting requests per user/IP

java

```java
public class TokenBucketRateLimiter {
    private final int capacity;
    private final int refillRate; // tokens per second
    private int availableTokens;
    private long lastRefillTimestamp;
    private final ReentrantLock lock = new ReentrantLock();
    
    public TokenBucketRateLimiter(int capacity, int refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.availableTokens = capacity;
        this.lastRefillTimestamp = System.currentTimeMillis();
    }
    
    public boolean allowRequest() {
        lock.lock();
        try {
            refill();
            if (availableTokens > 0) {
                availableTokens--;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    private void refill() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastRefillTimestamp;
        int tokensToAdd = (int) ((timePassed / 1000.0) * refillRate);
        
        if (tokensToAdd > 0) {
            availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
            lastRefillTimestamp = now;
        }
    }
}

// Multi-user rate limiter
public class MultiUserRateLimiter {
    private final ConcurrentHashMap<String, TokenBucketRateLimiter> limiters;
    private final int capacity;
    private final int refillRate;
    
    public MultiUserRateLimiter(int capacity, int refillRate) {
        this.limiters = new ConcurrentHashMap<>();
        this.capacity = capacity;
        this.refillRate = refillRate;
    }
    
    public boolean allowRequest(String userId) {
        TokenBucketRateLimiter limiter = limiters.computeIfAbsent(
            userId, 
            k -> new TokenBucketRateLimiter(capacity, refillRate)
        );
        return limiter.allowRequest();
    }
}
```

## 2. **Distributed Cache with TTL**

**Use Case**: In-memory cache like Redis with expiration

java

```java
public class CacheWithTTL<K, V> {
    private static class CacheEntry<V> {
        V value;
        long expiryTime;
        
        CacheEntry(V value, long ttlMillis) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
    
    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private final ReadWriteLock cleanupLock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService cleanupExecutor;
    
    public CacheWithTTL() {
        this.cache = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Cleanup expired entries every 5 seconds
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanup, 5, 5, TimeUnit.SECONDS
        );
    }
    
    public void put(K key, V value, long ttlMillis) {
        cleanupLock.readLock().lock();
        try {
            cache.put(key, new CacheEntry<>(value, ttlMillis));
        } finally {
            cleanupLock.readLock().unlock();
        }
    }
    
    public V get(K key) {
        cleanupLock.readLock().lock();
        try {
            CacheEntry<V> entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                cache.remove(key);
                return null;
            }
            return entry.value;
        } finally {
            cleanupLock.readLock().unlock();
        }
    }
    
    private void cleanup() {
        cleanupLock.writeLock().lock();
        try {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        } finally {
            cleanupLock.writeLock().unlock();
        }
    }
    
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
}
```

## 3. **Message Queue (Producer-Consumer)**

**Use Case**: Task queue, job scheduler, message broker

java

```java
public class MessageQueue<T> {
    private final BlockingQueue<T> queue;
    private final int capacity;
    private final AtomicInteger messageCount = new AtomicInteger(0);
    
    public MessageQueue(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedBlockingQueue<>(capacity);
    }
    
    public boolean publish(T message) throws InterruptedException {
        boolean added = queue.offer(message, 1, TimeUnit.SECONDS);
        if (added) {
            messageCount.incrementAndGet();
        }
        return added;
    }
    
    public T consume() throws InterruptedException {
        T message = queue.poll(1, TimeUnit.SECONDS);
        if (message != null) {
            messageCount.decrementAndGet();
        }
        return message;
    }
    
    public int size() {
        return messageCount.get();
    }
}

// Worker pool for processing messages
public class MessageProcessor<T> {
    private final MessageQueue<T> queue;
    private final ExecutorService workers;
    private final Consumer<T> processor;
    private volatile boolean running = true;
    
    public MessageProcessor(MessageQueue<T> queue, int workerCount, Consumer<T> processor) {
        this.queue = queue;
        this.processor = processor;
        this.workers = Executors.newFixedThreadPool(workerCount);
        
        for (int i = 0; i < workerCount; i++) {
            workers.submit(this::processMessages);
        }
    }
    
    private void processMessages() {
        while (running) {
            try {
                T message = queue.consume();
                if (message != null) {
                    processor.accept(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public void shutdown() {
        running = false;
        workers.shutdown();
    }
}
```

## 4. **Connection Pool**

**Use Case**: Database connection pool, HTTP client pool

java

```java
public class ConnectionPool<T> {
    private final BlockingQueue<T> available;
    private final Set<T> inUse;
    private final int maxSize;
    private final Supplier<T> connectionFactory;
    private final Lock lock = new ReentrantLock();
    private int currentSize = 0;
    
    public ConnectionPool(int maxSize, Supplier<T> connectionFactory) {
        this.maxSize = maxSize;
        this.connectionFactory = connectionFactory;
        this.available = new LinkedBlockingQueue<>();
        this.inUse = new HashSet<>();
    }
    
    public T acquire() throws InterruptedException {
        lock.lock();
        try {
            // Try to get from available pool
            T connection = available.poll();
            if (connection != null) {
                inUse.add(connection);
                return connection;
            }
            
            // Create new if under limit
            if (currentSize < maxSize) {
                connection = connectionFactory.get();
                currentSize++;
                inUse.add(connection);
                return connection;
            }
        } finally {
            lock.unlock();
        }
        
        // Wait for available connection
        T connection = available.take();
        lock.lock();
        try {
            inUse.add(connection);
            return connection;
        } finally {
            lock.unlock();
        }
    }
    
    public void release(T connection) {
        lock.lock();
        try {
            if (inUse.remove(connection)) {
                available.offer(connection);
            }
        } finally {
            lock.unlock();
        }
    }
    
    public int getAvailableCount() {
        return available.size();
    }
    
    public int getInUseCount() {
        lock.lock();
        try {
            return inUse.size();
        } finally {
            lock.unlock();
        }
    }
}
```

## 5. **Pub-Sub System**

**Use Case**: Event notification, real-time updates

java

```java
public class PubSubSystem<T> {
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<Subscriber<T>>> topicSubscribers;
    private final ExecutorService publisherExecutor;
    
    public interface Subscriber<T> {
        void onMessage(T message);
    }
    
    public PubSubSystem() {
        this.topicSubscribers = new ConcurrentHashMap<>();
        this.publisherExecutor = Executors.newFixedThreadPool(10);
    }
    
    public void subscribe(String topic, Subscriber<T> subscriber) {
        topicSubscribers.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>())
                       .add(subscriber);
    }
    
    public void unsubscribe(String topic, Subscriber<T> subscriber) {
        CopyOnWriteArraySet<Subscriber<T>> subscribers = topicSubscribers.get(topic);
        if (subscribers != null) {
            subscribers.remove(subscriber);
        }
    }
    
    public void publish(String topic, T message) {
        CopyOnWriteArraySet<Subscriber<T>> subscribers = topicSubscribers.get(topic);
        if (subscribers != null && !subscribers.isEmpty()) {
            publisherExecutor.submit(() -> {
                for (Subscriber<T> subscriber : subscribers) {
                    try {
                        subscriber.onMessage(message);
                    } catch (Exception e) {
                        // Log error
                    }
                }
            });
        }
    }
    
    public void shutdown() {
        publisherExecutor.shutdown();
    }
}
```

## 6. **Parking Lot (Thread-Safe)**

**Use Case**: Multi-level parking with concurrent entry/exit

java

```java
public enum VehicleType {
    MOTORCYCLE, CAR, TRUCK
}

public class ParkingSpot {
    private final int id;
    private final VehicleType type;
    private volatile boolean isOccupied;
    private final ReentrantLock lock = new ReentrantLock();
    
    public ParkingSpot(int id, VehicleType type) {
        this.id = id;
        this.type = type;
        this.isOccupied = false;
    }
    
    public boolean occupy() {
        lock.lock();
        try {
            if (!isOccupied) {
                isOccupied = true;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    public void vacate() {
        lock.lock();
        try {
            isOccupied = false;
        } finally {
            lock.unlock();
        }
    }
    
    public boolean isAvailable() {
        return !isOccupied;
    }
    
    public VehicleType getType() {
        return type;
    }
}

public class ParkingLot {
    private final ConcurrentHashMap<VehicleType, CopyOnWriteArrayList<ParkingSpot>> spotsByType;
    private final ConcurrentHashMap<String, ParkingSpot> activeTickets;
    private final AtomicInteger ticketCounter = new AtomicInteger(0);
    
    public ParkingLot() {
        spotsByType = new ConcurrentHashMap<>();
        activeTickets = new ConcurrentHashMap<>();
        
        for (VehicleType type : VehicleType.values()) {
            spotsByType.put(type, new CopyOnWriteArrayList<>());
        }
    }
    
    public void addParkingSpot(ParkingSpot spot) {
        spotsByType.get(spot.getType()).add(spot);
    }
    
    public String parkVehicle(VehicleType type) {
        CopyOnWriteArrayList<ParkingSpot> spots = spotsByType.get(type);
        
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable() && spot.occupy()) {
                String ticket = "TICKET-" + ticketCounter.incrementAndGet();
                activeTickets.put(ticket, spot);
                return ticket;
            }
        }
        
        return null; // No spot available
    }
    
    public boolean unparkVehicle(String ticket) {
        ParkingSpot spot = activeTickets.remove(ticket);
        if (spot != null) {
            spot.vacate();
            return true;
        }
        return false;
    }
    
    public int getAvailableSpots(VehicleType type) {
        return (int) spotsByType.get(type).stream()
                                .filter(ParkingSpot::isAvailable)
                                .count();
    }
}
```

## 7. **Task Scheduler**

**Use Case**: Cron-like job scheduler, delayed task execution

java

```java
public class TaskScheduler {
    private static class ScheduledTask implements Comparable<ScheduledTask> {
        Runnable task;
        long executeAt;
        long interval; // 0 for one-time tasks
        
        ScheduledTask(Runnable task, long executeAt, long interval) {
            this.task = task;
            this.executeAt = executeAt;
            this.interval = interval;
        }
        
        @Override
        public int compareTo(ScheduledTask other) {
            return Long.compare(this.executeAt, other.executeAt);
        }
    }
    
    private final PriorityBlockingQueue<ScheduledTask> taskQueue;
    private final ExecutorService executor;
    private final Thread schedulerThread;
    private volatile boolean running = true;
    
    public TaskScheduler(int workerThreads) {
        this.taskQueue = new PriorityBlockingQueue<>();
        this.executor = Executors.newFixedThreadPool(workerThreads);
        this.schedulerThread = new Thread(this::run);
        this.schedulerThread.start();
    }
    
    public void scheduleOnce(Runnable task, long delayMillis) {
        long executeAt = System.currentTimeMillis() + delayMillis;
        taskQueue.offer(new ScheduledTask(task, executeAt, 0));
    }
    
    public void scheduleRecurring(Runnable task, long initialDelayMillis, long intervalMillis) {
        long executeAt = System.currentTimeMillis() + initialDelayMillis;
        taskQueue.offer(new ScheduledTask(task, executeAt, intervalMillis));
    }
    
    private void run() {
        while (running) {
            try {
                ScheduledTask task = taskQueue.peek();
                
                if (task == null) {
                    Thread.sleep(100);
                    continue;
                }
                
                long now = System.currentTimeMillis();
                if (task.executeAt <= now) {
                    taskQueue.poll();
                    executor.submit(task.task);
                    
                    // Reschedule if recurring
                    if (task.interval > 0) {
                        task.executeAt = now + task.interval;
                        taskQueue.offer(task);
                    }
                } else {
                    Thread.sleep(Math.min(100, task.executeAt - now));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public void shutdown() {
        running = false;
        schedulerThread.interrupt();
        executor.shutdown();
    }
}
```

## 8. **Read-Write Lock Based File System**

**Use Case**: Concurrent file reads, exclusive writes

java

```java
public class FileSystem {
    private static class File {
        String content;
        ReadWriteLock lock = new ReentrantReadWriteLock();
        
        File(String content) {
            this.content = content;
        }
    }
    
    private final ConcurrentHashMap<String, File> files;
    
    public FileSystem() {
        this.files = new ConcurrentHashMap<>();
    }
    
    public void createFile(String path, String content) {
        files.putIfAbsent(path, new File(content));
    }
    
    public String read(String path) {
        File file = files.get(path);
        if (file == null) return null;
        
        file.lock.readLock().lock();
        try {
            return file.content;
        } finally {
            file.lock.readLock().unlock();
        }
    }
    
    public boolean write(String path, String content) {
        File file = files.get(path);
        if (file == null) return false;
        
        file.lock.writeLock().lock();
        try {
            file.content = content;
            return true;
        } finally {
            file.lock.writeLock().unlock();
        }
    }
    
    public boolean append(String path, String content) {
        File file = files.get(path);
        if (file == null) return false;
        
        file.lock.writeLock().lock();
        try {
            file.content += content;
            return true;
        } finally {
            file.lock.writeLock().unlock();
        }
    }
}
```

## 9. **Dining Philosophers (Deadlock Prevention)**

**Use Case**: Resource allocation, deadlock handling

java

```java
public class DiningPhilosophers {
    private final ReentrantLock[] forks;
    private final int numPhilosophers;
    
    public DiningPhilosophers(int numPhilosophers) {
        this.numPhilosophers = numPhilosophers;
        this.forks = new ReentrantLock[numPhilosophers];
        for (int i = 0; i < numPhilosophers; i++) {
            forks[i] = new ReentrantLock();
        }
    }
    
    public void philosopher(int id) throws InterruptedException {
        int leftFork = id;
        int rightFork = (id + 1) % numPhilosophers;
        
        // Prevent deadlock: lower numbered fork first
        int firstFork = Math.min(leftFork, rightFork);
        int secondFork = Math.max(leftFork, rightFork);
        
        while (true) {
            // Think
            Thread.sleep((long) (Math.random() * 1000));
            
            // Try to acquire forks
            if (forks[firstFork].tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    if (forks[secondFork].tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            // Eat
                            System.out.println("Philosopher " + id + " is eating");
                            Thread.sleep((long) (Math.random() * 1000));
                        } finally {
                            forks[secondFork].unlock();
                        }
                    }
                } finally {
                    forks[firstFork].unlock();
                }
            }
        }
    }
}
```

## 10. **Web Crawler**

**Use Case**: Concurrent URL fetching with politeness policy

java

```java
public class WebCrawler {
    private final ConcurrentHashMap<String, AtomicLong> domainLastAccess;
    private final Set<String> visited;
    private final BlockingQueue<String> urlQueue;
    private final ExecutorService workers;
    private final long politenessDelayMs;
    private final ReentrantLock visitedLock = new ReentrantLock();
    
    public WebCrawler(int numWorkers, long politenessDelayMs) {
        this.domainLastAccess = new ConcurrentHashMap<>();
        this.visited = new HashSet<>();
        this.urlQueue = new LinkedBlockingQueue<>();
        this.workers = Executors.newFixedThreadPool(numWorkers);
        this.politenessDelayMs = politenessDelayMs;
    }
    
    public void crawl(String startUrl) {
        urlQueue.offer(startUrl);
        
        for (int i = 0; i < 5; i++) {
            workers.submit(this::worker);
        }
    }
    
    private void worker() {
        while (true) {
            try {
                String url = urlQueue.poll(1, TimeUnit.SECONDS);
                if (url == null) continue;
                
                if (!shouldVisit(url)) continue;
                
                String domain = extractDomain(url);
                waitForPoliteness(domain);
                
                // Fetch and process URL
                processUrl(url);
                
                domainLastAccess.put(domain, new AtomicLong(System.currentTimeMillis()));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private boolean shouldVisit(String url) {
        visitedLock.lock();
        try {
            if (visited.contains(url)) {
                return false;
            }
            visited.add(url);
            return true;
        } finally {
            visitedLock.unlock();
        }
    }
    
    private void waitForPoliteness(String domain) throws InterruptedException {
        AtomicLong lastAccess = domainLastAccess.get(domain);
        if (lastAccess != null) {
            long timeSinceLastAccess = System.currentTimeMillis() - lastAccess.get();
            if (timeSinceLastAccess < politenessDelayMs) {
                Thread.sleep(politenessDelayMs - timeSinceLastAccess);
            }
        }
    }
    
    private String extractDomain(String url) {
        // Simplified domain extraction
        return url.split("/")[2];
    }
    
    private void processUrl(String url) {
        // Fetch content and extract new URLs
        System.out.println("Crawling: " + url);
    }
    
    public void shutdown() {
        workers.shutdown();
    }
}
```


# Java Code for Remaining LLD Problems

## 11. **LRU Cache**

java

```java
public class LRUCache<K, V> {
    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;
        
        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private final int capacity;
    private final ConcurrentHashMap<K, Node<K, V>> cache;
    private Node<K, V> head;
    private Node<K, V> tail;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>();
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }
    
    public V get(K key) {
        lock.readLock().lock();
        try {
            Node<K, V> node = cache.get(key);
            if (node == null) {
                return null;
            }
            moveToHead(node);
            return node.value;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            Node<K, V> node = cache.get(key);
            
            if (node != null) {
                node.value = value;
                moveToHead(node);
            } else {
                Node<K, V> newNode = new Node<>(key, value);
                cache.put(key, newNode);
                addToHead(newNode);
                
                if (cache.size() > capacity) {
                    Node<K, V> removed = removeTail();
                    cache.remove(removed.key);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addToHead(node);
    }
    
    private void addToHead(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private Node<K, V> removeTail() {
        Node<K, V> node = tail.prev;
        removeNode(node);
        return node;
    }
    
    public int size() {
        return cache.size();
    }
}
```

## 12. **Ride Sharing System**

java

```java
public enum RideStatus {
    REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
}

public class Location {
    double latitude;
    double longitude;
    
    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public double distanceTo(Location other) {
        return Math.sqrt(Math.pow(latitude - other.latitude, 2) + 
                        Math.pow(longitude - other.longitude, 2));
    }
}

public class Driver {
    String id;
    String name;
    volatile Location currentLocation;
    volatile boolean available;
    
    public Driver(String id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.currentLocation = location;
        this.available = true;
    }
}

public class Rider {
    String id;
    String name;
    
    public Rider(String id, String name) {
        this.id = id;
        this.name = name;
    }
}

public class Ride {
    String id;
    Rider rider;
    Driver driver;
    Location pickupLocation;
    Location dropLocation;
    volatile RideStatus status;
    long requestTime;
    
    public Ride(String id, Rider rider, Location pickup, Location drop) {
        this.id = id;
        this.rider = rider;
        this.pickupLocation = pickup;
        this.dropLocation = drop;
        this.status = RideStatus.REQUESTED;
        this.requestTime = System.currentTimeMillis();
    }
}

public class RideSharingSystem {
    private final ConcurrentHashMap<String, Driver> drivers;
    private final ConcurrentHashMap<String, Ride> activeRides;
    private final AtomicInteger rideCounter = new AtomicInteger(0);
    private final ExecutorService matchingExecutor;
    
    public RideSharingSystem() {
        this.drivers = new ConcurrentHashMap<>();
        this.activeRides = new ConcurrentHashMap<>();
        this.matchingExecutor = Executors.newFixedThreadPool(5);
    }
    
    public void addDriver(Driver driver) {
        drivers.put(driver.id, driver);
    }
    
    public String requestRide(Rider rider, Location pickup, Location drop) {
        String rideId = "RIDE-" + rideCounter.incrementAndGet();
        Ride ride = new Ride(rideId, rider, pickup, drop);
        activeRides.put(rideId, ride);
        
        matchingExecutor.submit(() -> matchDriver(ride));
        
        return rideId;
    }
    
    private void matchDriver(Ride ride) {
        Driver nearestDriver = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Driver driver : drivers.values()) {
            if (driver.available) {
                double distance = driver.currentLocation.distanceTo(ride.pickupLocation);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestDriver = driver;
                }
            }
        }
        
        if (nearestDriver != null) {
            synchronized (nearestDriver) {
                if (nearestDriver.available) {
                    nearestDriver.available = false;
                    ride.driver = nearestDriver;
                    ride.status = RideStatus.ACCEPTED;
                    System.out.println("Ride " + ride.id + " matched with driver " + nearestDriver.id);
                }
            }
        }
    }
    
    public void startRide(String rideId) {
        Ride ride = activeRides.get(rideId);
        if (ride != null && ride.status == RideStatus.ACCEPTED) {
            ride.status = RideStatus.IN_PROGRESS;
        }
    }
    
    public void completeRide(String rideId) {
        Ride ride = activeRides.get(rideId);
        if (ride != null && ride.status == RideStatus.IN_PROGRESS) {
            ride.status = RideStatus.COMPLETED;
            ride.driver.available = true;
            activeRides.remove(rideId);
        }
    }
    
    public void cancelRide(String rideId) {
        Ride ride = activeRides.get(rideId);
        if (ride != null) {
            ride.status = RideStatus.CANCELLED;
            if (ride.driver != null) {
                ride.driver.available = true;
            }
            activeRides.remove(rideId);
        }
    }
    
    public Ride getRideStatus(String rideId) {
        return activeRides.get(rideId);
    }
    
    public void updateDriverLocation(String driverId, Location location) {
        Driver driver = drivers.get(driverId);
        if (driver != null) {
            driver.currentLocation = location;
        }
    }
    
    public void shutdown() {
        matchingExecutor.shutdown();
    }
}
```

## 13. **Stock Exchange/Order Matching System**

java

```java
public enum OrderType {
    BUY, SELL
}

public enum OrderStatus {
    PENDING, PARTIALLY_FILLED, FILLED, CANCELLED
}

public class Order implements Comparable<Order> {
    String id;
    String symbol;
    OrderType type;
    double price;
    int quantity;
    int filledQuantity;
    OrderStatus status;
    long timestamp;
    
    public Order(String id, String symbol, OrderType type, double price, int quantity) {
        this.id = id;
        this.symbol = symbol;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.filledQuantity = 0;
        this.status = OrderStatus.PENDING;
        this.timestamp = System.currentTimeMillis();
    }
    
    public int getRemainingQuantity() {
        return quantity - filledQuantity;
    }
    
    @Override
    public int compareTo(Order other) {
        if (this.type == OrderType.BUY) {
            // Buy orders: higher price first, then earlier timestamp
            int priceCompare = Double.compare(other.price, this.price);
            return priceCompare != 0 ? priceCompare : Long.compare(this.timestamp, other.timestamp);
        } else {
            // Sell orders: lower price first, then earlier timestamp
            int priceCompare = Double.compare(this.price, other.price);
            return priceCompare != 0 ? priceCompare : Long.compare(this.timestamp, other.timestamp);
        }
    }
}

public class Trade {
    String id;
    String buyOrderId;
    String sellOrderId;
    String symbol;
    double price;
    int quantity;
    long timestamp;
    
    public Trade(String buyOrderId, String sellOrderId, String symbol, double price, int quantity) {
        this.id = UUID.randomUUID().toString();
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }
}

public class OrderBook {
    private final String symbol;
    private final PriorityBlockingQueue<Order> buyOrders;
    private final PriorityBlockingQueue<Order> sellOrders;
    private final ConcurrentHashMap<String, Order> activeOrders;
    private final ReentrantLock matchingLock = new ReentrantLock();
    private final List<Trade> trades;
    
    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.buyOrders = new PriorityBlockingQueue<>();
        this.sellOrders = new PriorityBlockingQueue<>();
        this.activeOrders = new ConcurrentHashMap<>();
        this.trades = new CopyOnWriteArrayList<>();
    }
    
    public void addOrder(Order order) {
        activeOrders.put(order.id, order);
        
        if (order.type == OrderType.BUY) {
            buyOrders.offer(order);
        } else {
            sellOrders.offer(order);
        }
        
        matchOrders();
    }
    
    private void matchOrders() {
        matchingLock.lock();
        try {
            while (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
                Order buyOrder = buyOrders.peek();
                Order sellOrder = sellOrders.peek();
                
                if (buyOrder.status == OrderStatus.FILLED || buyOrder.status == OrderStatus.CANCELLED) {
                    buyOrders.poll();
                    continue;
                }
                
                if (sellOrder.status == OrderStatus.FILLED || sellOrder.status == OrderStatus.CANCELLED) {
                    sellOrders.poll();
                    continue;
                }
                
                // Check if orders can match
                if (buyOrder.price >= sellOrder.price) {
                    int matchedQuantity = Math.min(buyOrder.getRemainingQuantity(), 
                                                   sellOrder.getRemainingQuantity());
                    double matchedPrice = sellOrder.price; // Price priority to seller
                    
                    // Execute trade
                    executeTrade(buyOrder, sellOrder, matchedPrice, matchedQuantity);
                    
                    // Update order status
                    updateOrderStatus(buyOrder);
                    updateOrderStatus(sellOrder);
                    
                    // Remove filled orders from queues
                    if (buyOrder.status == OrderStatus.FILLED) {
                        buyOrders.poll();
                    }
                    if (sellOrder.status == OrderStatus.FILLED) {
                        sellOrders.poll();
                    }
                } else {
                    break; // No more matches possible
                }
            }
        } finally {
            matchingLock.unlock();
        }
    }
    
    private void executeTrade(Order buyOrder, Order sellOrder, double price, int quantity) {
        buyOrder.filledQuantity += quantity;
        sellOrder.filledQuantity += quantity;
        
        Trade trade = new Trade(buyOrder.id, sellOrder.id, symbol, price, quantity);
        trades.add(trade);
        
        System.out.println("Trade executed: " + quantity + " shares at $" + price);
    }
    
    private void updateOrderStatus(Order order) {
        if (order.filledQuantity == order.quantity) {
            order.status = OrderStatus.FILLED;
        } else if (order.filledQuantity > 0) {
            order.status = OrderStatus.PARTIALLY_FILLED;
        }
    }
    
    public boolean cancelOrder(String orderId) {
        Order order = activeOrders.get(orderId);
        if (order != null && order.status != OrderStatus.FILLED) {
            order.status = OrderStatus.CANCELLED;
            activeOrders.remove(orderId);
            return true;
        }
        return false;
    }
    
    public Order getOrder(String orderId) {
        return activeOrders.get(orderId);
    }
    
    public List<Trade> getTrades() {
        return new ArrayList<>(trades);
    }
}

public class StockExchange {
    private final ConcurrentHashMap<String, OrderBook> orderBooks;
    private final AtomicInteger orderCounter = new AtomicInteger(0);
    
    public StockExchange() {
        this.orderBooks = new ConcurrentHashMap<>();
    }
    
    public String placeOrder(String symbol, OrderType type, double price, int quantity) {
        OrderBook orderBook = orderBooks.computeIfAbsent(symbol, OrderBook::new);
        
        String orderId = "ORDER-" + orderCounter.incrementAndGet();
        Order order = new Order(orderId, symbol, type, price, quantity);
        
        orderBook.addOrder(order);
        
        return orderId;
    }
    
    public boolean cancelOrder(String symbol, String orderId) {
        OrderBook orderBook = orderBooks.get(symbol);
        return orderBook != null && orderBook.cancelOrder(orderId);
    }
    
    public Order getOrderStatus(String symbol, String orderId) {
        OrderBook orderBook = orderBooks.get(symbol);
        return orderBook != null ? orderBook.getOrder(orderId) : null;
    }
    
    public List<Trade> getTradeHistory(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        return orderBook != null ? orderBook.getTrades() : new ArrayList<>();
    }
}
```

## 14. **Hotel Booking System**

java

```java
public enum RoomType {
    SINGLE, DOUBLE, SUITE
}

public class Room {
    String id;
    RoomType type;
    double pricePerNight;
    volatile boolean available;
    
    public Room(String id, RoomType type, double pricePerNight) {
        this.id = id;
        this.type = type;
        this.pricePerNight = pricePerNight;
        this.available = true;
    }
}

public class Booking {
    String id;
    String guestName;
    Room room;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    double totalPrice;
    volatile boolean confirmed;
    
    public Booking(String id, String guestName, Room room, 
                   LocalDate checkIn, LocalDate checkOut) {
        this.id = id;
        this.guestName = guestName;
        this.room = room;
        this.checkInDate = checkIn;
        this.checkOutDate = checkOut;
        this.totalPrice = calculatePrice();
        this.confirmed = false;
    }
    
    private double calculatePrice() {
        long days = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        return days * room.pricePerNight;
    }
}

public class HotelBookingSystem {
    private final ConcurrentHashMap<String, Room> rooms;
    private final ConcurrentHashMap<String, Booking> bookings;
    // Map of room ID to set of booked date ranges
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<DateRange>> roomSchedule;
    private final AtomicInteger bookingCounter = new AtomicInteger(0);
    private final ReentrantReadWriteLock scheduleLock = new ReentrantReadWriteLock();
    
    private static class DateRange implements Comparable<DateRange> {
        LocalDate start;
        LocalDate end;
        
        DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
        
        boolean overlaps(DateRange other) {
            return !this.end.isBefore(other.start) && !other.end.isBefore(this.start);
        }
        
        @Override
        public int compareTo(DateRange other) {
            return this.start.compareTo(other.start);
        }
    }
    
    public HotelBookingSystem() {
        this.rooms = new ConcurrentHashMap<>();
        this.bookings = new ConcurrentHashMap<>();
        this.roomSchedule = new ConcurrentHashMap<>();
    }
    
    public void addRoom(Room room) {
        rooms.put(room.id, room);
        roomSchedule.put(room.id, new ConcurrentSkipListSet<>());
    }
    
    public List<Room> searchAvailableRooms(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        List<Room> availableRooms = new ArrayList<>();
        DateRange requestedRange = new DateRange(checkIn, checkOut);
        
        scheduleLock.readLock().lock();
        try {
            for (Room room : rooms.values()) {
                if (room.type == type && isRoomAvailable(room.id, requestedRange)) {
                    availableRooms.add(room);
                }
            }
        } finally {
            scheduleLock.readLock().unlock();
        }
        
        return availableRooms;
    }
    
    private boolean isRoomAvailable(String roomId, DateRange requestedRange) {
        ConcurrentSkipListSet<DateRange> schedule = roomSchedule.get(roomId);
        if (schedule == null) return true;
        
        for (DateRange bookedRange : schedule) {
            if (bookedRange.overlaps(requestedRange)) {
                return false;
            }
        }
        return true;
    }
    
    public String bookRoom(String roomId, String guestName, 
                          LocalDate checkIn, LocalDate checkOut) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return null;
        }
        
        DateRange requestedRange = new DateRange(checkIn, checkOut);
        
        scheduleLock.writeLock().lock();
        try {
            if (!isRoomAvailable(roomId, requestedRange)) {
                return null; // Room not available
            }
            
            String bookingId = "BOOKING-" + bookingCounter.incrementAndGet();
            Booking booking = new Booking(bookingId, guestName, room, checkIn, checkOut);
            
            bookings.put(bookingId, booking);
            roomSchedule.get(roomId).add(requestedRange);
            booking.confirmed = true;
            
            return bookingId;
        } finally {
            scheduleLock.writeLock().unlock();
        }
    }
    
    public boolean cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            return false;
        }
        
        scheduleLock.writeLock().lock();
        try {
            DateRange range = new DateRange(booking.checkInDate, booking.checkOutDate);
            roomSchedule.get(booking.room.id).remove(range);
            bookings.remove(bookingId);
            return true;
        } finally {
            scheduleLock.writeLock().unlock();
        }
    }
    
    public Booking getBooking(String bookingId) {
        return bookings.get(bookingId);
    }
}
```

## 15. **Elevator System**

java

```java
public enum Direction {
    UP, DOWN, IDLE
}

public class Request {
    int floor;
    Direction direction;
    long timestamp;
    
    public Request(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
        this.timestamp = System.currentTimeMillis();
    }
}

public class Elevator {
    private final int id;
    private final int maxFloor;
    private volatile int currentFloor;
    private volatile Direction direction;
    private final TreeSet<Integer> upRequests;
    private final TreeSet<Integer> downRequests;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean running = true;
    
    public Elevator(int id, int maxFloor) {
        this.id = id;
        this.maxFloor = maxFloor;
        this.currentFloor = 0;
        this.direction = Direction.IDLE;
        this.upRequests = new TreeSet<>();
        this.downRequests = new TreeSet<>();
    }
    
    public void addRequest(int floor, Direction requestDirection) {
        lock.lock();
        try {
            if (floor > currentFloor) {
                upRequests.add(floor);
            } else if (floor < currentFloor) {
                downRequests.add(floor);
            }
        } finally {
            lock.unlock();
        }
    }
    
    public void run() {
        while (running) {
            lock.lock();
            try {
                if (direction == Direction.UP || direction == Direction.IDLE) {
                    if (!upRequests.isEmpty()) {
                        direction = Direction.UP;
                        int nextFloor = upRequests.first();
                        moveToFloor(nextFloor);
                        upRequests.remove(nextFloor);
                    } else if (!downRequests.isEmpty()) {
                        direction = Direction.DOWN;
                        int nextFloor = downRequests.last();
                        moveToFloor(nextFloor);
                        downRequests.remove(nextFloor);
                    } else {
                        direction = Direction.IDLE;
                    }
                } else if (direction == Direction.DOWN) {
                    if (!downRequests.isEmpty()) {
                        int nextFloor = downRequests.last();
                        moveToFloor(nextFloor);
                        downRequests.remove(nextFloor);
                    } else if (!upRequests.isEmpty()) {
                        direction = Direction.UP;
                        int nextFloor = upRequests.first();
                        moveToFloor(nextFloor);
                        upRequests.remove(nextFloor);
                    } else {
                        direction = Direction.IDLE;
                    }
                }
            } finally {
                lock.unlock();
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void moveToFloor(int floor) {
        System.out.println("Elevator " + id + " moving from " + currentFloor + " to " + floor);
        currentFloor = floor;
        System.out.println("Elevator " + id + " arrived at floor " + floor);
    }
    
    public int getCurrentFloor() {
        return currentFloor;
    }
    
    public Direction getDirection() {
        return direction;
    }
    
    public void stop() {
        running = false;
    }
}

public class ElevatorController {
    private final List<Elevator> elevators;
    private final ExecutorService executorService;
    private final int maxFloor;
    
    public ElevatorController(int numElevators, int maxFloor) {
        this.maxFloor = maxFloor;
        this.elevators = new ArrayList<>();
        this.executorService = Executors.newFixedThreadPool(numElevators);
        
        for (int i = 0; i < numElevators; i++) {
            Elevator elevator = new Elevator(i, maxFloor);
            elevators.add(elevator);
            executorService.submit(elevator::run);
        }
    }
    
    public void requestElevator(int floor, Direction direction) {
        Elevator bestElevator = findBestElevator(floor, direction);
        if (bestElevator != null) {
            bestElevator.addRequest(floor, direction);
        }
    }
    
    private Elevator findBestElevator(int floor, Direction direction) {
        Elevator best = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (Elevator elevator : elevators) {
            int distance = Math.abs(elevator.getCurrentFloor() - floor);
            
            // Prefer elevators moving in the same direction
            if (elevator.getDirection() == direction || elevator.getDirection() == Direction.IDLE) {
                if (distance < minDistance) {
                    minDistance = distance;
                    best = elevator;
                }
            }
        }
        
        // If no elevator in same direction, pick nearest
        if (best == null) {
            for (Elevator elevator : elevators) {
                int distance = Math.abs(elevator.getCurrentFloor() - floor);
                if (distance < minDistance) {
                    minDistance = distance;
                    best = elevator;
                }
            }
        }
        
        return best;
    }
    
    public void shutdown() {
        for (Elevator elevator : elevators) {
            elevator.stop();
        }
        executorService.shutdown();
    }
}
```

## 16. **Ticket Booking System (BookMyShow)**

java

```java
public class Seat {
    String id;
    int row;
    int col;
    double price;
    volatile SeatStatus status;
    volatile String lockedBy;
    volatile long lockExpiry;
    
    public enum SeatStatus {
        AVAILABLE, LOCKED, BOOKED
    }
    
    public Seat(String id, int row, int col, double price) {
        this.id = id;
        this.row = row;
        this.col = col;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }
}

public class Show {
    String id;
    String movieName;
    LocalDateTime showTime;
    List<Seat> seats;
    
    public Show(String id, String movieName, LocalDateTime showTime, int rows, int cols) {
        this.id = id;
        this.movieName = movieName;
        this.showTime = showTime;
        this.seats = new ArrayList<>();
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                seats.add(new Seat("S" + i + "-" + j, i, j, 200.0));
            }
        }
    }
}

public class Booking {
    String id;
    String userId;
    Show show;
    List<Seat> seats;
    double totalPrice;
    LocalDateTime bookingTime;
    
    public Booking(String id, String userId, Show show, List<Seat> seats) {
        this.id = id;
        this.userId = userId;
        this.show = show;
        this.seats = seats;
        this.totalPrice = seats.stream().mapToDouble(s -> s.price).sum();
        this.bookingTime = LocalDateTime.now();
    }
}

public class TicketBookingSystem {
    private final ConcurrentHashMap<String, Show> shows;
    private final ConcurrentHashMap<String, Booking> bookings;
    private final ConcurrentHashMap<String, ReentrantLock> seatLocks;
    private final AtomicInteger bookingCounter = new AtomicInteger(0);
    private final ScheduledExecutorService lockCleanupExecutor;
    private static final long LOCK_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
    
    public TicketBookingSystem() {
        this.shows = new ConcurrentHashMap<>();
        this.bookings = new ConcurrentHashMap<>();
        this.seatLocks = new ConcurrentHashMap<>();
        this.lockCleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Cleanup expired locks every minute
        lockCleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredLocks, 1, 1, TimeUnit.MINUTES
        );
    }
    
    public void addShow(Show show) {
        shows.put(show.id, show);
        for (Seat seat : show.seats) {
            seatLocks.put(show.id + "-" + seat.id, new ReentrantLock());
        }
    }
    
    public List<Seat> getAvailableSeats(String showId) {
        Show show = shows.get(showId);
        if (show == null) return new ArrayList<>();
        
        return show.seats.stream()
            .filter(s -> s.status == Seat.SeatStatus.AVAILABLE)
            .collect(Collectors.toList());
    }
    
    public boolean lockSeats(String showId, List<String> seatIds, String userId) {
        Show show = shows.get(showId);
        if (show == null) return false;
        
        List<Seat> seatsToLock = new ArrayList<>();
        List<ReentrantLock> locks = new ArrayList<>();
        
        // Collect seats and locks
        for (String seatId : seatIds) {
            Seat seat = show.seats.stream()
                .filter(s -> s.id.equals(seatId))
                .findFirst()
                .orElse(null);
            
            if (seat == null) return false;
            
            seatsToLock.add(seat);
            locks.add(seatLocks.get(showId + "-" + seatId));
        }
        
        // Acquire all locks
        for (ReentrantLock lock : locks) {
            lock.lock();
        }
        
        try {
            // Check if all seats are available
            for (Seat seat : seatsToLock) {
                if (seat.status != Seat.SeatStatus.AVAILABLE) {
                    return false;
                }
            }
            
            // Lock all seats
            long lockExpiry = System.currentTimeMillis() + LOCK_TIMEOUT_MS;
            for (Seat seat : seatsToLock) {
                seat.status = Seat.SeatStatus.LOCKED;
                seat.lockedBy = userId;
                seat.lockExpiry = lockExpiry;
            }
            
            return true;
        } finally {
            for (ReentrantLock lock : locks) {
                lock.unlock();
            }
        }
    }
    
    public String bookSeats(String showId, List<String> seatIds, String userId) {
        Show show = shows.get(showId);
        if (show == null) return null;
        
        List<Seat> seatsToBook = new ArrayList<>();
        List<ReentrantLock> locks = new ArrayList<>();
        
        for (String seatId : seatIds) {
            Seat seat = show.seats.stream()
                .filter(s -> s.id.equals(seatId))
                .findFirst()
                .orElse(null);
            
            if (seat == null) return null;
            
            seatsToBook.add(seat);
            locks.add(seatLocks.get(showId + "-" + seatId));
        }
        
        for (ReentrantLock lock : locks) {
            lock.lock();
        }
        
        try {
            // Verify seats are locked by this user
            for (Seat seat : seatsToBook) {
                if (seat.status != Seat.SeatStatus.LOCKED || 
                    !userId.equals(seat.lockedBy)) {
                    return null;
                }
            }
            
            // Book seats
            for (Seat seat : seatsToBook) {
                seat.status = Seat.SeatStatus.BOOKED;
                seat.lockedBy = null;
                seat.lockExpiry = 0;
            }
            
            String bookingId = "BOOKING-" + bookingCounter.incrementAndGet();
            Booking booking = new Booking(bookingId, userId, show, seatsToBook);
            bookings.put(bookingId, booking);
            
            return bookingId;
        } finally {
            for (ReentrantLock lock : locks) {
                lock.unlock();
            }
        }
    }
    
    public boolean unlockSeats(String showId, List<String> seatIds, String userId) {
        Show show = shows.get(showId);
        if (show == null) return false;
        
        List<Seat> seatsToUnlock = new ArrayList<>();
        List<ReentrantLock> locks = new ArrayList<>();
        
        for (String seatId : seatIds) {
            Seat seat = show.seats.stream()
                .filter(s -> s.id.equals(seatId))
                .findFirst()
                .orElse(null);
            
            if (seat == null) return false;
            
            seatsToUnlock.add(seat);
            locks.add(seatLocks.get(showId + "-" + seatId));
        }
        
        for (ReentrantLock lock : locks) {
            lock.lock();
        }
        
        try {
            for (Seat seat : seatsToUnlock) {
                if (seat.status == Seat.SeatStatus.LOCKED && 
                    userId.equals(seat.lockedBy)) {
                    seat.status = Seat.SeatStatus.AVAILABLE;
                    seat.lockedBy = null;
                    seat.lockExpiry = 0;
                }
            }
            return true;
        } finally {
            for (ReentrantLock lock : locks) {
                lock.unlock();
            }
        }
    }
    
    private void cleanupExpiredLocks() {
        long now = System.currentTimeMillis();
        
        for (Show show : shows.values()) {
            for (Seat seat : show.seats) {
                if (seat.status == Seat.SeatStatus.LOCKED && seat.lockExpiry < now) {
                    ReentrantLock lock = seatLocks.get(show.id + "-" + seat.id);
                    lock.lock();
                    try {
                        if (seat.status == Seat.SeatStatus.LOCKED && seat.lockExpiry < now) {
                            seat.status = Seat.SeatStatus.AVAILABLE;
                            seat.lockedBy = null;
                            seat.lockExpiry = 0;
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }
    }
    
    public Booking getBooking(String bookingId) {
        return bookings.get(bookingId);
    }
    
    public void shutdown() {
        lockCleanupExecutor.shutdown();
    }
}
```

## 17. **Vending Machine**

java

```java
public enum ProductType {
    SODA, CHIPS, CANDY, WATER
}

public class Product {
    String id;
    String name;
    ProductType type;
    double price;
    
    public Product(String id, String name, ProductType type, double price) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
    }
}

public class Inventory {
    private final ConcurrentHashMap<String, AtomicInteger> stock;
    private final ConcurrentHashMap<String, Product> products;
    
    public Inventory() {
        this.stock = new ConcurrentHashMap<>();
        this.products = new ConcurrentHashMap<>();
    }
    
    public void addProduct(Product product, int quantity) {
        products.put(product.id, product);
        stock.put(product.id, new AtomicInteger(quantity));
    }
    
    public boolean isAvailable(String productId) {
        AtomicInteger count = stock.get(productId);
        return count != null && count.get() > 0;
    }
    
    public boolean deduct(String productId) {
        AtomicInteger count = stock.get(productId);
        if (count != null) {
            return count.updateAndGet(current -> current > 0 ? current - 1 : current) >= 0;
        }
        return false;
    }
    
    public void restock(String productId, int quantity) {
        AtomicInteger count = stock.get(productId);
        if (count != null) {
            count.addAndGet(quantity);
        }
    }
    
    public int getStock(String productId) {
        AtomicInteger count = stock.get(productId);
        return count != null ? count.get() : 0;
    }
    
    public Product getProduct(String productId) {
        return products.get(productId);
    }
}

public enum VendingMachineState {
    IDLE, ACCEPTING_PAYMENT, DISPENSING
}

public class VendingMachine {
    private final Inventory inventory;
    private final AtomicReference<VendingMachineState> state;
    private final ConcurrentHashMap<String, Transaction> activeTransactions;
    private final ReentrantLock transactionLock = new ReentrantLock();
    
    private static class Transaction {
        String id;
        String productId;
        double amountPaid;
        long startTime;
        
        Transaction(String id, String productId) {
            this.id = id;
            this.productId = productId;
            this.amountPaid = 0;
            this.startTime = System.currentTimeMillis();
        }
    }
    
    public VendingMachine() {
        this.inventory = new Inventory();
        this.state = new AtomicReference<>(VendingMachineState.IDLE);
        this.activeTransactions = new ConcurrentHashMap<>();
    }
    
    public void addProduct(Product product, int quantity) {
        inventory.addProduct(product, quantity);
    }
    
    public String selectProduct(String productId) {
        transactionLock.lock();
        try {
            if (state.get() != VendingMachineState.IDLE) {
                return null;
            }
            
            if (!inventory.isAvailable(productId)) {
                return null;
            }
            
            state.set(VendingMachineState.ACCEPTING_PAYMENT);
            String transactionId = UUID.randomUUID().toString();
            Transaction transaction = new Transaction(transactionId, productId);
            activeTransactions.put(transactionId, transaction);
            
            return transactionId;
        } finally {
            transactionLock.unlock();
        }
    }
    
    public boolean insertMoney(String transactionId, double amount) {
        Transaction transaction = activeTransactions.get(transactionId);
        if (transaction == null) {
            return false;
        }
        
        synchronized (transaction) {
            transaction.amountPaid += amount;
            
            Product product = inventory.getProduct(transaction.productId);
            if (product == null) {
                return false;
            }
            
            if (transaction.amountPaid >= product.price) {
                return dispenseProduct(transactionId);
            }
            
            return true;
        }
    }
    
    private boolean dispenseProduct(String transactionId) {
        Transaction transaction = activeTransactions.get(transactionId);
        if (transaction == null) {
            return false;
        }
        
        transactionLock.lock();
        try {
            state.set(VendingMachineState.DISPENSING);
            
            if (inventory.deduct(transaction.productId)) {
                Product product = inventory.getProduct(transaction.productId);
                double change = transaction.amountPaid - product.price;
                
                System.out.println("Dispensing: " + product.name);
                if (change > 0) {
                    System.out.println("Change: $" + change);
                }
                
                activeTransactions.remove(transactionId);
                state.set(VendingMachineState.IDLE);
                return true;
            }
            
            return false;
        } finally {
            transactionLock.unlock();
        }
    }
    
    public double cancelTransaction(String transactionId) {
        Transaction transaction = activeTransactions.remove(transactionId);
        
        transactionLock.lock();
        try {
            state.set(VendingMachineState.IDLE);
            return transaction != null ? transaction.amountPaid : 0;
        } finally {
            transactionLock.unlock();
        }
    }
    
    public int getStock(String productId) {
        return inventory.getStock(productId);
    }
    
    public void restock(String productId, int quantity) {
        inventory.restock(productId, quantity);
    }
}
```

## 18. **ATM System**

java

```java
public class Account {
    private final String accountNumber;
    private volatile double balance;
    private final ReentrantLock lock = new ReentrantLock();
    
    public Account(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }
    
    public boolean withdraw(double amount) {
        lock.lock();
        try {
            if (balance >= amount) {
                balance -= amount;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    public void deposit(double amount) {
        lock.lock();
        try {
            balance += amount;
        } finally {
            lock.unlock();
        }
    }
    
    public double getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
}

public class Card {
    private final String cardNumber;
    private final String pin;
    private final String accountNumber;
    private int failedAttempts;
    private boolean blocked;
    private final ReentrantLock lock = new ReentrantLock();
    
    public Card(String cardNumber, String pin, String accountNumber) {
        this.cardNumber = cardNumber;
        this.pin = pin;
        this.accountNumber = accountNumber;
        this.failedAttempts = 0;
        this.blocked = false;
    }
    
    public boolean validatePin(String inputPin) {
        lock.lock();
        try {
            if (blocked) {
                return false;
            }
            
            if (pin.equals(inputPin)) {
                failedAttempts = 0;
                return true;
            }
            
            failedAttempts++;
            if (failedAttempts >= 3) {
                blocked = true;
            }
            
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public boolean isBlocked() {
        lock.lock();
        try {
            return blocked;
        } finally {
            lock.unlock();
        }
    }
}

public class Transaction {
    enum Type {
        WITHDRAW, DEPOSIT, BALANCE_INQUIRY
    }
    
    String id;
    String accountNumber;
    Type type;
    double amount;
    LocalDateTime timestamp;
    boolean success;
    
    public Transaction(String id, String accountNumber, Type type, double amount) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.success = false;
    }
}

public class ATM {
    private final String atmId;
    private final ConcurrentHashMap<String, Card> cards;
    private final ConcurrentHashMap<String, Account> accounts;
    private final ConcurrentHashMap<String, Transaction> transactions;
    private final AtomicInteger transactionCounter = new AtomicInteger(0);
    private volatile double cashAvailable;
    private final ReentrantLock cashLock = new ReentrantLock();
    
    public ATM(String atmId, double initialCash) {
        this.atmId = atmId;
        this.cashAvailable = initialCash;
        this.cards = new ConcurrentHashMap<>();
        this.accounts = new ConcurrentHashMap<>();
        this.transactions = new ConcurrentHashMap<>();
    }
    
    public void addCard(Card card) {
        cards.put(card.getCardNumber(), card);
    }
    
    public void addAccount(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }
    
    public boolean insertCard(String cardNumber, String pin) {
        Card card = cards.get(cardNumber);
        if (card == null) {
            return false;
        }
        
        return card.validatePin(pin);
    }
    
    public double checkBalance(String cardNumber) {
        Card card = cards.get(cardNumber);
        if (card == null || card.isBlocked()) {
            return -1;
        }
        
        Account account = accounts.get(card.getAccountNumber());
        if (account == null) {
            return -1;
        }
        
        String txnId = "TXN-" + transactionCounter.incrementAndGet();
        Transaction transaction = new Transaction(
            txnId, account.getAccountNumber(), 
            Transaction.Type.BALANCE_INQUIRY, 0
        );
        transaction.success = true;
        transactions.put(txnId, transaction);
        
        return account.getBalance();
    }
    
    public boolean withdraw(String cardNumber, double amount) {
        Card card = cards.get(cardNumber);
        if (card == null || card.isBlocked()) {
            return false;
        }
        
        Account account = accounts.get(card.getAccountNumber());
        if (account == null) {
            return false;
        }
        
        cashLock.lock();
        try {
            if (cashAvailable < amount) {
                return false;
            }
            
            if (account.withdraw(amount)) {
                cashAvailable -= amount;
                
                String txnId = "TXN-" + transactionCounter.incrementAndGet();
                Transaction transaction = new Transaction(
                    txnId, account.getAccountNumber(), 
                    Transaction.Type.WITHDRAW, amount
                );
                transaction.success = true;
                transactions.put(txnId, transaction);
                
                System.out.println("Dispensing $" + amount);
                return true;
            }
            
            return false;
        } finally {
            cashLock.unlock();
        }
    }
    
    public boolean deposit(String cardNumber, double amount) {
        Card card = cards.get(cardNumber);
        if (card == null || card.isBlocked()) {
            return false;
        }
        
        Account account = accounts.get(card.getAccountNumber());
        if (account == null) {
            return false;
        }
        
        cashLock.lock();
        try {
            account.deposit(amount);
            cashAvailable += amount;
            
            String txnId = "TXN-" + transactionCounter.incrementAndGet();
            Transaction transaction = new Transaction(
                txnId, account.getAccountNumber(), 
                Transaction.Type.DEPOSIT, amount
            );
            transaction.success = true;
            transactions.put(txnId, transaction);
            
            return true;
        } finally {
            cashLock.unlock();
        }
    }
    
    public double getCashAvailable() {
        cashLock.lock();
        try {
            return cashAvailable;
        } finally {
            cashLock.unlock();
        }
    }
    
    public List<Transaction> getTransactionHistory(String accountNumber) {
        return transactions.values().stream()
            .filter(t -> t.accountNumber.equals(accountNumber))
            .sorted((t1, t2) -> t2.timestamp.compareTo(t1.timestamp))
            .collect(Collectors.toList());
    }
}
```

## 19. **Logger System**

java

```java
public enum LogLevel {
    DEBUG, INFO, WARN, ERROR
}

public class LogMessage {
    LogLevel level;
    String message;
    String threadName;
    LocalDateTime timestamp;
    
    public LogMessage(LogLevel level, String message) {
        this.level = level;
        this.message = message;
        this.threadName = Thread.currentThread().getName();
        this.timestamp = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return String.format("[%s] [%s] [%s] %s",
            timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            level,
            threadName,
            message);
    }
}

public class LoggerSystem {
    private final BlockingQueue<LogMessage> logQueue;
    private final String logFilePath;
    private final long maxFileSize; // bytes
    private final int maxBackupFiles;
    private volatile long currentFileSize;
    private final ReentrantLock fileLock = new ReentrantLock();
    private final ExecutorService writerExecutor;
    private volatile boolean running = true;
    private final AtomicInteger fileCounter = new AtomicInteger(0);
    
    public LoggerSystem(String logFilePath, long maxFileSize, int maxBackupFiles) {
        this.logQueue = new LinkedBlockingQueue<>();
        this.logFilePath = logFilePath;
        this.maxFileSize = maxFileSize;
        this.maxBackupFiles = maxBackupFiles;
        this.currentFileSize = 0;
        this.writerExecutor = Executors.newSingleThreadExecutor();
        
        writerExecutor.submit(this::processLogs);
    }
    
    public void log(LogLevel level, String message) {
        LogMessage logMessage = new LogMessage(level, message);
        try {
            logQueue.offer(logMessage, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }
    
    public void info(String message) {
        log(LogLevel.INFO, message);
    }
    
    public void warn(String message) {
        log(LogLevel.WARN, message);
    }
    
    public void error(String message) {
        log(LogLevel.ERROR, message);
    }
    
    private void processLogs() {
        while (running) {
            try {
                LogMessage logMessage = logQueue.poll(1, TimeUnit.SECONDS);
                if (logMessage != null) {
                    writeToFile(logMessage);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void writeToFile(LogMessage logMessage) {
        fileLock.lock();
        try {
            String logLine = logMessage.toString() + System.lineSeparator();
            byte[] logBytes = logLine.getBytes();
            
            if (currentFileSize + logBytes.length > maxFileSize) {
                rotateLogFile();
            }
            
            try (FileOutputStream fos = new FileOutputStream(logFilePath, true)) {
                fos.write(logBytes);
                currentFileSize += logBytes.length;
            }
        } catch (IOException e) {
            System.err.println("Error writing log: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }
    
    private void rotateLogFile() throws IOException {
        File currentFile = new File(logFilePath);
        
        // Delete oldest backup if limit reached
        File oldestBackup = new File(logFilePath + "." + maxBackupFiles);
        if (oldestBackup.exists()) {
            oldestBackup.delete();
        }
        
        // Rotate existing backups
        for (int i = maxBackupFiles - 1; i >= 1; i--) {
            File backup = new File(logFilePath + "." + i);
            if (backup.exists()) {
                backup.renameTo(new File(logFilePath + "." + (i + 1)));
            }
        }
        
        // Rename current file to backup
        if (currentFile.exists()) {
            currentFile.renameTo(new File(logFilePath + ".1"));
        }
        
        currentFileSize = 0;
    }
    
    public void flush() {
        while (!logQueue.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public void shutdown() {
        running = false;
        flush();
        writerExecutor.shutdown();
        try {
            writerExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

## 20. **Semaphore-based Resource Pool**

java

```java
public class ResourcePool<T> {
    private final List<T> resources;
    private final Semaphore semaphore;
    private final ConcurrentHashMap<T, Boolean> resourceAvailability;
    private final Queue<T> availableResources;
    private final ReentrantLock lock = new ReentrantLock();
    
    public ResourcePool(List<T> resources) {
        this.resources = new ArrayList<>(resources);
        this.semaphore = new Semaphore(resources.size());
        this.resourceAvailability = new ConcurrentHashMap<>();
        this.availableResources = new ConcurrentLinkedQueue<>(resources);
        
        for (T resource : resources) {
            resourceAvailability.put(resource, true);
        }
    }
    
    public T acquire() throws InterruptedException {
        semaphore.acquire();
        
        lock.lock();
        try {
            T resource = availableResources.poll();
            if (resource != null) {
                resourceAvailability.put(resource, false);
                return resource;
            }
            
            // Should not happen if semaphore count is correct
            semaphore.release();
            return null;
        } finally {
            lock.unlock();
        }
    }
    
    public T tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        if (semaphore.tryAcquire(timeout, unit)) {
            lock.lock();
            try {
                T resource = availableResources.poll();
                if (resource != null) {
                    resourceAvailability.put(resource, false);
                    return resource;
                }
                
                semaphore.release();
                return null;
            } finally {
                lock.unlock();
            }
        }
        return null;
    }
    
    public void release(T resource) {
        lock.lock();
        try {
            if (resources.contains(resource) && !resourceAvailability.get(resource)) {
                resourceAvailability.put(resource, true);
                availableResources.offer(resource);
                semaphore.release();
            }
        } finally {
            lock.unlock();
        }
    }
    
    public int getAvailableCount() {
        return semaphore.availablePermits();
    }
    
    public int getTotalCount() {
        return resources.size();
    }
}

// Example: Thread Pool using Resource Pool
public class WorkerThreadPool {
    private static class Worker {
        int id;
        
        Worker(int id) {
            this.id = id;
        }
        
        void execute(Runnable task) {
            System.out.println("Worker " + id + " executing task");
            task.run();
        }
    }
    
    private final ResourcePool<Worker> workerPool;
    
    public WorkerThreadPool(int numWorkers) {
        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            workers.add(new Worker(i));
        }
        this.workerPool = new ResourcePool<>(workers);
    }
    
    public void executeTask(Runnable task) {
        new Thread(() -> {
            try {
                Worker worker = workerPool.acquire();
                try {
                    worker.execute(task);
                } finally {
                    workerPool.release(worker);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    public int getAvailableWorkers() {
        return workerPool.getAvailableCount();
    }
}
```

## 21. **Distributed ID Generator (Twitter Snowflake)**

java

```java
public class SnowflakeIdGenerator {
    // Bit allocation: 1 sign bit + 41 timestamp + 10 machine + 12 sequence
    private static final long EPOCH = 1609459200000L; // 2021-01-01 00:00:00
    
    private static final long MACHINE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    
    private final long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    private final ReentrantLock lock = new ReentrantLock();
    
    public SnowflakeIdGenerator(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException(
                "Machine ID must be between 0 and " + MAX_MACHINE_ID
            );
        }
        this.machineId = machineId;
    }
    
    public long generateId() {
        lock.lock();
        try {
            long timestamp = getCurrentTimestamp();
            
            if (timestamp < lastTimestamp) {
                throw new RuntimeException("Clock moved backwards!");
            }
            
            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & MAX_SEQUENCE;
                if (sequence == 0) {
                    // Sequence exhausted, wait for next millisecond
                    timestamp = waitNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0L;
            }
            
            lastTimestamp = timestamp;
            
            return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) |
                   (machineId << MACHINE_ID_SHIFT) |
                   sequence;
        } finally {
            lock.unlock();
        }
    }
    
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }
    
    // Parse components from ID
    public static long getTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + EPOCH;
    }
    
    public static long getMachineId(long id) {
        return (id >> MACHINE_ID_SHIFT) & MAX_MACHINE_ID;
    }
    
    public static long getSequence(long id) {
        return id & MAX_SEQUENCE;
    }
}

// Distributed coordinator for machine IDs
public class DistributedIdService {
    private final ConcurrentHashMap<Long, SnowflakeIdGenerator> generators;
    private final AtomicLong machineIdCounter = new AtomicLong(0);
    
    public DistributedIdService() {
        this.generators = new ConcurrentHashMap<>();
    }
    
    public SnowflakeIdGenerator registerMachine() {
        long machineId = machineIdCounter.getAndIncrement();
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(machineId);
        generators.put(machineId, generator);
        return generator;
    }
    
    public void unregisterMachine(long machineId) {
        generators.remove(machineId);
    }
}
```

## 22. **Notification Service**

java

```java
public enum NotificationType {
    EMAIL, SMS, PUSH
}

public class Notification {
    String id;
    NotificationType type;
    String recipient;
    String subject;
    String message;
    int retryCount;
    int maxRetries;
    boolean sent;
    LocalDateTime createdAt;
    
    public Notification(String id, NotificationType type, String recipient, 
                       String subject, String message, int maxRetries) {
        this.id = id;
        this.type = type;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
        this.retryCount = 0;
        this.maxRetries = maxRetries;
        this.sent = false;
        this.createdAt = LocalDateTime.now();
    }
}

public interface NotificationSender {
    boolean send(Notification notification);
}

public class EmailSender implements NotificationSender {
    @Override
    public boolean send(Notification notification) {
        System.out.println("Sending email to: " + notification.recipient);
        // Simulate sending
        return Math.random() > 0.2; // 80% success rate
    }
}

public class SMSSender implements NotificationSender {
    @Override
    public boolean send(Notification notification) {
        System.out.println("Sending SMS to: " + notification.recipient);
        return Math.random() > 0.1; // 90% success rate
    }
}

public class PushSender implements NotificationSender {
    @Override
    public boolean send(Notification notification) {
        System.out.println("Sending push notification to: " + notification.recipient);
        return Math.random() > 0.05; // 95% success rate
    }
}

public class NotificationService {
    private final ConcurrentHashMap<NotificationType, NotificationSender> senders;
    private final BlockingQueue<Notification> notificationQueue;
    private final ConcurrentHashMap<String, Notification> notifications;
    private final ExecutorService workerPool;
    private final ScheduledExecutorService retryScheduler;
    private final TokenBucketRateLimiter rateLimiter;
    private volatile boolean running = true;
    private final AtomicInteger notificationCounter = new AtomicInteger(0);
    
    public NotificationService(int numWorkers, int rateLimit) {
        this.senders = new ConcurrentHashMap<>();
        this.notificationQueue = new LinkedBlockingQueue<>();
        this.notifications = new ConcurrentHashMap<>();
        this.workerPool = Executors.newFixedThreadPool(numWorkers);
        this.retryScheduler = Executors.newScheduledThreadPool(2);
        this.rateLimiter = new TokenBucketRateLimiter(rateLimit, rateLimit);
        
        // Register senders
        senders.put(NotificationType.EMAIL, new EmailSender());
        senders.put(NotificationType.SMS, new SMSSender());
        senders.put(NotificationType.PUSH, new PushSender());
        
        // Start workers
        for (int i = 0; i < numWorkers; i++) {
            workerPool.submit(this::processNotifications);
        }
    }
    
    public String sendNotification(NotificationType type, String recipient, 
                                   String subject, String message) {
        String id = "NOTIF-" + notificationCounter.incrementAndGet();
        Notification notification = new Notification(id, type, recipient, subject, message, 3);
        
        notifications.put(id, notification);
        notificationQueue.offer(notification);
        
        return id;
    }
    
    private void processNotifications() {
        while (running) {
            try {
                Notification notification = notificationQueue.poll(1, TimeUnit.SECONDS);
                if (notification == null) continue;
                
                // Rate limiting
                if (!rateLimiter.allowRequest()) {
                    notificationQueue.offer(notification); // Re-queue
                    Thread.sleep(100);
                    continue;
                }
                
                NotificationSender sender = senders.get(notification.type);
                if (sender != null) {
                    boolean success = sender.send(notification);
                    
                    if (success) {
                        notification.sent = true;
                        System.out.println("Notification " + notification.id + " sent successfully");
                    } else {
                        handleFailure(notification);
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void handleFailure(Notification notification) {
        notification.retryCount++;
        
        if (notification.retryCount < notification.maxRetries) {
            long delaySeconds = (long) Math.pow(2, notification.retryCount); // Exponential backoff
            
            System.out.println("Scheduling retry " + notification.retryCount + 
                             " for notification " + notification.id + 
                             " in " + delaySeconds + " seconds");
            
            retryScheduler.schedule(() -> {
                notificationQueue.offer(notification);
            }, delaySeconds, TimeUnit.SECONDS);
        } else {
            System.out.println("Notification " + notification.id + 
                             " failed after " + notification.maxRetries + " retries");
        }
    }
    
    public Notification getNotificationStatus(String id) {
        return notifications.get(id);
    }
    
    public void shutdown() {
        running = false;
        workerPool.shutdown();
        retryScheduler.shutdown();
    }
}
```

## 23. **Leaderboard System**

java

```java
public class Player {
    String id;
    String name;
    volatile long score;
    LocalDateTime lastUpdated;
    
    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.score = 0;
        this.lastUpdated = LocalDateTime.now();
    }
}

public class LeaderboardSystem {
    private final ConcurrentHashMap<String, Player> players;
    private final ConcurrentSkipListMap<Long, ConcurrentHashMap<String, Player>> scoreIndex;
    private final ReentrantReadWriteLock rankLock = new ReentrantReadWriteLock();
    private final int maxLeaderboardSize;
    
    public LeaderboardSystem(int maxLeaderboardSize) {
        this.players = new ConcurrentHashMap<>();
        this.scoreIndex = new ConcurrentSkipListMap<>(Collections.reverseOrder());
        this.maxLeaderboardSize = maxLeaderboardSize;
    }
    
    public void addPlayer(Player player) {
        players.put(player.id, player);
        updateScoreIndex(player, 0, player.score);
    }
    
    public void updateScore(String playerId, long scoreIncrement) {
        Player player = players.get(playerId);
        if (player == null) return;
        
        rankLock.writeLock().lock();
        try {
            long oldScore = player.score;
            player.score += scoreIncrement;
            player.lastUpdated = LocalDateTime.now();
            
            updateScoreIndex(player, oldScore, player.score);
        } finally {
            rankLock.writeLock().unlock();
        }
    }
    
    public void setScore(String playerId, long newScore) {
        Player player = players.get(playerId);
        if (player == null) return;
        
        rankLock.writeLock().lock();
        try {
            long oldScore = player.score;
            player.score = newScore;
            player.lastUpdated = LocalDateTime.now();
            
            updateScoreIndex(player, oldScore, player.score);
        } finally {
            rankLock.writeLock().unlock();
        }
    }
    
    private void updateScoreIndex(Player player, long oldScore, long newScore) {
        // Remove from old score bucket
        if (oldScore > 0) {
            ConcurrentHashMap<String, Player> oldBucket = scoreIndex.get(oldScore);
            if (oldBucket != null) {
                oldBucket.remove(player.id);
                if (oldBucket.isEmpty()) {
                    scoreIndex.remove(oldScore);
                }
            }
        }
        
        // Add to new score bucket
        scoreIndex.computeIfAbsent(newScore, k -> new ConcurrentHashMap<>())
                  .put(player.id, player);
    }
    
    public List<Player> getTopPlayers(int count) {
        rankLock.readLock().lock();
        try {
            List<Player> topPlayers = new ArrayList<>();
            int collected = 0;
            
            for (Map.Entry<Long, ConcurrentHashMap<String, Player>> entry : scoreIndex.entrySet()) {
                if (collected >= count) break;
                
                for (Player player : entry.getValue().values()) {
                    if (collected >= count) break;
                    topPlayers.add(player);
                    collected++;
                }
            }
            
            return topPlayers;
        } finally {
            rankLock.readLock().unlock();
        }
    }
    
    public int getRank(String playerId) {
        Player player = players.get(playerId);
        if (player == null) return -1;
        
        rankLock.readLock().lock();
        try {
            int rank = 1;
            
            for (Map.Entry<Long, ConcurrentHashMap<String, Player>> entry : scoreIndex.entrySet()) {
                if (entry.getKey() > player.score) {
                    rank += entry.getValue().size();
                } else if (entry.getKey() == player.score) {
                    if (entry.getValue().containsKey(playerId)) {
                        return rank;
                    }
                    rank++;
                }
            }
            
            return -1;
        } finally {
            rankLock.readLock().unlock();
        }
    }
    
    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }
    
    public List<Player> getPlayersInRange(int startRank, int endRank) {
        rankLock.readLock().lock();
        try {
            List<Player> result = new ArrayList<>();
            int currentRank = 1;
            
            for (Map.Entry<Long, ConcurrentHashMap<String, Player>> entry : scoreIndex.entrySet()) {
                for (Player player : entry.getValue().values()) {
                    if (currentRank >= startRank && currentRank <= endRank) {
                        result.add(player);
                    }
                    currentRank++;
                    
                    if (currentRank > endRank) {
                        return result;
                    }
                }
            }
            
            return result;
        } finally {
            rankLock.readLock().unlock();
        }
    }
}
```

## 24. **Food Delivery System**

java

```java
public class Restaurant {
    String id;
    String name;
    Location location;
    volatile boolean accepting;
    ConcurrentHashMap<String, MenuItem> menu;
    
    public Restaurant(String id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.accepting = true;
        this.menu = new ConcurrentHashMap<>();
    }
}

public class MenuItem {
    String id;
    String name;
    double price;
    int preparationTimeMinutes;
    
    public MenuItem(String id, String name, double price, int prepTime) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.preparationTimeMinutes = prepTime;
    }
}

public class DeliveryPartner {
    String id;
    String name;
    volatile Location currentLocation;
    volatile boolean available;
    
    public DeliveryPartner(String id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.currentLocation = location;
        this.available = true;
    }
}

public enum OrderStatus {
    PLACED, ACCEPTED, PREPARING, READY, PICKED_UP, DELIVERED, CANCELLED
}

public class FoodOrder {
    String id;
    String customerId;
    Restaurant restaurant;
    List<MenuItem> items;
    Location deliveryLocation;
    volatile OrderStatus status;
    DeliveryPartner partner;
    double totalPrice;
    LocalDateTime placedAt;
    
    public FoodOrder(String id, String customerId, Restaurant restaurant, 
                     List<MenuItem> items, Location deliveryLocation) {
        this.id = id;
        this.customerId = customerId;
        this.restaurant = restaurant;
        this.items = items;
        this.deliveryLocation = deliveryLocation;
        this.status = OrderStatus.PLACED;
        this.totalPrice = items.stream().mapToDouble(i -> i.price).sum();
        this.placedAt = LocalDateTime.now();
    }
}

public class FoodDeliverySystem {
    private final ConcurrentHashMap<String, Restaurant> restaurants;
    private final ConcurrentHashMap<String, DeliveryPartner> partners;
    private final ConcurrentHashMap<String, FoodOrder> orders;
    private final ExecutorService matchingExecutor;
    private final AtomicInteger orderCounter = new AtomicInteger(0);
    
    public FoodDeliverySystem() {
        this.restaurants = new ConcurrentHashMap<>();
        this.partners = new ConcurrentHashMap<>();
        this.orders = new ConcurrentHashMap<>();
        this.matchingExecutor = Executors.newFixedThreadPool(5);
    }
    
    public void addRestaurant(Restaurant restaurant) {
        restaurants.put(restaurant.id, restaurant);
    }
    
    public void addDeliveryPartner(DeliveryPartner partner) {
        partners.put(partner.id, partner);
    }
    
    public List<Restaurant> searchRestaurants(Location userLocation, double radiusKm) {
        return restaurants.values().stream()
            .filter(r -> r.accepting && r.location.distanceTo(userLocation) <= radiusKm)
            .collect(Collectors.toList());
    }
    
    public String placeOrder(String customerId, String restaurantId, 
                           List<String> itemIds, Location deliveryLocation) {
        Restaurant restaurant = restaurants.get(restaurantId);
        if (restaurant == null || !restaurant.accepting) {
            return null;
        }
        
        List<MenuItem> items = itemIds.stream()
            .map(id -> restaurant.menu.get(id))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (items.isEmpty()) {
            return null;
        }
        
        String orderId = "ORDER-" + orderCounter.incrementAndGet();
        FoodOrder order = new FoodOrder(orderId, customerId, restaurant, items, deliveryLocation);
        
        orders.put(orderId, order);
        
        // Async restaurant acceptance and partner matching
        matchingExecutor.submit(() -> {
            acceptOrder(order);
            matchDeliveryPartner(order);
        });
        
        return orderId;
    }
    
    private void acceptOrder(FoodOrder order) {
        // Simulate restaurant accepting
        try {
            Thread.sleep(2000);
            order.status = OrderStatus.ACCEPTED;
            System.out.println("Order " + order.id + " accepted by restaurant");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void matchDeliveryPartner(FoodOrder order) {
        DeliveryPartner nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (DeliveryPartner partner : partners.values()) {
            if (partner.available) {
                double distance = partner.currentLocation.distanceTo(order.restaurant.location);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = partner;
                }
            }
        }
        
        if (nearest != null) {
            DeliveryPartner partner = nearest;
            synchronized (partner) {
                if (partner.available) {
                    partner.available = false;
                    order.partner = partner;
                    order.status = OrderStatus.PREPARING;
                    System.out.println("Order " + order.id + " assigned to partner " + partner.id);
                }
            }
        }
    }
    
    public void markOrderReady(String orderId) {
        FoodOrder order = orders.get(orderId);
        if (order != null && order.status == OrderStatus.PREPARING) {
            order.status = OrderStatus.READY;
        }
    }
    
    public void markOrderPickedUp(String orderId) {
        FoodOrder order = orders.get(orderId);
        if (order != null && order.status == OrderStatus.READY) {
            order.status = OrderStatus.PICKED_UP;
        }
    }
    
    public void markOrderDelivered(String orderId) {
        FoodOrder order = orders.get(orderId);
        if (order != null && order.status == OrderStatus.PICKED_UP) {
            order.status = OrderStatus.DELIVERED;
            if (order.partner != null) {
                order.partner.available = true;
            }
        }
    }
    
    public void cancelOrder(String orderId) {
        FoodOrder order = orders.get(orderId);
        if (order != null && order.status != OrderStatus.DELIVERED) {
            order.status = OrderStatus.CANCELLED;
            if (order.partner != null) {
                order.partner.available = true;
            }
        }
    }
    
    public FoodOrder getOrderStatus(String orderId) {
        return orders.get(orderId);
    }
    
    public void updatePartnerLocation(String partnerId, Location location) {
        DeliveryPartner partner = partners.get(partnerId);
        if (partner != null) {
            partner.currentLocation = location;
        }
    }
    
    public void shutdown() {
        matchingExecutor.shutdown();
    }
}
```

## 25. **Distributed Lock Service**

java

```java
public class DistributedLock {
    private final String lockName;
    private final long timeoutMs;
    private volatile String owner;
    private volatile long expiryTime;
    private final ReentrantLock localLock = new ReentrantLock();
    
    public DistributedLock(String lockName, long timeoutMs) {
        this.lockName = lockName;
        this.timeoutMs = timeoutMs;
    }
    
    public boolean tryAcquire(String requesterId) {
        localLock.lock();
        try {
            long now = System.currentTimeMillis();
            
            // Check if lock is expired
            if (owner != null && now > expiryTime) {
                owner = null;
            }
            
            // Try to acquire
            if (owner == null) {
                owner = requesterId;
                expiryTime = now + timeoutMs;
                return true;
            }
            
            // Already owned by same requester (reentrant)
            if (owner.equals(requesterId)) {
                expiryTime = now + timeoutMs; // Extend
                return true;
            }
            
            return false;
        } finally {
            localLock.unlock();
        }
    }
    
    public boolean release(String requesterId) {
        localLock.lock();
        try {
            if (owner != null && owner.equals(requesterId)) {
                owner = null;
                expiryTime = 0;
                return true;
            }
            return false;
        } finally {
            localLock.unlock();
        }
    }
    
    public boolean isLocked() {
        localLock.lock();
        try {
            long now = System.currentTimeMillis();
            if (owner != null && now > expiryTime) {
                owner = null;
                return false;
            }
            return owner != null;
        } finally {
            localLock.unlock();
        }
    }
    
    public String getOwner() {
        return owner;
    }
}

public class DistributedLockService {
    private final ConcurrentHashMap<String, DistributedLock> locks;
    private final ScheduledExecutorService cleanupExecutor;
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    
    public DistributedLockService() {
        this.locks = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Cleanup expired locks every 10 seconds
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredLocks, 10, 10, TimeUnit.SECONDS
        );
    }
    
    public boolean acquireLock(String lockName, String requesterId) {
        return acquireLock(lockName, requesterId, DEFAULT_TIMEOUT_MS);
    }
    
    public boolean acquireLock(String lockName, String requesterId, long timeoutMs) {
        DistributedLock lock = locks.computeIfAbsent(
            lockName, 
            k -> new DistributedLock(lockName, timeoutMs)
        );
        
        return lock.tryAcquire(requesterId);
    }
    
    public boolean tryAcquireLock(String lockName, String requesterId, 
                                 long waitTimeMs, long lockTimeoutMs) {
        long endTime = System.currentTimeMillis() + waitTimeMs;
        
        while (System.currentTimeMillis() < endTime) {
            if (acquireLock(lockName, requesterId, lockTimeoutMs)) {
                return true;
            }
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    public boolean releaseLock(String lockName, String requesterId) {
        DistributedLock lock = locks.get(lockName);
        if (lock != null) {
            return lock.release(requesterId);
        }
        return false;
    }
    
    public boolean isLocked(String lockName) {
        DistributedLock lock = locks.get(lockName);
        return lock != null && lock.isLocked();
    }
    
    private void cleanupExpiredLocks() {
        locks.entrySet().removeIf(entry -> !entry.getValue().isLocked());
    }
    
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
}

// Example usage with automatic lock management
public class LockManager {
    private final DistributedLockService lockService;
    
    public LockManager(DistributedLockService lockService) {
        this.lockService = lockService;
    }
    
    public <T> T executeWithLock(String lockName, String requesterId, 
                                 Supplier<T> task) {
        if (lockService.acquireLock(lockName, requesterId)) {
            try {
                return task.get();
            } finally {
                lockService.releaseLock(lockName, requesterId);
            }
        }
        throw new RuntimeException("Failed to acquire lock: " + lockName);
    }
    
    public void executeWithLock(String lockName, String requesterId, 
                               Runnable task) {
        if (lockService.acquireLock(lockName, requesterId)) {
            try {
                task.run();
            } finally {
                lockService.releaseLock(lockName, requesterId);
            }
        } else {
            throw new RuntimeException("Failed to acquire lock: " + lockName);
        }
    }
}
```

These implementations cover all the remaining LLD problems with multithreading solutions!

  

[Claude is AI and can make mistakes. Please double-check responses.](https://support.anthropic.com/en/articles/8525154-claude-is-providing-incorrect-or-misleading-responses-what-s-going-on)