#### Metadata

timestamp: **18:20**  &emsp;  **08-06-2021**
topic tags: #stack
list tags: #medium, #solve_again 
question link: https://leetcode.com/problems/largest-rectangle-in-histogram/
resource: [TUF](https://www.youtube.com/watch?v=X0X6G-eWgQ8&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=81)
parent link: [[1. Stacks & Queue Guide]]

---

# Largest Rectangle in Histogram

### Question

Given an array of integers `heights` representing the histogram's bar height where the width of each bar is `1`, return _the area of the largest rectangle in the histogram_.

>**Example 1:**
![](https://assets.leetcode.com/uploads/2021/01/04/histogram.jpg)
**Input:** heights = \[2,1,5,6,2,3\]
**Output:** 10
**Explanation:** The above is a histogram where width of each bar is 1.
The largest rectangle is shown in the red area, which has an area = 10 units.

---


### Approach
- Basic idea is that given a particular height, find the largest rectangle whose height is equal to the current height. This can be done by finding the left and rightmost bounds.
- The leftmost bound is equivalent of finding the smallest nearest element towards the left, the desired bound is 1 + the index of this element.
- Similarly right bound is the smallest nearest element towards the right.
- Watch the video for detailed understanding
#### Algorithm
First:
- For each bar, we find the leftmost and rightmost bound.
- We optimize it using stacks


#### Complexity Analysis

- Time: O(n)
- Space: 2 stacks


---


### Code

**NOTE:** Single pass solution: [TUF](https://www.youtube.com/watch?v=jC_cWLy7jSI&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=82)

Approach 1
``` cpp
class Solution {
public:
    int largestRectangleArea(vector<int>& heights) {
        
        int n = heights.size();
        vector<int> left(n), right(n);
        stack<int> s1, s2;

        for(int i = 0; i < n; i++){

            while(!s1.empty() && heights[s1.top()] >= heights[i])
                s1.pop();

            if(s1.empty())
                left[i] = 0;
            else
                left[i] = s1.top() + 1;

            s1.push(i);
        }

        for(int i = n-1; i >= 0; i--){

            while(!s2.empty() && heights[s2.top()] >= heights[i])
                s2.pop();

            if(s2.empty())
                right[i] = n-1;
            else    
                right[i] = s2.top()-1;

            s2.push(i);
        }

        int res = 0;
        for(int i = 0; i < n; i++)
            res = max(res, heights[i]*(right[i]-left[i]+1));

        return res;
    }
};

```

---


