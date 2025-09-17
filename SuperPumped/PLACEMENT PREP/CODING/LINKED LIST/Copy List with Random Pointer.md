#### Metadata

timestamp: **19:14**  &emsp;  **19-06-2021**
topic tags: #linked_list , #imp, #two_pointer 
list tags: #solve_again 
question link: https://leetcode.com/problems/copy-list-with-random-pointer/
resource: [TUF](https://www.youtube.com/watch?v=VNf6VynfpdM&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=40)
parent link: [[SDE SHEET]], [[1. LINKED LIST GUIDE]]

---

# Copy List with Random Pointer

### Question
A linked list of length `n` is given such that each node contains an additional random pointer, which could point to any node in the list, or `null`.

Construct a [**deep copy**](https://en.wikipedia.org/wiki/Object_copying#Deep_copy) of the list. The deep copy should consist of exactly `n` **brand new** nodes, where each new node has its value set to the value of its corresponding original node. Both the `next` and `random` pointer of the new nodes should point to new nodes in the copied list such that the pointers in the original list and copied list represent the same list state. **None of the pointers in the new list should point to nodes in the original list**.

For example, if there are two nodes `X` and `Y` in the original list, where `X.random --> Y`, then for the corresponding two nodes `x` and `y` in the copied list, `x.random --> y`.

Return _the head of the copied linked list_.

The linked list is represented in the input/output as a list of `n` nodes. Each node is represented as a pair of `[val, random_index]` where:

-   `val`: an integer representing `Node.val`
-   `random_index`: the index of the node (range from `0` to `n-1`) that the `random` pointer points to, or `null` if it does not point to any node.

Your code will **only** be given the `head` of the original linked list.

>**Example 1:**
![](https://assets.leetcode.com/uploads/2019/12/18/e1.png)
**Input:** head = \[\[7,null\],\[13,0\],\[11,4\],\[10,2\],\[1,0\]\]
**Output:** \[\[7,null\],\[13,0\],\[11,4\],\[10,2\],\[1,0\]\]


---


### Approach 1

>**NOTE:** LOOK for a O(1) space solution. [Here](https://leetcode.com/problems/copy-list-with-random-pointer/discuss/43491/A-solution-with-constant-space-complexity-O(1)-and-linear-time-complexity-O(N))
#### Algorithm

#### Complexity Analysis
- O(n) Time
- O(n) Space

#### Code

``` cpp
/*
// Definition for a Node.
class Node {
public:
    int val;
    Node* next;
    Node* random;
    
    Node(int _val) {
        val = _val;
        next = NULL;
        random = NULL;
    }
};
*/

class Solution {
public:
    Node* copyRandomList(Node* head) {
        Node *dummy = new Node(0);
        unordered_map<Node*, Node*> m;
        
        Node *it = head, *res = dummy;
        
        while(it){
            res->next = new Node(it->val);
            res = res->next;
            m[it] = res;
            it = it->next;
        }
        
        it = head;
        res = dummy->next;
        while(it){
            if(m[it->random] != NULL){
                res->random = m[it->random];
            }
            it = it->next;
            res = res->next;
        }
        return dummy->next;
    }
};

```

---


