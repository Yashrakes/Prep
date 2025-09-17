#### Metadata

timestamp: **20:44**  &emsp;  **26-06-2021**
topic tags: #string, #imp
question link: https://leetcode.com/problems/longest-palindromic-substring/
resource: https://www.geeksforgeeks.org/longest-palindromic-substring-set-2/
parent link: [[1. STRING GUIDE]]

---

# Longest Palindrome in a String

### Question
Given a string `s`, return _the longest palindromic substring_ in `s`.


---


### Approach 1 : DP

-   dp[i][j] represent whether string i to j is a palindrome or not.
    
-   dp[i][j] can be true only if s[i] == s[j] and dp[i+1][j-1] is true, meaning if the ith and jth character are equal and if the string btw them is also a palindrome then dp[i][j] is a palindrome.

#### Complexity Analysis
- Time: O(n^2)
- Space: O(n^2)

#### Code

``` cpp
string longestPalindrome(string s) { 

	//if the length of string is 1, then that char is the longest palindrome, hence we initialize 
	//maxLen to 1 and startIdx to n-1 
	int n = s.size(), i , j, startIdx = n-1, maxLen = 1; 
	vector<vector<bool>> dp(n, vector<bool>(n, true)); 

	//Bottom to up and left to right 
	//dp[n-1][n-1] is always true, so we start from n-2 
	for(i = n-2; i >= 0; i--){ 

		//only positions where i <= j is valid, hence, j starts from i 
		for(j = i; j < n; j++){ 

			if(s[i] != s[j]) 
				dp[i][j] = false; 
			else 
			{ 
				if(j-1 >= 0) 
					dp[i][j] = dp[i+1][j-1]; 
			} 

			if(dp[i][j]){ 
				if(j-i+1 > maxLen){ 
					maxLen = j-i+1; 
					startIdx = i; 
				} 
			} 
		} 
	} 
	return s.substr(startIdx, maxLen); 
}

```

---
### Approach 2 : Space optimized
1.  The idea is to generate all even length and odd length palindromes and keep track of the longest palindrome seen so far.
2.  To generate odd length palindrome, Fix a centre and expand in both directions for longer palindromes, i.e. fix i (index) as center and two indices as i1 = i+1 and i2 = i-1
3.  Compare i1 and i2 if equal then decrease i2 and increase i1 and find the maximum length.   
    Use a similar technique to find the even length palindrome.
4.  Take two indices i1 = i and i2 = i-1 and compare characters at i1 and i2 and find the maximum length till all pair of compared characters are equal and store the maximum length.
5.  Print the maximum length.


#### Complexity Analysis
- Time: O(n^2)
- Space: O(1)

#### Code

``` cpp
string longestPalin (string s) {
	int n = s.length(), startIdx = 0, maxLen = 1;


	for(int i = 1; i < n; i++){

		//Odd length, with i as the centre
		int left = i-1, right = i+1;
		while(left >= 0 && right < n && s[left] == s[right]){
			if(right-left+1 > maxLen){
				maxLen = right - left +1;
				startIdx = left;
			}
			left--;
			right++;
		}

		//Even length, with i-1 and i as the centre
		left = i-1, right = i;
		while(left >= 0 && right < n && s[left] == s[right]){
			if(right-left+1 > maxLen){
				maxLen = right - left +1;
				startIdx = left;
			}
			left--;
			right++;
		}
	}

	return s.substr(startIdx, maxLen);
}

```

---

