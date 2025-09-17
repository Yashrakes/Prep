#### Metadata

timestamp: **13:26**  &emsp;  **13-07-2021**
topic tags: #graph , #imp , #algo 
question link: https://leetcode.com/problems/redundant-connection/
resource: https://www.geeksforgeeks.org/union-find/
parent link: [[1. GRAPH GUIDE]], [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]]

---

# Redundant Connection

### Question

In this problem, a tree is an **undirected graph** that is connected and has no cycles.

You are given a graph that started as a tree with `n` nodes labeled from `1` to `n`, with one additional edge added. The added edge has two **different** vertices chosen from `1` to `n`, and was not an edge that already existed. The graph is represented as an array `edges` of length `n` where `edges[i] = [ai, bi]` indicates that there is an edge between nodes `ai` and `bi` in the graph.

Return _an edge that can be removed so that the resulting graph is a tree of_ `n` _nodes_. If there are multiple answers, return the answer that occurs last in the input.

>**Example 1:**
![](https://assets.leetcode.com/uploads/2021/05/02/reduntant1-1-graph.jpg)
**Input:** edges = [[1,2],[1,3],[2,3]]
**Output:** [2,3]

---


### Approach 1 : Union Find
#### Complexity Analysis
Note that the implementation of _union()_ and _find()_ is naive and takes O(n) time in the worst case. These methods can be improved to O(Logn) using _Union by Rank or Height_.

#### Code

``` cpp
class ds{
    private:
        int n;
        vector<int> parent;
    
    public:
        ds(int n){
            this->n = n;
            
            //1 indexing, hence pushing a dummy
            parent.push_back(0);
            
            for(int i = 1; i <= n; i++)
                parent.push_back(i);
        }
    
        int find(int u){
            if(u == parent[u])
                return u;
            
            //path compression
            return parent[u] = find(parent[u]);
        }
    
        //make u the parent of v
        void merge(int u, int v){
            parent[v] = u;
        }
};

class Solution {
public:
    /*
    Note: the question demands an edge that is part of a cycle and appears last in the input.
    By definition of the algo, this edge will be the edge that completes the cycle
    Hence the moment we find the cycle, corresponding edge will be our answer
    */
    vector<int> findRedundantConnection(vector<vector<int>>& edges) {
        int n = edges.size();
        
        ds uf(n);
        
        for(int i = 0; i < n; i++){
            int g1 = uf.find(edges[i][0]);
            int g2 = uf.find(edges[i][1]);
            
            //g1 and g2 are group identifiers of the node,
            //if g1 and g2 are same, then edge[][0] and edge[][1] belong to the same group
            //hence cycle found
            if(g1 == g2)
                return edges[i];
            else
                uf.merge(g1, g2);
        }
        
        return {};
    }
};

```

---
### Approach 2 : Based on the idea of Tarjan's Algo

#### Code

``` cpp
class Solution {
public:
    int dfs(vector<vector<int>> &graph, vector<int> &visited, int rank, int node, 
            set<pair<int, int>> &res){
        
        visited[node] = rank;
        
        int rv = INT_MAX, t = INT_MAX;
        for(auto nbr : graph[node]){
            if(!visited[nbr])
                t = dfs(graph, visited, rank+1, nbr, res);
            
            else if(rank > visited[nbr] + 1){
                t = visited[nbr];
            }
            
            
            if(rank > t)
                res.insert({node, nbr});
            
            rv = min(rv, t);           
        }
        
        return rv;
    }
    
    vector<int> findRedundantConnection(vector<vector<int>>& edges) {
        
        //Construct explicit graph
        //A tree has n nodes and n-1 edges, here a tree is converted to a graph by
        //adding an edge, therefore it has n edges --> no. of edges = no. of nodes
        int n = edges.size();
        vector<vector<int>> graph(n+1);
        vector<int> visited(n+1, 0);
        set<pair<int, int>> res;
        
        for(int i = 0; i < n; i++){
            graph[edges[i][0]].push_back(edges[i][1]);
            graph[edges[i][1]].push_back(edges[i][0]);
        }
        
        int t = dfs(graph, visited, 1, 1, res);
        
        for(int i = n-1; i >= 0; i--){
            pair<int, int> p1 = make_pair(edges[i][0], edges[i][1]);
            pair<int, int> p2 = make_pair(edges[i][1], edges[i][0]);
            
            if(res.find(p1) != res.end() || res.find(p2) != res.end())
                return {edges[i][0], edges[i][1]};
        }
        
        return {};
    }
};

```

---

