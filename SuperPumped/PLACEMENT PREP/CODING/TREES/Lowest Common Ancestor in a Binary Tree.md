#### Metadata

timestamp: **18:53**  &emsp;  **25-06-2021**
tags: #imp, #binary_tree 
question link: https://practice.geeksforgeeks.org/problems/lowest-common-ancestor-in-a-binary-tree/1#
resource: https://www.geeksforgeeks.org/lowest-common-ancestor-binary-tree-set-1/
parent link: [[1. TREE GUIDE]]

---

# Lowest Common Ancestor in a Binary Tree

### Question

Given a Binary Tree with all **unique** values and two nodes value **n1** and **n2**. The task is to find the **lowest common ancestor** of the given two nodes. We may assume that either both n1 and n2 are present in the tree or none of them is present.

---


### Approach

#### Code1
**Method 1 (By Storing root to n1 and root to n2 paths):**   
Following is a simple O(n) algorithm to find LCA of n1 and n2. 

**1)** Find a path from the root to n1 and store it in a vector or array.   
**2)** Find a path from the root to n2 and store it in another vector or array.   
**3)** Traverse both paths till the values in arrays are the same. Return the common element just before the mismatch.

``` cpp
bool findPath(Node* root, int target, vector<Node*>& res, vector<Node*> cur){
	if(!root) return false;

	cur.push_back(root);
	if(root->data == target) {
		res = cur;
		return true; 
	}

	return findPath(root->left, target, res, cur) || findPath(root->right, target, res, cur);
}

Node* lca(Node* root ,int n1 ,int n2 )
{
   vector<Node*> p1, p2, cur;
   bool l1 = findPath(root, n1, p1, cur);
   bool l2 = findPath(root, n2, p2, cur);

   if(l1 == 0 || l2 == 0)
		return root;

	int n = min(p1.size(), p2.size()), i;
	for(i = 0; i < n; i++)
		if(p1[i] != p2[i]) break;

	return p1[i-1];
}

```

---


#### Code2
- this approach is faster than previous

``` cpp

struct Node *findLCAUtil(struct Node* root, int n1, int n2, bool &v1, bool &v2)
{
    // Base case
    if (root == NULL) return NULL;
 
    // If either n1 or n2 matches with root's key, report the presence
    // by setting v1 or v2 as true and return root (Note that if a key
    // is ancestor of other, then the ancestor key becomes LCA)
    if (root->key == n1)
    {
        v1 = true;
        return root;
    }
    if (root->key == n2)
    {
        v2 = true;
        return root;
    }
 
    // Look for keys in left and right subtrees
    Node *left_lca  = findLCAUtil(root->left, n1, n2, v1, v2);
    Node *right_lca = findLCAUtil(root->right, n1, n2, v1, v2);
 
    // If both of the above calls return Non-NULL, then one key
    // is present in once subtree and other is present in other,
    // So this node is the LCA
    if (left_lca && right_lca)  return root;
 
    // Otherwise check if left subtree or right subtree is LCA
    return (left_lca != NULL)? left_lca: right_lca;
}
 
// Returns true if key k is present in tree rooted with root
bool find(Node *root, int k)
{
    // Base Case
    if (root == NULL)
        return false;
 
    // If key is present at root, or in left subtree or right subtree,
    // return true;
    if (root->key == k || find(root->left, k) ||  find(root->right, k))
        return true;
 
    // Else return false
    return false;
}
 
// This function returns LCA of n1 and n2 only if both n1 and n2 are present
// in tree, otherwise returns NULL;
Node *findLCA(Node *root, int n1, int n2)
{
    // Initialize n1 and n2 as not visited
    bool v1 = false, v2 = false;
 
    // Find lca of n1 and n2 using the technique discussed above
    Node *lca = findLCAUtil(root, n1, n2, v1, v2);
 
    // Return LCA only if both n1 and n2 are present in tree
    if (v1 && v2 || v1 && find(lca, n2) || v2 && find(lca, n1))
        return lca;
 
    // Else return NULL
    return NULL;
}
```

---

#### Code

``` cpp


```

---