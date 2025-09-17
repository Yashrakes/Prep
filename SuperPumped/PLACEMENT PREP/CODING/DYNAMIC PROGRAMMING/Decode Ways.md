#### Metadata

timestamp: **16:47**  &emsp;  **04-08-2021**
topic tags: #dp
question link: https://leetcode.com/problems/decode-ways/
resource:
parent link: [[1. DP GUIDE]]

---

# Decode Ways

### Question

A message containing letters from `A-Z` can be **encoded** into numbers using the following mapping:

'A' -> "1"
'B' -> "2"
...
'Z' -> "26"

To **decode** an encoded message, all the digits must be grouped then mapped back into letters using the reverse of the mapping above (there may be multiple ways). For example, `"11106"` can be mapped into:

-   `"AAJF"` with the grouping `(1 1 10 6)`
-   `"KJF"` with the grouping `(11 10 6)`

Note that the grouping `(1 11 06)` is invalid because `"06"` cannot be mapped into `'F'` since `"6"` is different from `"06"`.

Given a string `s` containing only digits, return _the **number** of ways to **decode** it_.

The answer is guaranteed to fit in a **32-bit** integer.

---


### Approach

#### Code 1 : Recursive

``` cpp
int numDecodings(string s) {
	return s.empty() ? 0: numDecodings(0,s);    
}
int numDecodings(int p, string& s) {
	int n = s.size();
	if(p == n) return 1;
	if(s[p] == '0') return 0; // sub string starting with 0 is not a valid encoding

	int res = numDecodings(p+1,s);
	if( p < n-1 && (s[p]=='1'|| (s[p]=='2'&& s[p+1]<'7'))) 
		res += numDecodings(p+2,s);
	return res;
}
```


#### Code 2 : 

```cpp
int numDecodings(string s) {
	//note: shifting the array towards the right by one index to handle the corner cases

	int n = s.size();
	vector<int> dp(n+1, 0);

	//tricky step
	dp[0] = 1;

	dp[1] = s[0] == '0' ? 0 : 1;

	for(int i = 2; i <= n; i++){
		//dp[i] = dp[i-1] + dp[i-2]

		if(s[i-1] != '0')
			dp[i] += dp[i-1];

		if((s[i-2] == '1') || (s[i-2] == '2' && s[i-1] <= '6'))
			dp[i] += dp[i-2];
	}

	return dp[n];
}

```
---


