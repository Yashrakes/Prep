#### Metadata

timestamp: **18:20**  &emsp;  **22-07-2021**
topic tags: #dp, #imp 
question link: https://practice.geeksforgeeks.org/problems/boolean-parenthesization5610/1
resource: https://www.geeksforgeeks.org/boolean-parenthesization-problem-dp-37/
[Aditya Verma](https://www.youtube.com/watch?v=pGVguAcWX4g&list=PL_z_8CaSLPWekqhdCPmFohncHwz8TY2Go&index=38)
parent link: [[1. DP GUIDE]]

---

# Boolean Parenthesization

### Question

Given a boolean expression **S** of length **N** with following symbols.  
Symbols  
 'T' ---> true  
 'F' ---> false  
and following operators filled between symbols  
Operators  
 &   ---> boolean AND  
 |   ---> boolean OR  
 ^   ---> boolean XOR  
Count the number of ways we can parenthesize the expression so that the value of expression evaluates to true.


>**Example 1:**
**Input:** N = 7
S = T|T&F^T
**Output:** 4
**Explanation:** The expression evaluates to true in 4 ways ((T|T)&(F^T)), 
(T|(T&(F^T))), (((T|T)&F)^T) and (T|((T&F)^T))


**Your Task:**  
You do not need to read input or print anything. Your task is to complete the function **countWays()** which takes N and S as input parameters and returns number of possible ways modulo 1003.

---


### Approach

#### Algorithm

#### Complexity Analysis
- Time: O(n^3)
- Space: O(n^3)


#### Code

``` cpp
//solve(i, j, true) implies the no of ways to parenthesize the string from i to j such that the result is true
//solve(i, j, false) implies the no of ways to parenthesize the string from i to j such that the result is false
int solve(string str, int i, int j, bool isTrue, vector<vector<vector<int>>> &dp){

	//Base case: invalid input
	if(i > j)
		return 0;

	//Base case: Valid: if i == j, then str[i] can either be 'T' or 'F'
	if(i == j)
		if(isTrue)
			return str[i] == 'T' ? 1 : 0;
		else
			return str[i] == 'F' ? 1 : 0;



	if(dp[i][j][isTrue] != -1)
		return dp[i][j][isTrue];

	int ans = 0;

	//we can divide the input string into two parts only if the breakpoint is an operator
	//we notice that every alternate character is an operator and hence a candidate to be the breakpoint, hence we increment k by 2
	//i and j both point to charcters 'T' or 'F', therfore k starts from i+1 and ends at j-1
	for(int k = i+1; k <= j-1; k = k+2){

		int lT, lF, rT, rF;

		if(dp[i][k-1][true] != -1)
			lT = dp[i][k-1][true];
		else
			lT = solve(str, i, k-1, true, dp);

		if(dp[i][k-1][false] != -1)
			lF = dp[i][k-1][false];
		else
			lF = solve(str, i, k-1, false, dp);

		if(dp[k+1][j][true] != -1)
			rT = dp[k+1][j][true];
		else
			rT = solve(str, k+1, j, true, dp);

		if(dp[k+1][j][false] != -1)
			rF = dp[k+1][j][false];
		else
			rF = solve(str, k+1, j, false, dp);

		//get the operator
		char op = str[k];
		if(op == '|')
			if(isTrue)
				ans += lT*rT + lT*rF + lF*rT;
			else
				ans += lF*rF;
		else if(op == '&')
			if(isTrue)
				ans += lT*rT;
			else
				ans += lF*rF + lF*rT + lT*rF;
		else if(op == '^')
			if(isTrue)
				ans += lT*rF + lF*rT;
			else
				ans += lT*rT + lF*rF;
	}

	return dp[i][j][isTrue] = ans%1003;
}

int countWays(int N, string S){
	vector<vector<vector<int>>> dp(N+1, vector<vector<int>>(N+1, vector<int>(2, -1)));
	return solve(S, 0, N-1, true, dp);
}

```

---


