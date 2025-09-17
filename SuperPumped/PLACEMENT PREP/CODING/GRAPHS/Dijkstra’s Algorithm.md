### Question

Given a weighted, undirected and connected graph where you have given [adjacency list](https://www.geeksforgeeks.org/adjacency-list-meaning-definition-in-dsa/ "adjacency list") **adj.** You have to find the shortest distance of all the vertices from the source vertex **src**, and return a list of integers denoting the shortest distance between **each node** and source vertex **src**.

**Note:** The Graph doesn't contain any negative weight edge.

---

### Approach

>**Note**: Dijkstra’s cannot be applied to a graph with negative weights

#### Why we can be certain that once a node is popped from the priority queue, we've found the shortest path to it?

- The crucial property that makes Dijkstra's work is this: **when you select the node with the minimum distance from the priority queue, that distance is guaranteed to be the shortest possible to that node**.

- To understand why, let's think about what the priority queue represents. It contains nodes that we've discovered but haven't finalized yet, ordered by their current best-known distance from the source.

- When we pop a node from the queue, we're saying: *"Of all the nodes we've discovered so far, this one has the smallest distance from the source."* If there were a **shorter path to this node**, it would involve passing through another node that's already in our queue - **but that's impossible** because we always **pick the node with the smallest distance first**.

- Time:  O(E log V) in the heap implementation

#### Important Observations:

- As stated above, one a node is popped, we are guaranteed, that we have determined the shorted path to reach that node.
- Secondly, the use of visited node is an optimization, for a given node, we can have multiple entries in the priority queue, when a node is popped, we essentially found the shortest path to reach here, and potentially the second best, third best and so on are still present in queue.
	- So if we have already found the shortest path, there is no point in exploring the second / third best path and its Nbrs, hence using the visited array, we can avoid these computations.
- Also, understand when a priority queue is required and when a queue will suffice because both approaches have tradeoffs it terms of time complexity. If operations are larger a normal queue can prove to be more efficient in comparison with a priority queue
#### Code

``` cpp
class Solution {
  public:
    // Function to find the shortest distance of all the vertices
    // from the source vertex src.
    vector<int> dijkstra(vector<vector<pair<int, int>>> &adj, int src) {
        int nodes = adj.size();
        vector<int> distances(nodes, INT_MAX);
        vector<bool> visited(nodes, false);
        priority_queue<pair<int, int>, vector<pair<int, int>>, greater<pair<int, int>>> minQ;
        
        distances[src] = 0;
        minQ.push({0, src});
        while (!minQ.empty()) {
            int distance = minQ.top().first;
            int curNode = minQ.top().second;
            minQ.pop();
            
            if (visited[curNode])
                continue;
            
            visited[curNode] = true;
                
            for (auto nbr : adj[curNode]) {
                int nbrNode = nbr.first;
                int weight = nbr.second;
                
                if (!visited[nbrNode] &&  distances[curNode] + weight < distances[nbrNode]) {
                    distances[nbrNode] = distances[curNode] + weight;
                    minQ.push({distances[nbrNode], nbrNode});
                }
            }
        }
        return distances;
    }
};    
```

---


