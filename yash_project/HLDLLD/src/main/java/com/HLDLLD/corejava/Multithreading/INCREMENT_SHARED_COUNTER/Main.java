package com.HLDLLD.corejava.Multithreading.INCREMENT_SHARED_COUNTER;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        IncrementThread [] incrementThreads = new IncrementThread[6];
        Counter counter = new Counter();
        for(Integer i =0;i<6;i++){
            incrementThreads[i] = new IncrementThread(10000, counter);
            incrementThreads[i].start();
        }
        try {
            for (IncrementThread thread: incrementThreads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Print the final count
        System.out.println("Final count: " + counter.getcount());

    }
}
