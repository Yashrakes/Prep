#### Metadata

timestamp: **17:51**  &emsp;  **02-07-2021**
topic tags: #heap, #imp 
question link: https://practice.geeksforgeeks.org/problems/nearly-sorted-algorithm/0#
resource:
parent link: [[1. HEAP GUIDE]]

---

# Nearly Sorted Algorithm

### Question

Given an array of n elements, where each element is at most k away from its target position. The task is to print array in sorted form.

---


### Approach

1) Create a Min Heap of size k+1 with first k+1 elements. This will take O(k) time (See [this GFact](https://www.geeksforgeeks.org/time-complexity-of-building-a-heap/))   
2) One by one remove min element from heap, put it in result array, and add a new element to heap from remaining elements.  
3) Removing an element and adding a new element to min heap will take log k time. So overall complexity will be O(k) + O((n-k) * log(k)).

#### Code

``` cpp
void sortK(vector<int>& arr, int k)
{
     
    // Insert first k+1 items in a priority queue (a O(k) operation)
    int heapSize, n = arr.size();
    heapSize = (n == k) ? k : k + 1;
    
    priority_queue<int, vector<int>, greater<int>> minQ(arr.begin(), arr.begin() + heapSize);
	    
    int index = 0, i;
    for(i = heapSize; i < n; i++){
        arr[index++] = minQ.top();
	    minQ.pop();
	    minQ.push(arr[i]);
    }
    
    while(!minQ.empty()){
        arr[index++] = minQ.top();
        minQ.pop();
    }
}

```

---


