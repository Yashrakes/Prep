#### Metadata

timestamp: **17:26**  &emsp;  **06-06-2021**
topic tags: #array
list tags: #medium , #solve_again 
similar:
question link: https://leetcode.com/problems/longest-consecutive-sequence/
resource:
parent link: [[1. HASHING GUIDE]]

---

# Longest Consecutive Sequence

### Question
Given an unsorted array of integers `nums`, return _the length of the longest consecutive elements sequence._

You must write an algorithm that runs in `O(n)` time.

>**Example 1:**
**Input:** nums = \[100,4,200,1,3,2\]
**Output:** 4
**Explanation:** The longest consecutive elements sequence is `[1, 2, 3, 4]`. Therefore its length is 4.


---


### Approach

#### Algorithm

The numbers are stored in a `HashSet` (or `Set`, in Python) to allow O(1)O(1) lookups, and we only attempt to build sequences from numbers that are not already part of a longer sequence. This is accomplished by first ensuring that the number that would immediately precede the current number in a sequence is not present, as that number would necessarily be part of a longer sequence.


#### Complexity Analysis
-   Time complexity : O(n).
    
    Although the time complexity appears to be quadratic due to the `while` loop nested within the `for` loop, closer inspection reveals it to be linear. Because the `while` loop is reached only when `currentNum` marks the beginning of a sequence (i.e. `currentNum-1` is not present in `nums`), the `while` loop can only run for nn iterations throughout the entire runtime of the algorithm. This means that despite looking like O(n \\cdot n)O(n⋅n) complexity, the nested loops actually run in O(n + n) = O(n)O(n+n)\=O(n) time. All other computations occur in constant time, so the overall runtime is linear.
    
-   Space complexity : O(n).
    
    In order to set up O(1)O(1) containment lookups, we allocate linear space for a hash table to store the O(n)O(n) numbers in `nums`. Other than that, the space complexity is identical to that of the brute force solution.

---


### Code

``` cpp
class Solution {
public:
    int longestConsecutive(vector<int>& nums) {
        
        unordered_set<int> numSet(nums.begin(), nums.end());
        
        int longestStreak = 0;
        
        for(auto num : nums){
            
            //if n-1 exists, then the current no is not the beginning of the sequence, hence skip it
            if(numSet.find(num-1) != numSet.end()) continue;
            
            //we come here because n-1 does not exist, hence there can be a sequence starting from num
            int currStreak = 1;
            while(numSet.find(num+1) != numSet.end()){
                currStreak++;
                num++;
            }
            longestStreak = max(longestStreak, currStreak);
            
        }
        return longestStreak;
    }
};

```

---


