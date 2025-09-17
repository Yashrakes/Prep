#### Metadata

timestamp: **17:47**  &emsp;  **24-06-2021**
question link: https://practice.geeksforgeeks.org/problems/print-a-binary-tree-in-vertical-order/1#
resource: https://www.geeksforgeeks.org/print-a-binary-tree-in-vertical-order-set-3-using-level-order-traversal/
parent link: [[1. TREE GUIDE]]

---

# Vertical Order

### Question
Given a Binary Tree, find the vertical traversal of it starting from the leftmost level to the rightmost level.  
If there are multiple nodes passing through a vertical line, then they should be printed as they appear in **level order** traversal of the tree.

**Example 1:**
**Explanation:**
![](https://media.geeksforgeeks.org/img-practice/ScreenShot2021-05-28at3-1622541589.png)
**Output:** 
4 2 1 5 6 3 8 7 9 

**Your Task:**  
You don't need to read input or print anything. Your task is to complete the function **verticalOrder()** which takes the root node as input parameter and returns an array containing the vertical order traversal of the tree from the leftmost to the rightmost level. If 2 nodes lie in the same vertical level, they should be printed in the order they appear in the level order traversal of the tree.


---


### Approach
- The given approach is using level order traversal. Check GFG for alternative approaches.
- HD for root is 0

#### Complexity Analysis
Time Complexity of the above implementation is O(n Log n). Note that the above implementation uses a map which is implemented using self-balancing BST.  
We can reduce the time complexity to O(n) using unordered_map. To print nodes in the desired order, we can have 2 variables denoting min and max horizontal distance. We can simply iterate from min to max horizontal distance and get corresponding values from Map. So it is O(n)  
Auxiliary Space: O(n)

#### Code

``` cpp
void helper(vector<int>& res, Node * root){

	map<int, vector<int>> m;
	queue<pair<Node*, int>> q;

	int hd = 0;
	q.push(make_pair(root, 0));

	while(!q.empty()){

		int n = q.size();
		for(int i = 0; i < n; i++){

			pair<Node*, int> temp = q.front();
			q.pop();

			hd = temp.second;
			Node* cur = temp.first;

			m[hd].push_back(cur->data);

			if(cur->left) q.push(make_pair(cur->left, hd-1));
			if(cur->right) q.push(make_pair(cur->right, hd+1));
		}
	}

	for(auto it : m)
		for(auto ele : it.second)
			res.push_back(ele);
}

vector<int> verticalOrder(Node *root)
{
	vector<int> res;
	helper(res, root);
	return res;
}
```

---


