
# Low-level design — Distributed ID generator (Java)

Nice — this is a classic systems problem with lots of practical trade-offs. I’ll cover common approaches, properties you must satisfy, concrete designs (including a production-ready **Snowflake-style** implementation in Java), failure modes, optimizations, APIs, deployment and testing guidance. I’ll also include sample code you can drop into a service.

---

## Goals & constraints (what you must decide up front)

- **Uniqueness**: IDs must not collide across machines.
    
- **Throughput / Latency**: how many IDs/sec per node and overall.
    
- **Ordering**: do you need monotonically increasing IDs (global ordering) or only roughly sortable?
    
- **ID size**: 64-bit? 128-bit? (64-bit is common)
    
- **Persistence**: IDs are not usually stored centrally; design must allow recovery.
    
- **Clock assumptions**: nodes’ clocks may drift or go backwards.
    
- **Availability**: design for high availability and no single point of failure.
    
- **Scalability**: ability to add nodes without huge reconfiguration.
    

Decide these early — they determine the architecture.

---

## Common architectures (pros/cons)

1. **UUID (v4 / v1)**
    
    - Pros: no coordination, easy, globally unique.
        
    - Cons: large (128-bit), not ordered, not human-friendly.
        
2. **Database-backed sequences (e.g., `AUTO_INCREMENT`, sequences)**
    
    - Pros: simple, globally unique, ordered.
        
    - Cons: DB is a bottleneck and single point of failure; high latency under load.
        
3. **Redis `INCR` or `INCRBY`**
    
    - Pros: fast, atomic, supports allocation of ranges (`INCRBY batchSize`) to nodes.
        
    - Cons: depends on Redis availability; can be scaled with clustering but still central.
        
4. **Zookeeper / etcd sequential nodes**
    
    - Pros: strong ordering, coordination, used in practice.
        
    - Cons: performance limited by coordination layer; higher latency.
        
5. **Snowflake style (Twitter)** — recommended for high-performance systems.
    
    - Pros: decentralised, very high throughput, 64-bit compact IDs, roughly time-ordered.
        
    - Cons: requires worker id allocation and handling clock drift/rollback.
        
6. **Hi/Lo or Segment allocation (Range allocation)**
    
    - Each node reserves a block/range (e.g., 1000 IDs), serve locally from block. Replenish via central allocator (DB/Redis).
        
    - Pros: avoids frequent central calls, simple.
        
    - Cons: complexity in range allocation, wasted IDs if node fails.
        

---

## Recommended design for most production systems

**Use Snowflake-style ID generator with worker-id coordination + fallbacks**, optionally combined with block allocation for certain use cases.

Rationale: low latency, very high throughput, compact 64-bit IDs, roughly time-ordered (useful for DB clustering/partitioning).

---

# Snowflake-style design (Twitter 64-bit) — details

### Typical 64-bit layout (example)

`0 | 41 bits timestamp (millis since custom epoch)   | 10 bits workerId (machine/shard)   | 12 bits sequence number (per ms)`

- 41 bits timestamp → ~69 years
    
- 10 bits workerId → 1024 nodes
    
- 12 bits sequence → 4096 IDs/ms per node
    

You can adjust bit allocation to your needs (more worker bits vs more sequence bits).

---

## Key problems & solutions

- **Worker ID allocation**: assign stable unique worker IDs (ZK, etcd, consul, manual config, or derive from machine IP/instance ID with hashing & collision check). Use ephemeral registrations to detect collisions.
    
- **Clock rollback**: if system clock moves backward, you must avoid generating duplicate earlier timestamps.
    
    - Solutions: wait until clock catches up; use logical clock bump (use lastTimestamp and if current < lastTimestamp then either throw, wait, or use sequence+flag to generate using lastTimestamp); use NTP disciplined machines.
        
- **Sequence overflow in a ms**: if sequence reaches max within same ms, wait till next millisecond.
    
- **Node failure with preallocated ranges**: lost/block of IDs — acceptable in many systems. If not, use persistent central allocator and optionally reclaim.
    
- **Id decoding**: allow decode (timestamp, workerId, sequence) for debugging.
    

---

## Java Snowflake implementation (thread-safe, simple)

Below is a straightforward single-JVM generator. For multi-JVM you need stable workerIds.

public class SnowflakeIdGenerator {
    private final long epoch = 1609459200000L; // custom epoch (2021-01-01)
    private final long workerId;
    private final long workerIdBits = 10L;
    private final long sequenceBits = 12L;

    private final long maxWorkerId = (1L << workerIdBits) - 1;
    private final long maxSequence = (1L << sequenceBits) - 1;

    private final long workerIdShift = sequenceBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > maxWorkerId) {
            throw new IllegalArgumentException("workerId out of range");
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            // Clock moved backwards
            long diff = lastTimestamp - timestamp;
            throw new IllegalStateException("Clock moved backwards. Refusing to generate id for " + diff + "ms");
            // Alternative: wait until lastTimestamp or use sequence with flagged bit
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0L) {
                // sequence overflow in this ms: wait for next ms
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - epoch) << timestampLeftShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long ts = timeGen();
        while (ts <= lastTimestamp) {
            ts = timeGen();
        }
        return ts;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    // Optionally: decode an id to components
    public static Map<String, Long> decode(long id, long epoch, long workerIdBits, long sequenceBits) {
        long sequenceMask = (1L << sequenceBits) - 1;
        long workerMask = ((1L << workerIdBits) - 1) << sequenceBits;
        long sequence = id & sequenceMask;
        long workerId = (id >> sequenceBits) & ((1L << workerIdBits) - 1);
        long timestamp = (id >> (workerIdBits + sequenceBits)) + epoch;
        Map<String, Long> m = new HashMap<>();
        m.put("timestamp", timestamp);
        m.put("workerId", workerId);
        m.put("sequence", sequence);
        return m;
    }
}


**Notes**

- `synchronized` ensures thread-safety within JVM. For ultra-high throughput use `sun.misc.Unsafe` CAS or `LongAdder`-based optimizations; but `synchronized` is OK for many use cases. You can reduce synchronization cost by pre-fetching batches (see below).
    
- This throws exception on clock-backwards; production systems often wait or use a reserved bit.
    

---

## Worker ID allocation (practical approaches)

- **Static config**: pass workerId as startup config (simple but manual).
    
- **Derived from container metadata**: e.g., Kubernetes pod index or instance id (risk collisions).
    
- **Service registry** (recommended): use Zookeeper/etcd/Consul to allocate unique worker id on startup (create ephemeral node `/ids/worker-XXXX` with incrementing sequence). Good because ephemeral node disappears on crash.
    
- **DB table**: allocate worker ids in DB with lease expiration.
    

---

## Resilience & clock skew strategies

- **Wait-on-rollback**: if `now < lastTimestamp`, sleep until lastTimestamp — safe but increases latency.
    
- **Use monotonic clock**: `System.nanoTime()` can't be used for epoch; but can help measure elapsed time. Still need wall-clock for timestamp.
    
- **Use logical timestamp**: if clock goes backward, bump timestamp = lastTimestamp + 1 (requires ensuring uniqueness).
    
- **NTP discipline & containerization**: ensure NTP works and clock adjustments are tiny.
    

---

## Optimization: Pre-allocated blocks / HiLo / Range allocation

To reduce synchronization overhead or avoid per-call synchronization, allocate ranges:

- **Central allocator (Redis / DB)**:
    
    - Node requests `INCRBY globalCounter batchSize`.
        
    - Redis returns start value S → node uses [S, S+batchSize-1] locally, no further coordination until exhausted.
        
    - On crash, unused IDs lost (often acceptable).
        
- **Benefits**: amortize central coordination, high throughput local generation.
    
- **Downside**: potential gaps and wasted IDs.
    

Example flow:

- Node asks Redis `INCRBY id:counter 10000` → gets base 20000 → node uses 20000...29999 locally.
    

---

## API & deployment

### Service API

- Expose a lightweight HTTP/gRPC endpoint:
    
    - `POST /id` → returns single ID
        
    - `POST /id?count=n` → returns batch of n IDs
        
- Prefer gRPC for performance or binary clients.
    
- Keep endpoint extremely simple and idempotent.
    

### Scaling

- Horizontal scale: run many generator instances (with unique worker IDs or assigned ranges).
    
- Use load balancer with healthchecks.
    
- Use local cache of ID ranges to reduce central contention.
    
- Monitor sequence exhaustion and queue replenishment latencies.
    

---

## Observability & monitoring

- Metrics: IDs/sec, latency p50/p95/p99, number of times wait-for-next-ms triggered, clock-backward events, sequence overflow occurrences.
    
- Logging: worker id on startup, assigned ID ranges, allocation failures.
    
- Alert on high clock-backwards or high tilNextMillis waits (indicates clock stability issues).
    

---

## Testing & benchmarking

- Unit test generator: multi-threaded test that generates N IDs concurrently and verifies uniqueness and monotonicity per node.
    
- Distributed test: run N JVMs with unique workerIds, generate IDs, aggregate and ensure no duplicates.
    
- Load test: measure IDs/sec and CPU; tune sequenceBits or batch size accordingly.
    
- Simulate clock rollback and ensure behavior is acceptable.
    

---

## Trade-offs at a glance

|Requirement|Best approach|Trade-offs|
|---|---|---|
|Simple & unique|UUID v4|Large, unordered|
|High throughput + order|Snowflake|Needs worker id coordination, clock care|
|Absolute ordering|Central sequence (DB)|Bottleneck|
|High availability & low latency|Snowflake + range allocation|gaps, complexity in worker ids|
|Guarantee no gaps & persistence|DB-based|latency & throughput limits|

---

## Extra production considerations

- **Backwards compatibility**: if you need more nodes later, you may need to reallocate bits (hard). Plan bit allocation for growth.
    
- **Multi-datacenter**: ensure worker-id uniqueness across DCs or reserve different worker id ranges for each DC.
    
- **Security**: IDs may leak time/seq info. If privacy matters, use opaque IDs or map to hash.
    
- **Encoding**: sometimes base62/hex encoding is used to shorten string representation for URLs.
    

---

## Quick checklist to implement in Java (practical steps)

1. Choose bit layout (timestamp / worker / sequence).
    
2. Implement thread-safe Snowflake generator (sample above).
    
3. Implement worker-id allocator (ZK/etcd/DB) — ephemeral registration recommended.
    
4. Add HTTP/gRPC wrapper exposing nextId and batch endpoints.
    
5. Add health checks & metrics.
    
6. Add clock-backwards handling strategy.
    
7. Load test and tune `sequenceBits` or implement batch allocation.
    
8. Deploy multiple instances and monitor.