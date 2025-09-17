#### Metadata

timestamp: **00:14**  &emsp;  **22-06-2021**
topic tags: #binary_search 
list tags:
similar: https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/
question link: https://leetcode.com/problems/search-in-rotated-sorted-array/
resource:
parent link: [[SDE SHEET]], [[1. BINARY SEARCH GUIDE]]

---

# Search in Rotated Sorted Array

### Question
Suppose an array of length `n` sorted in ascending order is **rotated** between `1` and `n` times. For example, the array `nums = [0,1,2,4,5,6,7]` might become:

-   `[4,5,6,7,0,1,2]` if it was rotated `4` times.
-   `[0,1,2,4,5,6,7]` if it was rotated `7` times.

Notice that **rotating** an array `[a[0], a[1], a[2], ..., a[n-1]]` 1 time results in the array `[a[n-1], a[0], a[1], a[2], ..., a[n-2]]`.

Given the sorted rotated array `nums` of **unique** elements, return _the minimum element of this array_.

You must write an algorithm that runs in `O(log n) time.`

>**Example 1:**
**Input:** nums = \[3,4,5,1,2\]
**Output:** 1
**Explanation:** The original array was \[1,2,3,4,5\] rotated 3 times.


---


### Approach

- First step is to find the inflection point, that is the maximum or the minimum element, after which the array can be divided into two parts that will be sorted in ascending order. Then we can apply the standard binary search to find the element
- To find the minimum element, see this [problem](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/)
- Once we have the index of minimum element, there are two sorted arrays, 0 to index-1 and index to n-1;

#### Code
Try single pass as well
``` cpp
class Solution {
public:
    /*
    First we find the smallest element and divide the search space into two parts 
    and then apply the traditional binary search
    */
    int search(vector<int>& nums, int target) {
        int n = nums.size();
        
        int low = 0, high = n-1, mid;
        while(low < high){
            mid = low + (high - low)/2;
            if(nums[mid] <= nums[high])
                high = mid;
            else 
                low = mid + 1;
        }
        
        int minIndex = low;
        if(target >= nums[minIndex] && target <= nums[n-1]){
            low = minIndex;
            high = n-1;
        }
        else if(minIndex != 0 && target <= nums[minIndex-1] && target >= nums[0]){
            low = 0;
            high = minIndex-1;
        }
        else 
            return -1;
        
        while(low < high){
            mid = low + (high - low + 1)/2;
            if(nums[mid] > target)
                high = mid-1;
            else
                low = mid;
        }
        
        return nums[low] == target ? low : -1;
    }
};

```

---


