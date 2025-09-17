#### Metadata

timestamp: **09:08**  &emsp;  **15-07-2021**
topic tags: #graph , #algo, #imp
question link: https://www.codingninjas.com/codestudio/problems/connecting-cities-with-minimum-cost_1386586?leftPanelTab=0
resource: Kruskal's MST ALGO
parent link: [[1. GRAPH GUIDE]], [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]]

---

# Connecting Cities With Minimum Cost

### Question

There are ‘N’ cities numbered from 1 to ‘N’ and ‘M’ roads. Each road connectss two different cities and described as a two-way road using three integers (‘U’, ‘V’, ‘W’) which represents the cost ‘W’ to connect city ‘U’ and city ‘V’ together.
Now, you are supposed to find the minimum cost so that for every pair of cities, there exists a path of connections (possibly of length 1) that connects those two cities together. The cost is the sum of the connection costs used. If the task is impossible, return -1.

##### Input Format :

```
The first line of input contains an integer ‘T’ denoting the number of test cases. Then each test case follows.

The first line of each test case contains two single space-separated integers ‘N’ and ‘M’ denoting the number of cities and roads respectively.

Each of the next ‘M’ lines contains three single space-separated integers ‘U’, ‘V’, and ‘W’ denoting a two-way road between city ‘U’ and ‘V’ of cost ‘W’.
```

##### Output Format :

```
For each test case, return an integer denoting the minimum cost.
```

##### Note :

```
You don't need to print the output, it has already been taken care of. Just implement the given function.
```

##### Constraints :

```
1 <= T <= 50
1 <= N <= 10^4
1 <= M <= 10^4
1 <= W <= 10^3
1 <= U, V <= N

Time Limit: 1 sec
```
---


### Approach

- The question asks to find a subgraph of the graph formed by connecting all the roads such that the net cost is minimum.
- To minimize the cost, we need to remove the redundant roads ie remove roads that form a cycle.
- With this approach, we are essentially finding a minimum spanning tree of the given graph.

- This can be done by applying `kruskal's mst algo using union-find`.

- In order to construct an mst, `we sort the edges on the basis of their weight/cost.`
WE iterate over the edges one by one and include them in our mst if this edge does not lead to a cycle. 

- This can easily be done using the union find algo. 
If the group identifier of two nodes are same then the edge btw those nodes completes a cycle and hence must not be included.
If the identifiers are different we perform the union operation and add the cost to our result.


- Finally we must observe that the mst algorithm works only for a connected graph. 
So if we have a graph that is not connected(a graph having more than one components/groups then its impossible to obtain mst and we return -1.
Again, using the union find operation we can find the no. of groups. If the given graph is connected, then no of groups will be 1



#### Complexity Analysis

#### Code

``` cpp
#include<bits/stdc++.h>
class DisjointSet{
    private:
    	int n;
    	vector<int> parent;
    	int group_count;
    public:
    	DisjointSet(int n){
            this->n = n;
            group_count = n;
            
            for(int i = 0; i <= n; i++)
                parent.push_back(i);
        }
    
    	int find(int u){
            if(u == parent[u])
                return u;
            return parent[u] = find(parent[u]);
        }
    
    	void merge(int u, int v){
            parent[v] = u;
            group_count--;
        }
    
    	int getGroupCount(){
            return group_count;
        }
};


//apply kruskal's MST algo
int getMinimumCost(int n, int m, vector<vector<int>> &connections)
{
	//we need atleast n-1 roads to obtain a valid solution
    if(m < n-1)
        return -1;
    
    //sort in increasing order of cost/weight
    sort(connections.begin(), connections.end(), [](vector<int> &a, vector<int> &b){
        return a[2] < b[2];
    });
    
    DisjointSet ds(n);
    int cost = 0;
    for(int i = 0; i < connections.size(); i++){
        int g1 = ds.find(connections[i][0]);
        int g2 = ds.find(connections[i][1]);
        
        //cycle contributing edge, hence discard
        if(g1 == g2) continue;
        
        ds.merge(g1, g2);
        cost += connections[i][2];
    }
    
    return ds.getGroupCount() > 1 ? -1 : cost;
}

```

---


