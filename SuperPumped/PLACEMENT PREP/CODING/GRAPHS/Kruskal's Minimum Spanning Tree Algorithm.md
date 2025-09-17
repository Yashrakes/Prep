#### Metadata
back Links: [[1. GRAPH GUIDE]], [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]]
problems:  [[Connecting Cities With Minimum Cost]]
resource: https://www.geeksforgeeks.org/kruskals-minimum-spanning-tree-algorithm-greedy-algo-2/

---
## What is Minimum Spanning Tree?

- Given a connected and undirected graph, a _spanning tree_ of that graph is a subgraph that is a tree and connects all the vertices together. 
- A single graph can have many different spanning trees. 
- A _minimum spanning tree (MST)_ or minimum weight spanning tree for a weighted, connected, undirected graph is a spanning tree with a weight less than or equal to the weight of every other spanning tree. The weight of a spanning tree is the sum of weights given to each edge of the spanning tree.  
- A minimum spanning tree has (V – 1) edges where V is the number of vertices in the given graph

---
## Applications
- https://www.geeksforgeeks.org/applications-of-minimum-spanning-tree/

---
## Algorithm

Below are the steps for finding MST using Kruskal’s algorithm:
1. Sort all the edges in non-decreasing order of their weight.   
2. Pick the smallest edge. Check if it forms a cycle with the spanning tree formed so far. If cycle is not formed, include this edge. Else, discard it.
	1. Basically for each edge, of both the nodes have a common parent then discard else merge
3. Repeat step#2 until there are (V-1) edges in the spanning tree.

Step #2 uses the [[DISJOINT SET - UNION FIND]] to detect cycles.

---

## Complexity

- O(ElogE) or O(ElogV). 
- Sorting of edges takes O(ELogE) time. 
- After sorting, we iterate through all edges and apply the find-union algorithm. The find and union operations can take at most O(LogV) time. 
- So overall complexity is O(ELogE + ELogV) time. The value of E can be at most O(V2), so O(LogV) is O(LogE) the same. Therefore, the overall time complexity is O(ElogE) or O(ElogV)

---