#### Metadata

timestamp: **17:53**  &emsp;  **01-07-2021**
topic tags: #two_pointer , #imp , #string 
question link: https://leetcode.com/problems/minimum-window-substring/
resource: 
[Aditya Verma](https://www.youtube.com/watch?v=iwv1llyN6mo&list=PL_z_8CaSLPWeM8BDJmIYDaoQ5zuwyxnfj&index=13)
https://www.geeksforgeeks.org/find-the-smallest-window-in-a-string-containing-all-characters-of-another-string/

parent link: [[1. TWO POINTER GUIDE]]

---

# Minimum Window Substring

### Question

Given two strings `s` and `t` of lengths `m` and `n` respectively, return _the **minimum window substring** of_ `s` _such that every character in_ `t` _(**including duplicates**) is included in the window. If there is no such substring__, return the empty string_ `""`_._

The testcases will be generated such that the answer is **unique**.

A **substring** is a contiguous sequence of characters within the string.

>**Example 1:**
**Input:** s = "ADOBECODEBANC", t = "ABC"
**Output:** "BANC"
**Explanation:** The minimum window substring "BANC" includes 'A', 'B', and 'C' from string t.

---


### Approach

#### Code1

``` cpp
string minWindow(string s, string t) {
	int i = 0, j = 0, n = s.size(), tCount = 0, len = INT_MAX, start = 0;
	map<char, int> freq;

	for(int i = 0; i < t.size(); i++)
			freq[t[i]]++;

	while(j < n){

		//Find a valid end of the window
		while(j < n && tCount < freq.size()){
			if(freq.find(s[j]) != freq.end()){
				freq[s[j]]--;

				if(freq[s[j]] == 0) 
					tCount++;  
			}
			j++;
		}

		if(j == n && tCount != freq.size()) break;


		//Find the valid start
		while(i < n && (freq.find(s[i]) == freq.end() || freq[s[i]] < 0)){
			if(freq.find(s[i]) != freq.end())
				freq[s[i]]++;
			i++;
		};

		if((i == n)) break;

		//Store the current window if its smaller than the existing one
		if(j-i < len){
			len = j-i;
			start = i;
		}

		//Move start by one for new window
		freq[s[i]]++;
		if(freq[s[i]] > 0) tCount--;
		i++;
	}
	return len == INT_MAX ? "" : s.substr(start, len);
}
```

---


#### Code2 : More Efficient
1.  First check if the length of the string is less than the length of the given pattern, if yes then “**no such window can exist** “.
2.  Store the occurrence of characters of the given pattern in a hash_pat[].
3.  we will be using two pointer technique basically
4.  Start matching the characters of pattern with the characters of string i.e. increment count if a character matches.
5.  Check if (count == length of pattern ) this means a window is found.
6.  If such a window found, try to minimize it by removing extra characters from the beginning of the current window.
7.  delete one character from first and again find this deleted key at right, once found apply step 5 .
8.  Update min_length.
9.  Print the minimum length window.
```cpp
const int no_of_chars = 256;
string smallestWindow (string str, string pat)
{
	int len1 = str.length();
	int len2 = pat.length();


	if (len1 < len2) 
		return "-1";


	int hash_pat[no_of_chars] = { 0 };
	int hash_str[no_of_chars] = { 0 };

	// Store occurrence ofs characters of pattern
	for (int i = 0; i < len2; i++)
		hash_pat[pat[i]]++;

	int start = 0, start_index = -1, min_len = INT_MAX;

	// Start traversing the string Count of characters
	int count = 0;
	for (int j = 0; j < len1; j++) {

		// Count occurrence of characters of string
		hash_str[str[j]]++;

		// If string's char matches with pattern's char then increment count
		if (hash_str[str[j]] <= hash_pat[str[j]])
			count++;

		// if all the characters are matched
		if (count == len2) {

			// Try to minimize the window
			while (hash_str[str[start]] > hash_pat[str[start]] 
					|| hash_pat[str[start]] == 0) {

				if (hash_str[str[start]] > hash_pat[str[start]])
					hash_str[str[start]]--;
				start++;
			}

			// update window size
			int len_window = j - start + 1;
			if (min_len > len_window) {
				min_len = len_window;
				start_index = start;
			}
		}
	}

	// If no window found
	if (start_index == -1) 
		return "-1";

	return str.substr(start_index, min_len);
}
```