#### Metadata

timestamp: **15:13**  &emsp;  **26-07-2021**
topic tags: #binary_tree 
question link: https://www.interviewbit.com/old/problems/maximum-edge-removal/
resource: https://www.geeksforgeeks.org/maximum-edge-removal-tree-make-even-forest/
parent link: [[1. TREE GUIDE]]

---

# Maximum Edge Removal

### Question
Given an undirected tree with an **even** number of nodes. Consider each connection between a parent and child node to be an edge.

You need to remove **maximum** number of these edges, such that the disconnected subtrees that remain each have an **even** number of nodes.

Return the **maximum** number of edges you can remove.


---


### Approach

As we need connected components that have even number of vertices so when we get one component we can remove the edge that connects it to the remaining tree and we will be left with a tree with even number of vertices which will be the same problem but of smaller size, we have to repeat this algorithm until the remaining tree cannot be decomposed further in the above manner. 

We will traverse the tree using [DFS](https://www.geeksforgeeks.org/depth-first-traversal-for-a-graph/) which will return the number of vertices in the component of which the current node is the root. If a node gets an even number of vertices from one of its children then the edge from that node to its child will be removed and result will be increased by one and if the returned number is odd then we will add it to the number of vertices that the component will have if the current node is the root of it.


#### Complexity Analysis
- Time: O(V+E)

#### Code

``` cpp
int dfs(vector<vector<int>> &graph, vector<bool> &visited, int node, int &count){
    int currComponentNode  = 0;

    visited[node] = true;
    for(auto &nbr : graph[node]){
        if(!visited[nbr]){
		
			// Count the number of nodes in a subtree
            int subtreeNodeCount = dfs(graph, visited, nbr, count);
            
			// if returned node count is even, disconnect
            // the subtree and increase result by one.
            if(subtreeNodeCount%2 == 0)
                count++;
            else
                currComponentNode  += subTree;
    
        }
    }
	
	// number of nodes in current component and one for
    // current node
    return currComponentNode +1;
}

int Solution::solve(int A, vector<vector<int> > &B) {

    //construct graph, undirected
    vector<vector<int>> graph(A+1);
    for(auto edge : B){
        graph[edge[0]].push_back(edge[1]);
        graph[edge[1]].push_back(edge[0]);
    }

    vector<bool> visited(A+1, false);
    int count = 0;
    int t = dfs(graph, visited, 1, count);
    return count;
}

```

---


