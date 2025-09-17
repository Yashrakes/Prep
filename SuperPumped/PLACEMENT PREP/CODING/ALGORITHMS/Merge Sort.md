#### Metadata

timestamp: **16:46**  &emsp;  **22-07-2021**
question link: https://practice.geeksforgeeks.org/problems/merge-sort/1#
resource: https://www.geeksforgeeks.org/merge-sort/
parent link: [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]], [[SEARCHING AND SORTING GUIDE]]

---

# Merge Sort

#### Algorithm
```
MergeSort(arr[], l,  r)
If r > l
     1. Find the middle point to divide the array into two halves:  
             middle m = l+ (r-l)/2
     2. Call mergeSort for first half:   
             Call mergeSort(arr, l, m)
     3. Call mergeSort for second half:
             Call mergeSort(arr, m+1, r)
     4. Merge the two halves sorted in step 2 and 3:
             Call merge(arr, l, m, r)
```

#### Complexity Analysis
**Time Complexity:** Sorting arrays on different machines. Merge Sort is a recursive algorithm and time complexity can be expressed as following recurrence relation.   
T(n) = 2T(n/2) + θ(n)

The above recurrence can be solved either using the Recurrence Tree method or the Master method. It falls in case II of Master Method and the solution of the recurrence is θ(nLogn). Time complexity of Merge Sort is  θ(nLogn) in all 3 cases (worst, average and best) as merge sort always divides the array into two halves and takes linear time to merge two halves.  

**Auxiliary Space:** O(n)
**Stable:** Yes

#### Code

``` cpp
void merge(int arr[], int l, int m, int r)
{
	 //First half is from l to m
	 //Second half is from m+1 to r
	 int n1 = m-l+1;
	 int n2 = r-m;

	 int L1[n1], L2[n2];
	 for(int i = 0; i < n1; i++)
		L1[i] = arr[l+i];

	 for(int i = 0; i < n2; i++)
		L2[i] = arr[m+1+i];
		
	 //Merge using two pointer technique
	 int i = 0, j = 0, k = l;
	 while(i < n1 && j < n2){
		 if(L1[i] < L2[j])
			 arr[k++] = L1[i++];
		 else
			 arr[k++] = L2[j++];
	 }

	 while(i < n1)
		arr[k++] = L1[i++];

	 while(j < n2)
		arr[k++] = L2[j++];
}

void mergeSort(int arr[], int l, int r)
{
	if(l < r){
		int mid = l + (r-l)/2;
		mergeSort(arr, l, mid);
		mergeSort(arr, mid+1, r);
		merge(arr, l, mid, r);
	}
}

```

---


