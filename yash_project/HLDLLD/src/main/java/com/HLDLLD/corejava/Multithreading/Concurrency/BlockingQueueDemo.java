package com.HLDLLD.corejava.Multithreading.Concurrency;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
//
//🧠 Key methods (this is important)
//        Method	Behavior
//        put()	Blocks if queue is full
//        take()	Blocks if queue is empty
//        offer()	Returns false if full
//        poll()	Returns null if empty
//        offer(e, timeout)	Waits up to timeout
//        poll(timeout)	Waits up to timeout

public class MessageQueue<T> {
    private final BlockingQueue<T> queue;
    private final int capacity;
    private final AtomicInteger messageCount = new AtomicInteger(0);

    public MessageQueue(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    public boolean publish(T message) throws InterruptedException {
        boolean added = queue.offer(message, 1, TimeUnit.SECONDS);
        if (added) {
            messageCount.incrementAndGet();
        }
        return added;
    }

    public T consume() throws InterruptedException {
        T message = queue.poll(1, TimeUnit.SECONDS);
        if (message != null) {
            messageCount.decrementAndGet();
        }
        return message;
    }

    public int size() {
        return messageCount.get();
    }
}



// ==== Demo with Producer ====
public class BlockingQueueDemo {

    public static void main(String[] args) throws Exception {

        MessageQueue<String> queue = new MessageQueue<>(5);

        // Consumer logic
        MessageProcessor<String> processor =
                new MessageProcessor<>(queue, 3, msg -> {
                    System.out.println(
                            Thread.currentThread().getName() + " processed: " + msg
                    );
                    try {
                        Thread.sleep(500); // simulate work
                    } catch (InterruptedException ignored) {}
                });

        // Producer (single producer thread)
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 15; i++) {
                try {
                    boolean published = queue.publish("Message-" + i);
                    if (published) {
                        System.out.println("Produced: Message-" + i +
                                " | Queue size: " + queue.size());
                    } else {
                        System.out.println("Failed to publish Message-" + i);
                    }
                    Thread.sleep(200); // simulate production speed
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        producer.start();

        // Let the system run
        Thread.sleep(8000);

        processor.shutdown();
        System.out.println("System shutdown.");
    }
}
