#### Metadata

timestamp: **10:34**  &emsp;  **16-06-2021**
topic tags: #backtracking, #bit_masking 
list tags: #solve_again 
similar:
question link: https://leetcode.com/problems/subsets/
resource:
parent link: [[BACKTRACKING GUIDE]]

---

# Subsets

### Question

Given an integer array `nums` of **unique** elements, return _all possible subsets (the power set)_.

The solution set **must not** contain duplicate subsets. Return the solution in **any order**.


>**Example 1:**
**Input:** nums = \[1,2,3\]
**Output:** \[\[\],\[1\],\[2\],\[1,2\],\[3\],\[1,3\],\[2,3\],\[1,2,3\]\]

---


### Approach1 - Recursive - Inclusion Exclusion

#### Algorithm
- We go backwards. The code is self explanatory

#### Complexity Analysis

#### Code


``` cpp
class Solution {
public:
    vector<vector<int>> f(vector<int>& nums, int i){
        
        int n = nums.size();
        if(i == n) return {{}};
        
        vector<vector<int>> temp = f(nums, i+1);
        
        //Exclude
        vector<vector<int>> res = temp;
        
        //Include
        int k = temp.size();
        for(int j = 0; j < k; j++){
            temp[j].push_back(nums[i]);
            res.push_back(temp[j]);
        }
        return res;
    }
    
    vector<vector<int>> subsets(vector<int>& nums) {
        return f(nums, 0);
    }
};

```


```cpp

class Solution {
public:
    void f(vector<vector<int>>& res, vector<int>& cur, vector<int> &nums, int i){
        int n = nums.size();
        res.push_back(cur);

        for(int j = i; j < n; j++){
            cur.push_back(nums[j]);
            f(res, cur, nums, j+1);
            cur.pop_back();
        }
    }
    
    vector<vector<int>> subsets(vector<int>& nums) {
        int n = nums.size();
        
        vector<vector<int>> res;
        vector<int> cur;
        f(res, cur, nums, 0);
        
        return res;
    }
};
```


---


### Approach2 - Bit Masking

#### Algorithm
>The idea is that we map each subset to a bitmask of length n, where `1` on the i_th_ position in bitmask means the presence of `nums[i]` in the subset, and `0` means its absence.


-   Generate all possible binary bitmasks of length n.
    
-   Map a subset to each bitmask: `1` on the i_th_ position in bitmask means the presence of `nums[i]` in the subset, and `0` means its absence.
    
-   Return output list.

#### Complexity Analysis

-   Time complexity: O(N x 2^N) to generate all subsets and then copy them into output list.
    
-   Space complexity: O(N x 2^N) to keep all the subsets of length N, since each of N elements could be present or absent


#### Code


``` cpp
class Solution {
public:
    vector<vector<int>> subsets(vector<int>& nums) {
        int n = nums.size();
        int size = pow(2, n);
        vector<vector<int>> res;
        
        for(int i = 0; i < size; i++){
            
            vector<int> cur;
            for(int j = 0; j < n; j++){
                
                if(i & (1 << j))
                    cur.push_back(nums[j]);
            }
            res.push_back(cur);
        }
        return res;
    }
};

```

---

