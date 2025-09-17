

## Problem Statement

Given two strings, **s1** and **s2**, the task is to find the length of the Longest Common Subsequence. If there is no ***common subsequence***, return `0`.  

A ***subsequence*** is a string generated from the original string by deleting 0 or more characters and without changing the relative order of the remaining characters. For example , subsequences of “ABC” are “”, “A”, “B”, “C”, “AB”, “AC”, “BC” and “ABC”.  


## Solution

```cpp
int longestCommonSubsequence(string text1, string text2) {
	int n1 = text1.length();
	int n2 = text2.length();
	vector<vector<int>> dp(n1+1, vector<int>(n2+1, 0));

	for (int i = 1; i <= n1; i++) {
		for (int j = 1; j <= n2; j++) {
			if (text1[i-1] == text2[j-1])
				dp[i][j] = 1 + dp[i-1][j-1];
			else
				dp[i][j] = max(dp[i][j-1], dp[i-1][j]);
		}
	}
	return dp[n1][n2];
}
```

---

## Variations

- [ ] **Longest Common Substring**:
	``` cpp
	// dp[i][j] -> length of LCS, such that the substing ends at ith and jth char
	for (int i = 1; i <= n1; i++) {
		for (int j = 1; j <= n2; j++) {
			if (s1[i-1] == s2[j-1])
				dp[i][j] = 1 + dp[i-1][j-1];
			
			res = max(res, dp[i][j]);
		}
	}
	```

- [ ] **Printing Longest common subsequence**
	```cpp
	// Start from the right-most-bottom-most corner and
	// one by one store characters in lcs[]
	int i = m, j = n;
	while (i > 0 && j > 0) {
		// If current character in X[] and Y are same, then
		// current character is part of LCS
		if (X[i - 1] == Y[j - 1]) {
			lcs[index - 1]
				= X[i - 1]; // Put current character in result
			i--;
			j--;
			index--; // reduce values of i, j and index
		}
	
		// If not same, then find the larger of two and
		// go in the direction of larger value
		else if (L[i - 1][j] > L[i][j - 1])
			i--;
		else
			j--;
	}
	```

- [ ] **Length of Shortest Common SuperSequence**
	- Q: Given two strings str1 and str2, find the shortest string that has both str1 and str2 as subsequences.
	``` cpp
	Res = length(str1) + length(str2) - lcs
	// print: https://leetcode.com/problems/shortest-common-supersequence/description/
	```

- [ ] **Minimum number of deletions and insertions**
	- Given two strings **s1** and **s2**. The task is to **remove or insert** the **minimum** **number** of characters from/in **s1** to transform it into **s2**. It could be possible that the same character needs to be removed from one point of **s1** and inserted into another point.
	
	``` cpp
	Res = length(s1) + length(s2) - 2*lcs
	```

- [ ] **Longest Palindromic Subsequence**

	```cpp
	Res = lcs(s1, rev(s1))
	```

- [ ] **Longest repeating subsequence**
	- Given string str, find the length of the longest repeating subsequence such that it can be found twice in the given string.
	- The two identified subsequences A and B can use the same ith character from string **s** if and only if that ith character has different indices in A and B. For example, A = "xax" and B = "xax" then the index of the first "x" must be different in the original string for A and B.
	``` cpp
	for(int i = 1; i <= n; i++) {
		for(int j = 1; j <= n; j++){
			if(s[i-1] == s[j-1] && i != j)
				dp[i][j] = 1 + dp[i-1][j-1];
			else
				dp[i][j] = max(dp[i-1][j], dp[i][j-1]);
		}
	}
	```