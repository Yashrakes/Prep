#### Metadata

timestamp: **11:37**  &emsp;  **26-06-2021**
tags: #imp, #binary_tree 
question link: https://leetcode.com/problems/binary-tree-maximum-path-sum/
resource: https://www.geeksforgeeks.org/find-maximum-path-sum-in-a-binary-tree/
parent link: [[1. TREE GUIDE]]

---

# Binary Tree Maximum Path Sum

### Question

A **path** in a binary tree is a sequence of nodes where each pair of adjacent nodes in the sequence has an edge connecting them. A node can only appear in the sequence **at most once**. Note that the path does not need to pass through the root.

The **path sum** of a path is the sum of the node's values in the path.

Given the `root` of a binary tree, return _the maximum **path sum** of any path_.

---


### Approach

For each node there can be four ways that the max path goes through the node:   
1. Node only   
2. Max path through Left Child + Node   
3. Max path through Right Child + Node   
4. Max path through Left Child + Node + Max path through Right Child .

The idea is to keep trace of four paths and pick up the max one in the end. 
**An important thing to note is, root of every subtree need to return maximum path sum such that at most one child of root is involved. This is needed for parent function call**. 
In below code, this sum is stored in ‘retVal’ and returned by the recursive function.

#### Code

``` cpp
int util(TreeNode* root, int& res){
	if(!root) return 0;

	int l = util(root->left, res);
	int r = util(root->right, res);
	
	//retVal is max of point 1, 2, 3
	int retVal = max(max(l, r) + root->val, root->val);

	int curMax = max(retVal, l + r + root->val);
	res = max(res, curMax);

	return retVal;
}

int maxPathSum(TreeNode* root) {
	int res = INT_MIN;
	int s = util(root, res);
	return max(res, s);
}

```

---


