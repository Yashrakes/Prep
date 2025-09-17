#### Metadata

timestamp: **15:41**  &emsp;  **17-06-2021**
topic tags: #backtracking 
list tags: #hard , #solve_again 
question link: https://leetcode.com/problems/combination-sum-ii/
parent link: [[BACKTRACKING GUIDE]]

---

# Combination Sum II

### Question
Given a collection of candidate numbers (`candidates`) and a target number (`target`), find all unique combinations in `candidates` where the candidate numbers sum to `target`.

Each number in `candidates` may only be used **once** in the combination.

**Note:** The solution set must not contain duplicate combinations.

>**Example 1:**
**Input:** candidates = \[10,1,2,7,6,1,5\], target = 8
**Output:** 
\[
\[1,1,6\],
\[1,2,5\],
\[1,7\],
\[2,6\]
\]


---


### Approach: Inclusion/Exclusion
- Important to realize the need to skip duplicates in the exclude part 

#### Code 1

``` cpp
void f(vector<vector<int>>& res, vector<int>& cur, vector<int>& c, int begin, int target){

	if(target == 0){
		res.push_back(cur);
		return;
	}

	for(int i = begin; i < c.size() && c[i] <= target; i++){
		if(i > begin && c[i] == c[i-1]) continue;
		cur.push_back(c[i]);
		f(res, cur, c, i+1, target-c[i]);
		cur.pop_back();
	}
}


vector<vector<int>> combinationSum2(vector<int>& c, int target) {
	sort(c.begin(), c.end());
	vector<vector<int>> res;
	vector<int> cur;
	f(res, cur, c, 0, target);
	return res;    
}

```

#### Code 2

``` cpp
void f2(vector<vector<int>>& res, vector<int>& cur, vector<int>& c, int i, int target){

	if(target == 0){
		res.push_back(cur);
		return;
	}

	if(target < 0 || i >= c.size()) return;  

	if(c[i] > target) return;

	int k = 0;
	while(i+k < c.size() && c[i+k] == c[i]) k++;

	//Exclude
	f2(res, cur, c, i+k, target);

	//Include

	cur.push_back(c[i]);
	f2(res, cur, c, i+1, target-c[i]);
	cur.pop_back();

}

vector<vector<int>> combinationSum2(vector<int>& c, int target) {
	sort(c.begin(), c.end());
	vector<vector<int>> res;
	vector<int> cur;
	f2(res, cur, c, 0, target);
	return res;    
}
```

---


