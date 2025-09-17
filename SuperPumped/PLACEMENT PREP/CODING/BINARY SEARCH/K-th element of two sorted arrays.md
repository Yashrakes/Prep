#### Metadata

timestamp: **12:09**  &emsp;  **23-06-2021**
topic tags: #binary_search 
list tags: #sde 
question link: https://practice.geeksforgeeks.org/problems/k-th-element-of-two-sorted-array1317/1#
resource: https://www.youtube.com/watch?v=nv7F4PiLUzo&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=66
parent link: [[SDE SHEET]], [[1. BINARY SEARCH GUIDE]]

---

# K-th element of two sorted arrays

### Question
Given two sorted arrays **arr1** and **arr2** of size **M** and **N** respectively and an element **K**. The task is to find the element that would be at the k’th position of the final sorted array

>Example
**Input:**
arr1[] = {2, 3, 6, 7, 9}
arr2[] = {1, 4, 8, 10}
k = 5
**Output:**
6
**Explanation:**
The final sorted array would be -
1, 2, 3, 4, 6, 7, 8, 9, 10
The 5th element of this array is 6.

---


### Approach: Binary Search

#### Code

``` cpp

int kthElement(int nums1[], int nums2[], int n, int m, int k)
{
	if(m < n) return kthElement(nums2, nums1, m, n, k);


	int low = max(0, k-m), high = min(k, n);

	while(low <= high){
		int cut1 = low + (high - low)/2;
		int cut2 = k - cut1;

		int left1  = cut1 == 0 ? INT_MIN : nums1[cut1-1];
		int left2  = cut2 == 0 ? INT_MIN : nums2[cut2-1];
		int right1 = cut1 >= n ? INT_MAX : nums1[cut1];
		int right2 = cut2 >= m ? INT_MAX : nums2[cut2];

		if(left1 <= right2 && left2 <= right1){
				return max(left1, left2);
		}
		else if(left1 > right2)
			high = cut1-1;
		else
			low = cut1+1;
	}
	return 0;
}
```

---


