#### Metadata

timestamp: **17:57**  &emsp;  **06-07-2021**
topic tags: #string , #imp 
question link: https://practice.geeksforgeeks.org/problems/longest-common-substring1452/1/?track=md-string&batchId=144#
resource: https://www.geeksforgeeks.org/longest-common-substring-dp-29/
parent link: [[1. STRING GUIDE]]

---

# Longest Common Substring

### Question
Given two strings. The task is to find the length of the longest common substring.


---


### Approach
- Let `dp[i][j]` be equal to the length of the longest common substring ending at `S1[i` and `S2[j]`.

#### Code

``` cpp
int longestCommonSubstr (string S1, string S2, int n, int m)
{
	// your code here
	vector<vector<int>> dp(n+1, vector<int>(m+1, 0));
	int res = 0;
	for(int i = 1; i <= n; i++){
		for(int j = 1; j <= m; j++){

			if(S1[i-1] == S2[j-1]){
				dp[i][j] = 1 + dp[i-1][j-1];
				res = max(res, dp[i][j]);
			}
			else
				dp[i][j] = 0;
		}
	}

	return res;
}

```

---


