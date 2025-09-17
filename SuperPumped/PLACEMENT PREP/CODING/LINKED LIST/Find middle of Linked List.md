#### Metadata

timestamp: **14:37**  &emsp;  **16-06-2021**
topic tags: #linked_list 
question link: https://leetcode.com/problems/middle-of-the-linked-list/
parent link: [[SDE SHEET]] , [[1. LINKED LIST GUIDE]]

---

# Find middle of Linked List

### Question

Given a non-empty, singly linked list with head node `head`, return a middle node of linked list.

If there are two middle nodes, return the second middle node.

---


### Approach: Tortoise

We move the slow pointer by one node and fast pointer by two nodes. At any given point, the slow pointer will have moved half the no of nodes as the fast pointer. Therefore, when the fast pointer reaches the end, slow will point to the middle.


---


### Code

``` cpp
ListNode* middleNode(ListNode* head) {
	ListNode *slow = head, *fast = head;

	while(fast->next && fast->next->next){
		slow = slow->next;
		fast = fast->next->next;
	}

	if(fast->next == NULL)
		return slow;
	else
		return slow->next;
}

```

---


