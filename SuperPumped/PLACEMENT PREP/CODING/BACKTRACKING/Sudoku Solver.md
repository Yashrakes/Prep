#### Metadata

timestamp: **08:45**  &emsp;  **08-07-2021**
topic tags: #backtracking , #imp 
question link: https://leetcode.com/problems/sudoku-solver/
resource:
parent link: [[BACKTRACKING GUIDE]]

---

# Sudoku Solver

### Question
Write a program to solve a Sudoku puzzle by filling the empty cells.

A sudoku solution must satisfy **all of the following rules**:

1.  Each of the digits `1-9` must occur exactly once in each row.
2.  Each of the digits `1-9` must occur exactly once in each column.
3.  Each of the digits `1-9` must occur exactly once in each of the 9 `3x3` sub-boxes of the grid.

The `'.'` character indicates empty cells.


---


### Approach

#### Code

``` cpp
class Solution {
public:
    void getNextEmptyCell(vector<vector<char>> &board, int &ni, int &nj, int sI, int sJ){
        int i = sI, j = sJ;
        
        //First check for an empty cell in the sith row
        while(j < 9){
            if(board[i][j] == '.'){
                ni = i;
                nj = j;
                return;
            }
            j++;
        }
        
        i++;
        for( ; i < 9; i++){
            for(j = 0; j < 9; j++){
                if(board[i][j] == '.'){
                    ni = i;
                    nj = j;
                    return;
                }
            }
        }
        
        //if the control comes here, then it means no empty cells left
        ni = -1, nj = -1;
        return;
    }
    
    void f(vector<vector<char>> &board, vector<vector<char>> &res, vector<vector<bool>> &rowv,
                vector<vector<bool>> &colv, vector<vector<bool>> &gridv, int sI, int sJ, int countFilled){
        
        if(countFilled == 81){
            res = board;
            return;
        }
        
        int nI, nJ;
        for(int i = 1; i <= 9; i++){
            if(rowv[sI][i] || colv[sJ][i] || gridv[(3*(sI/3)) + (sJ/3)][i]) 
                continue;
            
            getNextEmptyCell(board, nI, nJ, sI, sJ+1);
            
            board[sI][sJ] = (char)(i + '0');
            rowv[sI][i] = true;
            colv[sJ][i] = true;
            gridv[(3*(sI/3)) + (sJ/3)][i] = true;
            
            f(board, res, rowv, colv, gridv, nI, nJ, countFilled+1);
            
            board[sI][sJ] = '.';
            rowv[sI][i] = false;
            colv[sJ][i] = false;
            gridv[(3*(sI/3)) + (sJ/3)][i] = false;
        }
    }
    
    void solveSudoku(vector<vector<char>>& board) {
        
        //rowv[i][j] = true means that number j is present in row i
        //colv[i][j] = true means that number j is present in the ith column
        vector<vector<bool>> rowv(9, vector<bool>(10, false));
        vector<vector<bool>> colv(9, vector<bool>(10, false));
        vector<vector<bool>> gridv(9, vector<bool>(10, false));
        
        bool flag = true;
        int sI, sJ, countFilled = 0; //sI and sJ are the indices of first empty cell
        for(int i = 0; i < 9; i++){
            for(int j = 0; j < 9; j++){
                if(board[i][j] != '.'){
                    int num = board[i][j] - '0';
                    rowv[i][num] = true;
                    colv[j][num] = true;
                    gridv[(3*(i/3)) + (j/3)][num] = true;
                    countFilled++;
                }
                
                //Find first empty cell
                else if(flag){
                    flag = false;
                    sI = i;
                    sJ = j;
                }
            }
        }
        
        //Store the current state
        vector<vector<char>> res = board;
        f(board, res, rowv, colv, gridv, sI, sJ, countFilled);
        board = res;
    }
};

```

---


