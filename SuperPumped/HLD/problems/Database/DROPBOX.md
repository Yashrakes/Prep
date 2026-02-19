## The Core Problem Dropbox Solves

On the surface file storage seems simple — save files, sync them. But consider the real constraints:

```
Dropbox scale reality:
────────────────────────────────────────────────
Users:                       700+ million
Files stored:                Exabytes (10^18 bytes)
Average file operations:     100,000+ per second
Files per user:              10,000+ files average
Largest files:               Up to 50GB per file
Requirements:
→ Upload 50GB file without crashing/timing out
→ Resume upload from exactly where it failed
→ Deduplicate: if 1M users upload same file, store once
→ Sync changes instantly across all user's devices
→ Handle conflicts: user edits same file on 2 devices offline
→ Keep version history: restore any file to any point
→ Never lose data even if servers crash mid-upload
→ Show sync status in real-time (that spinning icon)
```

This combination of **massive files + deduplication + instant sync + conflict resolution + version history + reliability** is what forces this three-component architecture.

---

## Why PostgreSQL for Metadata?

### What is "Metadata" vs "File Content"?

```
WRONG MENTAL MODEL (what beginners think):
════════════════════════════════════════════════════════

Store entire file in database:

Files table:
file_id  │ filename    │ content (BLOB)
─────────────────────────────────────────────────────
1        │ photo.jpg   │ [10 MB binary data]
2        │ video.mp4   │ [500 MB binary data]
3        │ movie.mkv   │ [5 GB binary data]

Problems:
→ Database size explodes (exabytes in PostgreSQL??)
→ One query returns 5GB → crashes client
→ Backup takes months
→ Queries slow to a crawl
→ Cannot scale horizontally
→ Completely unworkable


CORRECT MODEL (what Dropbox does):
════════════════════════════════════════════════════════

Files table (ONLY metadata):
file_id  │ user_id │ path              │ filename   │ size_bytes │ version │ checksum     │ chunk_ids
────────────────────────────────────────────────────────────────────────────────────────────────────────────────
1        │ user_001│ /Photos/         │ photo.jpg  │ 10,485,760 │ 3       │ abc123...    │ [chunk_001, chunk_002]
2        │ user_001│ /Videos/         │ video.mp4  │ 524,288,000│ 1       │ def456...    │ [chunk_003, chunk_004, ...]
3        │ user_002│ /Movies/         │ movie.mkv  │ 5,368,709,120│ 1     │ ghi789...    │ [chunk_100, chunk_101, ...]

Size: Each row ~500 bytes
700M users × 10K files = 7 trillion rows × 500 bytes = 3.5TB
Totally manageable in PostgreSQL

Actual file content: Stored in S3
One S3 object per chunk
```

### Why PostgreSQL for This Metadata?

```
METADATA REQUIREMENTS:
════════════════════════════════════════════════════════

1. Complex relational queries:
────────────────────────────────────────────────

"Show me all .pdf files in /Work folder
 modified in last 7 days
 by user_001
 that are larger than 10MB
 sorted by modified date"

PostgreSQL:
SELECT f.file_id, f.filename, f.path, f.size_bytes, f.modified_at
FROM files f
WHERE f.user_id = 'user_001'
AND f.path LIKE '/Work/%'
AND f.filename LIKE '%.pdf'
AND f.modified_at > NOW() - INTERVAL '7 days'
AND f.size_bytes > 10485760
ORDER BY f.modified_at DESC;

→ Runs in <50ms with proper indexes
→ Natural SQL query
→ Complex filtering works

DynamoDB:
→ Cannot do this query efficiently
→ Would need 5 different GSIs (expensive)
→ Still can't filter on multiple conditions efficiently
→ Must scan and filter in application (slow, expensive)


2. ACID transactions for version atomicity:
────────────────────────────────────────────────

User uploads new version of file.jpg:
Must atomically:
- Insert new file version record
- Update previous version to mark as "not current"
- Insert chunk references
- Update user's quota usage

BEGIN TRANSACTION;
  -- Create new version
  INSERT INTO file_versions
  (file_id, version, checksum, chunk_ids, created_at)
  VALUES ('file_001', 4, 'new_hash', [...], NOW());
  
  -- Mark previous version as old
  UPDATE file_versions
  SET is_current = false
  WHERE file_id = 'file_001' AND version = 3;
  
  -- Update file metadata
  UPDATE files
  SET current_version = 4, modified_at = NOW()
  WHERE file_id = 'file_001';
  
  -- Update user quota
  UPDATE users
  SET storage_used = storage_used + new_size - old_size
  WHERE user_id = 'user_001';
COMMIT;

If commit fails: ALL rolled back, no corruption

DynamoDB:
→ Transactions limited to 100 items
→ If any step fails, manual cleanup needed
→ No native "mark old version" logic
→ Quota calculation becomes eventually consistent


3. Sharding by user_id for horizontal scale:
────────────────────────────────────────────────

PostgreSQL with Citus or manual sharding:

Shard 1 (users 0-249M):    user_id hash % 4 = 0
Shard 2 (users 250M-499M): user_id hash % 4 = 1
Shard 3 (users 500M-749M): user_id hash % 4 = 2
Shard 4 (users 750M-999M): user_id hash % 4 = 3

All queries include user_id → go to correct shard
No cross-shard joins needed
Linear scalability

Query: "Show user_001's files"
→ hash(user_001) % 4 = Shard 2
→ Only queries Shard 2
→ Fast, distributed


4. Rich indexes for file search:
────────────────────────────────────────────────

CREATE INDEX idx_user_path ON files(user_id, path);
CREATE INDEX idx_user_modified ON files(user_id, modified_at DESC);
CREATE INDEX idx_user_filename ON files(user_id, filename);
CREATE INDEX idx_checksum ON files(checksum);  ← for dedup

Search "all files in /Photos":
Uses idx_user_path → instant

Search "files modified today":
Uses idx_user_modified → instant

DynamoDB:
Each index (GSI) costs money
20 GSI max limit
Must predict all query patterns at design time
```

---

## Why Object Storage (S3) for File Chunks?

### The Chunking Strategy

```
Why chunk large files?
════════════════════════════════════════════════════════

WITHOUT CHUNKING:
────────────────────────────────────────────────
User uploads 5GB video:
Upload starts... 4.9GB transferred... network fails
Must restart from beginning
Upload entire 5GB again
Fails at 4.8GB this time
Repeat forever, never completes


WITH CHUNKING:
────────────────────────────────────────────────
5GB file split into 4MB chunks = 1,280 chunks

Upload starts:
Chunk 1: SUCCESS ✓
Chunk 2: SUCCESS ✓
...
Chunk 1200: SUCCESS ✓
Chunk 1201: FAIL ✗
Network error

Resume upload:
Chunks 1-1200 already on server (verified by hash)
Only upload chunks 1201-1280
80 chunks × 4MB = 320MB remaining
Completes successfully

Benefit: Resilient uploads, always resumable
```

### Why S3 Specifically?

```
S3 ADVANTAGES FOR FILE STORAGE:
════════════════════════════════════════════════════════

1. Infinite scalability:
────────────────────────────────────────────────
Store exabytes without thinking
No capacity planning
No disk management
No RAID arrays
Amazon handles all hardware


2. 99.999999999% durability (11 nines):
────────────────────────────────────────────────
S3 automatically replicates across:
- 3+ availability zones
- Multiple physical data centers
- Spread across geographic region

Probability of losing data: 0.00000001% per year
Dropbox would need massive infrastructure team
to match this with self-hosted storage


3. Built-in versioning:
────────────────────────────────────────────────
S3 can keep all versions of an object:

PUT s3://bucket/chunk_001  (version 1)
PUT s3://bucket/chunk_001  (version 2)
PUT s3://bucket/chunk_001  (version 3)

GET s3://bucket/chunk_001?versionId=v1

Dropbox file history maps to S3 versioning


4. Lifecycle policies (automatic cost optimization):
────────────────────────────────────────────────
Rules like:
"Move chunks older than 90 days to Glacier (cheaper)"
"Delete versions older than 1 year"

Automatic cost reduction:
S3 Standard:      $0.023 per GB/month
S3 Glacier:       $0.004 per GB/month
S3 Deep Archive:  $0.00099 per GB/month

Old file versions → Deep Archive
Active files → S3 Standard
Automatic, no code needed


5. Content Delivery Network (CloudFront):
────────────────────────────────────────────────
User in Tokyo downloads file
S3 in us-east-1 is slow (200ms latency)

With CloudFront CDN:
First user: Fetches from S3 (slow)
CloudFront caches in Tokyo edge location
Next users: Served from Tokyo (5ms latency)

Automatic global distribution


6. Multipart upload API:
────────────────────────────────────────────────
S3 has native chunked upload:

// Initiate
uploadId = s3.createMultipartUpload("bucket", "chunk_001")

// Upload parts (parallel!)
s3.uploadPart(uploadId, partNumber: 1, data: chunk1)
s3.uploadPart(uploadId, partNumber: 2, data: chunk2)
s3.uploadPart(uploadId, partNumber: 3, data: chunk3)

// Complete
s3.completeMultipartUpload(uploadId, [part1, part2, part3])

Built-in resumability
No custom chunking logic needed
```

### Why Not Store Chunks in PostgreSQL?

```
POSTGRESQL BYTEA/BLOB storage:
════════════════════════════════════════════════════════

Storing 4MB chunks in PostgreSQL:

Chunks table:
chunk_id │ data (BYTEA)
─────────────────────────────
chunk_001│ [4MB binary]
chunk_002│ [4MB binary]
...
(trillions of rows)

Problems:
────────────────────────────────────────────────
→ Table bloat: trillions of 4MB rows = petabytes
→ Backup takes weeks (must backup entire database)
→ Vacuum struggles with large BYTEA columns
→ Index size explodes
→ Query cache polluted with binary data
→ Cannot leverage CDN (data stuck in DB)
→ Expensive: PostgreSQL storage more expensive than S3
→ Single point of failure (one DB vs S3's multi-AZ)


S3 is purpose-built for blob storage
PostgreSQL is purpose-built for relational data
Use each for what it's designed for
```

---

## Why Redis for Sync State?

### What is "Sync State"?

```
The real-time sync problem:
════════════════════════════════════════════════════════

User has 3 devices:
- Laptop (currently online)
- Desktop (offline)
- Phone (online)

User edits file.txt on laptop:
1. Which devices need to be notified?
2. Which devices have already received this update?
3. Is desktop behind on syncs (offline for 2 days)?
4. Should we show "syncing" or "synced" icon?

This is transient, frequently-changing state
Not worth storing permanently
Perfect for Redis (in-memory, fast, ephemeral)
```

### Redis Schema for Sync State

```
REDIS SYNC STATE SCHEMA:
════════════════════════════════════════════════════════

1. Device online status:
────────────────────────────────────────────────
Key:   device:online:device_abc123
Value: "1"
TTL:   30 seconds

SET device:online:device_abc123 "1" EX 30

Device sends heartbeat every 15 seconds
If TTL expires → device is offline
No need to explicitly mark offline


2. Device sync cursor (what version device has):
────────────────────────────────────────────────
Key:   device:cursor:device_abc123
Value: hash of sync cursor state

HSET device:cursor:device_abc123
  user_id "user_001"
  last_sync_version "8472"
  last_sync_time "1708956789"

When file updates:
→ Check all user's devices
→ Find devices with cursor < new version
→ Push update to those devices


3. Active sync operations (in-progress uploads):
────────────────────────────────────────────────
Key:   sync:upload:user_001:file_abc
Value: progress state

HSET sync:upload:user_001:file_abc
  file_id "file_abc"
  chunks_total 1280
  chunks_completed 845
  status "IN_PROGRESS"
  started_at "1708956789"

Used for:
→ Resume interrupted uploads
→ Show progress bar percentage
→ Detect stuck uploads (cleanup)


4. File change notifications queue:
────────────────────────────────────────────────
Key:   notifications:user_001
Type:  LIST

LPUSH notifications:user_001 '{
  "type": "file_updated",
  "file_id": "file_abc",
  "version": 5,
  "timestamp": 1708956789
}'

Device polls:
RPOP notifications:user_001

→ Real-time notification delivery
→ Device processes immediately
→ Starts download if needed


5. Conflict detection (simultaneous edits):
────────────────────────────────────────────────
Key:   conflict:file_abc
Value: conflict metadata

SET conflict:file_abc '{
  "file_id": "file_abc",
  "device_1": "laptop",
  "device_2": "phone",
  "version_1": 4,
  "version_2": 5,
  "detected_at": 1708956789
}' EX 3600

Mark file as conflicted
Both devices notified
User must resolve manually
```

### Why Redis Over PostgreSQL for This?

```
SYNC STATE CHARACTERISTICS:
════════════════════════════════════════════════════════

Temporary data:
→ Device online status: only matters right now
→ Upload progress: only matters during upload
→ Notifications: consumed once, then deleted

High write frequency:
→ Heartbeats every 15 seconds × 700M users = 46M writes/sec
→ PostgreSQL cannot sustain this write rate
→ Redis handles 1M+ writes/second easily

Read-heavy:
→ Every file operation checks device sync state
→ Redis sub-millisecond reads
→ PostgreSQL would bottleneck


POSTGRESQL would struggle:
────────────────────────────────────────────────
INSERT INTO device_heartbeats (device_id, timestamp)
VALUES ('device_abc', NOW())
ON CONFLICT (device_id) UPDATE SET timestamp = NOW();

46 million writes per second?
→ Write-ahead log overwhelmed
→ Index maintenance can't keep up
→ Autovacuum lags behind
→ Disk I/O saturated
→ System dies


REDIS handles this trivially:
────────────────────────────────────────────────
SET device:online:device_abc "1" EX 30

→ In-memory write: ~100 nanoseconds
→ No disk I/O (or async if persistence enabled)
→ TTL auto-expires stale data (no DELETE needed)
→ 1M+ ops/second sustained
→ Perfect fit
```

---

## The Deduplication Magic: Content-Addressable Storage

```
THE DEDUPLICATION PROBLEM:
════════════════════════════════════════════════════════

1 million users all upload Ubuntu ISO (3GB)
Without deduplication: 3GB × 1M = 3 petabytes stored
With deduplication: 3GB stored once = 3GB total
Savings: 99.9999%

How?


CONTENT-ADDRESSABLE STORAGE:
════════════════════════════════════════════════════════

Traditional storage:
File identified by name: /user_001/ubuntu.iso

Content-addressable:
File identified by content hash: sha256(file_content)

ubuntu.iso → SHA-256 → a3f5b8c2... (64 hex chars)


CHUNK-LEVEL DEDUPLICATION:
════════════════════════════════════════════════════════

File chunked into 4MB pieces
Each chunk hashed: sha256(chunk_data)

ubuntu.iso (3GB):
Chunk 1: hash = abc123...
Chunk 2: hash = def456...
Chunk 3: hash = ghi789...
...
Chunk 768: hash = xyz999...

Chunks table (PostgreSQL):
────────────────────────────────────────────────
chunk_hash      │ storage_url              │ ref_count
────────────────────────────────────────────────────────
abc123...       │ s3://bucket/abc123       │ 1,000,000
def456...       │ s3://bucket/def456       │ 1,000,000
ghi789...       │ s3://bucket/ghi789       │ 1,000,000

ref_count = how many files reference this chunk

When user_001 uploads ubuntu.iso:
1. Chunk file → compute hashes
2. Check: SELECT * FROM chunks WHERE chunk_hash = 'abc123...'
3. Hash exists? Don't upload chunk, just increment ref_count
4. Hash doesn't exist? Upload to S3, insert into chunks table

Result: First user uploads 3GB
        Next 999,999 users upload ZERO bytes
        All users can download full file
        Storage cost: 3GB instead of 3 petabytes
```

---

## Complete Schema Architecture

```
POSTGRESQL SCHEMA (sharded by user_id):
════════════════════════════════════════════════════════

Files table:
────────────────────────────────────────────────────────────────────────────
file_id      │ user_id  │ path         │ filename    │ size_bytes │ version │ checksum  │ chunk_ids │ is_deleted
────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
file_001     │ user_001 │ /Photos/     │ photo.jpg   │ 10485760   │ 3       │ abc123... │ [...array]│ false
file_002     │ user_001 │ /Documents/  │ report.pdf  │ 2097152    │ 1       │ def456... │ [...array]│ false
file_003     │ user_002 │ /Videos/     │ movie.mp4   │ 524288000  │ 2       │ ghi789... │ [...array]│ false

Indexes:
  PRIMARY KEY (file_id)
  INDEX (user_id, path)           ← folder listing
  INDEX (user_id, filename)       ← search by name
  INDEX (user_id, is_deleted)     ← list active files
  INDEX (checksum)                ← dedupe check


File_Versions table (history):
────────────────────────────────────────────────────────────────────────────
version_id   │ file_id  │ version │ checksum  │ chunk_ids │ size_bytes │ created_at          │ is_current
────────────────────────────────────────────────────────────────────────────────────────────────────────────
ver_001      │ file_001 │ 1       │ aaa111... │ [...]     │ 10000000   │ 2024-01-15 10:00:00 │ false
ver_002      │ file_001 │ 2       │ bbb222... │ [...]     │ 10200000   │ 2024-02-01 15:30:00 │ false
ver_003      │ file_001 │ 3       │ abc123... │ [...]     │ 10485760   │ 2024-02-26 09:00:00 │ true

Purpose: Restore to any previous version


Chunks table (global, not sharded):
────────────────────────────────────────────────────────────────────────────
chunk_hash        │ storage_url                  │ size_bytes │ ref_count │ created_at
────────────────────────────────────────────────────────────────────────────────────────
abc123...         │ s3://bucket/abc/abc123       │ 4194304    │ 50000     │ 2024-01-15
def456...         │ s3://bucket/def/def456       │ 4194304    │ 30000     │ 2024-01-16
ghi789...         │ s3://bucket/ghi/ghi789       │ 4194304    │ 1         │ 2024-02-26

ref_count: How many file versions reference this chunk
When ref_count = 0 → can delete from S3 (garbage collection)

Index:
  PRIMARY KEY (chunk_hash)
  INDEX (ref_count) ← find orphaned chunks


Users table:
────────────────────────────────────────────────────────────────────────────
user_id      │ email                │ storage_quota │ storage_used │ plan
────────────────────────────────────────────────────────────────────────────────
user_001     │ alice@example.com    │ 2000000000    │ 1500000000   │ free
user_002     │ bob@example.com      │ 100000000000  │ 50000000000  │ pro

storage_used updated on every file operation
Enforce quota before allowing uploads


REDIS SCHEMA:
════════════════════════════════════════════════════════

Device online status:
────────────────────────────────────────────────
HASH device:online:user_001
  laptop_123    "1708956789"  ← last heartbeat timestamp
  phone_456     "1708956785"
  desktop_789   "1708956750"  ← older, possibly offline

TTL: 60 seconds per hash field


Sync cursors:
────────────────────────────────────────────────
HASH device:cursor:laptop_123
  last_sync_version  "8472"
  last_sync_time     "1708956789"
  pending_uploads    "3"
  pending_downloads  "0"


Upload progress:
────────────────────────────────────────────────
HASH upload:progress:user_001:file_abc
  total_chunks       "1280"
  completed_chunks   "845"
  failed_chunks      "2"
  status             "IN_PROGRESS"
  started_at         "1708956789"

TTL: 24 hours (cleanup stuck uploads)


Change notifications:
────────────────────────────────────────────────
LIST notifications:user_001:laptop_123

[
  '{"type":"file_updated","file_id":"file_abc","version":5}',
  '{"type":"file_deleted","file_id":"file_xyz","version":3}',
  '{"type":"conflict","file_id":"file_def","versions":[4,5]}'
]

Device polls: RPOP notifications:user_001:laptop_123


S3 STORAGE (via storage_url in chunks table):
════════════════════════════════════════════════════════

Bucket structure:
────────────────────────────────────────────────
s3://dropbox-chunks/
  abc/
    abc123def456ghi789...  ← chunk file (4MB)
  def/
    def456ghi789abc123...
  ghi/
    ghi789abc123def456...

Organized by hash prefix for even distribution
Prevents "hot" S3 partitions


Lifecycle policies:
────────────────────────────────────────────────
Chunks not accessed in 90 days:
  → Move to S3 Glacier

Chunks with ref_count=0 for 30 days:
  → Delete permanently

Automatic cost optimization
```

---

## Complete Database Flow

```
FLOW 1: User Uploads New File
════════════════════════════════════════════════════════

User uploads photo.jpg (10MB) from laptop
        │
        ▼
STEP 1: Client chunks file
────────────────────────────────────────────────
photo.jpg (10MB) → 3 chunks × 4MB each
Chunk 1: bytes 0-4MB     → hash: abc123...
Chunk 2: bytes 4-8MB     → hash: def456...
Chunk 3: bytes 8-10MB    → hash: ghi789...


STEP 2: Check which chunks already exist (dedup)
────────────────────────────────────────────────
SELECT chunk_hash, storage_url
FROM chunks
WHERE chunk_hash IN ('abc123...', 'def456...', 'ghi789...');

Results:
abc123... → exists (ref_count: 50000) ← Ubuntu chunk!
def456... → NOT FOUND
ghi789... → NOT FOUND

Only need to upload chunks 2 and 3 (8MB instead of 10MB)


STEP 3: Upload missing chunks to S3
────────────────────────────────────────────────
For chunk 2 (def456...):
PUT s3://bucket/def/def456... [4MB data]

For chunk 3 (ghi789...):
PUT s3://bucket/ghi/ghi789... [2MB data]

Parallel upload (both at once)
Time: ~5 seconds on good connection


STEP 4: Update chunks table
────────────────────────────────────────────────
BEGIN TRANSACTION;

-- Increment ref_count for existing chunk
UPDATE chunks
SET ref_count = ref_count + 1
WHERE chunk_hash = 'abc123...';

-- Insert new chunks
INSERT INTO chunks (chunk_hash, storage_url, size_bytes, ref_count)
VALUES 
  ('def456...', 's3://bucket/def/def456', 4194304, 1),
  ('ghi789...', 's3://bucket/ghi/ghi789', 2097152, 1);

COMMIT;


STEP 5: Create file metadata
────────────────────────────────────────────────
BEGIN TRANSACTION;

-- Insert file record
INSERT INTO files
(file_id, user_id, path, filename, size_bytes, version, checksum, chunk_ids)
VALUES
('file_abc', 'user_001', '/Photos/', 'photo.jpg', 10485760, 1, 
 'photo_hash', '["abc123...", "def456...", "ghi789..."]');

-- Insert version record
INSERT INTO file_versions
(version_id, file_id, version, checksum, chunk_ids, size_bytes, created_at, is_current)
VALUES
('ver_abc_1', 'file_abc', 1, 'photo_hash', '["abc123...", "def456...", "ghi789..."]', 
 10485760, NOW(), true);

-- Update user storage
UPDATE users
SET storage_used = storage_used + 10485760
WHERE user_id = 'user_001';

COMMIT;


STEP 6: Notify other devices via Redis
────────────────────────────────────────────────
-- Find user's other devices
HGETALL device:online:user_001

Returns:
phone_456     → online
desktop_789   → offline (old timestamp)

-- Push notification to online devices
LPUSH notifications:user_001:phone_456 '{
  "type": "file_created",
  "file_id": "file_abc",
  "path": "/Photos/photo.jpg",
  "version": 1,
  "timestamp": 1708956789
}'

Phone receives notification instantly
Starts downloading photo.jpg
```

```
FLOW 2: User Edits File on Multiple Devices (Conflict)
════════════════════════════════════════════════════════

Initial state: report.docx version 3 on all devices

User goes offline on both laptop and phone
        │
        ├──→ Laptop: Edits report.docx → version 4 (laptop)
        └──→ Phone:  Edits report.docx → version 4 (phone)

Both come online simultaneously
        │
        ▼
BOTH devices try to upload version 4

Timeline:
────────────────────────────────────────────────
T+0ms:  Laptop uploads version 4
        PostgreSQL transaction starts
        
T+1ms:  Phone uploads version 4
        PostgreSQL transaction starts (BLOCKED on row lock)

T+5ms:  Laptop transaction:
        SELECT version FROM files WHERE file_id = 'file_xyz'
        → version = 3 (as expected)
        
        UPDATE files
        SET version = 4, checksum = 'laptop_hash', ...
        WHERE file_id = 'file_xyz' AND version = 3;
        
        COMMIT;
        
        Laptop's version 4 saved successfully

T+6ms:  Phone transaction unblocks:
        SELECT version FROM files WHERE file_id = 'file_xyz'
        → version = 4 (laptop already updated!)
        
        UPDATE files
        SET version = 4, checksum = 'phone_hash', ...
        WHERE file_id = 'file_xyz' AND version = 3;
        
        WHERE clause fails (version is now 4, not 3)
        → 0 rows updated
        → CONFLICT DETECTED
        
        Transaction rolls back

T+7ms:  Server detects conflict:
        → Rename phone's file to "report (phone's conflicted copy).docx"
        → Save as new file with version 1
        → Notify user on both devices

STEP 7: Mark conflict in Redis
────────────────────────────────────────────────
SET conflict:file_xyz '{
  "file_id": "file_xyz",
  "device_1": "laptop_123",
  "device_2": "phone_456",
  "version_laptop": 4,
  "version_phone": 4,
  "resolution": "created_copy"
}' EX 86400

LPUSH notifications:user_001:laptop_123 '{
  "type": "conflict_resolved",
  "file_original": "report.docx",
  "file_copy": "report (phone conflicted copy).docx"
}'

LPUSH notifications:user_001:phone_456 '{
  "type": "conflict_resolved",
  "file_original": "report.docx",
  "file_copy": "report (phone conflicted copy).docx"
}'

User sees both files, can manually merge
```

```
FLOW 3: User Downloads File
════════════════════════════════════════════════════════

User clicks on video.mp4 (500MB, 125 chunks) on phone
        │
        ▼
STEP 1: Fetch file metadata from PostgreSQL
────────────────────────────────────────────────
SELECT file_id, chunk_ids, size_bytes, checksum
FROM files
WHERE file_id = 'file_video' AND user_id = 'user_001';

Returns:
chunk_ids: ["hash1", "hash2", ..., "hash125"]  (125 chunks)


STEP 2: Fetch chunk locations
────────────────────────────────────────────────
SELECT chunk_hash, storage_url
FROM chunks
WHERE chunk_hash IN ('hash1', 'hash2', ..., 'hash125');

Returns:
hash1 → s3://bucket/abc/hash1
hash2 → s3://bucket/def/hash2
...
hash125 → s3://bucket/xyz/hash125


STEP 3: Download chunks from S3 (parallel)
────────────────────────────────────────────────
Phone opens 10 parallel connections:
Thread 1: GET s3://bucket/abc/hash1   (chunk 1)
Thread 2: GET s3://bucket/def/hash2   (chunk 2)
Thread 3: GET s3://bucket/ghi/hash3   (chunk 3)
...
Thread 10: GET s3://bucket/xyz/hash10 (chunk 10)

As each chunk completes:
→ Write to local file at correct offset
→ Update Redis progress
→ Download next chunk

HSET download:progress:user_001:file_video
  total_chunks      "125"
  completed_chunks  "47"
  status            "IN_PROGRESS"


STEP 4: Verify checksum
────────────────────────────────────────────────
All chunks downloaded
Compute SHA-256 of assembled file
Compare to checksum from metadata

If match: Download successful ✓
If mismatch: Re-download corrupted chunks


STEP 5: Update Redis sync state
────────────────────────────────────────────────
HSET device:cursor:phone_456
  last_sync_version  "8475"
  last_sync_time     "1708956799"

DEL download:progress:user_001:file_video

Phone now has latest version
```

```
FLOW 4: User Restores Old Version
════════════════════════════════════════════════════════

User: "Restore report.pdf to version 3 from last week"
        │
        ▼
STEP 1: Query version history
────────────────────────────────────────────────
SELECT version_id, version, checksum, chunk_ids, created_at
FROM file_versions
WHERE file_id = 'file_report'
ORDER BY version DESC;

Returns:
Version 5: 2024-02-26 (current)
Version 4: 2024-02-20
Version 3: 2024-02-15 ← USER WANTS THIS
Version 2: 2024-02-10
Version 1: 2024-02-01


STEP 2: Create new version from old chunks
────────────────────────────────────────────────
BEGIN TRANSACTION;

-- Mark current version as not current
UPDATE file_versions
SET is_current = false
WHERE file_id = 'file_report' AND version = 5;

-- Copy version 3 as version 6 (restore creates new version)
INSERT INTO file_versions
(version_id, file_id, version, checksum, chunk_ids, size_bytes, created_at, is_current)
SELECT 
  gen_random_uuid(),
  file_id,
  6,  -- new version number
  checksum,
  chunk_ids,
  size_bytes,
  NOW(),
  true
FROM file_versions
WHERE file_id = 'file_report' AND version = 3;

-- Update files table
UPDATE files
SET version = 6, checksum = (SELECT checksum FROM file_versions WHERE version = 3)
WHERE file_id = 'file_report';

-- Increment ref_count for chunks (they're used again)
UPDATE chunks
SET ref_count = ref_count + 1
WHERE chunk_hash IN (SELECT unnest(chunk_ids) FROM file_versions WHERE version = 3);

COMMIT;


STEP 3: Notify all devices
────────────────────────────────────────────────
HGETALL device:online:user_001

For each online device:
LPUSH notifications:user_001:laptop_123 '{
  "type": "file_updated",
  "file_id": "file_report",
  "version": 6,
  "restored_from": 3
}'

All devices re-download latest version (which is old content)
```

```
FLOW 5: Garbage Collection (Delete Unused Chunks)
════════════════════════════════════════════════════════

Background job runs daily:

STEP 1: Find chunks with zero references
────────────────────────────────────────────────
SELECT chunk_hash, storage_url
FROM chunks
WHERE ref_count = 0
AND created_at < NOW() - INTERVAL '30 days';

These chunks are no longer referenced by any file version


STEP 2: Delete from S3
────────────────────────────────────────────────
For each chunk:
DELETE s3://bucket/abc/chunk_hash


STEP 3: Delete from PostgreSQL
────────────────────────────────────────────────
DELETE FROM chunks
WHERE ref_count = 0
AND created_at < NOW() - INTERVAL '30 days';

Frees up storage
Reduces costs
```

---

## Tradeoffs vs Other Databases

```
┌───────────────────────┬────────────────┬────────────────┬─────────────────┐
│                       │ THIS ARCH      │ MONGO ONLY     │ POSTGRES + BLOB │
├───────────────────────┼────────────────┼────────────────┼─────────────────┤
│ File metadata queries │ PostgreSQL ✓   │ MongoDB ✓      │ PostgreSQL ✓    │
│ ACID transactions     │ PostgreSQL ✓   │ Limited        │ PostgreSQL ✓    │
│ Chunk deduplication   │ Hash table ✓   │ Possible       │ Possible        │
│ Blob storage cost     │ S3 cheap ✓     │ MongoDB $$$$   │ PostgreSQL $$$$ │
│ CDN integration       │ CloudFront ✓   │ Manual         │ Manual          │
│ Durability            │ S3 11-nines ✓  │ 3-replica      │ PostgreSQL      │
│ Resumable uploads     │ S3 multipart✓  │ GridFS         │ Custom          │
│ Sync state speed      │ Redis <1ms ✓   │ MongoDB 5ms    │ PostgreSQL 10ms │
│ Horizontal scale      │ Sharding ✓     │ MongoDB ✓      │ Sharding ✓      │
│ Operational cost      │ HIGH           │ MEDIUM         │ LOW             │
└───────────────────────┴────────────────┴────────────────┴─────────────────┘
```

---

## One Line Summary

> **PostgreSQL stores lightweight file metadata (path, version, checksum, chunk references) because it enables complex queries like "show PDFs in /Work modified this week" and ACID transactions ensure version updates are atomic preventing corruption during conflicts, S3 stores the actual file chunks because it's 10x cheaper than database storage ($0.023/GB vs $0.23/GB), provides 11-nines durability through automatic multi-AZ replication that would require a massive infrastructure team to replicate, and offers built-in multipart upload for resumability plus CloudFront CDN for global distribution, while Redis stores ephemeral sync state (device online status, upload progress, change notifications) because tracking 46 million heartbeats per second requires sub-millisecond in-memory operations that would overwhelm PostgreSQL's disk-based write-ahead log — together they enable Dropbox's magic of uploading 5GB files resumably, storing Ubuntu ISO once despite a million uploads through content-addressable chunking with SHA-256 hashes, and syncing changes across devices in under 100ms through WebSocket push notifications orchestrated by Redis Pub/Sub.**