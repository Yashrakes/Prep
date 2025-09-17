#### Metadata

timestamp: **16:08**  &emsp;  **14-08-2021**
topic tags: #array
question link: https://www.interviewbit.com/problems/find-permutation/
resource:
parent link: [[1. ARRAY GUIDE]]

---

# Find Permutation

### Question

Given a positive integer `n` and a string `s` consisting only of letters _D_ or _I_, you have to find any permutation of first `n` positive integer that satisfy the given input string.

_D_ means the next number is smaller, while _I_ means the next number is greater.

**Notes**

-   Length of given string `s` will always equal to `n - 1`
-   Your solution should run in linear time and space.

**Example :**

```
Input 1:

n = 3

s = ID

Return: [1, 3, 2]
```

---


### Approach

When the input string contains only _D_ or _I_ we just need to return all positive number upto n either in descending or ascending orders respectively.  
So if _n_ = 3, _s_ = “II”, return [1, 2, 3]

Now, starting with each character of the input string, we need to substitute an appropriate number(from _1_ to _n_) corresponding to each character(_I_ or _D_).

So, Suppose we started with a set corresponding to all the elements from that we need to make permutation(i.e all integer from _1_ to _n_).

As _I_ denotes the next number should be larger, we need to substitute smallest remaining number from our set corresponding to subsequent _I_ as it automatically makes the next element to be larger.

Similar things will happens with character _D_, we need to substitute the largest remaining number from our set.

As the input string size is n - 1, we to append the last integer to our answer

#### Code

``` cpp
vector<int> Solution::findPerm(const string A, int B) {
    int cmax = B, cmin = 1;
    vector<int> res;
    for(int i = 0; i < B-1; i++){
        if(A[i] == 'D'){
            res.push_back(cmax);
            cmax--;
        }
        else{
            res.push_back(cmin);
            cmin++;
        }
    }
    res.push_back(cmax);
    return res;
}
```

---


