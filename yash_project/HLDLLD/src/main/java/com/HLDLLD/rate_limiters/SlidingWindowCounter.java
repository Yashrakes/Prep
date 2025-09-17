package com.HLDLLD.rate_limiters;

import java.time.Instant;

public class SlidingWindowCounter {
    private final int windowSizeInSeconds;   // Size of the sliding window in seconds
    private final int maxRequestsPerWindow;  // Maximum number of requests allowed in the window
    private int currentWindowStart;          // Start time of the current window
    private int previousWindowCount;         // Number of requests in the previous window
    private int currentWindowCount;          // Number of requests in the current window

    public SlidingWindowCounter(int windowSizeInSeconds, int maxRequestsPerWindow) {
        this.windowSizeInSeconds = windowSizeInSeconds;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.currentWindowStart = 0; //Instant.now().getEpochSecond();
        this.previousWindowCount = 0;
        this.currentWindowCount = 0;
    }
    public void updateWindows(int currTime){
        int timeElapsed = currTime - currentWindowStart;
        int windowsPassed = timeElapsed/windowSizeInSeconds;

        if(windowsPassed==1){
            previousWindowCount =currentWindowCount;
            currentWindowCount =0;
        }
        else if(windowsPassed>=2){
            previousWindowCount =0;
            currentWindowCount =0;
        }
        currentWindowStart = currTime - (timeElapsed % windowSizeInSeconds);
    }
    public int calcRequest(int currTime){
        int timeElapsed = currTime - currentWindowStart;
        int elapseTimeCurrWindow = timeElapsed % windowSizeInSeconds;
        int remainigTimeInPreviousWindow = windowSizeInSeconds - elapseTimeCurrWindow;

        double p1 = elapseTimeCurrWindow * 1.0/windowSizeInSeconds;
        double p2 = remainigTimeInPreviousWindow * 1.0/windowSizeInSeconds;

        return (int) Math.ceil(currentWindowCount*p1 + previousWindowCount*p2);
    }
    public synchronized boolean allowRequest(int currTime) {
        updateWindows(currTime);
        int reqCount = calcRequest(currTime);

        if(reqCount<maxRequestsPerWindow){
            currentWindowCount++;
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        SlidingWindowCounter slidingWindowCounter = new SlidingWindowCounter(10,5);
        System.out.println(slidingWindowCounter.allowRequest(1) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(3) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(4) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(7) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(8) +"\n");
        Thread.sleep(1000);

        System.out.println("/n");
        System.out.println(slidingWindowCounter.allowRequest(12) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(12) +"\n");
        Thread.sleep(1000);

        System.out.println("/n");
        System.out.println(slidingWindowCounter.allowRequest(35) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(35) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(35) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(35) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(35) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(35) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(35) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(35) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(35) +"\n");
        Thread.sleep(1000);
        System.out.println(slidingWindowCounter.allowRequest(35) +"\n");
        Thread.sleep(1000);


    }
}

//    How It Works
//        Similar to Fixed Window Counter, but instead of fixed windows, it uses overlapping small time slices to smooth out request distribution.
//
//        Example: If the window is 1 minute, we may split it into 6 slices of 10 seconds and calculate the request rate dynamically.
//
//        Key Characteristics
//        ✅ More accurate than Fixed Window, but less memory-intensive than Sliding Log.
//        ✅ Balances accuracy and performance.
//        ❌ More complex to implement.
//
//        Use Cases
//        🔹 Payment Systems – Limiting transactions (e.g., max 10 transactions per 5 minutes, but distributed smoothly).
//        🔹 Cloud APIs – Balancing load between clients.
//
//
