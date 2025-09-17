#### Metadata

timestamp: **15:48**  &emsp;  **30-07-2021**
topic tags: #dp, #imp
question link: https://leetcode.com/problems/longest-arithmetic-subsequence/
resource:
parent link: [[1. DP GUIDE]]

---

# Longest Arithmetic Subsequence

### Question

Given an array `nums` of integers, return the **length** of the longest arithmetic subsequence in `nums`.

Recall that a _subsequence_ of an array `nums` is a list `nums[i1], nums[i2], ..., nums[ik]` with `0 <= i1 < i2 < ... < ik <= nums.length - 1`, and that a sequence `seq` is _arithmetic_ if `seq[i+1] - seq[i]` are all the same value (for `0 <= i < seq.length - 1`).

---


### Approach

#### Algorithm

#### Complexity Analysis

#### Code

``` cpp
int longestArithSeqLength(vector<int>& A) {
	int n = A.size(), i, j, maxL = 2, cd;
	int maxEle = *max_element(A.begin(), A.end());

	//dp definition: dp[i][j] is the length of maximum subsequence ending at A[i],
	//and with common differece j
	//since -maxEle <= j <= maxEle, we shift the index by + maxEle to handle negative cds
	vector<vector<int>> dp(n, vector<int>(2*maxEle+1, 0));

	//Order of filling: row by row
	for(i = 0; i < n; i++){

		//Decision on the predecessor
		for(j = 0; j < i; j++){
			cd = A[i] - A[j];

			//if there is no subsequence with common difference cd
			if(dp[j][cd+maxEle] == 0)
				dp[i][cd+maxEle] = max(dp[i][cd+maxEle], 2);
			else
				dp[i][cd+maxEle] = max(dp[i][cd+maxEle], dp[j][cd+maxEle] + 1);

			maxL = max(maxL, dp[i][cd+maxEle]); 
		}

	}     
	return maxL;
}

```

---


