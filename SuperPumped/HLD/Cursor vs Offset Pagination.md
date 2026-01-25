
This is a **very common system-design + backend interview topic**, so I’ll explain it **clearly, with examples, SQL, and database load impact**.

---

# 1️⃣ Offset Pagination

### 🔹 What it is

Pagination using **LIMIT + OFFSET**  
You tell DB: _skip first N rows, then return next K rows_.

### 🔹 Example

Page size = 10

|Page|Query|
|---|---|
|Page 1|`LIMIT 10 OFFSET 0`|
|Page 2|`LIMIT 10 OFFSET 10`|
|Page 1000|`LIMIT 10 OFFSET 9990`|

### 🔹 SQL Example

`SELECT * FROM orders ORDER BY created_at DESC LIMIT 10 OFFSET 1000;`

---

## 🔹 How DB Executes This (Important 🔥)

Even if you only want **10 rows**, DB must:

1. Scan rows
    
2. Sort rows
    
3. **Skip first 1000 rows**
    
4. Return next 10 rows
    

👉 DB still _processes 1010 rows_

---

## 🔹 Database Load Impact

|Factor|Impact|
|---|---|
|Large OFFSET|❌ Very expensive|
|Disk IO|❌ High|
|CPU|❌ High|
|Index usage|⚠️ Partial|
|Deep pages|❌ Slow|
|Writes during pagination|❌ Results can shift|

---

## 🔹 Problems with Offset Pagination

### ❌ Slow for large data

`OFFSET 1,000,000 → DB still scans 1,000,010 rows`

### ❌ Inconsistent results

If new rows are inserted:

- Records shift
    
- Users see duplicates or miss rows
    

---

## 🔹 When Offset Pagination Is OK

✔ Small datasets  
✔ Admin dashboards  
✔ Page number UI (1,2,3…)

---

# 2️⃣ Cursor Pagination (Keyset Pagination)

### 🔹 What it is

Pagination using a **cursor** (usually a unique indexed column like `id` or `created_at`).

Instead of _“skip rows”_, we say:

> “Give me rows **after this value**”

---

## 🔹 Example

### First Page

`SELECT * FROM orders ORDER BY id DESC LIMIT 10;`

Last row has `id = 500`

---

### Next Page

`SELECT * FROM orders WHERE id < 500 ORDER BY id DESC LIMIT 10;`

---

## 🔹 Cursor = Last Seen Value

`{   "next_cursor": 500 }`

Client sends cursor for next request.

---

## 🔹 How DB Executes This (🔥 Efficient)

1. Uses **index**
    
2. Jumps directly to `id < 500`
    
3. Reads only **10 rows**
    

👉 **No skipping, no scanning millions**

---

## 🔹 Database Load Impact

|Factor|Impact|
|---|---|
|Rows scanned|✅ Minimal|
|Index usage|✅ Excellent|
|CPU|✅ Low|
|Disk IO|✅ Low|
|Large datasets|✅ Very fast|
|Writes during pagination|✅ Stable|

---

## 🔹 Why Cursor Pagination Is Faster

### Offset:

`Scan → Skip → Return`

### Cursor:

`Index seek → Return`

---

# 3️⃣ Visual Comparison (DB Work)

### OFFSET

`|----skip 100000 rows----|--> return 10`

### CURSOR

            `|--> return 10`

---

# 4️⃣ Consistency During Inserts

### Offset Pagination

If a new row is inserted at top:

- Page 2 shifts
    
- Duplicates / missing rows
    

### Cursor Pagination

- Cursor is fixed (`id < last_seen`)
    
- No shifting
    
- Stable results
    

---

# 5️⃣ Real-World Usage

|System|Pagination Type|
|---|---|
|Instagram feed|Cursor|
|Twitter timeline|Cursor|
|Infinite scroll|Cursor|
|Admin tables|Offset|
|Reports|Offset|
|Logs / events|Cursor|

---

# 6️⃣ Comparison Summary (Interview Ready)

|Feature|Offset|Cursor|
|---|---|---|
|Query|LIMIT + OFFSET|WHERE > / <|
|DB Load|❌ High|✅ Low|
|Deep pages|❌ Slow|✅ Fast|
|Index usage|⚠️ Partial|✅ Full|
|Consistency|❌ Poor|✅ Strong|
|Page number UI|✅ Easy|❌ Hard|
|Infinite scroll|❌ Bad|✅ Perfect|

---

# 7️⃣ Interview One-Line Answer (🔥)

> **Offset pagination scans and skips rows, causing high DB load for large offsets, while cursor pagination uses indexed range queries that directly fetch the next set of rows, making it more scalable and consistent.**