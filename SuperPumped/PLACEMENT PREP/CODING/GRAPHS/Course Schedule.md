#### Metadata

timestamp: **16:48**  &emsp;  **03-07-2021**
topic tags: #graph , #imp 
question link: https://leetcode.com/problems/course-schedule/
resource:
parent link: [[1. GRAPH GUIDE]]

---

# Course Schedule

### Question

There are a total of `numCourses` courses you have to take, labeled from `0` to `numCourses - 1`. You are given an array `prerequisites` where `prerequisites[i] = [ai, bi]` indicates that you **must** take course `bi` first if you want to take course `ai`.

-   For example, the pair `[0, 1]`, indicates that to take course `0` you have to first take course `1`.

Return `true` if you can finish all courses. Otherwise, return `false`

---


### Approach
- if we model the courses as a graph, we can see that if there exists a cycle, we cannot complete all courses.
- Hence, we check for a `cycle in the graph`, where the nodes are the courses and the edges denote the prerequisite criteria
- We check for a cycle using DFS, we maintain the current path during dfs, if we encounter a node that is already visited and present in the path, then there exists a cycle


#### Code

``` cpp
//Cycle in a directed graph
bool isCycle(vector<bool>& visited, vector<bool>& path, int n, vector<vector<int>>& adjList){

	visited[n] = true;
	path[n] = true;

	bool res = false;

	//Go through all the neighbours of n
	for(auto adj : adjList[n]){
		//Cycle
		if(path[adj])
			return true;

		//Otherwise traverse the edge if not visited already
		if(!visited[adj])
			res = res || isCycle(visited, path, adj, adjList);

		//Optimization
		if(res) return true;
	}

	path[n] = false;
	return res;
}

bool canFinish(int numCourses, vector<vector<int>>& prerequisites) {
	vector<bool> visited(numCourses, false);
	vector<bool> path(numCourses, false);

	//Create a graph - adjacency list
	vector<vector<int>> adjList(numCourses);

	for(auto pre : prerequisites){
		//if (a, b) is the prereq
		//Add an edge from b to a
		adjList[pre[1]].push_back(pre[0]);
	}

	bool res = false;
	for(int i = 0; i < numCourses; i++){
		if(!visited[i])
			res = res || isCycle(visited, path, i, adjList);

		//If cycle exists, we cannot complete the courses, hence return false
		if(res) return false;
	}

	return !res;
}

```

---


