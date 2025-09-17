#### Metadata

timestamp: **22:44**  &emsp;  **01-07-2021**
topic tags: #hashing , #imp 
question link: https://www.geeksforgeeks.org/count-number-subarrays-given-xor/
resource: [TUF](https://www.youtube.com/watch?v=lO9R5CaGRPY&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=24)
parent link: [[1. HASHING GUIDE]]

---

# Count the number of subarrays having a given XOR

### Question
Given an array of integers arr[] and a number m, count the number of subarrays having XOR of their elements as m.  

>**Input :** arr[] = {4, 2, 2, 6, 4}, m = 6
**Output :** 4
**Explanation :** The subarrays having XOR of their elements as 6 are {4, 2}, {4, 2, 2, 6, 4}, {2, 2, 6}, and {6}

>**Input :** arr[] = {5, 6, 7, 8, 9}, m = 5
**Output :** 2
**Explanation :** The subarrays having XOR of their elements as 5 are {5}  and {5, 6, 7, 8, 9}


---


### Approach

- Let us call the XOR of all elements 
	- in the range [i+1, j] as A, 
	- in the range [0, i] as B, 
	- and in the range [0, j] as C. 
- If we do XOR of B with C, the overlapping elements in [0, i] from B and C zero out, and we get XOR of all elements in the range [i+1, j], i.e. A. 
- Since A = B XOR C, we have B = A XOR C. 
- Now, if we know the value of C and we take the value of A as m, we get the count of A as the count of all B satisfying this relation. Essentially, we get the count of all subarrays having XOR-sum m for each C. As we take the sum of this count overall C, we get our answer.


#### Algorithm
1) Initialize ans as 0.
2) Compute xorArr, the prefix xor-sum array.
3) Create a map mp in which we store count of  all prefixes with XOR as a particular value. 
4) Traverse xorArr and for each element in xorArr
	1. If m^xorArr[i] XOR exists in map, then  there is another previous prefix with same XOR, i.e., there is a subarray ending at i with XOR equal to m. We add count of all such subarrays to result. 
   2. If xorArr[i] is equal to m, increment ans by 1.
   3. Increment count of elements having XOR-sum xorArr[i] in map by 1.
5) Return ans.

#### Complexity Analysis
**Time Complexity:** O(n)
**Space Complexity:** O(n)

#### Code

``` cpp
long long subarrayXor(int arr[], int n, int m)
{
	long long ans = 0; 
	unordered_map<int, int> mp;
    int xxor = 0;

	for (int i = 0; i < n; i++) {
	
		xxor ^= arr[i];
		int tmp = m ^ xxor;

		// If above XOR exists in map, then there
		// is another previous prefix with same
		// XOR, i.e., there is a subarray ending
		// at i with XOR equal to m.
		if(mp.find(tmp) != mp.end())
		    ans += ((long long)mp[tmp]);

		// If this subarray has XOR equal to m itself.
		if (xxor == m)
			ans++;

		mp[xxor]++;
	}
	return ans;
}

```

---


