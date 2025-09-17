#### Metadata

timestamp: **12:24**  &emsp;  **10-07-2021**
topic tags: #bst, #imp
question link: https://practice.geeksforgeeks.org/problems/brothers-from-different-root/1#
resource: https://www.geeksforgeeks.org/count-pairs-from-two-bsts-whose-sum-is-equal-to-a-given-value-x/
parent link: [[1. BST GUIDE]]

---

# Brothers From Different Roots

### Question
Given two BSTs containing N**1** and N**2** distinct nodes respectively and given a value **x**. Your task is to complete the function **countPairs()**, that returns the count of all pairs from both the BSTs whose sum is equal to **x**.


---


### Approach

Traverse BST 1 from smallest value to node to largest. This can be achieved with the help of [iterative inorder traversal](https://www.geeksforgeeks.org/inorder-tree-traversal-without-recursion/). Traverse BST 2 from largest value node to smallest. This can be achieved with the help of reverse inorder traversal. Perform these two traversals simultaneously. Sum up the corresponding node’s value from both the BSTs at a particular instance of traversals. If sum == x, then increment **count**. If x > sum, then move to the inorder successor of the current node of BST 1, else move to the inorder predecessor of the current node of BST 2. Perform these operations until either of the two traversals gets completed.

#### Complexity Analysis
- **Time Complexity:** O(n1 + n2)   
- **Auxiliary Space:** O(h1 + h2) Where h1 is height of first tree and h2 is height of second tree
#### Code

``` cpp
int countPairs(Node* root1, Node* root2, int x)
{
    if(!root1 || !root2) return 0;
    
    stack<Node*> s1, s2;
    Node *cur1 = root1, *cur2 = root2;
    int count = 0;
    
    //perform inorder of root1 and reverese inorder of root2
    while(1){
        
        while(cur1){
            s1.push(cur1);
            cur1 = cur1->left;
        }
        
        while(cur2){
            s2.push(cur2);
            cur2 = cur2->right;
        }
        
        if(s1.empty() || s2.empty())
            break;
            
        cur1 = s1.top();
        cur2 = s2.top();
        
        if(cur1->data + cur2->data == x){
            count++;
            
            s1.pop();
            s2.pop();
            
            cur1 = cur1->right;
            cur2 = cur2->left;
        }
		
		//current sum is lesser than target, hence move towards larger sum by
		//moving cur1
        else if(cur1->data + cur2->data < x){
            s1.pop();
            cur1 = cur1->right;
            cur2 = NULL;
        }
        else{
            s2.pop();
            cur2 = cur2->left;
            cur1 = NULL;
        }
    }
    
    return count;
}

```

---


