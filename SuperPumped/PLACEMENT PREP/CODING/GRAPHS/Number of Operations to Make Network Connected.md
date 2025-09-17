#### Metadata

timestamp: **13:35**  &emsp;  **13-07-2021**
topic tags: #graph , #imp
question link: https://leetcode.com/problems/number-of-operations-to-make-network-connected/
resource:
parent link: [[1. GRAPH GUIDE]]

---

# Number of Operations to Make Network Connected

### Question

There are `n` computers numbered from `0` to `n-1` connected by ethernet cables `connections` forming a network where `connections[i] = [a, b]` represents a connection between computers `a` and `b`. Any computer can reach any other computer directly or indirectly through the network.

Given an initial computer network `connections`. You can extract certain cables between two directly connected computers, and place them between any pair of disconnected computers to make them directly connected. Return the _minimum number of times_ you need to do this in order to make all the computers connected. If it's not possible, return -1. 

>**Example 1:**
**![](https://assets.leetcode.com/uploads/2020/01/02/sample_1_1677.png)**
**Input:** n = 4, connections = [[0,1],[0,2],[1,2]]
**Output:** 1
**Explanation:** Remove cable between computer 1 and 2 and place between computers 1 and 3.

---
### Strategy

**Approach 1:**
```
We find no of components and no of extra edges, 
if no of extra edges is >= no of components-1,
	then the ans is no of (components - 1)
else 
	Not possible --> -1
```


**Approach 2:**
```
We need at least n - 1 cables to connect all nodes (like a tree).
If connections.size() < n - 1, we can directly return -1.

One trick is that, if we have enough cables,
we don't need to worry about where we can get the cable from.

We only need to count the number of connected components.
To connect two unconnected components, we need to set one cable.

The number of operations we need = the number of connected components - 1
```

---

## Approach 1

- How to find no of connected components?
	- Use disjoint set-union.
	- Let count be the no of connected components initialize with the total no of nodes. For every merge, decrement the count
#### Complexity Analysis

#### Code

``` cpp
class ds{
    private:
        int n;
        vector<int> parent;
    
    public:
        ds(int n){
            this->n = n;
            
            for(int i = 0; i < n; i++)
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
    //Approach 1
    int makeConnected(int n, vector<vector<int>>& connections) {
        if(connections.size() < n-1)
            return -1;
        
        ds uf(n);
        int group_count = n;
        
        for(int i = 0; i < connections.size(); i++){
            int g1 = uf.find(connections[i][0]);
            int g2 = uf.find(connections[i][1]);
            
            if(g1 != g2){
                group_count--;
                uf.merge(g1, g2);
            }
        }
        
        return group_count - 1;
    }
};

```

---


