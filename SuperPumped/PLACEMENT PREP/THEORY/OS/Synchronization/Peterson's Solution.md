#### Metadata

back link: [[1. Process Synchronization]]
resource:

---

## Intro

- Peterson’s solution is `restricted to two processes` that alternate execution between their critical sections and remainder sections. The processes are numbered P0 and P1. For convenience, when presenting Pi, we use Pj to denote the other process; that is, j equals 1 − i.

- Peterson’s solution requires the two processes to share two data items:
int turn;
boolean flag[2];

- The variable turn indicates whose turn it is to enter its critical section. That is, if turn == i, then process Pi is allowed to execute in its critical section. The flag array is used to indicate if a process is ready to enter its critical section.

- To enter the critical section, process Pi first sets flag[i] to be true and then sets turn to the value j, thereby asserting that if the other process wishes to enter the critical section, it can do so. If both processes try to enter at the same time, turn will be set to both i and j at roughly the same time. Only one of these assignments will last; the other will occur but will be overwritten immediately. 
- The eventual value of turn determines which of the two processes is allowed
to enter its critical section first.


- Disadvantages of Peterson’s Solution:
	-   It involves Busy waiting
	-   It is limited to 2 processes.

#### Code
![[Pasted image 20210801192456.png]]

---