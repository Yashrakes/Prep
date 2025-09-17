#### Metadata

timestamp: **19:37**  &emsp;  **24-06-2021**
question link: https://practice.geeksforgeeks.org/problems/zigzag-tree-traversal/1#
resource: https://www.geeksforgeeks.org/zigzag-tree-traversal/
parent link: [[1. TREE GUIDE]]

---

# ZigZag Level Order

### Question
Given a Binary Tree. Find the Zig-Zag Level Order Traversal of the Binary Tree.

**Example:**
```
**Input:** 
		   7
        /     \
       9       7
     /  \     /   
    8    8   6     
   /  \
  10   9 

```
**Output:** 7 7 9 8 8 6 9 10

---


### Approach

#### Code 1
- We can solve this problem by using a conventional level order traversal and a stack.
- Whenever we are required to print the reverse of a particular level, we store the values of that level in a stack.

``` cpp
vector <int> zigZagTraversal(Node* root)
{
    if(!root) return {};
    
	queue<Node*> q;
	vector<int> res;
	
	q.push(root);
	int level = 0;
	while(!q.empty()){
	    
	    stack<int> s;
	    int n = q.size();
	    for(int i = 0; i < n; i++){
	        Node* cur = q.front();
	        q.pop();
	        
	        if(level%2 == 0)
	            res.push_back(cur->data);
	        else
	            s.push(cur->data);
	        
	        if(cur->left) q.push(cur->left);
	        if(cur->right) q.push(cur->right);
	    }
	    
	    while(!s.empty()){
	        res.push_back(s.top());
	        s.pop();
	    }
	    
	    level++;
	    
	}
	return res;
}

```

#### Code 2

``` cpp


```

---


