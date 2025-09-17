#### Metadata

timestamp: **13:36**  &emsp;  **16-06-2021**
topic tags: #linked_list 
question link: https://leetcode.com/problems/intersection-of-two-linked-lists/
resource:
parent link: [[1. LINKED LIST GUIDE]], [[SDE SHEET]]

---

# Intersection of Two Linked Lists

### Question

Given the heads of two singly linked-lists `headA` and `headB`, return _the node at which the two lists intersect_. If the two linked lists have no intersection at all, return `null`.

For example, the following two linked lists begin to intersect at node `c1`:

![](https://assets.leetcode.com/uploads/2021/03/05/160_statement.png)

It is **guaranteed** that there are no cycles anywhere in the entire linked structure.

**Note** that the linked lists must **retain their original structure** after the function returns.

---


### Approach

1. Trivial:
	1. Calculate the length of both the list. 
	2. The difference in the length is the offset. The  iterator of the longer list must be ahead of the iterator of the shorter list by offset no of nodes.
	3. Once, the offset difference is achieved, we simply have to move both iterators together until we find the intersection.

2. O(n) Time
	1. If  we link the tail of list1 with the head of list2 and the tail of list2 with the head of list1, we get two lists of the same length having the same intersection node as before.
	2. Hence we indirectly found the offset without having to find the length of the lists individually.

---


### Code

``` cpp
    ListNode *getIntersectionNode(ListNode *headA, ListNode *headB) {
        
        
        ListNode *it1 = headA, *it2 = headB;
        
        while(it1 != it2){
            it1 = it1 ? it1->next : headB;
            it2 = it2 ? it2->next : headA;
        }
        return it1;
    }

```

---


