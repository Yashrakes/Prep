# Operating System Intro and Structure
1.  [What is an OS?](https://www.guru99.com/operating-system-tutorial.html)
2.  [Types of OS](https://www.geeksforgeeks.org/types-of-operating-systems/)
3.  [Kernel](https://afteracademy.com/blog/what-is-kernel-in-operating-system-and-what-are-the-various-types-of-kernel)
4.  [Micro kernel](https://www.geeksforgeeks.org/microkernel-in-operating-systems/)
5.  [Monolithic kernel](https://www.geeksforgeeks.org/monolithic-kernel-and-key-differences-from-microkernel/)
6.  [What is difference between monolithic and micro kernel?](https://stackoverflow.com/questions/4537850/what-is-difference-between-monolithic-and-micro-kernel)

#### Additional Reads

1. [What happens when we turn on computer?](https://www.geeksforgeeks.org/what-happens-when-we-turn-on-computer/)
2. [BIOS and UEFI](https://www.howtogeek.com/56958/htg-explains-how-uefi-will-replace-the-bios/)
3. [MBR and GPT](https://www.howtogeek.com/193669/whats-the-difference-between-gpt-and-mbr-when-partitioning-a-drive/)


---

# Process 

1. [Program and Process](https://www.geeksforgeeks.org/difference-between-program-and-process/)
2. [Process and Thread](https://www.geeksforgeeks.org/difference-between-process-and-thread/)
3. [[Process State]]
4. [Process Scheduling](https://www.tutorialspoint.com/operating_system/os_process_scheduling.htm)
	1. Process Scheduling Queue: Job, Ready and Device Queues
	2. Schedulers: Long term, Short term, **Mid term **
5. [Context Switch](https://www.tutorialspoint.com/what-is-context-switching-in-operating-system)
6. Zombie and Orphan Process
7. [Inter Process Communication](https://www.geeksforgeeks.org/inter-process-communication-ipc/)
8. [Pipes](https://www.tutorialspoint.com/inter_process_communication/inter_process_communication_pipes.htm)
9. [Maximum number of Zombie process a system can handle](https://www.geeksforgeeks.org/maximum-number-zombie-process-system-can-handle/)

---

# Threads


---
# Process Synchronization

## Critical Section Problem
- Critical section is a code segment that can be accessed by only one process at a time. Critical section contains shared variables which need to be synchronized to maintain consistency of data variables.
- Any solution to the critical section problem must satisfy three requirements:
	-   **Mutual Exclusion** : If a process is executing in its critical section, then no other process is allowed to execute in the critical section.
	-   **Progress** : If no process is executing in the critical section and other processes are waiting outside the critical section, then only those processes that are not executing in their remainder section can participate in deciding which will enter in the critical section next, and the selection can not be postponed indefinitely.
	-   **Bounded Waiting** : A bound must exist on the number of times that other processes are allowed to enter their critical sections after a process has made a request to enter its critical section and before that request is granted.
---

## Solution to the Critical Section Problem

- [[Peterson's Solution]] (Software Approach)
- Test and Set
- [[Mutex]] Locks
- [[Semaphores]]
- [[Producer Consumer]]

---


## Questions

1. What is race condition?
	-  where several processes access and manipulate the same data concurrently and the 		outcome of the execution depends on the particular order in which the access takes place, is called a race condition.
2. [Difference between mutex and semaphores](https://www.geeksforgeeks.org/mutex-vs-semaphore/)
3. [Deadlock and Starvation](https://www.geeksforgeeks.org/difference-between-deadlock-and-starvation-in-os/)
4. [Priority Inversion](https://www.geeksforgeeks.org/priority-inversion-what-the-heck/)
5. [What is a critical section](https://www.geeksforgeeks.org/g-fact-70/)
6. [[Why is Prempetive better than Non - Preemptive]]

---
# CPU Scheduling


---
# Deadlocks
1. [What is deadlock? Example](https://www.cs.rpi.edu/academics/courses/fall04/os/c10/)
2. [[Conditions for Deadlock]]
3. [Deadlock Prevention and Avoidance](https://www.geeksforgeeks.org/deadlock-prevention/)
4. [Banker's Algorithm](https://www.geeksforgeeks.org/bankers-algorithm-in-operating-system-2/)
	1. https://www.geeksforgeeks.org/bankers-algorithm-in-operating-system-2/
5. [Deadlock Detection and Recovery](https://www.geeksforgeeks.org/deadlock-detection-recovery/)

---
# Memory Management (Read text)

1. [Swapping](https://www.geeksforgeeks.org/swap-space-management-in-operating-system/)
2. [Variable/Dynamic Partitioning](https://www.geeksforgeeks.org/variable-or-dynamic-partitioning-in-operating-system/)
3. [Fixed Partitioning](https://www.geeksforgeeks.org/fixed-or-static-partitioning-in-operating-system/)
4. [Partition Allocation Methods in Memory Management](https://www.geeksforgeeks.org/partition-allocation-methods-in-memory-management/) First, Best, Worst Fit
5. Solution to external fragmentation:
	1. Compaction
	2. Non contiguous allocation: 
		1.  [Segmentation](https://www.geeksforgeeks.org/segmentation-in-operating-system/) , Leads to external fragmentation
		2. [Paging](https://www.geeksforgeeks.org/paging-in-operating-system/)
6. [How does the paging increase the context switch time?](https://gateoverflow.in/178062/paging-os-galvin-book-page-no-332)

---