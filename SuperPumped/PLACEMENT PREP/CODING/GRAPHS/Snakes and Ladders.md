#### Metadata

timestamp: **22:18**  &emsp;  **21-07-2021**
topic tags: #graph , #bfs
question link: https://leetcode.com/problems/snakes-and-ladders/
resource:
parent link: [[1. GRAPH GUIDE]]

---

# Snakes and Ladders

### Question

You are given an `n x n` integer matrix `board` where the cells are labeled from `1` to `n2` in a [**Boustrophedon style**](https://en.wikipedia.org/wiki/Boustrophedon) starting from the bottom left of the board (i.e. `board[n - 1][0]`) and alternating direction each row.

You start on square `1` of the board. In each move, starting from square `curr`, do the following:

-   Choose a destination square `next` with a label in the range `[curr + 1, min(curr + 6, n2)]`.
    -   This choice simulates the result of a standard **6-sided die roll**: i.e., there are always at most 6 destinations, regardless of the size of the board.
-   If `next` has a snake or ladder, you **must** move to the destination of that snake or ladder. Otherwise, you move to `next`.
-   The game ends when you reach the square `n2`.

A board square on row `r` and column `c` has a snake or ladder if `board[r][c] != -1`. The destination of that snake or ladder is `board[r][c]`. Squares `1` and `n2` do not have a snake or ladder.

Note that you only take a snake or ladder at most once per move. If the destination to a snake or ladder is the start of another snake or ladder, you do **not** follow the subsequent snake or ladder.

-   For example, suppose the board is `[[-1,4],[-1,3]]`, and on the first move, your destination square is `2`. You follow the ladder to square `3`, but do **not** follow the subsequent ladder to `4`.

Return _the least number of moves required to reach the square_ `n2`_. If it is not possible to reach the square, return_ `-1`.

---


### Approach

#### Code

``` cpp

/*
Approach:
we have to find the shortest path from source to destination. 
The given problem can be modeled into a standard shortest path problem in a directed graph           without weighted edge.

We use BFS to solve it and the result is the level of the destination. The only catch is that
we have to handle cases whenever there is a snake or a ladder
*/
int snakesAndLadders(vector<vector<int>>& board) {

	int n = board.size();
	vector<bool> visited(n*n + 1, false);
	vector<int> value;

	//storing the values of the matrix in a linear fashion so that it becomes easy to travese the
	//nbrs of a particular node
	value.push_back(0);
	int j = 0;
	for(int i = n-1; i >= 0; i--){
		if(j == 0){
			for(; j < n; j++)
				value.push_back(board[i][j]);
			j = n-1;
		}
		else{
			for(; j >= 0; j--)
				value.push_back(board[i][j]);
			j = 0;
		}
	}


	queue<pair<int, int>> q;
	q.push({1, 0});
	visited[1] = true;

	while(!q.empty()){

		int node = q.front().first;
		int level = q.front().second;
		q.pop();

		if(node == n*n)
			return level;

		//Traverse its nbrs
		for(int k = 1; k <= 6; k++){
			int nbr = node+k;
			if(!visited[nbr] && nbr <= n*n){
				visited[nbr] = true;

				//snale or ladder found, bypass the nbr
				if(value[nbr] != -1){
					q.push({value[nbr], level+1});
				} 
				else
					q.push({nbr, level+1});
			}
		}
	}

	return -1;
}
```

---


