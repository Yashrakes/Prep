 #### Metadata

timestamp: **18:28**  &emsp;  **06-07-2021**
topic tags: #string, #dp, #imp
question link: https://practice.geeksforgeeks.org/problems/form-a-palindrome1455/1/?track=md-string&batchId=144#
resource: https://www.geeksforgeeks.org/minimum-insertions-to-form-a-palindrome-dp-28/
parent link: [[1. STRING GUIDE]]

---

# Minimum insertions to form a palindrome

### Question

Given a string, find the minimum number of characters to be inserted to convert it to palindrome.  
For Example:  
ab: Number of insertions required is 1. **b**ab or aba  
aa: Number of insertions required is 0. aa  
abcd: Number of insertions required is 3. **dcb**abcd

---


### Approach
The minimum number of insertions in the string **str[l…..h]** can be given as:   

-   minInsertions(str[l+1…..h-1]) if str[l] is equal to str[h]
-   min(minInsertions(str[l…..h-1]), minInsertions(str[l+1…..h])) + 1 otherwise

#### Code 1 : Top Down

``` cpp
int f(string s, int i, int j, vector<vector<int>>& dp){
	if(i >= j)
		return 0;

	if(dp[i][j] != -1)
		return dp[i][j];

	int res;
	if(s[i] == s[j]) 
		res =  f(s, i+1, j-1, dp);
	else
		res =  min(f(s, i+1, j, dp), f(s, i, j-1, dp)) + 1;

	return dp[i][j] = res;
}

int countMin(string s){
	int n = s.length();

	if(n == 0) return 1;

	vector<vector<int>> dp(n, vector<int>(n, -1));

	return f(s, 0, n-1, dp);
}

```

---
#### Code2 : Bottom Up

The problem of finding minimum insertions can also be solved using [Longest Common Subsequence (LCS) Problem](https://www.geeksforgeeks.org/longest-common-subsequence-dp-4/). If we find out LCS of string and its reverse, we know how many maximum characters can form a palindrome. We need insert remaining characters. Following are the steps.  
 1.  Find the length of LCS of input string and its reverse. Let the length be ‘l’.
2.  The minimum number insertions needed is length of input string minus ‘l’.


``` cpp
int countMin(string s){
	int n = s.length();
	if(n == 0) return 1;

	//Using longest common subsequence of s and reverse_s
	vector<vector<int>> dp(n+1, vector<int>(n+1, 0));
	string rev = s;
	reverse(rev.begin(), rev.end());

	for(int i = 1; i <= n; i++){
		for(int j = 1; j <= n; j++){
			if(s[i-1] == rev[j-1])
				dp[i][j] = 1 + dp[i-1][j-1];
			else
				dp[i][j] = max(dp[i][j-1], dp[i-1][j]);
		}
	}

	int lcs = dp[n][n];
	return n-lcs;
}

```

---


