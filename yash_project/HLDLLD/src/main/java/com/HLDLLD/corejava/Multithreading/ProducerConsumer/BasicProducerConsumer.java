package com.HLDLLD.corejava.Multithreading.ProducerConsumer;

import java.util.LinkedList;
import java.util.Queue;

public class BasicProducerConsumer {
    private final Queue<Integer> buffer = new LinkedList<>();
    private final int capacity = 5;

    public synchronized void produce(int item) throws InterruptedException {
        while(buffer.size() == capacity){
            System.out.println("Buffer full, producer waiting...");
            wait();
        }
        buffer.offer(item);
        System.out.println("Produced: " + item + " (Buffer size: " + buffer.size() + ")");
        notifyAll();
    }

    public synchronized int consume() throws InterruptedException {
        while(buffer.isEmpty()){
            System.out.println("Buffer empty, consumer waiting...");
            wait();
        }

        int item = buffer.poll();
        System.out.println("Consumed: " + item + " (Buffer size: " + buffer.size() + ")");

        notifyAll();
        return item;
    }

    public static void runex(){
        BasicProducerConsumer basicProducerConsumer = new BasicProducerConsumer();

        for(int i =0;i<2;i++){
            final int producerId = i;
            new Thread ( () -> {
                for(int j =0;j<500;j++){
                    int item = producerId * 10 + j;
                    try {
                        basicProducerConsumer.produce(item);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

            }, "Producer-" + i).start();

        }

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < 300; j++) {
                        basicProducerConsumer.consume();
                        Thread.sleep(150); // Simulate consumption time
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Consumer-" + i).start();
        }
    }

    public static void main(String[] args) {
        runex();
    }
}
