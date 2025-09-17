#### Metadata

timestamp: **15:08**  &emsp;  **01-07-2021**
topic tags: #two_pointer , #imp , #string 
question link: https://leetcode.com/problems/longest-substring-without-repeating-characters/
resource: https://www.geeksforgeeks.org/length-of-the-longest-substring-without-repeating-characters/
parent link: [[1. TWO POINTER GUIDE]]

---

# Longest Substring Without Repeating Characters

### Question

Given a string `s`, find the length of the **longest substring** without repeating characters.

---


### Approach
#### Code1 : Linear Time

``` cpp
int longestUniqueSubsttr(string s){
	vector<int> lastIndex(256, -1);
	int i = 0, j = 0, n = s.length(), res = 0;

	for(j = 0; j < n; j++){

		i = max(i, lastIndex[s[j]] + 1);

		res = max(res, j-i+1);
		lastIndex[s[j]] = j;
	}
	return res;
}
```

---

#### Code2 : Two pointer using set

``` cpp
int lengthOfLongestSubstring(string s) {
	unordered_set<char> unique;
	int i = 0, j = 0, maxL = 0, n = s.length();

	for(i = 0; i < n; i++){

		while(j < n && unique.find(s[j]) == unique.end()){
			unique.insert(s[j]);
			j++;
		}
		maxL = max(maxL, j-i);
		unique.erase(s[i]);
	}

	return maxL;
}

```

---
#### Code3 : Two Pointer, modifying [[Longest K unique characters substring]]

``` cpp
int lengthOfLongestSubstring(string s) {
	unordered_map<char, int> mp;
	int i = 0, j = 0, maxL = 0, n = s.length();

	while(j < n){

		mp[s[j]]++;

		if(j-i+1 == mp.size())
			maxL = max(maxL, j-i+1);
		else if(j-i+1 > mp.size()){

			while(i < n && (j-i+1) > mp.size()){
				mp[s[i]]--;
				if(mp[s[i]] == 0)
					mp.erase(s[i]);
				i++;
			}
		}

		j++;
	}

	return maxL;
}
```

---


