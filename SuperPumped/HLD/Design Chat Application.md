# 1. System Requirements

- Peer-to-peer messaging
- Group messaging
- Last seen/online/offline status
- Chat history
- User authentication
---
# 2. System Architecture

The architecture will follow a microservices pattern with the following core components:

- **Client Applications**: Mobile, web, and desktop interfaces
- **API Gateway**: Entry point for all client requests
- **Authentication Service**: Handles user registration, login, and token validation
- **Chat Service**: Core message handling and delivery
- **Presence Service**: Manages online/offline status and last seen timestamps
- **Notification Service**: Manages push notifications for offline users
- **Storage Layer**: Databases for persistence

---

# 3. Technology Choices with Reasoning

## 3.1 Communication Protocol

**Primary: WebSockets**

- Reasoning: WebSockets provide a persistent, bidirectional connection between client and server. This is essential for chat applications because:
    - Messages can be pushed to clients in real-time without polling
    - Connection overhead is minimized after initial handshake
    - Reduces bandwidth usage compared to HTTP polling
    - Provides lower latency for message delivery (crucial for chat experience)
    - Supports client-to-server and server-to-client communication efficiently

**Secondary: REST APIs**

- Reasoning: REST APIs complement WebSockets for specific operations:
    - Authentication flows (login/logout) where stateless requests are preferable
    - Resource-intensive operations like fetching extended chat history
    - Operations that don't require real-time feedback
    - System administration functions
    - File uploads and media sharing where WebSockets may not be optimal

---

## 3.2 Database Selection

**Primary: Apache Cassandra**
- Reasoning:
    - **Write optimization**: Chat applications are write-heavy, and Cassandra excels at write throughput
    - **Horizontal scalability**: Scales linearly by simply adding more nodes
    - **Decentralized architecture**: No single point of failure
    - **Tunable consistency**: Flexibility to choose consistency levels for different operations
    - **Time-series efficiency**: Naturally handles time-ordered data like messages
    - **Partition tolerance**: Continues functioning even with network partitions

**Secondary: Redis**
- Reasoning:
    - **In-memory performance**: Sub-millisecond access for presence data
    - **Data structure variety**: Supports complex operations with specialized data structures
    - **Built-in TTL**: Automatically handles expiry for temporary data
    - **Pub/Sub capabilities**: Enables real-time notifications across the system
    - **Message queuing**: Efficiently handles temporary message storage for offline users

---

## 3.3 Schema Design

**Cassandra Schema with Reasoning:**

```
// Users table
CREATE TABLE users (
  user_id UUID PRIMARY KEY,
  username TEXT,
  email TEXT,
  password_hash TEXT,
  created_at TIMESTAMP
);
```
- Reasoning: Simple lookup by user_id, which will be referenced in other tables


```
// Messages table
CREATE TABLE messages (
  conversation_id UUID,
  message_id TIMEUUID,
  sender_id UUID,
  message_text TEXT,
  timestamp TIMESTAMP,
  read_status MAP<UUID, BOOLEAN>,
  PRIMARY KEY (conversation_id, message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);
```
- Reasoning:
    - Composite primary key enables efficient retrieval by conversation
    - TIMEUUID for automatic time-based ordering while maintaining uniqueness
    - Clustering order optimized for retrieving recent messages first
    - Map structure for read status avoids separate tracking table


```
// Conversations table
CREATE TABLE conversations (
  conversation_id UUID PRIMARY KEY,
  conversation_name TEXT,
  conversation_type TEXT,  // 'direct' or 'group'
  created_at TIMESTAMP,
  last_message_id TIMEUUID
);
```
- Reasoning: Stores metadata about conversations, linked to messages via conversation_id


```
// Conversation participants
CREATE TABLE conversation_participants (
  conversation_id UUID,
  user_id UUID,
  joined_at TIMESTAMP,
  PRIMARY KEY (conversation_id, user_id)
);
```
- Reasoning: Tracks who belongs to which conversations, especially important for group chats

**Redis Schema with Reasoning:**

```
// User presence
HASH user:{user_id}:presence
  -> status: "online"|"offline"|"away"
  -> last_seen: timestamp
  -> device_id: "mobile"|"web"|"desktop"
```
- Reasoning: Hash structure allows multiple properties with single key lookup; TTL expiry handles session timeouts


```
// Message queues for offline delivery
LIST queue:messages:{user_id} -> [message_json1, message_json2, ...]
```
- Reasoning: List structure enables FIFO processing of queued messages

---

# 4. Partitioning Strategy (Detailed Implementation)

## 4.1 Cassandra Partitioning

**Partition Key Selection:** The partition key determines how data is distributed across the cluster:
- **For messages table:**
    - Partition key: `conversation_id`
    - Reasoning: Messages are most frequently accessed by conversation, ensuring all messages for one conversation reside on the same node (or replica set)
- **For users table:**
    - Partition key: `user_id`
    - Reasoning: User lookups happen by ID, distributing user data evenly


**Physical Implementation:**
1. **Consistent Hashing**: Cassandra applies a hash function to the partition key:
```
node_number = hash(partition_key) % number_of_nodes
```
    
2. **Virtual Nodes (Vnodes)**: Each physical node contains multiple virtual nodes
    - Improves data distribution
    - Simplifies adding/removing nodes
    - Better handles heterogeneous hardware
3. **Data Distribution Example:**
    - If hash(conversation_123) = 42 and maps to Node A
    - All messages for conversation_123 go to Node A
    - When Node B is added, only some conversations redistribute
    - Typically only ~1/n of data moves when adding the nth node
4. **Replication Strategy:**
    - NetworkTopologyStrategy with replication factor 3
    - Data replicated across multiple data centers for fault tolerance

---

## 4.2 Query Patterns and Access

**Reading Messages:** When a user opens a conversation, we query:

```
SELECT * FROM messages 
WHERE conversation_id = ? 
ORDER BY message_id DESC 
LIMIT 50;
```

- This query goes directly to the nodes containing the conversation's data
- No need to search across the entire cluster

**Writing Messages:**
1. Write request goes to any node (coordinator)
2. Coordinator determines which nodes own the partition
3. Data written to all replica nodes based on consistency level

---

# 5. Service Workflows

## 5.1 Authentication Service Workflow
1. User provides credentials
2. Service validates credentials against users table
3. On success, generates JWT token with user_id and permissions
4. Token returned to client for subsequent authenticated requests
5. Token verification handled through API Gateway for all secured endpoints

## 5.2 Presence Service Workflow
1. User connects via WebSocket
2. Connection established through API Gateway to Presence Service
3. Periodic heartbeats from client refresh the TTL
4. Service publishes status change to Redis pub/sub channel
5. Other services subscribe to presence updates for relevant users


## 5.3 Chat Service Workflow for Message Scenarios

#### Scenario 1: Sender and Receiver Both Online
1. Sender sends message to Chat Service via WebSocket
2. Chat Service:
    - Validates message format and permissions
    - Assigns message_id (TIMEUUID)
    - Writes to Cassandra messages table
    - Queries Presence Service to confirm receiver is online
    - Routes message to receiver's WebSocket connection
3. Receiver client:
    - Displays message
    - Sends delivery confirmation
4. Chat Service:
    - Updates read_status in messages table
    - Confirms delivery to sender

```
Sender Client -> API Gateway -> Chat Service -> Cassandra
                                   |
                                   v
                              Presence Service
                                   |
                                   v
Sender Client <- API Gateway <- Chat Service -> API Gateway -> Receiver Client
```

---
#### Scenario 2: Sender Online, Receiver Offline

1. Sender sends message to Chat Service via WebSocket
2. Chat Service:
    - Validates message and permissions
    - Writes to Cassandra messages table
    - Queries Presence Service and finds receiver is offline
    - Adds message to Redis queue:
        
	```
	RPUSH queue:messages:{receiver_id} {message_json}
	```

    - Notifies Notification Service
3. Notification Service:
    - Retrieves user's device tokens
    - Sends push notification via FCM/APNS
4. When receiver comes online:
    - Presence Service detects status change
    - Triggers Chat Service to check message queue
    - Chat Service delivers queued messages via WebSocket
    - Receiver confirms receipt
    - Messages removed from queue

```
Sender Client -> API Gateway -> Chat Service -> Cassandra
                                   |
                                   v
                              Presence Service
                                   |
                                   v
                            Notification Service -> Push Notification -> Receiver Device
                                   
When receiver comes online:
Receiver Client -> API Gateway -> Presence Service
                                     |
                                     v
Receiver Client <- API Gateway <- Chat Service <- Redis (message queue)
```

---

#### Scenario 3: Group Messaging
1. Sender sends message to group conversation
2. Chat Service:
    - Validates message
    - Writes single copy to Cassandra
    - Queries conversation_participants to get all members
    - For each member:
        - Checks online status via Presence Service
        - If online: Routes message via WebSocket
        - If offline: Queues message + triggers notification
3. Online recipients confirm receipt individually
4. Chat Service updates read_status map with confirmations

```
Sender -> API Gateway -> Chat Service -> Cassandra
                            |
                            v
                       Presence Service
                            |
                     ┌──────┴──────┐
                     v             v
       Online recipients      Notification Service
                                    |
                                    v
                            Offline recipients
```

---

#### Scenario 4: Last Seen Implementation

##### Step 1: Tracking User Activity
When a user is active in the application, their client sends signals to the server:
- **Initial connection**: When a user logs in, the Presence Service records their status as "online" and stores the current timestamp
- **Heartbeats**: The client sends periodic heartbeat signals (every 30 seconds) to indicate continued activity
- **Explicit actions**: User actions like sending messages or navigating between conversations also serve as activity signals.

---
##### Step 2: Handling User Disconnections
Users can disconnect from the application in several ways:
###### Explicit Logout
When a user explicitly logs out:
1. The client sends a logout request
2. The Presence Service updates their status
3. The "last_seen" timestamp is set to the moment of logout
4. This record remains in Redis without expiration to preserve the last seen time

###### Connection Loss Without Logout
If a user closes their app without logging out:
1. The client stops sending heartbeats
2. After a defined timeout period (typically 60 seconds with no heartbeat), the Presence Service:
3. The "last_seen" timestamp reflects their last detected activity

###### Session Expiration
If a user's presence record expires due to inactivity:
1. Redis automatically removes the key after the TTL period
2. The last known timestamp before expiration is preserved in a separate long-term storage:
3. This ensures we maintain last seen data even for long-inactive users
---
##### Step 3: Presence Information Retrieval
When a user wants to see when another user was last active:
1. The client requests the presence information for a specific user
2. The Presence Service checks Redis:
3. If the key exists, the service returns the status and last_seen timestamp
4. If the key doesn't exist (expired), the service checks the long-term storage:
5. The service formats the response based on the elapsed time:
    - "Online" - If status is "online"
    - "Away" - If status is "away"
    - "Last seen X minutes ago" - If status is "offline" or from long-term storage
    - "Last seen today at 2:30 PM" - For same-day activity
    - "Last seen on April 5" - For previous days

---

# 6. System Scaling Considerations

1. **Horizontal Scaling:**
    - Stateless services (Auth, API Gateway) scale via load balancing
    - Stateful services (Chat, Presence) scale with sticky sessions or distributed state
2. **Cassandra Cluster Expansion:**
    - Add nodes to increase capacity
    - Data automatically rebalances across new topology
    - No downtime required for scaling operations
3. **Redis Cluster for Presence:**
    - Redis Cluster for partitioning presence data
    - Redis Sentinel for high availability
    - Data partitioned by user_id hash
4. **WebSocket Connection Management:**
    - Connection pools distributed across servers
    - Consistent hashing to route users to servers
    - Connection metadata stored in Redis for failover

---

# How do we identify the receiver's WebSocket connection?

- To identify the receiver's WebSocket connection, we use a connection registry system. Here's how it works:
- When a user establishes a WebSocket connection, we create a mapping between their user ID and their connection information. This mapping is stored in a distributed system that allows quick lookups.
- The process works like this:
	1. When a user connects via WebSocket, the system generates a unique connection ID
	2. This connection is registered in our connection registry with a mapping:
	```
	user_id -> {connection_id, server_id, session_data}
	```
	3. When we need to send a message to a specific user, the Chat Service:
		- Looks up the user's connection details from Redis
		- Finds which server is handling their connection
		- Routes the message to that specific server
		- The server then pushes the message through the appropriate WebSocket
- For scalability, these connections are distributed across multiple servers. If the receiver is connected to a different server than the sender, the message is routed internally between servers using a message bus or direct server communication.
- If a user has multiple active sessions (e.g., web and mobile), we maintain multiple connection entries for that user ID, and messages are delivered to all active connections unless a specific device is targeted.

---
