#### Metadata

timestamp: **21:56**  &emsp;  **23-06-2021**
topic tags: #binary_tree 
question link: https://leetcode.com/problems/construct-binary-tree-from-inorder-and-postorder-traversal/
parent link: [[1. TREE GUIDE]]

---

# Construct Binary Tree from Inorder and Postorder Traversal

### Question

Given two integer arrays `inorder` and `postorder` where `inorder` is the inorder traversal of a binary tree and `postorder` is the postorder traversal of the same tree, construct and return _the binary tree_.

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
vector<int> inorder;
vector<int> postorder;
int rootIdx;
unordered_map<int, int> inMap;
public:    
    TreeNode* helper(int inLeft, int inRight){
        
        // if there is no elements to construct subtrees
        if(inLeft > inRight) return NULL;
        
        int rootVal = postorder[rootIdx];
        TreeNode* root = new TreeNode(rootVal);

        
        int index = inMap[rootVal];
        
        rootIdx--;
        
        root->right = helper(index+1, inRight);
        root->left = helper(inLeft, index-1);
        
        return root;
    }
    
    TreeNode* buildTree(vector<int>& inorder, vector<int>& postorder) {
        
        this->inorder = inorder;
        this->postorder = postorder;
        
        //Root of the tree is postorder[n-1]
        rootIdx = postorder.size() - 1;
        
        //hash map storing indices of inorder
        for(int i = 0; i < inorder.size(); i++)
            inMap[inorder[i]] = i;
        
        return helper(0, inorder.size()-1);
    }
};
```

---


