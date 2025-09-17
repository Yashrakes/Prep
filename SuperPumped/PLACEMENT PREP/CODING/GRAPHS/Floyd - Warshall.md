
>The [Floyd Warshall Algorithm](http://en.wikipedia.org/wiki/Floyd%E2%80%93Warshall_algorithm) is for solving the All Pairs Shortest Path problem. The problem is to find shortest distances between every pair of vertices in a given edge weighted directed Graph. This algorithm works for both the **directed** and **undirected weighted** graphs and can handle graphs with both **positive** and **negative weight edges**.
>It can also be used to **detect negative cycles**.

#### Metadata

timestamp: **08:47**  &emsp;  **15-07-2021**
topic tags: #graph , #imp, #algo
question link: https://leetcode.com/problems/find-the-city-with-the-smallest-number-of-neighbors-at-a-threshold-distance/
resource: https://www.geeksforgeeks.org/floyd-warshall-algorithm-dp-16/
parent link: [[1. GRAPH GUIDE]], [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]]

---

## Intuition

- Imagine you're planning a road trip across multiple cities. You want to know the shortest path between every pair of cities. One approach would be to calculate each route separately, but that's inefficient.

- The key insight of Floyd-Warshall is this: **If the shortest path from city A to city C passes through city B, then it consists of the shortest path from A to B followed by the shortest path from B to C.**

- This clever observation allows us to build up our solution incrementally by considering potential "intermediate" cities one by one.

---

## Algorithm

1. **Initialize a distance matrix** where each cell [i][j] represents the direct distance from vertex i to j. If there's no direct edge, we use infinity (∞).
2. **Iteratively consider each vertex as a potential intermediate point** in our paths.
3. For each pair of vertices (i, j), we ask: "Would the path from i to j be shorter if we went through vertex k?" If yes, we update our distance.

The magic happens in this core operation:

``` cpp
if (dist[i][j] > dist[i][k] + dist[k][j])
    dist[i][j] = dist[i][k] + dist[k][j]
```

---

## Code

``` cpp
 // Floyd-Warshall algorithm
    void floydWarshall() {
        // Phase 1: Initialize the distance matrix
        // (Already done in constructor and addEdge)
        // if no edge btw i to j, then dist[i][j] is INT_MAX
        
        // Phase 2: Consider each vertex as an intermediate
        for (int k = 0; k < V; k++) {
            // For each pair of vertices (i, j)
            for (int i = 0; i < V; i++) {
                for (int j = 0; j < V; j++) {
                    // Skip if path through k is impossible
                    if (dist[i][k] == INT_MAX || dist[k][j] == INT_MAX)
                        continue;
                    
                    // Update distance if path through k is shorter
                    if (dist[i][j] > dist[i][k] + dist[k][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                    }
                }
            }
        }
        
        // Phase 3: Check for negative cycles (optional)
        // If dist[i][i] becomes negative, there's a negative cycle
        for (int i = 0; i < V; i++) {
            if (dist[i][i] < 0) {
                cout << "Graph contains negative cycle!" << endl;
                return;
            }
        }
    }
```
