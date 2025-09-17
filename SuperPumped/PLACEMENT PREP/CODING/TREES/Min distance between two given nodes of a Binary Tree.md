#### Metadata

timestamp: **22:33**  &emsp;  **25-06-2021**
question link: https://practice.geeksforgeeks.org/problems/min-distance-between-two-given-nodes-of-a-binary-tree/1#
resource: https://www.geeksforgeeks.org/find-distance-between-two-nodes-of-a-binary-tree/
parent link: [[1. TREE GUIDE]]

---

# Min distance between two given nodes of a Binary Tree

### Question

Given a binary tree and two node values your task is to find the minimum distance between them.

---


### Approach

>**Dist(n1, n2)  =  Dist(root, n1)  +  Dist(root, n2)  -  2\*Dist(root, lca)** 
- n1 and n2 are the two given keys
- root is root of given Binary Tree.
- lca is lowest common ancestor of n1 and n2
- Dist(n1, n2) is the distance between n1 and n2.

#### Code

``` cpp
bool findPath(Node* root, vector<Node*>& path, int key){
    if(!root) return false;
    
    path.push_back(root);
    if(root->data == key) return true;
    
    if(findPath(root->left, path, key) || findPath(root->right, path, key))
        return true;
    
    path.pop_back();
    return false;
}

int findDist(Node* root, int a, int b) {
    vector<Node*> p1, p2;
    bool l1 = findPath(root, p1, a);
    bool l2 = findPath(root, p2, b);
    
    if(!l1 || !l2) return -1;
    
    int n = min(p1.size(), p2.size()), i;
    for(i = 0; i < n; i++)
        if(p1[i] != p2[i]) break;
        
    int d1 = p1.size()-1;
    int d2 = p2.size()-1;
    int d3 = 2*(i-1);
    return d1 + d2 - d3;
}

```

---


