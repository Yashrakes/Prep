#### Metadata

timestamp: **22:30**  &emsp;  **06-07-2021**
topic tags: #backtracking , #imp
question link: https://leetcode.com/problems/permutation-sequence/
resource:
parent link: [[BACKTRACKING GUIDE]]

---

# Permutation Sequence

### Question

The set `[1, 2, 3, ..., n]` contains a total of `n!` unique permutations.

By listing and labeling all of the permutations in order, we get the following sequence for `n = 3`:

1.  `"123"`
2.  `"132"`
3.  `"213"`
4.  `"231"`
5.  `"312"`
6.  `"321"`

Given `n` and `k`, return the `kth` permutation sequence.

---


### Approach


#### Code

``` cpp
class Solution {
public:
    void getFact(vector<int>& fact, int n){
        
        int f = 1;
        //0! = 1
        fact.push_back(1);
        for(int i = 1; i <= n; i++){
            f *= i;
            fact.push_back(f);
        }
    }
    
    int getIndex(vector<bool>& digit, int n, int pos){
        
        int idx = 0;
        for(int i = 1; i <= n; i++){
            if(digit[i]) 
                continue;
            
            idx++;
            
            if(idx == pos) 
                return i;
        }
        return 0;
    }
    
    string getPermutation(int n, int k) {
        vector<int> fact;
        vector<bool> digit(n+1, false);
        getFact(fact, n);
        
        string res = "";
        int tempK = k, tempN = n, curr_digit_index, curr_digit;
        
        while(tempK > 0 && tempN > 0){
            curr_digit_index = (tempK-1)/fact[tempN-1] + 1;
            curr_digit = getIndex(digit, n, curr_digit_index);
            digit[curr_digit] = true;
            
            res += (char)(curr_digit + '0');
            
            tempK = (tempK-1)%fact[tempN-1] + 1;
            tempN--;
        }
        
        return res;
    }
};

```

---


