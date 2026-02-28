

# Why This Architecture for Zoom?

Let me build your intuition from the ground up — starting with what makes real-time video conferencing brutally hard at scale.

---

## The Core Problem Zoom Solves

On the surface video conferencing seems simple — stream video between participants. But consider the real constraints:

```
Zoom scale reality:
────────────────────────────────────────────────
Daily meeting participants: 300 million+
Concurrent meetings:        3 million+ at peak
Largest meetings:           1,000+ participants (webinars: 10,000+)
Video streams:              
  - 1 participant: 1 upload stream
  - 100 participants: 100 upload streams
  - Naive P2P: 100 × 99 = 9,900 connections per meeting!
Audio/video data rate:      
  - HD video: 1-3 Mbps per stream
  - Audio: 50-100 Kbps per stream

Requirements:
→ Sub-150ms latency (real-time feel)
→ Handle participant joins/leaves gracefully
→ Screen sharing (high resolution, low latency)
→ Recording (store and process afterward)
→ Mute/unmute instantly
→ Virtual backgrounds (CPU-intensive processing)
→ Network resilience (handle packet loss)
→ Scale to 1,000 participants without quality degradation
→ Waiting room (hold participants before admit)
→ Breakout rooms (split meeting into sub-meetings)
```

This combination of **real-time media streaming + massive concurrent connections + instant state changes + recording + scalability** is what forces this specific architecture.

---

## Why PostgreSQL for Meetings Metadata?

### What Meeting Data Looks Like

```
MEETING METADATA:
════════════════════════════════════════════════════════

{
  meeting_id: "mtg_abc123",
  host_user_id: "user_001",
  title: "Engineering Standup",
  scheduled_time: "2024-02-26T10:00:00Z",
  duration_minutes: 30,
  timezone: "America/Los_Angeles",
  meeting_type: "scheduled",  // scheduled, instant, recurring
  recurrence: {
    frequency: "daily",
    until: "2024-03-26",
    days: ["Mon", "Tue", "Wed", "Thu", "Fri"]
  },
  settings: {
    require_password: true,
    waiting_room_enabled: true,
    allow_recording: true,
    mute_on_entry: true,
    video_on_entry: false,
    allow_screen_share: "all"  // host_only, all
  },
  participants: [
    {user_id: "user_002", role: "panelist", email: "alice@..."},
    {user_id: "user_003", role: "attendee", email: "bob@..."}
  ],
  created_at: "2024-02-20T15:00:00Z",
  status: "scheduled"  // scheduled, active, ended, cancelled
}
```

This is structured, relational data with complex queries needed.

### PostgreSQL Schema

```
POSTGRESQL SCHEMA:
════════════════════════════════════════════════════════

Meetings table:
────────────────────────────────────────────────────────────────────────────────
meeting_id │ host_user_id │ title              │ scheduled_time      │ duration │ status
───────────────────────────────────────────────────────────────────────────────────────────
mtg_abc123 │ user_001     │ Engineering Standup│ 2024-02-26 10:00:00 │ 30       │ scheduled
mtg_def456 │ user_002     │ Client Presentation│ 2024-02-26 14:00:00 │ 60       │ scheduled

Additional columns:
  meeting_type: ENUM('scheduled', 'instant', 'recurring', 'webinar')
  timezone: VARCHAR
  password_hash: VARCHAR (if require_password)
  waiting_room_enabled: BOOLEAN
  recording_enabled: BOOLEAN
  created_at: TIMESTAMP
  updated_at: TIMESTAMP

Indexes:
  PRIMARY KEY (meeting_id)
  INDEX (host_user_id, scheduled_time DESC)
  INDEX (scheduled_time) WHERE status = 'scheduled'


Meeting_Participants table:
────────────────────────────────────────────────────────────────────────
meeting_id │ user_id  │ role      │ email           │ invited_at │ status
────────────────────────────────────────────────────────────────────────────────
mtg_abc123 │ user_002 │ panelist  │ alice@email.com │ ...        │ accepted
mtg_abc123 │ user_003 │ attendee  │ bob@email.com   │ ...        │ pending

Indexes:
  PRIMARY KEY (meeting_id, user_id)
  INDEX (user_id, scheduled_time)  ← "my upcoming meetings"


Recurring_Meetings table:
────────────────────────────────────────────────────────────────────────
series_id │ meeting_id │ occurrence_date     │ is_exception
────────────────────────────────────────────────────────────────────────────────
series_001│ mtg_abc123 │ 2024-02-26 10:00:00 │ false
series_001│ mtg_abc124 │ 2024-02-27 10:00:00 │ false
series_001│ mtg_abc125 │ 2024-02-28 10:00:00 │ true  ← cancelled this one


Meeting_Recordings table:
────────────────────────────────────────────────────────────────────────
recording_id │ meeting_id │ started_at          │ duration │ file_size │ storage_url
───────────────────────────────────────────────────────────────────────────────────────
rec_001      │ mtg_abc123 │ 2024-02-26 10:05:00 │ 28       │ 1.2GB     │ s3://...
rec_002      │ mtg_def456 │ 2024-02-26 14:00:00 │ 58       │ 2.5GB     │ s3://...

Additional columns:
  processing_status: ENUM('queued', 'processing', 'completed', 'failed')
  transcript_url: VARCHAR
  thumbnail_url: VARCHAR


Users table:
────────────────────────────────────────────────────────────────────────
user_id  │ email            │ display_name  │ plan      │ created_at
────────────────────────────────────────────────────────────────────────────────
user_001 │ john@company.com │ John Smith    │ business  │ 2023-01-15
user_002 │ alice@email.com  │ Alice Johnson │ pro       │ 2023-03-20
```

### Complex Queries PostgreSQL Handles

```
QUERY 1: Get user's upcoming meetings
════════════════════════════════════════════════════════

SELECT m.meeting_id, m.title, m.scheduled_time,
       u.display_name as host_name,
       mp.role
FROM meetings m
JOIN meeting_participants mp ON m.meeting_id = mp.meeting_id
JOIN users u ON m.host_user_id = u.user_id
WHERE mp.user_id = 'user_001'
AND m.scheduled_time > NOW()
AND m.status = 'scheduled'
ORDER BY m.scheduled_time ASC
LIMIT 10;

→ Multi-table JOIN
→ Date filtering
→ Natural in SQL
→ <10ms with indexes


QUERY 2: Check if user can join meeting
════════════════════════════════════════════════════════

WITH meeting_info AS (
  SELECT host_user_id, password_hash, waiting_room_enabled, status
  FROM meetings
  WHERE meeting_id = 'mtg_abc123'
)
SELECT 
  CASE
    WHEN status != 'scheduled' THEN 'meeting_not_available'
    WHEN password_hash IS NOT NULL AND :provided_password != password_hash THEN 'wrong_password'
    WHEN waiting_room_enabled AND host_user_id != :user_id THEN 'waiting_room'
    ELSE 'allowed'
  END as join_status
FROM meeting_info;

→ Complex conditional logic
→ Security checks
→ Impossible in key-value stores


QUERY 3: Generate meeting analytics
════════════════════════════════════════════════════════

SELECT 
  DATE(scheduled_time) as date,
  COUNT(DISTINCT meeting_id) as total_meetings,
  AVG(duration_minutes) as avg_duration,
  COUNT(DISTINCT CASE WHEN status = 'ended' THEN meeting_id END) as completed_meetings
FROM meetings
WHERE host_user_id = 'user_001'
AND scheduled_time >= NOW() - INTERVAL '30 days'
GROUP BY DATE(scheduled_time)
ORDER BY date DESC;

→ Aggregations
→ Conditional counting
→ Business intelligence
```

### Why Not NoSQL for Meeting Metadata?

```
MONGODB ATTEMPT:
════════════════════════════════════════════════════════

{
  _id: "mtg_abc123",
  host_user_id: "user_001",
  title: "Engineering Standup",
  participants: [
    {user_id: "user_002", email: "alice@...", role: "panelist"},
    {user_id: "user_003", email: "bob@...", role: "attendee"}
  ],
  scheduled_time: ISODate("2024-02-26T10:00:00Z")
}

Problems:
────────────────────────────────────────────────
✗ Cannot efficiently query: "show me all meetings user_002 is invited to"
  → Must scan all meetings, check participants array
  → No index on array elements
  
✗ Updating participant status requires:
  → Find meeting by meeting_id
  → Update nested array element
  → Complex array manipulation
  
✗ Join queries impossible:
  → "Show meeting title + host name + my role"
  → Requires application-level joins
  
✗ Recurring meetings complex:
  → Must duplicate data or complex references
  
✗ Analytics difficult:
  → Aggregation pipeline verbose
  → No COUNT DISTINCT across nested arrays


PostgreSQL excels at:
────────────────────────────────────────────────
✓ Normalized schema (participants separate table)
✓ Efficient JOINs
✓ Complex WHERE clauses
✓ Foreign keys (data integrity)
✓ ACID transactions (critical for meeting creation)
```

---

## Why Redis for Active Sessions?

### The Real-Time Presence Problem

```
ACTIVE SESSION REQUIREMENTS:
════════════════════════════════════════════════════════

During active meeting with 100 participants:
- Each participant sends/receives video/audio streams
- Real-time state changes (mute, video on/off, screen share)
- Participant joins/leaves (notify all others)
- Hand raised (notify host)
- Chat messages (broadcast to all)

State changes per second:
- 100 participants × 1 state change per 10 seconds = 10 updates/second per meeting
- 3 million concurrent meetings × 10 = 30 million updates/second globally

Queries needed:
- "Who is currently in meeting X?" (every second for UI updates)
- "Is user Y still connected?" (heartbeat check)
- "Which media server is handling this meeting?"
- "How many participants in meeting X?" (capacity check)

This is EPHEMERAL, HIGH-VELOCITY state
Perfect for Redis
```

### Redis Schema for Active Sessions

```
REDIS SCHEMA:
════════════════════════════════════════════════════════

Active meeting participants (Set):
────────────────────────────────────────────────
Key:   meeting:mtg_abc123:participants
Type:  Set
Value: {user_001, user_002, user_003, ...}
TTL:   None (managed by application)

SADD meeting:mtg_abc123:participants user_001
SADD meeting:mtg_abc123:participants user_002

SMEMBERS meeting:mtg_abc123:participants
→ Returns all participants instantly

SREM meeting:mtg_abc123:participants user_002  (user left)

Query time: <1ms


Participant connection state (Hash):
────────────────────────────────────────────────
Key:   meeting:mtg_abc123:user:user_001
Type:  Hash
TTL:   60 seconds (expires if no heartbeat)

HSET meeting:mtg_abc123:user:user_001
  connection_id "conn_xyz"
  audio_enabled "true"
  video_enabled "false"
  screen_share "false"
  joined_at "1708945200"
  last_heartbeat "1708945260"

Every 10 seconds, client sends heartbeat:
EXPIRE meeting:mtg_abc123:user:user_001 60

If no heartbeat: TTL expires → user disconnected


Media server assignment:
────────────────────────────────────────────────
Key:   meeting:mtg_abc123:media_server
Value: "media_server_5"
TTL:   Duration of meeting

SET meeting:mtg_abc123:media_server "media_server_5"

When participant joins:
GET meeting:mtg_abc123:media_server
→ Route to media_server_5


Waiting room queue:
────────────────────────────────────────────────
Key:   meeting:mtg_abc123:waiting_room
Type:  List (FIFO queue)

LPUSH meeting:mtg_abc123:waiting_room '{
  "user_id": "user_004",
  "display_name": "Charlie",
  "joined_at": 1708945300
}'

Host admits:
RPOP meeting:mtg_abc123:waiting_room
→ Returns next user to admit


Real-time events (Pub/Sub):
────────────────────────────────────────────────
Channel: meeting:mtg_abc123:events

Publish event:
PUBLISH meeting:mtg_abc123:events '{
  "type": "user_joined",
  "user_id": "user_002",
  "display_name": "Alice",
  "timestamp": 1708945200
}'

All clients subscribed to channel receive immediately
Use for:
- User join/leave notifications
- Chat messages
- Hand raised
- Mute/unmute broadcasts
- Screen share start/stop
```

### Why PostgreSQL Cannot Handle Active Sessions

```
POSTGRESQL ATTEMPT:
════════════════════════════════════════════════════════

Active_Sessions table:
────────────────────────────────────────────────
meeting_id │ user_id  │ connection_id │ audio │ video │ last_heartbeat
───────────────────────────────────────────────────────────────────────────────
mtg_abc123 │ user_001 │ conn_xyz      │ true  │ false │ 2024-02-26 10:05:00
mtg_abc123 │ user_002 │ conn_abc      │ true  │ true  │ 2024-02-26 10:05:02

Heartbeat updates:
UPDATE active_sessions
SET last_heartbeat = NOW()
WHERE meeting_id = 'mtg_abc123'
AND user_id = 'user_001';

Problems at scale:
────────────────────────────────────────────────
→ 30 million updates per second
→ PostgreSQL max: ~10K writes/second
→ Need 3,000 PostgreSQL servers (impossible)
→ B-tree index updated on every write
→ Row-level locking contention
→ VACUUM cannot keep up

Query all participants:
SELECT user_id FROM active_sessions
WHERE meeting_id = 'mtg_abc123'
AND last_heartbeat > NOW() - INTERVAL '30 seconds';

→ Table scan or index scan
→ Slow for real-time UI updates
→ Stale data (writes slower than queries)


REDIS SOLUTION:
────────────────────────────────────────────────
✓ In-memory: <1ms per operation
✓ 100K+ operations/second per instance
✓ TTL auto-expires stale connections
✓ Set operations: SMEMBERS instant
✓ Pub/Sub: real-time event broadcasting
✓ Horizontal scaling: cluster mode
```

---

## WebRTC and SFU Architecture

### The P2P Problem

```
PEER-TO-PEER (P2P) APPROACH (DOESN'T SCALE):
════════════════════════════════════════════════════════

Each participant connects directly to every other participant

Meeting with 10 participants:
────────────────────────────────────────────────
User A must:
- Upload video stream to 9 others
- Download video streams from 9 others
- Total: 9 uploads + 9 downloads = 18 connections

Network bandwidth:
- Upload: 9 × 1 Mbps = 9 Mbps
- Download: 9 × 1 Mbps = 9 Mbps
- Total: 18 Mbps per participant


Meeting with 100 participants:
────────────────────────────────────────────────
User A must:
- Upload to 99 others
- Download from 99 others
- Total: 99 uploads + 99 downloads = 198 connections

Network bandwidth:
- Upload: 99 × 1 Mbps = 99 Mbps
- Download: 99 × 1 Mbps = 99 Mbps
- Total: 198 Mbps per participant

Most home networks: 10-50 Mbps upload
IMPOSSIBLE to support 100 participants!


Total connections in meeting:
────────────────────────────────────────────────
N participants: N × (N - 1) / 2 connections
100 participants: 100 × 99 / 2 = 4,950 connections
1,000 participants: 1,000 × 999 / 2 = 499,500 connections

Each connection:
- CPU overhead (encoding/decoding)
- Memory overhead (buffers)
- Network overhead (NAT traversal, ICE candidates)

P2P is ONLY viable for 2-4 participants
```

### SFU Architecture (Selective Forwarding Unit)

```
SFU ARCHITECTURE:
════════════════════════════════════════════════════════

All participants connect to central media server (SFU)
SFU forwards streams selectively

Meeting with 100 participants:
────────────────────────────────────────────────

Each participant:
- Uploads 1 stream to SFU (1 Mbps upload)
- Downloads streams from SFU (varies by layout)

Gallery view (showing 25 participants):
- Download: 25 × 1 Mbps = 25 Mbps

Speaker view (showing 1 active speaker):
- Download: 1 × 1 Mbps = 1 Mbps

Bandwidth per participant:
- Upload: 1 Mbps (constant)
- Download: 1-25 Mbps (depends on layout)
- Total: 2-26 Mbps (vs 198 Mbps in P2P!)


SFU server:
────────────────────────────────────────────────
Receives: 100 upload streams (100 Mbps inbound)
Forwards: Selectively to each participant

User A (gallery view, sees 25):
  → SFU sends 25 streams to User A

User B (speaker view, sees 1):
  → SFU sends 1 stream to User B

SFU doesn't decode/re-encode (low CPU)
SFU just forwards packets (high network throughput)


Total connections in meeting:
────────────────────────────────────────────────
N participants: N connections (not N²)
100 participants: 100 connections to SFU
1,000 participants: 1,000 connections to SFU

Linear scaling vs quadratic!
```

### WebRTC Connection Establishment

```
WEBRTC SIGNALING FLOW:
════════════════════════════════════════════════════════

User A wants to join meeting with User B (already connected)

STEP 1: User A requests to join
────────────────────────────────────────────────
POST /api/meetings/mtg_abc123/join
{
  user_id: "user_001",
  device_info: {video: true, audio: true}
}

Backend:
1. Validate: Check PostgreSQL (user invited? password correct?)
2. Check Redis: Is meeting active? Which media server?
3. Assign media server: media_server_5
4. Store in Redis:
   SADD meeting:mtg_abc123:participants user_001
   HSET meeting:mtg_abc123:user:user_001 media_server "media_server_5"


STEP 2: Exchange ICE candidates (NAT traversal)
────────────────────────────────────────────────
User A generates ICE candidates (potential connection paths):
- Local IP: 192.168.1.100:54321
- Public IP (from STUN): 203.0.113.45:54321
- TURN relay: turn.zoom.us:3478

User A → WebSocket → Backend:
{
  type: "ice_candidate",
  candidate: "candidate:1 udp 2130706431 192.168.1.100 54321 typ host"
}

Backend → Redis Pub/Sub:
PUBLISH meeting:mtg_abc123:signaling '{...}'

Media Server 5 receives via subscription


STEP 3: SDP offer/answer exchange
────────────────────────────────────────────────
User A creates SDP offer (describes media capabilities):
{
  type: "offer",
  sdp: "v=0
        o=- 12345 2 IN IP4 192.168.1.100
        s=-
        m=video 9 UDP/TLS/RTP/SAVPF 96
        a=rtpmap:96 VP8/90000
        m=audio 9 UDP/TLS/RTP/SAVPF 111
        a=rtpmap:111 opus/48000/2
        ..."
}

Describes:
- Video codec: VP8
- Audio codec: Opus
- Resolution: 1280x720
- Framerate: 30fps
- Bitrate: 1 Mbps

Media Server responds with SDP answer (agreed parameters)


STEP 4: Establish DTLS-SRTP encrypted connection
────────────────────────────────────────────────
WebRTC uses:
- DTLS: Encryption handshake
- SRTP: Encrypted media transport

User A ←─────DTLS handshake─────→ Media Server
        ←──────SRTP media──────→

Video/audio packets now flow encrypted
End-to-end encryption (zero-trust)


STEP 5: Media streaming begins
────────────────────────────────────────────────
User A:
- Captures video from webcam (30 fps)
- Encodes with VP8 codec
- Packetizes into RTP packets
- Encrypts with SRTP
- Sends to Media Server

Media Server:
- Receives from User A
- Forwards to all other participants (User B, C, ...)
- No decoding/re-encoding (low CPU)

User B:
- Receives encrypted packets from Media Server
- Decrypts SRTP
- Decodes VP8
- Renders to screen

Total latency: 50-150ms (real-time!)
```

---

## Complete Architecture Flow

```
FLOW 1: Schedule Meeting
════════════════════════════════════════════════════════

User schedules meeting for tomorrow 10 AM
        │
        ▼
POST /api/meetings
{
  title: "Team Sync",
  scheduled_time: "2024-02-27T10:00:00Z",
  duration_minutes: 30,
  participants: ["alice@co.com", "bob@co.com"]
}
        │
        ▼
STEP 1: Write to PostgreSQL
────────────────────────────────────────────────
BEGIN TRANSACTION;

-- Create meeting
INSERT INTO meetings (meeting_id, host_user_id, title, scheduled_time, ...)
VALUES ('mtg_new', 'user_001', 'Team Sync', '2024-02-27 10:00:00', ...);

-- Add participants
INSERT INTO meeting_participants (meeting_id, user_id, email, role)
VALUES 
  ('mtg_new', 'user_002', 'alice@co.com', 'panelist'),
  ('mtg_new', 'user_003', 'bob@co.com', 'attendee');

COMMIT;

Time: <20ms


STEP 2: Send invitations (async)
────────────────────────────────────────────────
Background job:
- Sends email invitations
- Adds to calendar (iCal attachment)
- Creates reminder notifications


STEP 3: Return to user
────────────────────────────────────────────────
Response:
{
  meeting_id: "mtg_new",
  join_url: "https://zoom.us/j/mtg_new",
  meeting_password: "abc123"
}
```

```
FLOW 2: Start Meeting
════════════════════════════════════════════════════════

10:00 AM - Host clicks "Start Meeting"
        │
        ▼
POST /api/meetings/mtg_new/start
        │
        ▼
STEP 1: Assign media server
────────────────────────────────────────────────
Find available media server with capacity:
SELECT server_id, current_meetings, max_capacity
FROM media_servers
WHERE current_meetings < max_capacity
ORDER BY current_load ASC
LIMIT 1;

Returns: media_server_5


STEP 2: Initialize meeting in Redis
────────────────────────────────────────────────
SET meeting:mtg_new:media_server "media_server_5"
SET meeting:mtg_new:status "active"
SET meeting:mtg_new:host "user_001"
SADD meeting:mtg_new:participants user_001

EXPIRE meeting:mtg_new:* 14400  (4 hours max)


STEP 3: Notify media server
────────────────────────────────────────────────
POST http://media_server_5/meetings/mtg_new/initialize
{
  meeting_id: "mtg_new",
  host_user_id: "user_001",
  max_participants: 100
}

Media server allocates resources:
- Bandwidth: 100 Mbps
- CPU: 2 cores
- Memory: 4GB


STEP 4: Update PostgreSQL
────────────────────────────────────────────────
UPDATE meetings
SET status = 'active',
    started_at = NOW(),
    media_server_id = 'media_server_5'
WHERE meeting_id = 'mtg_new';


STEP 5: Host joins WebRTC connection
────────────────────────────────────────────────
(See WebRTC flow above)

Host's browser establishes encrypted media stream to media_server_5
```

```
FLOW 3: Participant Joins
════════════════════════════════════════════════════════

Participant clicks join link
        │
        ▼
GET /api/meetings/mtg_new/join?user_id=user_002
        │
        ▼
STEP 1: Validate access (PostgreSQL)
────────────────────────────────────────────────
SELECT m.status, m.waiting_room_enabled, m.password_hash,
       mp.user_id
FROM meetings m
LEFT JOIN meeting_participants mp 
  ON m.meeting_id = mp.meeting_id 
  AND mp.user_id = 'user_002'
WHERE m.meeting_id = 'mtg_new';

Checks:
- Meeting exists ✓
- Meeting is active ✓
- User is invited OR meeting is public ✓
- Password matches (if required) ✓


STEP 2: Check waiting room (Redis)
────────────────────────────────────────────────
HGET meeting:mtg_new:settings waiting_room_enabled
→ Returns: "true"

LPUSH meeting:mtg_new:waiting_room '{
  "user_id": "user_002",
  "display_name": "Alice",
  "joined_at": 1708945300
}'

Publish event:
PUBLISH meeting:mtg_new:events '{
  "type": "waiting_room_user",
  "user_id": "user_002"
}'

Host sees notification: "Alice is waiting"


STEP 3: Host admits participant
────────────────────────────────────────────────
Host clicks "Admit"

RPOP meeting:mtg_new:waiting_room
→ Returns user_002 data

SADD meeting:mtg_new:participants user_002

PUBLISH meeting:mtg_new:events '{
  "type": "user_admitted",
  "user_id": "user_002"
}'


STEP 4: Participant establishes WebRTC
────────────────────────────────────────────────
GET meeting:mtg_new:media_server
→ Returns: "media_server_5"

Participant's browser:
- Connects to media_server_5
- Exchanges ICE candidates
- SDP offer/answer
- DTLS handshake
- Begins streaming video/audio


STEP 5: Notify all participants
────────────────────────────────────────────────
PUBLISH meeting:mtg_new:events '{
  "type": "user_joined",
  "user_id": "user_002",
  "display_name": "Alice",
  "video": true,
  "audio": true
}'

All participants see: "Alice joined the meeting"
UI updates: Gallery view adds Alice's video tile
```

```
FLOW 4: Screen Sharing
════════════════════════════════════════════════════════

Participant clicks "Share Screen"
        │
        ▼
STEP 1: Request permission
────────────────────────────────────────────────
Browser prompts: "Zoom wants to share your screen"
User selects: "Entire Screen" or specific window
Grants permission


STEP 2: Create screen share track
────────────────────────────────────────────────
navigator.mediaDevices.getDisplayMedia({
  video: {
    width: {ideal: 1920},
    height: {ideal: 1080},
    frameRate: {ideal: 15}  ← lower FPS for screen (not 30)
  }
})

Screen capture:
- Resolution: 1920x1080
- Framerate: 15 fps
- Bitrate: 2-3 Mbps (higher than webcam video)


STEP 3: Add screen track to WebRTC
────────────────────────────────────────────────
peerConnection.addTrack(screenTrack)

SDP renegotiation:
- Sends updated SDP offer to media server
- Media server accepts (SDP answer)


STEP 4: Update meeting state
────────────────────────────────────────────────
HSET meeting:mtg_new:user:user_002 screen_share "true"

PUBLISH meeting:mtg_new:events '{
  "type": "screen_share_started",
  "user_id": "user_002"
}'


STEP 5: All participants receive screen stream
────────────────────────────────────────────────
Media server forwards screen stream to all
Participants' UI switches to screen share layout:
- Screen in center (large)
- Video thumbnails on side (small)


STEP 6: Screen share stops
────────────────────────────────────────────────
User clicks "Stop Sharing"

peerConnection.removeTrack(screenTrack)

HSET meeting:mtg_new:user:user_002 screen_share "false"

PUBLISH meeting:mtg_new:events '{
  "type": "screen_share_stopped",
  "user_id": "user_002"
}'

All participants return to gallery view
```

```
FLOW 5: Recording
════════════════════════════════════════════════════════

Host clicks "Record"
        │
        ▼
STEP 1: Start recording on media server
────────────────────────────────────────────────
POST http://media_server_5/meetings/mtg_new/record/start

Media server:
1. Allocates recording buffer
2. Begins capturing mixed audio/video streams
3. Writes to temporary file: /tmp/rec_mtg_new.webm


STEP 2: Update meeting state
────────────────────────────────────────────────
HSET meeting:mtg_new:recording recording "true"
HSET meeting:mtg_new:recording started_at "1708945400"

PUBLISH meeting:mtg_new:events '{
  "type": "recording_started"
}'

All participants see: 🔴 "This meeting is being recorded"


STEP 3: Meeting ends, stop recording
────────────────────────────────────────────────
Host clicks "End Meeting"

POST http://media_server_5/meetings/mtg_new/record/stop

Media server:
- Finalizes recording file
- File size: 1.2GB (30 minutes × 2.5 Mbps avg)


STEP 4: Upload to object storage (S3)
────────────────────────────────────────────────
Background job on media server:

PUT s3://zoom-recordings/user_001/mtg_new/recording.webm
[1.2GB file upload]

Time: ~2 minutes


STEP 5: Create database record
────────────────────────────────────────────────
INSERT INTO meeting_recordings
(recording_id, meeting_id, started_at, duration_seconds, file_size, storage_url, processing_status)
VALUES
('rec_001', 'mtg_new', '2024-02-26 10:05:00', 1800, 1258291200, 's3://...', 'queued');


STEP 6: Process recording (async)
────────────────────────────────────────────────
Background processing pipeline:

Job 1: Generate thumbnail (5 min)
- Extract frame at 10% mark
- Resize to 320x180
- Upload: s3://.../thumbnail.jpg

Job 2: Transcode to MP4 (10 min)
- Convert WebM → MP4 (better compatibility)
- Optimize for streaming
- Upload: s3://.../recording.mp4

Job 3: Generate transcript (15 min)
- Speech-to-text (Whisper model)
- Creates: transcript.vtt
- Upload: s3://.../transcript.vtt

UPDATE meeting_recordings
SET processing_status = 'completed',
    thumbnail_url = 's3://.../thumbnail.jpg',
    transcript_url = 's3://.../transcript.vtt'
WHERE recording_id = 'rec_001';


STEP 7: Notify user
────────────────────────────────────────────────
Email: "Your recording is ready!"
Link: https://zoom.us/recordings/rec_001
```

```
FLOW 6: Meeting Ends
════════════════════════════════════════════════════════

Host clicks "End Meeting for All"
        │
        ▼
STEP 1: Broadcast end event
────────────────────────────────────────────────
PUBLISH meeting:mtg_new:events '{
  "type": "meeting_ended",
  "ended_by": "user_001"
}'

All participants receive event
Clients disconnect WebRTC
Browsers show: "Meeting has ended"


STEP 2: Clean up Redis
────────────────────────────────────────────────
DEL meeting:mtg_new:participants
DEL meeting:mtg_new:media_server
DEL meeting:mtg_new:user:*
DEL meeting:mtg_new:waiting_room

(Or let TTL expire naturally)


STEP 3: Update PostgreSQL
────────────────────────────────────────────────
UPDATE meetings
SET status = 'ended',
    ended_at = NOW(),
    actual_duration = EXTRACT(EPOCH FROM (NOW() - started_at))
WHERE meeting_id = 'mtg_new';


STEP 4: Generate analytics
────────────────────────────────────────────────
INSERT INTO meeting_analytics
(meeting_id, total_participants, avg_participants, peak_participants, total_duration)
VALUES ('mtg_new', 15, 12.5, 15, 1800);


STEP 5: Media server cleanup
────────────────────────────────────────────────
POST http://media_server_5/meetings/mtg_new/cleanup

Media server:
- Closes all WebRTC connections
- Releases bandwidth allocation
- Frees memory buffers
- Updates capacity: current_meetings -= 1
```

---

## System Architecture Diagram

```
                        ┌──────────────────┐
                        │  User Browsers   │
                        │  (WebRTC client) │
                        └────────┬─────────┘
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
              WebSocket       HTTPS        WebRTC
                    │            │         (media)
                    ▼            ▼            │
         ┌──────────────┐ ┌──────────────┐  │
         │  API Gateway │ │  Signaling   │  │
         │              │ │   Server     │  │
         └──────┬───────┘ └──────┬───────┘  │
                │                 │          │
      ┌─────────┼─────────────────┼──────────┘
      │         │                 │
      │         ▼                 ▼
      │  ┌────────────┐   ┌─────────────┐
      │  │PostgreSQL  │   │    Redis    │
      │  │            │   │             │
      │  │ -Meetings  │   │ -Active     │
      │  │ -Users     │   │  sessions   │
      │  │ -Recordings│   │ -Pub/Sub    │
      │  └────────────┘   │ -Waiting    │
      │                   │  room       │
      │                   └─────────────┘
      │
      └──────────────────┐
                         │
                    ┌────▼────────────────┐
                    │  Media Servers      │
                    │  (SFU architecture) │
                    │                     │
                    │  ┌──────────────┐   │
                    │  │ Server 1     │   │
                    │  │ Server 2     │   │
                    │  │ Server 3     │   │
                    │  │ ...          │   │
                    │  └──────────────┘   │
                    └──────┬──────────────┘
                           │
                    ┌──────▼──────┐
                    │     S3      │
                    │ (recordings)│
                    └─────────────┘
```

---

## Tradeoffs vs Other Databases

```
┌────────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                            │ THIS ARCH    │ POSTGRES ALL │ MONGO ALL    │ CASSANDRA ALL│
├────────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Meeting metadata queries   │ PostgreSQL✓  │ PostgreSQL✓  │ MongoDB ✓    │ Limited ✗    │
│ Active session updates     │ Redis ✓      │ 10K/sec ✗    │ 50K/sec      │ 100K/sec ✓   │
│ Real-time Pub/Sub          │ Redis ✓      │ LISTEN ✗     │ Change Str✓  │ NO ✗         │
│ TTL auto-expiration        │ Redis ✓      │ Manual ✗     │ Native ✓     │ Native ✓     │
│ Complex JOINs              │ PostgreSQL✓  │ PostgreSQL✓  │ NO ✗         │ NO ✗         │
│ ACID transactions          │ PostgreSQL✓  │ PostgreSQL✓  │ Limited      │ NO ✗         │
│ Recurring meeting logic    │ PostgreSQL✓  │ PostgreSQL✓  │ Manual       │ Manual       │
│ Heartbeat (<1ms)           │ Redis ✓      │ 5-10ms       │ 5ms          │ 5ms          │
│ Horizontal scaling (media) │ SFU sharding │ N/A          │ N/A          │ N/A          │
│ Operational complexity     │ HIGH         │ MEDIUM       │ MEDIUM       │ HIGH         │
└────────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **PostgreSQL stores meeting metadata and participants because scheduling queries like "show my upcoming meetings WHERE scheduled_time > NOW() JOIN users ON host_id to display host name" require multi-table JOINs and date filtering that NoSQL databases cannot express efficiently, while ACID transactions ensure atomic meeting creation where INSERT meeting + INSERT participants + SEND invitations either all succeed or all rollback preventing orphaned records, and recurring meeting logic with exception handling (daily standup EXCEPT skip Feb 28) relies on relational queries checking occurrence_date and is_exception flags — Redis stores active session state (meeting:mtg_X:participants Set, connection heartbeats with 60-second TTL, waiting room FIFO queue) because 30 million heartbeat updates per second (3M meetings × 10 updates/sec) and <1ms participant list queries (SMEMBERS) require in-memory operations that PostgreSQL's disk-based B-tree indexes taking 5-10ms per write cannot sustain, while Pub/Sub channels (meeting:mtg_X:events) broadcast join/leave/mute events to all subscribers instantly without polling enabling real-time UI updates showing "Alice joined" notification within 50ms — SFU (Selective Forwarding Unit) architecture replaces peer-to-peer connections where 100 participants require 4,950 P2P connections consuming 99 Mbps upload per user (impossible on home networks) with centralized media servers where each participant uploads 1 stream to SFU and downloads 1-25 streams (speaker view vs gallery view) totaling 2-26 Mbps making 1,000-participant meetings feasible through linear connection scaling (N connections not N²) — WebRTC establishes encrypted DTLS-SRTP connections after exchanging ICE candidates for NAT traversal and SDP offers describing codec capabilities (VP8 video, Opus audio) directly between browsers and media servers with 50-150ms glass-to-glass latency, while signaling messages (ICE candidates, SDP offers) route through backend API and Redis Pub/Sub coordinating the connection establishment without touching actual media streams — media server sharding assigns meetings to specific servers (media_server_5) stored in Redis (GET meeting:mtg_X:media_server) routing all participants to the same server for that meeting, with server selection based on current load balancing across cluster, and recording happens on the assigned media server capturing mixed audio/video streams to temporary WebM file then asynchronously uploading to S3 and processing (MP4 transcode, thumbnail generation, speech-to-text transcription) in background pipeline without blocking meeting or requiring database writes during active call.**
