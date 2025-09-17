#### Metadata

timestamp: **23:56**  &emsp;  **03-07-2021**
topic tags: #backtracking , #imp 
question link: https://leetcode.com/problems/palindrome-partitioning/
resource: [TUF](https://www.youtube.com/watch?v=WBgsABoClE0&list=PLgUwDviBIf0rQ6cnlaHRMuOp4H_D-7hwP&index=3)
parent link: [[BACKTRACKING GUIDE]]

---

# Palindrome Partitioning

### Question
Given a string `s`, partition `s` such that every substring of the partition is a **palindrome**. Return all possible palindrome partitioning of `s`.

A **palindrome** string is a string that reads the same backward as forward.


---


### Approach

#### Code

``` cpp
bool isPalindrome(string s, int start, int end) {
	while(start <= end){
		if(s[start] != s[end])
			return false;
		start++, end--;
	}
	return true;
}

void partitionHelper(string &s, int startIdx, vector<string>& currSet, vector<vector<string>>& res){
	//Base step
	if(startIdx == s.size()) {
		res.push_back(currSet);
		return;
	}

	//Recursive step
	int i, n = s.size();
	string prefix = "";
	for(i = startIdx; i < n; i++) {
		prefix += s[i];

		//If prefix is not palindrome, skip the function call and proceed to the next iteration
		if(!isPalindrome(prefix, 0, prefix.size()-1))
			continue;

		//if prefix is palindrome, pass the contribution
		currSet.push_back(prefix);

		//call the helper function on s - prefix
		partitionHelper(s, i+1, currSet, res);

		//restore the state of currSet
		currSet.pop_back();

	}
}
vector<vector<string>> partition(string s) {
	vector<string> currSet;
	vector<vector<string>> res;
	partitionHelper(s, 0, currSet, res);
	return res;
}

```

---


