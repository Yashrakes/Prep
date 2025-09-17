#### Metadata
Resource: https://www.geeksforgeeks.org/union-find-algorithm-set-2-union-by-rank/
parent links: [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]], [[1. GRAPH GUIDE]]

---

## Introduction

A [_disjoint-set data structure_](http://en.wikipedia.org/wiki/Disjoint-set_data_structure) is a data structure that keeps track of a set of elements partitioned into a number of disjoint (non-overlapping) subsets. A [_union-find algorithm_](http://en.wikipedia.org/wiki/Disjoint-set_data_structure) is an algorithm that performs two useful operations on such a data structure:

_**Find:**_ Determine which subset a particular element is in. This can be used for determining if two elements are in the same subset.

_**Union:**_ Join two subsets into a single subset.

---
## Motivation Behind Its Creation

The disjoint set data structure was developed to efficiently solve problems where we need to:

1. Group elements into distinct, non-overlapping sets
2. Quickly determine if two elements belong to the same group
3. Join groups together as relationships between elements evolve

Historically, it arose from the need to solve graph connectivity problems efficiently. Before disjoint sets, checking if two vertices in a graph were connected required expensive traversal operations. The disjoint set structure provided a more efficient alternative, particularly for algorithms like Kruskal's minimum spanning tree algorithm.

---
## Intuition Behind the Design

- Think of the disjoint set structure as managing a collection of trees, where each tree represents a set. Every element in a set points to another element as its "parent," with one designated element (the root) representing the entire set.

- The intuition comes from real-world grouping scenarios:

- Imagine a company with employees. Initially, each employee works independently (each person is their own "set"). As teams form, employees join different departments (sets merge via union operations). When you want to know if two employees work in the same department, you trace up their management chain until you find their common boss (find operation).

- The brilliance of the design lies in two optimizations:
	- **Path compression**: When finding an element's set, update that element to point directly to the root, flattening the tree
	- **Union by rank/size**: When merging sets, attach the smaller tree to the root of the larger one to keep trees balanced

- These optimizations together make operations extremely efficient, with near-constant time complexity.

---

## Applications

- Union Find can also be used to find the *no of connected components* in a graph. 
	-  Let count be total nodes in the graph, then for every merge operation decrement count by 1.
- **Kruskal's Minimum Spanning Tree algorithm**: Efficiently determines if adding an edge would create a cycle
- **Connected components in undirected graphs**: Quickly identify separate connected regions
- **Network connectivity**: Determine if two devices can communicate
- **Image processing**: For connected component labeling in computer vision
- **Percolation theory**: Study how materials with random connections conduct
- **Dynamic graph algorithms**: Track connected components as edges are added
- **Detecting cycles in graphs**: Check if adding an edge creates a cycle

---
## Code

#### Approach 1 : Naive, without optimizations
- [Question](https://leetcode.com/problems/redundant-connection/solution/)
- implementation of _union()_ and _find()_ is naive and takes O(n) time in the worst case.


```cpp
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
            while(u != parent[u])
				u = parent[u];
			return u;
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

#### Approach 2 : Rank and Path Compression
- [Reference](https://www.geeksforgeeks.org/union-find-algorithm-set-2-union-by-rank/)
##### Union By rank
- The previous union method can be optimized to _O(Log n)_ in worst case. 
- The idea is to always attach smaller depth tree under the root of the deeper tree. This technique is called _**union by rank**_. The term _rank_ is preferred instead of height because if path compression technique (we have discussed it below) is used, then _rank_ is not always equal to height. Also, size (in place of height) of trees can also be used as _rank_. 
- **Using size as _rank_ also yields worst case time complexity as O(Logn)**

##### Path compression
- The second optimization to naive method is _**Path Compression**_. 
- The idea is to flatten the tree when _find()_ is called. When _find()_ is called for an element x, root of the tree is returned. 
- The _find()_ operation traverses up from x to find root. The idea of path compression is to make the found root as parent of x so that we don’t have to traverse all intermediate nodes again. If x is root of a subtree, then path (to root) from all nodes under x also compresses.

##### Conclusion
- The two techniques complement each other. The time complexity of each operation becomes even smaller than `O(Logn)` In fact, amortized time complexity effectively becomes small constant.


```cpp
struct subset{
    int parent;
    int rank; //Denotes the depth of the tree with root as the parent
};

class ds{
    private:
        int n;
        vector<subset> subsets;
    
    public:
        ds(int n){
            this->n = n;
            
            //1 indexing, hence pushing a dummy
            subsets.push_back({0, 0});
            
            for(int i = 1; i <= n; i++)
                subsets.push_back({i, 0});
        }
    
        int find(int u){
            if(u == subsets[u].parent)
                return u;
            
            //path compression
            return subsets[u].parent = find(subsets[u].parent);
        }
    
        //Union By rank
        void merge(int u, int v){
            //the node with higher rank becomes the parent
            if(subsets[u].rank > subsets[v].rank)
                subsets[v].parent = u;
            else if(subsets[u].rank < subsets[v].rank)
                subsets[u].parent = v;
            
            // If ranks are same, then make one as root and
            // increment its rank by one
            else{
                subsets[v].parent = u;
                subsets[u].rank++;
            }
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
#### Approach 3: Standard implementation

``` cpp
class Ds {
    vector<int> parent;
    vector<int> rank;

public:
    Ds(int n) {
        parent.resize(n);
        rank.resize(n, 0);
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }

    // find the ultimate paretn
    int find(int u) {
        if (parent[u] == u) {
            return u;
        }

        // path compression
        return parent[u] = find(parent[u]);
    }

    void merge(int u, int v) {
        int rootU = find(u);
        int rootV = find(v);

        if (rootU == rootV) {
            return;
        }

        if (rank[rootU] > rank[rootV]) {
            parent[rootV] = rootU;
        } else if (rank[rootU] < rank[rootV]) {
            parent[rootU] = rootV;
        } else {
            parent[rootU] = rootV;
            rank[rootV]++;
        }
    }
};
```
---

## Time Complexity Analysis of the Optimized Method