package com.HLDLLD.rate_limiters;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;

public class SlidingWindowLoger {
    private final long windowSizeInSeconds;   // Size of the sliding window in seconds
    private final long maxRequestsPerWindow;  // Maximum number of requests allowed in the window
    private Queue<Long> requestLog;     // Log of request timestamps
    public SlidingWindowLoger(){
        windowSizeInSeconds = 10;
        maxRequestsPerWindow = 5;
        this.requestLog = new LinkedList<>();
    }

    public synchronized boolean allowRequest() {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSizeInSeconds;

        // Remove timestamps that are outside of the current window
        while (!requestLog.isEmpty() && requestLog.peek() <= windowStart) {
            requestLog.poll();
        }

        if (requestLog.size() < maxRequestsPerWindow) {
            requestLog.offer(now);  // Log this request
            return true;            // Allow the request
        }
        return false;  // We've exceeded the limit for this window, deny the request
    }
}
