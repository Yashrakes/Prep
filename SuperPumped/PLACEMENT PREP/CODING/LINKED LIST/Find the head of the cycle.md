#### Metadata

timestamp: **23:04**  &emsp;  **16-06-2021**
topic tags: #linked_list 
list tags: #solve_again 
similar:
question link: https://leetcode.com/problems/linked-list-cycle-ii/
parent link: [[SDE SHEET]], [[1. LINKED LIST GUIDE]]

resources: 
1. https://www.geeksforgeeks.org/detect-and-remove-loop-in-a-linked-list/
2. [[Floyd’s Cycle detection algorithm]]
3. https://www.youtube.com/watch?v=354J83hX7RI&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=34

---

# Find the head of the cycle

### Question
Given a linked list, return the node where the cycle begins. If there is no cycle, return `null`.

There is a cycle in a linked list if there is some node in the list that can be reached again by continuously following the `next` pointer. Internally, `pos` is used to denote the index of the node that tail's `next` pointer is connected to. **Note that `pos` is not passed as a parameter**.

**Notice** that you **should not modify** the linked list.


---


### Approach 1

#### Algorithm
We know that Floyd’s Cycle detection algorithm terminates when fast and slow pointers meet at a common point. We also know that this common point is one of the loop nodes (2 or 3 or 4 or 5 in the above diagram). Store the address of this in a pointer variable say ptr2. After that start from the head of the Linked List and check for nodes one by one if they are reachable from ptr2. Whenever we find a node that is reachable, we know that this node is the starting node of the loop in Linked List and we can get the pointer to the previous of this node.
#### Code

``` cpp
ListNode *getHead(ListNode *loopPtr, ListNode *head){
	ListNode *ptr1, *ptr2;

	ptr1 = head;
	while(1){

		ptr2 = loopPtr;
		while(ptr2->next != loopPtr && ptr2->next != ptr1)
			ptr2 = ptr2->next;

		if(ptr2->next == ptr1) return ptr1;

		ptr1 = ptr1->next;
	}
}

ListNode *detectCycle(ListNode *head) {

	ListNode *slow = head, *fast = head;

	while(slow && fast && fast->next){
		slow = slow->next;
		fast = fast->next->next;

		if(slow == fast)
			return getHead(slow, head);
	}

	return NULL;
}

```

---
### Approach 2

>Better than approach 1
>After detecting the loop, if we start slow pointer from head and move both slow and fast pointers at same speed until fast don’t meet, they would meet at the beginning of the loop. 	
#### Algorithm

Let slow and fast meet at some point after Floyd’s Cycle finding algorithm. Below diagram shows the situation when cycle is found.

![LinkedListCycle](http://www.geeksforgeeks.org/wp-content/uploads/LinkedListCycle.jpg)

We can conclude below from above diagram :
```
Distance traveled by fast pointer = 2 * (Distance traveled 
                                         by slow pointer)

(m + n*x + k) = 2*(m + n*y + k)

Note that before meeting the point shown above, fast
was moving at twice speed.

x -->  Number of complete cyclic rounds made by 
       fast pointer before they meet first time

y -->  Number of complete cyclic rounds made by 
       slow pointer before they meet first time

```
<br>
From above equation, we can conclude below:

```
m + k = (x-2y)*n

Which means m+k is a multiple of n. 

Thus we can write, m + k = i*n or m = i*n - k.

Hence, distance moved by slow pointer: m, is equal to distance moved by fast pointer:
i*n - k or (i-1)*n + n - k (cover the loop completely i-1 times and start from n-k).
```
<br>

So if we start moving both pointers again at **same speed** such that one pointer (say slow) begins from head node of linked list and other pointer (say fast) begins from meeting point. When slow pointer reaches beginning of loop (has made m steps), fast pointer would have made also moved m steps as they are now moving same pace. Since m+k is a multiple of n and fast starts from k, they would meet at the beginning. Can they meet before also? No because slow pointer enters the cycle first time after m steps.

#### Code

``` cpp
/**
 * Definition for singly-linked list.
 * struct ListNode {
 *     int val;
 *     ListNode *next;
 *     ListNode(int x) : val(x), next(NULL) {}
 * };
 */
class Solution {
public:
ListNode *getHead(ListNode *loopPtr, ListNode *head){
	ListNode *slow = head, *fast = loopPtr;

	while(slow != fast){
		slow = slow->next;
		fast = fast->next;
	}

	return slow;
}

ListNode *detectCycle(ListNode *head) {

	ListNode *slow = head, *fast = head;

	while(slow && fast && fast->next){
		slow = slow->next;
		fast = fast->next->next;

		if(slow == fast)
			return getHead(slow, head);
	}

	return NULL;
}
};

```

---

