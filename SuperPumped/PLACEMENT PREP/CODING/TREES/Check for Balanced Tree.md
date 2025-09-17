#### Metadata

timestamp: **20:21**  &emsp;  **24-06-2021**
question link: https://practice.geeksforgeeks.org/problems/check-for-balanced-tree/1#
resource:
parent link: [[1. TREE GUIDE]]

---

# Check for Balanced Tree

### Question

Given a binary tree, find if it is height balanced or not.   
A tree is height balanced if difference between heights of left and right subtrees is **not more than one** for all nodes of tree.

---


### Approach
Consider a height-balancing scheme where following conditions should be checked to determine if a binary tree is balanced.   
An empty tree is height-balanced. A non-empty binary tree T is balanced if:   
1) Left subtree of T is balanced   
2) Right subtree of T is balanced   
3) The difference between heights of left subtree and right subtree is not more than 1.

To check if a tree is height-balanced, get the height of left and right subtrees. Return true if difference between heights is not more than 1 and left and right subtrees are balanced, otherwise return false.

#### Code

``` cpp
bool isBalancedUtil(Node *root, int *height)
{
    if(!root){
        *height = 0;
        return true;
    } 
    
    int lh = 0, rh = 0;
    bool left = isBalancedUtil(root->left, &lh);
    bool right = isBalancedUtil(root->right, &rh);
    
    *height = max(lh, rh) + 1;
    
    if(abs(lh-rh) > 1) return false;
    
    return left && right;
}

bool isBalanced(Node *root)
{
    int h = 0;
    return isBalancedUtil(root, &h);
}

```

---


