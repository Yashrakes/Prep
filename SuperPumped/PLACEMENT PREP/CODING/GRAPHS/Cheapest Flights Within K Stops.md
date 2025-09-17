#### Metadata

timestamp: **08:53**  &emsp;  **15-07-2021**
topic tags: #graph , #imp
question link: https://leetcode.com/problems/cheapest-flights-within-k-stops/
resource: [[Network Delay Time]]
parent link: [[1. GRAPH GUIDE]]

---

# Cheapest Flights Within K Stops

### Question

There are `n` cities connected by some number of flights. You are given an array `flights` where `flights[i] = [fromi, toi, pricei]` indicates that there is a flight from city `fromi` to city `toi` with cost `pricei`.

You are also given three integers `src`, `dst`, and `k`, return _**the cheapest price** from_ `src` _to_ `dst` _with at most_ `k` _stops._ If there is no such route, return `-1`.

>**Example 1:**
![](https://s3-lc-upload.s3.amazonaws.com/uploads/2018/02/16/995.png)
**Input:** n = 3, flights = [[0,1,100],[1,2,100],[0,2,500]], src = 0, dst = 2, k = 1
**Output:** 200
**Explanation:** The graph is shown.
The cheapest price from city `0` to city `2` with at most 1 stop costs 200, as marked red in the picture.

---


### Approach 1

- This question is a modification of the dijkstra's algorithm
- In the dijkstra's algo we maintained a priority queue and a visited array and our claim was that the first time an unvisited node had been popped off, we had found the shortest weighted path for it, irrespective of the no. of levels the path consisted. And the subsequent instances of that node in the priority_queue were the second best, third best and so on..
- But in this case, the cost as well as the level of the path is important. Among the cheapest paths we need to find the first one such that the level of the path is <= k+1.
- So if we maintain a visited array, once we obtain the cheapest path of a particular node from the root, if we ignore all the next best paths as we have already marked the node as visited. Therefore, for this problem, using the same approach we can never gurantee that the cheapest path will be of utmost k stops and hence **we do not maintain a visited array**.
- Aim is to process all path possible upto the given limit

#### Complexity Analysis

#### Code
- Time Complexity: **O((K+1) \* E \* log(N * (K+1)))**
``` cpp
class Solution {
public:
    //Modification of dijkstra's algo    
    int findCheapestPrice(int n, vector<vector<int>>& flights, int src, int dst, int k) {
        
        //pair ->node, weight
        vector<vector<pair<int, int>>> graph(n);
        for(int i = 0; i < flights.size(); i++)
            graph[flights[i][0]].push_back({flights[i][1], flights[i][2]});
        
        //weight,node, level
        //queue will be ordered on the basis of weight
        priority_queue<vector<int>, vector<vector<int>>, greater<vector<int>>> pq;
        
        // Optimization for cases where no paths are possible
        // stopsCount[i] is the no of stops required to reach Node i with the cheapest price
        vector<int> stopsCount(n, INT_MAX);
        
        //weight, node, level
        pq.push({0, src, 0});
        
        while(!pq.empty()){
            vector<int> t = pq.top();
            int price = t[0];
            int node = t[1];
            int curLevel = t[2];
            pq.pop();
            
            //If the no of stops have exceed the requirement, then do not push its nbrs
            if(curLevel > k+1) 
                continue;
                                    
            if(node == dst) 
                return price;

            if (curLevel > stopsCount[node])
                continue;
            
            // we have reached the most effecient route to reach city node
            stopsCount[node] = curLevel;
            
            for(auto &nbr : graph[t[1]])
                pq.push({price + nbr.second, nbr.first, curLevel+1});
        }
        
        return -1;
    }
};
```

---

### Approach 2

- Standard BFS, since the max no of edges are fixed
- Time Complexity: O((K+1)×E)
#### Code

``` cpp
class Solution {
public:
    // standard BFS
    int findCheapestPrice(int n, vector<vector<int>>& flights, int src, int dst, int k) {
        vector<vector<pair<int, int>>> graph(n);
        for (auto edge : flights) {
            graph[edge[0]].push_back({edge[1], edge[2]});
        }

        vector<int> prices(n, INT_MAX);
        prices[src] = 0;
        queue<vector<int>> q;
        q.push({src, 0, 0}); // Node, weight, level
        while (!q.empty()) {
            vector<int> t = q.front();
            int node = t[0];
            int price = t[1];
            int curLevel = t[2];
            q.pop();

            if (curLevel > k+1) {
                continue;
            }

            for (auto nbr : graph[node]) {
                int nbrNode = nbr.first;
                int edgeWeight = nbr.second;
                if (curLevel <= k && price + edgeWeight < prices[nbrNode]) {
                    q.push({nbrNode, price + edgeWeight, curLevel + 1});
                    prices[nbrNode] = price + edgeWeight;
                }
            }
        }
        
        return prices[dst] == INT_MAX ? -1 : prices[dst];
    }
};
```
