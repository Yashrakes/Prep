#### Metadata

timestamp: **08:54**  &emsp;  **08-06-2021**
topic tags: #greedy, #two_pointer
list tags: #hard, #solve_again 
similar: https://leetcode.com/problems/largest-rectangle-in-histogram/
question link: https://leetcode.com/problems/maximum-score-of-a-good-subarray/
resource:
parent link: [[1. Topic Wise Problem Guide]]

---

# Maximum Score of a Good Subarray

### Question
You are given an array of integers `nums` **(0-indexed)** and an integer `k`.

The **score** of a subarray `(i, j)` is defined as `min(nums[i], nums[i+1], ..., nums[j]) * (j - i + 1)`. A **good** subarray is a subarray where `i <= k <= j`.

Return _the maximum possible **score** of a **good** subarray._

>**Example 1:**
**Input:** nums = \[1,4,3,7,4,5\], k = 3
**Output:** 15
**Explanation:** The optimal subarray is (1, 5) with a score of min(4,3,7,4,5) \* (5-1+1) = 3 \* 5 = 15.

---


### Approach: Greedy / Two Pointer

#### Algorithm
- Here, we have two variables, the min value and the length of the array
- We try to fix the length of the array and find the maximum score for that particular length.
- If we start from k, then at any point of time we have two options, either move towards the
right or towards the left. We move in the direction, which has the greater value

#### Complexity Analysis
- Time: O(n)
- Space: O(1)


---


### Code

``` cpp
class Solution {
public:
    /*  
    Here, we have two variables, the min value and the length of the array
    We try to fix the length of the array and find the maximum score for that particular length.
    
    If we start from k, then at any point of time we have two options, either move towards the
    right or towards the left. We move in the direction, which has the greater value
    */
    int maximumScore(vector<int>& nums, int k) {
        int n = nums.size(), i = k,  j = k, res = nums[k], curMin = nums[k];
        
        while(i > 0 || j < n-1){
            
            //if i is 0, we can expand only in the right direction
            if(i == 0) 
                j++;
            else if(j == n-1)
                i--;
            else if(nums[i-1] < nums[j+1])
                j++;
            else i--;
            
            curMin = min(curMin, min(nums[i], nums[j]));
            res = max(res, curMin*(j-i+1));
        }
        return res;
    }
};

```

---


