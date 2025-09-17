#### Metadata

timestamp: **14:54**  &emsp;  **24-07-2021**
topic tags: #array, #imp
question link: https://www.interviewbit.com/problems/max-distance/
resource: https://www.geeksforgeeks.org/given-an-array-arr-find-the-maximum-j-i-such-that-arrj-arri/
parent link: [[1. ARRAY GUIDE]]

---

# Max Distance

### Question
Given an array **A** of integers, find the maximum of **j - i** subjected to the constraint of **A[i] <= A[j]**.

---


### Approach
To solve this problem, we need to get two optimum indexes of arr[]: left index i and right index j. For an element arr[i], we do not need to consider arr[i] for left index if there is an element smaller than arr[i] on left side of arr[i]. Similarly, if there is a greater element on right side of arr[j] then we do not need to consider this j for right index. So we construct two auxiliary arrays LMin[] and RMax[] such that LMin[i] holds the smallest element on left side of arr[i] including arr[i], and RMax[j] holds the greatest element on right side of arr[j] including arr[j]. After constructing these two auxiliary arrays, we traverse both of these arrays from left to right. While traversing LMin[] and RMax[] if we see that LMin[i] is greater than RMax[j], then we must move ahead in LMin[] (or do i++) because all elements on left of LMin[i] are greater than or equal to LMin[i]. Otherwise we must move ahead in RMax[j] to look for a greater j – i value.


#### Code

``` cpp
int Solution::maximumGap(const vector<int> &A) {
    int n = A.size();
    vector<int> rightMax(n), leftMin(n);

    rightMax[n-1] = A[n-1];
    for(int i = n-2; i >= 0; i--)
        rightMax[i] = max(A[i], rightMax[i+1]);
    
    leftMin[0] = A[0];
    for(int i = 1; i < n; i++)
        leftMin[i] = min(A[i], leftMin[i-1]);

    int i = 0, j = 0, maxDiff = 0;
    while(i < n && j < n){
        if(leftMin[i] <= rightMax[j]){
            maxDiff = max(maxDiff, j-i);
            j++;
        }
        else
            i++;
    }

    return maxDiff;
}

```

---


