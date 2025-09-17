---

tags: [system-design, dropbox, file-sync, chunking, blob-storage]

created: 2025-06-18

---

## [Chat GPT Ref](https://chatgpt.com/share/6852fa78-337c-8009-a3ea-ca111f47bb52)

# High-Level Design of Dropbox – File Chunking, Storage, Sync, and UI Display

  

## 📁 1. Chunking Files (Client & Server Side)

  

### 🧩 On the Client Side

- Files are split into **chunks** (typically 4MB–8MB, configurable).

- Each chunk is:

  - Hashed (e.g., using SHA-256).

  - Given a unique **Chunk ID**.

- Deduplication is performed using the hash — if a chunk already exists, no need to upload again.

- Upload is done in **parallel/multipart** to improve speed and resilience.

  

#### ✅ Benefits

- Efficient resuming of interrupted uploads.

- Supports deduplication and delta syncing.

  

---

  

## 🗃️ 2. Chunk Storage (Blob Storage Backend)

  

### Blob Storage (e.g., S3, GCS)

- Each chunk is stored as a blob using its hash as a key.

- Metadata DB (PostgreSQL, Cassandra, etc.) keeps:

  - File → [List of Chunk IDs]

  - Chunk ID → Blob location, reference count

  - Versioning info for sync and rollback

  

#### ✅ Benefits

- Scalability.

- Deduplication.

- Easy cleanup via reference counting.

  

---

  

## 🔁 3. File Syncing Between Client & Server

  

### 🖥️ Client-Side

- Maintains a local lightweight DB (e.g., SQLite) of file state.

- Detects new or modified files via timestamp/hash comparison.

- Uploads only changed chunks and sends a **manifest**:

  ```json

  {

    "fileName": "example.txt",

    "chunkIDs": ["abc123", "def456", "ghi789"],

    "version": 2

  }

  ```

  

### ☁️ Server-Side

- Compares manifests with current version.

- Applies updates or resolves conflicts.

- Stores only the new/changed chunks.

  

### 📤 Sync Down to Client

- Clients subscribe to file change events.

- Downloads only changed chunks and reconstructs the updated file.

  

---

  

## 🌐 4. Dropbox UI Display

  

### Metadata Service

- UI queries metadata DB to list files and folders.

- Response includes:

  - File name, size, version, last modified, mime type.

  - Icon or preview thumbnail.

  

### File Download

- UI sends download request → server streams and reconstructs file.

- For previews:

  - Pre-rendered or real-time preview links are used.

  

---

  

## 📊 Summary Architecture

  

```text

[Client]

   |

   |---> Chunk File (hash, split)

   |---> Upload Chunks (if not exists)

   |---> Send Manifest (file → chunks)

   |

[Server]

   |---> Store chunks in Blob Storage

   |---> Save metadata in DB

   |---> Handle sync/versioning/conflicts

   |

[Blob Storage]

   |---> chunk_hash → actual blob

   |

[Metadata DB]

   |---> file_id → [chunk_ids], version, etc.

   |

[UI]

   |---> List files/folders

   |---> Fetch and display previews/download files

```

  

---

  

## 🔍 5. How Dropbox Detects Changed Chunks

  

### 📁 Re-upload Flow

  

1. Client re-reads and **re-chunks** the modified file.

2. Computes **hashes** for each chunk.

3. Compares hashes to the previous version (cached locally).

4. Uploads only changed chunks.

  

### 🧠 Why Content-Defined Chunking (CDC)?

- Fixed-size chunking fails on small inserts (all chunks shift).

- CDC (e.g., Rabin fingerprinting) aligns chunk boundaries with content.

- Only nearby chunks change, rest are reused.

  

#### ✅ Benefits

- Better deduplication

- Efficient delta sync

  

---

  

## 🗂 Local Cache

- Stores chunk boundaries and hashes per file.

- Uses lightweight DB (e.g., SQLite) for quick diffing.

  

---

  

## ☁️ Server-Side Deduplication

- Receives manifest of chunk hashes.

- Checks blob store for existing chunks.

- Stores only new chunks and updates metadata with new file version.

  

---

  

## 🔍 Visual Example

  

```text

Old File:

[Chunk A][Chunk B][Chunk C][Chunk D]

  

Modified File:

[Chunk A][Chunk B'][Chunk C][Chunk D]

  

Client:

- Rehashes chunks

- Finds Chunk B changed

- Only uploads Chunk B'

  

Server:

- Reuses A, C, D

- Stores new manifest:

  file_v2 = [A, B', C, D]

```

  

---

  

## 🛠 Tools Used

- Hashing: SHA-256 or similar

- CDC Algorithms: Rabin fingerprinting

- Local DB: SQLite

  

---

  

## ✅ TL;DR

Dropbox detects changed chunks by **rehashing** the file using a consistent chunking strategy (preferably CDC), comparing against cached hashes, and uploading only the **modified chunks**, enabling efficient delta syncing.