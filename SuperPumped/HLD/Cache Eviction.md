

# 1. Least Recently Used (LRU)

- LRU evicts the cache entry that hasn't been accessed for the longest time. The fundamental idea is that items that haven't been used recently are less likely to be used in the near future.

### Algorithm Implementation:
```cpp
DATA STRUCTURES:
- HashMap<Key, ListNode>: For O(1) lookups
- DoublyLinkedList: Tracks recency order

ALGORITHM:
1. On get(key):
   - Lookup node in HashMap
   - Remove node from current position
   - Move node to head of list (most recent)
   - Return value

2. On put(key, value):
   - If key exists:
     * Update value
     * Move node to head (like get)
   - If key doesn't exist:
     * If cache full:
       > Remove tail node (least recent)
       > Remove from HashMap
     * Create new node
     * Add to head of list
     * Add to HashMap
```

---
# 2. Least Frequently Used (LFU)

- LFU evicts the item that has been used the least number of times. If multiple items have the same access frequency, the algorithm typically removes the oldest one (least recently used among items with the minimum frequency).

### Algorithm Implementation:
```cpp
DATA STRUCTURES:
- HashMap<Key, Value+Frequency+ListIterator>: For O(1) lookups
- HashMap<Frequency, LinkedList<Keys>>: Groups keys by frequency
- MinFrequency: Tracks minimum frequency for O(1) eviction

ALGORITHM:
1. On get(key):
   - Lookup key info in first HashMap
   - Remove key from its frequency list
   - Increment frequency
   - Add key to new frequency list
   - Update MinFrequency if needed
   - Return value

2. On put(key, value):
   - If key exists:
     * Update value
     * Handle frequency change (like get)
   - If key doesn't exist:
     * If cache full:
       > Evict from MinFrequency list (from back)
     * Add new key with frequency=1
     * Add to frequency=1 list
     * Set MinFrequency=1
```

---
# 3. First-In-First-Out (FIFO)

- FIFO evicts items in the order they were added to the cache, regardless of how often or recently they've been accessed. It functions essentially as a queue, where the oldest entry is always the next to be removed.
### Algorithm Implementation:

```cpp
DATA STRUCTURES:
- HashMap<Key, Value>: For O(1) lookups
- Queue<Keys>: Tracks insertion order

ALGORITHM:
1. On get(key):
   - Simply lookup in HashMap and return value
   - No reordering

2. On put(key, value):
   - If key exists:
     * Just update value in HashMap
   - If key doesn't exist:
     * If cache full:
       > Dequeue oldest key
       > Remove from HashMap
     * Add new key to HashMap
     * Enqueue new key
```

---
# 4. Random Replacement (RR)

- Random Replacement simply selects a random entry to evict when the cache is full. Despite its simplicity, it can work surprisingly well in practice for certain workloads and avoids some edge cases that can affect other algorithms.
### Algorithm Implementation:
```cpp
DATA STRUCTURES:
- HashMap<Key, Value>: For O(1) lookups
- Array<Keys>: For O(1) random access

ALGORITHM:
1. On get(key):
   - Simply lookup in HashMap and return value
   - No reordering

2. On put(key, value):
   - If key exists:
     * Just update value in HashMap
   - If key doesn't exist:
     * If cache full:
       > Generate random index
       > Remove key at that index from HashMap
       > Replace with new key in array
     * Otherwise:
       > Add to array
     * Add to HashMap
```

**Advantages:**
- Extremely simple implementation
- No bookkeeping overhead
- Immune to deliberate cache-thrashing attacks
- Avoids worst-case scenarios that can affect deterministic policies

**Use Cases:**
- Systems where security against cache attacks is important
- When access patterns are unpredictable or unknown
- As a baseline comparison for more complex algorithms
- Systems where implementation simplicity is paramount
---
# 5. Least Recently Used with Second Chance (Clock Algorithm)

- This is a more efficient approximation of LRU. It maintains items in a circular list and uses a "reference bit" for each item. When an item needs to be evicted, the algorithm gives recently accessed items a "second chance" by clearing their reference bit and checking again later.
### How It Works: Step by Step

1. **Initial Setup**:
    - All cache entries are arranged in a circular buffer
    - Each entry has a reference bit initially set to 0 (false)
    - The "clock hand" points to an arbitrary position (often the oldest entry)
2. **When an item is accessed (read or updated)**:
    - Its reference bit is set to 1 (true)
    - This marks the item as "recently used"
    - We don't need to move the item or update any list structure
3. **When we need to evict an item (cache is full and new item needs to be added)**:
    - The clock hand examines the entry it's currently pointing to
    - If the reference bit is 1 (true):
        - This item gets a "second chance"
        - Reset its reference bit to 0 (false)
        - Move the clock hand to the next position
        - Repeat this process (keep giving second chances until finding an entry with bit = 0)
    - If the reference bit is 0 (false):
        - This item hasn't been accessed since its last "second chance"
        - Evict this item and replace it with the new item
        - Set the new item's reference bit to 1 (true)
        - Move the clock hand to the next position
### Algorithm Implementation:
```cpp
DATA STRUCTURES:
- HashMap<Key, ArrayIndex>: Maps keys to positions
- CircularArray<{Key,Value,ReferenceBit}>: Fixed-size circular buffer
- ClockHand: Current position in buffer

ALGORITHM:
1. On get(key):
   - Lookup in HashMap to find position
   - Set ReferenceBit = true
   - Return value

2. On put(key, value):
   - If key exists:
     * Update value
     * Set ReferenceBit = true
   - If key doesn't exist:
     * If cache not full:
       > Add at next position
     * Otherwise:
       > Sweep ClockHand until finding entry with ReferenceBit=false
       > For each position with ReferenceBit=true:
         * Set ReferenceBit=false
         * Advance ClockHand
       > Replace entry at current position
       > Advance ClockHand

```

**Advantages:**
- More efficient than LRU in terms of overhead
- Good approximation of LRU's behavior
- No need for expensive reordering operations
- Works well in operating systems and other environments where full LRU is too costly

**Use Cases:**
- Operating system page replacement
- Disk caches
- Large in-memory caches where full LRU would be too expensive
- Systems where approximate LRU behavior is sufficient
---
# 6. Time-Aware Least Recently Used (TLRU)

**How it works:** TLRU extends LRU by adding a time-to-live (TTL) field to each cache entry. Items can be evicted either when the cache is full (using standard LRU policy) or when they expire according to their TTL.

### Algorithm Implementation:
```cpp
DATA STRUCTURES:
- HashMap<Key, ListNode>: For O(1) lookups
- DoublyLinkedList<{Key,Value,ExpirationTime}>: Tracks recency
- ExpirationHeap (optional): For efficient expiration checks

ALGORITHM:
1. On get(key):
   - Cleanup expired entries (optional)
   - Lookup node in HashMap
   - If expired:
     * Remove and return not found
   - Move to head of list
   - Return value

2. On put(key, value, ttl):
   - Cleanup expired entries (optional)
   - Calculate expiration time
   - If key exists:
     * Update value and expiration
     * Move to head
   - If key doesn't exist:
     * If cache full:
       > Remove tail node
     * Create new node with expiration
     * Add to head and HashMap
```

**Advantages:**
- Combines benefits of LRU with automatic expiration
- Good for caches where data freshness is important
- Addresses common staleness issues in standard LRU
- Can reduce memory pressure by automatically clearing expired items

**Use Cases:**
- Session stores
- Authentication token caches
- Time-sensitive data caches
- Any application where data has a natural expiration time
---
# Visual Reference Table

| Policy                    | Primary Data Structures                              | Core Algorithm                                     | Time Complexity                             |
| ------------------------- | ---------------------------------------------------- | -------------------------------------------------- | ------------------------------------------- |
| **LRU**                   | HashTable + Doubly Linked List                       | Move accessed item to head, evict from tail        | Get/Put: O(1)                               |
| **LFU**                   | HashTable + Min-Heap/Counter Dictionary + Freq Lists | Increment access count, evict minimum frequency    | Get/Put: O(1) with optimized implementation |
| **FIFO**                  | HashTable + Queue                                    | Enqueue on add, dequeue on evict                   | Get/Put: O(1)                               |
| **Random**                | HashTable + Array of Keys                            | Select random index for eviction                   | Get/Put: O(1)                               |
| **Clock (Second Chance)** | HashTable + Circular Array + Reference Bits          | Sweep hand, clear ref bits, evict first unmarked   | Get/Put: O(1) amortized                     |
| **TLRU**                  | HashTable + Doubly Linked List + Timestamp Field     | LRU with expiration checks                         | Get/Put: O(1)                               |
| **ARC**                   | HashTable + 4 Doubly Linked Lists                    | Adapt sizes of recent/frequent lists based on hits | Get/Put: O(1)                               |
|                           |                                                      |                                                    |                                             |

---
