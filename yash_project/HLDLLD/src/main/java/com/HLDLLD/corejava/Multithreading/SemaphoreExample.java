package com.HLDLLD.corejava.Multithreading;

import java.util.concurrent.Semaphore;

class SharedResource {
    private final Semaphore semaphore;

    public SharedResource(int availableResources) {
        this.semaphore = new Semaphore(availableResources); // Initialize semaphore with a certain number of resources
    }

    // Method that each thread will use to access a shared resource
    public void accessResource(String threadName) {
        try {
            System.out.println(threadName + " is trying to acquire a resource...");
            semaphore.acquire(); // Acquire a permit (a resource)
            System.out.println(threadName + " has acquired a resource.");

            // Simulate doing some work with the shared resource
            Thread.sleep(10000); // Simulate time spent using the resource

            System.out.println(threadName + " is releasing the resource.");
            semaphore.release(); // Release the permit (resource)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Worker1 implements Runnable {
    private final SharedResource sharedResource;
    private final String threadName;

    public Worker1(SharedResource sharedResource, String threadName) {
        this.sharedResource = sharedResource;
        this.threadName = threadName;
    }

    @Override
    public void run() {
        sharedResource.accessResource(threadName);
    }
}

public class SemaphoreExample {
    public static void main(String[] args) {
        SharedResource sharedResource = new SharedResource(3); // Only 3 resources available

        // Create and start multiple worker threads
        Thread thread1 = new Thread(new Worker1(sharedResource, "Thread-1"));
        Thread thread2 = new Thread(new Worker1(sharedResource, "Thread-2"));
        Thread thread3 = new Thread(new Worker1(sharedResource, "Thread-3"));
        Thread thread4 = new Thread(new Worker1(sharedResource, "Thread-4"));
        Thread thread5 = new Thread(new Worker1(sharedResource, "Thread-5"));

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread5.start();
    }
}

//Sure! A Semaphore in Java is a synchronization mechanism that controls access to a shared resource by multiple threads. It maintains a set number of permits, and a thread can acquire a permit before accessing the resource and release the permit when done. If no permits are available, the thread will wait until one becomes available.

//    acquire(): Acquires a permit (resource). If no permit is available, the thread will block until one is released.
//
//        release(): Releases a permit (resource), allowing other waiting threads to acquire it.
//
//        availablePermits(): Returns the number of available permits.
//
//        tryAcquire(): Attempts to acquire a permit without blocking.
//
//        When to Use Semaphore:
//        Limit the number of threads that can access a certain resource at the same time.
//
//        Useful when you want to control concurrent access to a finite resource (e.g., database connections, file handles, or limited hardware resources).