#### Metadata

timestamp: **17:42**  &emsp;  **10-07-2021**
topic tags: #bst, #imp
question link: https://www.geeksforgeeks.org/flatten-bst-to-sorted-list-increasing-order/
resource:
parent link: [[1. BST GUIDE]]

---

# Flatten BST to sorted list

### Question

Given a binary search tree, the task is to flatten it to a sorted list. Precisely, the value of each node must be lesser than the values of all the nodes at its right, and its left node must be NULL after flattening. We must do it in O(H) extra space where ‘H’ is the height of BST.

---


### Approach
A simple approach will be to recreate the BST from its in-order traversal. This will take O(N) extra space were N is the number of node in BST. 

To improve upon that, we will simulate in order traversal of a binary tree as follows:  

1.  Create a dummy node.
2.  Create a variable called ‘prev’ and make it point to the dummy node.
3.  Perform in-order traversal and at each step. 
    -   Set prev -> right = curr
    -   Set prev -> left = NULL
    -   Set prev = curr

This will improve the space complexity to O(H) in worst case as in-order traversal takes O(H) extra space.

#### Code

``` cpp
void inorder(node* curr, node*& prev)
{
    // Base case
    if (curr == NULL)
        return;
    inorder(curr->left, prev);
    prev->left = NULL;
    prev->right = curr;
    prev = curr;
    inorder(curr->right, prev);
}
 
// Function to flatten binary tree using
// level order traversal
node* flatten(node* parent)
{
    // Dummy node
    node* dummy = new node(-1);
 
    // Pointer to previous element
    node* prev = dummy;
 
    // Calling in-order traversal
    inorder(parent, prev);
 
    prev->left = NULL;
    prev->right = NULL;
    node* ret = dummy->right;
 
    // Delete dummy node
    delete dummy;
    return ret;
}
 

```

---


