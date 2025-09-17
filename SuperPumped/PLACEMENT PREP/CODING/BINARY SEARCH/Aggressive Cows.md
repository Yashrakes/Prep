#### Metadata

timestamp: **14:41**  &emsp;  **23-06-2021**
topic tags: #binary_search  
list tags: #sde 
question link: https://www.spoj.com/problems/AGGRCOW/
resource: [TUF](https://www.youtube.com/watch?v=wSOfYesTBRk&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=71)
parent link: [[SDE SHEET]], [[1. BINARY SEARCH GUIDE]]

---

# Aggressive Cows

### Question

Farmer John has built a new long barn, with N (2 <= N <= 100,000) stalls. The stalls are located along a straight line at positions x1,...,xN (0 <= xi <= 1,000,000,000).  
  
His C (2 <= C <= N) cows don't like this barn layout and become aggressive towards each other once put into a stall. To prevent the cows from hurting each other, FJ wants to assign the cows to the stalls, such that the minimum distance between any two of them is as large as possible. What is the largest minimum distance?

#### Example

**Input:**
1
5 3
1
2
8
4
9

**Output:**
3

**Output details:**
FJ can put his 3 cows in the stalls at positions 1, 4 and 8,  
resulting in a minimum distance of 3.

---


### Approach
- First we identify that the (low)minimum possible distance will be 1 and the (high)max possible distance will be the difference of the last stall and the first stall(in a sorted array).
- Next, we realize that in the range of [low, high] , if we pick a distance, then it is monotonic in nature. To elaborate, if some mid in the range [low, high] is a valid minimum distance, then we can say with certainty that all distances from [low, mid] are valid.
- Hence the search space becomes a T\*F\* pattern, where we need to find the last T.
- Hence we apply binary search.

#### Complexity
- Time: O(n x log(max distance))
- Space: O(1)

#### Code

``` cpp
#include <bits/stdc++.h>
using namespace std;
bool isPossible(vector<int> &stalls, int C, int dist)
{

    int i, j = 0, cows = 1;
    for (i = 1; i < stalls.size(); i++)
    {
        if (stalls[i] - stalls[j] >= dist)
        {
            cows++;
            j = i;
        }

        if (cows >= C)
            return true;
    }
    return false;
}

int main()
{
    // your code goes here
    int t;
    cin >> t;
    while (t--)
    {
        int N, C, ele;
        cin >> N >> C;

        vector<int> stalls(N);
        for (int i = 0; i < N; i++)
        {
            cin >> ele;
            stalls[i] = ele;
        }
		
        sort(stalls.begin(), stalls.end());

        int low = 1, high = stalls[N - 1] - stalls[0], mid;
        while (low < high)
        {
            mid = low + (high - low + 1) / 2;

            //T* F*, find last T
            if (isPossible(stalls, C, mid))
                low = mid;
            else
                high = mid - 1;
        }

        cout << low << endl;
    }
    return 0;
}

```

---


