#### Metadata

timestamp: **11:01**  &emsp;  **03-07-2021**
topic tags: #graph , #dfs_from_boundary , #imp
similar: [[Surrounded Regions]]
question link: https://leetcode.com/problems/number-of-closed-islands/
parent link: [[1. GRAPH GUIDE]]

---

# Number of Closed Islands

### Question

Given a 2D `grid` consists of `0s` (land) and `1s` (water).  An _island_ is a maximal 4-directionally connected group of `0s` and a _closed island_ is an island **totally** (all left, top, right, bottom) surrounded by `1s.`

Return the number of _closed islands_.

>**Example 1:**
![](https://assets.leetcode.com/uploads/2019/10/31/sample_3_1610.png)
**Input:** grid = \[[1,1,1,1,1,1,1,0],[1,0,0,0,0,1,1,0],[1,0,1,0,1,1,1,0],[1,0,0,0,0,1,0,1],[1,1,1,1,1,1,1,0]]\
**Output:** 2
**Explanation:** 
Islands in gray are closed because they are completely surrounded by water (group of 1s).

---


### Approach


#### Code

``` cpp
/*
Approach:
Step 1: 
Perform flood fill of all those islands having a border cell as 0 (dfs from boundary)
After performing flood fill, all the remaining islands will be closed islands

Step 2: 
Hence, now perform a standard dfs like in no. of islands to find the result.
*/

vector<vector<int>> dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

void dfs(vector<vector<int>>& grid, vector<vector<bool>>& visited, int i, int j){
	int m = grid.size(), n = grid[0].size();
	if(i < 0 || i >= m || j < 0 || j >= n || visited[i][j] || grid[i][j])
		return;

	visited[i][j] = true;
	grid[i][j] = 1;

	for(auto dir : dirs)
		dfs(grid, visited, i+dir[0], j+dir[1]);
}

int closedIsland(vector<vector<int>>& grid) {
	int m = grid.size(), n = grid[0].size();
	vector<vector<bool>> visited(m, vector<bool>(n, false));
	int count = 0;

	//Remove all the land connected to the edges using flood fill
	//ie remove all 0 cells connected to a border 0 cell
	for(int i = 0; i < m; i++){
		for(int j = 0; j < n; j++){
			if(i == 0 || j == 0 || i == m-1 || j == n-1) 
				if(!visited[i][j] && grid[i][j] == 0)
					dfs(grid, visited, i, j);
		}
	}

	//preform standard dfs
	for(int i = 1; i < m-1; i++){
		for(int j = 1; j < n-1; j++){
			if(!visited[i][j] && grid[i][j] == 0){
				count++;
				dfs(grid, visited, i, j);
			}
		}
	}

	return count;
}

```

---


