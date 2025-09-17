#### Metadata

timestamp: **22:05**  &emsp;  **25-06-2021**
tags: #imp , #binary_tree 
question link: https://www.geeksforgeeks.org/print-k-sum-paths-binary-tree/
parent link: [[1. TREE GUIDE]]

---

# Print all k-sum paths in a binary tree

### Question

A binary tree and a number k are given. Print every path in the tree with sum of the nodes in the path as k.  
A path can start from any node and end at any node and must be downward only, i.e. they need not be root node and leaf node; and negative numbers can also be there in the tree.

---


### Approach

- The basic idea to solve the problem is to do a preorder traversal of the given tree. We also need a container (vector) to keep track of the path that led to that node. At each node we check if there are any path that sums to k, if any we print the path and proceed recursively to print each path.
- We can ensure unique paths are generated as in each traversal we try to **find the valid path ending at that particular node**. Its important to realize this.

#### Code

``` cpp
void printVector(const vector<int>& v, int i)
{
	for (int j=i; j<v.size(); j++)
		cout << v[j] << " ";
	cout << endl;
}


void printKPathUtil(Node *root, vector<int>& path, int k)
{
	if(!root) return;
	
	path.push_back(root->data);
	
	printKPathUtil(root->left, path, k);
	printKPathUtil(root->right, path, k);
	
	int n = path.size(), sum = 0;
	for(int j = n-1; j >= 0; j--){
	    sum += path[j];
	    if(sum == k)
	        printVector(path, j);
	}
	
	path.pop_back();
}

void printKPath(Node *root, int k)
{
	vector<int> path;
	printKPathUtil(root, path, k);
}

```

---


