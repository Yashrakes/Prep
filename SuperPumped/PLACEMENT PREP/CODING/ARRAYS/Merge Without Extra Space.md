#### Metadata

timestamp: **16:12**  &emsp;  **28-06-2021**
topic tags: #array , #imp 
question link: https://practice.geeksforgeeks.org/problems/merge-two-sorted-arrays-1587115620/1
resource: https://www.geeksforgeeks.org/efficiently-merging-two-sorted-arrays-with-o1-extra-space/
parent link: [[1. ARRAY GUIDE]]

---

# Merge Without Extra Space

### Question
Given two sorted arrays **arr1[]** and **arr2[]** of sizes **n** and **m** in non-decreasing order. Merge them in sorted order without using any extra space. Modify arr1 so that it contains the first N elements and modify arr2 so that it contains the last M elements.


---


### Approach

#### Algorithm
 We start comparing elements that are far from each other rather than adjacent.   
For every pass, we calculate the gap and compare the elements towards the right of the gap. Every pass, the gap reduces to the ceiling value of dividing by 2.

#### Complexity Analysis
**Time Complexity:** O((n+m) log(n+m))  
**Auxilliary Space:** O(1)

#### Code

``` cpp
int nextGap(int gap){
    if(gap <= 1)
        return 0;
    return ceil(gap/2.0);
}


void merge(long long arr1[], long long arr2[], int n, int m) 
{ 
    
    int i, j, gap = n+m;
    for(gap = nextGap(gap); gap > 0; gap = nextGap(gap)){
        
		// comparing elements in the first array.	
        for(i = 0; i + gap < n; i++)
            if(arr1[i] > arr1[i+gap])
                swap(arr1[i], arr1[i+gap]);
        
		// comparing elements in both arrays.
        for(j = gap > n ? gap-n : 0; i < n && j < m; i++, j++)
            if(arr1[i] > arr2[j])
                swap(arr1[i], arr2[j]);
        
		// comparing elements in the second array.
        if(j < m)
            for(j = 0; j + gap < m; j++)
                if(arr2[j] > arr2[j+gap])
                    swap(arr2[j], arr2[j+gap]);
    }
}

```

---


