package com.HLDLLD.rate_limiters;

import java.time.Instant;

public class FixedWindowCounter {
    private final long windowSizeInSeconds;  // Size of each window in seconds
    private final long maxRequestsPerWindow; // Maximum number of requests allowed per window
    private long currentWindowStart;         // Start time of the current window
    private long requestCount;               // Number of requests in the current window

    public FixedWindowCounter(long windowSizeInSeconds, long maxRequestsPerWindow) {
        this.windowSizeInSeconds = windowSizeInSeconds;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.currentWindowStart = Instant.now().getEpochSecond();
        this.requestCount = 0;
    }

    public synchronized boolean allowRequest() {
        long now = Instant.now().getEpochSecond();

        // Check if we've moved to a new window
        if (now - currentWindowStart >= windowSizeInSeconds) {
            currentWindowStart = now;  // Start a new window
            requestCount = 0;          // Reset the count for the new window
        }

        if (requestCount < maxRequestsPerWindow) {
            requestCount++;  // Increment the count for this window
            return true;     // Allow the request
        }
        return false;  // We've exceeded the limit for this window, deny the request
    }
}

//    How It Works
//        Divides time into fixed intervals (windows) (e.g., 1-minute window).
//
//        Counts requests within each window – If count exceeds the limit, requests are denied.
//
//        At the end of each window, the counter resets.
//
//        Key Characteristics
//        ✅ Simple to implement and memory-efficient.
//        ✅ Works well for predictable traffic patterns.
//        ❌ Window Reset Problem – If a burst of requests comes at the end of one window and the start of the next, it may allow more requests than expected.
//
//        Use Cases
//        🔹 Login Attempts – Allowing 5 login attempts per minute.
//        🔹 Basic API Throttling – Limiting 100 requests per hour per user.


