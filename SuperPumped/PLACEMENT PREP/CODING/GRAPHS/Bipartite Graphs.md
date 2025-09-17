#### Metadata
resource: https://www.geeksforgeeks.org/bipartite-graph/
problems: [[Possible Bipartition]]
back links: [[1. GRAPH GUIDE]], [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]]

---

## Intro

A [Bipartite Graph](http://en.wikipedia.org/wiki/Bipartite_graph) is a graph whose vertices can be divided into two independent sets, U and V such that every edge (u, v) either connects a vertex from U to V or a vertex from V to U. In other words, for every edge (u, v), either u belongs to U and v to V, or u belongs to V and v to U. We can also say that there is no edge that connects vertices of same set.

>**NOTE:**
A bipartite graph cannot contain odd cycles. So if we are asked to find if the given graph contains any odd cycle, we can check if its bipartite. If the graph is bipartite, it cannot have any odd cycles.

---

## Algo

One approach is to check whether the graph is 2-colorable or not using [backtracking algorithm m coloring problem](https://www.geeksforgeeks.org/backttracking-set-5-m-coloring-problem/).   
Following is a simple algorithm to find out whether a given graph is Birpartite or not using Breadth First Search (BFS).   
1. Assign RED color to the source vertex (putting into set U).   
2. Color all the neighbors with BLUE color (putting into set V).   
3. Color all neighbor’s neighbor with RED color (putting into set U).   
4. This way, assign color to all vertices such that it satisfies all the constraints of m way coloring problem where m = 2.   
5. While assigning colors, if we find a neighbor which is colored with same color as current vertex, then the graph cannot be colored with 2 vertices (or graph is not Bipartite)


The above algo can be implemented by using both BFS and DFS. A DFS approach is discussed here [[Possible Bipartition]].

---
