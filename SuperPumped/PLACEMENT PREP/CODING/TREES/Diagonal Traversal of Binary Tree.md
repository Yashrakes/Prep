#### Metadata

timestamp: **20:39**  &emsp;  **24-06-2021**
question link: https://practice.geeksforgeeks.org/problems/diagonal-traversal-of-binary-tree/1
resource: https://www.geeksforgeeks.org/diagonal-traversal-of-binary-tree/
parent link: [[1. TREE GUIDE]]

---

# Diagonal Traversal of Binary Tree

### Question
Given a Binary Tree, print the **diagonal traversal** of the binary tree.

Consider lines of slope -1 passing between nodes. Given a Binary Tree, print all diagonal elements in a binary tree belonging to same line.

![[Pasted image 20210624204009.png]]

>Output : 
Diagonal Traversal of binary tree : 8 10 14 3 6 7 13 1 4
 
---


### Approach

- slope of right child is same as root
- slope of left child is one more than the root

#### Code

``` cpp
void diagonalUtil(map<int, vector<int>> &m, Node* root, int slope){
    
    if(!root) return;
    
    m[slope].push_back(root->data);
    
    diagonalUtil(m, root->left, slope+1);
    diagonalUtil(m, root->right, slope);
}

vector<int> diagonal(Node *root)
{
    if(!root) return {};
    
   vector<int> res;
   map<int, vector<int>> m;
   diagonalUtil(m, root, 0);
   
   for(auto it : m)
        for(auto ele : it.second)
            res.push_back(ele);
        
    return res;
}

```

---


