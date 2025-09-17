package com.logger.LogHandlers;

import com.logger.LogAppender.LogAppender;

// Logger for DEBUG level messages
public class DebugLogger extends LogHandler {
    public DebugLogger(int level, LogAppender appender) {
        super(level, appender);
    }

    @Override
    protected void write(String message) {
        System.out.println("DEBUG: " + message);
    }
}
