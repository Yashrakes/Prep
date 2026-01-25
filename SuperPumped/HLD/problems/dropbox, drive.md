	
requirments -> core entoties -> api, -> high level design ->deep dives

requirments 

https://www.hellointerview.com/learn/system-design/problem-breakdowns/dropbox


functional requirments 

upload a file from remote storaeg 
download storage 
sync files across devices
 Users should be able to edit files
Users should be able to view files without downloading them

non functional 
low latency uploadas and downloads
availability (priortize)>> consistency 
support largefiles gbs
resumable uploads
high sync accuracy 

core entities 
files, users

apis
/post/file
{
file,
filemetadata
}
get/files/{fileid}
get/changes/timestamp
-> returnd fileid



1. The client will chunk the file into 5-10Mb pieces and calculate a fingerprint for each chunk. It will also calculate a fingerprint for the entire file, this becomes the fileId.
    
2. The client will send a GET request to fetch the FileMetadata for the file with the given fileId (fingerprint) in order to see if it already exists -- in which case, we can resume the upload.
    
3. If the file does not exist, the client will POST a request to /files/presigned-url to get a presigned URL for the file. The backend will save the file metadata in the FileMetadata table with a status of "uploading" and the chunks array will be a list of the chunk fingerprints with a status of "not-uploaded".
    
4. The client will then upload each chunk to S3 using the presigned URL. After each chunk is uploaded, S3 will send a message to our backend using S3 event notifications. Our backend will then update the chunks field in the FileMetadata table to mark the chunk as "uploaded".
    
5. Once all chunks in our chunks array are marked as "uploaded", the backend will update the FileMetadata table to mark the file as "uploaded".



Good question. Let’s break **why the second (fully normalized) approach is considered better**, explicitly in terms of **trade-offs**, not just “it’s cleaner”.

---
### 3) Users should be able to share a file with other users

## Recap of the two approaches

### 1️⃣ Cached inverse mapping (denormalized)

You store:

- File metadata contains `shareList = [user1, user2, …]`
    
- Separate cache/table:
    
    `user1 → [fileId1, fileId2]`
    

This gives **O(1)** lookup for “files shared with me”.

### 2️⃣ Normalized mapping table

You store:

`SharedFiles(userId, fileId)`

Each row = one share relationship  
No share list inside file metadata.

---

## Why approach #2 is better: trade-offs explained

### 1. **Data consistency vs read speed**

|Aspect|Approach 1 (Cache inverse)|Approach 2 (Normalized)|
|---|---|---|
|Read performance|🔥 Very fast (KV lookup)|⚡ Fast but not constant|
|Consistency|❌ Hard|✅ Easy|

**Why #2 wins**

- In approach #1, you have **two sources of truth**
    
    - File metadata sharelist
        
    - User → file cache
        
- Any failure, retry, partial write, or race condition can cause **data drift**
    

Example failure:

`Share file with user1 ✔ file metadata updated ❌ user cache update failed`

Now:

- File says it’s shared
    
- User can’t see it
    

Approach #2 has **single source of truth**, so this class of bugs disappears.

👉 **Trade-off**: Slightly slower reads for **much stronger correctness guarantees**

---

### 2. **Transactional simplicity**

|Aspect|Approach 1|Approach 2|
|---|---|---|
|Transactions|Cross-entity|Single write|
|Failure handling|Complex|Simple|

**Approach 1**

- Requires a **multi-write transaction**
    
- Harder to scale in distributed DBs (DynamoDB, Cassandra, sharded SQL)
    

**Approach 2**

- One insert = one share
    
- One delete = unshare
    
- Naturally transactional
    

👉 **Trade-off**: You trade read micro-optimizations for write safety and simpler logic

---

### 3. **Scalability of sharing**

|Aspect|Approach 1|Approach 2|
|---|---|---|
|File shared with 1M users|❌ Bad|✅ Fine|
|User shared with 1M files|❌ Cache explosion|✅ Row-based|

**Approach 1 problems**

- File metadata sharelist can grow unbounded
    
- Cache entry for a heavy user becomes huge
    

**Approach 2**

- Each share is one row
    
- Storage and access scale linearly
    

👉 **Trade-off**: Normalization allows **horizontal scaling** at the cost of one indexed lookup

---

### 4. **Easier queries & future features**

With `SharedFiles(userId, fileId)` you get:

- Pagination
    
- Sorting by time
    
- Filtering by permission
    
- “Files shared in last 7 days”
    
- Analytics (“top shared files”)
    

With cached lists:

- You fetch everything
    
- Filter in memory
    
- Hard to paginate efficiently
    

👉 **Trade-off**: You pay a small query cost to gain **query flexibility**

---

### 5. **Cache correctness becomes optional**

In approach #2:

- DB is authoritative
    
- Cache becomes a **read optimization**, not a correctness requirement
    

In approach #1:

- Cache is **part of correctness**
    
- Cache bugs = data bugs
    

👉 This is a huge architectural advantage.

---

### 6. **Failure modes comparison**

|Scenario|Approach 1|Approach 2|
|---|---|---|
|Partial write|❌ Inconsistent|✅ Impossible|
|Cache eviction|❌ User loses files|✅ Just slower|
|Retry storms|❌ Duplicate bugs|✅ Idempotent|

Approach #2 degrades gracefully.  
Approach #1 fails **incorrectly**.

---

## Why “slightly less efficient” is acceptable

Yes, approach #2 uses an index lookup instead of KV fetch.

But in real systems:

- Index lookup ≈ few milliseconds
    
- Correctness bugs = hours/days of production pain
    

Modern systems **optimize for correctness first**, then cache.

---

## Final verdict (interview-quality answer)

> The second approach is better because it **eliminates data duplication and synchronization complexity**, providing a single source of truth. While reads are marginally slower due to indexed queries, the system becomes significantly more reliable, easier to scale, and simpler to reason about. Any performance loss can be mitigated with caching, but correctness cannot be retrofitted later.

If you want, I can:

- Map this to **DynamoDB / SQL / Redis**
    
- Explain how **Google Drive / Dropbox** do this
    
- Show when approach #1 _might_ still be acceptable











