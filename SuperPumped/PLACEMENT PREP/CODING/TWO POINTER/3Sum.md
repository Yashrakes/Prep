#### Metadata

timestamp: **16:31**  &emsp;  **30-06-2021**
topic tags: #two_pointer , #imp 
question link: https://leetcode.com/problems/3sum/
resource: [TUF](https://www.youtube.com/watch?v=onLoX6Nhvmg&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=41)
parent link: [[1. TWO POINTER GUIDE]]

---

# 3Sum

### Question

Given an integer array nums, return all the triplets `[nums[i], nums[j], nums[k]]` such that `i != j`, `i != k`, and `j != k`, and `nums[i] + nums[j] + nums[k] == 0`.

Notice that the solution set must not contain duplicate triplets.

---


### Approach
- Firstly we try to fix the first number of the triplet so that it reduces to a classic two pointer problem to find a subsegment of target sum with a fixed window of size 2
- Now, to handle duplicates, we sort the array.
- As we have fixed the first element of the triplet, our task reduces to finding two elements whose sum is equal to the negative of the first triplet. 
- Since the array is sorted, we keep a pointer low at the beginning of valid window and high at the end of the array and move the low towards the right and high towards the left as required.

#### Complexity Analysis
- Time: O(n^2)
- Space: O(1)
#### Code

``` cpp
vector<vector<int>> threeSum(vector<int>& nums) {

	sort(nums.begin(), nums.end());
	vector<vector<int>> res;
	int n = nums.size(), i = 0;

	while(i < n){
		int low = i+1;
		int high = n-1;

		while(low < high){
			if(nums[i] + nums[low] + nums[high] == 0){
				res.push_back({nums[i], nums[low], nums[high]});

				//Skip duplicates
				while(low+1 < n && nums[low] == nums[low+1]) low++;
				while(high-1 >= 0 && nums[high] == nums[high-1]) high--;

				low++;
				high--;
			}
			else if(nums[low] + nums[high] < -nums[i])
				low++;
			else
				high--;
		}

		//skip duplicates
		while(i+1 < n && nums[i] == nums[i+1]) i++;
		i++;
	}
	return res;
}

```

---


