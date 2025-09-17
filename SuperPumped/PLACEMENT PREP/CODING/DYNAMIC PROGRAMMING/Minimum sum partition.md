#### Metadata

timestamp: **13:02**  &emsp;  **27-06-2021**
topic tags: #dp , #imp 
question link: https://practice.geeksforgeeks.org/problems/minimum-sum-partition3317/1#
resource: https://www.geeksforgeeks.org/partition-a-set-into-two-subsets-such-that-the-difference-of-subset-sums-is-minimum/
parent link: [[1. DP GUIDE]]

---

# Minimum sum partition

### Question
Given an integer array **arr** of size **N**, the task is to divide it into two sets S1 and S2 such that the absolute difference between their sums is minimum and find the minimum difference

---


### Approach

#### Algorithm
- Let `dp[i][j]` indicate whether it is possible to chose a set from 0 to ith element such that its sum is equal to j.
- i ranges from 0 to n-1 and, j from 0 to sum
- Hence, we have two decisions 
	- Exclude: `dp[i][j] = dp[i-1][j]`
	- Include: `dp[i][j] = dp[i-1][j-arr[i]]`
- The maximum sum a set can have to obtain min difference is equal to half of total sum
- Therefore, we pick the max j which is less than sum/2

#### Complexity Analysis
**Time Complexity:** O(**N*|sum of array elements|**)  
**Auxiliary Space:** O(**N*|sum of array elements|**)

#### Code

``` cpp
int minDifference(int arr[], int n)  { 

	int sum = 0, set1_sum = INT_MIN;
	for(int i = 0; i < n; i++)
		sum += arr[i];

	vector<vector<bool>> dp(n, vector<bool>(sum+1, false));

	dp[0][0] = true, dp[0][arr[0]] = true;
	for(int i = 1; i < n; i++){
	   for(int j = 0; j <= sum; j++){

		   //Exclude
		   dp[i][j] = dp[i-1][j];

		   //include	           
		   if(arr[i] <= j)
				dp[i][j] = dp[i][j] || dp[i-1][j-arr[i]];

		   if(i == n-1 && j <= sum/2 && dp[i][j])
				set1_sum = max(set1_sum, j);
	   }
	}

	return abs((sum-set1_sum)-set1_sum);
}
```

---


