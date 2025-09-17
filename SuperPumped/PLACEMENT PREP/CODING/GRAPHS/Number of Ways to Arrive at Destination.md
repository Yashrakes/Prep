#### Metadata

topic tags: #graph , #imp
question link: https://leetcode.com/problems/number-of-ways-to-arrive-at-destination/description/
resource: https://takeuforward.org/data-structure/g-40-number-of-ways-to-arrive-at-destination/
parent link: [[1. GRAPH GUIDE]]

---
### Question

You are in a city that consists of `n` intersections numbered from `0` to `n - 1` with **bi-directional** roads between some intersections. The inputs are generated such that you can reach any intersection from any other intersection and that there is at most one road between any two intersections.

You are given an integer `n` and a 2D integer array `roads` where `roads[i] = [ui, vi, timei]` means that there is a road between intersections `ui` and `vi` that takes `timei` minutes to travel. You want to know in how many ways you can travel from intersection `0` to intersection `n - 1` in the **shortest amount of time**.

Return _the **number of ways** you can arrive at your destination in the **shortest amount of time**_. Since the answer may be large, return it **modulo** `109 + 7`.

**Example 1:**

![](https://assets.leetcode.com/uploads/2025/02/14/1976_corrected.png)

**Input:** n = 7, roads = [[0,6,7],[0,1,2],[1,2,3],[1,3,3],[6,3,3],[3,5,1],[6,5,1],[2,5,1],[0,4,5],[4,6,2]]
**Output:** 4
**Explanation:** The shortest amount of time it takes to go from intersection 0 to intersection 6 is 7 minutes.
The four ways to get there in 7 minutes are:
- 0 ➝ 6
- 0 ➝ 4 ➝ 6
- 0 ➝ 1 ➝ 2 ➝ 5 ➝ 6
- 0 ➝ 1 ➝ 3 ➝ 5 ➝ 6

---

### Approach 1

- No of ways to reach destination in shortest time = Summation (No of ways to reach destination via its nbrs) and so on.
- Hence for each node, the result is the summation of ways you can reach that node in the shortest path via its nbrs
- So we implement a standard Dijkstra's with an added ways array that keeps track of the no of ways to reach a given node from the source node.
- Important realization is that when we process a nbr node, we are making use of the ways\[parentNode]\,
- This is possible because at this point the parentNode has been popped from the queue, which ensures that we have already reached the parentNode in the shortest amount of time and we have also computed the ways to reach at this point, so it is safe to use this value
#### Complexity Analysis

#### Code
- Time Complexity: O(E Log V)
``` cpp
#define ll long long
class Solution {
public:
    const int MOD = 1e9+7;
    int countPaths(int n, vector<vector<int>>& roads) {

        vector<ll> time(n, LONG_MAX), ways(n, 0);
        vector<vector<pair<int, int>>> graph(n);
        for (auto &road : roads) {
            graph[road[0]].push_back({road[1], road[2]});
            graph[road[1]].push_back({road[0], road[2]});
        }

        priority_queue<pair<ll, ll>, vector<pair<ll, ll>>, greater<pair<ll, ll>>> minQ;
        time[0] = 0;
        ways[0] = 1;
        minQ.push({0, 0});
        while (!minQ.empty()) {
            auto [mins, node] = minQ.top();
            minQ.pop();

            if (mins > time[node]) {
                continue;
            }

            for (auto nbr : graph[node]) {
                ll nbrNode = nbr.first;
                ll newTime = mins + nbr.second;
                // shorter path found, update it
                if (newTime < time[nbrNode]) {
                    time[nbrNode] = newTime;
                    ways[nbrNode] = ways[node];
                    minQ.push({newTime, nbrNode});
                } else if (newTime == time[nbrNode]) {
                    ways[nbrNode] = (ways[nbrNode]%MOD + ways[node]%MOD)%MOD;
                }
            }
        }

        return ways[n-1];
    }
};
```

---
