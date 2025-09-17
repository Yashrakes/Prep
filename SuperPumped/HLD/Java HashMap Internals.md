
# Core Data Structure
A HashMap in Java is essentially an array of linked lists (or balanced trees in newer versions), with each list/tree representing a "bucket." Understanding this hybrid structure is fundamental:

```
[ 0: → (key,value) → (key,value) ]
[ 1: → (key,value) ]
[ 2: null ]
[ 3: → (key,value) → (key,value) → (key,value) ]
...
```

## Hashing and Indexing
When you put a key-value pair into a HashMap:
1. The key's `hashCode()` method is called to generate an integer hash code
2. This hash code is transformed (typically with bit manipulation) to reduce clustering
3. The result is modulo-divided by the array length to get the bucket index

This is why objects used as keys should have well-implemented `hashCode()` and `equals()` methods.

---
## Collision Handling

Collisions occur when multiple keys map to the same bucket. Java handles this in two ways:
- For small bucket sizes (≤ 8 elements): A linked list stores entries sequentially
- For larger buckets (> 8 elements): The structure converts to a balanced red-black tree to improve lookup performance from O(n) to O(log n).

This transformation to trees for large buckets was introduced in Java 8 as a significant performance improvement.

---
## Load Factor and Resizing

The load factor (default 0.75) defines when the HashMap resizes:
- Load Factor = Number of entries / Array capacity
- When entries exceed (capacity × load factor), the HashMap resizes to (capacity × 2)
- During resizing, all entries are rehashed and redistributed.

This is where the `tableSizeFor` method you shared comes in—it ensures the capacity is always a power of 2, which makes the modulo operation more efficient through bit manipulation.

---
## Time Complexity
- Average case: O(1) for `get()`, `put()`, `containsKey()`, and `remove()`
- Worst case: O(log n) when many keys collide (in Java 8+)
- Pre-Java 8 worst case: O(n) for collisions

---
## Iterating Order
HashMap makes no guarantees about iteration order, which may change when the map is resized.

---
## Null Handling
HashMap allows one null key (stored in bucket 0) and multiple null values.

---
# Important Implementation Details
1. **Initial Capacity and Growth**: The capacity is always a power of 2, starting at 16 by default. The array doubles in size during rehashing. (here capacity refers to the number of buckets)
2. **The put() Operation**:
    - Calculate the bucket index using the key's hash
    - If bucket is empty, create new node
    - If key already exists, replace the value
    - If collision occurs, append to list or tree
    - Check if resizing is needed
3. **The tableSizeFor Method**: Finds the next power of 2 greater than or equal to a given capacity, ensuring efficient indexing.
4. **Hash Function**: Java doesn't use the raw hashCode but applies additional transformations to improve distribution:
```
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

This XORs the higher bits with lower bits, which helps distribute hash codes more evenly across buckets.

---
## Thread Safety
Standard HashMap is not thread-safe. For concurrent access, you should use ConcurrentHashMap or Collections.synchronizedMap().

---
# Interview Question
## What happens when two different keys produce the same hash code?
- When two different keys produce the same hash code (a hash collision), both entries end up in the same bucket. Java resolves this by storing multiple entries in each bucket:
	- In Java 7 and earlier, each bucket contained a linked list of entries.
	- In Java 8+, each bucket starts as a linked list but converts to a balanced red-black tree when the list exceeds 8 elements (and converts back when it shrinks below 6 elements).
- When retrieving a value, Java first finds the correct bucket using the hash code, then traverses the list/tree within that bucket, using the `equals()` method to find the exact key. This is why both `hashCode()` and `equals()` are important—`hashCode()` gets you to the right bucket, and `equals()` finds the right entry within that bucket.

---
## Explain the role of load factor in a HashMap

- The load factor determines how full the HashMap can get before it resizes. It's defined as:
- Load Factor = Number of Entries / Current Capacity
- Java's default load factor is 0.75, meaning when the HashMap is 75% full, it will resize. The load factor represents a trade-off:
	- A lower load factor (like 0.5) means more empty space but fewer collisions and faster operations.
	- A higher load factor (like 0.9) means less wasted space but more collisions and potentially slower operations.
- When the number of entries exceeds (capacity × load factor), the HashMap doubles its capacity and rehashes all existing entries into new buckets. This resizing operation is expensive (O(n)), but it keeps subsequent operations fast by reducing collisions.

----
## Why must objects used as HashMap keys implement hashCode() and equals()?

- Objects used as HashMap keys must properly implement both methods because:
	- **hashCode()** determines which bucket to use. Without a good implementation, you might get poor distribution (many keys in the same bucket) or inconsistent results (same key maps to different buckets).
	- **equals()** identifies the exact key within a bucket. Without proper equals(), Java can't tell if an existing key matches the one you're looking for.
- Most importantly, these two methods must be consistent with each other:
	- If `a.equals(b)` is true, then `a.hashCode()` must equal `b.hashCode()`
	- If `a.hashCode()` equals `b.hashCode()`, `a.equals(b)` may be true or false (collision)
- If you override one, you almost always need to override the other. Using an object with inconsistent implementations as a key will cause HashMap to malfunction—you might put a value with one key and never be able to retrieve it.

---
## What time complexity guarantees does HashMap provide?
- HashMap offers these time complexity guarantees:
	- **Best/Average case**: O(1) for get(), put(), containsKey(), and remove()
	- **Worst case (Java 7 and earlier)**: O(n) when many keys hash to the same bucket
	- **Worst case (Java 8+)**: O(log n) when many keys hash to the same bucket (due to red-black trees)
- The worst case occurs when many keys have the same hash code or when the hash function distributes keys poorly. In practice, with good key objects and hash functions, operations typically stay close to O(1).
- Other operations have different complexities:
	- containsValue(): O(n) as it must scan all entries
	- clear(): O(capacity + size) as it must clear all buckets and entries
	- keySet(), values(), entrySet(): O(capacity + size) to create the view collections.

---
## How does a HashMap handle null keys?
- HashMap explicitly allows one null key. Here's how it's handled:
	- When you put a null key, its hash value is treated as 0, placing it in bucket 0.
	- During retrieval with get(null), HashMap looks for an entry with a null key in bucket 0.
	- If multiple entries hash to bucket 0, the null key entry is distinguished by checking key equality.
- This special treatment of null keys is coded directly into the HashMap implementation, with specific checks for null at the beginning of most operations.

---
## What's the difference between HashMap and Hashtable?

The key differences between HashMap and Hashtable are:
1. **Thread safety**: Hashtable is synchronized (thread-safe), while HashMap is not. This makes HashMap faster in single-threaded environments.
2. **Null handling**: HashMap allows one null key and any number of null values. Hashtable prohibits both null keys and null values.
3. **Iteration order**: Both make no guarantees about iteration order, but they use different internal structures.
4. **Performance**: HashMap is generally faster in single-threaded applications due to lack of synchronization overhead.
5. **Heritage**: Hashtable is a legacy class dating from Java 1.0, while HashMap was introduced in Java 1.2 with the Collections Framework.
6. **Modern alternatives**: For thread-safe needs, ConcurrentHashMap is preferred over Hashtable, as it offers better concurrency by locking at a finer granularity.

---
## Why is HashMap's capacity always a power of 2?

HashMap's capacity is always a power of 2 for several reasons:
1. **Efficient modulo operations**: When computing the bucket index, Java needs to perform a modulo operation (hash % capacity). Using powers of 2 allows replacing this with a faster bitwise AND operation: (capacity-1) & hash.
2. **Better distribution with bit manipulation**: The hash spreading function can be designed to work optimally with power-of-2 sizes, allowing effective bit mixing.
3. **Simpler resizing**: When doubling the capacity, bit patterns remain relevant, simplifying the rehashing process.
4. **Consistent with binary computation patterns**: Powers of 2 align well with how computers represent and manipulate data at the binary level.

When you specify a non-power-of-2 capacity, the `tableSizeFor` method we discussed earlier rounds it up to the next power of 2. This ensures all the benefits above while allowing users to specify approximate capacities.

---
