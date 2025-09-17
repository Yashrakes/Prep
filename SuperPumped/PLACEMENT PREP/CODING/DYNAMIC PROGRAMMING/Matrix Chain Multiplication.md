#### Metadata

timestamp: **17:25**  &emsp;  **18-07-2021**
topic tags: #dp, #imp
question link: https://practice.geeksforgeeks.org/problems/matrix-chain-multiplication0303/1#
resource: [Aditya Verma](https://www.youtube.com/watch?v=kMK148J9qEE&list=PL_z_8CaSLPWekqhdCPmFohncHwz8TY2Go&index=34)
parent link: [[1. DP GUIDE]]

---

# Matrix Chain Multiplication

### Question

Given an array p[] which represents the chain of matrices such that the ith matrix Ai is of dimension p[i-1] x p[i]. We need to write a function MatrixChainOrder() that should return the minimum number of multiplications needed to multiply the chain.

>**Input: p[] = {40, 20, 30, 10, 30} **  
**Output: 26000** 
There are 4 matrices of dimensions 40x20, 20x30, 30x10 and 10x30.
Let the input 4 matrices be A, B, C and D.  The minimum number of 
multiplications are obtained by putting parenthesis in following way
(A(BC))D --> 20\*30\*10 + 40\*20\*10 + 40\*10\*30

---


### Approach 1 : Recursive with memoization

- A simple solution is to place parenthesis at all possible places, calculate the cost for each placement and return the minimum value. 
- In a chain of matrices of size n, we can place the first set of parenthesis in n-1 ways. 
- For example, if the given chain is of 4 matrices. let the chain be ABCD, then there are 3 ways to place first set of parenthesis outer side: (A)(BCD), (AB)(CD) and (ABC)(D). So when we place a set of parenthesis, we divide the problem into subproblems of smaller size. 
- Therefore, the problem has optimal substructure property and can be easily solved using recursion.  
- Minimum number of multiplication needed to multiply a chain of size n = Minimum of all n-1 placements (these placements create subproblems of smaller size)

#### Complexity Analysis
**Time Complexity:** O(n3 )  
**Auxiliary Space:** O(n2)
#### Code

``` cpp
int solve(int n, int arr[], int i, int j, vector<vector<int>> &dp){
	if(i >= j)
		return 0;

	if(dp[i][j] != -1)
		return dp[i][j];

	int res = INT_MAX;
	for(int k = i; k < j; k++){
		int ans = solve(n, arr, i, k, dp) + solve(n, arr, k+1, j, dp) +
			(arr[i-1]*arr[k]*arr[j]);
		res = min(ans, res);
	}

	return dp[i][j] = res;
}

int matrixMultiplication(int n, int arr[])
{
	vector<vector<int>> dp(n, vector<int>(n, -1));
	return solve(n, arr, 1, n-1, dp);
}

```

---
### Approach 2 : Bottom Up

#### Code

``` cpp
int matrixMultiplication(int n, int arr[])
{
	vector<vector<int>> dp(n+1, vector<int>(n+1, 0));

	for(int i = n-2; i >= 1; i--){
	
		//for i == j, ans is 0, which is the default value. so start j from i+1
		for(int j = i+1; j < n; j++){
		
			dp[i][j] = INT_MAX;
			
			for(int k = i; k < j; k++){
				int ans = dp[i][k] + dp[k+1][j] + (arr[i-1]*arr[k]*arr[j]);
				dp[i][j] = min(dp[i][j], ans);
			}
		}
	}

	return dp[1][n-1];
}

```

---

