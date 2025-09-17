#### Metadata

resource: https://www.geeksforgeeks.org/producer-consumer-problem-using-semaphores-set-1/
back Link: [[1. Process Synchronization]]

---

## Bounded Buffer - BUFFER_SIZE-1

#### Intro

```
#define BUFFER SIZE 10

typedef struct {

. . .

}item;

item buffer[BUFFER SIZE];
int in = 0;
int out = 0;
```

- The shared buffer is implemented as a circular array with two logical pointers: in and out. 
- The variable `in` points to the next free position in the buffer; `out` points to the first full position in the buffer. 
- The buffer is `empty when in == out`; 
- The buffer is `full when ((in + 1) % BUFFER SIZE) == out.`

#### Producer
```
item next produced; 

while (true) {
	/* produce an item in next produced */
	
	while (((in + 1) % BUFFER SIZE) == out)
		; /* do nothing */
	
	buffer[in] = next produced;
	
	in = (in + 1) % BUFFER SIZE;
}

```

#### Consumer
```
item next consumed;

while (true) {
	while (in == out)
		; /* do nothing */
		
	next consumed = buffer[out];
	
	out = (out + 1) % BUFFER SIZE;
	
	/* consume the item in next consumed */
}
```

---


## Bounded Buffer - Using Semaphores

- We assume that the pool consists of n buffers, each capable of holding one item.  
- The `mutex` semaphore provides mutual exclusion for accesses to the buffer pool and is initialized to the value 1. 
- The empty and full semaphores count the number of empty and full buffers. 
- The semaphore `empty` is initialized to the value n.
- The semaphore `full` is initialized to the value 0.


``` cpp
Initialization:

int n;
semaphore mutex = 1;
semaphore empty = n;
semaphore full = 0
```
<br>


``` cpp 
Producer:

do{
	//produce an item

	wait(empty);
	wait(mutex);

	//place in buffer

	signal(mutex);
	signal(full);

}while(true);
```

<br>

``` cpp 
Consumer:

do{
	wait(full);
	wait(mutex);

	// remove item from buffer

	signal(mutex);
	signal(empty);

	// consumes item

}while(true);
```

---