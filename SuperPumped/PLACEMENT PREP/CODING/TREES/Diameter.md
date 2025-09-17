#### Metadata

timestamp: **15:34**  &emsp;  **24-06-2021**
question link: https://practice.geeksforgeeks.org/problems/diameter-of-binary-tree/1#
parent link: [[1. TREE GUIDE]]

---

# Diameter of a Binary Tree

### Question

Given a Binary Tree, **find diameter of it**.  
The diameter of a tree is the number of nodes on the longest path between two end nodes in the tree. The diagram below shows two trees each with diameter nine, the leaves that form the ends of a longest path are shaded (note that there is more than one path in each tree of length nine, but no path longer than nine nodes).

---


### Approach

The diameter of a tree (sometimes called the width) is the number of nodes on the longest path between two end nodes. The diagram below shows two trees each with diameter nine, the leaves that form the ends of the longest path are shaded (note that there is more than one path in each tree of length nine, but no path longer than nine nodes). 

  
![](https://media.geeksforgeeks.org/wp-content/uploads/Diameter-of-Binary-Tree.png)  

The diameter of a tree T is the largest of the following quantities:
-   the diameter of T's left subtree.
-   the diameter of T's right subtree.
-   the longest path between leaves that goes through the root of T (this can be computed from the heights of the subtrees of T)


#### Code

``` cpp
int maxHeight(Node* root){
	if(!root) return 0;
	return max(maxHeight(root->left), maxHeight(root->right)) + 1;
}

int diameter(Node* root) {
	// Your code here
	if(!root) return 0;

	int lheight = maxHeight(root->left);
	int rheight = maxHeight(root->right);

	int ldiameter = diameter(root->left);
	int rdiameter = diameter(root->right);

	return max({ldiameter, rdiameter, lheight+rheight+1});
}

```

---


