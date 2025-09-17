#### Metadata

resources: [TUF](https://www.youtube.com/watch?v=HnD676J56ak&list=PLgUwDviBIf0rGEWe64KWas0Nryn7SCRWw&index=20)
https://www.geeksforgeeks.org/prims-minimum-spanning-tree-mst-greedy-algo-5/
parent links: [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]], [[1. GRAPH GUIDE]]
problems: 


---
# Prim's Algorithm for Minimum Spanning Trees

Prim's algorithm is a fundamental graph algorithm used to find the minimum spanning tree (MST) of a connected, weighted graph. Let me walk you through this elegant algorithm from the ground up.

## What is an MST?
- An MST is a subset of edges from a **connected, undirected, weighted** graph that:
	- Connects all vertices (nodes) in the graph
	- Contains no cycles (making it a tree)
	- Has the minimum possible total edge weight among all possible spanning trees
- In simpler terms, it's the cheapest possible way to connect all nodes in a network where each connection has a cost.

---
## Core Intuition
- At its heart, Prim's algorithm follows a simple and intuitive greedy approach:
- Imagine you're building a network of roads connecting cities, and you want to use the minimum total length of road. You start at one city and gradually expand outward, always choosing the shortest possible road that connects a new city to your existing network.
- The algorithm begins with a single vertex and incrementally grows the MST one edge at a time, always selecting the edge with the minimum weight that connects a vertex in the growing MST with a vertex outside it.

---

## Step-by-Step Process

1. **Start at any vertex**: Choose any vertex as the starting point for the MST.
2. **Initialize**: Create two sets - one for vertices already included in the MST, and another for vertices not yet included.
3. **Grow the tree**: In each step, find the minimum-weight edge that connects a vertex in the MST to a vertex outside the MST.
4. **Add to the tree**: Add this edge and the new vertex to the MST.
5. **Repeat**: Continue steps 3-4 until all vertices are included in the MST.

---

## Use Cases

Prim's algorithm shines in scenarios where:

1. **Network Design**: Creating the minimum-cost network that connects all points (like telecommunications networks).
2. **Electrical Grid Planning**: Designing power grids that connect all areas with minimal wiring.
3. **Transportation Planning**: Building road networks with minimum total distance.
4. **Clustering**: In machine learning, minimum spanning trees can be used for clustering algorithms.
5. **Circuit Design**: Minimizing the total wire length in circuit designs.

---

## Code

``` cpp
class Solution {
  public:
    // Function to find sum of weights of edges of the Minimum Spanning Tree.
    int spanningTree(int V, vector<vector<int>> adj[]) {
        // code here
        vector<bool> inMst(V, false);
        vector<int> parent(V, -1);
        vector<int> mstEdgeWeight(V, INT_MAX);
        priority_queue<pair<int, int>, vector<pair<int, int>>, greater<pair<int, int>>> minQ;
        
        mstEdgeWeight[0] = 0;
        minQ.push({0, 0}); // weight, node
        while (!minQ.empty()) {
            int weight = minQ.top().first;
            int node = minQ.top().second;
            minQ.pop();
            
            if (inMst[node]) {
                continue;
            }
            
            inMst[node] = true;
            
            for (auto nbr : adj[node]) {
                int nbrNode = nbr[0];
                int edgeWeight = nbr[1];
                if (!inMst[nbrNode] && edgeWeight < mstEdgeWeight[nbrNode]) {
                    minQ.push({edgeWeight, nbrNode});
                    parent[nbrNode] = node;
                    mstEdgeWeight[nbrNode] = edgeWeight;
                }
            }
        }
        
        int mstCount = 0;
        for (int i = 0; i < V; i++) {
            mstCount += mstEdgeWeight[i];
        }
        
        return mstCount;
    }
};
```

---
