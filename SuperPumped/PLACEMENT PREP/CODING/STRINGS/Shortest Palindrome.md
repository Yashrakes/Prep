#### Metadata

timestamp: **14:22**  &emsp;  **06-07-2021**
topic tags: #string , #imp 
question link: https://leetcode.com/problems/shortest-palindrome/
resource: https://leetcode.com/problems/shortest-palindrome/solution/
parent link: [[1. STRING GUIDE]]

---

# Shortest Palindrome

### Question

You are given a string `s`. You can convert `s` to a palindrome by adding characters in front of it.

Return _the shortest palindrome you can find by performing this transformation_.

>**Example 1:**
**Input:** s = "aacecaaa"
**Output:** "aaacecaaa"

---


### Approach

-   We use the KMP lookup table generation
-   Create \text{new_s} as s + \text{"#"} + \text{reverse(s)} and use the string in the lookup-generation algorithm
    -   The "#" in the middle is required, since without the #, the 2 strings could mix with each ther, producing wrong answer. For example, take the string \text{"aaaa"}"aaaa". Had we not inserted "#" in the middle, the new string would be \text{"aaaaaaaa"}"aaaaaaaa" and the largest prefix size would be 7 corresponding to "aaaaaaa" which would be obviously wrong. Hence, a delimiter is required at the middle.
-   Return reversed string after the largest palindrome from beginning length(given by n-\text{f[n_new-1]}) + original string ss

#### Code

``` cpp
string shortestPalindrome(string s) {
	string rev = s;
	reverse(rev.begin(), rev.end());

	string t = s + "#" + rev;
	int n = t.size(), j;
	vector<int> lps(n, 0);

	lps[0] = 0;
	for(int i = 1; i < n; i++){
		j = lps[i-1];
		while(j > 0 && t[i] != t[j])
			j = lps[j-1];

		if(t[i] != t[j])
			lps[i] = 0;
		else
			lps[i] = j+1;
	}

	string append = rev.substr(0, rev.size()-lps[n-1]);
	return append + s;
}

```

---


