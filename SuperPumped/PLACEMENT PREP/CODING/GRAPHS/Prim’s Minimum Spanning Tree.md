#### Metadata
back links: [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]], [[1. GRAPH GUIDE]], [[Optimize Water distribution in a village]]
resources: 
https://www.geeksforgeeks.org/prims-minimum-spanning-tree-mst-greedy-algo-5/
https://www.geeksforgeeks.org/prims-mst-for-adjacency-list-representation-greedy-algo-6/
problem: [[Optimize Water distribution in a village]]

---

## Introduction

Prim’s algorithm is also a [Greedy algorithm](https://www.geeksforgeeks.org/archives/18528). It starts with an empty spanning tree. The idea is to maintain two sets of vertices. The first set contains the vertices already included in the MST, the other set contains the vertices not yet included. At every step, it considers all the edges that connect the two sets, and picks the minimum weight edge from these edges. After picking the edge, it moves the other endpoint of the edge to the set containing MST.

A group of edges that connects two set of vertices in a graph is called [cut in graph theory](http://en.wikipedia.org/wiki/Cut_%28graph_theory%29). _So, at every step of Prim’s algorithm, we find a cut (of two sets, one contains the vertices already included in MST and other contains rest of the vertices), pick the minimum weight edge from the cut and include this vertex to MST Set (the set that contains already included vertices)._

#### How does Prim’s Algorithm Work?
The idea behind Prim’s algorithm is simple, a spanning tree means all vertices must be connected. So the two disjoint subsets (discussed above) of vertices must be connected to make a _Spanning_ Tree. And they must be connected with the minimum weight edge to make it a _Minimum_ Spanning Tree.