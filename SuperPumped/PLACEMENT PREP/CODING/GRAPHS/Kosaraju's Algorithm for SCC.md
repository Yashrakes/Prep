#### Metadata
resource: https://takeuforward.org/graph/strongly-connected-components-kosarajus-algorithm-g-54/
problems: 
back links: [[1. GRAPH GUIDE]], [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]]

---
## What is Kosaraju's Algorithm?

Kosaraju's algorithm is an elegant and efficient method to find all strongly connected components (SCCs) in a directed graph. A strongly connected component is a maximal subgraph where there exists a path between any two vertices in the subgraph. In simpler terms, within an SCC, you can reach any node from any other node.

---
## Motivation Behind Its Creation

The primary motivation behind Kosaraju's algorithm was to efficiently decompose directed graphs into their fundamental building blocks. Before its development, finding SCCs was a less efficient process. This decomposition is crucial because:

1. It simplifies complex graph analysis by breaking down large graphs into smaller, more manageable components.
2. It enables us to treat each SCC as a single "super vertex," creating a condensed acyclic graph that reveals the high-level structure of the original graph.
3. It provides essential information for solving many graph-related problems, including cycle detection and component connectivity.

---
## Use Cases

Kosaraju's algorithm finds applications in numerous domains:

1. **Connectivity Analysis**: Identifying SCCs helps understand the structure and connectivity patterns in directed networks like social networks, citation networks, or web pages.
2. **Cycle Detection**: Finding SCCs is directly related to finding cycles in directed graphs, as every non-trivial SCC contains at least one cycle.
3. **Graph Condensation**: Creating a condensed graph where each SCC becomes a single node helps visualize the high-level structure of complex networks.
4. **Compiler Design**: In control flow analysis, SCCs help identify loops and recursive function calls.
5. **Dependency Resolution**: When dealing with dependencies (like in package management systems), SCCs identify circular dependencies that need special handling.
6. **Network Analysis**: Identifying bottlenecks or critical components in communication networks.
7. **Bioinformatics**: Analyzing metabolic networks or gene regulatory networks.

---
## Intuition Behind Its Derivation

>[Video Explanation](https://www.youtube.com/watch?v=R6uoSjZ2imo&list=PLgUwDviBIf0oE3gA41TKO2H5bHpPd7fzn&index=54)

The key insight behind Kosaraju's algorithm might seem surprising at first: it uses two depth-first searches (DFS) on the graph, with the second DFS running on the transposed graph (where all edge directions are reversed).

Here's the intuition:

1. **First DFS to establish a "finishing time" ordering**: We perform a standard DFS, keeping track of the order in which nodes finish processing. This ordering has a special property: in the original graph, if there's a path from component A to component B (but not vice versa), the nodes in A will generally finish after the nodes in B.
2. **Transposing the graph**: By reversing all the edges, we maintain the same SCCs, but we break the connections between different SCCs. This is critical because it isolates each SCC.
3. **Second DFS following the finishing time order**: By processing nodes in decreasing order of finishing times from the first DFS, we ensure that we process SCCs in a "bottom-up" manner. When we start DFS from a node, we can only reach other nodes in the same SCC (due to the transposition), so each DFS tree becomes exactly one SCC.
---
## Implementation

``` cpp
 class Solution {
private:
    void dfsWithOrder(int node, vector<bool> &visited, stack<int> &st, vector<vector<int>> &graph) {
        visited[node] = true;
        for (auto nbr : graph[node]) {
            if (!visited[nbr]) {
                dfsWithOrder(nbr, visited, st, graph);
            }
        }
        st.push(node);
    }
    
    vector<vector<int>> transposeGraph(vector<vector<int>> &graph) {
        int n = graph.size();
        vector<vector<int>> revGraph(n);
        for (int i = 0; i < n; i++) {
            for (auto nbr : graph[i]) {
                revGraph[nbr].push_back(i);
            }
        }
        return revGraph;
    }
    
    void dfs(int node, vector<bool> &visited, vector<vector<int>> &graph) {
        visited[node] = true;
        for (auto nbr : graph[node]) {
            if (!visited[nbr]) {
                dfs(nbr, visited, graph);
            }
        }
    }
    
public:
    int kosaraju(vector<vector<int>> &adj) {
        // step 1: dfs with finishing time
        int n = adj.size();
        stack<int> st;
        vector<bool> visited(n, false);
        
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfsWithOrder(i, visited, st, adj);
            }
        }
        
        
        // step 2: reverse graph -> transpose
        vector<vector<int>> revGraph = transposeGraph(adj);
        
        
        // Step 3: Reset visited array for the second DFS
        for (int i = 0; i < n; i++) {
            visited[i] = false;
        }
        
        
        // step 4: normal dfs
        int scc = 0;
        while (!st.empty()) {
            int node = st.top();
            st.pop();
            
            if (!visited[node]) {
                scc++;
                dfs(node, visited, revGraph);
            }
        }
        
        return scc;
    }
    
    
};
```
---
