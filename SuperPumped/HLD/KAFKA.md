Problems 
1. cant handle two many request on a single queue  -> so scale horizontally 
2. then after scaling the request are not processed in order then -> partition properly
3. single consumer cant handle all, so keep multiple consumer -> consumer groups
4. want to keep each sport separate -> introduce topics
5. periodically commit offset to Kafka 
6. brokers have details of topics and partitions 
7. use Kafka when processing can be done asynchronously
8. use Kafka queue for book my show event queue, use in leetcode, use in streams
9. for scaling. -> reduce size of msg, have more broker which means more messages can be read, 
10. strong fault tolerance and durability
11. every broker has a leader partition of corresponding topic 
12. kafka is always available , it doesn't goes down
13. when consumer goes down , kafka commits the offset, kafka commits  when data is processed
14. kafka support retries , exponential retries can be coded and integrated
15. retention messages in kafka by default for  7 days  modifilable
16. when can kafka start deleting datad based on size  1 gb modifiable
17. for dtaa consistency , suppose if there is a primatry partiion and aother replicaerted partition with same data. what if the repllicated partition misses tohave that replicated data , then how does we handle that case, zookeper decide who should be the leader ,solution ->consumer usually consume only from that read replicated partition only when they found all the data in partitions
18. usually for large messagaes , we produce and consumer manages in from of batches
19. consumer acknowledge when he process the message (slow) or acknowldge immediately after having message (fast)
https://www.youtube.com/watch?v=hNDjd9I_VGA
https://medium.com/@mayankbansal933/design-messaging-queue-515e0e912d93
https://github.com/ninjakx/LLD-kafka?tab=readme-ov-file


In Apache Kafka, ==the leader-follower model is used for data replication and fault tolerance within partitions==. Each partition in a Kafka topic has one leader and multiple followers. The leader handles all read and write requests for that partition, while followers replicate the leader's data to provide redundancy and fault tolerance. If the leader fails, a follower is elected as the new leader. 

Here's a more detailed explanation:

Leader:

- **Handles all read and write requests:**
    
    For a specific partition, the leader is the sole broker that receives and processes all incoming messages (writes) and also serves data to consumers (reads).
    
- **Replication point:**
    
    The leader is also responsible for replicating the data to follower replicas.

![[Pasted image 20250720211545.png]]sin



### 1. **Kafka**

> A **distributed, high-throughput, fault-tolerant event streaming platform** used to build real-time data pipelines and streaming apps.

---

## 📦 **Basic Components**

### 2. **Producer**

> A client that **sends records (messages)** to Kafka **topics**.

### 3. **Consumer**

> A client that **reads records** from Kafka **topics**.

### 4. **Broker**

> A Kafka **server** that stores data and serves client requests. A Kafka cluster consists of multiple brokers.

### 5. **Topic**

> A **category/feed name** to which messages are published. Topics are **partitioned** and **replicated** for fault tolerance.

---

## 🧱 **Storage & Scalability**

### 6. **Partition**

> A **subset of a topic**. Each topic has one or more partitions, enabling **parallelism**. Messages within a partition are ordered.

### 7. **Offset**

> A unique, monotonically increasing number assigned to each message within a partition. Consumers use it to keep track of read messages.

### 8. **Segment**

> Kafka stores data in **log segment files** within partitions. When a segment exceeds a certain size or age, a new one is created.

---

## 🔁 **Data Durability & Replication**

### 9. **Replication**

> Kafka replicates each partition **across multiple brokers** to ensure **fault tolerance**.

### 10. **Leader Replica**

> The **main replica** of a partition that handles **all reads/writes**.

### 11. **Follower Replica**

> A **passive copy** of the leader. It replicates data from the leader.

### 12. **ISR (In-Sync Replica)**

> A set of replicas (including the leader) that are **fully caught up** with the leader. Kafka only acknowledges writes when they are written to all ISRs (depending on `acks` setting).

---

## 🧰 **Consumer Group & Offset Management**

### 13. **Consumer Group**

> A group of consumers that **share the work** of consuming records from topics. Each partition is consumed by **only one consumer** in a group at a time.

### 14. **Rebalance**

> When the consumer group changes (a consumer joins/leaves), Kafka **redistributes partitions** among consumers, called a rebalance.

### 15. **Committed Offset**

> The offset **saved in Kafka** (or external storage) that marks the last message **successfully processed** by a consumer.

---

## ⚙️ **Producer Configurations**

### 16. **acks (Acknowledgment)**

> Controls **when a producer gets acknowledgment**:

- `acks=0`: No ack.
    
- `acks=1`: Ack after leader receives it.
    
- `acks=all`: Ack after all ISRs receive it (most reliable).
    

### 17. **Batch Size**

> The number of messages sent in a **single batch** to increase throughput.

### 18. **Linger.ms**

> Time to wait for more messages before sending a batch (even if batch size not reached).

---

## ⏱️ **Consumer Configurations**

### 19. **Auto Offset Reset**

> Defines what to do if no committed offset is found:

- `earliest`: Read from beginning.
    
- `latest`: Read from end (default).
    
- `none`: Throw error.
    

### 20. **enable.auto.commit**

> If `true`, offsets are **automatically committed** periodically (can cause data loss if crash before commit).

---

## 📜 **Kafka APIs**

### 21. **Producer API**

> For **publishing** records to Kafka topics.

### 22. **Consumer API**

> For **subscribing** and consuming records.

### 23. **Admin API**

> For managing topics, brokers, ACLs, etc.

### 24. **Kafka Streams**

> A **Java library** for building **stream processing** apps on top of Kafka.

### 25. **Kafka Connect**

> A tool to **integrate Kafka with external systems** (e.g., databases, Elasticsearch) using **source and sink connectors**.

---

## 🏗️ **Advanced Kafka Concepts**

### 26. **Log Compaction**

> A cleanup policy that **retains only the latest record per key**, useful for stateful data.

### 27. **Retention Policy**

> Defines how long Kafka keeps messages. Controlled by:

- Time (`retention.ms`)
    
- Size (`retention.bytes`)
    

### 28. **Dead Letter Queue (DLQ)**

> A Kafka topic where **bad/failed records** are sent for manual inspection.

### 29. **Exactly Once Semantics (EOS)**

> Kafka guarantees **each message is processed exactly once** using **idempotent producers** and **transactional APIs**.

---

## 🧪 **Reliability & Performance**

### 30. **Idempotent Producer**

> Ensures that **duplicate messages** sent by retries are **not written multiple times**.

### 31. **Kafka Transaction**

> Used to **commit/abort a group of writes** (to topics and offsets) atomically.

### 32. **ZooKeeper (legacy)**

> Used to manage Kafka **metadata and leader election**, but Kafka is moving to **KRaft (Kafka Raft)** mode to remove this dependency.
