#### Metadata

timestamp: **22:46**  &emsp;  **15-06-2021**
topic tags: #linked_list
question link: https://leetcode.com/problems/remove-nth-node-from-end-of-list/
resource:
parent link: [[1. Topic Wise Problem Guide]]

---

# Remove N-th node from back of LinkedList

### Question
Given the `head` of a linked list, remove the `nth` node from the end of the list and return its head.

---


### Approach

- If fast points to the last node of the list, and slow points to the nth node from the end then,     we see that fast is n-1 nodes ahead of slow.
    
- Therefore, we put fast n-1 nodes ahead of slow and then  move fast and slow together

---


### Code

``` cpp

class Solution {
public:
    ListNode* removeNthFromEnd(ListNode* head, int n) {
        ListNode *slow = head, *fast = head, *prev = NULL;
        
        
        for(int k = 1; k <= n-1; k++)
            fast = fast->next;
        
        while(fast->next != NULL){
            fast = fast->next;
            prev = slow;
            slow = slow->next;
        }
        
        if(!prev) return slow->next;
        
        prev->next = slow->next;
        slow->next = NULL;
        
        return head;
    }
};

```

---


