#### Metadata

timestamp: **09:56**  &emsp;  **26-06-2021**
question link: https://www.geeksforgeeks.org/kth-ancestor-node-binary-tree-set-2/
parent link: [[1. TREE GUIDE]]

---

# Kth ancestor of a node in binary tree

### Question

Given a binary tree in which nodes are numbered from 1 to n. Given a node and a positive integer K. We have to print the Kth ancestor of the given node in the binary tree. If there does not exist any such ancestor then print -1.

---


### Approach
- WE store the root to node path of the given value in a vector and then simply extract the kth element from the end.


#### Code1

``` cpp
bool findPath(Node* root, vector<Node*>& path, int n1){
    if(!root) return false;
    
    path.push_back(root);
    if(root->data == n1) return true;
    
    if(findPath(root->left, path, n1) || findPath(root->right, path, n1))
        return true;
        
    path.pop_back();
    return false;
}

void kthAncestorDFS(Node* root, int n1, int k){
    vector<Node*> path;
    if(!findPath(root, path, n1))
        cout<<"-1\n";
    
    int size = path.size();
    if(size - (k+1) < 0)
        cout<<"-1\n";
        
    cout<<path[size-(k+1)]->data;
}

```

---

#### Code2 : Recursive

```cpp
Node* kthAncestorDFS(Node *root, int node , int &k)
{  
    // Base case
    if (!root)
        return NULL;
     
	Node* temp; 
    if (root->data == node||
       (temp =  kthAncestorDFS(root->left,node,k)) ||
       (temp =  kthAncestorDFS(root->right,node,k)))
    {  
        if (k > 0)       
            k--;
         
        else if (k == 0)
        {
            // print the kth ancestor
            cout<<"Kth ancestor is: "<<root->data;
             
            // return NULL to stop further backtracking
            return NULL;
        }
         
        // return current node to previous call
        return root;
    }
}

```
