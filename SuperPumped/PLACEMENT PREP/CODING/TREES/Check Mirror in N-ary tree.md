#### Metadata

timestamp: **17:17**  &emsp;  **25-06-2021**
tags: #imp, #binary_tree 
question link: https://practice.geeksforgeeks.org/problems/check-mirror-in-n-ary-tree1528/1
resource: 
	https://www.geeksforgeeks.org/check-mirror-n-ary-tree/
	https://www.youtube.com/watch?v=NGHaYEq2EFM
parent link: [[1. TREE GUIDE]]

---

# Check Mirror in N-ary tree

### Question

Given two **n**-ary trees. Check if they are mirror images of each other or not. You are also given **e** denoting the number of edges in both trees, and two arrays, **A[]** and **B[]**. Each array has 2\*e space separated values u,v denoting an edge from u to v for the both trees.

**Your Task:**  
You don't need to read input or print anything. Your task is to complete the function **checkMirrorTree()** which takes 2 Integers n, and e;  and two arrays A[] and B[] of size 2\*e as input and returns 1 if the trees are mirror images of each other and 0 if not.

---


### Approach
- The idea is to use an **unordered map of stacks** to check if given N-ary tree are mirror of each other or not.   
- Let the first n-ary tree be t1 and the second n-ary tree is t2. 
- For each node in t1, push its connected node in their corresponding stack in the map. 
- Now, for each node in t2, if their connected node match with the top of the stack, then pop elements from the stack.
- Otherwise, if the node does not match with the top of the stack then it means two trees are not mirror of each other.
- Therefore create a stack for each parent node and repeat the procedure.


#### Code

``` cpp
int checkMirrorTree(int n, int e, int A[], int B[]) {
	unordered_map<int, stack<int>> m;

	for(int i = 0; i < 2*e; i = i+2)
		m[A[i]].push(A[i+1]);

	for(int i = 0; i < 2*e; i = i+2){
		if(m[B[i]].top() != B[i+1]) return 0;
		m[B[i]].pop();
	}
	return 1;    
}

```

---


