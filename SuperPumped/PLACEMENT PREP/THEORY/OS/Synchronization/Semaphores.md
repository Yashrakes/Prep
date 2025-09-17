#### Metadata
resource: https://www.geeksforgeeks.org/semaphores-in-process-synchronization/
tags: #semaphore
back link: [[1. Process Synchronization]]

---

# Semaphores

- A semaphore S is an integer variable that, apart from initialization, is accessed only through two standard atomic operations: wait() and signal().
- All modifications to the integer value of the semaphore in the wait() and signal() operations must be executed indivisibly. That is, when one process modifies the semaphore value, no other process can simultaneously modify that same semaphore value. 
- In addition, in the case of wait(S), the testing of the integer value of S (S ≤ 0), as well as its possible modification (S--), must be executed without interruption.

``` cpp
wait(S) {
	while (S <= 0)
		; // busy wait
	S--;
}

signal(S) {
	S++;
}
```

## Types

- `Binary Semaphore:` The value of a binary semaphore can range only between 0 and 1. Thus, binary semaphores behave similarly to mutex locks. In fact, on systems that do not  provide mutex locks, binary semaphores can be used instead for providing mutual exclusion.
- `Counting Semaphore:`Counting semaphores can be used to control access to a given resource consisting of a finite number of instances. The semaphore is initialized to the number of resources available. Each process that wishes to use a resource performs a wait() operation on the semaphore (thereby decrementing the count). When a process releases a resource, it performs a signal() operation (incrementing the count). When the count for the semaphore goes to 0, all resources are being used. After that, processes that wish to use a resource will block until the count becomes greater than 0.


## Implementation

- To overcome the need for busy waiting, we can modify the definition of the `wait()` and `signal() `operations as follows: 
- When a process executes the wait() operation and finds that the semaphore value is not positive, it must wait. However, rather than engaging in busy waiting, the process can `block` itself. 
- The `block` operation places a process into a waiting queue associated with the semaphore, and the state of the process is switched to the waiting state. Then control is transferred to the CPU scheduler, which selects another process to execute.
-  A process that is blocked, waiting on a semaphore S, should be restarted when some other process executes a signal() operation. The process is restarted by a `wakeup()` operation, which changes the process from the waiting state to the ready state. 
-  The process is then placed in the ready queue.

``` cpp
typedef struct {
	int value;
	struct process *list;
} semaphore;

wait(semaphore *S) {
	S->value--;
	if (S->value < 0) {
		add this process to S->list;
		block();
	}
}

signal(semaphore *S) {
	S->value++;
	if (S->value <= 0) {
		remove a process P from S->list;
		wakeup(P);
	}
}
```