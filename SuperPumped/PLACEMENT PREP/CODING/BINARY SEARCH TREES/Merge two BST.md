#### Metadata

timestamp: **11:42**  &emsp;  **10-07-2021**
topic tags: #bst, #imp 
question link: https://practice.geeksforgeeks.org/problems/merge-two-bst-s/1#
resource: https://www.geeksforgeeks.org/merge-two-bsts-with-limited-extra-space/
parent link: [[1. BST GUIDE]]

---

# Merge two BST

### Question

Given two BSTs, return elements of both BSTs in **sorted** form.

---


### Approach

The idea is to use [iterative inorder traversal](https://www.geeksforgeeks.org/inorder-tree-traversal-without-recursion/). We use two auxiliary stacks for two BSTs. Since we need to print the elements in the sorted form, whenever we get a smaller element from any of the trees, we print it. If the element is greater, then we push it back to stack for the next iteration.

#### Complexity Analysis
**Time Complexity:** O(M+N) where M and N are the sizes if the two BSTs.  
**Auxiliary Space:** O(Height of BST1 + Height of BST2).
#### Code 1

``` cpp
void storeInorder(Node* root, vector<int> &inorder){
	if(!root) return;

	storeInorder(root->left, inorder);
	inorder.push_back(root->data);
	storeInorder(root->right, inorder);
}

vector<int> merge(Node *root1, Node *root2)
{
   vector<int> res;

   if(!root1){
	   storeInorder(root2, res);
	   return res;
   }

   if(!root2){
	   storeInorder(root1, res);
	   return res;
   }

   stack<Node*> s1, s2;
   Node *cur1 = root1, *cur2 = root2;

   while(cur1 || cur2 || !s1.empty() || !s2.empty()){

	   if(cur1 || cur2){
		   if(cur1){
			   s1.push(cur1);
			   cur1 = cur1->left;
		   }

		   if(cur2){
			   s2.push(cur2);
			   cur2 = cur2->left;
		   }
	   }
	   //both cur1 and cur2 are null
	   else{

		   if(!s1.empty() && !s2.empty()){
			   cur1 = s1.top();
			   cur2 = s2.top();
			   s1.pop();
			   s2.pop();

			   if(cur1->data < cur2->data){
				   res.push_back(cur1->data);
				   cur1 = cur1->right;
				   s2.push(cur2);
				   cur2 = NULL;
			   }
			   else{
				   res.push_back(cur2->data);
				   cur2 = cur2->right;
				   s1.push(cur1);
				   cur1 = NULL;
			   }
		   }
		   else if(s1.empty()){
			   cur2 = s2.top();
			   s2.pop();
			   res.push_back(cur2->data);
			   cur2 = cur2->right;
		   }
		   else if(s2.empty()){
			   cur1 = s1.top();
			   s1.pop();
			   res.push_back(cur1->data);
			   cur1 = cur1->right;
		   }

	   }
   }
   return res;
}

```

---
#### Code 2

``` cpp
void storeInorder(Node* root, vector<int> &inorder){
	if(!root) return;

	storeInorder(root->left, inorder);
	inorder.push_back(root->data);
	storeInorder(root->right, inorder);
}

vector<int> merge(Node *root1, Node *root2)
{
   vector<int> res;

   if(!root1){
	   storeInorder(root2, res);
	   return res;
   }

   if(!root2){
	   storeInorder(root1, res);
	   return res;
   }

   stack<Node*> s1, s2;
   Node *cur1 = root1, *cur2 = root2;

   while(cur1 || cur2 || !s1.empty() || !s2.empty()){

	   if(cur1 || cur2){
		   if(cur1){
			   s1.push(cur1);
			   cur1 = cur1->left;
		   }

		   if(cur2){
			   s2.push(cur2);
			   cur2 = cur2->left;
		   }
	   }
	   //both cur1 and cur2 are null
	   else{
		   if(s1.empty()) {
				while (!s2.empty()) {
					cur2 = s2.top();
					s2.pop();
					cur2->left = NULL;
					storeInorder(cur2, res);
				}
				return res;
			}

			if(s2.empty()) {
				while (!s1.empty()) {
					cur1 = s1.top();
					s1.pop();
					cur1->left = NULL;
					storeInorder(cur1, res);
				}
				return res;
			}


		   cur1 = s1.top();
		   cur2 = s2.top();
		   s1.pop();
		   s2.pop();

		   if(cur1->data < cur2->data){
			   res.push_back(cur1->data);
			   cur1 = cur1->right;
			   s2.push(cur2);
			   cur2 = NULL;
		   }
		   else{
			   res.push_back(cur2->data);
			   cur2 = cur2->right;
			   s1.push(cur1);
			   cur1 = NULL;
		   }
	   }
   }
   return res;
}

```

---


