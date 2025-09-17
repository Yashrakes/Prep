#### Metadata

timestamp: **15:10**  &emsp;  **24-06-2021**
question link: https://practice.geeksforgeeks.org/problems/height-of-binary-tree/1#
parent link: [[1. TREE GUIDE]]

---

# Height of a Tree

### Question

Given a binary tree, find its height.

---



#### Code1 : Using Level Order

``` cpp
int height(struct Node* node){
	queue<Node*> q;

	q.push(node);
	int level = 0;
	while(!q.empty()){

	  int n = q.size();
	  for(int i = 0; i < n; i++){
		  Node* temp = q.front();
		  q.pop();

		  if(temp->left) q.push(temp->left);
		  if(temp->right) q.push(temp->right);
	  }
	  level++;
	}
	return level;
}
```


#### Code2 : Using Recursion

``` cpp
int height(struct Node* node){
   if(!node) return 0;

   int left = height(node->left);
   int right = height(node->right);

   return max(left, right) + 1;
}

```

---


