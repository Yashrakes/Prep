#### Metadata

timestamp: **10:47**  &emsp;  **31-07-2021**
topic tags: #array 
question link: https://www.interviewbit.com/old/problems/maximum-unsorted-subarray/
resource: https://www.geeksforgeeks.org/minimum-length-unsorted-subarray-sorting-which-makes-the-complete-array-sorted/
parent link: [[1. ARRAY GUIDE]]

---

# Maximum Unsorted Subarray

### Question

Given an unsorted array arr[0..n-1] of size n, find the minimum length subarray arr[s..e] such that sorting this subarray makes the whole array sorted.   
**Examples:**   
1) If the input array is [10, 12, 20, 30, 25, 40, 32, 31, 35, 50, 60], your program should be able to find that the subarray lies between the indexes 3 and 8.  
2) If the input array is [0, 1, 15, 25, 6, 7, 30, 40, 50], your program should be able to find that the subarray lies between the indexes 2 and 5.

---


### Approach

**1) Find the candidate unsorted subarray**   
a) Scan from left to right and find the first element which is greater than the next element. Let _s_ be the index of such an element. In the above example 1, _s_ is 3 (index of 30).   
b) Scan from right to left and find the first element (first in right to left order) which is smaller than the next element (next in right to left order). Let _e_ be the index of such an element. In the above example 1, e is 7 (index of 31).  

**2) Check whether sorting the candidate unsorted subarray makes the complete array sorted or not. If not, then include more elements in the subarray.**   
a) Find the minimum and maximum values in _arr[s..e]_. Let minimum and maximum values be _min_ and _max_. _min_ and _max_ for [30, 25, 40, 32, 31] are 25 and 40 respectively.   
b) Find the first element (if there is any) in _arr[0..s-1]_ which is greater than _min_, change _s_ to index of this element. There is no such element in above example 1.   
c) Find the last element (if there is any) in _arr[e+1..n-1]_ which is smaller than max, change _e_ to index of this element. In the above example 1, e is changed to 8 (index of 35)  

**3) Print** _**s**_ **and** _**e**_**.**

#### Code

``` cpp
void printUnsorted(int arr[], int n) {
	int s = 0, e = n-1, i, max, min;

	// step 1(a) of above algo
	for (s = 0; s < n-1; s++) {
		if (arr[s] > arr[s+1])
		break;
	}

	if (s == n-1) {
		cout << "The complete array is sorted";
		return;
	}

	// step 1(b) of above algo
	for(e = n - 1; e > 0; e--) {
		if(arr[e] < arr[e-1])
			break;
	}

	// step 2(a) of above algo
	max = arr[s]; min = arr[s];
	for(i = s + 1; i <= e; i++) {
		if(arr[i] > max)
			max = arr[i];
		if(arr[i] < min)
			min = arr[i];
	}

	// step 2(b) of above algo
	for( i = 0; i < s; i++) {
		if(arr[i] > min) {
			s = i;
			break;
		}    
	}

	// step 2(c) of above algo
	for( i = n -1; i >= e+1; i--) {
		if(arr[i] < max){
			e = i;
			break;
		}
	}

	// step 3 of above algo
	cout << "The unsorted subarray which"
		 << " makes the given array" << endl
		 << "sorted lies between the indees "
		 << s << " and " << e;
	return;
}
 
```

---


