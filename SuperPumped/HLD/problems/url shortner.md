
URL shortening is used for creating short aliases for much longer URLs. For example, goo.gl becomes https://developers.googleblog.com/2018/03/transitioning-google-url-shortener.html. How did that happen! Well, let’s have a look.

Before designing any system, we need to decide on the most basic requirement. So let’s get to it.

##### **Functional Requirements**

- Get short URL from a long URL
- Redirect to long URL when a user tries to access Short URL

##### **Non Functional Requirements**

Since this service can be used by a lot of companies, it needs to have these two NFRs

- Very low latency.
- Very high availability.

There are a few things we need to know before we start building this system.

##### **What should be the length of the URL?**

This will depend on the scale at which our system will work. For a few hundred URLs, 2-3 characters will be enough but for something with a larger scale like Facebook or Google, we might need longer URLs. We can either fix the length of the URLs our system will be generating or we can start with a length and then keep incrementing as per requirement. For now let us discuss the fixed-length approach.

###### **What will be the factors affecting the length of our URL?**

- Traffic expected - how many URLs we might have to shorten per second
- For how long do we need to support these URLs

Let us look at it mathematically. Say we get X requests in a second, and we need to support these URLs for, let’s say, ten years, then we need to be able to store Y unique URLs at a time where,

Y = X * 60 * 60 * 24 * 365 * 10

##### **What all characters can we include in the URL?**

Generally, most systems support URLs with A-Z, a-z, 0-9 character set, so let us build on this character set as well. However, in an interview, it might be a good idea to confirm before proceeding with your solution.

So we have 62 items in our character set which means we need to come up with a length that can support Y URLs with 62 characters.

If length is 1, we can have 62 URLsIf length is 2, we can have 62power2 URLsSimilarly, if the length is _l_, we can have 62_l_ URLs. That means,

so for 7 character we can support 62 power 7 which is 35 trillion number

Now let us look at a basic architecture that can potentially solve this problem.

[![Tiny URL shortening service using redis](https://www.codekarle.com/images/blog-images/tinyurl-sytem-design-intermediate-solution.svg)](https://www.codekarle.com/images/blog-images/tinyurl-sytem-design-intermediate-solution.svg)

##### **System Architecture**

We will have a **UI** that takes a long URL as an input, calls a **short URL service** which will somehow generate a short URL and store it in a **database** and then also returns the short URL. Similarly it can also be used to fetch the longer URL for a short URL. In this case, the short URL service will fetch the long URL from the database and redirect the user to the longer URL

##### **But more importantly, how is this short URL generated?**

Even though we have 62 characters, let us assume that we are generating numbers instead of URLs, for ease of understanding. So when we say unique URL, for the scope of this article we mean a unique number. We can safely assume we will be running multiple instances of this short URL service, say SUS1, SUS2, SUS3, etc. Now there is a possibility that more than one of them will end up generating the same number, which means one short URL will now point to two long URLs, which cannot happen. This is known as a **collision** in computer science.

One way to avoid this would be to check in the database before storing the short URL, to ensure that it doesn’t already exist and retry if it does. But this is a very poor solution. A better solution would be to use a Redis. This Redis will basically start counting from 1 and keep incrementing the count for each request before responding back. With this unique number, we can generate a unique URL by converting it to base 62. This is a very simple way to avoid collisions, but there will be a few challenges with this system.

### Example: Encode ID `125`
Characters used:
0–9  → 10 digits  
a–z  → 26 lowercase letters  
A–Z  → 26 uppercase letters  
Total → 62 characters

text = 125

CopyEdit

`125 / 62 = 2 remainder 1 2 / 62 = 0 remainder 2  Reverse the remainders: [2, 1] → map to base62 chars: "2" + "1" = "21"### ▶️ Decoding: Convert base62 string back to decimal

Given "21" → characters from base62:

text

CopyEdit

`'2' = 2 '1' = 1  Decimal = 2 * 62^1 + 1 * 62^0 = 124 + 1 = 125``



First of all, every machine will be interacting with Redis, which will increase the load on Redis. Also, the Redis here becomes a single point of failure, which is a big NO! If Redis goes down we will have no way to recover. Another issue is if the load becomes more than what our Redis can handle, it will slow down the whole system.

What if we use multiple instances of Redis? This will give us better performance and higher availability! Until both systems start generating duplicates. We can avoid this by assigning a series to each Redis, but what happens when we want to add another Redis? In this case, we need a managing element to keep track of which Redis has which series. Since we are introducing a managing component, we might as well look into alternatives for Redis.

###### **So let’s look into some solutions that don’t require a Redis.**

[![Tiny URL shortening service optimal solution architecture design](https://www.codekarle.com/images/blog-images/tinyurl-system-design-scalable-solution.svg)](https://www.codekarle.com/images/blog-images/tinyurl-system-design-scalable-solution.svg)

Our requirement is to ensure our short URL service is generating unique numbers such that even different instances of the service cannot return the same number. That way each service generates a unique number converts it to base 62 and returns it. The simplest way to implement it would be to set a range for each service, and to make sure each service has a different range we will use something called **Token Service**, while will be the managing component in our system. The token service will run on a single-threaded model and cater to only one machine at a time so that each machine has a different range. Our services will only interact with this token service on startup and when they are about to run out of their range, so token service can be something simple like a MySQL service as it will be dealing with a very minimal load. We will of course make sure that this MySQL service is distributed across geographies to reduce latency and also to make sure it is not a single point of failure.

Let’s look at an example. Say we have 3 short URL services, SUS1, SUS2, SUS3. On startup say SUS1 has range 1-1000, SUS2 has range 1001-2000 and SUS3 has range 2001-3000. When SUS1 runs out of its range token service will make sure it gets a range that hasn’t been assigned to another machine. One way to ensure this would be to maintain these ranges as records and keeping an “assigned” flag against them. When a range is assigned to a service, the “assigned” flag can be set to true. There could be other ways to do it, this is just one approach that can be followed.

##### **How do we scale it?**

Now we mentioned using a MySQL service that handles a very low load. But what if our system gets bombarded with requests? Well, we can either spin up multiple instances of the MySQL instances and distribute them across the map, as mentioned previously, or we could simply increase the length of our range. That would mean that machines will approach the token service at a much lower frequency.

##### **Missing Ranges**

So we now have a solution that gives us unique numbers, is distributed around the world to reduce latency, and doesn’t have a single point of failure. But what if one of the services that haven’t used up the complete range shuts down? How will we track what part of its range was left unused? We won’t! Tracking these missing ranges will add a level of complexity to our solution. Now, remember that we have more than 3.5 trillion possible unique numbers, which is a huge amount compared to the few thousand unique numbers that we are losing. A few thousand numbers are not significant enough to complicate our system and possibly compromise the performance. So we will just let them go and when the service starts back up we will assign it a new range.

##### **How will we redirect a short URL to the longer URL?**

When a request comes for a short URL to be redirected to the longer URL, we will fetch the long URL from the database, the service does a redirect and the user ends up on the right page. Now if you look back at the diagram, we have used Cassandra as our database. We could have used any database that has the capability to support 3.5 trillion data points, it can also be done with a MySQL database with a little bit of sharding. It is just more easily possible with Cassandra so that is what we used, but you can go with any other alternative that you find more convenient. We have discussed more database solutions for various scenarios in this article, you can check it out for a detailed explanation.

##### **Where is the analytics element here?**

Now let’s try to improve this system a bit. Because we can extract a lot of data that can later be used for making business decisions, let us try to add an analytics component to our system. Every time we get a request to generate a short URL, we will get some attributes along with it, like which platform it is using - could be something like Facebook or Twitter, which user agent it is coming from - iOS, Android, web browser, etc, we will also get the IP address of the sender. These attributes can give us a lot of information like which companies are using our system - these are our clients, which part of the world most requests are coming from - we can keep an instance of the token service there to reduce latency, etc. So when we get a request, instead of straight away responding back with the longer URL, we will first save this information into a **Kafka** which can be used to power the analytics. But this adds an additional step in our process and that increases the latency. So instead of doing this in a sequential manner, we can make this write to Kafka an asynchronous parallel operation running on a separate thread.

The only challenge here is that if for some reason the write operation fails, we don’t have that data anymore and it will be lost. But since it is not very critical information, just most basic information about the users, losing out on some of this data will not be a huge loss.

##### **Optimize Optimize Optimize**

Ok, so we also have analytics built into our system now. Can we still optimize it further? Well, remember those Kafka-writes after every request? We don’t necessarily need to do them so often. Instead of sending out these events to Kafka with every request, we could maintain this information in a local data structure like with a threshold size, queue for example, and when the data structure is about to overflow we can do a bulk write to Kafka and empty the queue. Or we could also make it a scheduled operation, for example, do a bulk write after every 30 seconds and empty the queue. Since CPU and bandwidth utilization are reduced we can drive more performance from the machine helping with low latency and high availability. Again this will be an asynchronous operation, and the drawback with maintaining the data locally is that if the machine unexpectedly shuts down and the write operation fails we will lose more information than we were losing on single writes, but you can discuss these trade-offs with your interviewer to come up with a solution that best suits your requirements.




### ✅ **1. How will you handle collisions in short URLs?**

**Answer:**

- **Base62 + Auto-increment ID**: Since IDs are unique in a relational DB, encoding them guarantees no collision.
    
- **UUID + Hash**: If generating using a hash (e.g., MD5), use salting to reduce collision risk.
    
- **Check Before Insert**: Always check if the generated short URL already exists in the DB. If yes, regenerate.
    

👉 **Preferred:** Auto-increment ID + Base62 encoding for guaranteed uniqueness.

---

### 🚀 **2. How do you ensure scalability?**

**Answer:**

- **App Layer**: Use stateless services behind a load balancer.
    
- **Database**:
    
    - Shard by user or hash of URL.
        
    - Use horizontally scalable DBs like Cassandra or DynamoDB.
        
- **Caching**:
    
    - Redis/Memcached for read-heavy traffic (redirects).
        
- **CDN**:
    
    - Use Cloudflare/Akamai to serve frequently accessed short URLs closer to users.
        

---

### 🗃️ **3. How do you store and retrieve mappings?**

**Answer:**

- Schema:
    
    sql
    
    CopyEdit
    
    `CREATE TABLE urls (   id BIGINT PRIMARY KEY AUTO_INCREMENT,   long_url TEXT NOT NULL,   short_code VARCHAR(10) UNIQUE,   created_at DATETIME,   expiry DATETIME,   user_id BIGINT );`
    
- Retrieval:
    
    - Use short_code to look up the original URL.
        
    - Cache hot entries in Redis.
        

---

### ⏳ **4. How is expiration handled?**

**Answer:**

- Allow users to specify TTL (e.g., 30 days).
    
- Store `expiry` field in DB.
    
- Use a background scheduler or TTL feature (like in DynamoDB or Redis) to delete old entries.
    
- Avoid serving expired URLs on redirect (check expiry before serving).
    

---

### 🔢 **5. How is the short URL generated?**

**Answer:**

- Method 1: **Base62(ID)**
    
    - Use a DB auto-increment ID.
        
    - Encode it using Base62.
        
- Method 2: **Hash + Random**
    
    - Use a hash (e.g., SHA256) of the long URL + userID or timestamp → substring of hash.
        

👉 For **custom aliases**, check for conflict in the DB before assigning.

---

### 🛡️ **6. How do you prevent abuse?**

**Answer:**

- **Input validation**: Reject URLs that are unsafe (e.g., `javascript:`, phishing).
    
- **Virus scanning**: Use APIs like Google Safe Browsing.
    
- **Rate Limiting**: Token bucket algorithm by IP or user.
    
- **CAPTCHA**: For unauthenticated users after N attempts.
    
- **Blacklist Domains**: Prevent shortening of suspicious domains.
    

---

### 🚦 **7. How do you ensure low latency on redirection?**

**Answer:**

- **Cache** the mapping: Short code → Long URL in Redis.
    
- Use **Geo-CDNs** to reduce latency.
    
- Use read-replicas for scaling DB read operations.
    
- Pre-warm cache for popular links.
    

---

### 📈 **8. How do you track analytics (clicks, geo, browser)?**

**Answer:**

- On each redirect:
    
    - Log metadata: IP, referrer, browser-agent, timestamp.
        
    - Push logs to Kafka or Kinesis.
        
    - Consume into an OLAP store (like Redshift or ClickHouse).
        
- Use this for:
    
    - Real-time dashboards.
        
    - Abuse detection.
        
    - Per-user metrics.
        

---

### 🧑‍🤝‍🧑 **9. Do users shortening the same URL get the same short URL?**

**Answer:**

Depends on the business requirement:

- **Unique per URL**: Use hash of long URL, return existing mapping if found.
    
- **Unique per user**: Allow each user to generate their own short URL even if long URL is same.
    

👉 In most real systems, per-user uniqueness is preferred to track usage independently.

---

### 🌍 **10. How would you design it for multi-region deployment?**

**Answer:**

- **DNS routing** to nearest region using Route53/GSLB.
    
- Replicate DBs across regions using:
    
    - DynamoDB global tables
        
    - Active-active PostgreSQL with conflict resolution
        
- Use **Geo-local caching** for short code lookups.
    
- Use **eventual consistency** for creating mappings globally.
    

---

### ✅ Bonus Follow-Ups

#### 🔄 Can users edit/delete short URLs?

- Store a user ID with the short URL.
    
- Only allow editing/deleting for the owner.
    
- Soft-delete (mark as deleted) to preserve analytics.
    

#### 🔑 How to test the system?

- Use **Locust or JMeter** for load testing.
    
- Test read/write QPS separately.
    
- Validate cache hit ratio under load.



first long url -> hex using hash function 
hex -> decimal 
decimal (use base 62 encoder) -> then generate a base 62 encoder and store it too database


### **1. URL Shortening Flow (Write Path)**

```
Client Request (POST /shorten)
↓
Load Balancer (Round Robin/Least Connections)
↓
Application Server/Worker Node
├─ Input Validation
├─ Rate Limiting Check
├─ Duplicate Check (Cache → Database)
└─ If duplicate exists → return existing short URL
↓
Key Generation Service (KGS)
├─ Generate unique short code (Base62)
├─ Collision detection
└─ Return unique code
↓
Database Write
├─ Store mapping: short_code → original_url
├─ Store metadata (user_id, created_at, expires_at)
└─ Update analytics counters
↓
Cache Update
├─ Store in Redis/Memcached
└─ Set TTL for expiration
↓
Response to Client
└─ Return short URL: domain.com/abc123
```

### **2. URL Redirection Flow (Read Path)**

```
Client Request (GET /abc123)
↓
Load Balancer
↓
Application Server/Worker Node
├─ Extract short_code: "abc123"
├─ Input validation
└─ Rate limiting check
↓
Cache Lookup (Redis)
├─ Cache HIT → Get original_url
└─ Cache MISS → Query database
↓
Database Query (if cache miss)
├─ SELECT original_url FROM urls WHERE short_code = 'abc123'
├─ Update cache with result
└─ Return original_url
↓
Analytics (Async)
├─ Log click event
├─ Update click counters
├─ Store user agent, IP, referrer
└─ Queue for batch processing
↓
HTTP Redirect Response
├─ Status: 301 (permanent) or 302 (temporary)
├─ Location: original_url
└─ Client browser redirects automatically
```

### **3. Ideal System Components**

```
┌─────────────────┐    ┌──────────────────┐
│   Client Apps   │    │   CDN/Edge Cache │
└─────────────────┘    └──────────────────┘
          │                       │
          └───────────┬───────────┘
                      │
          ┌──────────────────┐
          │   Load Balancer  │ (Nginx/HAProxy)
          └──────────────────┘
                      │
    ┌─────────────────┼─────────────────┐
    │                 │                 │
┌───▼───┐       ┌───▼───┐       ┌───▼───┐
│Worker │       │Worker │       │Worker │
│Node 1 │       │Node 2 │       │Node N │
└───┬───┘       └───┬───┘       └───┬───┘
    │               │               │
    └───────────────┼───────────────┘
                    │
        ┌──────────────────┐
        │  Cache Layer     │ (Redis Cluster)
        │  - Hot URLs      │
        │  - Rate Limits   │
        │  - Session Data  │
        └──────────────────┘
                    │
        ┌──────────────────┐
        │  Database        │ (Distributed)
        │  - Sharded       │
        │  - Master/Slave  │
        │  - Auto-scaling  │
        └──────────────────┘
                    │
        ┌──────────────────┐
        │  Analytics DB    │ (Separate)
        │  - Click events  │
        │  - Time-series   │
        │  - Batch writes  │
        └──────────────────┘
```


![[Pasted image 20250723233600.png]]


##### PROBLEM ->But for wach worker it has its own redis cluster and then how it wil comunicate and each worker will genrate differnt short url for same long url



Excellent follow-up — this is where **distributed system consistency** really gets tested. Let’s go step-by-step and clarify **why multiple Redis clusters per worker causes problems**, and **how to solve it**.

---

## ❓Problem Breakdown

> "**Each worker node has its own Redis cluster.**"

Now imagine:

- **Worker 1 (W1)** and **Worker 2 (W2)** receive the **same long URL**.
    
- Both have **independent Redis clusters** (say `R1`, `R2`).
    
- W1 checks Redis (`R1`) → not found.
    
- W2 checks Redis (`R2`) → not found.
    
- Both generate different short codes (random or salted hash).
    
- Both store different mappings in their **own Redis and maybe even DB**.
    

➡️ **Disaster**: Same long URL → **two different short URLs**.

---

## ⚠️ Why This Happens

Because:

1. Redis clusters are **isolated per worker**.
    
2. There is **no shared view of state**.
    
3. There’s **no distributed coordination**.
    

In other words:

> "Redis is being used like a local cache, but the problem is global."

---

## ✅ Correct Solutions (to Ensure Consistency)

---

### ✅ Option 1: **Use a Central Redis Cluster (Shared by All Workers)**

- All worker nodes connect to **the same Redis cluster**.
    
- This acts as a **distributed coordination layer**:
    
    - Locks.
        
    - Shared cache for long_url → short_code.
        

#### Flow:

1. W1 and W2 both receive `long_url_123`.
    
2. Both connect to **shared Redis**.
    
3. First one to acquire Redis lock (`SETNX lock:long_url_123`) proceeds.
    
4. Second waits or retries.
    
5. Mapping is written **once**, and then cached in Redis for fast reads.
    

#### Benefits:

- Simple.
    
- Consistent.
    
- Fast.
    

#### Drawback:

- Redis becomes a **central dependency** (can be scaled using clustering and sharding).
    

---

### ✅ Option 2: **Move Deduplication Logic to the Database**

Even without shared Redis, you can rely on **database-level coordination**.

#### Flow:

1. Both workers compute deterministic short codes (e.g., via hashing).
    
2. Each tries:
    
    sql
    
    CopyEdit
    
    `INSERT INTO url_map(short_code, long_url) VALUES ('abc123', 'https://xyz.com') ON CONFLICT (short_code) DO NOTHING;`
    
3. One succeeds; other fails and fetches:
    
    sql
    
    CopyEdit
    
    `SELECT short_code FROM url_map WHERE long_url = 'https://xyz.com';`
    

This uses the **database itself as the coordination mechanism**, **no Redis needed**.

---

### ✅ Option 3: **Use a Shared Message Queue + Coordinator Worker**

If you **must keep Redis per worker** for performance/isolation:

- Make **workers push incoming long URLs to a central Kafka/SQS queue**.
    
- A single "coordinator worker" consumes and:
    
    - Checks the DB or centralized Redis.
        
    - Writes mapping.
        
    - Publishes short URL back to the original requester.
        

➡️ This **removes race conditions completely**.

---

## ❌ What **Not** To Do

- Having each worker **with its own Redis** and no shared DB will cause:
    
    - **Split brain** situation.
        
    - Inconsistent mappings.
        
    - User confusion (same link → different short URLs).
        

---

## 🧠 TL;DR

|Option|Redis Setup|Safe in Parallel?|Notes|
|---|---|---|---|
|❌ Per-worker Redis|❌ No|Causes race and inconsistency||
|✅ Shared Redis|✅ Yes|Use for locks + caching||
|✅ No Redis, use DB|✅ Yes|Use hash + `INSERT ON CONFLICT`||
|✅ Central queue|✅ Yes|Requires more infra but fully safe||

---

## 🔚 Final Recommendation

If your architecture allows:

> ✅ **Use a shared Redis** cluster (or shared DB) to coordinate between workers.

Otherwise:

> ✅ Let DB handle uniqueness via deterministic hashing and conflict-resolution logic.