#### Metadata

timestamp: **16:48**  &emsp;  **28-06-2021**
topic tags: #array , #imp
question link: https://leetcode.com/problems/maximum-subarray/
resource: https://www.geeksforgeeks.org/largest-sum-contiguous-subarray/
parent link: [[1. ARRAY GUIDE]]

---

# Kadane’s Algorithm

### Question

Given an integer array `nums`, find the contiguous subarray (containing at least one number) which has the largest sum and return _its sum_.

---


### Approach

The genius of Kadane's algorithm lies in how it makes a local decision at each step that leads to finding the global maximum. Here's the core insight: at any position, we only need to make one decision - should we extend the previous subarray or start a new subarray at the current position.

#### Code

``` cpp
//Kadane's Algo
int maxSubArray(vector<int>& nums) {
	int n = nums.size(), currMax = nums[0], maxV = nums[0];

	for(int i = 1; i < n; i++){
		//currMax is the maximum subarray ending at i
		currMax = max(nums[i], currMax+nums[i]);
		maxV = max(maxV, currMax);
	}
	return maxV;
}


int Solution::maxSubArray(const vector<int> &A) {
    int sum = 0, res = INT_MIN;
    int n = A.size();

    for(int i = 0; i < n; i++){
        sum += A[i];
        res = max(res, sum);
        if(sum < 0)
            sum = 0;
    }

    return res;
}
```

#### Code 2 : If the questions asks to return the index of the max array

``` cpp
struct KadaneResult {
    int maxSum;
    int start;
    int end;
};

KadaneResult kadaneWithBounds(vector<int>& nums) {
    int currentSum = 0;
    int maxSum = nums[0];
    int start = 0;
    int end = 0;
    int tempStart = 0;
    
    for (int i = 0; i < nums.size(); i++) {
        // If current sum becomes negative, start fresh
        if (currentSum + nums[i] < nums[i]) {
            currentSum = nums[i];
            tempStart = i;
        } else {
            currentSum += nums[i];
        }
        
        // Update maximum if we found a better sum
        if (currentSum > maxSum) {
            maxSum = currentSum;
            start = tempStart;
            end = i;
        }
    }
    
    return {maxSum, start, end};
}
```

---

## Maximum Product Subarray

``` cpp
int maxProduct(vector<int>& nums) {
    int maxSoFar = nums[0];
    int minSoFar = nums[0];
    int result = maxSoFar;
    
    for (int i = 1; i < nums.size(); i++) {
        int curr = nums[i];
        int tempMax = max({curr, maxSoFar * curr, minSoFar * curr});
        minSoFar = min({curr, maxSoFar * curr, minSoFar * curr});
        maxSoFar = tempMax;
        
        result = max(result, maxSoFar);
    }
    return result;
}
```

