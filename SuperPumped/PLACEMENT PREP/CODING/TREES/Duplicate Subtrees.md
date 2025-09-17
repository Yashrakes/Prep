#### Metadata

timestamp: **10:43**  &emsp;  **26-06-2021**
tags: #imp, #binary_tree 
question link: https://practice.geeksforgeeks.org/problems/duplicate-subtrees/1#
resource: https://www.geeksforgeeks.org/find-duplicate-subtrees/
parent link: [[1. TREE GUIDE]]

---

# Duplicate Subtrees

### Question
Given a binary tree of size **N**, your task is to that find all duplicate subtrees from the given binary tree.

**Your Task:**  
You don't need to take input. Just complete the function **printAllDups()** that takes the root **node** as a parameter and returns an array of Node*, which contains all the duplicate subtree.  
**Note:** Here the Output of every Node printed in the Pre-Order tree traversal format.

---


### Approach

- The idea is to use hashing. 
- We store inorder traversals of subtrees in a hash. Since simple inorder traversal cannot uniquely identify a tree, we use symbols like ‘(‘ and ‘)’ to represent NULL nodes.   
- We pass an Unordered Map in C++ as an argument to the helper function which recursively calculates inorder string and increases its count in map. If any string gets repeated, then it will imply duplication of the subtree rooted at that node so push that node in the Final result and return the vector of these nodes.

#### Code

``` cpp
unordered_map<string, int> mp;

string serialize(Node* root, vector<Node*>& res){
    if(!root) return "";
    

    string ans = "(";
    ans += serialize(root->left, res);
    ans += to_string(root->data);
    ans += serialize(root->right, res);
    ans += ")";
    
    mp[ans]++;
    if(mp[ans] == 2) res.push_back(root);
    return ans;
}

vector<Node*> printAllDups(Node* root)
{
    mp.clear();
    vector<Node*> res;
    serialize(root, res);
    return res;
}

//                OR
string serialize(Node* root, vector<Node*>& res){
    if(!root) return "$";
    

    string ans = "";
    ans += serialize(root->left, res);
    ans += to_string(root->data);
    ans += serialize(root->right, res);

    mp[ans]++;
    if(mp[ans] == 2) res.push_back(root);
    return ans;
}

```

---


