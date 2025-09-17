#### Metadata

timestamp: **10:18**  &emsp;  **10-07-2021**
topic tags: #bst, #imp
question link: https://practice.geeksforgeeks.org/problems/binary-tree-to-bst/1#
resource: https://www.geeksforgeeks.org/binary-tree-to-binary-search-tree-conversion/
parent link: [[1. BST GUIDE]]

---

# Binary Tree to BST

### Question
Given a Binary Tree, convert it to Binary Search Tree in such a way that keeps the **original structure of Binary Tree intact.**


---


### Approach

1) Create a temp array arr[] that stores inorder traversal of the tree. This step takes O(n) time.

  
2) Sort the temp array arr[]. Time complexity of this step depends upon the sorting algorithm. In the following implementation, Quick Sort is used which takes (n^2) time. This can be done in O(nLogn) time using Heap Sort or Merge Sort.

3) Again do inorder traversal of tree and copy array elements to tree nodes one by one. This step takes O(n) time.

#### Complexity

-   **Time Complexity:** O(nlogn). This is the complexity of the sorting algorithm which we are using after first in-order traversal, rest of the operations take place in linear time.
-   **Auxiliary Space:** O(n). Use of data structure ‘array’ to store in-order traversal.

#### Code

``` cpp
void btInorder(Node* root, vector<int>& inorder){
	if(!root) return;

	btInorder(root->left, inorder);
	inorder.push_back(root->data);
	btInorder(root->right, inorder);
}

void convert(Node* root, int &idx, vector<int>& inorder){
	if(!root) return;

	convert(root->left, idx, inorder);
	root->data = inorder[idx++];
	convert(root->right, idx, inorder);
}

Node *binaryTreeToBST (Node *root)
{
	//Your code goes here
	vector<int> inorder;
	btInorder(root, inorder);

	sort(inorder.begin(), inorder.end());

	int idx = 0;
	convert(root, idx, inorder);
	return root;
}

```

---


