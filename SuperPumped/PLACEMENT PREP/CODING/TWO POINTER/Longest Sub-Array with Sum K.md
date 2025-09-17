#### Metadata

timestamp: **18:52**  &emsp;  **30-06-2021**
topic tags: #two_pointer , #imp 
question link: https://practice.geeksforgeeks.org/problems/longest-sub-array-with-sum-k0809/1#
resource: https://www.youtube.com/watch?v=cyu_nuW5utA&list=PL_z_8CaSLPWeM8BDJmIYDaoQ5zuwyxnfj&index=7
parent link: [[1. TWO POINTER GUIDE]]

---

# Longest Sub-Array with Sum K

### Question

Given an array containing **N** integers and an integer **K**., Your task is to find the length of the longest Sub-Array with the sum of the elements equal to the given value **K**.

>**Example 1:**  
**Input :** A[] = {10, 5, 2, 7, 1, 9}
K = 15
**Output :** 4
**Explanation:** The sub-array is **{5, 2, 7, 1}**. 


>**Example 2:**
**Input :** 
A[] = {-1, 2, 3}
K = 6
**Output :** 0


**NOTE:** The array contains negative elements as well.


---


### Approach

#### Algorithm
1.  Initialize **sum** = 0 and **maxLen** = 0.
2.  Create a hash table having **(sum, index)** tuples.
3.  For i = 0 to n-1, perform the following steps:
    1.  Accumulate arr[i] to **sum**.
    2.  If sum == k, update **maxLen** = i+1.
    3.  Check whether **sum** is present in the hash table or not. If not present, then add it to the hash table as **(sum, i)** pair.
    4.  Check if **(sum-k)** is present in the hash table or not. If present, then obtain index of **(sum-k)** from the hash table as **index**. Now check if maxLen < (i-index), then update **maxLen** = (i-index).
4.  Return **maxLen**.


#### Complexity Analysis
**Time Complexity:** O(n).   
**Auxiliary Space:** O(n).

#### Code

``` cpp
int lenOfLongSubarr(int A[],  int n, int K) 
{ 
	unordered_map<int, int> map;
	int sum = 0, maxL = 0, i = 0, j = 0;

	for(int i = 0; i < n; i++){
		sum += A[i];

		if(sum == K) maxL = i+1;

		if(map.find(sum) == map.end())
			map[sum] = i;

		if(map.find(sum-K) != map.end())
			maxL = max(maxL, i - map[sum-K]);

	}
	return maxL;
}

```

---


