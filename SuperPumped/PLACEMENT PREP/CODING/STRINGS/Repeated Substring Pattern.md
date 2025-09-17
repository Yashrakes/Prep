#### Metadata

timestamp: **23:59**  &emsp;  **04-07-2021**
topic tags: #string , #imp 
question link: https://leetcode.com/problems/repeated-substring-pattern/
resource: https://www.geeksforgeeks.org/find-given-string-can-represented-substring-iterating-substring-n-times/
parent link: [[1. STRING GUIDE]]

---

# Repeated Substring Pattern

### Question
Given a string `s`, check if it can be constructed by taking a substring of it and appending multiple copies of the substring together.

---

### Approach

#### Code 1
- [Time complexity of find](https://stackoverflow.com/questions/8869605/c-stringfind-complexity/8869689#8869689) is O(n) in the average case.
- Part 1: Let as assume string S is of the form Px Px Px, where Px is the substring of S.
  Then S+S = Px Px Px Px Px Px, now if we modify the first and last characters, we get
  S+S =Px' Px Px Px Px Px' . After modifying S+S, if we are able to find S, then there exists a
  substring Px which when repeated can generate the string S.

``` cpp
bool repeatedSubstringPattern(string s) {
	string t = s+s;
	string tprime = t.substr(1, t.size()-2);

	int idx = tprime.find(s);
	if(idx == string::npos)
		return false;

	return true;
}

```

---


#### Code 2 : Better Approach

Let the given string be ‘str’ and length of given string be ‘n’.  
1) Find length of the longest proper prefix of ‘str’ which is also a suffix. Let the length of the longest proper prefix suffix be ‘len’. This can be computed in O(n) time using pre-processing step of [KMP string matching algorithm](https://www.geeksforgeeks.org/searching-for-patterns-set-2-kmp-algorithm/).  
2) If value of ‘n – len’ divides n (or ‘n % (n-len)’ is 0), then return true, else return false.  
In case of ‘true’ , the substring ‘str[0..n-len-1]’ is the substring that repeats n/(n-len) times.

```cpp
bool repeatedSubstringPattern(string s) {
	int n = s.length(), i, j;
	vector<int> lps(n, 0);

	lps[0] = 0;
	for(i = 1; i < n; i++){
		j = lps[i-1];
		while(j > 0 && s[j] != s[i])
			j = lps[j-1];

		if(s[j] != s[i])
			lps[i] = 0;
		else
			lps[i] = j+1;
	}

	int len = lps[n-1];
	
	
    // If there exist a suffix which is also prefix AND
    // Length of the remaining substring divides total
    // length, then str[0..n-len-1] is the substring that
    // repeats n/(n-len) times
	if(len > 0 && (n%(n-len)) == 0)
		return true;

	return false;
}
```