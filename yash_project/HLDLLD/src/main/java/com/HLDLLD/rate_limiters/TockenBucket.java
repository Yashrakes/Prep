package com.HLDLLD.rate_limiters;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

public class TockenBucket {
    private final long capacity;        // Maximum number of tokens the bucket can hold
    private final double fillRate;      // Rate at which tokens are added to the bucket (tokens per second)
    private double tokens;              // Current number of tokens in the bucket
    private Instant lastRefillTimestamp; // Last time we refilled the bucket

    private final ReentrantLock lock = new ReentrantLock();

    public TockenBucket(long capacity, double fillRate){
        this.capacity = capacity;
        this.fillRate = fillRate;
        this.tokens = capacity;
        this.lastRefillTimestamp = Instant.now();
    }

    public synchronized boolean allowRequest(int tokens){
        lock.lock();
        try {
            refill();
            if (this.tokens < tokens) {
                return false;
            }
            this.tokens = this.tokens - tokens;
            return true;
        }
        finally {
            lock.unlock();
        }

    }

    private void refill(){
        Instant now = Instant.now();

        double tockenToAdd = (now.toEpochMilli() - lastRefillTimestamp.toEpochMilli())*fillRate/1000.0;
        if(tockenToAdd>0) {
            this.tokens = Math.min(capacity, tokens + tockenToAdd);
            this.lastRefillTimestamp = now;
        }
    }
}

//    How It Works
//        Maintains a "bucket" that holds tokens (each token represents a request).
//
//        Tokens replenish at a fixed rate (e.g., 2 tokens per second).
//
//        A request is allowed if a token is available (removes one token per request).
//
//        If the bucket is empty, the request is denied.
//
//        Key Characteristics
//        ✅ Supports bursty traffic – If tokens accumulate, sudden bursts can be handled.
//        ✅ Efficient and memory-friendly – O(1) complexity for checking and updating tokens.
//        ❌ Not ideal for strictly enforcing rate limits – If tokens accumulate, users can send requests in bursts.
//
//        Use Cases
//        🔹 API Rate Limiting – Cloud services (AWS, Google APIs) allow short bursts while maintaining an average request rate.
//        🔹 Distributed Systems – Controlling microservices interaction frequency.
