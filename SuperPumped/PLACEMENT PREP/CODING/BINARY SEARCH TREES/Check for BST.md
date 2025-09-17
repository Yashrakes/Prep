#### Metadata

timestamp: **19:20**  &emsp;  **09-07-2021**
topic tags: #bst, #imp 
question link: https://practice.geeksforgeeks.org/problems/check-for-bst/1#
resource: https://www.geeksforgeeks.org/a-program-to-check-if-a-binary-tree-is-bst-or-not/
parent link: [[1. BST GUIDE]]

---

# Check for BST

### Question

Given a binary tree. Check whether it is a BST or not.  
**Note:** We are considering that BSTs can not contain duplicate Nodes.

---


### Approach

#### Code

``` cpp
bool isBSTUtil(Node* root, Node* min, Node* max){
	if(!root) return true;

	if(min && root->data <= min->data)
		return false;

	if(max && root->data >= max->data)
		return false;

	return isBSTUtil(root->left, min, root) &&
			isBSTUtil(root->right, root, max);
}

bool isBST(Node* root) {
	return isBSTUtil(root, NULL, NULL);
}
```

---


