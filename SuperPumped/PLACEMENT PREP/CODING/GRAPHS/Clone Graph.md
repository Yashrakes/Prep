#### Metadata

timestamp: **12:41**  &emsp;  **30-06-2021**
question link: https://leetcode.com/problems/clone-graph/
tags: #graph
parent link: [[1. GRAPH GUIDE]]

---

# Clone Graph

### Question
Given a reference of a node in a **[connected](https://en.wikipedia.org/wiki/Connectivity_(graph_theory)#Connected_graph)** undirected graph.

Return a [**deep copy**](https://en.wikipedia.org/wiki/Object_copying#Deep_copy) (clone) of the graph.

Each node in the graph contains a value (`int`) and a list (`List[Node]`) of its neighbors.


---


### Approach
- Note: The trick here is to decide what to do when you come across an already visited node.

#### Code

``` cpp
Node* dfs(Node* root, unordered_map<Node*, Node*>& m){
	if(!root) return root;

	Node* newNode = new Node(root->val);
	m[root] = newNode;

	for(Node* neighbor : root->neighbors)
		if(m.find(neighbor) == m.end())
			newNode->neighbors.push_back(dfs(neighbor, m));
		else
			newNode->neighbors.push_back(m[neighbor]);

	return newNode;
}

Node* cloneGraph(Node* root){
	unordered_map<Node*, Node*> m;
	return dfs(root, m);
}

```

---


