package com.HLDLLD.corejava.Multithreading.Concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

public class TaskScheduler {
    private static class ScheduledTask implements Comparable<ScheduledTask> {
        Runnable task;
        long executeAt;
        long interval; // 0 for one-time tasks

        ScheduledTask(Runnable task, long executeAt, long interval) {
            this.task = task;
            this.executeAt = executeAt;
            this.interval = interval;
        }

        @Override
        public int compareTo(ScheduledTask other) {
            return Long.compare(this.executeAt, other.executeAt);
        }
    }

    private final PriorityBlockingQueue<ScheduledTask> taskQueue;
    private final ExecutorService executor;
    private final Thread schedulerThread;
    private volatile boolean running = true;

    public TaskScheduler(int workerThreads) {
        this.taskQueue = new PriorityBlockingQueue<>();
        this.executor = Executors.newFixedThreadPool(workerThreads);
        this.schedulerThread = new Thread(this::run);
        this.schedulerThread.start();
    }

    public void scheduleOnce(Runnable task, long delayMillis) {
        long executeAt = System.currentTimeMillis() + delayMillis;
        taskQueue.offer(new ScheduledTask(task, executeAt, 0));
    }

    public void scheduleRecurring(Runnable task, long initialDelayMillis, long intervalMillis) {
        long executeAt = System.currentTimeMillis() + initialDelayMillis;
        taskQueue.offer(new ScheduledTask(task, executeAt, intervalMillis));
    }

    private void run() {
        while (running) {
            try {
                ScheduledTask task = taskQueue.peek();

                if (task == null) {
                    Thread.sleep(100);
                    continue;
                }

                long now = System.currentTimeMillis();
                if (task.executeAt <= now) {
                    taskQueue.poll();
                    executor.submit(task.task);

                    // Reschedule if recurring
                    if (task.interval > 0) {
                        task.executeAt = now + task.interval;
                        taskQueue.offer(task);
                    }
                } else {
                    Thread.sleep(Math.min(100, task.executeAt - now));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        running = false;
        schedulerThread.interrupt();
        executor.shutdown();
    }
}
