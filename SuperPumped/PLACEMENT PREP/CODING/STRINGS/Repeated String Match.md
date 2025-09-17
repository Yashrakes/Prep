#### Metadata

timestamp: **16:27**  &emsp;  **06-07-2021**
topic tags: #string , #imp, #kmp
question link: https://leetcode.com/problems/repeated-string-match/
resource:
parent link: [[1. STRING GUIDE]]

---

# Repeated String Match

### Question

Given two strings `a` and `b`, return the minimum number of times you should repeat string `a` so that string `b` is a substring of it. If it is impossible for `b`​​​​​​ to be a substring of `a` after repeating it, return `-1`.

**Notice:** string `"abc"` repeated 0 times is `""`,  repeated 1 time is `"abc"` and repeated 2 times is `"abcabc"`.

>**Example 1:**
**Input:** a = "abcd", b = "cdabcdab"
**Output:** 3
**Explanation:** We return 3 because by repeating a three times "ab**cdabcdab**cd", b is a substring of it.

---


### Approach

#### Algorithm

#### Complexity Analysis

#### Code

``` cpp
/*
for b to be a substring of a, there should exist a prefix of b that is a 
suffix of a and suffix of b which is a prefix of a.
*/
int repeatedStringMatch(string a, string b) {
	//Pattern is b and text is a


	int nB = b.size(), nA = a.size(), j, i;

	//LPS for b
	vector<int> lps(nB, 0);

	lps[0] = 0;
	for(i = 1; i < nB; i++){

		j = lps[i-1];
		while(j > 0 && b[i] != b[j])
			j = lps[j-1];

		if(b[j] != b[i])
			lps[i] = 0;
		else
			lps[i] = j+1;
	}

	//KMP

	/*
	total_len is the upper bound of the maximum possible size required, such
	that b can be a substring of a.

	*/
	int total_len = nA*((nB/nA) + 2);
	i = 0;
	j = 0;
	int count = 0;

	while(i < total_len){

		//using mod to wrap arround, treating like a circular array
		if(a[i%nA] == b[j]){
			i++;
			j++;
		}

		if(j == nB){
			return ceil((float)i/nA);
		}
		else if(i < total_len && a[i%nA] != b[j]){
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


