#### Metadata

timestamp: **16:49**  &emsp;  **25-06-2021**
topic tags: #imp , #binary_tree 
question link: https://practice.geeksforgeeks.org/problems/duplicate-subtree-in-binary-tree/1
resource: 
https://www.youtube.com/watch?v=_j7yb_nWFO8
https://www.geeksforgeeks.org/check-binary-tree-contains-duplicate-subtrees-size-2/
parent link: [[1. TREE GUIDE]]

---

# Duplicate subtree in Binary Tree

### Question
Given a binary tree, find out whether it contains a duplicate sub-tree of size two or more, or not.

---


### Approach
- We use the method of serializing a tree to solve this problem.
- We serialilze the inorder traversal of a particular subtree and store it in a hash table.

>NOTE: whenever serialization is used, its better to consider the inorder traversal along with null nodes to uniquely identify the subtree uniquely.
#### Code

``` cpp
unordered_map<string, int> m;
string serialize(Node* root){
    if(!root) return "";
    
    if(!root->left && !root->right){
        string s;
        s = root->data;
        return "(" + s + ")";
    }

    string ans = "(";
    ans += serialize(root->left);
    ans += root->data;
    ans += serialize(root->right);
    ans += ")";
    
    m[ans]++;
    return ans;
}

bool dupSub(Node *root)
{
    m.clear();
    string str = serialize(root);
     
     for(auto it : m)
        if(it.second >= 2) return true;
        
    return false;
}

```

---


