package com.HLDLLD.corejava.Multithreading;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

public class BoundedBlockingQueue {
    private Semaphore full;
    private Semaphore empty ;
    private ConcurrentLinkedDeque<Integer> deque;

    public BoundedBlockingQueue(int capcity){
        full = new Semaphore(0);
        empty = new Semaphore(capcity);
        deque = new ConcurrentLinkedDeque<>();
    }

    public void enqueue(int element) throws InterruptedException {
       empty.acquire();
       deque.addFirst(element);
       full.release();
    }

    public int dequeue() throws InterruptedException {
        int result =-1;
        full.acquire();
        result = deque.pollLast();
        empty.release();
        return result;
    }

    public int size() throws InterruptedException {
        int result = 0;
        // Retrieve and return the size of the deque
        result = deque.size();
        return result;
    }


}
