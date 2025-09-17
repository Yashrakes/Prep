#### Metadata

timestamp: **13:33**  &emsp;  **2021-06-05**
topic tags:  #bfs,  #graph
list tags: #solve_again,  #medium
similar:
question link: https://leetcode.com/problems/open-the-lock/
resource:
parent link: [[1. Topic Wise Problem Guide]]

---

# Open the Lock

### Question

You have a lock in front of you with 4 circular wheels. Each wheel has 10 slots: `'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'`. The wheels can rotate freely and wrap around: for example we can turn `'9'` to be `'0'`, or `'0'` to be `'9'`. Each move consists of turning one wheel one slot.

The lock initially starts at `'0000'`, a string representing the state of the 4 wheels.

You are given a list of `deadends` dead ends, meaning if the lock displays any of these codes, the wheels of the lock will stop turning and you will be unable to open it.

Given a `target` representing the value of the wheels that will unlock the lock, return the minimum total number of turns required to open the lock, or -1 if it is impossible.

>Example
**Input:** deadends = \["0201","0101","0102","1212","2002"\], target = "0202"
**Output:** 6
**Explanation:**
A sequence of valid moves would be "0000" -> "1000" -> "1100" -> "1200" -> "1201" -> "1202" -> "0202".
Note that a sequence like "0000" -> "0001" -> "0002" -> "0102" -> "0202" would be invalid,
because the wheels of the lock become stuck after the display becomes the dead end "0102".


---


### Approach


- Standard BFS
- Main observation is to find the list of next states


---


### Code

``` cpp
class Solution {
public:
    vector<string> getCombinations(string cur){
        vector<string> res;
        
        for(int i = 0; i < 4; i++){
            string up = cur, down = cur;
            
            up[i] = (up[i] == '9' ? '0' : up[i] + 1);
            down[i] = (down[i] == '0' ? '9' : down[i] - 1);
            
            res.push_back(up);
            res.push_back(down);
        }
        return res;
    }
    
    int openLock(vector<string> &deadends, string target) {
        
        unordered_set<string> deadendSet(deadends.begin(), deadends.end());
        unordered_set<string> visited;
        queue<string> q;
        
        if(deadendSet.find("0000") != deadendSet.end()) return -1;
        
        int level = 0;
        q.push("0000");
        visited.insert("0000");
        
        while(!q.empty()){
            
            int size = q.size();
            for(int i = 0; i < size; i++){
                string currentWheel = q.front();
                q.pop();

                if(currentWheel == target) return level;
                
                vector<string> combinations = getCombinations(currentWheel);
                for(string comb : combinations){
                    if(deadendSet.find(comb) == deadendSet.end() && visited.find(comb) == visited.end()){
                        visited.insert(comb);
                        q.push(comb);
                    }
                }
            }
            level++;
        }
        return -1;
    }
};

```

---


