#### Metadata

timestamp: **23:36**  &emsp;  **25-06-2021**
tags: #imp, #binary_tree 
question link: https://practice.geeksforgeeks.org/problems/maximum-path-sum/1#
resource: https://www.geeksforgeeks.org/find-maximum-path-sum-two-leaves-binary-tree/
parent link: [[1. TREE GUIDE]]

---

# Maximum Path Sum between 2 Leaf Nodes

### Question
Given a binary tree in which each node element contains a number. Find the maximum possible sum from one leaf node to another leaf node.

**NOTE:** Here Leaf node is a node which is connected to exactly one different node.

---


### Approach
The idea is to maintain two values in recursive calls:
1) Maximum root to leaf path sum for the subtree rooted under current node.   
2) The maximum path sum between leaves (desired output).  
For every visited node X, we find the maximum root to leaf sum in left and right subtrees of X. We add the two values with X->data, and compare the sum with maximum path sum found so far.

>NOTE: According to the definition of leaf node mentioned in the question, a tree consisting of a root node and a single child has a valid path from the root to the child. In this case the value of res will remain unchanged and the actual answer the value returned by the util function.

#### Code

``` cpp
int util(Node* root, int* res){

	if(!root) return 0;

	if(!root->left && !root->right) return root->data;

	int ls = util(root->left, res);
	int rs = util(root->right, res);

	//if the root has both left and right child, only then can there be a valid path
	if(root->left && root->right){
		*res = max(*res, ls + rs + root->data);
		return max(ls, rs) + root->data;
	}

	return root->left ? ls + root->data : rs + root->data;
}

int maxPathSum(Node* root)
{
	int res = INT_MIN;
	int val = util(root, &res);
	return res == INT_MIN ? val : res;
}

```

---


