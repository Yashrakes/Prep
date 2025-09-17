#### Metadata

timestamp: **00:52**  &emsp;  **03-07-2021**
topic tags: #heap , #imp
question link: https://leetcode.com/problems/merge-k-sorted-lists/
resource:
parent link: [[1. HEAP GUIDE]]

---

# Merge k Sorted Lists

### Question
You are given an array of `k` linked-lists `lists`, each linked-list is sorted in ascending order.

_Merge all the linked-lists into one sorted linked-list and return it._


---


### Approach

#### Code

``` cpp
//Space: O(K) , since it is inplace
//Time: O(N * log k) each pop is O(log k), and we perform N pops
struct compare{
	bool operator()(const ListNode* l, const ListNode* r){
		return l->val > r->val;
	}
};

ListNode* mergeKLists(vector<ListNode*>& lists) {

	//Optimized min heap implementation
	//1.Initially we push only the head of each list in the queue
	priority_queue<ListNode*, vector<ListNode*>, compare> minq;
	for(auto list : lists)
		if(list)
			minq.push(list);

	if(minq.empty()) return NULL;

	/*2.Based on our comparator, we have used a min heap in the priority queue
		such that, the top will be the smallest element.
  		Since we have stored only the head of each list, every time we
  		pop we check whether head->next exists, if it does then we push it 
	*/
	ListNode* result = minq.top();
	minq.pop();

	if(result->next)
		minq.push(result->next);

	ListNode* tail = result;
	while(!minq.empty()){
		tail->next = minq.top();
		minq.pop();
		tail = tail->next;

		if(tail->next)
			minq.push(tail->next);
	}
	return result;
}
```

---


