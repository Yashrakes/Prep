#### Metadata

timestamp: **12:02**  &emsp;  **08-07-2021**
topic tags: #dp, #backtracking , #imp
question link: https://leetcode.com/problems/word-break/
resource:
parent link: [[1. DP GUIDE]]

---

# Word Break

### Question

Given a string `s` and a dictionary of strings `wordDict`, return `true` if `s` can be segmented into a space-separated sequence of one or more dictionary words.

**Note** that the same word in the dictionary may be reused multiple times in the segmentation.

>**Example 1:**
**Input:** s = "leetcode", wordDict = ["leet","code"]
**Output:** true
**Explanation:** Return true because "leetcode" can be segmented as "leet code".

---


### Approach

#### Code

``` cpp
class Solution {
public:
    /*
    We start from the end. For every iteration, we check whether the substr
	starting from i and ending at j exists in the dictionary. If it exists than the
	subproblem becomes the string s[j+1...n-1] which is dp[j+1].
    */
    bool wordBreak(string s, vector<string>& wordDict) {
        unordered_set<string> dict(wordDict.begin(), wordDict.end());
        
        int n = s.size(), i, j;
        vector<bool> dp(n+1);
        
        //this is to handle the case when s[i .. j] is a valid string and j is n-1,
        //then dp[i] = dp[j+1] = dp[n], which should be true
        dp[n] = true;    
        string temp = "";
        
        for(i = n-1; i >= 0; i--){
            for(j = i; j < n; j++){
                
                temp = s.substr(i, j-i+1);
                if(dict.find(temp) != dict.end() && dp[i] != true)
                    dp[i] = dp[j+1];
            }
        }
        return dp[0];
    }
};

```

---


