#### Metadata

timestamp: **11:58**  &emsp;  **17-06-2021**
topic tags: #backtracking 
list tags:
similar:
question link: https://leetcode.com/problems/combination-sum/
resource: https://leetcode.com/problems/combination-sum/discuss/16502/A-general-approach-to-backtracking-questions-in-Java-(Subsets-Permutations-Combination-Sum-Palindrome-Partitioning)
parent link: [[BACKTRACKING GUIDE]]

---

# Combination Sum

### Question
Given an array of **distinct** integers `candidates` and a target integer `target`, return _a list of all **unique combinations** of_ `candidates` _where the chosen numbers sum to_ `target`_._ You may return the combinations in **any order**.

The **same** number may be chosen from `candidates` an **unlimited number of times**. Two combinations are unique if the frequency of at least one of the chosen numbers is different.

It is **guaranteed** that the number of unique combinations that sum up to `target` is less than `150` combinations for the given input.


---


### Approach

- Inclusion Exclusion used

---


### Code

``` cpp
class Solution {
public:
void f(vector<int>& candidates, vector<vector<int>>& res, vector<int>& cur, int i, int target){

	if(i >= candidates.size() || target < 0)
		return;

	if(target == 0){
		res.push_back(cur);
		return;
	}

	//Exclude
	f(candidates, res, cur, i+1, target);

	//Include
	if(candidates[i] <= target){
		cur.push_back(candidates[i]);
		f(candidates, res, cur, i, target-candidates[i]);
		cur.pop_back();
	}
}

vector<vector<int>> combinationSum(vector<int>& candidates, int target) {
	vector<vector<int>> res;
	vector<int> cur;
	f(candidates, res, cur, 0, target);
	return res;
}
};

```

---


