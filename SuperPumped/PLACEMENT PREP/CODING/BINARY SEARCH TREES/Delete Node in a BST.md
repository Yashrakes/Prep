#### Metadata

timestamp: **18:36**  &emsp;  **09-07-2021**
topic tags: #bst, #imp
question link: https://leetcode.com/problems/delete-node-in-a-bst/
resource: https://www.geeksforgeeks.org/binary-search-tree-set-2-delete/
parent link: [[1. BST GUIDE]]

---

# Delete Node in a BST

### Question

Given a root node reference of a BST and a key, delete the node with the given key in the BST. Return the root node reference (possibly updated) of the BST.

Basically, the deletion can be divided into two stages:

1.  Search for a node to remove.
2.  If the node is found, delete the node.

---


### Approach

#### Code 1
**Time Complexity:** The worst case time complexity of delete operation is O(h) where h is the height of the Binary Search Tree. In worst case, we may have to travel from the root to the deepest leaf node. The height of a skewed tree may become n and the time complexity of delete operation may become O(n).

``` cpp
TreeNode* deleteNode(TreeNode* root, int key) {
	if(!root) return root;

	if(key < root->val)
		root->left = deleteNode(root->left, key);
	else if(key > root->val)
		root->right = deleteNode(root->right, key);
	else{
		//If key found
		//Case 1: if root is a leaf node
		if(!root->left && !root->right){
			delete(root);
			return NULL;
		}

		//Case 2.1 : Left child is null
		else if(root->left == NULL){
			TreeNode *temp = root->right;
			delete(root);
			return temp;
		}

		//case 2.2: Right child is null
		else if(root->right == NULL){
			TreeNode *temp = root->left;
			delete(root);
			return temp;
		}

		//case 3: if root has both the children, then find inorder successor of 
		//root and replace root's vlalue with the successor's value and finally
		//delete the successor
		else{
			TreeNode *temp = root->right;
			while(temp->left)
				temp = temp->left;

			root->val = temp->val;
			root->right = deleteNode(root->right, root->val);

		}
	}

	return root;
}

```

---
#### Code 2 : **Optimization**
In the above recursive code, we recursively call delete() for the successor. We can avoid recursive calls by keeping track of the parent node of the successor so that we can simply remove the successor by making the child of a parent NULL. We know that the successor would always be a leaf node.

``` cpp
TreeNode* deleteNode(TreeNode* root, int key) {
	if(!root) return root;

	if(key < root->val)
		root->left = deleteNode(root->left, key);
	else if(key > root->val)
		root->right = deleteNode(root->right, key);
	else{
		//If key found
		//Case 1: if root is a leaf node
		if(!root->left && !root->right){
			delete(root);
			return NULL;
		}

		//Case 2.1 : Left child is null
		else if(root->left == NULL){
			TreeNode *temp = root->right;
			delete(root);
			return temp;
		}

		//case 2.2: Right child is null
		else if(root->right == NULL){
			TreeNode *temp = root->left;
			delete(root);
			return temp;
		}

		//case 3: if root has both the children, then find inorder successor of root
		else{
			TreeNode *parent = root;
			TreeNode *succ = root->right;
			while(succ->left){
				parent = succ;
				succ = succ->left;
			}

			/* 
			Delete successor.  Since successoris always left child of its parent
			we can safely make successor's right child as left of its parent.
			If there is no succ, then assign succ->right to succParent->right 
			*/
			if(parent != root)
				parent->left = succ->right;
			else
				parent->right = succ->right;

			root->val = succ->val;
			delete(succ);
		}
	}

	return root;
}

```

---


