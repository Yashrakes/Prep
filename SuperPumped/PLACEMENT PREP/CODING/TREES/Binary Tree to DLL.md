#### Metadata

timestamp: **10:36**  &emsp;  **25-06-2021**
question link: https://practice.geeksforgeeks.org/problems/binary-tree-to-dll/1#
resource: https://www.geeksforgeeks.org/convert-given-binary-tree-doubly-linked-list-set-3/
parent link: [[1. TREE GUIDE]]

---

# Binary Tree to DLL

### Question

Given a Binary Tree (BT), convert it to a Doubly Linked List(DLL) In-Place. The left and right pointers in nodes are to be used as previous and next pointers respectively in converted DLL. The order of nodes in DLL must be same as Inorder of the given Binary Tree. The first node of Inorder traversal (leftmost node in BT) must be the head node of the DLL.

![[Pasted image 20210625103719.png]]


---


### Approach
- Modify the iterative inorder taversal
- After traversal, ensure to point the right of last node to null and the left of first node to null.

#### Code1: Iterative

``` cpp
Node * bToDLL(Node *root)
{
	if(!root) return root;

	stack<Node*> s;
	Node *cur = root;
	Node *dummy = new Node(), *l1 = dummy;

	while(cur || !s.empty()){

		while(cur){
			s.push(cur);
			cur = cur->left;
		}

		cur = s.top();
		s.pop();

		Node *temp = cur->right;
		l1->right = cur;
		cur->left = l1;
		l1 = l1->right;

		cur = temp;
	}

	l1->right = NULL;
	Node *head = dummy->right;
	head->left = NULL;

	return head;
}

```

---

#### Code2 : Recursive

**NOTE**: This solution will not work if there are multiple function calls to this as `static` variable is used.

```cpp
void convert(Node* root, Node **head){
	if(!root) return;

	static Node* prev = NULL;

	convert(root->left, head);

	if(!prev)
		*head = root;
	else{
		root->left = prev;
		prev->right = root;
	}
	prev = root;
	convert(root->right, head);
}

Node * bToDLL(Node *root)
{
	Node *head = NULL;
	convert(root, &head);
	return head;
}


```
