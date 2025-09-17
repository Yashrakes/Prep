#### Metadata

timestamp: **18:36**  &emsp;  **09-06-2021**
topic tags: #deque , #dp , #priority_queue , #imp 
list tags: #hard , #new 
similar:[[Jump Game VI]]
question link: https://leetcode.com/problems/sliding-window-maximum/
resource:
parent link: [[1. TWO POINTER GUIDE]]

---

# Sliding Window Maximum

### Question
You are given an array of integers `nums`, there is a sliding window of size `k` which is moving from the very left of the array to the very right. You can only see the `k` numbers in the window. Each time the sliding window moves right by one position.

Return _the max sliding window_.

>**Example 1:**
**Input:** nums = \[1,3,-1,-3,5,3,6,7\], k = 3
**Output:** \[3,3,5,5,6,7\]

>**Example 2:**
**Input:** nums = \[1\], k = 1
**Output:** \[1\]

>**Example 3:**
**Input:** nums = \[1,-1\], k = 1
**Output:** \[1,-1\]

---


### Approach
Similar to [[Jump Game VI]]
Standard monoque problem.

---


#### Code 1 : Using double ended queue
- We try to maintain a monotonic decreasing queue at all times.
- So at any given stage, the front of the queue will consist the index of the maximum element seen so far.
- Algo:
	- if the current element is greater than the element at the back of the queue, pop untill the back of the queue contains an element greater than the current.
	- Then insert the index of this element at the back. This way we ensure that we have a decreasing queue

``` cpp
class Solution {
public:
    vector<int> maxSlidingWindow(vector<int>& nums, int k) {
        
        int n = nums.size();
        deque<int> dq;
        vector<int> dp(n-k+1);
        
        for(int i = 0; i < k; i++){
            while(!dq.empty() && nums[i] > nums[dq.back()])
                dq.pop_back();
            dq.push_back(i);
        }
        
        dp[0] = nums[dq.front()];
        
        for(int end = k; end < n; end++){
            int start = end-k+1;
            
			//If the front index is outside current window, pop it
            if(dq.front() < start) dq.pop_front();
            
            while(!dq.empty() && nums[end] > nums[dq.back()])
                dq.pop_back();
            dq.push_back(end);
            
            dp[start] = nums[dq.front()];
        }
        
        return dp;
    }
};

```

---


#### Code 2 : Using priority queue

``` cpp
vector<int> maxSlidingWindow(vector<int>& nums, int k) {
	vector<int> res;
	priority_queue<pair<int, int>> maxQ;
	int n = nums.size(), i, j = 0;

	for(int i = 0; i <= n - k; i++){
		if(i == 0){
			while(j < k && j < n){
				maxQ.push({nums[j], j});
				j++;
			}
			res.push_back(maxQ.top().first);    
		}
		else{
			maxQ.push({nums[j], j});
			j++;

			while(!maxQ.empty() && maxQ.top().second < i)
				maxQ.pop();

			res.push_back(maxQ.top().first);
		}   
	}
	return res;
}

```

---