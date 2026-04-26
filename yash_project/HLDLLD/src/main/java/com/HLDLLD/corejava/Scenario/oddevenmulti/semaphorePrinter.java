package com.HLDLLD.corejava.Scenario.oddevenmulti;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class semaphorePrinter {
    private Semaphore oddsemaphore = new Semaphore(1);
    private Semaphore evensamphore = new Semaphore(0);

    public int number = 1;
    private int max = 20;

    public void printOdd() {
        while (number <= max) {
                try {
                    oddsemaphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(number);
                number ++;
                evensamphore.release();
        }
    }

    public void printeven() {
        while (number <= max) {
                try {
                    evensamphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(number);
                number++;
                oddsemaphore.release();
        }
    }
}

class Semaphoremain {
    public static void main(String[] args) {
        semaphorePrinter semaphorePrinter1 = new semaphorePrinter();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(( ) -> semaphorePrinter1.printeven());
        executorService.execute(() -> semaphorePrinter1.printOdd());
    }
}

// so if we put sycnronized as well on a methods of even  odd then deadlock occurs , because thread 1 acquire a lock using syncronized
// and waits for semaphore to free and another thread wait for syncronized monitor lock to get acquired and acquire can be executed
// so basicallly hold 1 resorce wait for 2nd resource -> thread 1 and holde 2nd resource and wait for 1 -> thread 2;



//public class DeadlockExample {
//
//    private static final Object lock1 = new Object();
//    private static final Object lock2 = new Object();
//
//    public static void main(String[] args) {
//
//        Thread t1 = new Thread(() -> {
//            synchronized (lock1) {
//                System.out.println("Thread 1 acquired lock1");
//
//                try { Thread.sleep(100); } catch (InterruptedException e) {}
//
//                synchronized (lock2) {
//                    System.out.println("Thread 1 acquired lock2");
//                }
//            }
//        });
//
//        Thread t2 = new Thread(() -> {
//            synchronized (lock2) {
//                System.out.println("Thread 2 acquired lock2");
//
//                try { Thread.sleep(100); } catch (InterruptedException e) {}
//
//                synchronized (lock1) {
//                    System.out.println("Thread 2 acquired lock1");
//                }
//            }
//        });
//
//        t1.start();
//        t2.start();
//    }
//}


// cannot use atomic integer here because


//    AtomicInteger num = new AtomicInteger(1);
//
//while (num.get() <= 20) {
//        if (num.get() % 2 == 0) {
//        System.out.println(num.getAndIncrement());
//        }
////        }
//
//    Thread A: sees num = 2
//        Thread B: sees num = 2
//        Thread A: increments → 3
//        Thread B: increments → 4

