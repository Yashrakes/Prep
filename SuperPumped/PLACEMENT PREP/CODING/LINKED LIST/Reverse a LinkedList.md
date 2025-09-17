#### Metadata

timestamp: **19:33**  &emsp;  **15-06-2021**
question link: https://leetcode.com/problems/reverse-linked-list/
resource:
parent link: [[1. LINKED LIST GUIDE]], [[SDE SHEET]]

---

# Reverse a LinkedList

### Question

Given the `head` of a singly linked list, reverse the list, and return _the reversed list_.

---


### Approach

#### Algorithm
###### Iterative:
Assume that we have linked list `1 → 2 → 3 → Ø`, we would like to change it to `Ø ← 1 ← 2 ← 3`.

While you are traversing the list, change the current node's next pointer to point to its previous element. Since a node does not have reference to its previous node, you must store its previous element beforehand. You also need another pointer to store the next node before changing the reference. Do not forget to return the new head reference at the end!

###### Recursive
The recursive version is slightly trickier and the key is to work backwards. Assume that the rest of the list had already been reversed, now how do I reverse the front part? 

Let's assume the list is: n1 → … → nk-1 → nk → nk+1 → … → nm → Ø

Assume from node nk+1 to nm had been reversed and you are at node nk.

n1 → … → nk-1 → **nk** → nk+1 ← … ← nm

We want nk+1’s next node to point to nk.

So,
nk.next.next = nk;

Be very careful that n1's next must point to Ø. If you forget about this, your linked list has a cycle in it. This bug could be caught if you test your code with a linked list of size 2.

#### Complexity Analysis
###### Iterative:
-   Time complexity : O(n) . Assume that n is the list's length, the time complexity is O(n).
    
-   Space complexity : O(1).

###### Recursive

-   Time complexity : O(n). Assume that nn is the list's length, the time complexity is O(n).
    
-   Space complexity : O(n).  The extra space comes from implicit stack space due to recursion. The recursion could go up to n levels deep.

---


### Code

**Iterative**:
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
    ListNode* reverseList(ListNode* head) {
        
        ListNode *prev = NULL, *cur = head;
        
        while(cur){
            ListNode *nextTemp = cur->next;
            cur->next = prev;
            prev = cur;
            cur = nextTemp;
        }
        return prev;
    }
};
```

<br>

**Recursive**:
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
    ListNode* reverseList(ListNode* head) {
        
        if(!head || head->next == NULL) return head;
        
        ListNode *p = reverseList(head->next);
        head->next->next = head;
        head->next = NULL;
        return p;
    }
};
```

---


