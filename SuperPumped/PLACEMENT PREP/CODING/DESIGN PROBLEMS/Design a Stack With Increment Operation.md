#### Metadata

timestamp: **10:37**  &emsp;  **06-07-2021**
topic tags: #design, #imp
question link: https://leetcode.com/problems/design-a-stack-with-increment-operation/
resource: https://leetcode.com/problems/design-a-stack-with-increment-operation/discuss/539716/JavaC%2B%2BPython-Lazy-increment-O(1)
parent link: [[1. DESIGN GUIDE]]

---

# Design a Stack With Increment Operation

### Question

Design a stack which supports the following operations.

Implement the `CustomStack` class:

-   `CustomStack(int maxSize)` Initializes the object with `maxSize` which is the maximum number of elements in the stack or do nothing if the stack reached the `maxSize`.
-   `void push(int x)` Adds `x` to the top of the stack if the stack hasn't reached the `maxSize`.
-   `int pop()` Pops and returns the top of stack or **-1** if the stack is empty.
-   `void inc(int k, int val)` Increments the bottom `k` elements of the stack by `val`. If there are less than `k` elements in the stack, just increment all the elements in the stack.

---


### Approach

#### Code 1 : Lazy increment
``` cpp
class CustomStack {
public:
    //Lazy increment
    vector<int> stack, inc;
    int n;
    
    CustomStack(int maxSize) {
        n = maxSize;
    }
    
    void push(int x) {
        if(stack.size() == n)
            return;
        
        stack.push_back(x);
        inc.push_back(0);
    }
    
    int pop() {
        if(stack.size() == 0) 
            return -1;
        
        int i = stack.size()-1;
        if(i > 0) inc[i-1] += inc[i];
        
        int val = stack[i] + inc[i];
        stack.pop_back();
        inc.pop_back();
        
        return val;
    }
    
    void increment(int k, int val) {
        int i = min(k, (int)stack.size()) - 1;
        
        if(i >= 0)
            inc[i] += val;
    }
};
```

---

#### Code 2
- We can also use a vector, but deque is more efficient at insertion and deleltion
``` cpp
int maxSize;
deque<int> dq;

CustomStack(int maxSize) {
	this->maxSize = maxSize;
}

void push(int x) {
	if(dq.size() < maxSize)
		dq.push_back(x);
}

int pop() {
	if(dq.empty()) return -1;

	int val = dq.back();
	dq.pop_back();
	return val;
}

void increment(int k, int val) {
	deque<int> :: iterator it;
	for(it = dq.begin(); it != dq.end(); it++){
		if(k > 0){
			*it = *it + val;
			k--;
		}
		else
			break;
	}

}

```

---


