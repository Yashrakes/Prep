package com.HLDLLD.corejava.Multithreading.Concurrency;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CacheWithTTL<K, V> {

    private static class CacheEntry<V> {
        private V value;
        private long expiryTime;

        public CacheEntry(V value, long ttl) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }

    }

    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService scheduledExecutorService;

    public CacheWithTTL() {
        this.cache = new ConcurrentHashMap<>();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(this::cleanup,5,5, TimeUnit.SECONDS);
    }

    public void put(K key, V value,long ttl) {
        reentrantReadWriteLock.readLock().lock();
        try{
            cache.put(key,new CacheEntry<>(value,ttl));
        }
        finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }
//    if we put lock in try block then
//    try is entered
//
//finally must run
//
//    unlock() is called without owning the lock
//
//💥 IllegalMonitorStateException

    public V get(K key) {
        reentrantReadWriteLock.readLock().lock();
        try {
            CacheEntry<V> entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                cache.remove(key);
                return null;
            }
            return entry.value;
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }

    }

    public void cleanup() {
        reentrantReadWriteLock.writeLock().lock();
        try {
            cache.entrySet().removeIf(e -> e.getValue().isExpired());
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }

    public void shutDown() {
        scheduledExecutorService.shutdown();
    }

    public static void main(String[] args) {
        CacheWithTTL<Integer,Integer> cacheWithTTL = new CacheWithTTL<>();
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for(int i =0;i<10;i++){
            executorService.submit(() -> cacheWithTTL.put(1,2,3));
        }
    }

}
