#### Metadata

timestamp: **13:55**  &emsp;  **02-07-2021**
topic tags: #graph, #imp 
question link: https://www.geeksforgeeks.org/find-the-number-of-distinct-islands-in-a-2d-matrix/
resource:
parent link: [[1. GRAPH GUIDE]]

---

# Find the number of distinct islands in a 2D matrix

### Question

Given a boolean 2D matrix. The task is to find the number of distinct islands where a group of connected 1s (horizontally or vertically) forms an island. Two islands are considered to be same if and only if one island is equal to another (not rotated or reflected).

>**Input:** grid[][] =  
{{1, 1, 0, 0, 0},  
1, 1, 0, 0, 0},  
0, 0, 0, 1, 1},  
0, 0, 0, 1, 1}} <br>
**Output:** 1  
Island at the top left corner is same as island  at the bottom right corner

>**Input:** grid[][] =  
{{1, 1, 0, 1, 1},  
1, 0, 0, 0, 0},  
0, 0, 0, 0, 1},  
1, 1, 0, 1, 1}} <br>
**Output:** 3  
Distinct islands in the example above are: 1at the top left corner; 1 at the top right corner and 1 at the bottom right corner. We ignore the island 1, at the bottom left corner since  it is identical to the top right corner.

---


### Approach
- We encode the path of each island in a string and store the result in a set to get distinct encoding. 
#### Algorithm

#### Complexity Analysis

#### Code

``` cpp
vector<vector<int>> dirs = {{0, 1}, {0, -1}, {-1, 0}, {1, 0}};
vector<char> ch = {'R', 'L', 'U', 'D'};

void dfs(vector<vector<int>>& grid, vector<vector<bool>>& visited, int i, int j, string& island, char c){
	int m = grid.size(), n = grid[0].size();
	if(i < 0 || i >= m || j < 0 || j >= n || grid[i][j] == 0 || visited[i][j])
		return ;

	visited[i][j] = true;
	island += c;

	for(int k = 0; k < dirs.size(); k++)
		dfs(grid, visited, i+dirs[k][0], j+dirs[k][1], island, ch[k]);

	island += 'B';
}

int numDistinctIslands(vector<vector<int>>& grid){
	unordered_set<string> s;
	int m = grid.size(), n = grid[0].size();
	vector<vector<bool>> visited(m, vector<bool>(n, false));

	for(int i = 0; i < m; i++){
		for(int j = 0; j < n; j++){
			string island = "";
			dfs(grid, visited, i, j, island, 'S');

			if(island != "" && s.find(island) == s.end())
				s.insert(island);
		}
	}
	return s.size();
}

```

---


