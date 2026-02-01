https://systemdesignschool.io/problems/google-calendar/solution

## 1️⃣ What are we designing? (Problem Statement)

Design **Google Calendar** — a distributed system that allows users to:

- Create, update, delete events
    
- Invite users
    
- Handle recurring events
    
- Send notifications & reminders
    
- Support real-time updates across devices
    
- Work globally with **low latency & high availability**
    

---

## 2️⃣ Functional Requirements (FR)

### Core

- Create / Update / Delete events
    
- View calendar (day / week / month)
    
- Invite participants
    
- Accept / Reject invitations
    
- Set reminders (email / push / SMS)
    
- Support **recurring events**
    
- Handle time zones
    
- Free/Busy availability
    

### Advanced

- Event sharing & permissions
    
- Sync across devices instantly
    
- Offline edits + later sync
    
- Search events
    
- Attach conferencing links (Meet/Zoom)
    

---

## 3️⃣ Non-Functional Requirements (NFR) – Deep Dive

|Requirement|Target|
|---|---|
|Availability|**99.99%**|
|Latency|< 100 ms for reads|
|Scalability|100M+ DAU|
|Consistency|Strong for writes, eventual for reads|
|Durability|No event loss|
|Fault Tolerance|Multi-region|
|Security|OAuth2, encryption|
|Observability|Logs, metrics, traces|

👉 **Reads dominate writes**  
👉 Peak traffic during **work hours globally**

---

## 4️⃣ High-Level Architecture

![https://media.geeksforgeeks.org/wp-content/uploads/20230405135659/calendar%281%29.webp](https://media.geeksforgeeks.org/wp-content/uploads/20230405135659/calendar%281%29.webp)

![https://docs.oracle.com/cd/E54932_01/doc.705/e54933/img/sys_arch.png](https://docs.oracle.com/cd/E54932_01/doc.705/e54933/img/sys_arch.png)

![https://www.researchgate.net/publication/221239017/figure/fig2/AS%3A667802625142811%401536227961809/Event-management-framework-architecture.png](https://www.researchgate.net/publication/221239017/figure/fig2/AS%3A667802625142811%401536227961809/Event-management-framework-architecture.png)

### Core Components

`Client (Web / Mobile)         |    API Gateway         | Auth Service (OAuth)         | Calendar Service         | ------------------------------------------------- | Event Store | User Store | Invitation Store | -------------------------------------------------         |  Notification Service         |  Email / Push / SMS`

---

## 5️⃣ Core Services Breakdown

### 1. API Gateway

- Rate limiting
    
- Authentication
    
- Request routing
    
- Throttling abusive clients
    

---

### 2. Authentication Service

- OAuth2 based
    
- Issues JWT tokens
    
- Validates identity & permissions
    

---

### 3. Calendar Service (Core Brain 🧠)

Responsibilities:

- CRUD events
    
- Handle recurring rules
    
- Conflict detection
    
- Time zone normalization
    
- Permissions validation
    

This service is **stateless** → horizontally scalable.

---

### 4. Event Store (Data Layer)

#### Why NOT simple SQL?

- Huge scale
    
- Recurring event expansion
    
- Heavy read patterns
    

👉 **Hybrid Storage**

|Data|Storage|
|---|---|
|Event metadata|Distributed SQL (Spanner-like)|
|Recurrence rules|Separate table|
|User calendar view|Precomputed cache|
|Search|Elasticsearch|

---

### 5. Notification Service

- Async processing
    
- Reminder scheduling
    
- Retry on failure
    
- Idempotent delivery
    

Uses:

- Message Queue (Kafka / PubSub)
    
- Worker pools
    

---

## 6️⃣ Data Model (Interview Favorite ⭐)

### Event Table

`event_id (PK) organizer_id start_time_utc end_time_utc timezone title location recurrence_rule_id (nullable) status (active / cancelled) version`

---

### Recurrence Rule Table

`rule_id frequency (DAILY / WEEKLY / MONTHLY) interval by_day until`

💡 **Recurring events are NOT expanded eagerly**  
They’re expanded **on read**, with caching.

---

### Invitation Table

`event_id user_id status (PENDING / ACCEPTED / DECLINED)`

---

## 7️⃣ Read & Write Flow (VERY IMPORTANT)

### 🟢 Create Event Flow

`Client → API Gateway       → Auth Service (validate token)       → Calendar Service       → Validate time & conflicts       → Save event in DB       → Publish "EventCreated" message       → Notification Service schedules reminders`

---

### 🔵 View Calendar Flow

`Client → API Gateway       → Calendar Service       → Cache lookup (Redis)       → If miss → DB       → Expand recurrence       → Return events`

💡 **Heavy caching here** (user-day/week view)

---

## 8️⃣ Handling Conflicts (Tricky Interview Question)

When two users edit same event:

- Use **Optimistic Locking**
    
- `version` field
    
- Reject stale updates with `409 Conflict`
    

---

## 9️⃣ Time Zones (Common Pitfall ⚠️)

✔ Store everything in **UTC**  
✔ Save original timezone for display  
✔ Convert on client

---

## 🔁 Offline Sync Strategy

- Client stores edits locally
    
- Sends batch updates later
    
- Server resolves conflicts using versioning
    

---

## 10️⃣ Scalability Strategy

### Horizontal Scaling

- Stateless services
    
- Auto-scaling based on QPS
    

### Database

- Shard by `user_id`
    
- Read replicas
    
- Geo-replication
    

### Caching

- Redis for hot calendars
    
- TTL-based eviction
    

---

## 11️⃣ Consistency Model

|Operation|Consistency|
|---|---|
|Create / Update event|Strong|
|Calendar view|Eventual|
|Notifications|At-least-once|

---

## 12️⃣ Failure Handling

|Failure|Strategy|
|---|---|
|Notification failure|Retry + DLQ|
|DB failure|Multi-region replicas|
|Cache failure|Fallback to DB|
|Partial writes|Transaction rollback|

---

## 13️⃣ Security

- OAuth2 authentication
    
- Role-based access control
    
- Event-level ACLs
    
- Data encryption at rest & transit
    

---

## 14️⃣ Monitoring & Observability

- Metrics: QPS, latency, errors
    
- Distributed tracing (request → DB → notification)
    
- Alerts on missed reminders (critical!)
    

---

## 15️⃣ Interview-Level Follow-Up Questions

🔥 **Expect these**:

1. How do you scale recurring events?
    
2. How do you avoid duplicate notifications?
    
3. What happens if reminder service is down?
    
4. How do you support real-time updates?
    
5. How do you handle daylight saving changes?
    

---

## 16️⃣ One-Line Summary (Use This in Interview)

> “Google Calendar is a globally distributed, event-driven system with strong consistency for writes, heavy caching for reads, async notifications, and recurrence expansion on demand.”
