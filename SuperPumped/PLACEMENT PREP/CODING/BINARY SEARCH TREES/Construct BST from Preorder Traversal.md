#### Metadata

timestamp: **08:38**  &emsp;  **10-07-2021**
topic tags: #bst, #imp
question link: https://leetcode.com/problems/construct-binary-search-tree-from-preorder-traversal/
resource: https://www.geeksforgeeks.org/construct-bst-from-given-preorder-traversa/
parent link: [[1. BST GUIDE]]

---

# Construct BST from Preorder Traversal

### Question
Given an array of integers preorder, which represents the **preorder traversal** of a BST (i.e., **binary search tree**), construct the tree and return _its root_.

It is **guaranteed** that there is always possible to find a binary search tree with the given requirements for the given test cases.

A **binary search tree** is a binary tree where for every node, any descendant of `Node.left` has a value **strictly less than** `Node.val`, and any descendant of `Node.right` has a value **strictly greater than** `Node.val`.

A **preorder traversal** of a binary tree displays the value of the node first, then traverses `Node.left`, then traverses `Node.right`


---


### Approach 1

#### Algorithm
- The first element of preorder traversal is always root. 
- We first construct the root. Then we find the index of the first element which is greater than the root. 
- Let the index be ‘i’. 
- The values between root and ‘i’ will be part of the left subtree, and the values between ‘i+1’ and ‘n-1’ will be part of the right subtree. 
- Divide given pre[] at index “i” and recur for left and right sub-trees.

#### Complexity Analysis
- **O(n2) time complexity**

#### Code

``` cpp
TreeNode* construct(vector<int>& preorder, int &index, int low, int high){

	if(low > high) return NULL;

	int val = preorder[index];
	TreeNode* root = new TreeNode(val);

	index++;

	int i;
	for(i = low; i <= high; i++)
		if(preorder[i] > val)
			break;

	root->left = construct(preorder, index, index, i-1);
	root->right = construct(preorder, index, i, high);

	return root;
}

TreeNode* bstFromPreorder(vector<int>& preorder) {
	int index = 0, n = preorder.size();
	return construct(preorder, index, 0, n-1);
}

```

---
### Approach 2  : Using min max, O(n)

- **O(n) time complexity **

#### Code

``` cpp
TreeNode* construct(vector<int>& preorder, int &preIdx, int key, int min, int max){
	int n = preorder.size();

	if(preIdx >= n)
		return NULL;

	TreeNode* root = NULL;

	if(key > min && key < max){
		root = new TreeNode(key);

		preIdx++;

		if(preIdx < n)
			root->left = construct(preorder, preIdx, preorder[preIdx], min, key);
		if(preIdx < n)
			root->right = construct(preorder, preIdx,preorder[preIdx], key, max);
	}

	return root;
}

TreeNode* bstFromPreorder(vector<int>& preorder) {
	int idx = 0;
	return construct(preorder, idx, preorder[idx], INT_MIN, INT_MAX);
}

```

---

