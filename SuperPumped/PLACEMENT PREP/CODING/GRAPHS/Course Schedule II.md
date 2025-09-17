#### Metadata

timestamp: **14:21**  &emsp;  **09-07-2021**
topic tags: #graph , #imp, #topological_sort
question link: https://leetcode.com/problems/course-schedule-ii/
resource:
parent link: [[1. GRAPH GUIDE]] , [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]]

---

# Course Schedule II

### Question
There are a total of `numCourses` courses you have to take, labeled from `0` to `numCourses - 1`. You are given an array `prerequisites` where `prerequisites[i] = [ai, bi]` indicates that you **must** take course `bi` first if you want to take course `ai`.

-   For example, the pair `[0, 1]`, indicates that to take course `0` you have to first take course `1`.

Return _the ordering of courses you should take to finish all courses_. If there are many valid answers, return **any** of them. If it is impossible to finish all courses, return **an empty array**.


---

### Approach 1: DFS - Topological Sort only, Soln in approach 2

- We will print the node v only after it goes through to all its descendants. This ensures that anything that follows v will have been added to the output vector before v.
- Initialize a stack and a visited array of size n.
- For each unvisited vertex in the graph, do the following:
    - Call the DFS function with the vertex as the parameter.
    - In the DFS function, mark the vertex as visited and recursively call the DFS function for all unvisited neighbors of the vertex.
    - Once all the neighbors have been visited, push the vertex onto the stack.
- After all, vertices have been visited, pop elements from the stack and append them to the output list until the stack is empty.
- The resulting list is the topologically sorted order of the graph.

#### Code

``` cpp
// C++ program to find topological sort.
#include <bits/stdc++.h>
using namespace std;

// Function to perform DFS and topological sorting
void topologicalSortUtil(int v, vector<vector<int> >& adj,
    vector<bool>& visited, stack<int>& st) {
        
    // Mark the current node as visited
    visited[v] = true;

    // Recur for all adjacent vertices
    for (int i : adj[v]) {
        if (!visited[i])
            topologicalSortUtil(i, adj, visited, st);
    }

    // Push current vertex to stack which stores the result
    st.push(v);
}

// Function to perform Topological Sort
vector<int> topologicalSort(vector<vector<int>>& adj) {
    int V = adj.size();
    
    // Stack to store the result
    stack<int> st; 
    vector<bool> visited(V, false);

    // Call the recursive helper function to store
    // Topological Sort starting from all vertices one by
    // one
    for (int i = 0; i < V; i++) {
        if (!visited[i])
            topologicalSortUtil(i, adj, visited, st);
    }
    
    vector<int> ans;

    // append contents of stack
    while (!st.empty()) {
        ans.push_back(st.top());
        st.pop();
    }
    
    return ans;
}

int main() {

    // Graph represented as an adjacency list
    vector<vector<int>> adj = {{1}, {2}, {}, {1, 2}};

    vector<int> ans = topologicalSort(adj);
    
    for (auto node: ans) {
        cout << node << " ";
    }
    cout << endl;

    return 0;
}

```


---

### Approach 2: Kahns' Algo using inDegree - BFS

- Add all nodes with in-degree **0** to a queue.
- While the queue is not empty:
    - Remove a node from the queue.
    - For each outgoing edge from the removed node, decrement the in-degree of the destination node by **1**
    - If the in-degree of a destination node becomes **0** add it to the queue.
- If the **queue is empty** and there are still nodes in the graph, the **graph contains a cycle** and **cannot be topologically sorted**.
- The nodes in the queue represent the topological ordering of the graph.

#### Code

``` cpp
class Solution {
public:
    /*
    find out the indegree of all the nodes, pick those nodes who have indegree 0...
    */
    vector<int> findOrder(int numCourses, vector<vector<int>>& prerequisites) {
        
        //create a graph
        vector<vector<int>> graph(numCourses);
        vector<int> in(numCourses, 0);
        
        for(auto &pre : prerequisites){
            graph[pre[1]].push_back(pre[0]);
            in[pre[0]]++;
        }
        
        //Push all the nodes with 0 indegree to the queue
        queue<int> q;
        for(int i = 0; i < in.size(); i++)
            if(in[i] == 0)
                q.push(i);
        
        vector<int> res;
        int count = 0;
        
        while(!q.empty()){
            int t = q.front();
            q.pop();
            
            count++;
            res.push_back(t);
            
            //Decrease the indegree of all the adjacent nodes
            for(auto &nbr : graph[t]){
                in[nbr]--;
                
                if(in[nbr] == 0)
                    q.push(nbr);
            }
        }
        
        return count == numCourses ? res : vector<int>();
    }
};

```

---


