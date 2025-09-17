#### Metadata
parent: [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]]
Used in:
- [[Find the head of the cycle]]

---


## Algo

The algorithm is to start two pointers, slow and fast from head of linked list. We move slow one node at a time and fast two nodes at a time. If there is a loop, then they will definitely meet. This approach works because of the following facts.

1) When slow pointer enters the loop, the fast pointer must be inside the loop. Let fast pointer be distance k from slow.

2) Now if consider movements of slow and fast pointers, we can notice that distance between them (from slow to fast) increase by one after every iteration. After one iteration (of slow = next of slow and fast = next of next of fast), distance between slow and fast becomes k+1, after two iterations, k+2, and so on. When distance becomes n, they meet because they are moving in a cycle of length n.

<br>

>Example
Initial distance is 2. After one iteration, distance becomes 3, after 2 iterations, it becomes 4. After 3 iterations, it becomes 5 which is distance 0. And they meet.
![](https://media.geeksforgeeks.org/wp-content/uploads/Floyd-Proof.jpg)


---

## Applications

### Detect a Loop

**Floyd’s Cycle-Finding Algorithm:**   
**Approach:** This is the fastest method and has been described below:  

-   Traverse linked list using two pointers.
-   Move one pointer(slow\_p) by one and another pointer(fast\_p) by two.
-   If these pointers meet at the same node then there is a loop. If pointers do not meet then linked list doesn’t have a loop.

#### Code

``` cpp
bool hasCycle(ListNode *head) {
	ListNode *slow = head, *fast = head;

	while(slow && fast && fast->next){
		slow = slow->next;
		fast = fast->next->next;

		if(fast == slow) return true;
	}

	return false;
}

```



### Remove the loop