#### Metadata

timestamp: **21:45**  &emsp;  **23-07-2021**
topic tags: #dp, #imp
question link: https://practice.geeksforgeeks.org/problems/optimal-strategy-for-a-game-1587115620/1#
resource: https://www.youtube.com/watch?v=ww4V7vRIzSk
parent link: [[1. DP GUIDE]]

---

# Optimal Strategy For A Game

### Question
You are given an array **A of size N**. The array contains integers and is of **even length**. The elements of the array represent N **coin** of **values V1, V2, ....Vn**. You play against an opponent in an **alternating** way.

In each **turn**, a player selects either the **first or last coin** from the **row**, removes it from the row permanently, and **receives the value** of the coin.

You need to determine the **maximum possible amount of money** you can win if you **go first**.  
**Note:** Both the players are playing optimally.


---


### Approach

#### Code

``` cpp
long long f(int arr[], int n, int i, int j, vector<vector<int>> &dp){
    if(i >= n || j < 0 || i > j)
        return 0;
        
    if(i == j)
        return arr[i];
        
    if(dp[i][j] != -1)
        return dp[i][j];

    int ch1 = arr[i] + min(f(arr, n, i+2, j, dp), f(arr, n, i+1, j-1, dp));
    int ch2 = arr[j] + min(f(arr, n, i+1, j-1, dp), f(arr, n, i, j-2, dp));
    
    return dp[i][j] = max(ch1, ch2);
}

long long maximumAmount(int arr[], int n) 
{
    // Your code here
    vector<vector<int>> dp(n, vector<int>(n, -1));
    return f(arr, n, 0, n-1, dp);
}
```

---


