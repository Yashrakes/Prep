
timestamp: **08:47**  &emsp;  **15-07-2021**
topic tags: #graph , #imp
question link: https://leetcode.com/problems/find-the-city-with-the-smallest-number-of-neighbors-at-a-threshold-distance/
resource: https://www.geeksforgeeks.org/floyd-warshall-algorithm-dp-16/
parent link: [[1. GRAPH GUIDE]]

---

# Find the City With the Smallest Number of Neighbors at a Threshold Distance

### Question

There are `n` cities numbered from `0` to `n-1`. Given the array `edges` where `edges[i] = [fromi, toi, weighti]` represents a bidirectional and weighted edge between cities `fromi` and `toi`, and given the integer `distanceThreshold`.

Return the city with the smallest number of cities that are reachable through some path and whose distance is **at most** `distanceThreshold`, If there are multiple such cities, return the city with the greatest number.

Notice that the distance of a path connecting cities _**i**_ and _**j**_ is equal to the sum of the edges' weights along that path.

>**Example 1:**
![](https://assets.leetcode.com/uploads/2020/01/16/find_the_city_01.png)

>**Input:** n = 4, edges = [[0,1,3],[1,2,1],[1,3,4],[2,3,1]], distanceThreshold = 4
**Output:** 3
**Explanation:** The figure above describes the graph. 
The neighboring cities at a distanceThreshold = 4 for each city are:
City 0 -> [City 1, City 2] 
City 1 -> [City 0, City 2, City 3] 
City 2 -> [City 0, City 1, City 3] 
City 3 -> [City 1, City 2] 
Cities 0 and 3 have 2 neighboring cities at a distanceThreshold = 4, but we have to return city 3 since it has the greatest number.

---


### Approach

- Read the notebook for better understanding

#### Complexity Analysis
- Time: O(n^3)
- Space: O(n^2)
#### Code

``` cpp
class Solution {
public:
    //All pair shortest path - floyd warshall algo
    int findTheCity(int n, vector<vector<int>>& edges, int distanceThreshold) {
        
        vector<vector<int>> dp(n, vector<int>(n, INT_MAX));
        
        //Base cases
        //Intialize
        for(auto &edge : edges){
            dp[edge[0]][edge[1]] = edge[2];
            dp[edge[1]][edge[0]] = edge[2]; 
        }
        
        //for dp[i][i]
        for(int i = 0; i < n; i++)
            dp[i][i] = 0;
        
        //Bottom - up
        for(int k = 0; k < n; k++){
            for(int i = 0; i < n; i++){
                for(int j = 0; j < n; j++){
                    
                    //i, k = INT_MAX indicates that there is no path btw i, k. Therefore there
                    //does not exist a path from i to j through k
                    if(dp[i][k] == INT_MAX || dp[k][j] == INT_MAX) 
                        continue;
                    
                    dp[i][j] = min(dp[i][k]+dp[k][j], dp[i][j]);
                }
            }
        }
        
        int count, min_count = INT_MAX, city;
        for(int i = 0; i < n; i++){
            count = 0;
            for(int j = 0; j < n; j++){
                if(dp[i][j] <= distanceThreshold)
                    count++;
            }
            
            if(count <= min_count){
                city = i;
                min_count = count;
            }
        }
        
        return city;
    }
};

```

---
