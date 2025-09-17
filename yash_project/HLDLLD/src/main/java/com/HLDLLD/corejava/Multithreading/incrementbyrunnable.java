package com.HLDLLD.corejava.Multithreading;

import java.util.concurrent.atomic.AtomicInteger;

public class incrementbyrunnable implements Runnable{
    private static AtomicInteger count = new AtomicInteger(0);
   /* lock is used
    so we can make a syncronized block without making method increment sttaic
    if we now dont amnke methid increment static then we dont need to make threadname static as well
  */
    private static final Object lock = new Object();

   // private static int num =0;

    // only required in staic method , as static method cant have non staic variaboles
   // private  static String threadName;

    private  String threadName;

    public incrementbyrunnable(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) { // Each thread increments 5 times
            count.getAndIncrement();
            //num++;
            //increment();
            System.out.println("Thread " + threadName + " is running and increased value to "+ count);
        }
    }
/*     if we not make static then its a object level lock so 6 object -> 6 lock
    by making this method static we ensure that its a c lass level lock means 1 lock and accurate answer
*/

//    public synchronized void increment() {
//
////        synchronized (lock) {
////            num++;
////            System.out.println("Thread " + threadName + " is running and increased value to "+ num);
////        }
//        num++;
//        System.out.println("Thread " + threadName + " is running and increased value to "+ num);
//    }

    public static void main(String[] args) {
        incrementbyrunnable[] runnableObjects = new incrementbyrunnable[6];
        Thread[]  threads = new Thread[6];
        for(int i =0;i<6;i++){
            runnableObjects[i] = new incrementbyrunnable("Thread-" +i);
            threads[i] = new Thread(runnableObjects[i]);
        }
        for (int i = 0; i < 6; i++) {
            threads[i].start();
        }

        try {
            for (int i = 0; i < 6; i++) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("final count " + count);
        System.out.println("All threads finished");
    }
}

