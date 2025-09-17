#### Metadata

timestamp: **17:26**  &emsp;  **09-07-2021**
topic tags: #graph , #imp, #topological_sort 
question link: https://practice.geeksforgeeks.org/problems/alien-dictionary/1#
resource: https://www.geeksforgeeks.org/given-sorted-dictionary-find-precedence-characters/
https://www.naukri.com/code360/problems/alien-dictionary_630423?leftPanelTabValue=SUBMISSION
parent link: [[1. GRAPH GUIDE]]

---

# Alien Dictionary

### Question

Given a sorted dictionary of an alien language having N words and k starting alphabets of standard dictionary. Find the order of characters in the alien language.  
**Note:** Many orders may be possible for a particular test case, thus you may return any valid order and output will be 1 if the order of string returned by the function is correct else 0 denoting incorrect string returned.

**NOTE: In the code k is alphaSize**

---


### Approach

- Pairwise compare strings and add an edge at every mismatch, then perform standard topological sort.

#### Code

``` cpp
//alphaSize starting alphabets of standard dictionary
string findOrder(string dict[], int N, int alphaSize) {
	//Construct the graph
	//1. Find out the nodes
	//2.Dont insert duplicates

	unordered_map<char, vector<char>> graph;

	//visited[i][j] = true indicates that there is an edge btw (char)i to (char)j
	vector<vector<bool>> visited(alphaSize, vector<bool>(alphaSize, false));
	unordered_map<char, int> in;

	//insert all the valid characters of the alphabet and initialize
	//its indegree as 0
	for(int  i = 0; i < N; i++){
		for(int j = 0; j < dict[i].length(); j++){
			if(in.find(dict[i][j]) == in.end())
				in.insert({dict[i][j], 0});
		}
	}


	//construct the graph
	for(int i = 0; i < N-1; i++){

		if(dict[i] == dict[i+1]) continue;

		//go over the strings untill a mismatch is found
		int l = 0, k = 0;
		while(l < dict[i].length() && k < dict[i+1].length() && dict[i][l] == dict[i+1][k]){
			l++;
			k++;
		}

		//if k reaches the end, then this is an invalid lex order
		if(k == dict[i+1].length()) return "";

		//No mismatch found
		if(l == dict[i].length()) continue;

		//Mismatch found, add an edge from dict[i][l] to dict[i+1][k] if not already added
		if(!visited[dict[i][l] - 'a'][dict[i+1][k] - 'a']){
			graph[dict[i][l]].push_back(dict[i+1][k]);
			visited[dict[i][l] - 'a'][dict[i+1][k] - 'a'] = true;

			//update indegree
			in[dict[i+1][k]]++;
		}
	}

	//Now we have the graph and indegree ready, so we can perform topoplogical sort
	queue<char> q;

	//push the nodes with indegree 0
	for(auto it : in)
		if(it.second == 0)
			q.push(it.first);

	string res = "";
	while(!q.empty()){
		char t = q.front();
		q.pop();

		res.push_back(t);

		//Iterate throught the nbrs and reduce their indegree
		for(auto it : graph[t]){
			in[it]--;

			if(in[it] == 0)
				q.push(it);
		}
	}

	return res.length() == alphaSize ? res : ""; 
}

```

---


