#### Metadata

timestamp: **23:02**  &emsp;  **04-07-2021**
topic tags: #string, #imp, #algo
question link: https://leetcode.com/problems/implement-strstr/
resource: https://www.geeksforgeeks.org/kmp-algorithm-for-pattern-searching/
parent link: [[1. STRING GUIDE]]

---

# Implement strStr()

### Question

Implement [strStr()](http://www.cplusplus.com/reference/cstring/strstr/).

Return the index of the first occurrence of needle in haystack, or `-1` if `needle` is not part of `haystack`.

**Clarification:**

What should we return when `needle` is an empty string? This is a great question to ask during an interview.

For the purpose of this problem, we will return 0 when `needle` is an empty string. This is consistent to C's [strstr()](http://www.cplusplus.com/reference/cstring/strstr/) and Java's [indexOf()](https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#indexOf(java.lang.String)).

---


### Approach

#### Complexity Analysis
- Time: O(n) for LPS and O(n) for KMP, net = O(n)
#### Code

``` cpp

//Using KMP
int strStr(string haystack, string needle) {

	//First let us create LPS array
	int n = needle.length(), i, j;

	if(n == 0) return 0;

	vector<int> lps(n, 0);

	lps[0] = 0;
	for(i = 1; i < n; i++){

		j = lps[i-1];
		while(j > 0 && needle[j] != needle[i])
			j = lps[j-1];

		if(needle[j] != needle[i])
			lps[i] = 0;
		else
			lps[i] = 1+j;
	}

	//Apply KMP now
	i = 0;
	j = 0;
	while(i < haystack.length()){

		if(haystack[i] == needle[j]){
			i++;
			j++;
		}

		if(j == n)
			return i-j;

		//If there is a mismatch
		else if(i < haystack.length() && haystack[i] != needle[j]){

			if(j == 0)
				i++;
			else
				j = lps[j-1];
		}
	}

	return -1;

}
```

---


