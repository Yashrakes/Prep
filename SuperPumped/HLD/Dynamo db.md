https://www.youtube.com/watch?v=2X2SO3Y-af8


## 🔑 Core Concepts

1. **Data Model**
    
    - **Tables** → Like in RDBMS, but without fixed schema.
        
    - **Items** → Equivalent to rows; each item is a collection of key-value pairs.
        
    - **Attributes** → Equivalent to columns, but flexible (each item can have different attributes).
        
    - **Primary Key** → Mandatory; uniquely identifies each item.
        
        - **Partition Key only (simple primary key)**
            
        - **Partition Key + Sort Key (composite primary key)**
            
2. **Indexes**
    
    - **Primary Index** (always exists): Based on table’s partition + optional sort key.
        
    - **Local Secondary Index (LSI)**: Same partition key as table but different sort key.
        
    - **Global Secondary Index (GSI)**: Different partition + sort key (flexible querying).
        

---

## ⚙️ How DynamoDB Works Internally

1. **Partitioning (Data Distribution)**
    
    - Every item’s partition key is hashed.
        
    - That hash decides which **partition (physical storage node)** the item goes to.
        
    - Partitions are spread across multiple servers for scalability.
        
    - Hot partitions can occur if partition keys are not well-distributed.
        
2. **Storage Engine**
    
    - Data is stored across **SSD-backed storage** with replication across multiple Availability Zones (AZs).
        
    - Ensures durability & high availability.
        
    - Data is eventually consistent by default, but you can request strong consistency (within the same region).
        
3. **Read/Write Model**
    
    - **WCU (Write Capacity Units)** → Throughput for writes.
        
    - **RCU (Read Capacity Units)** → Throughput for reads.
        
    - **On-Demand Mode** → You don’t pre-allocate RCUs/WCUs, pay per request.
        
    - **Provisioned Mode** → You reserve a certain throughput capacity.
        
4. **Consistency**
    
    - **Eventually Consistent Reads** → Fast, may not reflect the latest write immediately.
        
    - **Strongly Consistent Reads** → Always reflect the latest write (but can have slightly higher latency).
        
5. **Indexes & Querying**
    
    - You can only query efficiently by partition key (and sort key range).
        
    - For other access patterns, you need GSIs/LSIs.
        
    - Supports `Scan` (full table scan, costly) and `Query` (efficient, by key).
        
6. **Caching**
    
    - Optionally integrate with **DAX (DynamoDB Accelerator)** for in-memory caching, reducing read latency from ms → microseconds.
        

---

## 🔄 Write Path

1. Client sends a `PutItem` or `UpdateItem`.
    
2. DynamoDB hashes the partition key → finds partition.
    
3. The request goes through a **replication log (commit log)**.
    
4. Written to **3 AZs** before confirming success.
    
5. Optionally streams the change to **DynamoDB Streams** (for event-driven processing).
    

---

## 📖 Read Path

1. Client issues `GetItem` / `Query`.
    
2. Partition key is hashed → correct partition is located.
    
3. Data is fetched from the replica (eventually consistent) or the leader (strong consistent).
    
4. If DAX is enabled, data might be served directly from cache.
    

---

## ✨ Strengths

- Fully managed (no server ops).
    
- Scales to millions of requests/sec.
    
- High availability (multi-AZ replication).
    
- Predictable low latency.
    

## ⚠️ Limitations

- Limited querying (only via keys or indexes).
    
- Expensive for scans & large analytics workloads.
    
- Hot partition issue if partition keys are not well-distributed.

