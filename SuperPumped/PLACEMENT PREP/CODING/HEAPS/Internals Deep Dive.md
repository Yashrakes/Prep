
### **Priority Queues in C++**

A **priority queue** is a data structure that allows elements to be inserted in arbitrary order but ensures that retrieval (extraction) always happens in a predefined priority order (e.g., highest or lowest first). In **C++**, `std::priority_queue` is implemented as a **max-heap** by default, meaning the largest element has the highest priority and will always be at the top.

---

### **How Priority Queues Work Internally in C++**

C++'s `std::priority_queue` is typically implemented using a **binary heap**, which is stored as a **dynamic array (std::vector)**. Internally, it uses **heap operations** such as:

- **Insertion (`push`)**: Adds an element and rearranges the heap using **heapify-up (sift-up)** to maintain order.
- **Deletion (`pop`)**: Removes the top element (highest priority) and rearranges using **heapify-down (sift-down)**.
- **Top (`top`)**: Returns the highest-priority element (root of the heap) in O(1) time.

---

### **Understanding Heaps**

A **heap** is a **tree-based** data structure that satisfies the **heap property**:

- In a **max-heap**, each parent node is **greater than or equal** to its children.
- In a **min-heap**, each parent node is **less than or equal** to its children.

The most common type of heap used in C++ priority queues is a **binary heap**, where:

- It is a **complete binary tree** (all levels except the last are full, and nodes are left-aligned).
- It is implemented as a **1-based or 0-based array** for efficient indexing.

For an element at index iii:

- **Left child** → Index 2i+12i + 12i+1
- **Right child** → Index 2i+22i + 22i+2
- **Parent** → Index (i−1)/2(i - 1) / 2(i−1)/2

---

### **Heap Operations in a Priority Queue**

1. **Insertion (`push`)** → O(log N)
    
    - Insert at the end of the array.
    - Perform **heapify-up** (bubble-up) to restore heap order.
2. **Deletion (`pop`)** → O(log N)
    
    - Remove the root (highest priority).
    - Move the last element to the root and perform **heapify-down** (sift-down).
3. **Accessing Top (`top`)** → O(1)
    
    - Simply returns the root element.

---
