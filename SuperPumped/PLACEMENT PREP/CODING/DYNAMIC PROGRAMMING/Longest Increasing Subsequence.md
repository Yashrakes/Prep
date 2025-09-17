#### Metadata

timestamp: **10:59**  &emsp;  **27-06-2021**
topic tags: #imp , #dp 
question link: https://leetcode.com/problems/longest-increasing-subsequence/
resource: https://www.geeksforgeeks.org/longest-monotonically-increasing-subsequence-size-n-log-n/
parent link: [[1. DP GUIDE]]

---

# Longest Increasing Subsequence

### Question

Given an integer array `nums`, return the length of the longest strictly increasing subsequence.

A **subsequence** is a sequence that can be derived from an array by deleting some or no elements without changing the order of the remaining elements. For example, `[3,6,2,7]` is a subsequence of the array `[0,3,1,6,2,2,7]`.

---


### Approach 1 : Best

- `tails` is an array storing the smallest tail of all increasing subsequences with length `i+1` in `tails[i]`.  
- For example, say we have `nums = [4,5,6,3]`, then all the available increasing subsequences are:

```
len = 1   :      [4], [5], [6], [3]   => tails[0] = 3
len = 2   :      [4, 5], [5, 6]       => tails[1] = 5
len = 3   :      [4, 5, 6]            => tails[2] = 6
```

- We can easily prove that tails is a increasing array. Therefore it is possible to do a binary search in tails array to find the one needs update.

- Each time we only do one of the two:

```
(1) if x is larger than all tails, append it, increase the size by 1
(2) if tails[i-1] < x <= tails[i], update tails[i]
```

- Doing so will maintain the tails invariant. The the final answer is just the size.
- Case 1 and 2 can be implemented using binary search
- [Reference](https://leetcode.com/problems/longest-increasing-subsequence/discuss/74824/JavaPython-Binary-search-O(nlogn)-time-with-explanation)

#### Complexity Analysis
- **Time:** O(N log N)
#### Code

``` cpp
int binarySearch(vector<int> &tail, int l, int h, int key){
	int mid;
	while(l < h){
		mid = l + (h-l)/2;
		if(key <= tail[mid])
			h = mid;
		else
			l = mid+1;
	}

	return l;
}

int lengthOfLIS(vector<int>& nums) {
	int n = nums.size();

	if(n <= 1) return n;

	vector<int> tail(n, 0);

	// always points empty slot in tail
	int nextSlot = 0;

	for(int x : nums){
		//Notice that nextSlot points to an empty index in tails
		int index = binarySearch(tail, 0, nextSlot, x);
		tail[index] = x;
		if(index == nextSlot) nextSlot++;
	}

	return nextSlot;
}

```

---

### Approach 2 : DP

#### Code

``` cpp
int lengthOfLIS(vector<int>& nums) {
	int n = nums.size(), i, j, runningMaxL = 1, maxL = 1;

	if(n <= 1)
		return n;

	vector<int> dp(n, 0); 
	dp[0] = 1;

	//Calculating the maximum subsequence where the last element is nums[i]
	for(i = 1; i < n; i++){
		runningMaxL = 1;
		for(j = 0; j < i; j++){
			if(nums[i] > nums[j])
				runningMaxL = max(runningMaxL, 1 + dp[j]);
		}
		dp[i] = runningMaxL;
		maxL = max(maxL, dp[i]);
	}     
	return maxL;       
}

```

---

