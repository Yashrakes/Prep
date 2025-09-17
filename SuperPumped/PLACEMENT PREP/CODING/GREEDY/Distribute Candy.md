#### Metadata

timestamp: **19:54**  &emsp;  **08-08-2021**
topic tags: #greedy 
question link: https://www.interviewbit.com/problems/distribute-candy/
resource: https://medium.com/javarevisited/how-to-solve-the-minimum-candy-distribution-problem-4c868740f16a
parent link: [[1. GREEDY GUIDE]]

---

# Distribute Candy

### Question

There are **N** children standing in a line. Each child is assigned a rating value.

You are giving candies to these children subjected to the following requirements:

```
1. Each child must have at least one candy.
2. Children with a higher rating get more candies than their neighbors.
```

What is the minimum candies you must give?

---


### Approach


#### Code

``` cpp
int Solution::candy(vector<int> &ratings) {
    int n = ratings.size();

    vector<int> candies(n, 1);

    for(int i = n-1; i > 0; i--)
        if(ratings[i-1] > ratings[i])
            candies[i-1] = 1 + candies[i];

    for(int i = 0; i < n-1; i++)
        if(ratings[i+1] > ratings[i])
            candies[i+1] = max(candies[i+1], 1 + candies[i]);

    int res = 0;
    for(int it : candies)
        res += it;

    return res;
}
```

---


 