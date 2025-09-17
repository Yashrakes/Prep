#### Metadata

timestamp: **16:33**  &emsp;  **09-06-2021**
topic tags: #deque, #dp
list tags: #medium, #new
similar: https://leetcode.com/problems/sliding-window-maximum/
question link: https://leetcode.com/problems/jump-game-vi/
resource: discussion forum, [[Monoqueue]]
parent link: [[1. Topic Wise Problem Guide]]

---

# Jump Game VI

### Question
You are given a **0-indexed** integer array `nums` and an integer `k`.

You are initially standing at index `0`. In one move, you can jump at most `k` steps forward without going outside the boundaries of the array. That is, you can jump from index `i` to any index in the range `[i + 1, min(n - 1, i + k)]` **inclusive**.

You want to reach the last index of the array (index `n - 1`). Your **score** is the **sum** of all `nums[j]` for each index `j` you visited in the array.

Return _the **maximum score** you can get_.

>**Example 1:**
**Input:** nums = \[1,\-1,-2,4,-7,3\], k = 2
**Output:** 7
**Explanation:** You can choose your jumps forming the subsequence \[1,-1,4,3\] (underlined above). The sum is 7.

>**Example 2:**
**Input:** nums = \[10,-5,-2,4,0,3\], k = 3
**Output:** 17
**Explanation:** You can choose your jumps forming the subsequence \[10,4,3\] (underlined above). The sum is 17.


---


### Approach

- Let dp\[i\] be "the maximum score to reach the end starting at index i". The answer for dp\[i\] is nums\[i\] + min{dp\[i+j\]} for 1 <= j <= k. That gives an O(n\*k) solution which will give a TLE. (Since n, k  <= 10^5)
- Instead of checking every j for every i, keep track of the largest dp\[i\] values in a deque and calculate dp\[i\] from right to left.
- In this algo we will be using a `monotonic decreasing dequeue implemented` using a deque such that, the best option is always at the front of the deque.
- Note: We are storing the result in the same array to avoid using an additional array.


#### Algorithm
1.	Add the maximum available value to nums[i] to get the answer for index i.
2.	Now, we insert the obtained answer in the deque in such a manner that the monotic decreasing nature is not disturbed.
3.	Finally, if the current best option is out of bounds, we remove it.

#### Complexity Analysis
Time: `O(n)`. 
Space `O(n)`



---


### Code

``` cpp
class Solution {
public:

    int maxResult(vector<int>& nums, int k) {
        
        int n = nums.size();
        deque<int> dq;
        
        dq.push_front(n-1);
        for(int i = n-2; i >= 0; i--){
            
            nums[i] = nums[i] + nums[dq.front()]; //1.
            
            while(!dq.empty() && nums[i] > nums[dq.back()]) //2.
                dq.pop_back();
            dq.push_back(i);

            if(dq.front() >= i+k)   //3.
                dq.pop_front();
        }
        return nums[0];
    }
};

```

---


