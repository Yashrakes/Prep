#### Metadata

timestamp: **19:08**  &emsp;  **03-07-2021**
topic tags: #graph , #imp 
question link: https://leetcode.com/problems/find-eventual-safe-states/	
resource: See the notebook
parent link: [[1. GRAPH GUIDE]]

---

# Find Eventual Safe States

### Question
We start at some node in a directed graph, and every turn, we walk along a directed edge of the graph. If we reach a terminal node (that is, it has no outgoing directed edges), we stop.

We define a starting node to be **safe** if we must eventually walk to a terminal node. More specifically, there is a natural number `k`, so that we must have stopped at a terminal node in less than `k` steps for **any choice of where to walk**.

Return _an array containing all the safe nodes of the graph_. The answer should be sorted in **ascending** order.

The directed graph has `n` nodes with labels from `0` to `n - 1`, where `n` is the length of `graph`. The graph is given in the following form: `graph[i]` is a list of labels `j` such that `(i, j)` is a directed edge of the graph, going from node `i` to node `j`.


`Summarizing: A node is safe, if all the paths from that node lead to a terminal node.`

---


### Approach

#### Code

``` cpp
/* 
A node is safe, if all the paths from that node lead to a terminal node.

Therefore, all the nodes that are a part of a cycle or are directed towards a cycle
are unsafe.

WE find all the unsafe nodes first, once we have found all the unsafe node, 
the remaining are safe.
*/
int hasCycle(vector<vector<int>>& graph, vector<int>& visited, vector<bool>& path, int n){

	path[n] = true;

	//Safe is 1, Unsafe is 2, not visited is 0
	int safe = 1, t;
	for(auto nbr : graph[n]){
		//Cycle found, hence parent unsafe
		if(path[nbr])
			safe = 2;

		else if(!visited[nbr]){
			t = hasCycle(graph, visited, path, nbr);
			//if child is unsafe, parent is unsafe
			if(t == 2)
				safe = 2;
		}

		//if child is unsafe, parent is unsafe
		else if(visited[nbr] == 2)
			safe = 2;
	}

	path[n] = false;
	visited[n] = safe;

	return safe;
}

vector<int> eventualSafeNodes(vector<vector<int>>& graph) {
	int numNodes = graph.size();

	//visited array also tracks if the node is safe or not
	vector<int> visited(numNodes, 0);
	vector<bool> path(numNodes, 0);

	int t;
	for(int i = 0; i < numNodes; i++)
		if(!visited[i])
			t = hasCycle(graph, visited, path, i);

	vector<int> res;
	//all nodes that are marked as 1 are safe in the visited array
	for(int i = 0; i < numNodes; i++)
		if(visited[i] == 1)
			res.push_back(i);

	return res;
}

```

---


