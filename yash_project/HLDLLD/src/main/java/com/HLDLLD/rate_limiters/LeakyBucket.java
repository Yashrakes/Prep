package com.HLDLLD.rate_limiters;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;

public class LeakyBucket {
    private final long capacity;        // Maximum number of requests the bucket can hold
    private final double leakRate;      // Rate at which requests leak out of the bucket (requests per second)
    private final Queue<Instant> bucket; // Queue to hold timestamps of requests
    private Instant lastLeakTimestamp;   // Last time we leaked from the bucket

    public LeakyBucket(long capacity, double leakRate) {
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.bucket = new LinkedList<>();
        this.lastLeakTimestamp = Instant.now();
    }

    public synchronized boolean allowRequest() {
        leak();
        if(bucket.size()<capacity){
            //bucket.add(Instant.now());
            bucket.offer(Instant.now());
            return true;
        }
        return false;
    }
    private void leak() {
        Instant now = Instant.now();
        long elapsedMillis = now.toEpochMilli() - lastLeakTimestamp.toEpochMilli();
        int leakedItems = (int) (elapsedMillis * leakRate / 1000.0);  // Calculate how many items should have leaked

        // Remove the leaked items from the bucket
        for (int i = 0; i < leakedItems && !bucket.isEmpty(); i++) {
            bucket.poll();
        }
        lastLeakTimestamp = now;
    }



}



//    How It Works
//        Maintains a "bucket" that leaks at a fixed rate (one request processed at a time).
//
//        New requests add to the bucket if space is available.
//
//        If the bucket is full, excess requests are discarded or queued.
//
//        Ensures a constant outflow rate, unlike the Token Bucket.
//
//        Key Characteristics
//        ✅ Smooth request handling – Prevents sudden spikes in traffic.
//        ✅ Ensures fair request processing – Requests are processed at a constant rate.
//        ❌ Not suitable for handling bursts – Extra requests will be discarded.
//
//        Use Cases
//        🔹 Network Traffic Shaping – TCP/IP congestion control to prevent packet flooding.
//        🔹 Streaming Services – Controlling the flow of video or audio streams.
