#### Metadata

timestamp: **14:44**  &emsp;  **08-07-2021**
topic tags: #graph , #imp 
question link: https://leetcode.com/problems/network-delay-time/
resource: See notebook
parent link: [[1. GRAPH GUIDE]], [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]]

---

# Network Delay Time

### Question

You are given a network of `n` nodes, labeled from `1` to `n`. You are also given `times`, a list of travel times as directed edges `times[i] = (ui, vi, wi)`, where `ui` is the source node, `vi` is the target node, and `wi` is the time it takes for a signal to travel from source to target.

We will send a signal from a given node `k`. Return the time it takes for all the `n` nodes to receive the signal. If it is impossible for all the `n` nodes to receive the signal, return `-1`.

**Input:** times = \[[2,1,1],[2,3,1],[3,4,1]], n = 4, k = 2
**Output:** 2

---


### Approach

- Time:  O(E log V) in the heap implementation
#### Code

``` cpp
class Solution {
public:
    int networkDelayTime(vector<vector<int>>& times, int n, int k) {
        
        //We need to create an explicit graph
        //Adjacency list, pair<target, weight>
        vector<vector<pair<int, int>>> graph(n+1);
        for(auto &time : times)
            graph[time[0]].push_back({time[1], time[2]});
        
        
        //Weighted BFS, using priority queue -->min heap
        //Every elt is pair <- first as the weight, second as the node number
        //weight is first, since we need ordering based on te weight
        priority_queue<pair<int, int>, vector<pair<int, int>>, greater<pair<int, int>>> pq;
        
        
        vector<bool> visited(n+1, false);
        int max_weight = 0;
        int counter = 0; //to check the no of nodes visited
        
        
        //Push the starting node
        pq.push({0, k});
        while(!pq.empty()){
            
            int cur_weight = pq.top().first;
            int node = pq.top().second;
            pq.pop();
            
            //Ignore already visited nodes
            if(visited[node]) continue;
            
            //Shortest path to this node has been computed
            max_weight = max(max_weight, cur_weight);
            visited[node] = true;
            counter++;
            
            //Push the unvisited nbrs of node in the queue
            for(auto &nbr : graph[node]){
                if(!visited[nbr.first]){
                    pq.push({nbr.second + cur_weight, nbr.first});
                }
            }
        }
        
        return counter == n ? max_weight : -1;
    }
};
```

---


