
# Difference Array

>[Resource](https://codeforces.com/blog/entry/78762)

## Algorithm

The difference array technique works as follows:
1. Create an array `diff` of size `n+1` (one larger than your original array)
2. For each range update operation "add value `v` to range `[l,r]`":
    - Set `diff[l] += v`
    - Set `diff[r+1] -= v`
3. Compute the prefix sum of the `diff` array to get the final values

---

## Intuition

- The key intuition is that we're marking only the points where changes begin and end, rather than updating every element in the range.

- When we add `v` at position `l`, we're saying "from this point forward, add `v` to everything." When we subtract `v` at position `r+1`, we're saying "from this point forward, stop adding `v`." This creates a "window" where the value is applied only to the desired range.

- The prefix sum then accumulates these changes as we move through the array, giving us the correct final value at each position. It's like laying down a series of instructions about where values change, then walking through from start to finish to calculate the cumulative effect.

---

## Visualization

```
Original array:  [0, 0, 0, 0, 0, 0, 0, 0]
Operation: Add 3 to range [2,5]

diff array:      [0, 0, 3, 0, 0, 0, -3, 0]
                     ↑        ↑
                 Start here   End here
                 
After prefix sum: [0, 0, 3, 3, 3, 3, 0, 0]
                     ↑           ↑
                     Range [2,5] has 3 added
```

- Multiple operations stack naturally:
```
Add 3 to range [2,5]
Add 2 to range [1,3]

diff array:      [0, 2, 3, 0, 0, 0, -3, 0, -2]
                    ↑  ↑        ↑     ↑
                    
After prefix sum: [0, 2, 5, 5, 3, 3, 0, 0, 0]
```

---

## Use Cases

1. **Range Update Queries**: When you need to perform multiple "add value v to range [l,r]" operations efficiently
2. **Interval Coverage**: Counting how many intervals cover each point (each interval adds 1 to its range)
3. **Event Scheduling**: Tracking how many events are active at any given time (each event adds 1 from start time to end time)
4. **Range Addition**: Problems where you need to apply many additive operations to ranges without querying intermediate results
5. **Preprocessing**: Converting range updates into point queries for more complex algorithms

This technique is particularly powerful when you have many range updates but only need the final values, as it reduces the time complexity from O(updates × range_size) to O(updates + array_size).

---

## Example Problem:

>[3355. Zero Array Transformation I](https://leetcode.com/problems/zero-array-transformation-i/)

You are given an integer array `nums` of length `n` and a 2D array `queries`, where `queries[i] = [li, ri]`.

For each `queries[i]`:

- Select a subset of indices within the range `[li, ri]` in `nums`.
- Decrement the values at the selected indices by 1.

A **Zero Array** is an array where all elements are equal to 0.

Return `true` if it is _possible_ to transform `nums` into a **Zero Array** after processing all the queries sequentially, otherwise return `false`.

```
Input: nums = [1,0,1], queries = [[0,2]]

Output: true

Explanation:

For i = 0:
Select the subset of indices as [0, 2] and decrement the values at these indices by 1.
The array will become [0, 0, 0], which is a Zero Array.
```

#### Code

``` cpp
class Solution {
public:
    bool isZeroArray(vector<int>& nums, vector<vector<int>>& queries) {
        int n = nums.size();
        vector<int> differenceArray(n+1, 0);
        for (auto query : queries) {
            int L = query[0];
            int R = query[1];
            differenceArray[L]++;
            differenceArray[R+1]--;
        }

        // keeps track of maximum decrements that can happen for ith index
        int maxDecrementOperations = 0;
        for (int i = 0; i < n; i++) {
            maxDecrementOperations += differenceArray[i];
            if (nums[i] > maxDecrementOperations) {
                return false;
            }
        }

        return true;
    }
};
```

---
