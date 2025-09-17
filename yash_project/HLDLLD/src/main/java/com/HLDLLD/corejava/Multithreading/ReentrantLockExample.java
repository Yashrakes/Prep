package com.HLDLLD.corejava.Multithreading;

import java.util.concurrent.locks.ReentrantLock;

class SharedCounter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();  // ReentrantLock

    public void increment() {
        lock.lock();  // Acquire the lock
        try {
            count++;
            System.out.println(Thread.currentThread().getName() + " incremented count to " + count);
        } finally {
            lock.unlock();  // Always release the lock in finally
        }
    }

    public int getCount() {
        return count;
    }
}

class Worker implements Runnable {
    private final SharedCounter counter;

    public Worker(SharedCounter counter) {
        this.counter = counter;
    }

    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            counter.increment();
        }
    }
}

public class ReentrantLockExample {
    public static void main(String[] args) {
        SharedCounter counter = new SharedCounter();

        Thread thread1 = new Thread(new Worker(counter), "Thread-1");
        Thread thread2 = new Thread(new Worker(counter), "Thread-2");
        Thread thread3 = new Thread(new Worker(counter), "Thread-3");

        thread1.start();
        thread2.start();
        thread3.start();

        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Final count is: " + counter.getCount());
    }
}


//    ReentrantLock allows explicit control over when to acquire and release the lock.
//
//        The lock() and unlock() methods give more flexibility than the synchronized keyword.
//
//        Non-blocking TryLock:
//
//        With ReentrantLock, you can attempt to acquire the lock without blocking indefinitely (via tryLock()), which is not possible with synchronized.
//
//        Interruptible Locking:
//
//        You can acquire a lock in a way that allows for interruption if needed (via lockInterruptibly()), while synchronized is not interruptible.
//
//        Fairness:
//
//        You can configure ReentrantLock to be fair, meaning threads acquire the lock in the order they requested it. This is not possible with synchronized.
//
