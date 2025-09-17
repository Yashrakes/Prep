#### Metadata

timestamp: **16:00**  &emsp;  **08-06-2021**
topic tags: #greedy , #priority_queue 
list tags: #medium , #solve_again 
similar:
question link: https://leetcode.com/problems/maximum-average-pass-ratio/
resource:
parent link: [[1. Topic Wise Problem Guide]]
note: *Usage of syntax of priority queue*

---

# Maximum Average Pass Ratio

### Question
There is a school that has classes of students and each class will be having a final exam. You are given a 2D integer array `classes`, where `classes[i] = [passi, totali]`. You know beforehand that in the `ith` class, there are `totali` total students, but only `passi` number of students will pass the exam.

You are also given an integer `extraStudents`. There are another `extraStudents` brilliant students that are **guaranteed** to pass the exam of any class they are assigned to. You want to assign each of the `extraStudents` students to a class in a way that **maximizes** the **average** pass ratio across **all** the classes.

The **pass ratio** of a class is equal to the number of students of the class that will pass the exam divided by the total number of students of the class. The **average pass ratio** is the sum of pass ratios of all the classes divided by the number of the classes.

Return _the **maximum** possible average pass ratio after assigning the_ `extraStudents` _students._ Answers within `10-5` of the actual answer will be accepted.

>**Example 1:**
**Input:** classes = \[\[1,2\],\[3,5\],\[2,2\]\], `extraStudents` = 2
**Output:** 0.78333
**Explanation:** You can assign the two extra students to the first class. The average pass ratio will be equal to (3/4 + 3/5 + 2/2) / 3 = 0.78333.

>**Example 2:**
**Input:** classes = \[\[2,4\],\[3,9\],\[4,5\],\[2,10\]\], `extraStudents` = 4
**Output:** 0.53485


---


### Approach

#### Algorithm
- Pay attention to how much the pass ratio changes when you add a student to the class. If you keep adding students, what happens to the change in pass ratio? The more students you add to a class, the smaller the change in pass ratio becomes.
- Since the change in the pass ratio is always decreasing with the more students you add, then the very first student you add to each class is the one that makes the biggest change in the pass ratio.
- Because each class's pass ratio is weighted equally, it's always optimal to put the student in the class that makes the biggest change among all the other classes.
- Keep a max heap of the current class sizes and order them by the change in pass ratio. For each extra student, take the top of the heap, update the class size, and put it back in the heap.

#### Complexity Analysis




---


### Code

``` cpp
class Solution {
public:
    double calculateDelta(double pass, double size){
        return ((pass+1)/(size+1)) - (pass/size);
    }
    
    double maxAverageRatio(vector<vector<int>>& classes, int extraStudents) {
        
        //Max heap
        priority_queue<pair<double, array<int, 2>>> maxQ;
        double total = 0;
        
        for(auto c : classes){
            total += (double) c[0]/c[1];
            maxQ.push({calculateDelta(c[0], c[1]), {c[0], c[1]}});
        }
        
        while(extraStudents--){
            auto [added_profit, c] = maxQ.top();
            maxQ.pop();
            
            total += added_profit;
            maxQ.push({calculateDelta(c[0]+1, c[1]+1), {c[0]+1, c[1]+1}});
        }
        
        return total/classes.size();
    }
};

```

---


