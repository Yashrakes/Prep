#### Metadata

timestamp: **10:28**  &emsp;  **07-07-2021**
topic tags: #graph #imp , #bfs 
question link: https://leetcode.com/problems/01-matrix/
resource:
parent link: [[1. GRAPH GUIDE]]

---

# 01 Matrix

### Question

Given an `m x n` binary matrix `mat`, return _the distance of the nearest_ `0` _for each cell_.

The distance between two adjacent cells is `1`.

---


### Approach
- `distance of the nearest..` , gives us a hint to try BFS.
- Brute force approach would be to try BFS from all 1's and calculate the distance, but that will give a TLE.
- So we try something called as `PARALLEL BFS`. 
- We push all 0's in the queue and mark its distance as 0 and the corresponding cells as visited. Then one by one pop the elements of the queue and visit its non visited neighbors and update their distance. (Distance will be minimal because we start from a 0).


#### Complexity Analysis
- Time: O(m x n)
- Space: O(m x n)

#### Code

``` cpp
vector<vector<int>> updateMatrix(vector<vector<int>>& mat) {
	int m = mat.size(), n = mat[0].size();

	vector<vector<int>> dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

	//Store the result
	vector<vector<int>> dist(m, vector<int>(n));
	vector<vector<bool>> visited(m, vector<bool>(n, false));

	//q[0, 1, 2] = [i, j, distance]
	queue<vector<int>> q;

	for(int i = 0; i < m; i++){
		for(int j = 0; j < n; j++){
			if(mat[i][j] == 0){
				q.push({i, j, 0});
				visited[i][j] = true;

				//dist of 0 cells is 0
				dist[i][j] = 0;
			}
		}
	}

	while(!q.empty()){
		vector<int> t = q.front();
		q.pop();

		//iterate through the nbrs
		for(auto &dir : dirs){
			int x = t[0] + dir[0];
			int y = t[1] + dir[1];

			//If invalid or already visited, ignore
			if(x < 0 || x >= m || y < 0 || y >= n || visited[x][y])
				continue;

			//Update and push
			q.push({x, y, t[2] + 1});
			visited[x][y] = true;
			dist[x][y] = t[2]+1;
		}
	}

	return dist;
}

```

---


