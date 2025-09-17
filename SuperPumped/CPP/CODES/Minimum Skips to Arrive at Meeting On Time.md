#### Metadata

timestamp: **12:55**  &emsp;  **11-06-2021**
topic tags: #dp 
list tags: #hard , #solve_again 
similar:
question link: https://leetcode.com/problems/minimum-skips-to-arrive-at-meeting-on-time/
resource: https://www.youtube.com/watch?v=mKtvWPdnvzY
parent link: [[1. Topic Wise Problem Guide]]

---

# Minimum Skips to Arrive at Meeting On Time

### Question

You are given an integer `hoursBefore`, the number of hours you have to travel to your meeting. To arrive at your meeting, you have to travel through `n` roads. The road lengths are given as an integer array `dist` of length `n`, where `dist[i]` describes the length of the `ith` road in **kilometers**. In addition, you are given an integer `speed`, which is the speed (in **km/h**) you will travel at.

After you travel road `i`, you must rest and wait for the **next integer hour** before you can begin traveling on the next road. Note that you do not have to rest after traveling the last road because you are already at the meeting.

-   For example, if traveling a road takes `1.4` hours, you must wait until the `2` hour mark before traveling the next road. If traveling a road takes exactly `2` hours, you do not need to wait.

However, you are allowed to **skip** some rests to be able to arrive on time, meaning you do not need to wait for the next integer hour. Note that this means you may finish traveling future roads at different hour marks.

-   For example, suppose traveling the first road takes `1.4` hours and traveling the second road takes `0.6` hours. Skipping the rest after the first road will mean you finish traveling the second road right at the `2` hour mark, letting you start traveling the third road immediately.

Return _the **minimum number of skips required** to arrive at the meeting on time, or_ `-1` _if it is **impossible**_.

>**Example 1:**
**Input:** dist = \[1,3,2\], speed = 4, hoursBefore = 2
**Output:** 1
**Explanation:**
Without skipping any rests, you will arrive in (1/4 + 3/4) + (3/4 + 1/4) + (2/4) = 2.5 hours.
You can skip the first rest to arrive in ((1/4 + 0) + (3/4 + 0)) + (2/4) = 1.5 hours.
Note that the second rest is shortened because you finish traveling the second road at an integer hour due to skipping the first rest.

---


### Approach : DP

- Firstly we identify that there are three variables involved,
	- the current road, i ( i <= 1000)
	- minimum no of skips taken until now, j (j <= 1000)
	- and the total time take, k (k <= 10^7)
	<br>
- To solve the problem we can use dp, so we have two choices
	- First, use `dp[i][k] = j` , which gives us the the minimum no of skips taken after traveling the ith road in k hours. Since this approach will require floating point operations, the total operations will exceed `10^3 x 10^7`, which will in turn give a TLE.
	- Hence we use `dp[i][j] = k`


#### Algorithm
- For every road i, we have two choices
	- either `skip` from i-1 to i
	- or, `dont skip` from i-1 to i
- The required answer is the minimum of both the options
- Instead of dividing each distance by speed to calculate the time taken, we multiply each of the distance by speed as in
	-  (1/4 + 3/4) + (3/4 + 1/4) + (2/4) = 2.5 hours, now multiply it with the speed
	-   ((1/4 + 3/4) + (3/4 + 1/4) + (2/4)) x 4= 10 hours,
	-    we compare this to `speed x hoursBefore`.

<br>

- `NOTE` the technique used to add rest :
 ` ((dp[i-1][j] + dist[i-1] + speed - 1)/speed)*speed;`

#### Complexity Analysis
Space: O(n^2)
Time: O(n^2)



---


### Code

#### O(n^2) Space
``` cpp

class Solution {
public:
    int minSkips(vector<int>& dist, int speed, int hoursBefore) {
        
        int n = dist.size();
        
        vector<vector<int>> dp(n+1, vector<int>(n, 0));
        
        for(int i = 1; i <= n; i++)
            for(int j = 0; j < n; j++){
                //Dont skip
                dp[i][j] = ((dp[i-1][j] + dist[i-1] + speed - 1)/speed)*speed;
                
                //Skip
                if(j > 0)
                    dp[i][j] = min(dp[i][j], dp[i-1][j-1]+dist[i-1]);
            }
        
        for(int j = 0; j < n; j++)
            if(dp[n][j] <= (long)speed*hoursBefore)
                return j;
        
        return -1;
    }
};
```

---


#### O(n) Space

``` cpp

class Solution {
public:
    int minSkips(vector<int>& dist, int speed, int hoursBefore) {
        
        int n = dist.size();
        
        vector<vector<int>> dp(2, vector<int>(n, 0));
        
        for(int i = 1; i <= n; i++)
            for(int j = 0; j < n; j++){
                //Dont skip
                dp[i%2][j] = ((dp[(i-1)%2][j] + dist[i-1] + speed - 1)/speed)*speed;
                
                //Skip
                if(j > 0)
                    dp[i%2][j] = min(dp[i%2][j], dp[(i-1)%2][j-1]+dist[i-1]);
            }
        
        for(int j = 0; j < n; j++)
            if(dp[n%2][j] <= (long)speed*hoursBefore)
                return j;
        
        return -1;
    }
};

```

----