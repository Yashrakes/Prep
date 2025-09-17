#### Metadata

timestamp: **14:49**  &emsp;  **29-07-2021**
topic tags: #dp, #imp
question link: https://leetcode.com/problems/wildcard-matching/
resource:
parent link: [[1. DP GUIDE]]

---

# Wildcard Matching

### Question

Given an input string (`s`) and a pattern (`p`), implement wildcard pattern matching with support for `'?'` and `'*'` where:

-   `'?'` Matches any single character.
-   `'*'` Matches any sequence of characters (including the empty sequence).

The matching should cover the **entire** input string (not partial).

>**Example 4:**
**Input:** s = "adceb", p = "*a*b"
**Output:** true
**Explanation:** The first '*' matches the empty sequence, while the second '*' matches the substring "dce".

---


### Approach

#### Algorithm

#### Complexity Analysis

#### Code

``` cpp
bool isMatch(string s, string p) {
	int m = p.length(), n = s.length();

	//i refers to pattern, j to text
	vector<vector<bool>> dp(m+1, vector<bool>(n+1, false));

	//Boundary conditions
	dp[0][0] = true;

	//for coulmn 0 -->dp[i][0], indicates that we have to match the pattern 
	//with an empty string
	for(int i = 1; i <= m; i++)
		if(p[i-1] == '*')
			dp[i][0] = dp[i-1][0];

	for(int i = 1; i <= m; i++){
		for(int j = 1; j <= n; j++){
			if(p[i-1] == s[j-1] || p[i-1] == '?')
				dp[i][j] = dp[i-1][j-1];
			else if(p[i-1] == '*'){
				dp[i][j] = dp[i-1][j] || dp[i][j-1];

			}

		}
	}

	return dp[m][n];
}

```

---


