#### Metadata

resource: https://takeuforward.org/data-structure/articulation-point-in-graph-g-56/
problems:  https://www.geeksforgeeks.org/problems/articulation-point-1/
back links: [[1. GRAPH GUIDE]]
---

Given an undirected connected graph with **V** vertices and adjacency list **adj**. You are required to find all the vertices removing which (and edges through it) disconnects the graph into 2 or more components and return it in sorted manner.  
**Note:** Indexing is zero-based i.e nodes numbering from (0 to V-1). There might be loops present in the graph.

---

## Intuition Behind Finding Articulation Points

Here's the intuitive understanding:

1. We perform a depth-first search (DFS) of the graph, building a DFS tree.
2. For each vertex, we track two values:
    - A discovery time: When the vertex was first encountered during DFS
    - A "low" value: The earliest discovered vertex that can be reached from this vertex's subtree via a back edge
3. A vertex `u` is an articulation point if either:
    - It is the root of the DFS tree and has at least two children
    - It is not the root, and there exists a child `v` such that no vertex in the subtree of `v` has a back edge to an ancestor of `u`

The second condition can be checked by seeing if the lowest discovery time vertex reachable from the subtree of a child `v` is greater than or equal to the discovery time of `u`.

---

## Detailed Explanation of the Algorithm

Let me break down the key parts of this solution and the intuition behind each:

### 1. DFS Traversal and Tree Construction
When we perform DFS, we create a DFS tree where:

- Each vertex is assigned a discovery time when it's first visited
- Edges in the original graph are classified as either:
    - Tree edges: Part of the DFS tree
    - Back edges: Connect a vertex to an ancestor in the DFS tree

### 2. Low Values and Their Meaning
The "low" value of a vertex represents the earliest discovery time of any vertex that can be reached from its subtree. This is crucial because:

- If a vertex's subtree can reach an ancestor through a back edge, it means there's an alternative path
- If no such back edge exists, removing the parent vertex would disconnect the subtree

### 3. Identifying Articulation Points
There are two distinct cases for articulation points:

#### Case 1: The Root Node
If the root of the DFS tree has more than one child, it's an articulation point. Why? Because each child forms a separate subtree, and there's no back edge between these subtrees (otherwise, they'd be part of the same DFS subtree).

``` cpp
if (parent[u] == -1 && children > 1) {
    isAP[u] = true;
}
```

#### Case 2: Non-Root Nodes

A non-root node `u` is an articulation point if there exists a child `v` such that the lowest discovery time reachable from `v`'s subtree is greater than or equal to the discovery time of `u`. This indicates that the subtree can't reach any ancestor of `u` through a back edge.

``` cpp
if (parent[u] != -1 && low[v] >= disc[u]) {
    isAP[u] = true;
}
```

Notice the `>=` comparison (instead of `>` which we used for bridges). This is because we're considering vertex removal rather than edge removal.

### 4. Handling Loops and Multiple Edges
The problem mentions that the graph might have loops (self-edges) and potentially multiple edges between the same pair of vertices. Our implementation handles these naturally:

- Self-loops don't affect articulation points and are effectively ignored
- Multiple edges are handled correctly because we're checking the connectivity, not counting edges

### 5. Time and Space Complexity
- Time Complexity: O(V + E) where V is the number of vertices and E is the number of edges
- Space Complexity: O(V) for the discovery time, low values, parent array, and the result set
---

## Code

``` cpp
class Solution {
  public:
    void dfsUtil(int u, int parent, int &time, vector<int> &discovery, vector<int> &low, vector<int> adj[], set<int> &res) {
        time++;
        discovery[u] = time;
        low[u] = time;
        
        int children = 0;
        for (auto v : adj[u]) {
            // if v is unvisited, initiate dfs
            if (discovery[v] == -1) {
                
                children++;
                dfsUtil(v, u, time, discovery, low, adj, res);
                
                low[u] = min(low[u], low[v]);
                
                // if u is the root node and it has multiple children ie separate
                // subtrees, such that there is no edge connecting the two subtrees 
                // rooted at u
                if (parent == -1 && children > 1) {
                    res.insert(u);
                }
                
                if (parent != -1 && low[v] >= discovery[u]) {
                    res.insert(u);
                }
            } 
            // When v is already visited and v is not the parent of u, it 
            // indicates a back edge connecting u directly to an ancestor v.
            else if (v != parent) {
                low[u] = min(low[u], discovery[v]);
            }
        }
    }
    
    
    vector<int> articulationPoints(int V, vector<int>adj[]) {
        vector<int> discovery(V, -1), low(V, INT_MAX);
        int time = 0;
        set<int> res;
        dfsUtil(0, -1, time, discovery, low, adj, res);
        
        if (res.size() == 0)
	        return {-1};
        
        return vector<int>(res.begin(), res.end());
    }
};
```
