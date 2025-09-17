#### Metadata

timestamp: **21:25**  &emsp;  **24-06-2021**
question link: https://practice.geeksforgeeks.org/problems/boundary-traversal-of-binary-tree/1#
resource: https://www.geeksforgeeks.org/boundary-traversal-of-binary-tree/
parent link: [[1. TREE GUIDE]]

---

# Boundary Traversal of binary tree

### Question

Given a Binary Tree, find its Boundary Traversal. The traversal should be in the following order: 

1.  **Left boundary nodes:** defined as the path from the root to the left-most node ie- the leaf node you could reach when you always travel preferring the left subtree over the right subtree. 
2.  **Leaf nodes:** All the leaf nodes except for the ones that are part of left or right boundary.
3.  **Reverse right boundary nodes:** defined as the path from the right-most node to the root. The right-most node is the leaf node you could reach when you always travel preferring the right subtree over the left subtree. Exclude the root from this as it was already included in the traversal of left boundary nodes.

**Note:** If the root doesn't have a left subtree or right subtree, then the root itself is the left or right boundary.

---


### Approach

1. Print the left boundary in top-down manner.  
2. Print all leaf nodes from left to right, which can again be sub-divided into two sub-parts: 
	1. Print all leaf nodes of left sub-tree from left to right.  
	2. Print all leaf nodes of right subtree from left to right.  
3. Print the right boundary in bottom-up manner.

#### Code

``` cpp
void leftBoundary(vector<int>& res, Node* root){
	if(!root) return;

	if(root->left){
		res.push_back(root->data);
		leftBoundary(res, root->left);
	}
	else if(root->right){
		res.push_back(root->data);
		leftBoundary(res, root->right);
	}
}

void generateLeaf(vector<int>& res, Node* root){
	if(!root) return;
	
	if(!root->left && !root->right) res.push_back(root->data);
	
	generateLeaf(res, root->left);
	generateLeaf(res, root->right);
}

void rightBoundary(vector<int> &res, Node* root){
	if(!root) return;

	if(root->right){
		rightBoundary(res, root->right);
		res.push_back(root->data);
	}
	else if(root->left){
		rightBoundary(res, root->left);
		res.push_back(root->data);
	}
}

vector <int> printBoundary(Node *root)
{
	if(!root) return {};

	vector<int> res;
	res.push_back(root->data);

	leftBoundary(res, root->left);

	generateLeaf(res, root->left);
	generateLeaf(res, root->right);

	rightBoundary(res, root->right);
	return res;
}

```

---


