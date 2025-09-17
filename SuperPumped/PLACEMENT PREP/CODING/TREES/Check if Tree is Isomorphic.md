#### Metadata

timestamp: **23:32**  &emsp;  **25-06-2021**
question link: https://practice.geeksforgeeks.org/problems/check-if-tree-is-isomorphic/1#
resource: https://www.geeksforgeeks.org/tree-isomorphism-problem/
parent link: [[1. TREE GUIDE]]

---

# Check if Tree is Isomorphic

### Question

Given two Binary Trees. Check whether they are Isomorphic or not.

**Note:**   
Two trees are called isomorphic if one can be obtained from another by a series of flips, i.e. by swapping left and right children of several nodes. Any number of nodes at any level can have their children swapped. Two empty trees are isomorphic.

---


### Approach

We simultaneously traverse both trees. Let the current internal nodes of two trees being traversed be **n1** and **n2** respectively. There are following two conditions for subtrees rooted with n1 and n2 to be isomorphic.  
1) Data of n1 and n2 is same.  
2) One of the following two is true for children of n1 and n2  
	1) Left child of n1 is isomorphic to left child of n2 and right child of n1 is isomorphic to right child of n2.  
	2)  Left child of n1 is isomorphic to right child of n2 and right child of n1 is isomorphic to left child of n2.

#### Code

``` cpp
bool isIsomorphic(Node *root1,Node *root2)
{
	if(!root1 && !root2) return true;

	if(!root1 || !root2) return false;

	if(root1->data != root2->data) return false;

	return (isIsomorphic(root1->left, root2->left) && isIsomorphic(root1->right, root2->right)) ||
		(isIsomorphic(root1->left, root2->right) && isIsomorphic(root1->right, root2->left));
}

```

---


