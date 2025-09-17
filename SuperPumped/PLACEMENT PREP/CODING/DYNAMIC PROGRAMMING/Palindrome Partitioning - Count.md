#### Metadata

timestamp: **21:06**  &emsp;  **19-07-2021**
topic tags: #dp, #imp 
question link: https://practice.geeksforgeeks.org/problems/palindromic-patitioning4845/1
resource: [Aditya verma](https://www.youtube.com/watch?v=9h10fqkI7Nk&list=PL_z_8CaSLPWekqhdCPmFohncHwz8TY2Go&index=38)
parent link: [[1. DP GUIDE]]

---

# Palindrome Partitioning - Count

### Question

Given a string **str**, a partitioning of the string is a _palindrome partitioning_ if every sub-string of the partition is a palindrome. Determine the fewest cuts needed for palindrome partitioning of given string.

>**Example 1:**
**Input:** str = "ababbbabbababa"
**Output:** 3
**Explaination:** After 3 partitioning substrings 
are "a", "babbbab", "b", "ababa".

---


### Approach
- Using the concept of MCM

#### Algorithm

#### Complexity Analysis

#### Code 1 

``` cpp
bool isPalindrome(string s, int start, int end) {
	while(start <= end){
		if(s[start] != s[end])
			return false;
		start++, end--;
	}
	return true;
}

int solve(string &s, int i, int j, vector<vector<int>> &dp){
	//Base step
	if(i >= j || isPalindrome(s, i, j))
		return 0;

	if(dp[i][j] != -1)
		return dp[i][j];

	int res = INT_MAX;
	for(int k = i; k < j; k++){
		int ans = 1 + solve(s, i, k, dp) + solve(s, k+1, j, dp);
		res = min(ans, res);
	}

	return dp[i][j] = res;
}

int palindromicPartition(string str)
{
	vector<vector<int>> dp(str.length(), vector<int>(str.length(), -1));
	return solve(str, 0, str.length()-1, dp);
}

```

---
#### Code 2

``` cpp
bool isPalindrome(string s, int start, int end) {
	while(start <= end){
		if(s[start] != s[end])
			return false;
		start++, end--;
	}
	return true;
}

int solve(string &s, int i, int j, vector<vector<int>> &dp){
	//Base step
	if(i >= j || isPalindrome(s, i, j))
		return 0;

	if(dp[i][j] != -1)
		return dp[i][j];

	int res = INT_MAX;
	for(int k = i; k < j; k++){

		int ans = 1 ;
		if(dp[i][k] != -1)
			ans += dp[i][k];
		else
			ans += dp[i][k] = solve(s, i, k, dp);

		if(dp[k+1][j] != -1)
			ans += dp[k+1][j];
		else
			ans += dp[k+1][j] = solve(s, k+1, j, dp);

		res = min(ans, res);
	}

	return dp[i][j] = res;
}

int palindromicPartition(string str)
{
	vector<vector<int>> dp(str.length(), vector<int>(str.length(), -1));
	return solve(str, 0, str.length()-1, dp);
}

```

---

