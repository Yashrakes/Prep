#### Metadata
resource: 
problems:  [[Critical Connections in a Network]]
back links: [1. GRAPH GUIDE](app://obsidian.md/1.%20GRAPH%20GUIDE), [PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE](app://obsidian.md/PLACEMENT%20PREP/CODING/ALGORITHMS/ALGO%20GUIDE)

---

# Tarjan's For Directed Graph

## Intuition Behind the Algorithm

1. **Depth-First Exploration**: The algorithm performs a depth-first search, exploring as far as possible along branches before backtracking.
2. **Discovery and Low-Link Values**: For each vertex, the algorithm keeps track of two values:
    - A "discovery time" (or index) - when the vertex was first discovered during DFS
    - A "low-link value" - the smallest discovery time of any vertex reachable from this vertex through the DFS tree, including itself
3. **SCC Identification**: The key insight is that a vertex is the root of an SCC if and only if its discovery time equals its low-link value. When we find such a vertex, we can extract all vertices on the current recursion stack up to and including this root vertex as a single SCC.
4. **Stack Tracking**: The algorithm maintains a stack of vertices that have been discovered but not yet assigned to any SCC. This stack helps identify all vertices in an SCC once its root is found.

The beauty of this approach is that it identifies SCCs without needing to run separate searches for each potential component.

---

## Code

``` cpp
// Recursive function that finds strongly connected components
void tarjanSCCUtil(int u, std::vector<int> &disc, std::vector<int> &low,
				  std::vector<bool> &stackMember, std::stack<int> &st,
				  int &time) {
	
	// Initialize discovery time and low value
	disc[u] = low[u] = ++time;
	
	// Add current vertex to stack and mark as being on stack
	st.push(u);
	stackMember[u] = true;

	// Go through all vertices adjacent to this
	for (int v : adj[u]) {
		// If v is not visited yet, then recur for it
		if (disc[v] == -1) {
			tarjanSCCUtil(v, disc, low, stackMember, st, time);
			
			// Check if the subtree rooted with v has a connection to
			// one of the ancestors of u
			low[u] = std::min(low[u], low[v]);
		}
		// Update low value of u if v is already in stack (i.e., part of current SCC)
		// This is the back-edge case in DFS
		else if (stackMember[v]) {
			low[u] = std::min(low[u], disc[v]);
		}
	}

	// Head of SCC found, pop the stack and create an SCC
	if (low[u] == disc[u]) {
		std::cout << "SCC: ";
		int w;
		do {
			w = st.top();
			st.pop();
			stackMember[w] = false;
			std::cout << w << " ";
		} while (w != u);
		std::cout << std::endl;
	}
}

void tarjanSCC() {
	// Discovery time for each vertex
	std::vector<int> disc(V, -1);
	
	// Lowest discovery time vertex reachable
	std::vector<int> low(V, -1);
	
	// Is vertex currently on the recursion stack?
	std::vector<bool> stackMember(V, false);
	
	// Stack to keep track of vertices in current DFS tree
	std::stack<int> st;
	
	// Counter for discovery times
	int time = 0;

	// Call the recursive helper function for all undiscovered vertices
	for (int i = 0; i < V; i++) {
		if (disc[i] == -1) {
			tarjanSCCUtil(i, disc, low, stackMember, st, time);
		}
	}
}
```

---
## When to Use Tarjan's Algorithm

You should consider using Tarjan's algorithm when:

1. **You need to find all SCCs in a directed graph.** This is its primary purpose, and it performs this task optimally.
2. **Efficiency is critical.** With O(V + E) time complexity, it's optimal for this problem and outperforms naive approaches.
3. **You're working with large graphs.** The algorithm's efficiency becomes more apparent as graph size increases.
4. **You need to perform topological sorting of SCCs.** The order in which SCCs are discovered can be used to create a condensation graph.
5. **You're solving problems that require identifying cycles in directed graphs.** SCCs represent the "cycles" in a directed graph.
6. **You're implementing algorithms that build upon SCC decomposition.** Many advanced graph algorithms utilize SCC decomposition as a preprocessing step.

---

# Tarjan's For Undirected Graph

>To solve for **Critical Connections in a Network**

While the original Tarjan's algorithm finds strongly connected components in directed graphs, a modified version can identify bridges in undirected graphs. The intuition remains similar:

1. We perform a depth-first search (DFS) of the graph.
2. For each vertex, we track:
    - Its discovery time
    - The earliest discovered vertex that can be reached from this vertex's subtree
3. The key insight: If for an edge (u, v), the lowest discovery time vertex reachable from v (excluding through u) is greater than the discovery time of u, then (u, v) is a bridge.

This works because if there's no alternative path from v back to any ancestor of u, removing the edge (u, v) would disconnect the graph.


``` cpp
class Solution {
public:
    /*
        Below is a solution using Tarjan's Algorith
    */
    void dfs(int node, int parent, int &time, vector<int> &discoveryTime, vector<int> &low, vector<vector<int>> &graph, vector<vector<int>> &res) {
        time += 1;
        discoveryTime[node] = time;
        low[node] = time;

        for (auto nbr : graph[node]) {
            // if nbr is not visited, perform dfs
            if (discoveryTime[nbr] == -1) {
                dfs(nbr, node, time, discoveryTime, low, graph, res);

                low[node] = min(low[node], low[nbr]);

                // If the lowest vertex reachable from subtree under nbr is above node in DFS tree,
                // then node-nbr is a bridge
                if (discoveryTime[node] < low[nbr]) {
                    res.push_back({node, nbr});
                }
            } 
            // Update low value of node for parent function calls
            // Ignore the edge that leads back to the parent of node
            else if (nbr != parent) {
                low[node] = min(low[node], low[nbr]);
            }
        }
    }

    vector<vector<int>> criticalConnections(int n, vector<vector<int>>& connections) {
        vector<vector<int>> graph(n);
        for (auto connection : connections) {
            graph[connection[0]].push_back(connection[1]);
            graph[connection[1]].push_back(connection[0]);
        }

        // Discovery time for each vertex
        vector<int> discoveryTime(n, -1);

        // Lowest discovery time vertex reachable
        vector<int> low(n, INT_MAX);

        vector<vector<int>> res;
        int time = 0;
        dfs(0, -1, time, discoveryTime, low, graph, res);
        return res;
    }
};
```