#### Metadata

timestamp: **16:29**  &emsp;  **19-06-2021**
topic tags: 
list tags:
similar:
question link: https://leetcode.com/problems/range-sum-query-mutable/
resource:
parent link: [[1. Topic Wise Problem Guide]], [[Square Root Decomposition]],

---

# Range Sum Query - Mutable

### Question

Given an integer array `nums`, handle multiple queries of the following types:

1.  **Update** the value of an element in `nums`.
2.  Calculate the **sum** of the elements of `nums` between indices `left` and `right` **inclusive** where `left <= right`.

Implement the `NumArray` class:

-   `NumArray(int[] nums)` Initializes the object with the integer array `nums`.
-   `void update(int index, int val)` **Updates** the value of `nums[index]` to be `val`.
-   `int sumRange(int left, int right)` Returns the **sum** of the elements of `nums` between indices `left` and `right` **inclusive** (i.e. `nums[left] + nums[left + 1] + ... + nums[right]`).

>**Example 1:**
**Input**
\["NumArray", "sumRange", "update", "sumRange"\]
\[\[\[1, 3, 5\]\], \[0, 2\], \[1, 2\], \[0, 2\]\]
**Output**
\[null, 9, null, 8\]

>**Explanation**
NumArray numArray = new NumArray(\[1, 3, 5\]);
numArray.sumRange(0, 2); // return 1 + 3 + 5 = 9
numArray.update(1, 2);   // nums = \[1, 2, 5\]
numArray.sumRange(0, 2); // return 1 + 2 + 5 = 8

---


### Approach
The idea is to split the array in blocks with length of \\sqrt{n}n​. Then we calculate the sum of each block and store it in auxiliary memory `b`. To query `RSQ(i, j)`, we will add the sums of all the blocks lying inside and those that partially overlap with range \[i....j]\.

#### Algorithm
![Range sum query using SQRT decomposition](https://leetcode.com/problems/range-sum-query-mutable/Figures/307/307_RSQ_Sqrt.png)

_Figure 1. Range sum query using SQRT decomposition._

In the example above, the array `nums`'s length is `9`, which is split into blocks of size sqrt{9}. To get `RSQ(1, 7)` we add `b[1]`. It stores the sum of `range [3, 5]` and partially sums from `block 0` and `block 2`, which are overlapping boundary blocks.
#### Complexity Analysis
-   Time complexity : O(n)- preprocessing, O(sqrt{n})- range sum query, O(1) - update query
    
    For range sum query in the worst-case scenario we have to sum approximately 3 x sqrt{n} elements. In this case the range includes sqrt{n} - 2 blocks, which total sum costs sqrt{n} - 2 operations. In addition to this we have to add the sum of the two boundary blocks. This takes another 2 (sqrt{n} - 1) operations. The total amount of operations is around 3 x sqrt{n}.
    
-   Space complexity : O(sqrt{n})
    
 -  We need additional sqrt{n} memory to store all block sums.

#### Code

``` cpp
class NumArray {
public:
    vector<int> block;
    vector<int> arr;
    int n;
    int len;
    
    NumArray(vector<int>& nums) {
        arr = nums;
        n = nums.size();
        preProcess();
    }
    
    void preProcess(){
        double blockSize = sqrt(n);
        len = (int)ceil(n/blockSize);
        block = vector<int>(len, 0);
        
        
        for(int i = 0; i < n; i++)
            block[i/len] += arr[i];
    }
    
    void update(int index, int val) {
        block[index/len] += val - arr[index];
        arr[index] = val;
    }
    
    int sumRange(int left, int right) {
        
        int sum = 0;
        int startBlock = left/len;
        int endBlock = right/len;
        
        if(startBlock == endBlock){
            for(int i = left; i <= right; i++)
                sum += arr[i];
            return sum;
        }
        for(int i = startBlock+1; i < endBlock; i++)
            sum += block[i];
        
        for(int i = left; i <= len*(startBlock+1)-1; i++)
            sum += arr[i];
        
        for(int i = len*endBlock; i <= right; i++)
            sum += arr[i];
        
        return sum;
    }
};

/**
 * Your NumArray object will be instantiated and called as such:
 * NumArray* obj = new NumArray(nums);
 * obj->update(index,val);
 * int param_2 = obj->sumRange(left,right);
 */

```

---


