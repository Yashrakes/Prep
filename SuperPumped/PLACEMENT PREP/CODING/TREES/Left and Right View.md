#### Metadata

timestamp: **16:15**  &emsp;  **24-06-2021**
question link: https://practice.geeksforgeeks.org/problems/left-view-of-binary-tree/1
https://practice.geeksforgeeks.org/problems/right-view-of-binary-tree/1
parent link: [[1. TREE GUIDE]]

---

# Left and Right View of a Binary Tree

### Question
Print left and right view of a tree.

**NOTE**:  This can easily be solved using conventional level order. Below is the recursive approach.

---


#### Code : Left View

``` cpp
void util(vector<int>& res,int level, int *maxLevel, Node* root){
	if(!root) return;

	if(level > *maxLevel){
		res.push_back(root->data);
		*maxLevel = level;
	}
	
	util(res, level+1, maxLevel, root->left);
	util(res, level+1, maxLevel, root->right);
}

vector<int> leftView(Node *root)
{
   if(!root) return {};

	vector<int> res;
	int maxlevel = 0;
	util(res, 1, &maxlevel, root);
	return res;
}

```


#### Code : Right View

``` cpp
void util(vector<int>& res,int level, int *maxLevel, Node* root){
	if(!root) return;

	if(level > *maxLevel){
		res.push_back(root->data);
		*maxLevel = level;
	}

	util(res, level+1, maxLevel, root->right);
	util(res, level+1, maxLevel, root->left);
}

vector<int> rightView(Node *root)
{
   if(!root) return {};

	vector<int> res;
	int maxlevel = 0;
	util(res, 1, &maxlevel, root);
	return res;
}

```


---


