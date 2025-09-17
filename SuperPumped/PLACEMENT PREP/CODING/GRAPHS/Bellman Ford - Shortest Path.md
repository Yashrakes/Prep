> Not applicable on graphs having negative cycles.
> Can be applied to only directed graphs, if undirected then convert it to directed by adding another edge.

**_The bellman-Ford algorithm_** helps to find the shortest distance from the source node to all other nodes. But, we have already learned **_Dijkstra's algorithm (_**Dijkstra's algorithm article link**_)_** to fulfill the same purpose. Now, the question is **_how this algorithm is different from Dijkstra's algorithm_**.

While learning Dijkstra's algorithm, we came across the following two situations, where Dijkstra's algorithm failed:
- **_If the graph contains negative edges._**
- **_If the graph has a negative cycle (In this case Dijkstra's algorithm fails to minimize the distance, keeps on running, and goes into an infinite loop. As a result it gives TLE error)._**

**_Bellman-Ford's algorithm_** successfully solves these problems. **_It works fine with negative edges_** as well as **_it is able to detect if the graph contains a negative cycle_**. But this algorithm is only applicable for **_directed graphs_**. In order to apply this algorithm to an undirected graph, we just need to convert the undirected edges into directed edges like the following:

![](https://static.takeuforward.org/wp/uploads/2022/11/Screenshot-2022-11-23-165854.png)

**Explanation:** An undirected edge between nodes u and v necessarily means that there are two opposite-directed edges, one towards node u and the other towards node v. So the above conversion is valid.

After converting the undirected graph into a directed graph following the above method, we can use the Bellman-Ford algorithm as it is.

---

## Algorithm

- In this algorithm, the edges can be given in any order. The intuition is to relax all the edges for N-1( N = no. of nodes) times sequentially. After N-1 iterations, we should have minimized the distance to every node.
- **_Let’s understand what the relaxation of edges means using an example._**

![](https://static.takeuforward.org/wp/uploads/2022/11/Screenshot-2022-11-23-170013.png)

- Let's consider the above graph with dist[u], dist[v], and wt. Here, wt is the weight of the edge and dist[u] signifies the shortest distance to reach node u found until now. Similarly, dist[v] (maybe infinite) signifies the shortest distance to reach node v found until now. If the distance to reach v through u(i.e. dist[u] + wt) is smaller than dist[v], we will update the value of dist[v] with (dist[u] + wt). This process of updating the distance is called the relaxation of edges.
``` cpp
if (dist[u] != 1e8 && dist[u] + wt < dist[v]) {
	dist[v] = dist[u] + wt;
}
```
- **_How to detect a negative cycle in the graph?_**
    - We know if we keep on rotating inside a negative cycle, the path weight will be decreased in every iteration. But according to our intuition, we should have minimized all the distances within N-1 iterations(that means, after N-1 iterations no relaxation of edges is possible). 
    - In order to check the existence of a negative cycle, we will relax the edges one more time after the completion of N-1 iterations. And if in that Nth iteration, it is found that further relaxation of any edge is possible, we can conclude that the graph has a negative cycle. Thus, the Bellman-Ford algorithm detects negative cycles.

---


## Code

``` cpp
include <bits/stdc++.h>
using namespace std;

class Solution {
public:
	/*  Function to implement Bellman Ford
	*   edges: vector of vectors which represents the graph
	*   S: source vertex to start traversing graph with
	*   V: number of vertices
	*/
	vector<int> bellman_ford(int V, vector<vector<int>>& edges, int S) {
		vector<int> dist(V, 1e8);
		dist[S] = 0;
		for (int i = 0; i < V - 1; i++) {
			for (auto it : edges) {
				int u = it[0];
				int v = it[1];
				int wt = it[2];
				if (dist[u] != 1e8 && dist[u] + wt < dist[v]) {
					dist[v] = dist[u] + wt;
				}
			}
		}
		// Nth relaxation to check negative cycle
		for (auto it : edges) {
			int u = it[0];
			int v = it[1];
			int wt = it[2];
			if (dist[u] != 1e8 && dist[u] + wt < dist[v]) {
				return { -1};
			}
		}

		return dist;
	}
};
```


---
