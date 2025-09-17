package com.HLDLLD.corejava.Multithreading;

import java.util.concurrent.atomic.AtomicInteger;

public class incrementbythread extends Thread {

    private static AtomicInteger count = new AtomicInteger(0);

    // private static int num =0;

    private String threadName;

    public incrementbythread(String threadName) {
        this.threadName = threadName;
    }



    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) { // Each thread increments 5 times
            count.getAndIncrement();
            //num++;
            System.out.println("Thread " + threadName + " is running and increased value to "+ count);
        }
    }

    public static void main(String[] args) {
        incrementbythread[] runnableObjects = new incrementbythread[6];
        for(int i =0;i<6;i++){
            runnableObjects[i] = new incrementbythread("Thread-" +i);

        }
        for (int i = 0; i < 6; i++) {
            runnableObjects[i].start();
        }

        try {
            for (int i = 0; i < 6; i++) {
                runnableObjects[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("final count " + count);
        System.out.println("All threads finished");
    }
}
