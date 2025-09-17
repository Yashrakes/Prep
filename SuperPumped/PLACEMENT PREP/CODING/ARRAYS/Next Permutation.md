#### Metadata

timestamp: **18:46**  &emsp;  **28-06-2021**
topic tags: #array , #imp
question link: https://leetcode.com/problems/next-permutation/
resource: https://www.youtube.com/watch?v=LuLCLgMElus&list=PLgUwDviBIf0rPG3Ictpu74YWBQ1CaBkm2&index=9
parent link: [[1. ARRAY GUIDE]]

---

# Next Permutation

### Question
Implement **next permutation**, which rearranges numbers into the lexicographically next greater permutation of numbers.

If such an arrangement is not possible, it must rearrange it as the lowest possible order (i.e., sorted in ascending order).

The replacement must be **[in place](http://en.wikipedia.org/wiki/In-place_algorithm)** and use only constant extra memory.


---


### Approach

#### Algorithm
- Four Part Algo:
	1. Find index 'k' from the right end, such that all element after k are in descending order.
	2. Next, from the right end, find the first element that is greater than `nums[k]`.
	3. Swap both the numbers
	4. Reverse the elements of the array after the kth index, ie from k+1 to n.

- To understand the algo, try doing many dry runs.

#### Complexity Analysis
- Time: O(n)
- Space: O(1)

#### Code

``` cpp
vector<int> nextPermutation(int n, vector<int> arr){
	int k;
	//Step 1
	for(k = n-2; k >= 0; k--)
		if(arr[k] < arr[k+1]) break;

	//Found the breakpoint
	if(k >= 0){
	
		//Step 2: From the right, find the first element greater than arr[k]
		int i = n-1;
		while(arr[i] <= arr[k])
			i--;
		
		//Step 3
		swap(arr[i], arr[k]);
		
		//Step 4
		reverse(arr.begin() + k + 1, arr.end());
	}
	//No breakpoint => arr is sorted in decreasing order, hence return
	//lowest arrangement, ie the reverse
	else
		reverse(arr.begin(), arr.end());
	return arr;
}

```

---


