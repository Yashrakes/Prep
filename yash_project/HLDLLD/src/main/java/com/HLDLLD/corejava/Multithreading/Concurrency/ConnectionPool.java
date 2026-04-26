package com.HLDLLD.corejava.Multithreading.Concurrency;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class ConnectionPool<T> {
    private final BlockingQueue<T> available;
    private final Set<T> inUse;
    private final int maxSize;
    private final Supplier<T> connectionFactory;
    private final ReentrantLock lock = new ReentrantLock();
    private AtomicInteger currentSize = new AtomicInteger(0);

    public ConnectionPool(int maxSize, Supplier<T> connectionFactory) {
        this.maxSize = maxSize;
        this.connectionFactory = connectionFactory;
        this.available = new LinkedBlockingQueue<>();
        this.inUse = new HashSet<>();
    }

    public T acquire() throws InterruptedException {
        lock.lock();
        try {
            // Try to get from available pool
            T connection = available.poll();
            if (connection != null) {
                inUse.add(connection);
                return connection;
            }

            // Create new if under limit
            if (currentSize.getAndIncrement() < maxSize && connection == null) {
                connection = connectionFactory.get();
                inUse.add(connection);
                return connection;
            }
        } finally {
            lock.unlock();
        }

        // Wait for available connection
        T connection = available.take();  // Never hold a lock while performing a potentially blocking operation. so lock is after take
        lock.lock();
        try {
            inUse.add(connection); // se
            return connection;
        } finally {
            lock.unlock();
        }
    }

    public void release(T connection) {
        lock.lock();
        try {
            if(inUse.remove(connection)) {
                available.offer(connection); // need to amke sure if connection was used , otherwisse it will release something that is never be used
            }
        } finally {
            lock.unlock();
        }
    }

    public int getAvailableCount() {
        return available.size();
    }

    public int getInUseCount() {
        lock.lock();
        try {
            return inUse.size();
        } finally {
            lock.unlock();
        }
    }

   // We lock around inUse.size() because HashSet is not thread-safe.
    // Even read-only operations must be synchronized to avoid data races and visibility issues.
    // The lock establishes a happens-before relationship with modifications done during acquire and release.
}






//
//public class SemaphoreConnectionPool<T> {
//
//    private final Semaphore permits;
//    private final BlockingQueue<T> available;
//    private final Supplier<T> factory;
//
//    public SemaphoreConnectionPool(int maxSize, Supplier<T> factory) {
//        this.permits = new Semaphore(maxSize, true); // fair semaphore
//        this.available = new LinkedBlockingQueue<>();
//        this.factory = factory;
//    }
//
//    public T acquire() throws InterruptedException {
//        // 1️⃣ Acquire permit (blocks if pool exhausted)
//        permits.acquire();
//
//        // 2️⃣ Try to reuse existing connection
//        T connection = available.poll();
//        if (connection != null) {
//            return connection;
//        }
//
//        // 3️⃣ Otherwise create new connection
//        return factory.get();
//    }
//
//    public void release(T connection) {
//        if (connection == null) return;
//
//        // 4️⃣ Return connection to pool
//        available.offer(connection);
//
//        // 5️⃣ Release permit
//        permits.release();
//    }
//
//    public int availableConnections() {
//        return available.size();
//    }
//
//    public int inUseConnections() {
//        return permits.getQueueLength();
//    if i want to know how much semaphoere been used
//      int available = semaphore.availablePermits();
//     if i want to know how much semaphoere is free to  use
// max semaphore size = 6 , currnet pooool created = 5, curenet semaphore use =2,
// so from this data free semaphore = 4, currnet pool available are 5-2 = 3

//    }
//}



//public class SemaphorePoolDemo {
//
//    public static void main(String[] args) throws Exception {
//
//        SemaphoreConnectionPool<String> pool =
//                new SemaphoreConnectionPool<>(3, () -> {
//                    String conn = "CONN-" + System.nanoTime();
//                    System.out.println("Created: " + conn);
//                    return conn;
//                });
//
//        ExecutorService executor = Executors.newFixedThreadPool(6);
//
//        for (int i = 0; i < 6; i++) {
//            int id = i;
//            executor.submit(() -> {
//                try {
//                    System.out.println("Thread-" + id + " acquiring...");
//                    String conn = pool.acquire();
//                    System.out.println("Thread-" + id + " got " + conn);
//
//                    Thread.sleep(2000); // simulate work
//
//                    pool.release(conn);
//                    System.out.println("Thread-" + id + " released " + conn);
//
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            });
//        }
//
//        executor.shutdown();
//        executor.awaitTermination(10, TimeUnit.SECONDS);
//
//        System.out.println("Demo finished");
//    }
//}
//
