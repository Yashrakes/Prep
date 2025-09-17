#### Metadata

timestamp: **23:22**  &emsp;  **06-07-2021**
topic tags: #backtracking , #imp
question link: https://leetcode.com/problems/n-queens/
resource:
parent link: [[BACKTRACKING GUIDE]]

---

# N-Queens

### Question

The **n-queens** puzzle is the problem of placing `n` queens on an `n x n` chessboard such that no two queens attack each other.

Given an integer `n`, return _all distinct solutions to the **n-queens puzzle**_. You may return the answer in **any order**.

Each solution contains a distinct board configuration of the n-queens' placement, where `'Q'` and `'.'` both indicate a queen and an empty space, respectively.

---


### Approach

#### Code

``` cpp
class Solution {
public:
    void solve(vector<vector<string>> &res, vector<string> &currBoard, vector<bool> &rowV, vector<bool> &colV,                       vector<bool> &pdV, vector<bool> &sdV, int n, int row){
        
        if(row == n){
            res.push_back(currBoard);
            return;
        }
        
        for(int col = 0; col < n; col++){
            //invalid queen position
            if(rowV[row] || colV[col] || pdV[row-col+n-1] || sdV[row+col]) continue;
            
            currBoard[row][col] = 'Q';
            rowV[row] = true;
            colV[col] = true;
            pdV[row-col+n-1] = true;
            sdV[row+col] = true;
            
            solve(res, currBoard, rowV, colV, pdV, sdV, n, row+1);
            
            //undo
            currBoard[row][col] = '.';
            rowV[row] = false;
            colV[col] = false;
            pdV[row-col+n-1] = false;
            sdV[row+col] = false;
            
        }
    }
    
    
    vector<vector<string>> solveNQueens(int n) {
        vector<string> currBoard(n, string(n, '.'));
        vector<vector<string>> res;
        
        //visited vector check
        vector<bool> rowV(n, false);
        vector<bool> colV(n, false);
        vector<bool> pdV(2*n-1, false);
        vector<bool> sdV(2*n-1, false);
        
        solve(res, currBoard, rowV, colV, pdV, sdV, n, 0);
        return res;
 
    }
};

```

---


