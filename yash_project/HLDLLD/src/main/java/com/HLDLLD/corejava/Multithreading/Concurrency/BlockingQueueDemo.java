package com.HLDLLD.corejava.Multithreading.Concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
//
//🧠 Key methods (this is important)
//        Method	Behavior
//        put()	Blocks if queue is full
//        take()	Blocks if queue is empty
//        offer()	Returns false if full
//        poll()	Returns null if empty
//        offer(e, timeout)	Waits up to timeout
//        poll(timeout)	Waits up to timeout

//Producer behavior
//
//        If queue is full
//        👉 producer waits
//
//        When space becomes available
//        👉 producer resumes automatically
//
//        Consumer behavior
//
//        If queue is empty
//        👉 consumer waits
//
//        When item is added
//        👉 consumer resumes automatically
//
//        No manual wait() / notify() needed 🙌


//
//🆚 Side-by-side comparison
//        Aspect	ArrayBlockingQueue	LinkedBlockingQueue
//        Data structure	Array	Linked list
//        Capacity	Fixed (mandatory)	Optional (can be unbounded)
//        Memory usage	Lower	Higher (node objects)
//        Locks	Single lock	Two locks (put & take)
//        Throughput	Lower under contention	Higher under contention
//        Fairness option	✅ Yes	❌ No
//        Cache locality	Better	Worse
//        GC pressure	Low	Higher
//        Default choice	❌ Rare	✅ Very common

 class MessageQueue<T> {
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

class MessageProcessor<T> {
    private final MessageQueue<T> queue;
    private final ExecutorService workers;
    private final Consumer<T> processor;
    private volatile boolean running = true;

    public MessageProcessor(MessageQueue<T> queue, int workerCount, Consumer<T> processor) {
        this.queue = queue;
        this.processor = processor;
        this.workers = Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            //workers.execute(this::processMessages);
           workers.submit(this::processMessages);

        }
    }
// if we change return type of this message top any other wrapper class then it became callable to worker.submit otherwise in void it became runnable task
    private void processMessages() {
        while (running) {
            try {
                T message = queue.consume();
                if (message != null) {
                    processor.accept(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        running = false;
        workers.shutdown();
    }
}

// ==== Demo with Producer ====
public class BlockingQueueDemo {

    public static void main(String[] args) throws Exception {

        MessageQueue<String> queue = new MessageQueue<>(5);

        // Consumer logic
        MessageProcessor<String> processor =
                new MessageProcessor<>(queue, 3, (msg) -> {
                    System.out.println(Thread.currentThread().getName() + " processed: " + msg);
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

//
//
//    Feature	execute()	submit()
//        Returns Future	❌	✅
//        Track completion	❌	✅
//        Catch exceptions	❌	✅
//        Cancel task	❌	✅
//        Callable support	❌	✅


//    Consumer thread:
//        ├─ lock takeLock
//        ├─ while queue empty → wait (with timeout)
//        ├─ dequeue element
//        ├─ decrement count
//        ├─ signal other consumers if needed
//        ├─ unlock takeLock
//        ├─ if queue was full → signal producers
//        └─ return element

//
//    Producer thread:
//        ├─ acquire putLock
//        ├─ while queue is FULL
//        │     └─ wait on notFull (with/without timeout)
//        ├─ enqueue element
//        ├─ increment count
//        ├─ signal other producers if space still exists
//        ├─ release putLock
//        ├─ if queue was EMPTY before insert
//        │     └─ signal consumers (notEmpty)

   // ArrayBlockingQueue uses a fixed-size array and a single lock, making it memory-efficient but lower throughput.
// LinkedBlockingQueue uses linked nodes (optionally bounded) and separate locks for inserting/removing, providing higher throughput.


