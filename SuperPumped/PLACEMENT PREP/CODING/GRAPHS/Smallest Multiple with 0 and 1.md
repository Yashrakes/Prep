#### Metadata

timestamp: **09:28**  &emsp;  **11-08-2021**
topic tags: #graph , #imp 
question link: https://www.interviewbit.com/problems/smallest-multiple-with-0-and-1/
resource: https://www.youtube.com/watch?v=sdTubUR99OA
parent link: [[1. GRAPH GUIDE]]

---

# Smallest Multiple with 0 and 1

### Question
You are given an integer N. You have to find smallest multiple of N which consists of digits `0` and `1` only. Since this multiple could be large, return it in form of a string.

**Note**:

-   Returned string should not contain leading zeroes.

For example,

```
For N = 55, 110 is smallest multiple consisting of digits 0 and 1.
For N = 2, 10 is the answer.
```


---


### Approach

Let’s represent our numbers as strings here. Now, consider there are N states, where i’th state stores the smallest string which when take modulo with N gives i. Our aim is to reach state 0. Now, we start from state “1” and at each step we have two options, either to append “0” or “1” to current state. We try to explore both the options, but note that if I have already visited a state, why would I visit it again? It already stores the smallest string which achieves that state and if I visit it again with a new string it will surely have more characters than already stored string.

So, this is basically a BFS on the states. We’ll visit a state atmost once, hence overall complexity is O(N). Interesting thing is that I need not store the whole string for each state, I can just store the value modulo N and I can easily see which two new states I am going to.

But, how do we build the solution?  
If I reach a state x, I store two values

-   From which node I arrived at node x from. Say this is node y.
-   What(0 or 1) did I append at string at node y to reach node x

Using this information, I can build my solution by repeatedly going to parents starting from node 0.

> **NOTE:** For detail explanation, watch the video in metadata


#### Complexity Analysis
- Time: O(N)


#### Code

``` cpp
string Solution::multiple(int A)
{
    if(A == 0) return "0";
    if(A == 1) return "1";

    vector<int> parent(A, -1);
    vector<int> value(A, -1);
    queue<int> q;
    q.push(1);

    while(!q.empty()){
        int rem = q.front();
        q.pop();
        
        if(rem == 0){
            string res;
            for(int it = 0; it != 1; it = parent[it])
                res.push_back((char)(value[it] + '0'));
                
            res.push_back('1');
            reverse(res.begin(), res.end());
            return res;
        }
        
        for(int digit : {0, 1}){
            int next = (rem*10 + digit)%A;
            if(parent[next] == -1){
                parent[next] = rem;
                value[next] = digit;
                q.push(next);   
            }
        }
    } 

    return "-1";
}
```

---


