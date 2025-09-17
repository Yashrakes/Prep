#### Metadata

timestamp: **19:19**  &emsp;  **16-06-2021**
topic tags: #linked_list 
list tags: #solve_again 
similar:
question link: https://practice.geeksforgeeks.org/problems/flattening-a-linked-list/1#
resource: [Take you forward soln](https://www.youtube.com/watch?v=ysytSSXpAI0&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=38)
parent link: [[SDE SHEET]], [[1. LINKED LIST GUIDE]]

---

# Flattening a Linked List

### Question

Given a Linked List of size N, where every node represents a sub-linked-list and contains two pointers:  
(i) a **next** pointer to the next node,  
(ii) a **bottom** pointer to a linked list where this node is head.  
Each of the sub-linked-list is in sorted order.  
Flatten the Link List such that all the nodes appear in a single level while maintaining the sorted order.   
**Note:** The flattened list will be printed using the bottom pointer instead of next pointer.

---


### Approach

- We take two lists at a time and merge it 
- We repeat the process using recursion


---


### Code

``` cpp
/* Node structure  used in the program

struct Node{
	int data;
	struct Node * next;
	struct Node * bottom;
	
	Node(int x){
	    data = x;
	    next = NULL;
	    bottom = NULL;
	}
	
};
*/

/*  Function which returns the  root of 
    the flattened linked list. */

Node* merge(Node *l1, Node *l2){
    Node *res = new Node(0), *l3 = res;
    
    while(l1 && l2){
       if(l1->data <= l2->data){
           l3->bottom = l1;
           l1 = l1->bottom;
       }
       else {
           l3->bottom = l2;
           l2 = l2->bottom;
       }
       l3 = l3->bottom;
   }
   l3->bottom = l1 ? l1 : l2;
   return res->bottom;
}

Node *flatten(Node *root)
{
    if(root == NULL || root->next == NULL) return root;
    
    Node *temp = flatten(root->next);
    
    return merge(root, temp);
}

```

---


