#### Metadata

timestamp: **14:11**  &emsp;  **23-06-2021**
topic tags: #bit_masking 
list tags: #sde 
question link: https://practice.geeksforgeeks.org/problems/allocate-minimum-number-of-pages0937/1#
resource:[TUF](https://www.youtube.com/watch?v=gYmWHvRHu-s&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=70)
parent link: [[SDE SHEET]], [[1. BINARY SEARCH GUIDE]]

---

# Allocate Minimum Number of Pages

### Question

You are given **N** number of books. Every ith book has **Ai** number of pages.   
You have to allocate books to **M** number of students. There can be many ways or permutations to do so. In each permutation, one of the **M** students will be allocated the maximum number of pages. Out of all these permutations, the task is to find that particular permutation in which the maximum number of pages allocated to a student is minimum of those in all the other permutations and print this minimum value. 

Each book will be allocated to exactly one student. Each student has to be allocated at least one book.

>**Example 1:**
**Input:** N = 4
A[] = {12,34,67,90}
M = 2
**Output:** 113 
**Explanation:** 
Allocation can be done in following ways:
{12} and {34, 67, 90} Maximum Pages = 191
{12, 34} and {67, 90} Maximum Pages = 157
{12, 34, 67} and {90}  Maximum Pages =113
Therefore, the minimum of these cases is 113, which is selected as the output.

---


### Approach 
- The idea is to use **Binary Search**. We fix a value for the number of pages as mid of current minimum and maximum. We initialize minimum and maximum as 0 and sum-of-all-pages respectively. If a current mid can be a solution, then we search on the lower half, else we search in higher half.  
- Now the question arises, how to check if a mid value is feasible or not? Basically, we need to check if we can assign pages to all students in a way that the maximum number doesn’t exceed current value. To do this, we sequentially assign pages to every student while the current number of assigned pages doesn’t exceed the value. In this process, if the number of students becomes more than m, then the solution is not feasible. Else feasible.
- NOTE: Low can be initialized to the maximum no of pages in a book.

#### Complexity Analysis
- Time: O(n x logn)
- Space: O(1)

#### Code

``` cpp
class Solution 
{
    public:
    //Function to find minimum number of pages.
    bool isPossible(int A[], int n, int m, int mpages){
		
		
		//count stores the no of students required to allocate all books
        int cur = 0,  count = 1;
        for(int i = 0; i < n; i++){
    
            if(A[i] > mpages) return false;
    
            if(cur + A[i] <= mpages)
                cur += A[i];
            else {
                count++;
                cur = A[i];
                if(count > m) return false;
            }
      
        }
    
        return true;
    }
    
    int findPages(int A[], int n, int m) 
    {
        //code here
        int sum = 0;
        for(int i = 0; i < n; i++) 
            sum += A[i];
    
        int low = 0, high = sum, mid;
    	
		//F* T*, find first T
        while(low < high){
            mid = low + (high - low)/2;
    
            if(isPossible(A, n, m, mid))
                high = mid;
            else
                low = mid + 1;
        }
    
        return low;
    }
};

```

---


