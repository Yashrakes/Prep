#### Metadata

timestamp: **22:02**  &emsp;  **01-07-2021**
topic tags: #hashing, #imp 
question link: https://practice.geeksforgeeks.org/problems/largest-subarray-with-0-sum/1#
resource: https://www.geeksforgeeks.org/find-the-largest-subarray-with-0-sum/
parent link: [[1. HASHING GUIDE]]

---

# Largest subarray with 0 sum

### Question

Given an array having both positive and negative integers. The task is to compute the length of the largest subarray with sum 0.

---


### Approach
The sum-index pair will be stored in a _hash-map_. A **Hash map** allows insertion and deletion of key-value pair in constant time. Therefore, the time complexity remains unaffected. So, if the same value appears twice in the array, it will be guaranteed that the particular array will be a zero-sum sub-array.

#### Complexity Analysis
-   **Time Complexity:** O(n), as use of the good hashing function, will allow insertion and retrieval operations in O(1) time.
-   **Space Complexity:** O(n), for the use of extra space to store the prefix array and hashmap.

#### Code

``` cpp
int maxLen(int arr[], int n)
{
    unordered_map<int, int> presum;
 
    int sum = 0;
    int max_len = 0; 
 
    for (int i = 0; i < n; i++) {
        // Add current element to sum
        sum += arr[i];
 
 
        if (sum == 0)
            max_len = i + 1;
 
        // Look for this sum in Hash table
        if (presum.find(sum) != presum.end()) {
            // If this sum is seen before, then update max_len
            max_len = max(max_len, i - presum[sum]);
        }
        else {
            // Else insert this sum with index in hash table
            presum[sum] = i;
        }
    }
 
    return max_len;
}

```

---


