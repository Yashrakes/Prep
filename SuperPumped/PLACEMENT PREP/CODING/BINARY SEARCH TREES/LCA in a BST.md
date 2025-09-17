#### Metadata

timestamp: **08:18**  &emsp;  **10-07-2021**
topic tags: #bst, #imp
question link: https://practice.geeksforgeeks.org/problems/lowest-common-ancestor-in-a-bst/1#
resource: https://www.geeksforgeeks.org/lowest-common-ancestor-in-a-binary-search-tree/
parent link: [[1. BST GUIDE]]

---

# LCA in a BST

### Question

Given a Binary Search Tree (with all values unique) and two node values. Find the Lowest Common Ancestors of the two nodes in the BST.

---


### Approach

1.  Create a recursive function that takes a node and the two values n1 and n2.
2.  If the value of the current node is less than both n1 and n2, then LCA lies in the right subtree. Call the recursive function for thr right subtree.
3.  If the value of the current node is greater than both n1 and n2, then LCA lies in the left subtree. Call the recursive function for thr left subtree.
4.  If both the above cases are false then return the current node as LCA.

#### Code

``` cpp
Node* LCA(Node *root, int n1, int n2)
{
   
    if(!root) return root;

    if(root->data > n1 && root->data > n2)
        return LCA(root->left, n1, n2);
        
    if(root->data < n1 && root->data < n2)
        return LCA(root->right, n1, n2);
        
    return root;
}

```

---


