#### Metadata

timestamp: **14:02**  &emsp;  **12-07-2021**
topic tags: #graph, #imp, #algo
question link: https://leetcode.com/problems/critical-connections-in-a-network/
resource: Notebook, 
parent link: [[1. GRAPH GUIDE]], [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]]

---

# Critical Connections in a Network

### Question

There are `n` servers numbered from `0` to `n - 1` connected by undirected server-to-server `connections` forming a network where `connections[i] = [ai, bi]` represents a connection between servers `ai` and `bi`. Any server can reach other servers directly or indirectly through the network.

A _critical connection_ is a connection that, if removed, will make some servers unable to reach some other server.

Return all critical connections in the network in any order.

>**Example 1:**
**![](https://assets.leetcode.com/uploads/2019/09/03/1537_ex1_2.png)**
>**Input:** n = 4, connections = \[[0,1],[1,2],[2,0],[1,3]]
**Output:** \[[1,3]]
**Explanation:** \[[3,1]] is also accepted.

---


### Approach 1


#### Code

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

                // If the lowest vertex reachable from subtree under nbr is below node in DFS tree,
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

---
