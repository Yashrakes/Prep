tags: #dp, #bit_masking, #solve_again 
link: https://leetcode.com/problems/minimum-xor-sum-of-two-arrays/
resource: https://www.youtube.com/watch?v=Nn_4flUEcH8

---
# Minimum XOR Sum of Two Arrays

## Question

>You are given two integer arrays `nums1` and `nums2` of length `n`.
The **XOR sum** of the two integer arrays is `(nums1[0] XOR nums2[0]) + (nums1[1] XOR nums2[1]) + ... + (nums1[n - 1] XOR nums2[n - 1])` (**0-indexed**). <br>
For example, the **XOR sum** of `[1,2,3]` and `[3,2,1]` is equal to `(1 XOR 3) + (2 XOR 2) + (3 XOR 1) = 2 + 0 + 2 = 4`. <br>
Rearrange the elements of `nums2` such that the resulting **XOR sum** is **minimized**.
Return _the **XOR sum** after the rearrangement_.



>**Input:** nums1 = \[1,0,3\], nums2 = \[5,3,4\]
**Output:** 8
**Explanation:** Rearrange `nums2` so that it becomes `[5,4,3]`. 
The XOR sum is (1 XOR 5) + (0 XOR 4) + (3 XOR 3) = 4 + 4 + 0 = 8.

---




## Approach

- Since the max size of nums is 14 we can apply brute force and iterate over all permutations of nums2 and return the minimum answer.
- We use dp with bitmasking to optimize this approach.
- To begin with, for a particular element in nums1, we try the combination with every element in nums2 and then repeat the process with the next element of nums1 and nums2-previously chosen element. 
- To identify which element of nums2 have already been chosed, we use bitmasking using an int mask, wherein we set that particular bit to 1 if its chosen and to 0 if its not. Hence we need 14 bits to represent the entire array and hence mask will range from 0 to 2^14 -1;

---


## Code
``` cpp
	class Solution {

	public:

		int dp[14][16834];

		int helper(vector<int>& v1, vector<int>& v2, int i, int mask){

			int n = v1.size();

			if(i == n) return 0;

			if(dp[i][mask] != -1) return dp[i][mask];

			int ans  = INT_MAX;
			for(int j = 0; j < n; j++){

				//If v2[j] is not visited, i can pick it
				if((mask & 1 << j) == 0){
					ans = min(ans, (v1[i]^v2[j]) + helper(v1, v2, i+1, mask|1<<j));
				}
			}
			dp[i][mask] = ans;
			return ans;
		}

		int minimumXORSum(vector<int>& nums1, vector<int>& nums2) {
			memset(dp, -1, sizeof dp);
			return helper(nums1, nums2, 0, 0);
		}
	};
	
```