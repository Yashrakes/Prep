#### Metadata

timestamp: **09:55**  &emsp;  **06-07-2021**
topic tags: #graph , #imp 
question link: https://leetcode.com/problems/word-ladder/
resource: https://www.geeksforgeeks.org/word-ladder-length-of-shortest-chain-to-reach-a-target-word/
parent link: [[1. GRAPH GUIDE]]

---

# Word Ladder

### Question
A **transformation sequence** from word `beginWord` to word `endWord` using a dictionary `wordList` is a sequence of words `beginWord -> s1 -> s2 -> ... -> sk` such that:

-   Every adjacent pair of words differs by a single letter.
-   Every `si` for `1 <= i <= k` is in `wordList`. Note that `beginWord` does not need to be in `wordList`.
-   `sk == endWord`

Given two words, `beginWord` and `endWord`, and a dictionary `wordList`, return _the **number of words** in the **shortest transformation sequence** from_ `beginWord` _to_ `endWord`_, or_ `0` _if no such sequence exists._


>**Example 1:**
**Input:** beginWord = "hit", endWord = "cog", wordList = ["hot","dot","dog","lot","log","cog"]
**Output:** 5
**Explanation:** One shortest transformation sequence is "hit" -> "hot" -> "dot" -> "dog" -> cog", which is 5 words long.


---


### Approach

#### Algorithm

#### Complexity Analysis

#### Code

``` cpp
/*
if we consider the words as nodes, then the problem reduces to finding the 
shortest path in the graph, which can be done using BFS.

Now, instead of traversing the entire list to search for a valid 
transformation sequence, we can iterate over all the possible words that
differ by a letter from the current word and then check whether that word 
exists in the wordList or not.

The required shortest distance is essentially the level count of 
the endword in the graph.
*/
int ladderLength(string beginWord, string endWord, vector<string>& wordList) {

	//set to store the words
	unordered_set<string> dict(wordList.begin(), wordList.end());
	queue<pair<string, int>> q;
	unordered_set<string> visited;

	q.push({beginWord, 0});
	visited.insert(beginWord);

	while(!q.empty()){
		string curr_word = q.front().first;
		int dist = q.front().second;
		q.pop();

		if(curr_word == endWord)
			return dist+1;

		string temp = curr_word;
		for(int i = 0; i < curr_word.size(); i++){
			for(int k = 0; k < 26; k++){
				temp[i] = (char)(k + 'a');
				if(visited.find(temp) == visited.end() && dict.find(temp) != dict.end()){
					visited.insert(temp);
					q.push({temp, dist+1});
				}
			}

			//Reset temp
			temp[i] = curr_word[i];
		}
	}

	return 0;
}

```

---


