
- thread vs process
- context switching
	- explain parallelism, how many threads can actually run concurrently, what determines this?
- two ways to implement
	- implements Runnable
	- extends Thread class
- thread lifecycle
- monitor locks on objects
- producer consumer problem and solution
	- synchronized
	- wait
	- notifyAll
	- sleep
	- start
- why stop, resume and suspended are deprecated methods

https://leetcode.com/problems/print-in-order/solutions/4292700/java-9-different-solutions-exchanger-barrier-latch-lock-etc/?envType=problem-list-v2&envId=concurrency


Still learning. There are just so many choices Java gave us to handle multithreading.

I am sure there are more ways to do this. And my knowledge is limited, I will come back to this post and improve and add more comparison once I understand better.

For synchronized solution:

If a thread calls wait() but no other threads are performing a corresponding notify() or notifyAll(), those threads will remain in the waiting state indefinitely. Moreover, if your application creates an excessive number of threads that all try to acquire the lock using synchronized blocks, it could lead to resource exhaustion or decreased performance due to excessive context switching.

We need to put wait() in a loop, also check the condition in the loop. Java does not guarantee that the thread will be woken up only by a notify()/notifyAll() call or the right notify()/notifyAll() call at all. Because of this property the loop-less version might work on your development environment and fail on the production environment unexpectedly.

A thread can also wake up without being notified, interrupted, or timing out, a so-called spurious wakeup. While this will rarely occur in practice, applications must guard against it by testing for the condition that should have caused the thread to be awakened, and continuing to wait if the condition is not satisfied. In other words, waits should always occur in loops, like this one:

```java
 synchronized (obj) {
     while (<condition does not hold>)
         obj.wait(timeout);
     ... // Perform action appropriate to condition
 }
```

[https://docs.oracle.com/javase/6/docs/api/java/lang/Object.html#wait(long)](https://docs.oracle.com/javase/6/docs/api/java/lang/Object.html#wait\(long\))

### volatile

volatile keyword id quite different from other mechanism. It's to tell JVM to put the variable in the main memory of the server, instead of the cache on the cpu. When we have multi threads running on the server, they run on diferent CPUs, then using their own caches, so one CPU may get older value when another CPU already updated the value in its cache. Volatile keyword ensures that any thread reading the variable sees the most recent modification made by any other thread.

We should understand that this is on web server level, not database level. A often brought up question during interviews is when we have multi web servers trying to update the same data in database, how do we make sure we are getting the latest update. Volatile will have nothing to do with that.

Another question is about redis, is there a similar mechanism in Redis? answer is no, since redis is single threaded by default, that'show it's built. How to sync up redis servers in different data centers is a totally different challenge.

```java
    public static volatile int methodCompleted;
    public Foo() {
        methodCompleted = 0;
    }

    public void first(Runnable printFirst) throws InterruptedException {
        // printFirst.run() outputs "first". Do not change or remove this line.
        printFirst.run();
        methodCompleted = 1;
    }

    public void second(Runnable printSecond) throws InterruptedException {
        while(methodCompleted != 1) ;
        // printSecond.run() outputs "second". Do not change or remove this line.
        printSecond.run();
        methodCompleted = 2;
    }

    public void third(Runnable printThird) throws InterruptedException {
        while(methodCompleted != 2) ;
        // printThird.run() outputs "third". Do not change or remove this line.
        printThird.run();
        methodCompleted = 3;
    }
```

### Exchanger

```java
import java.util.concurrent.Exchanger;

public class Foo {
    private final Exchanger<Boolean> exchanger12 = new Exchanger<>();
    private final Exchanger<Boolean> exchanger23 = new Exchanger<>();

    public Foo() {}

    public void first(Runnable printFirst) throws InterruptedException {
        printFirst.run();
        try {
            exchanger12.exchange(true); // Signal that first() has completed
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void second(Runnable printSecond) throws InterruptedException {
        try {
            exchanger12.exchange(true); // Wait for first() to complete
            printSecond.run();
            exchanger23.exchange(true); // Signal that second() has completed
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void third(Runnable printThird) throws InterruptedException {
        try {
            exchanger23.exchange(true); // Wait for second() to complete
            printThird.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### CyclicBarrier

```java
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Foo {
    private final CyclicBarrier barrier1 = new CyclicBarrier(2);
    private final CyclicBarrier barrier2 = new CyclicBarrier(2);

    public Foo() {}

    public void first(Runnable printFirst) throws InterruptedException {
        printFirst.run();
        try {
            barrier1.await();
        } catch (BrokenBarrierException e) {}

    }

    public void second(Runnable printSecond) throws InterruptedException {
        try {
            barrier1.await();
        } catch (BrokenBarrierException e) {}
        printSecond.run();
        try {
            barrier2.await();
        } catch (BrokenBarrierException e) {}
    }

    public void third(Runnable printThird) throws InterruptedException {
        try {
            barrier2.await();
        } catch (BrokenBarrierException e) {}
        printThird.run();
    }
}
```

### Latch:

```java
import java.util.concurrent.CountDownLatch;

public class Foo {
    private CountDownLatch latch1;
    private CountDownLatch latch2;

    public Foo() {
        latch1 = new CountDownLatch(1);
        latch2 = new CountDownLatch(1);
    }

    public void first(Runnable printFirst) throws InterruptedException {
        // printFirst.run() outputs "first". Do not change or remove this line.
        printFirst.run();
        latch1.countDown(); // Signal that the first method has been executed
    }

    public void second(Runnable printSecond) throws InterruptedException {
        latch1.await(); // Wait until the first method completes
        // printSecond.run() outputs "second". Do not change or remove this line.
        printSecond.run();
        latch2.countDown(); // Signal that the second method has been executed
    }

    public void third(Runnable printThird) throws InterruptedException {
        latch2.await(); // Wait until the second method completes
        // printThird.run() outputs "third". Do not change or remove this line.
        printThird.run();
    }
}
```

### Lock (same as Mutex):

```java
class Foo {
    private volatile int jobCompleted = 0;
    private final Lock lock = new ReentrantLock();
    private final Condition isJobCompleted = lock.newCondition();

    private void orderJob(final int jobNumber, Runnable job) throws InterruptedException {
        lock.lock();
        try {
            while (jobCompleted != jobNumber - 1) isJobCompleted.await();
            job.run();
            jobCompleted = jobNumber;
            isJobCompleted.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void first(Runnable printFirst) throws InterruptedException {
        orderJob(1, printFirst);
    }

    public void second(Runnable printSecond) throws InterruptedException {
        orderJob(2, printSecond);
    }

    public void third(Runnable printThird) throws InterruptedException {
        orderJob(3, printThird);
    }
}
```

### AtomicBoolean:

```java
class Foo {

  private AtomicBoolean firstJobDone = new AtomicBoolean(false);
  private AtomicBoolean secondJobDone = new AtomicBoolean(false);

  public Foo() {}

  public void first(Runnable printFirst) throws InterruptedException {
    // printFirst.run() outputs "first".
    printFirst.run();
    // mark the first job as done, by increasing its count.
    firstJobDone.set(true);
  }

/*Second and third threads in Java solution based on AtomicInteger will consume CPU cycles while waiting. This may be good to have faster runtime in the test, but in real life it is better to have these threads in WAITING state. For concurrency tasks it makes sense to add one more index - CPU load or something like this (in addition to runtime and memory distributions)
*/
  public void second(Runnable printSecond) throws InterruptedException {
    while (!firstJobDone.get()) {
      // waiting for the first job to be done.
    }
    // printSecond.run() outputs "second".
    printSecond.run();
    // mark the second as done, by increasing its count.
    secondJobDone.set(true);
  }

  public void third(Runnable printThird) throws InterruptedException {
    while (!secondJobDone.get()) {
      // waiting for the second job to be done.
    }
    // printThird.run() outputs "third".
    printThird.run();
  }
}
```

### AtomicBooleans Approach:

AtomicBooleans provide a simple mechanism for ensuring visibility and atomicity in thread-safe operations without explicit synchronization.  
The AtomicBoolean approach ensures that threads don't waste CPU cycles actively waiting for conditions to be met.  
However, the while loop continuously polls for the state change, potentially consuming CPU resources.

Everybody has seen these three:

### Semaphore:

```java
class Foo {
    // By starting with 0 permits, you ensure that any attempt to acquire a permit 
    // using s1.acquire() in the second method will block until s1.release() is called 
    Semaphore s1 = new Semaphore(0), s2 = new Semaphore(0);
    public Foo() {
        
    }

    public void first(Runnable printFirst) throws InterruptedException {
        
        // printFirst.run() outputs "first". Do not change or remove this line.
        printFirst.run();
        //  increases the number of available permits by 1.
        s1.release();
    }

    public void second(Runnable printSecond) throws InterruptedException {
        s1.acquire();
        // printSecond.run() outputs "second". Do not change or remove this line.
        printSecond.run();
        s2.release();
    }

    public void third(Runnable printThird) throws InterruptedException {
        s2.acquire();
        // printThird.run() outputs "third". Do not change or remove this line.
        printThird.run();
    }
}
```

### Semaphore Approach:

Semaphores are synchronization constructs used to control access to a shared resource.  
This approach allows threads to block until a signal (in the form of a release) is received, minimizing active CPU polling.  
Semaphores provide a more efficient solution in terms of not actively consuming CPU resources while waiting for conditions to be met.

### AtomicInteger:

```java
class Foo {

  private AtomicInteger firstJobDone = new AtomicInteger(0);
  private AtomicInteger secondJobDone = new AtomicInteger(0);

  public Foo() {}

  public void first(Runnable printFirst) throws InterruptedException {
    // printFirst.run() outputs "first".
    printFirst.run();
    // mark the first job as done, by increasing its count.
    firstJobDone.incrementAndGet();
  }

  public void second(Runnable printSecond) throws InterruptedException {
    while (firstJobDone.get() != 1) {
      // waiting for the first job to be done.
    }
    // printSecond.run() outputs "second".
    printSecond.run();
    // mark the second as done, by increasing its count.
    secondJobDone.incrementAndGet();
  }

  public void third(Runnable printThird) throws InterruptedException {
    while (secondJobDone.get() != 1) {
      // waiting for the second job to be done.
    }
    // printThird.run() outputs "third".
    printThird.run();
  }
}
```

AtomicInteger should be similar as AtomicBoolean, just not as clean for this question.

### synchronized:

```java
class Foo {
    private boolean oneDone;
    private boolean twoDone;
    
    public Foo() {
        oneDone = false;
        twoDone = false;
    }

    public synchronized void first(Runnable printFirst) throws InterruptedException {
        
        // printFirst.run() outputs "first". Do not change or remove this line.
        printFirst.run();
        oneDone = true;
        notifyAll();
    }

    public synchronized void second(Runnable printSecond) throws InterruptedException {
        while(!oneDone) wait();
        // printSecond.run() outputs "second". Do not change or remove this line.
        printSecond.run();
        twoDone = true;
        notifyAll();
    }

    public synchronized void third(Runnable printThird) throws InterruptedException {
        while(!twoDone) wait();
        // printThird.run() outputs "third". Do not change or remove this line.
        printThird.run();
    }
}
```