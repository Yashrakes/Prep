#### Metadata

timestamp: **18:00**  &emsp;  **24-06-2021**
question link:
https://practice.geeksforgeeks.org/problems/top-view-of-binary-tree/1#
https://practice.geeksforgeeks.org/problems/bottom-view-of-binary-tree/1#
resource:
https://www.geeksforgeeks.org/print-nodes-top-view-binary-tree/
https://www.geeksforgeeks.org/bottom-view-binary-tree/
parent link: [[1. TREE GUIDE]]

---

# Top View

### Question
Given below is a binary tree. The task is to print the top view of binary tree. Top view of a binary tree is the set of nodes visible when the tree is viewed from the top. For the given below tree

---


### Approach
- We use [[Vertical Order]] using level order.
- All we have to do is store the first node for each unique horizontal distance.
- horizontal distance of root is 0.

#### Code

``` cpp
vector<int> topView(Node *root)
{
	int hd = 0;
	queue<pair<Node*, int>> q;
	map<int, int> m;

	q.push(make_pair(root, hd));
	while(!q.empty()){

		pair<Node*, int> temp = q.front();
		q.pop();

		hd = temp.second;
		Node* cur = temp.first;

		if(m.find(hd) == m.end()) m[hd] = cur->data;

		if(cur->left) q.push(make_pair(cur->left, hd-1));
		if(cur->right) q.push(make_pair(cur->right, hd+1));
	}

	vector<int> res;
	for(auto it : m)
		res.push_back(it.second);

	return res;
}

```

---





# Bottom View

### Question
Given a binary tree, print the bottom view from left to right.  
A node is included in bottom view if it can be seen when we look at the tree from bottom.

``` 				  
					  20  
                    /    \  
                  8       22  
                /   \        \  
              5      3       25  
                    /   \        
                  10    14
				  
```

For the above tree, the bottom view is 5 10 3 14 25.  
If there are **multiple** bottom-most nodes for a horizontal distance from root, then print the later one in level traversal. For example, in the below diagram, 3 and 4 are both the bottom most nodes at horizontal distance 0, we need to print 4.

```
 					  20  
                    /    \  
                  8       22  
                /   \     /   \  
              5      3 4     25  
                     /    \        
                 10       14

```

For the above tree the output should be 5 10 4 14 25.

---


### Approach
- [[Vertical Order]] can be used. Only modification is to pick the last element from the vector for a particular hd;

#### Code1 : Using Vertical Order

``` cpp
vector <int> bottomView(Node *root)
{
    if(!root) return {};
    
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
    
    vector<int> res;
    for(auto it : m){
        int n = it.second.size();
        res.push_back(it.second[n-1]);
    }
    return res;        
}

```

---

#### Code2 : Without Queue
Create a map, where key is the horizontal distance and value is a pair(a, b) where a is the value of the node and b is the height of the node. Perform a pre-order traversal of the tree. If the current node at a horizontal distance of h is the first we’ve seen, insert it in the map. Otherwise, compare the node with the existing one in map and if the height of the new node is greater, update in the Map.

``` cpp
void preorder(map<int, pair<int, int>> &m, Node* root, int h, int d){
    
    if(!root) return;
    
    if(m.find(h) == m.end())
        m[h] = make_pair(root->data, d);
    else if(d >= m[h].second)
        m[h] = make_pair(root->data, d);
        
    preorder(m, root->left, h-1, d+1);
    preorder(m, root->right, h+1, d+1);
}

vector <int> bottomView(Node *root)
{
    if(!root) return {};
    
    //key is hd, value.first is Node value
    //value.second is depth/height
    map<int, pair<int, int>> m;
    preorder(m, root, 0, 0);
    
    vector<int> res;
    for(auto it : m){
        res.push_back(it.second.first);
    }
    return res;        
}

```

---