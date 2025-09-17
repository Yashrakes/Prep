#### Metadata

timestamp: **21:43**  &emsp;  **23-06-2021**
topic tags: #binary_tree
question link: https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/
resource:
parent link: [[1. TREE GUIDE]]

---

# Construct Binary Tree from Preorder and Inorder Traversal

### Question

Given two integer arrays `preorder` and `inorder` where `preorder` is the preorder traversal of a binary tree and `inorder` is the inorder traversal of the same tree, construct and return _the binary tree_.

---


### Approach


#### Code

``` cpp
/**
 * Definition for a binary tree node.
 * struct TreeNode {
 *     int val;
 *     TreeNode *left;
 *     TreeNode *right;
 *     TreeNode() : val(0), left(nullptr), right(nullptr) {}
 *     TreeNode(int x) : val(x), left(nullptr), right(nullptr) {}
 *     TreeNode(int x, TreeNode *left, TreeNode *right) : val(x), left(left), right(right) {}
 * };
 */
class Solution {
public:
    vector<int> preorder;
    vector<int> inorder;
    int rootIdx;
    unordered_map<int, int> map;
    
    TreeNode* helper(int inLeft, int inRight){
        if(inLeft > inRight) return NULL;
        
        int val = preorder[rootIdx];
        TreeNode* root = new TreeNode(val);
        rootIdx++;
        
        if(inLeft == inRight) return root;
        
        int index = map[val];
        
        root->left = helper(inLeft, index-1);
        root->right = helper(index+1, inRight);
        
        return root;
        
    }
    
    TreeNode* buildTree(vector<int>& preorder, vector<int>& inorder) {
        this->preorder = preorder;
        this->inorder = inorder;
        rootIdx = 0;
        
        int n = preorder.size();
        for(int i = 0; i < n ; i++)
            map[inorder[i]] = i;
        
        return helper(0, n-1);
    }
};

```

---


