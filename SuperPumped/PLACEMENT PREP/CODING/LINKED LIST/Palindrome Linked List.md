#### Metadata

timestamp: **14:57**  &emsp;  **16-06-2021**
topic tags: #linked_list 
question link: https://leetcode.com/problems/palindrome-linked-list/
resource: [Video explanation](https://www.youtube.com/watch?v=-DtNInqFUXs&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=36)
parent link: [[SDE SHEET]], [[1. LINKED LIST GUIDE]]

---

# Palindrome Linked List

### Question

Given the `head` of a singly linked list, return `true` if it is a palindrome.

---


### Approach
- First we find the middle of the list using the tortoise approach
- Then we reverse the second half of the list and then compare
---


### Code

``` cpp

/**
 * Definition for singly-linked list.
 * struct ListNode {
 *     int val;
 *     ListNode *next;
 *     ListNode() : val(0), next(nullptr) {}
 *     ListNode(int x) : val(x), next(nullptr) {}
 *     ListNode(int x, ListNode *next) : val(x), next(next) {}
 * };
 */
class Solution {
public:
    ListNode* rev(ListNode* head){
        
        if(head == NULL || head->next == NULL) return head;
        
        ListNode* temp = rev(head->next);
        head->next->next = head;
        head->next = NULL;
        
        return temp;
    }
    
    bool isPalindrome(ListNode* head) {
        ListNode *slow = head, *fast = head, *it = head;
        
        while(fast && fast->next){
            slow = slow->next;
            fast = fast->next->next;
        }
        
        slow = rev(slow);
        
        // slow = slow->next;
        while(slow){
            if(it->val != slow->val) return false;
            it = it->next;
            slow = slow->next;
        }
        
        return true;
    }
};


```

---


