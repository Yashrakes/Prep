#### Metadata

timestamp: **10:30**  &emsp;  **01-07-2021**
topic tags: #two_pointer , #imp 
question link: https://practice.geeksforgeeks.org/problems/longest-k-unique-characters-substring0853/1#
resource: https://www.youtube.com/watch?v=Lav6St0W_pQ&list=PL_z_8CaSLPWeM8BDJmIYDaoQ5zuwyxnfj&index=10
parent link: [[1. TWO POINTER GUIDE]]

---

# Longest K unique characters substring

### Question

Given a string you need to print the size of the longest possible substring that has exactly K unique characters. If there is no possible substring then print -1.

---


### Approach


#### Code

``` cpp
int longestKSubstr(string s, int k) {

	unordered_map<char, int> mp;
	int i = 0, j = 0, n = s.length(), maxL = 0;

	while(j < n){
		mp[s[j]]++;

		if(mp.size() == k)
			maxL = max(maxL, j-i+1);
		else if(mp.size() > k){
			while(i < n && mp.size() > k){
				mp[s[i]]--;
				if(mp[s[i]] == 0)
					mp.erase(s[i]);
				i++;
			}
		}

		j++;
	}

	return maxL == 0 ? -1 : maxL;
}

```

---


