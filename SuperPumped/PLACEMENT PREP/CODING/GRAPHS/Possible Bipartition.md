#### Metadata

timestamp: **15:07**  &emsp;  **13-07-2021**
topic tags: #graph , #imp 
question link: https://leetcode.com/problems/possible-bipartition/
resource:
parent link: [[1. GRAPH GUIDE]]

---

# Possible Bipartition

### Question

Given a set of `n` people (numbered `1, 2, ..., n`), we would like to split everyone into two groups of **any** size.

Each person may dislike some other people, and they should not go into the same group. 

Formally, if `dislikes[i] = [a, b]`, it means it is not allowed to put the people numbered `a` and `b` into the same group.

Return `true` if and only if it is possible to split everyone into two groups in this way.

>**Example 1:**
**Input:** n = 4, dislikes = \[[1,2],[1,3],[2,4]]
**Output:** true
**Explanation**: group1 [1,4], group2 [2,3]

---


### Approach
>**NOTE**: the given question asks us to find whether a graph formed by the given edges is bipartite or not. Any graph containing an odd cycle cannot be a bipartite graph, therefore
>the question could have been modeled in this way too.
>This is also an example of 2-coloring problem

#### Code

``` cpp
/*
Approach:
We need to find if the given question can be modelled to a bipartite graph or not.

We can achieve this by labelling the nodes, no two adjacent nodes can have the same label
(2 color problem).

We do a dfs and assign color to every node, if we come across a visited node and are trying to
assign a new color, then the graph is not bipartite
*/
class Solution {
public:
    bool dfs(vector<vector<int>> &graph, vector<int> &visited, int node, int parentColor){
        //assign the node, the complement of parentcolor
        visited[node] = 1 - parentColor;
        
        bool t = true;
        for(auto &nbr : graph[node]){
            if(visited[nbr] == -1)
                t = t && dfs(graph, visited, nbr, 1-parentColor);
            
            //If visited and the nbr has the same color as the current node then return falsr
            else if(visited[nbr] == 1-parentColor)
                return false;
        }
        
        return t;
    }
    
    bool possibleBipartition(int n, vector<vector<int>>& dislikes) {
        
        vector<vector<int>> graph(n+1);
        for(int i = 0; i < dislikes.size(); i++){
            graph[dislikes[i][0]].push_back(dislikes[i][1]);
            graph[dislikes[i][1]].push_back(dislikes[i][0]);
        }
        
        //visited has three states
        //1. Unvisited = -1
        //2. Group 1 = 0
        //3. Group 2 = 1
        vector<int> visited(n+1, -1);
        bool res = true;
        
        //there can be disconnected components, hence do dfs for all unvisited nodes
        for(int i = 1; i <= n; i++)
            if(visited[i] == -1)
                res = res && dfs(graph, visited, i, 0);
        
        return res;
    }
};

```

---


