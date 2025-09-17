
## Problem statement

>https://www.naukri.com/code360/problems/ninja-and-the-fence_3210208?topList=love-babbar-dsa-sheet-problems&leftPanelTab=0&utm_source=youtube&utm_medium=affiliate&utm_campaign=Lovebabbar


Ninja has given a fence, and he gave a task to paint this fence. The fence has 'N' posts, and Ninja has 'K' colors. Ninja wants to paint the fence so that not more than two adjacent posts have the same color.
Ninja wonders how many ways are there to do the above task, so he asked for your help.
Your task is to find the number of ways Ninja can paint the fence. Print the answer modulo 10^9 + 7.

**Example:**

```
Input: 'N' = 3, 'K' = 2
Output: 6
```

---

## Code

- `same[i]` = number of ways to paint i posts where the last two have the same color
- `diff[i]` = number of ways to paint i posts where the last two have different colors

### The Recurrence Relations in Plain Language

For any position i (i ≥ 3):
1. Ways to have posts (i-1) and i be the same color = Ways to have posts (i-2) and (i-1) be different colors
    - Because if posts (i-2) and (i-1) are different, we can make post i match post (i-1)
2. Ways to have posts (i-1) and i be different colors = (Ways to have posts (i-2) and (i-1) be same OR different) × (k-1)
    - Because no matter what posts (i-2) and (i-1) are, we have (k-1) choices for post i to be different from post (i-1)

``` cpp
#include <iostream>
#include <vector>
using namespace std;

const int MOD = 1e9 + 7;

int numWaysToPaintFence(int n, int k) {
    // Handle edge cases
    if (n == 0) return 0;
    if (n == 1) return k;
    
    // Initialize our two key variables
    // same[i] = ways to paint i posts where the last two are the same color
    // diff[i] = ways to paint i posts where the last two are different colors
    vector<long long> same(n + 1, 0);
    vector<long long> diff(n + 1, 0);
    
    // Base cases
    same[1] = 0;          // Can't have "same" with just one post
    diff[1] = k;          // Can paint one post in k ways
    
    same[2] = k;          // Choose same color for both posts (k ways)
    diff[2] = k * (k-1);  // Choose different colors (k * (k-1) ways)
    
    // Fill in the dp arrays
    for (int i = 3; i <= n; i++) {
        // To have last two the same color:
        // The last post must match the second-last post, but that requires
        // the second-last post to be different from the third-last post
        same[i] = diff[i-1];
        
        // To have last two different colors:
        // Take all ways to paint i-1 posts (both same[i-1] and diff[i-1])
        // and for each way, we have k-1 choices for the last post
        diff[i] = (same[i-1] + diff[i-1]) * (k - 1) % MOD;
    }
    
    // Total ways = ways ending with the same color + ways ending with different colors
    return (same[n] + diff[n]) % MOD;
}

int main() {
    int n, k;
    cin >> n >> k;
    
    int result = numWaysToPaintFence(n, k);
    cout << result << endl;
    
    return 0;
}
```

---
