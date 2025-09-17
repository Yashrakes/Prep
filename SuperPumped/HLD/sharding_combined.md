# 📘 Database Sharding and Partitioning in Distributed Systems

## Overview
- **Partitioning:** Divides data within a single DB instance.
- **Sharding:** Distributes data across multiple DB instances.

---

## Types of Partitioning

### 1. Horizontal Partitioning
```sql
CREATE TABLE users_2023 (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    created_at TIMESTAMP
) WHERE created_at >= '2023-01-01' AND created_at < '2024-01-01';
```

### 2. Vertical Partitioning
```sql
CREATE TABLE users_core (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100)
);

CREATE TABLE users_extended (
    id INT PRIMARY KEY,
    bio TEXT,
    preferences JSON,
    last_login TIMESTAMP
);
```

### 3. Functional Partitioning
**Splits data by feature or service boundaries.**

User Service DB: users, profiles, authentication

Order Service DB: orders, payments, shipping

Product Service DB: products, inventory, categories


---

## Sharding Strategies

### 1. Range-Based Sharding
**Distributes data based on value ranges.**

Shard 1: user

id 1-1000000

Shard 2: user

id 1000001-2000000

Shard 3: user

id 2000001-3000000

- **Pros:** Simple, efficient for range queries
- **Cons:** Hotspots, uneven distribution

### 2. Hash-Based Sharding
```python
def get_shard(user_id, num_shards):
    return hash(user_id) % num_shards
```
- **Pros:** Even distribution
- **Cons:** Poor for range queries

### 3. Directory-Based Sharding
- Maintains mapping in a separate directory
- **Pros:** Flexible
- **Cons:** Extra complexity, SPOF

### 4. Consistent Hashing
```python
Hash Ring: [0 ─── Shard A ─── Shard B ─── Shard C ─── 2^32-1]
```

---

## Implementation Architectures
- **Master-Slave**
- **Federated Sharding**
- **Cross-Shard Joins / Transactions**

---

## Example: Cross-Shard Query Coordination
```python
results = []
for shard in shards:
    result = shard.query("SELECT * FROM orders WHERE user_id IN (?)", user_ids)
    results.append(result)
return merge_and_sort(results)
```

---

## Resharding Strategies

### Live Migration
```python
def live_resharding():
    enable_dual_write()
    for batch in get_data_batches():
        new_shard.write(batch)
        verify_consistency(batch)
    switch_reads_to_new_shard()
    disable_dual_write()
```

---

## Challenges & Solutions
- **Hotspots** → Composite keys, auto-balancing
- **Consistency** → Sagas, event-driven
- **Complexity** → Automation, standardization

---

## Best Practices
- Choose shard keys wisely
- Optimize queries
- Make application shard-aware

---

## Popular Sharding Solutions
- **Database-native:** MySQL Cluster, MongoDB, Citus
- **Middleware:** Vitess, ShardingSphere, ProxySQL
- **App-Level:** Custom logic, microservices

---

## Monitoring & Metrics
```python
metrics = {
    'shard_distribution': 'Data size per shard',
    'query_latency': 'Response time per shard',
    ...
}
```

---

# 📙 Complete Analysis of Sharding Approaches

## 1. Range-Based Sharding
- **Pros:** Great for range queries, easy to implement
- **Cons:** Hotspots, uneven load
- **Best for:** Time-series, logs

---

## 2. Hash-Based Sharding
- **Pros:** Uniform distribution
- **Cons:** No range queries, hard to reshard
- **Best for:** OLTP, session data

---

## 3. Directory-Based Sharding
- **Pros:** Flexible, dynamic resharding
- **Cons:** SPOF, complexity
- **Best for:** Multi-tenant, microservices

---

## 4. Consistent Hashing
- **Pros:** Minimal resharding, scalable
- **Cons:** Complex, no range queries
- **Best for:** NoSQL, caching

---

## 5. Geographic Sharding
- **Pros:** Local latency, compliance
- **Cons:** Regional skew, complexity
- **Best for:** Global apps, CDNs

---

## 6. Functional (Vertical) Sharding
- **Pros:** Service isolation, autonomy
- **Cons:** Cross-service joins, eventual consistency
- **Best for:** Microservices, large teams

---

## 7. Hybrid Approaches
- **Range + Hash:** Time-based + even distribution
- **Geo + Functional:** Low latency + isolation
- **Dir + Consistent Hashing:** Flexibility + auto-scaling

---

## 📊 Comparison Matrix

| Approach         | Distribution | Query Flexibility | Complexity | Scalability | Consistency |
|------------------|--------------|--------------------|------------|-------------|-------------|
| Range-Based      | Poor         | Excellent           | Low        | Moderate    | Strong      |
| Hash-Based       | Excellent    | Poor                | Low        | Poor        | Strong      |
| Directory        | Good         | Good                | High       | Excellent   | Moderate    |
| Consistent Hash  | Good         | Poor                | High       | Excellent   | Eventual    |
| Geographic       | Poor         | Poor                | High       | Moderate    | Regional    |
| Functional       | N/A          | Poor                | Moderate   | Excellent   | Weak        |

---

## 🧭 Decision Framework
- **Start with**: Range or Hash
- **Advance to**: Directory or Consistent Hashing
- **Use Hybrid**: When one size doesn’t fit all
- **Consider**: Query patterns, growth, team expertise