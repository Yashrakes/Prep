#### Metadata

timestamp: **11:36**  &emsp;  **27-06-2021**
topic tags: #dp, #imp 
question link: https://leetcode.com/problems/edit-distance/
resource: https://www.geeksforgeeks.org/edit-distance-dp-5/
parent link: [[1. DP GUIDE]]

---

# Edit Distance

### Question

Given two strings `word1` and `word2`, return _the minimum number of operations required to convert `word1` to `word2`_.

You have the following three operations permitted on a word:

-   Insert a character
-   Delete a character
-   Replace a character

---


### Approach

- If the ith and jth char are same, then no operations are required, hence `res = [i-1][j-1]`
- Else, we perform all three operations on the ith character
	- **Insert**: insert at the ith char such that the inserted char matches jth char. Hence, the result will be `res = 1 + dp[i][j-1]`
	- **Remove**: remove the ith character, `res = 1 + dp[i-1][j]`
	- **Replace**: replace the ith character to match the jth, `res = 1 + dp[i-1][j-1]`
- The final answer is minimum of all three operations

#### Code

``` cpp
int editDistance(string s, string t) {
	int m = s.length(), n = t.length();

	vector<vector<int>> dp(m+1, vector<int>(n+1, 0));

	for(int i = 0; i <= m; i++){
		for(int j = 0; j <= n; j++){
			
			//Boundary Case
			if(i == 0){
				dp[i][j] = j;
				continue;
			}
			
			//Boundary Case
			if(j == 0){
				dp[i][j] = i;
				continue;
			}

			if(s[i-1] == t[j-1])
				dp[i][j] = dp[i-1][j-1];
			else
				dp[i][j] = 1 + min({dp[i][j-1], dp[i-1][j], dp[i-1][j-1]});
		}
	}
	return dp[m][n];
}

```

---


