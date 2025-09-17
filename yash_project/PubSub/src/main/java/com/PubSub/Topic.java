package com.PubSub;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Topic {
    private final String name;
    private final Set<Subscriber> subscribers = new CopyOnWriteArraySet<>();

    private final ExecutorService executorService;

    public Topic(String name) {
        this.name = name;
        this.executorService = Executors.newCachedThreadPool();
    }

    public String getName() {
        return name;
    }

    public void addSubscriber(Subscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void removeSubscriber(Subscriber subscriber) {
        subscribers.remove(subscriber);
    }
    public void publish(Message message){
        subscribers.stream().forEach(x->x.onMessage(message));

//        for (Subscriber subscriber : subscribers) {
//            executorService.submit(() -> subscriber.onMessage(message));
//        }
    }
//    public void shutdown() {
//        executorService.shutdown();
//    }
}
