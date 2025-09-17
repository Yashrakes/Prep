#### Metadata

timestamp: **22:57**  &emsp;  **09-06-2021**
topic tags: #binary_search 
list tags: #hard , #solve_again 
similar:
question link: https://leetcode.com/problems/minimum-space-wasted-from-packaging/
resource:
parent link: [[1. Topic Wise Problem Guide]]

---

# Minimum Space Wasted From Packaging

### Question

You have `n` packages that you are trying to place in boxes, **one package in each box**. There are `m` suppliers that each produce boxes of **different sizes** (with infinite supply). A package can be placed in a box if the size of the package is **less than or equal to** the size of the box.

The package sizes are given as an integer array `packages`, where `packages[i]` is the **size** of the `ith` package. The suppliers are given as a 2D integer array `boxes`, where `boxes[j]` is an array of **box sizes** that the `jth` supplier produces.

You want to choose a **single supplier** and use boxes from them such that the **total wasted space** is **minimized**. For each package in a box, we define the space **wasted** to be `size of the box - size of the package`. The **total wasted space** is the sum of the space wasted in **all** the boxes.

-   For example, if you have to fit packages with sizes `[2,3,5]` and the supplier offers boxes of sizes `[4,8]`, you can fit the packages of size-`2` and size-`3` into two boxes of size-`4` and the package with size-`5` into a box of size-`8`. This would result in a waste of `(4-2) + (4-3) + (8-5) = 6`.

Return _the **minimum total wasted space** by choosing the box supplier **optimally**, or_ `-1` _if it is **impossible** to fit all the packages inside boxes._ Since the answer may be **large**, return it **modulo** `109 + 7`.

>**Example 1:**
**Input:** packages = \[2,3,5\], boxes = \[\[4,8\],\[2,8\]\]
**Output:** 6
**Explanation**: It is optimal to choose the first supplier, using two size-4 boxes and one size-8 box.
The total waste is (4-2) + (4-3) + (8-5) = 6.

---


### Approach

1. Firstly, we sort the packages and the boxes arrays so that we can apply binary search
2. Let total be the sum of sizes of all packages, then 
>Total wasted space for any supplier 
	>= total - summation{boxSize*(No. of packages to be places in boxSize)}

3. We can find the no of packages that can be placed in a given box size by using upper_bound.
---


### Code

``` cpp
class Solution {
public:
    int minWastedSpace(vector<int>& packages, vector<vector<int>>& boxes) {
        
        sort(packages.begin(), packages.end());
        
        long total = 0;
        for(int val : packages)
            total += val;
        
        long res = LONG_MAX;
        for(auto B : boxes){
            sort(B.begin(), B.end());
            
            //if the largest packet cannot fit in the largest box, skip the supplier
            if(packages[packages.size() - 1] > B[B.size()-1]) continue;
            
            long curSum = 0, i = 0;
            for(int b : B){
                int j = upper_bound(packages.begin() + i, packages.end(), b) - packages.begin();
                curSum += b * (j - i); //(j-i) is the no. of packages that can fit in a box size of b
                i = j;
            }
            res = min(res, curSum);
        }
        
        return res == LONG_MAX ? -1 : (res - total) % 1000000007;
    }
};

```

---


