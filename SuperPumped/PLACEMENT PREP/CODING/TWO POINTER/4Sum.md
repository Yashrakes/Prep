#### Metadata

timestamp: **19:01**  &emsp;  **01-07-2021**
topic tags: #two_pointer , #imp 
question link: https://leetcode.com/problems/4sum/
resource:
parent link: [[1. TWO POINTER GUIDE]]

---

# 4Sum

### Question

Given an array `nums` of `n` integers, return _an array of all the **unique** quadruplets_ `[nums[a], nums[b], nums[c], nums[d]]` such that:

-   `0 <= a, b, c, d < n`
-   `a`, `b`, `c`, and `d` are **distinct**.
-   `nums[a] + nums[b] + nums[c] + nums[d] == target`

You may return the answer in **any order**.

---


### Approach

#### Algorithm
We can implement `k - 2` loops using a recursion. We will pass the starting point and `k` as the parameters. When `k == 2`, we will call `twoSum`, terminating the recursion.

1.  For the main function:
    
    -   Sort the input array `nums`.
    -   Call `kSum` with `start = 0`, `k = 4`, and `target`, and return the result.
2.  For `kSum` function:
    
    -   Check if the sum of `k` smallest values is greater than `target`, or the sum of `k` largest values is smaller than `target`. Since the array is sorted, the smallest value is `nums[start]`, and largest - the last element in `nums`.
        -   If so, no need to continue - there are no `k` elements that sum to `target`.
    -   If `k` equals `2`, call `twoSum` and return the result.
    -   Iterate `i` through the array from `start`:
        -   If the current value is the same as the one before, skip it.
        -   Recursively call `kSum` with `start = i + 1`, `k = k - 1`, and `target - nums[i]`.
        -   For each returned `set` of values:
            -   Include the current value `nums[i]` into `set`.
            -   Add `set` to the result `res`.
    -   Return the result `res`.
3.  For `twoSum` function:
    
    -   Set the low pointer `lo` to `start`, and high pointer `hi` to the last index.
    -   While low pointer is smaller than high:
        -   If the sum of `nums[lo]` and `nums[hi]` is less than `target`, increment `lo`.
            -   Also increment `lo` if the value is the same as for `lo - 1`.
        -   If the sum is greater than `target`, decrement `hi`.
            -   Also decrement `hi` if the value is the same as for `hi + 1`.
        -   Otherwise, we found a pair:
            -   Add it to the result `res`.
            -   Decrement `hi` and increment `lo`.
    -   Return the result `res`.
#### Complexity Analysis
-   Time Complexity: O(n^k−1), or O(n3) for 4Sum. We have k−2 loops, and `twoSum` is O(n).
    
    Note that for k>2, sorting the array does not change the overall time complexity.
    
-   Space Complexity: O(n). We need O(k) space for the recursion. kk can be the same as n in the worst case for the generalized algorithm.
    
    Note that, for the purpose of complexity analysis, we ignore the memory required for the output.
#### Code : This is a generic Solution for KSum

``` cpp
vector<vector<int>> fourSum(vector<int>& nums, int target) {
	sort(nums.begin(), nums.end());
	return kSum(nums, target, 0, 4);
}

vector<vector<int>> kSum(vector<int>& nums, int target, int start, int k){
	vector<vector<int>> res;
	int n = nums.size();

	if(start == n || nums[start]*k > target || nums[n-1]*k < target)
		return res;

	if(k == 2)
		return twoSum(nums, target, start);

	for(int i = start; i < n; i++){
		if(i == start || nums[i] != nums[i-1])
			for(auto row : kSum(nums, target-nums[i], i+1, k-1)){
				row.push_back(nums[i]);
				res.push_back(row);
			}
	}
	return res;
}

vector<vector<int>> twoSum(vector<int>& nums, int target, int start){
	vector<vector<int>> res;
	int n = nums.size(), low = start, high = n-1;

	while(low < high){
		int sum = nums[low] + nums[high];
		if(sum == target){
			res.push_back({nums[low], nums[high]});

			while(low+1 < n && nums[low] == nums[low+1]) low++;
			while(high-1 >= 0 && nums[high] == nums[high-1]) high--;

			low++;
			high--;
		}
		else if(sum < target)
			low++;
		else
			high--;
	}

	return res;
} 

```

---


