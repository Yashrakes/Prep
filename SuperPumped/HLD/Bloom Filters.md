# Index

- [[# RESOURCE]]
- [[#PROBLEM STATMENT]]
- [[#COMPARE WITH HASH SET]]
- [[#CORE WORKING OF BLOOM FILTER]]


# RESOURCE
https://www.geeksforgeeks.org/bloom-filters-introduction-and-python-implementation/



# PROBLEM STATMENT

==PROBLEM STATMENT==
Suppose you are entering a username a a particular site , and every time you enter a new name or id. it will go and check its entry and compare all records in databases which will be a quiet user frustrating experience

- Alternate ways  -> Linear search, binary search , -> very bad for millions of entries

To solve this problem Bloom Filters came in picture
Bloom filter uses a space optimised hash algorithms
and bloom filter might tell you false positive but it will never tell false negative 


- FALSE POSITIVE -> so this is the case where there is no entry of that unique name in database but it might tell that there is already a entry present in database
- FALSE NEGATIVE -> so this is the case where there is a entry in database with that entered id, but it might tell you that there is not a entry in database

We call **Bloom filters** a **space-optimized** or **space-efficient** approach because they let us **test set membership using dramatically less memory** than traditional data structures like hash sets or arrays — especially when dealing with **huge volumes of data**.

### ✅ **Advantages**

- Very **space-efficient**.
    
- **Fast** insert and lookup (constant time).
    
- Suitable for large-scale, memory-sensitive applications.
    

---

### ❌ **Disadvantages**

- **False positives**: might say an element is in the set when it's not.
    
- **No deletion** (standard Bloom filters).
    
- Cannot retrieve elements – only tells membership.

### **Who Uses Bloom Filters?**

#### 🔹 **Databases & Caches**

- **Apache Cassandra** & **HBase** use Bloom filters to avoid unnecessary disk reads.
    
    - Before reading from disk, they check Bloom filters to see if a key **might** exist.
        

#### 🔹 **Big Data & Distributed Systems**

- **Apache Hadoop** and **Apache Spark** for efficient join filtering.
    
- **Google Bigtable** and **LevelDB** use them for SSTable lookups.
    

#### 🔹 **Web Browsers / Security**

- **Google Safe Browsing** uses Bloom filters to check if URLs are malicious.
    
- **Ad blockers** use them to test domain presence quickly.
    

#### 🔹 **Networking**

- Caching DNS queries.
    
- Peer-to-peer systems (e.g., BitTorrent DHT) for sharing membership info.

# COMPARE WITH HASH SET


## ✅ Why Are Bloom Filters Space Optimized?

### 1. **They use bits, not full elements**

- A Bloom filter doesn't store the **actual data** (e.g., full strings or keys).
    
- It only stores a **bit array** and relies on hash functions.
    
- This drastically reduces memory usage.
    

Example:

- Storing 1 billion strings in a `HashSet` might take **dozens of GBs**.
    
- A Bloom filter can do approximate membership checking with **~1.2 GB** (as we calculated earlier). for 1 billion user according to algo it will take 1.2gb data for all bits 
    

---

### 2. **They trade accuracy for space**

- You get **false positives**, but **never false negatives**.
    
- By accepting this minor inaccuracy, you save **enormous amounts of memory**.
    

This trade-off makes Bloom filters ideal when:

- **Absolute precision isn't required**
    
- **Speed and space** are more critical than correctness
    

---

## 📊 Memory Comparison Example

Let’s say you're storing **1 billion 10-character strings**:

### 🟥 Traditional HashSet:

- Assume ~50–100 bytes per string (includes object overhead, hash codes, pointers, etc.)
    
- Total: **~50–100 GB**
    

### 🟩 Bloom Filter:

- With 1% false positive rate: ~1.2 GB (as we saw)
    
- **40–80x space saving**
    

---

## 🧠 Real-World Intuition

- Imagine you’re a **bouncer at a club** with a guest list of 1 billion names.
    
    - **HashSet approach**: Carry a full book of all names → very heavy.
        
    - **Bloom filter approach**: Carry a small booklet with **just some hash-based marks**. You'll **never let in someone not invited**, but may occasionally double-check someone who’s on the fence.
        

---

## 🧵 Summary

|Feature|Bloom Filter|HashSet|
|---|---|---|
|Stores actual data|❌ No|✅ Yes|
|Memory usage|✅ Very low|❌ Very high|
|Supports deletion|❌ (unless CBF)|✅ Yes|
|False positives|✅ Possible|❌ No|
|False negatives|❌ Never|❌ Never|
|Space optimized?|✅ Yes|❌ No|




# CORE WORKING OF BLOOM FILTER

---

That’s why Bloom filters are widely adopted in **distributed systems, databases, security tools, and networking**, where memory and performance are more valuable than absolute accuracy.

Would you like a visual example or Java code showing this space advantage in practice?
## 1. **Core Components of a Bloom Filter**

### ✅ Bit Array

- A Bloom filter has a bit array of size `m`.
    
- Initially, all bits are set to `0`.
    

### ✅ k Hash Functions

- These are independent hash functions that take an input and return a number between `0` and `m-1`.
    
- Each function maps the same input to a deterministic but different** index.
    

---

## 🟢 2. **Adding Elements to the Filter**

Let’s say we're adding the element `"cat"`.

### Step-by-step:

1. **Hash "cat" with k hash functions**:  
    Suppose:
    
    - `h1("cat") = 2`
        
    - `h2("cat") = 5`
        
    - `h3("cat") = 9`
        
2. **Set the corresponding bits to 1**:
    
    - bit[2] = 1
        
    - bit[5] = 1
        
    - bit[9] = 1
        

🔢 Now the bit array might look like:

makefile

CopyEdit

`Index:    0 1 2 3 4 5 6 7 8 9 Bits:     0 0 1 0 0 1 0 0 0 1`

---

## 🔍 3. **Checking Membership (Querying)**

Let’s check if `"dog"` is in the set.

1. **Hash "dog"**:  
    Suppose:
    
    - `h1("dog") = 2`
        
    - `h2("dog") = 3`
        
    - `h3("dog") = 9`
        
2. **Check the bits**:
    
    - bit[2] = 1 ✅
        
    - bit[3] = 0 ❌
        
    - bit[9] = 1 ✅
        

Since one of the bits (bit[3]) is **0**, the Bloom filter concludes:

> ❌ **"dog" is definitely not in the set.**

---

## ⚠️ 4. **False Positives**

Now suppose we check `"bat"`:

- `h1("bat") = 2`
    
- `h2("bat") = 5`
    
- `h3("bat") = 9`
    

All these positions are **already set to 1** from when we added `"cat"`, **even though "bat" was never added**.

Hence, the Bloom filter will say:

> ✅ **"bat" is possibly in the set** — **false positive**.

This happens because different elements can hash to the same positions.

---

## 💡 5. **Why False Negatives Are Not Possible**

If you insert `"cat"` and then check for `"cat"`, all bits you set during insert will still be set.

So:

- You will never get a false negative.
    
- If any bit required for a query is `0`, the item **was definitely never added**.
    

---

## 📉 6. **Tuning Parameters (m, k, n)**

To control false positive rate, you choose:

- `m`: Size of the bit array.
    
- `k`: Number of hash functions.
    
- `n`: Expected number of inserted items.
    

### False Positive Probability Formula:

P≈(1−e−kn/m)kP \approx \left(1 - e^{-kn/m}\right)^kP≈(1−e−kn/m)k

Where:

- `P` = false positive probability
    
- `e` ≈ 2.718 (Euler's number)
    

Trade-off:

- Bigger `m` → fewer false positives.
    
- More `k` → better accuracy, but more time and complexity.
    

---

## 🧠 7. Example with Numbers

Suppose:

- m = 10 bits
    
- k = 3 hash functions
    
- Insert: "apple", "banana"
    

### Inserting "apple":

- h1("apple") = 1, h2 = 4, h3 = 7 → set bit[1], bit[4], bit[7]
    

### Inserting "banana":

- h1("banana") = 2, h2 = 4, h3 = 9 → set bit[2], bit[4], bit[9]
    

Bit array now:

makefile

CopyEdit

`Index:    0 1 2 3 4 5 6 7 8 9 Bits:     0 1 1 0 1 0 0 1 0 1`

### Check for "grape":

- h1("grape") = 1, h2 = 4, h3 = 9 → All bits are 1 → "possibly present" ➝ false positive (if not actually added).




# ✅ Confusion Matrix (Base Concept)

|Actual \ Predicted|Positive|Negative|
|---|---|---|
|**Positive**|**True Positive (TP)**|**False Negative (FN)**|
|**Negative**|**False Positive (FP)**|**True Negative (TN)**|

---

# 1️⃣ True Positive (TP)

### Definition

> **Model says YES and reality is YES**

### Example (Fraud Detection)

- Transaction **is fraud**
    
- System **flags it as fraud**
    

✔ Correct detection

---

# 2️⃣ False Positive (FP) — _False Alarm_

### Definition

> **Model says YES but reality is NO**

### Example (Fraud Detection)

- Transaction **is genuine**
    
- System **flags it as fraud**
    

❌ Wrong → blocks user unnecessarily

📌 **Also called Type-I Error**

---

# 3️⃣ False Negative (FN) — _Missed Detection_

### Definition

> **Model says NO but reality is YES**

### Example (Fraud Detection)

- Transaction **is fraud**
    
- System **does NOT flag it**
    

❌ Dangerous → fraud passes through

📌 **Also called Type-II Error**

---

# 4️⃣ True Negative (TN)

### Definition

> **Model says NO and reality is NO**

### Example

- Transaction **is genuine**
    
- System **does NOT flag it**
    

✔ Correct non-action

The four components are defined as:

- **True Positive (TP):** The model correctly predicted the positive class (both actual and predicted are positive).
- **True Negative (TN):** The model correctly predicted the negative class (both actual and predicted are negative).
- **False Positive (FP):** The model incorrectly predicted the positive class when the actual class was negative (Type I error).
- **False Negative (FN):** The model incorrectly predicted the negative class when the actual class was positive (Type II error).