#### Metadata

timestamp: **17:52**  &emsp;  **21-06-2021**
topic tags: #binary_search 
list tags: #sde 
question link: https://www.interviewbit.com/problems/matrix-median/
resource: https://www.youtube.com/watch?v=63fPPOdIr2c&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=63
parent link: [[SDE SHEET]], [[1. BINARY SEARCH GUIDE]]

---

# Matrix Median

### Question

Given a matrix of integers **A** of size **N x M** in which each row is sorted.
Find an return the overall median of the matrix **A**.

**Note:** No extra memory is allowed.
**Note:** Rows are numbered from top to bottom and columns are numbered from left to right.

  
**Input Format**

```
The first and only argument given is the integer matrix A.
```

**Output Format**

```
Return the overall median of the matrix A.
```

**Constraints**

```
1 <= N, M <= 10^5
1 <= N*M  <= 10^6
1 <= A[i] <= 10^9
N*M is odd
```


>**For Example**
Input 1:
A =[
[1, 3, 5],
[2, 6, 9],
[3, 6, 9]  
]
Output 1: 5
Explanation 1: A = [1, 2, 3, 3, 5, 6, 6, 9, 9] Median is 5. So, we return 5.

---


### Approach: Binary Search

- Trivially, the sample space will be from 1 to 10^9, but we can reduce this by finding the smallest and the largest element in the array.
- Let mid be in the reduced range, target be half of the total no of elements( (n\*m +1)/2) and count be the no of elements lesser than equal to mid in the array.
- For mid to be the median, count has to be greater than equal to target. Therefore, if count is lesser than target, its not a valid case.


#### Algorithm

- Sample Space pattern : F\* T\*, find first T
- predicate: count < target
#### Complexity Analysis

#### Code

``` cpp
int Solution::findMedian(vector<vector<int> > &A) {
    int minV = INT_MAX, maxV = INT_MIN, n = A.size(), m = A[0].size();
    
    for(int i = 0; i < n; i++)
        minV = min(minV, A[i][0]);
    
    for(int i = 0; i < n; i++)
        maxV = max(maxV, A[i][m-1]);
        
    int low = minV, high = maxV, mid, target = ((n*m)+1)/2;
    while(low < high){
        mid = low + (high - low)/2;

        //Count the no of elements lesser than mid
        int count = 0;
        for(auto row : A)
            count += upper_bound(row.begin(), row.end(), mid) - row.begin();
		
		
		//F* T*, find first T
        if(count < target)
            low = mid+1;
        else 
            high = mid;
    }
    return low;
}


```

---


