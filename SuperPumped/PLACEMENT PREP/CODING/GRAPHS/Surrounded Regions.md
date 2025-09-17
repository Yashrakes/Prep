#### Metadata

timestamp: **12:22**  &emsp;  **02-07-2021**
topic tags: #graph , #imp , #dfs_from_boundary
question link: https://leetcode.com/problems/surrounded-regions/
resource:
parent link: [[1. GRAPH GUIDE]]

---

# Surrounded Regions

### Question

Given an `m x n` matrix `board` containing `'X'` and `'O'`, _capture all regions surrounded by_ `'X'`.

A region is **captured** by flipping all `'O'`s into `'X'`s in that surrounded region.

>**Example 1:**
![](https://assets.leetcode.com/uploads/2021/02/19/xogrid.jpg)
**Input:** board = \[\["X","X","X","X"],["X","O","O","X"],["X","X","O","X"],["X","O","X","X"]]
**Output:** \[\["X","X","X","X"],["X","X","X","X"],["X","X","X","X"],["X","O","X","X"]]
**Explanation:** Surrounded regions should not be on the border, which means that any 'O' on the border of the board are not flipped to 'X'. Any 'O' that is not on the border and it is not connected to an 'O' on the border will be flipped to 'X'. Two cells are connected if they are adjacent cells connected horizontally or vertically.

---


### Approach

- WE know that all the components consisting a border cell with value 'O' should not be modified.
- Hence, we perform a dfs from all the border cells with a value 'O' and mark all the cells that are connected to these border cells as visited.
- Finally we iterate through the board again and if we find an unvisted 'O', we flip it to 'X'.
- 'O' will be unvisited only if it was unreachable from any of the border 'O'.

#### Complexity Analysis

#### Code : DFS From the boundary
- We can avoid using the visited matrix by marking the visited cells using a special symbol and unmarking it back in the second traversal.

``` cpp
vector<vector<int>> dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
void dfs(vector<vector<char>>& board, vector<vector<bool>>& visited, int i, int j){
	int m = board.size(), n = board[0].size();
	if(i < 0 || j < 0 || i >= m || j >= n || board[i][j] == 'X' || visited[i][j])
		return;

	visited[i][j] = true;

	for(auto dir : dirs)
		dfs(board, visited, i+dir[0], j+dir[1]);
}

void solve(vector<vector<char>>& board) {
	int m = board.size(), n = board[0].size();
	vector<vector<bool>> visited(m, vector<bool>(n, false));
	
	//Preform dfs on the boundary 'O'
	for(int i = 0; i < m; i++)
		for(int j = 0; j < n; j++){
			if(i == 0 || j == 0 || i == m-1 || j == n-1)
				if(!visited[i][j])
					dfs(board, visited, i, j);
		}
	
	//Flip the unvisited 'O'
	for(int i = 1; i < m-1; i++)
		for(int j = 1; j < n-1; j++)
			if(!visited[i][j] && board[i][j] == 'O')
				board[i][j] = 'X';
}

```

---


