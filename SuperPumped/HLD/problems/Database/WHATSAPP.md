## The Core Problem WhatsApp Solves

On the surface messaging seems simple — send text, deliver to recipient. But consider the real constraints:

```
WhatsApp scale reality:
────────────────────────────────────────────────
Active users:              2 billion+
Daily active users:        1 billion+
Messages per day:          100 billion+ (1.2M messages/second sustained)
Peak messages:             5 million messages/second (New Year's Eve)
Average messages/user:     50-100 per day
Group size:                Up to 1,024 members
Media messages:            50% of all messages (images, videos, voice notes)
Online status updates:     1 billion users × every 30 seconds = 33M updates/second

Requirements:
→ Deliver message to recipient within <1 second
→ Never lose messages (durability)
→ Show delivery status (sent ✓, delivered ✓✓, read ✓✓ blue)
→ Work offline (queue messages, deliver when online)
→ End-to-end encryption (server cannot read messages)
→ Group messages to 1,024 people instantly
→ Multimedia messages (up to 100MB videos)
→ Show "typing..." indicator in real-time
→ Show online/offline status in real-time
→ Query: "Show conversation history with user X" (<100ms)
→ Cross-device sync (phone + web + tablet)
```

This combination of **massive write throughput (1.2M msg/sec) + real-time delivery (<1s) + end-to-end encryption + group fan-out + offline queuing + status tracking** is what forces this specific architecture.

---

## Why Cassandra for Messages?

### The Message Write Pattern

```
MESSAGE CHARACTERISTICS:
════════════════════════════════════════════════════════

Write pattern:
- 1.2 million messages/second sustained
- 5 million messages/second peak
- Pure append (messages never updated, only status changes)
- Time-series data (timestamp critical for ordering)
- Partition by user_id + conversation_id

Read pattern:
- "Show conversation with user X" (most common)
- Always ordered by timestamp (newest first)
- Pagination (load 50 messages, scroll up for more)
- Rarely query old messages (>1 month)

Data volume:
- 100 billion messages/day × 365 = 36.5 trillion messages/year
- Average message: 500 bytes (text) to 100MB (video)
- Text messages: ~200 bytes average
- Total: ~10 petabytes/year (text only)

This is a MASSIVE write-heavy, time-series, append-only workload
Cassandra is PERFECT for this
```

### Cassandra Schema Design

```
CASSANDRA SCHEMA:
════════════════════════════════════════════════════════

Messages table:
────────────────────────────────────────────────────────────────────────────────
user_id      │ conversation_id │ message_id   │ timestamp           │ sender_id │ text      │ media_url │ status
───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
user_001     │ conv_001_002    │ msg_abc      │ 2024-02-26 10:00:00 │ user_001  │ "Hi!"     │ NULL      │ delivered
user_001     │ conv_001_002    │ msg_xyz      │ 2024-02-26 10:01:00 │ user_002  │ "Hello!"  │ NULL      │ read
user_002     │ conv_001_002    │ msg_abc      │ 2024-02-26 10:00:00 │ user_001  │ "Hi!"     │ NULL      │ read
user_002     │ conv_001_002    │ msg_xyz      │ 2024-02-26 10:01:00 │ user_002  │ "Hello!"  │ NULL      │ delivered

PRIMARY KEY ((user_id, conversation_id), timestamp, message_id)
CLUSTERING ORDER BY (timestamp DESC, message_id DESC)

Why this schema:
────────────────────────────────────────────────

Partition key: (user_id, conversation_id)
→ All messages in a conversation for one user in same partition
→ Efficient query: "show conversation between user_001 and user_002"
→ Each user gets their own copy of conversation

Clustering key: timestamp (DESC), message_id (DESC)
→ Messages naturally sorted newest-first within partition
→ Most recent messages at top (common access pattern)
→ message_id breaks ties for same timestamp

Denormalization:
→ Same message stored TWICE (once per user)
→ user_001 has copy in their partition
→ user_002 has copy in their partition
→ Allows independent access patterns
→ Enables different delivery status per recipient


Why denormalize:
────────────────────────────────────────────────
Cassandra has no JOINs
Must query by partition key
Different users need independent status tracking:
- user_001 sees: "delivered" (message sent to user_002)
- user_002 sees: "read" (user_002 read the message)

Storage is cheap (~$20/TB/month)
Write throughput is cheap (Cassandra strength)
Duplication is acceptable tradeoff for query performance
```

### Cassandra Query Patterns

```
QUERY 1: Load conversation (most common query)
════════════════════════════════════════════════════════

"Show me last 50 messages with user_002"

SELECT message_id, timestamp, sender_id, text, media_url, status
FROM messages
WHERE user_id = 'user_001'
AND conversation_id = 'conv_001_002'
ORDER BY timestamp DESC
LIMIT 50;

Cassandra execution:
────────────────────────────────────────────────
1. Hash (user_001, conv_001_002) → Node 3
2. Go directly to Node 3's partition
3. Read from SSTable (data already sorted by timestamp DESC)
4. Return top 50 rows

Query time: <10ms
No index needed
No sorting needed (already sorted)
Linear scan of 50 rows only


QUERY 2: Pagination (scroll up for older messages)
════════════════════════════════════════════════════════

"Show next 50 messages before timestamp X"

SELECT message_id, timestamp, sender_id, text, media_url, status
FROM messages
WHERE user_id = 'user_001'
AND conversation_id = 'conv_001_002'
AND timestamp < '2024-02-26 09:00:00'
ORDER BY timestamp DESC
LIMIT 50;

Uses same partition
Continues reading from SSTable
Query time: <10ms


QUERY 3: Mark message as delivered/read
════════════════════════════════════════════════════════

UPDATE messages
SET status = 'read'
WHERE user_id = 'user_001'
AND conversation_id = 'conv_001_002'
AND timestamp = '2024-02-26 10:01:00'
AND message_id = 'msg_xyz';

Cassandra write path:
1. Append to commit log (sequential disk write)
2. Update MemTable (in-memory)
3. Acknowledge immediately

Update time: <5ms
```

### Why PostgreSQL Cannot Handle This

```
POSTGRESQL ATTEMPT:
════════════════════════════════════════════════════════

Messages table:
────────────────────────────────────────────────
message_id │ conversation_id │ sender_id │ timestamp           │ text │ status
───────────────────────────────────────────────────────────────────────────────────
msg_abc    │ conv_001_002    │ user_001  │ 2024-02-26 10:00:00 │ "Hi!"│ sent
msg_xyz    │ conv_001_002    │ user_002  │ 2024-02-26 10:01:00 │ "Hello!"│ delivered

Message_Recipients (for delivery tracking):
────────────────────────────────────────────────
message_id │ recipient_id │ status
──────────────────────────────────────────────────
msg_abc    │ user_002     │ delivered
msg_xyz    │ user_001     │ read


Problems at WhatsApp scale:
────────────────────────────────────────────────

1. Write throughput:
────────────────────────────────────────────────
1.2 million messages/second = 1.2M INSERTs/second
Each INSERT:
- Updates B-tree index on conversation_id
- Updates B-tree index on timestamp
- Updates B-tree index on recipient_id (in recipients table)

PostgreSQL max write throughput: ~10K writes/second (single node)
Need 120 PostgreSQL servers just for writes
Unmanageable


2. Table size after 1 year:
────────────────────────────────────────────────
36.5 trillion messages
Even with indexes, queries become slow
VACUUM cannot keep up with insert rate
Autovacuum lag causes table bloat
Query performance degrades over time


3. Hot partitions:
────────────────────────────────────────────────
Popular users (celebrities) have millions of messages
All in same conversation_id
Queries scan millions of rows even with index
Index bloat on conversation_id


4. Horizontal scaling:
────────────────────────────────────────────────
Sharding PostgreSQL by user_id is complex
Requires application-level routing
Rebalancing shards is painful
No native support


Cassandra solves ALL these problems:
────────────────────────────────────────────────
✓ 1M+ writes/second per node (horizontal scaling)
✓ No index maintenance (clustering key sorts naturally)
✓ No VACUUM overhead (append-only, compaction in background)
✓ Partitioning by (user_id, conversation_id) distributes load
✓ Native horizontal scaling (add nodes → more capacity)
✓ Time-based compaction (old messages auto-archived)
```

---

## Why Redis for Online Status?

### The Real-Time Presence Problem

```
ONLINE STATUS REQUIREMENTS:
════════════════════════════════════════════════════════

1 billion users online simultaneously
Each user:
- Opens WhatsApp app
- Sends heartbeat every 30 seconds
- 1B / 30s = 33 million status updates per second

Queries:
- "Is user_002 online?" (every time user_001 opens chat)
- "Show online status for all my contacts" (on app open)
- 1B users × 50 contacts avg = 50B status checks per day
- = 580K status queries/second sustained

This is PURE key-value workload
Extremely high read/write throughput
Low latency required (<10ms)
Perfect for Redis
```

### Redis Schema for Presence

```
REDIS SCHEMA:
════════════════════════════════════════════════════════

User online status:
────────────────────────────────────────────────
Key:   online:user_001
Value: "1"  (online) or NULL (offline)
TTL:   45 seconds (expire if no heartbeat)

SET online:user_001 "1" EX 45

Every 30 seconds, app sends heartbeat:
SET online:user_001 "1" EX 45

If user closes app or loses connection:
No heartbeat → TTL expires → key deleted → offline

Check if user online:
GET online:user_001
→ Returns "1" (online) or NULL (offline)

Query time: <1ms


User's WebSocket connection mapping:
────────────────────────────────────────────────
Key:   connection:user_001
Value: "ws_server_3"  (which WebSocket server user connected to)
TTL:   60 seconds

SET connection:user_001 "ws_server_3" EX 60

When message arrives for user_001:
GET connection:user_001
→ Returns "ws_server_3"
→ Route message to WebSocket server 3
→ Server 3 pushes to user_001's active connection

This enables message routing across distributed WebSocket servers


Last seen timestamp:
────────────────────────────────────────────────
Key:   lastseen:user_001
Value: Unix timestamp
TTL:   30 days

SET lastseen:user_001 1708945200

When user goes offline:
Set last seen timestamp
Display: "last seen today at 10:00 AM"


Typing indicator:
────────────────────────────────────────────────
Key:   typing:conv_001_002:user_001
Value: "1"
TTL:   5 seconds

SET typing:conv_001_002:user_001 "1" EX 5

As user types:
Continuously refresh TTL (every 2 seconds)

When user stops typing:
Stop refreshing → TTL expires → indicator disappears

Check if user typing:
GET typing:conv_001_002:user_001
→ Returns "1" (typing) or NULL (not typing)
```

### Why PostgreSQL/Cassandra Cannot Handle Presence

```
POSTGRESQL ATTEMPT:
════════════════════════════════════════════════════════

User_Status table:
────────────────────────────────────────────────
user_id  │ is_online │ last_heartbeat
───────────────────────────────────────────────────
user_001 │ true      │ 2024-02-26 10:00:00
user_002 │ true      │ 2024-02-26 10:00:30
...

Heartbeat updates:
UPDATE user_status
SET is_online = true,
    last_heartbeat = NOW()
WHERE user_id = 'user_001';

33 million updates per second at PostgreSQL:
→ Impossible (max 10K writes/sec)
→ B-tree index on user_id updated every write
→ Row-level locking contention
→ Vacuum cannot keep up

Background job to mark users offline:
UPDATE user_status
SET is_online = false
WHERE last_heartbeat < NOW() - INTERVAL '45 seconds';

→ Scans entire table every 30 seconds
→ 1 billion rows scanned
→ Query takes minutes
→ Stale offline status


REDIS SOLUTION:
────────────────────────────────────────────────
✓ TTL handles automatic expiration
✓ No background jobs needed
✓ 1M+ operations/second per instance
✓ <1ms per operation
✓ Horizontal scaling (cluster mode)
✓ Memory-based (instant access)

Redis is PERFECT for ephemeral, high-velocity status data
```

---

## Why PostgreSQL for User Data?

### What User Data Looks Like

```
USER DATA:
════════════════════════════════════════════════════════

{
  user_id: "user_001",
  phone_number: "+1234567890",
  profile_name: "Alice Johnson",
  profile_photo_url: "https://...",
  status_message: "Hey there! I'm using WhatsApp",
  settings: {
    privacy: {
      last_seen: "contacts",  // everyone, contacts, nobody
      profile_photo: "everyone",
      about: "contacts",
      read_receipts: true,
      groups: "everyone"  // who can add me to groups
    },
    notifications: {
      message_notifications: true,
      group_notifications: true,
      call_notifications: true,
      notification_sound: "default"
    },
    security: {
      two_step_verification: true,
      show_security_notifications: true
    }
  },
  contacts: [
    {contact_id: "user_002", name: "Bob", phone: "+..."},
    {contact_id: "user_003", name: "Charlie", phone: "+..."}
  ],
  blocked_users: ["user_999"],
  created_at: "2018-01-15",
  last_login: "2024-02-26"
}
```

This is relational data with complex structure and settings.

### PostgreSQL Schema

```
POSTGRESQL SCHEMA:
════════════════════════════════════════════════════════

Users table:
────────────────────────────────────────────────────────────────────────────────
user_id      │ phone_number  │ profile_name   │ profile_photo_url │ status_msg │ created_at │ last_login
───────────────────────────────────────────────────────────────────────────────────────────────────────────────
user_001     │ +1234567890   │ Alice Johnson  │ https://...       │ "Hey..."   │ 2018-01-15 │ 2024-02-26
user_002     │ +9876543210   │ Bob Smith      │ https://...       │ "Busy"     │ 2019-03-20 │ 2024-02-26

Indexes:
  PRIMARY KEY (user_id)
  UNIQUE (phone_number)  ← phone number is unique identifier
  INDEX (last_login)


User_Settings table:
────────────────────────────────────────────────────────────────────────
user_id  │ setting_key              │ setting_value
────────────────────────────────────────────────────────────────────────────────
user_001 │ privacy_last_seen        │ "contacts"
user_001 │ privacy_profile_photo    │ "everyone"
user_001 │ notifications_enabled    │ "true"
user_001 │ two_step_verification    │ "true"

Indexes:
  PRIMARY KEY (user_id, setting_key)


Contacts table:
────────────────────────────────────────────────────────────────────────
user_id  │ contact_user_id │ contact_name  │ contact_phone
────────────────────────────────────────────────────────────────────────────────
user_001 │ user_002        │ "Bob"         │ "+9876543210"
user_001 │ user_003        │ "Charlie"     │ "+1111111111"

Indexes:
  PRIMARY KEY (user_id, contact_user_id)


Blocked_Users table:
────────────────────────────────────────────────
user_id  │ blocked_user_id
────────────────────────────────────────────────
user_001 │ user_999


Groups table:
────────────────────────────────────────────────────────────────────────
group_id │ group_name      │ creator_id │ created_at          │ member_count
────────────────────────────────────────────────────────────────────────────────
grp_001  │ "Family"        │ user_001   │ 2024-01-01 10:00:00 │ 8
grp_002  │ "Work Team"     │ user_002   │ 2024-02-15 09:00:00 │ 15


Group_Members table:
────────────────────────────────────────────────────────────────────────
group_id │ user_id  │ role     │ joined_at
────────────────────────────────────────────────────────────────────────────────
grp_001  │ user_001 │ admin    │ 2024-01-01 10:00:00
grp_001  │ user_002 │ member   │ 2024-01-01 10:05:00
grp_002  │ user_002 │ admin    │ 2024-02-15 09:00:00

Indexes:
  PRIMARY KEY (group_id, user_id)
  INDEX (user_id)  ← find all groups user belongs to
```

### Complex Queries PostgreSQL Handles

```
QUERY 1: Get user profile with settings
════════════════════════════════════════════════════════

SELECT u.user_id, u.profile_name, u.phone_number,
       json_object_agg(s.setting_key, s.setting_value) as settings
FROM users u
LEFT JOIN user_settings s ON u.user_id = s.user_id
WHERE u.user_id = 'user_001'
GROUP BY u.user_id;

→ JOIN with aggregation
→ Returns nested settings JSON
→ Natural in SQL
→ <10ms with indexes


QUERY 2: Check if message should be delivered based on privacy
════════════════════════════════════════════════════════

"Can user_002 see user_001's online status?"

SELECT 
  CASE 
    WHEN privacy_last_seen = 'everyone' THEN true
    WHEN privacy_last_seen = 'contacts' AND EXISTS (
      SELECT 1 FROM contacts 
      WHERE user_id = 'user_001' 
      AND contact_user_id = 'user_002'
    ) THEN true
    ELSE false
  END as can_see_status
FROM user_settings
WHERE user_id = 'user_001'
AND setting_key = 'privacy_last_seen';

→ Complex conditional logic
→ Subquery for contact check
→ Natural in SQL
→ Impossible in key-value stores


QUERY 3: Find mutual groups
════════════════════════════════════════════════════════

"Which groups do user_001 and user_002 both belong to?"

SELECT g.group_id, g.group_name
FROM groups g
JOIN group_members gm1 ON g.group_id = gm1.group_id
JOIN group_members gm2 ON g.group_id = gm2.group_id
WHERE gm1.user_id = 'user_001'
AND gm2.user_id = 'user_002';

→ Self-join
→ Relational query
→ Natural in SQL
```

---

## Message Delivery Flow

### End-to-End Encryption Challenge

```
END-TO-END ENCRYPTION ARCHITECTURE:
════════════════════════════════════════════════════════

Key principle:
Server CANNOT read message content
Server only routes encrypted blobs

Encryption flow:
────────────────────────────────────────────────

User A sends message to User B:
        │
        ▼
STEP 1: Encrypt on User A's device
────────────────────────────────────────────────
plaintext = "Hello, how are you?"

User A's device:
1. Fetch User B's public key (from key server)
2. Generate session key (random AES-256 key)
3. Encrypt message with session key:
   encrypted_content = AES_encrypt(plaintext, session_key)
4. Encrypt session key with User B's public key:
   encrypted_session_key = RSA_encrypt(session_key, user_B_public_key)

Message payload:
{
  sender_id: "user_001",
  recipient_id: "user_002",
  encrypted_content: "0x7F8A9B...",  ← server cannot decrypt
  encrypted_session_key: "0x3D4E...", ← only User B can decrypt
  timestamp: 1708945200,
  message_id: "msg_abc"
}
        │
        ▼
STEP 2: Send to server
────────────────────────────────────────────────
POST /api/messages

Server receives encrypted blob
Server CANNOT read content
Server only sees metadata:
- sender_id
- recipient_id
- timestamp
- encrypted_content (opaque blob)


STEP 3: Store in Cassandra (encrypted)
────────────────────────────────────────────────
INSERT INTO messages
(user_id, conversation_id, message_id, timestamp, sender_id, encrypted_content)
VALUES
('user_001', 'conv_001_002', 'msg_abc', NOW(), 'user_001', '0x7F8A9B...'),
('user_002', 'conv_001_002', 'msg_abc', NOW(), 'user_001', '0x7F8A9B...');

Both users get copy of encrypted message
Both stored as opaque blobs


STEP 4: Deliver to User B (if online)
────────────────────────────────────────────────
Check Redis:
GET connection:user_002
→ Returns "ws_server_5"

Route to WebSocket server 5
Server 5 pushes encrypted blob to User B's device


STEP 5: Decrypt on User B's device
────────────────────────────────────────────────
User B's device receives encrypted blob:
1. Decrypt session key with User B's private key:
   session_key = RSA_decrypt(encrypted_session_key, user_B_private_key)
2. Decrypt content with session key:
   plaintext = AES_decrypt(encrypted_content, session_key)
3. Display: "Hello, how are you?"


Result:
────────────────────────────────────────────────
✓ Server never sees plaintext
✓ Messages stored encrypted in Cassandra
✓ Only sender and recipient can decrypt
✓ Even WhatsApp employees cannot read messages
✓ Government agencies cannot read messages (controversial!)
```

### Complete Message Flow

```
FLOW 1: User A Sends Message to User B (1-on-1)
════════════════════════════════════════════════════════

User A types: "Hey, let's meet at 3pm"
        │
        ▼
STEP 1: Encrypt on device
────────────────────────────────────────────────
encrypted_payload = encrypt(message, user_B_public_key)


STEP 2: Send to WhatsApp server via WebSocket
────────────────────────────────────────────────
User A connected to ws_server_2

WebSocket message:
{
  type: "MESSAGE",
  message_id: "msg_abc123",
  sender_id: "user_001",
  recipient_id: "user_002",
  encrypted_content: "0x...",
  timestamp: 1708945200
}


STEP 3: Server validates and stores
────────────────────────────────────────────────
ws_server_2 receives message

Validate:
- Check sender_id matches authenticated user
- Check recipient_id exists (PostgreSQL lookup)
- Check sender not blocked by recipient

Write to Cassandra (async, both partitions):
INSERT INTO messages
(user_id, conversation_id, message_id, ...)
VALUES
('user_001', 'conv_001_002', 'msg_abc123', ...),  ← sender's copy
('user_002', 'conv_001_002', 'msg_abc123', ...);  ← recipient's copy

Time: <5ms (write to commit log + MemTable)


STEP 4: Update delivery status (sender)
────────────────────────────────────────────────
Send acknowledgment to User A via WebSocket:
{
  type: "ACK",
  message_id: "msg_abc123",
  status: "sent"  ← one checkmark ✓
}

User A sees: ✓ (sent)


STEP 5: Check if User B is online
────────────────────────────────────────────────
Redis lookup:
GET connection:user_002

CASE 1: User B online
→ Returns: "ws_server_5"
→ Route message to ws_server_5
→ ws_server_5 pushes to User B

CASE 2: User B offline
→ Returns: NULL
→ Message queued in Cassandra
→ Will be delivered when User B comes online


STEP 6: Deliver to User B (if online)
────────────────────────────────────────────────
ws_server_5 pushes encrypted message to User B

WebSocket message:
{
  type: "NEW_MESSAGE",
  message_id: "msg_abc123",
  sender_id: "user_001",
  encrypted_content: "0x...",
  timestamp: 1708945200
}

User B's device:
1. Receives encrypted message
2. Decrypts with private key
3. Displays: "Hey, let's meet at 3pm"
4. Sends delivery acknowledgment


STEP 7: Update delivery status
────────────────────────────────────────────────
User B sends delivery ack to ws_server_5

ws_server_5:
UPDATE messages
SET status = 'delivered'
WHERE user_id = 'user_001'
AND conversation_id = 'conv_001_002'
AND message_id = 'msg_abc123';

Notify User A via WebSocket:
{
  type: "STATUS_UPDATE",
  message_id: "msg_abc123",
  status: "delivered"  ← two checkmarks ✓✓
}

User A sees: ✓✓ (delivered)


STEP 8: User B reads message
────────────────────────────────────────────────
User B opens chat (message visible on screen)

User B's app sends read receipt:
{
  type: "READ_RECEIPT",
  message_id: "msg_abc123",
  conversation_id: "conv_001_002"
}

ws_server_5:
UPDATE messages
SET status = 'read'
WHERE user_id = 'user_001'
AND conversation_id = 'conv_001_002'
AND message_id = 'msg_abc123';

Notify User A:
{
  type: "STATUS_UPDATE",
  message_id: "msg_abc123",
  status: "read"  ← blue checkmarks ✓✓
}

User A sees: ✓✓ (blue, read)


Total latency (User A press send → User B receives):
────────────────────────────────────────────────
Online: <500ms
Offline: Delivered when User B comes online
```

```
FLOW 2: Group Message (1-to-Many Fan-Out)
════════════════════════════════════════════════════════

User A sends message to group (100 members)
        │
        ▼
STEP 1: Encrypt message (once)
────────────────────────────────────────────────
Generate group session key (shared secret)
Encrypt message once with group session key

encrypted_content = AES_encrypt("Meeting at 3pm", group_session_key)


STEP 2: Fan-out to all members
────────────────────────────────────────────────
Query PostgreSQL for group members:
SELECT user_id FROM group_members WHERE group_id = 'grp_001';

Returns: 100 user_ids


STEP 3: Write to Cassandra (fan-out write)
────────────────────────────────────────────────
For each of 100 members:
INSERT INTO messages
(user_id, conversation_id, message_id, timestamp, sender_id, encrypted_content)
VALUES
('member_001', 'grp_001', 'msg_xyz', NOW(), 'user_001', '0x...'),
('member_002', 'grp_001', 'msg_xyz', NOW(), 'user_001', '0x...'),
...
('member_100', 'grp_001', 'msg_xyz', NOW(), 'user_001', '0x...');

100 writes to Cassandra
Batched for efficiency
Time: <50ms


STEP 4: Deliver to online members (async)
────────────────────────────────────────────────
For each member:
  GET connection:member_id
  
  If online:
    Route to appropriate WebSocket server
    Push encrypted message
  
  If offline:
    Message already in Cassandra
    Will be delivered when they come online

Delivery is ASYNCHRONOUS
Doesn't block sender
Sender gets "sent" status immediately


STEP 5: Track delivery per member
────────────────────────────────────────────────
Group messages have per-member delivery status:

In Cassandra messages table:
Each member's copy has individual status:
- member_001: delivered
- member_002: read
- member_003: sent (offline)
- member_004: delivered
...

Sender sees aggregate:
"Delivered to 85/100"
"Read by 42/100"


Problem with 1,024-member groups:
────────────────────────────────────────────────
Max group size: 1,024 members
1 message → 1,024 Cassandra writes

At 1M messages/second:
If all messages to 1024-member groups:
1M × 1024 = 1 BILLION writes/second

Solution: Most groups are small
Average group size: ~8 members
Actual writes: 1M × 8 = 8M writes/second
Cassandra can handle this (1M+ writes/sec per node)

For large groups (500+ members):
Use message queue (Kafka) to rate-limit delivery
Deliver in batches over 10-30 seconds
Acceptable for large broadcast groups
```

```
FLOW 3: Offline Message Delivery
════════════════════════════════════════════════════════

User B is offline for 2 hours
During this time, User A sends 10 messages
        │
        ▼
STEP 1: Messages stored in Cassandra
────────────────────────────────────────────────
All 10 messages written to user_002's partition:
(user_id='user_002', conversation_id='conv_001_002')

Status: "sent" (not delivered yet)


STEP 2: User B comes online
────────────────────────────────────────────────
User B opens WhatsApp app

App establishes WebSocket connection:
ws_connect(user_id='user_002') → ws_server_7

ws_server_7:
SET connection:user_002 "ws_server_7" EX 60
SET online:user_002 "1" EX 45


STEP 3: Fetch pending messages
────────────────────────────────────────────────
Query Cassandra for undelivered messages:
SELECT message_id, sender_id, encrypted_content, timestamp
FROM messages
WHERE user_id = 'user_002'
AND status IN ('sent', 'pending')
AND timestamp > (last_sync_timestamp)
ORDER BY timestamp ASC;

Returns: 10 messages

Push all 10 messages via WebSocket:
{
  type: "SYNC_MESSAGES",
  messages: [
    {message_id: "msg_1", sender_id: "user_001", ...},
    {message_id: "msg_2", sender_id: "user_001", ...},
    ...
  ]
}


STEP 4: Update delivery status
────────────────────────────────────────────────
For each of 10 messages:
UPDATE messages SET status = 'delivered'
WHERE user_id = 'user_002' AND message_id IN (...);

Send delivery receipts to senders:
Notify User A that messages delivered


STEP 5: Background sync
────────────────────────────────────────────────
If user was offline for days/weeks:
Sync in batches (100 messages at a time)
To avoid overwhelming connection

Progressive loading:
First: Last 50 messages of each conversation
Then: Older messages on-demand (scroll up)
```

---

## Multimedia Message Handling

```
MEDIA MESSAGE FLOW:
════════════════════════════════════════════════════════

User sends image (5MB):
        │
        ▼
STEP 1: Upload to object storage (S3/CDN)
────────────────────────────────────────────────
User A's device:
1. Compress image (if needed)
2. Encrypt image with AES-256
3. Upload to S3:
   PUT s3://whatsapp-media/user_001/img_abc123.enc

Response: media_url = "https://cdn.whatsapp.com/img_abc123"

Time: 1-5 seconds (depends on network)


STEP 2: Send message with media reference
────────────────────────────────────────────────
WebSocket message:
{
  type: "MESSAGE",
  message_id: "msg_media_001",
  sender_id: "user_001",
  recipient_id: "user_002",
  media_type: "image",
  media_url: "https://cdn.whatsapp.com/img_abc123",
  encrypted_key: "0x...",  ← decryption key for media
  thumbnail: "data:image/jpeg;base64,..."  ← small preview
}


STEP 3: Store in Cassandra (reference only)
────────────────────────────────────────────────
INSERT INTO messages
(user_id, conversation_id, message_id, media_url, encrypted_key)
VALUES
('user_001', 'conv_001_002', 'msg_media_001', 'https://...', '0x...'),
('user_002', 'conv_001_002', 'msg_media_001', 'https://...', '0x...');

Only URL stored, not actual media


STEP 4: Deliver to User B
────────────────────────────────────────────────
User B receives message with:
- Thumbnail (shows immediately)
- media_url (for full download)

User B's device:
1. Shows thumbnail immediately
2. Downloads full media from CDN (lazy load)
3. Decrypts with encrypted_key
4. Displays full image


Benefits:
────────────────────────────────────────────────
✓ Fast message delivery (only URL sent)
✓ No database bloat (media in object storage)
✓ CDN caching (popular media cached closer to users)
✓ Progressive loading (thumbnail first, full media later)
✓ Cheap storage (S3 ~$0.023/GB vs Cassandra ~$0.10/GB)
```

---

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                       CLIENT DEVICES                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ Phone 1  │  │ Phone 2  │  │  Web     │  │  Tablet  │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
└───────┼─────────────┼─────────────┼─────────────┼──────────────┘
        │             │             │             │
        └──────┬──────┴──────┬──────┴──────┬──────┘
               │             │             │
          WebSocket      WebSocket     WebSocket
               │             │             │
        ┌──────┴─────────────┴─────────────┴──────┐
        │         LOAD BALANCER                    │
        └──────┬─────────────┬─────────────┬───────┘
               │             │             │
        ┌──────▼────┐ ┌──────▼────┐ ┌─────▼─────┐
        │ WS Server │ │ WS Server │ │ WS Server │
        │     1     │ │     2     │ │     3     │
        └──────┬────┘ └──────┬────┘ └─────┬─────┘
               │             │             │
               └──────┬──────┴──────┬──────┘
                      │             │
        ┌─────────────┼─────────────┼─────────────┐
        │             │             │             │
        ▼             ▼             ▼             ▼
   ┌────────┐   ┌─────────┐   ┌─────────┐   ┌────────┐
   │ Redis  │   │Cassandra│   │PostgreSQL│   │  S3    │
   │        │   │         │   │         │   │ (media)│
   │-online │   │-messages│   │ -users  │   │        │
   │-typing │   │         │   │ -groups │   │        │
   │-connxn │   │         │   │-settings│   │        │
   └────────┘   └─────────┘   └─────────┘   └────────┘
```

---

## Tradeoffs vs Other Databases

```
┌──────────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                              │ THIS ARCH    │ POSTGRES ALL │ MONGO ALL    │ MYSQL ALL    │
├──────────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Message write throughput     │ Cassandra✓   │ 10K/sec ✗    │ 50K/sec      │ 10K/sec ✗    │
│ Conversation load (<100ms)   │ Cassandra✓   │ Slow (>1yr)  │ Good ✓       │ Slow (>1yr)  │
│ Online status (33M/sec)      │ Redis ✓      │ Impossible✗  │ Impossible✗  │ Impossible✗  │
│ User settings queries        │ PostgreSQL✓  │ PostgreSQL✓  │ MongoDB ✓    │ MySQL ✓      │
│ Group member JOINs           │ PostgreSQL✓  │ PostgreSQL✓  │ Manual ✗     │ MySQL ✓      │
│ Time-series partitioning     │ Native ✓     │ Manual       │ Manual       │ Manual       │
│ Horizontal scaling           │ Native ✓     │ Sharding     │ Native ✓     │ Sharding     │
│ TTL for ephemeral data       │ Redis ✓      │ Manual ✗     │ Native ✓     │ Manual ✗     │
│ End-to-end encryption        │ Opaque blobs✓│ Possible ✓   │ Possible ✓   │ Possible ✓   │
│ Operational complexity       │ HIGH         │ LOW          │ MEDIUM       │ LOW          │
│ Cost at WhatsApp scale       │ VERY HIGH    │ Impossible   │ HIGH         │ Impossible   │
└──────────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **Cassandra stores messages partitioned by (user_id, conversation_id) with clustering on timestamp DESC because the append-heavy workload (1.2M messages/second sustained, 5M peak) requires sequential commit log writes completing in <5ms versus PostgreSQL's B-tree index maintenance causing 500ms write latency spikes under load, and denormalizing messages into each participant's partition (storing same message twice) enables independent delivery status tracking where user_001 sees "delivered ✓✓" while user_002 sees "read ✓✓ blue" without cross-partition queries that would require scatter-gather across nodes — Redis stores ephemeral online status and WebSocket connection mappings because 33 million heartbeat updates per second (1B users / 30s) and 580K status queries per second require sub-millisecond in-memory operations with automatic TTL expiration (45 seconds) that marks users offline without background jobs scanning billions of rows like PostgreSQL's "UPDATE user_status SET offline WHERE last_heartbeat < NOW() - 45s" would attempt — PostgreSQL stores user profiles, contacts, groups, and privacy settings because relational queries like "can user_002 see user_001's online status based on privacy='contacts' AND contact relationship exists" require JOINs and complex conditional logic that key-value stores cannot express, while phone number uniqueness constraints and group membership queries depend on ACID transactions that NoSQL databases handle weakly — end-to-end encryption stores only opaque encrypted blobs in all databases since messages are encrypted on sender's device with recipient's public key before transmission and the server never possesses decryption keys, making database content unreadable even to WhatsApp employees or government agencies requiring legal access — group messages fan out writes (1 message to 100-member group = 100 Cassandra writes) which scales because average group size is 8 members not 1,024 so actual fan-out multiplier is 8x not 1024x giving 8M writes/second not 1B, while multimedia messages store only CDN URLs in Cassandra with actual encrypted media in S3 object storage at $0.023/GB versus $0.10/GB for database storage preventing bloat from 100MB videos — offline message delivery queues messages in Cassandra during recipient's offline period then syncs all pending messages when recipient reconnects by querying "WHERE user_id = X AND status = 'sent' AND timestamp > last_sync" returning hundreds of messages in <100ms through Cassandra's time-ordered clustering key versus PostgreSQL scanning billions of rows even with indexes.**