#### Metadata

timestamp: **10:16**  &emsp;  **09-07-2021**
topic tags: #graph, #imp, #bfs
similar: [[Matrix ( Parallel BFS)]]
question link: https://leetcode.com/problems/as-far-from-land-as-possible/
resource:
parent link: [[1. GRAPH GUIDE]]

---

# As Far from Land as Possible

### Question
Given an `n x n` `grid` containing only values `0` and `1`, where `0` represents water and `1` represents land, find a water cell such that its distance to the nearest land cell is maximized, and return the distance. If no land or water exists in the grid, return `-1`.

The distance used in this problem is the Manhattan distance: the distance between two cells `(x0, y0)` and `(x1, y1)` is `|x0 - x1| + |y0 - y1|`.


>**Example 1:**
![](https://assets.leetcode.com/uploads/2019/05/03/1336_ex1.JPG)
**Input:** grid = \[[1,0,1],[0,0,0],[1,0,1]]
**Output:** 2
**Explanation:** The cell (1, 1) is as far as possible from all the land with distance 2.

---


### Approach

- Basically we need to find the distance of the shortest path from a 0 to the farthest 1.
- Sounds Similar?? --> Apply BFS
- This is similar to [[Matrix ( Parallel BFS)]], as in we need to find the nearest one for every 0 and keeping a running track of the maximum distance.
- Essentially, we push all the 1's in a queue and then do a parallel BFS on it to find the distance of the nearest 0.


#### Code

``` cpp
int maxDistance(vector<vector<int>>& mat) {
	int m = mat.size(), n = mat[0].size(), maxDist = -1;

	vector<vector<int>> dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
	vector<vector<bool>> visited(m, vector<bool>(n, false));
	queue<vector<int>> q;

	for(int i = 0; i < m; i++){
		for(int j = 0; j < n; j++){
			if(mat[i][j] == 1){
				q.push({i, j, 0});
				visited[i][j] = true;
			}
		}
	}

	while(!q.empty()){
		vector<int> t = q.front();
		q.pop();

		for(auto &dir : dirs){
			int x = t[0] + dir[0];
			int y = t[1] + dir[1];

			if(x < 0 || x >= m || y < 0 || y >= n || visited[x][y])
				continue;

			q.push({x, y, t[2] + 1});
			visited[x][y] = true;
			maxDist = max(maxDist, t[2]+1);
		}
	}

	return maxDist;
}

```

---


