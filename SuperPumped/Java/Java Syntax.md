# Comprehensive Java Data Structures and Operations Guide

## 1. String Operations

### String Buffer and StringBuilder

Both `StringBuffer` and `StringBuilder` are mutable sequences of characters, unlike `String` which is immutable.

- `StringBuffer`: Thread-safe (synchronized), slower
- `StringBuilder`: Not thread-safe, faster

```java
// Initialization
StringBuffer sb1 = new StringBuffer();          // Empty buffer with default capacity (16)
StringBuffer sb2 = new StringBuffer(20);        // Empty buffer with capacity 20
StringBuffer sb3 = new StringBuffer("Hello");   // Buffer containing "Hello"

StringBuilder sb4 = new StringBuilder();        // Same initialization methods as StringBuffer
StringBuilder sb5 = new StringBuilder(20);
StringBuilder sb6 = new StringBuilder("Hello");

// Common operations
sb1.append(" World");        // Appends text to the buffer
sb1.insert(5, "Beautiful "); // Inserts text at specified position
sb1.delete(5, 10);           // Deletes text from start to end position (exclusive)
sb1.replace(0, 5, "Hi");     // Replaces text from start to end position (exclusive)
sb1.reverse();               // Reverses the characters in the buffer
char c = sb1.charAt(2);      // Gets the character at specified position
sb1.setCharAt(0, 'h');       // Sets character at specified position
int len = sb1.length();      // Gets the length of buffer
String str = sb1.toString(); // Converts buffer to String
```

**Time Complexity**: Most operations are O(n) where n is the length of the string, but `append()` is amortized O(1).

### String to Char and Char to String

```java
// String to char array
String str = "Hello";
char[] charArray = str.toCharArray();  // ['H', 'e', 'l', 'l', 'o']

// Character at specific position
char c = str.charAt(0);  // 'H'

// Char to String
char ch = 'A';
String str1 = Character.toString(ch);  // "A"
String str2 = String.valueOf(ch);      // "A"
String str3 = "" + ch;                 // "A"

// Char array to String
char[] chars = {'H', 'e', 'l', 'l', 'o'};
String str4 = new String(chars);        // "Hello"
String str5 = String.valueOf(chars);    // "Hello"
```

### String Concatenation

```java
// Using + operator (creates new String objects)
String s1 = "Hello";
String s2 = "World";
String s3 = s1 + " " + s2;  // "Hello World"

// Using concat method (creates new String object)
String s4 = s1.concat(" ").concat(s2);  // "Hello World"

// Using StringBuilder/StringBuffer (more efficient for multiple operations)
StringBuilder sb = new StringBuilder();
sb.append(s1).append(" ").append(s2);
String s5 = sb.toString();  // "Hello World"

// Using String.join (Java 8+)
String s6 = String.join(" ", s1, s2);  // "Hello World"
```

### String Modification

Strings are immutable in Java, so "modification" actually creates new strings:

```java
String original = "Hello World";

// Substring
String sub = original.substring(0, 5);  // "Hello"

// Replace
String replaced = original.replace('l', 'L');  // "HeLLo WorLd"
String replacedStr = original.replace("Hello", "Hi");  // "Hi World"
String replacedRegex = original.replaceAll("l", "L");  // "HeLLo WorLd"

// Trim
String spacedStr = "  Hello  ";
String trimmed = spacedStr.trim();  // "Hello"

// Case conversion
String upper = original.toUpperCase();  // "HELLO WORLD"
String lower = original.toLowerCase();  // "hello world"

// Split
String[] parts = original.split(" ");  // ["Hello", "World"]
```

### String Sort / Reverse

```java
// Reverse using StringBuilder
String str = "abcdef";
String reversed = new StringBuilder(str).reverse().toString();  // "fedcba"

// Sort characters in a string
char[] chars = str.toCharArray();
Arrays.sort(chars);
String sorted = new String(chars);  // "abcdef" (already sorted)
```

**Time Complexity**: String operations that create new strings typically run in O(n) time where n is the string length.

## 2. Stack Operations

A stack is a Last-In-First-Out (LIFO) data structure.

```java
// Initialization
Stack<Integer> stack = new Stack<>();

// Push (add element to top)
stack.push(1);
stack.push(2);
stack.push(3);  // Stack: [1, 2, 3]

// Pop (remove and return top element)
int top = stack.pop();  // top = 3, Stack: [1, 2]

// Peek (view top element without removing)
int peek = stack.peek();  // peek = 2, Stack: [1, 2]

// Check if empty
boolean isEmpty = stack.empty();  // false

// Search (returns 1-based position from top, or -1 if not found)
int position = stack.search(1);  // 2 (second from the top)

// Size
int size = stack.size();  // 2

// Iteration
for (Integer element : stack) {
    System.out.println(element);  // Prints from bottom to top: 1, 2
}

// Note: The legacy Stack class extends Vector and is not recommended for new code
// Preferred alternative using Deque:
Deque<Integer> stack2 = new ArrayDeque<>();
stack2.push(1);
stack2.push(2);
int top2 = stack2.pop();
```

**Time Complexity**:
- push(): O(1)
- pop(): O(1)
- peek(): O(1)
- search(): O(n)
- size(): O(1)

## 3. Queue Operations

### FIFO Queue

```java
// Initialization using LinkedList (implements Queue interface)
Queue<String> queue = new LinkedList<>();

// Using ArrayDeque (more efficient implementation)
Queue<String> queue2 = new ArrayDeque<>();

// add() - throws exception if no space
queue.add("a");
queue.add("b");
queue.add("c");  // Queue: [a, b, c]

// offer() - returns false if no space (preferred method)
boolean success = queue.offer("d");  // Queue: [a, b, c, d]

// remove() - throws exception if queue is empty
String first = queue.remove();  // first = "a", Queue: [b, c, d]

// poll() - returns null if queue is empty (preferred method)
String next = queue.poll();  // next = "b", Queue: [c, d]

// element() - throws exception if queue is empty
String head = queue.element();  // head = "c", Queue: [c, d]

// peek() - returns null if queue is empty (preferred method)
String headPeek = queue.peek();  // headPeek = "c", Queue: [c, d]

// Size
int size = queue.size();  // 2

// Check if empty
boolean isEmpty = queue.isEmpty();  // false

// Iteration
for (String element : queue) {
    System.out.println(element);  // Prints: c, d
}
```

### Double-ended Queue (Deque)

```java
// Initialization
Deque<Integer> deque = new ArrayDeque<>();
Deque<Integer> deque2 = new LinkedList<>();

// Adding elements
deque.addFirst(1);    // Add to front
deque.offerFirst(2);  // Add to front (preferred, returns false if full)
deque.addLast(3);     // Add to end
deque.offerLast(4);   // Add to end (preferred, returns false if full)
// Deque: [2, 1, 3, 4]

// Removing elements
int first = deque.removeFirst();    // first = 2, throws exception if empty
int first2 = deque.pollFirst();     // first2 = 1, returns null if empty (preferred)
int last = deque.removeLast();      // last = 4, throws exception if empty
int last2 = deque.pollLast();       // last2 = 3, returns null if empty (preferred)
// Deque is now empty

// Examining elements (without removing)
deque.offerLast(10);
deque.offerLast(20);
int peek1 = deque.getFirst();       // peek1 = 10, throws exception if empty
int peek2 = deque.peekFirst();      // peek2 = 10, returns null if empty (preferred)
int peek3 = deque.getLast();        // peek3 = 20, throws exception if empty
int peek4 = deque.peekLast();       // peek4 = 20, returns null if empty (preferred)
```

**Time Complexity** for Queue/Deque operations:
- add/offer/push: O(1)
- remove/poll/pop: O(1)
- peek/element: O(1)

## 4. Priority Queue

A PriorityQueue maintains elements according to their natural ordering or by a Comparator.

```java
// Initialization with natural ordering (min-heap by default)
PriorityQueue<Integer> minPq = new PriorityQueue<>();

// Initialization with explicit capacity
PriorityQueue<Integer> pq2 = new PriorityQueue<>(10);

// Initialization with custom comparator (max-heap)
PriorityQueue<Integer> maxPq = new PriorityQueue<>(Comparator.reverseOrder());

// Initialization with custom comparator for objects
PriorityQueue<Student> studentPq = new PriorityQueue<>((s1, s2) -> 
    s1.getGpa().compareTo(s2.getGpa()));

// Adding elements
minPq.add(3);       // throws exception if queue is full
minPq.offer(1);     // returns false if queue is full (preferred method)
minPq.add(2);
// PQ: [1, 3, 2] (internal structure, but logically organized as [1, 2, 3])

// Removing elements
int min = minPq.remove();  // min = 1, throws exception if empty
int next = minPq.poll();   // next = 2, returns null if empty (preferred method)
// PQ: [3]

// Examining the head element
minPq.add(5);
minPq.add(4);
int head = minPq.element();  // head = 3, throws exception if empty
int peek = minPq.peek();     // peek = 3, returns null if empty (preferred method)

// Size and emptiness
int size = minPq.size();     // 3
boolean isEmpty = minPq.isEmpty();  // false

// Iteration
// Note: Iteration order is NOT guaranteed to be in priority order
for (Integer element : minPq) {
    System.out.println(element);  // Order is not guaranteed
}

// To process elements in priority order, repeatedly poll the queue
while (!minPq.isEmpty()) {
    System.out.println(minPq.poll());  // Will print in sorted order: 3, 4, 5
}

// Custom comparator example for a Student class
class Student {
    private String name;
    private Double gpa;
    
    // Constructor, getters, setters...
    
    public Double getGpa() {
        return gpa;
    }
}

// Create PQ with custom comparator using lambda
PriorityQueue<Student> byGpaDesc = new PriorityQueue<>(
    (s1, s2) -> s2.getGpa().compareTo(s1.getGpa())  // High GPA first
);

// Alternative using Comparator methods
PriorityQueue<Student> byGpa = new PriorityQueue<>(
    Comparator.comparing(Student::getGpa)  // Low GPA first
);
```

**Time Complexity**:
- offer/add: O(log n)
- poll/remove: O(log n)
- peek/element: O(1)
- size/isEmpty: O(1)

**When to use offer vs add**:
- `offer()`: Returns false if element cannot be inserted due to capacity restrictions
- `add()`: Throws an exception if element cannot be inserted due to capacity restrictions
- Best practice: Use `offer()`, `poll()`, and `peek()` as they handle edge cases gracefully

## 5. Map Types and Operations

### HashMap

```java
// Initialization
HashMap<String, Integer> map = new HashMap<>();
HashMap<String, Integer> map2 = new HashMap<>(100);  // Initial capacity
HashMap<String, Integer> map3 = new HashMap<>(100, 0.75f);  // Load factor

// Adding/updating entries
map.put("apple", 10);
map.put("banana", 20);
map.put("cherry", 30);  // Map: {apple=10, banana=20, cherry=30}

// Accessing values
int value = map.get("apple");  // value = 10
Integer nullableValue = map.get("unknown");  // nullableValue = null

// Checking for keys and values
boolean hasKey = map.containsKey("apple");  // true
boolean hasValue = map.containsValue(20);  // true

// Getting default value if key not present
int defaultValue = map.getOrDefault("unknown", 0);  // defaultValue = 0

// Removing entries
int removed = map.remove("banana");  // removed = 20, Map: {apple=10, cherry=30}

// Size and emptiness
int size = map.size();  // 2
boolean isEmpty = map.isEmpty();  // false

// Clearing the map
map.clear();  // Map: {}

// Iterating over a map
map.put("a", 1);
map.put("b", 2);
map.put("c", 3);

// Iterate over entries
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}

// Iterate over keys
for (String key : map.keySet()) {
    System.out.println(key);
}

// Iterate over values
for (Integer value2 : map.values()) {
    System.out.println(value2);
}

// Java 8+ forEach
map.forEach((k, v) -> System.out.println(k + ": " + v));

// Putting if absent (only if key not present)
map.putIfAbsent("a", 10);  // No change as "a" exists
map.putIfAbsent("d", 4);   // Adds {d=4}

// Compute and merge operations
map.compute("a", (k, v) -> v + 10);  // Updates "a" to 11
map.computeIfPresent("b", (k, v) -> v * 2);  // Updates "b" to 4
map.computeIfAbsent("e", k -> 5);  // Adds {e=5}
map.merge("a", 10, Integer::sum);  // Updates "a" to 21
```

### TreeMap (Sorted Map)

```java
// Initialization with natural ordering
TreeMap<String, Integer> treeMap = new TreeMap<>();

// Initialization with custom comparator
TreeMap<String, Integer> customTreeMap = new TreeMap<>(
    Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder())
);

// Basic operations (same as HashMap)
treeMap.put("c", 3);
treeMap.put("a", 1);
treeMap.put("b", 2);
// TreeMap is sorted by keys: {a=1, b=2, c=3}

// TreeMap-specific operations
String firstKey = treeMap.firstKey();  // "a"
String lastKey = treeMap.lastKey();    // "c"

// Navigation operations
String ceiling = treeMap.ceilingKey("b");  // Key >= "b": "b"
String higher = treeMap.higherKey("b");    // Key > "b": "c"
String floor = treeMap.floorKey("b");      // Key <= "b": "b"
String lower = treeMap.lowerKey("b");      // Key < "b": "a"

// Submaps
SortedMap<String, Integer> headMap = treeMap.headMap("b");  // Keys < "b": {a=1}
SortedMap<String, Integer> tailMap = treeMap.tailMap("b");  // Keys >= "b": {b=2, c=3}
SortedMap<String, Integer> subMap = treeMap.subMap("a", "c");  // Keys >= "a" and < "c": {a=1, b=2}
```

### LinkedHashMap (Preserves Insertion Order)

```java
// Initialization
LinkedHashMap<String, Integer> linkedMap = new LinkedHashMap<>();

// Initialization with access-order (for LRU cache-like behavior)
LinkedHashMap<String, Integer> accessOrderMap = new LinkedHashMap<>(
    16, 0.75f, true  // Initial capacity, load factor, access-order
);

// Basic operations (same as HashMap but maintains insertion order)
linkedMap.put("c", 3);
linkedMap.put("a", 1);
linkedMap.put("b", 2);
// LinkedHashMap: {c=3, a=1, b=2} (insertion order)

// Iterating will follow insertion order
for (Map.Entry<String, Integer> entry : linkedMap.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
    // Prints in insertion order: c: 3, a: 1, b: 2
}

// Access-order example (least recently used entries first)
accessOrderMap.put("a", 1);
accessOrderMap.put("b", 2);
accessOrderMap.put("c", 3);
accessOrderMap.get("a");  // Accessing "a"
// accessOrderMap order is now: {b=2, c=3, a=1} (least to most recently accessed)
```

**Time Complexity** for common Map operations:

| Operation | HashMap | TreeMap | LinkedHashMap |
|-----------|---------|---------|---------------|
| get       | O(1)    | O(log n)| O(1)          |
| put       | O(1)    | O(log n)| O(1)          |
| containsKey| O(1)   | O(log n)| O(1)          |
| remove    | O(1)    | O(log n)| O(1)          |
| iteration | O(n)    | O(n)    | O(n)          |

## 6. Set Operations

Sets store unique elements with no duplicates.

### HashSet

```java
// Initialization
HashSet<String> set = new HashSet<>();
HashSet<String> set2 = new HashSet<>(100);  // Initial capacity
HashSet<String> set3 = new HashSet<>(Arrays.asList("a", "b", "c"));  // From collection

// Adding elements
set.add("apple");
set.add("banana");
set.add("apple");  // Duplicate ignored
// Set: [banana, apple]

// Checking for presence
boolean contains = set.contains("apple");  // true

// Removing elements
boolean removed = set.remove("banana");  // true, Set: [apple]

// Size and emptiness
int size = set.size();  // 1
boolean isEmpty = set.isEmpty();  // false

// Clearing the set
set.clear();  // Set: []

// Iteration
set.add("a");
set.add("b");
set.add("c");

for (String element : set) {
    System.out.println(element);  // Order not guaranteed in HashSet
}

// Set operations
HashSet<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
HashSet<String> set2b = new HashSet<>(Arrays.asList("b", "c", "d"));

// Union
HashSet<String> union = new HashSet<>(set1);
union.addAll(set2b);  // union = [a, b, c, d]

// Intersection
HashSet<String> intersection = new HashSet<>(set1);
intersection.retainAll(set2b);  // intersection = [b, c]

// Difference (set1 - set2)
HashSet<String> difference = new HashSet<>(set1);
difference.removeAll(set2b);  // difference = [a]

// Symmetric difference (elements in either set but not in both)
HashSet<String> symmetricDiff = new HashSet<>(set1);
symmetricDiff.addAll(set2b);
HashSet<String> temp = new HashSet<>(set1);
temp.retainAll(set2b);
symmetricDiff.removeAll(temp);  // symmetricDiff = [a, d]

// Check if a set is subset of another
boolean isSubset = set2b.containsAll(intersection);  // true
```

### TreeSet (Sorted Set)

```java
// Initialization with natural ordering
TreeSet<String> treeSet = new TreeSet<>();

// Initialization with custom comparator
TreeSet<String> customTreeSet = new TreeSet<>(
    Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder())
);

// Initialization from collection
TreeSet<String> treeSet2 = new TreeSet<>(Arrays.asList("c", "a", "b"));

// Basic operations (similar to HashSet but maintains sorted order)
treeSet.add("c");
treeSet.add("a");
treeSet.add("b");
// TreeSet: [a, b, c] (natural ordering)

// TreeSet-specific operations
String first = treeSet.first();  // "a"
String last = treeSet.last();    // "c"

// Navigation operations
String ceiling = treeSet.ceiling("b");  // Element >= "b": "b"
String higher = treeSet.higher("b");    // Element > "b": "c"
String floor = treeSet.floor("b");      // Element <= "b": "b"
String lower = treeSet.lower("b");      // Element < "b": "a"

// Subsets
SortedSet<String> headSet = treeSet.headSet("b");  // Elements < "b": [a]
SortedSet<String> tailSet = treeSet.tailSet("b");  // Elements >= "b": [b, c]
SortedSet<String> subSet = treeSet.subSet("a", "c");  // Elements >= "a" and < "c": [a, b]

// Iteration (always in sorted order)
for (String element : treeSet) {
    System.out.println(element);  // Prints: a, b, c
}
```

### LinkedHashSet (Preserves Insertion Order)

```java
// Initialization
LinkedHashSet<String> linkedSet = new LinkedHashSet<>();
LinkedHashSet<String> linkedSet2 = new LinkedHashSet<>(Arrays.asList("c", "a", "b"));

// Basic operations (similar to HashSet but maintains insertion order)
linkedSet.add("c");
linkedSet.add("a");
linkedSet.add("b");
// LinkedHashSet: [c, a, b] (insertion order)

// Iteration (follows insertion order)
for (String element : linkedSet) {
    System.out.println(element);  // Prints: c, a, b
}
```

**Time Complexity** for common Set operations:

| Operation | HashSet | TreeSet | LinkedHashSet |
|-----------|---------|---------|---------------|
| add       | O(1)    | O(log n)| O(1)          |
| contains  | O(1)    | O(log n)| O(1)          |
| remove    | O(1)    | O(log n)| O(1)          |
| iteration | O(n)    | O(n)    | O(n)          |

## 7. LinkedList Operations

LinkedList implements both List and Deque interfaces, offering functionality of both a list and a double-ended queue.

```java
// Initialization
LinkedList<String> list = new LinkedList<>();
LinkedList<String> list2 = new LinkedList<>(Arrays.asList("a", "b", "c"));

// Adding elements
list.add("a");                 // Adds to the end
list.add(0, "b");              // Adds at specified index
list.addFirst("c");            // Adds at the beginning
list.addLast("d");             // Adds at the end
// List: [c, b, a, d]

// Accessing elements
String first = list.getFirst();  // "c"
String last = list.getLast();    // "d"
String element = list.get(1);    // "b"

// Updating elements
list.set(1, "x");  // Replaces element at index 1
// List: [c, x, a, d]

// Removing elements
String removed1 = list.removeFirst();  // removed1 = "c", List: [x, a, d]
String removed2 = list.removeLast();   // removed2 = "d", List: [x, a]
String removed3 = list.remove(0);      // removed3 = "x", List: [a]
boolean removed4 = list.remove("a");   // removed4 = true, List: []

// Size and checking
list.add("b");
int size = list.size();              // 1
boolean isEmpty = list.isEmpty();    // false
boolean contains = list.contains("b");  // true

// Clear
list.clear();  // List: []

// Queue and stack operations
list.push("a");      // Same as addFirst(), List: [a]
list.push("b");      // List: [b, a]
String popped = list.pop();  // popped = "b", List: [a]

list.offer("c");     // Same as add() or addLast(), List: [a, c]
String polled = list.poll();  // polled = "a", List: [c]
String peeked = list.peek();  // peeked = "c", List: [c]

// Iteration
list = new LinkedList<>(Arrays.asList("a", "b", "c"));
for (String element2 : list) {
    System.out.println(element2);  // Prints: a, b, c
}

// Iterator with modification
Iterator<String> iterator = list.iterator();
while (iterator.hasNext()) {
    String value = iterator.next();
    if (value.equals("b")) {
        iterator.remove();  // Removes current element safely
    }
}
// List: [a, c]

// Using ListIterator (can move backwards and modify list)
ListIterator<String> listIterator = list.listIterator();
while (listIterator.hasNext()) {
    int index = listIterator.nextIndex();
    String value = listIterator.next();
    if (index == 0) {
        listIterator.add("x");  // Add after current position
    }
}
// List: [a, x, c]
```

**Time Complexity**:
- get(index) / set(index): O(n) (need to traverse to that position)
- add(element) / addLast() / offer(): O(1)
- add(index, element): O(n) (need to traverse to that position)
- addFirst() / push(): O(1)
- remove(index) / remove(element): O(n)
- removeFirst() / pop(): O(1)
- removeLast(): O(1)
- contains(): O(n)
- size(): O(1)

## 8. Collections Utility Methods

```java
List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5, 9));

// Sorting
Collections.sort(list);  // [1, 1, 3, 4, 5, 9]

// Custom sort using comparator
Collections.sort(list, Comparator.reverseOrder());  // [9, 5, 4, 3, 1, 1]

// Binary search (requires sorted list)
Collections.sort(list);  // [1, 1, 3, 4, 5, 9]
int index = Collections.binarySearch(list, 4);  // index = 3

// Reversing
Collections.reverse(list);  // [9, 5, 4, 3, 1, 1]

// Shuffling
Collections.shuffle(list);  // Random order

// Finding min/max
int min = Collections.min(list);  // 1
int max = Collections.max(list);  // 9

// Frequency (count occurrences)
int freq = Collections.frequency(list, 1);  // 2

// Finding subList
int indexOfSubList = Collections.indexOfSubList(list, Arrays.asList(4, 3));  // Returns starting index or -1

// Fill all elements with a value
Collections.fill(list, 0);  // All elements become 0

// Copy a list (destination must be at least as large as source)
List<Integer> dest = new ArrayList<>(Collections.nCopies(list.size(), 0));
Collections.copy(dest, list);

// Disjoint (check if two collections have no elements in common)
boolean disjoint = Collections.disjoint(list, Arrays.asList(10, 20));  // true

// Unmodifiable wrappers
List<Integer> unmodifiableList = Collections.unmodifiableList(list);

// Synchronized wrappers (thread-safe)
List<Integer> synchronizedList = Collections.synchronizedList(list);
```

## 9. Binary Search Operations

```java
// Binary search on a sorted array
int[] array = {1, 3, 5, 7, 9, 11};
int index = Arrays.binarySearch(array, 5);  // index = 2
int notFound = Arrays.binarySearch(array, 6);  // negative value (-insertion_point - 1)

// Binary search on a sorted list
List<Integer> sortedList = Arrays.asList(1, 3, 5, 7, 9, 11);
int listIndex = Collections.binarySearch(sortedList, 5);  // listIndex = 2

// Binary search with comparator (for custom objects or different order)
List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David");
int nameIndex = Collections.binarySearch(
    names, 
    "Charlie", 
    String.CASE_INSENSITIVE_ORDER
);  // nameIndex = 2

// Manual binary search implementation
public static int binarySearch(int[] arr, int target) {
    int left = 0;
    int right = arr.length - 1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;  // Avoids integer overflow
        
        if (arr[mid] == target) return mid;
        
        if (arr[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    
    return -left - 1;  // Returns -(insertion point) - 1
}
```

**Time Complexity**:
- Binary search: O(log n)

## 10. ArrayList/Array Operations

```java
// ArrayList initialization
ArrayList<String> list = new ArrayList<>();
ArrayList<String> list2 = new ArrayList<>(10);  // With initial capacity
ArrayList<String> list3 = new ArrayList<>(Arrays.asList("a", "b", "c"));  // From collection

// Basic operations
list.add("a");                 // Add to end
list.add(0, "b");              // Add at index
list.set(0, "c");              // Replace element at index
String element = list.get(0);  // Access by index
list.remove(0);                // Remove by index
list.remove("a");              // Remove by value
boolean contains = list.contains("a");  // Check if contains
int size = list.size();        // Get size
boolean isEmpty = list.isEmpty();  // Check if empty
list.clear();                  // Remove all elements

// Bulk operations
list.addAll(Arrays.asList("d", "e", "f"));  // Add all from collection
list.removeAll(Arrays.asList("d", "f"));    // Remove all in collection
list.retainAll(Arrays.asList("e"));         // Keep only elements in collection

// Sublist (view of portion of list)
List<String> subList = list.subList(0, 1);  // [e]

// Array to List conversion
String[] array = {"x", "y", "z"};
List<String> arrayAsList = Arrays.asList(array);  // Fixed-size view of array
ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(array));  // New ArrayList

// List to Array conversion
String[] toArray = list.toArray(new String[0]);  // Preferred since Java 8
String[] sizedArray = list.toArray(new String[list.size()]);  // Alternative

// Sorting ArrayList
ArrayList<Integer> nums = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5, 9));
Collections.sort(nums);  // Natural order: [1, 1, 3, 4, 5, 9]
Collections.sort(nums, Collections.reverseOrder());  // Reverse: [9, 5, 4, 3, 1, 1]

// Custom sort with Comparator
class Person {
    String name;
    int age;
    
    Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() { return name; }
    public int getAge() { return age; }
}

ArrayList<Person> people = new ArrayList<>();
people.add(new Person("Alice", 30));
people.add(new Person("Bob", 25));
people.add(new Person("Charlie", 35));

// Sort by age (ascending)
Collections.sort(people, Comparator.comparing(Person::getAge));

// Sort by age (descending)
Collections.sort(people, Comparator.comparing(Person::getAge).reversed());

// Multiple criteria sort (by name, then by age)
Collections.sort(people, 
    Comparator.comparing(Person::getName)
              .thenComparing(Person::getAge)
);

// Finding maximum element
Integer max = Collections.max(nums);  // 9

// Finding maximum using custom comparator
Person oldest = Collections.max(people, Comparator.comparing(Person::getAge));

// Creating and pre-filling List/ArrayList
List<Integer> zeros = Collections.nCopies(5, 0);  // [0, 0, 0, 0, 0] (immutable)
ArrayList<Integer> zeroList = new ArrayList<>(Collections.nCopies(5, 0));  // Mutable

// Arrays.fill for arrays
int[] intArray = new int[5];
Arrays.fill(intArray, 10);  // [10, 10, 10, 10, 10]

// Partial fill
int[] partialFill = new int[5];
Arrays.fill(partialFill, 1, 4, 20);  // [0, 20, 20, 20, 0]

// Stream operations (Java 8+)
List<Integer> evenNumbers = nums.stream()
                                .filter(n -> n % 2 == 0)
                                .collect(Collectors.toList());

List<Integer> doubled = nums.stream()
                           .map(n -> n * 2)
                           .collect(Collectors.toList());

// Arrays operations
int[] numbers = {3, 1, 4, 1, 5, 9};

// Sorting arrays
Arrays.sort(numbers);  // [1, 1, 3, 4, 5, 9]

// Sorting part of an array
Arrays.sort(numbers, 0, 3);  // Sort elements at index 0, 1, 2

// Sort array of objects with comparator
Person[] peopleArray = people.toArray(new Person[0]);
Arrays.sort(peopleArray, Comparator.comparing(Person::getAge));

// Binary search (array must be sorted)
Arrays.sort(numbers);
int index = Arrays.binarySearch(numbers, 4);  // index = 3

// Equals and hashCode
boolean arraysEqual = Arrays.equals(new int[]{1, 2, 3}, new int[]{1, 2, 3});  // true

// Deep equals (for multi-dimensional arrays)
boolean deepEqual = Arrays.deepEquals(
    new int[][]{{1, 2}, {3, 4}}, 
    new int[][]{{1, 2}, {3, 4}}
);  // true

// Creating a copy
int[] copy = Arrays.copyOf(numbers, numbers.length);
int[] partialCopy = Arrays.copyOfRange(numbers, 1, 4);  // Elements at index 1, 2, 3

// Creating String representation
String arrayStr = Arrays.toString(numbers);  // "[1, 1, 3, 4, 5, 9]"
String multiArrayStr = Arrays.deepToString(new int[][]{{1, 2}, {3, 4}});  // "[[1, 2], [3, 4]]"

// Parallel sort (Java 8+, uses multiple threads for large arrays)
Arrays.parallelSort(numbers);

// Arrays as stream (Java 8+)
int sum = Arrays.stream(numbers).sum();  // 23

// Array comparison with comparator
Integer[] boxedNums = {3, 1, 4, 1, 5, 9};
Arrays.sort(boxedNums, Comparator.reverseOrder());  // [9, 5, 4, 3, 1, 1]

// Finding min/max in array
int min = Arrays.stream(numbers).min().getAsInt();  // 1
int max = Arrays.stream(numbers).max().getAsInt();  // 9

// 2D arrays
int[][] matrix = new int[3][3];
matrix[0][0] = 1;
matrix[1][1] = 5;
matrix[2][2] = 9;

// Iterating over 2D array
for (int i = 0; i < matrix.length; i++) {
    for (int j = 0; j < matrix[i].length; j++) {
        System.out.print(matrix[i][j] + " ");
    }
    System.out.println();
}

// Enhanced for loop for 2D array
for (int[] row : matrix) {
    for (int value : row) {
        System.out.print(value + " ");
    }
    System.out.println();
}

```

## 11. Additional Important Topics

### 11.1 Iterator and ListIterator

```java
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));

// Basic Iterator
Iterator<String> iterator = list.iterator();
while (iterator.hasNext()) {
    String element = iterator.next();
    if (element.equals("b")) {
        iterator.remove();  // Safe way to remove during iteration
    }
}
// list: [a, c, d]

// WARNING: Don't modify collection during iteration without using iterator methods
// This will throw ConcurrentModificationException:
// for (String s : list) {
//     if (s.equals("c")) list.remove(s);  // DON'T DO THIS
// }

// ListIterator (bidirectional, only for Lists)
ListIterator<String> listIterator = list.listIterator();
while (listIterator.hasNext()) {
    int index = listIterator.nextIndex();
    String element = listIterator.next();
    if (index == 1) {
        listIterator.add("b");  // Add after current position
    }
}
// list: [a, c, b, d]

// Backward iteration with ListIterator
listIterator = list.listIterator(list.size());  // Start at the end
while (listIterator.hasPrevious()) {
    String element = listIterator.previous();
    System.out.print(element + " ");  // Prints: d b c a
}
```

### 11.2 Comparable and Comparator

```java
// Comparable interface (natural ordering)
class Student implements Comparable<Student> {
    private String name;
    private int age;
    
    // Constructor, getters, setters...
    
    @Override
    public int compareTo(Student other) {
        // Sort by age ascending
        return Integer.compare(this.age, other.age);
    }
}

// Using natural ordering
List<Student> students = new ArrayList<>();
Collections.sort(students);  // Uses compareTo method

// Comparator interface (custom ordering)
Comparator<Student> byNameThenAge = Comparator
    .comparing(Student::getName)
    .thenComparing(Student::getAge);

// Anonymous comparator class (pre-Java 8)
Comparator<Student> byAgeThenName = new Comparator<Student>() {
    @Override
    public int compare(Student s1, Student s2) {
        int ageCompare = Integer.compare(s1.getAge(), s2.getAge());
        if (ageCompare != 0) return ageCompare;
        return s1.getName().compareTo(s2.getName());
    }
};

// Using custom comparator
Collections.sort(students, byNameThenAge);
```

### 11.3 Streams API (Java 8+)

```java
List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David", "Eva");

// Filtering
List<String> filtered = names.stream()
    .filter(name -> name.length() > 4)
    .collect(Collectors.toList());  // [Alice, Charlie, David]

// Mapping
List<Integer> lengths = names.stream()
    .map(String::length)
    .collect(Collectors.toList());  // [5, 3, 7, 5, 3]

// Flat mapping (for nested collections)
List<List<Integer>> nestedList = Arrays.asList(
    Arrays.asList(1, 2), 
    Arrays.asList(3, 4)
);
List<Integer> flatList = nestedList.stream()
    .flatMap(Collection::stream)
    .collect(Collectors.toList());  // [1, 2, 3, 4]

// Sorting
List<String> sorted = names.stream()
    .sorted()
    .collect(Collectors.toList());  // [Alice, Bob, Charlie, David, Eva]

List<String> customSorted = names.stream()
    .sorted(Comparator.comparing(String::length))
    .collect(Collectors.toList());  // [Bob, Eva, Alice, David, Charlie]

// Distinct
List<Integer> distinct = Arrays.asList(1, 2, 2, 3, 3, 3).stream()
    .distinct()
    .collect(Collectors.toList());  // [1, 2, 3]

// Limiting
List<String> limited = names.stream()
    .limit(3)
    .collect(Collectors.toList());  // [Alice, Bob, Charlie]

// Skipping
List<String> skipped = names.stream()
    .skip(2)
    .collect(Collectors.toList());  // [Charlie, David, Eva]

// Reduction
Optional<String> longest = names.stream()
    .reduce((s1, s2) -> s1.length() > s2.length() ? s1 : s2);  // Optional[Charlie]

int totalLength = names.stream()
    .mapToInt(String::length)
    .sum();  // 23

// Collectors
String joined = names.stream()
    .collect(Collectors.joining(", "));  // "Alice, Bob, Charlie, David, Eva"

Map<Integer, List<String>> groupedByLength = names.stream()
    .collect(Collectors.groupingBy(String::length));
// {3=[Bob, Eva], 5=[Alice, David], 7=[Charlie]}

Map<Boolean, List<String>> partitioned = names.stream()
    .collect(Collectors.partitioningBy(s -> s.length() > 4));
// {false=[Bob, Eva], true=[Alice, Charlie, David]}

// Finding
Optional<String> anyMatch = names.stream()
    .filter(s -> s.startsWith("D"))
    .findAny();  // Optional[David]

Optional<String> firstMatch = names.stream()
    .filter(s -> s.contains("a"))
    .findFirst();  // Optional[Alice]

// Matching
boolean hasD = names.stream()
    .anyMatch(s -> s.startsWith("D"));  // true

boolean allLong = names.stream()
    .allMatch(s -> s.length() > 2);  // true

boolean noneZ = names.stream()
    .noneMatch(s -> s.startsWith("Z"));  // true
```

### 11.4 Functional Interfaces (Java 8+)

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// Function<T, R> - takes T, returns R
Function<Integer, String> intToString = Object::toString;
List<String> strings = numbers.stream()
    .map(intToString)
    .collect(Collectors.toList());

// BiFunction<T, U, R> - takes T and U, returns R
BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
int sum = add.apply(10, 20);  // 30

// Consumer<T> - takes T, returns void
Consumer<String> printer = System.out::println;
names.forEach(printer);

// BiConsumer<T, U> - takes T and U, returns void
BiConsumer<String, Integer> entryPrinter = (key, value) -> 
    System.out.println(key + ": " + value);
Map<String, Integer> map = new HashMap<>();
map.put("one", 1);
map.forEach(entryPrinter);

// Predicate<T> - takes T, returns boolean
Predicate<Integer> isEven = n -> n % 2 == 0;
List<Integer> evens = numbers.stream()
    .filter(isEven)
    .collect(Collectors.toList());

// BiPredicate<T, U> - takes T and U, returns boolean
BiPredicate<String, String> startsWith = String::startsWith;
boolean result = startsWith.test("Hello", "He");  // true

// Supplier<T> - takes nothing, returns T
Supplier<Double> random = Math::random;
Double value = random.get();

// UnaryOperator<T> - takes T, returns T (specialized Function)
UnaryOperator<Integer> square = n -> n * n;
List<Integer> squares = numbers.stream()
    .map(square)
    .collect(Collectors.toList());

// BinaryOperator<T> - takes two T, returns T (specialized BiFunction)
BinaryOperator<Integer> multiply = (a, b) -> a * b;
int product = numbers.stream()
    .reduce(1, multiply);  // 120
```

## 12. Time Complexity Summary

| Data Structure | Get/Access | Search | Insert | Delete | Space Complexity |
|----------------|------------|--------|--------|--------|------------------|
| Array          | O(1)       | O(n)   | O(n)   | O(n)   | O(n)             |
| ArrayList      | O(1)       | O(n)   | O(n)   | O(n)   | O(n)             |
| LinkedList     | O(n)       | O(n)   | O(1)*  | O(1)*  | O(n)             |
| Stack          | O(n)       | O(n)   | O(1)   | O(1)   | O(n)             |
| Queue          | O(n)       | O(n)   | O(1)   | O(1)   | O(n)             |
| PriorityQueue  | O(1)*      | O(n)   | O(log n) | O(log n) | O(n)         |
| HashMap        | N/A        | O(1)   | O(1)   | O(1)   | O(n)             |
| TreeMap        | N/A        | O(log n) | O(log n) | O(log n) | O(n)       |
| HashSet        | N/A        | O(1)   | O(1)   | O(1)   | O(n)             |
| TreeSet        | N/A        | O(log n) | O(log n) | O(log n) | O(n)       |

Notes:
- * LinkedList insert/delete is O(1) only if you already have a pointer to the node
- * PriorityQueue access to the highest-priority element is O(1), but not arbitrary elements
- HashMap, TreeMap, HashSet, TreeSet don't have direct "get/access" as they're accessed by key/value

## 13. Other Important Topics Not Covered

1. **ConcurrentHashMap** and other concurrent collections
2. **BlockingQueue** implementations
3. **WeakHashMap**, **IdentityHashMap** for specialized use cases
4. **EnumSet** and **EnumMap** for enum-based collections
5. **BitSet** for compact bit operations
6. **NavigableMap** and **NavigableSet** interfaces
7. **CopyOnWriteArrayList** and **CopyOnWriteArraySet** for thread-safe collections
8. **Collectors** utility methods for stream operations
9. **Spliterator** interface for parallel iteration
10. **Optional** class for avoiding null references

## 14. Choosing the Right Data Structure

When selecting a data structure for your application, consider:

1. **Operations needed**: Access, search, insert, delete
2. **Time complexity requirements**: Are certain operations more critical for performance?
3. **Ordering requirements**: Natural order, insertion order, or custom order
4. **Memory constraints**: Some structures use more memory than others
5. **Concurrency needs**: Thread safety requirements
6. **Null handling**: Some collections don't allow null elements
7. **Duplicate elements**: Whether duplicates should be allowed or not

This guide covers the most commonly used data structures and operations in Java, but the Java Collections Framework is extensive. For specific use cases, refer to the official Java documentation.
