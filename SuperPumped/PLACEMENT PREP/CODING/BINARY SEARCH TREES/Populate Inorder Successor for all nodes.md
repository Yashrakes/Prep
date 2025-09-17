#### Metadata

timestamp: **19:43**  &emsp;  **09-07-2021**
topic tags: #bst, #imp
question link: https://practice.geeksforgeeks.org/problems/populate-inorder-successor-for-all-nodes/1#
resource: https://www.geeksforgeeks.org/populate-inorder-successor-for-all-nodes/
parent link: [[1. BST GUIDE]]

---

# Populate Inorder Successor for all nodes

### Question

Given a Binary Tree, write a function to populate next pointer for all nodes. The next pointer for every node should be set to point to inorder successor.

---


### Approach : Reverse Inorder

#### Code

``` cpp
void f(Node *root, Node* &succ)
{
	if(!root) return;

	f(root->right, succ);

	root->next = succ;
	succ = root;

	f(root->left, succ);
}

void populateNext(Node *root)
{
	Node* temp = NULL;
	f(root, temp);
}

```

---


