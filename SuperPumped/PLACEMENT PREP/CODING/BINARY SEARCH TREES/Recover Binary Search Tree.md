#### Metadata

timestamp: **11:41**  &emsp;  **02-08-2021**
topic tags: #bst 
question link: https://www.interviewbit.com/problems/recover-binary-search-tree/
resource: https://leetcode.com/problems/recover-binary-search-tree/discuss/32535/No-Fancy-Algorithm-just-Simple-and-Powerful-In-Order-Traversal
parent link: [[1. BINARY SEARCH GUIDE]], [[1. TREE GUIDE]]

---

# Recover Binary Search Tree

### Question

Two elements of a binary search tree (BST) are swapped by mistake.  
Tell us the 2 values swapping which the tree will be restored.

> **Note:**  
> A solution using O(n) space is pretty straight forward. Could you devise a constant space solution?

---


### Approach

#### Code

``` cpp
void inorder(TreeNode *root, TreeNode **first, TreeNode **second, TreeNode **prev){
    if(!root) return;

    inorder(root->left, first, second, prev);

    if(*prev != NULL){
        if(*first == NULL && (*prev)->val >= root->val)
            *first = *prev;
    }

    if(*first != NULL && (*prev)->val >= root->val)
        *second = root;

    *prev = root;

    inorder(root->right, first, second, prev);
}

vector<int> Solution::recoverTree(TreeNode* A) {
    TreeNode *first = NULL, *second = NULL, *prev = NULL;

    inorder(A, &first, &second, &prev);

    vector<int> res;
    res.push_back(first->val);
    res.push_back(second->val);
    sort(res.begin(), res.end());
    
    return res;
}

```

---


