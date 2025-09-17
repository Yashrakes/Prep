#### Metadata

parent link: [[1. HEAP GUIDE]]
documentation: https://en.cppreference.com/w/cpp/container/priority_queue

--- 

## Syntax

![[Pasted image 20250129203147.png]]

- We use priority queue to simulate heaps.
- **Max heap:** `priority_queue<int> maxHeap;`
- **Min heap:** `priority_queue<int, vector<int>, greater<int>> minHeap;`
- **Custom heap:** `priority_queue<int, vector<int>, compare> heap;`

#### Priority Queue with Custom compare
- [Used here](https://practice.geeksforgeeks.org/problems/sorting-elements-of-an-array-by-frequency/0#)

```cpp
/* 
This compare fuction is to implement a maxheap where the element to be inserted is 
a pair<int, int> where pair.first represents the frequency and pair.second is the
number. If frequency of two numbers are equal we must place the smaller number
before the greater one.
*/
struct compare{
    bool operator()(const pair<int, int> &p1, const pair<int, int> &p2){
        if(p1.first == p2.first)
            return p1.second > p2.second;
			
		//	
        return p1.first < p2.first;
    }    
};


bool comparator(Type a, Type b);

/*
Return Value (`true` or `false`):
- If `true` → Swap `a` and `b` (means `a` should come after `b`).
- If `false` → Keep `a` and `b` in the same order.
*/

```

---

## Analysis

- **Insertion**: Insertion of n items one by one is `O(n * log n)`, however if we insert all the items through the constructor then it takes `O(n)`. [See this to understand](https://www.geeksforgeeks.org/time-complexity-of-building-a-heap/)
- **Deletion**: Deletion of n items is `O(n * log k)`
- **Lookup (top)**: `O(1)`

>**STACK OVERFLOW:**<br>
>If you have an array of size `n` and you want to build a heap from all items at once, Floyd's algorithm can do it with O(n) complexity. See [Building a heap](https://en.wikipedia.org/wiki/Binary_heap#Building_a_heap). This corresponds to the [std::priority_queue constructors](http://en.cppreference.com/w/cpp/container/priority_queue/priority_queue) that accept a container parameter. <br>
>If you have an empty priority queue to which you want to add `n` items, one at a time, then the complexity is O(n \* log(n)). <br>
>So if you have all of the items that will go into your queue before you build it, then the first method will be more efficient. You use the second method--adding items individually--when you need to maintain a queue: adding and removing elements over some time period.<br>
>Removing `n` items from the priority queue also is O(n \* log(n)). <br>
>Documentation for [std::priority_queue](http://en.cppreference.com/w/cpp/container/priority_queue) includes runtime complexity of all operations.






---