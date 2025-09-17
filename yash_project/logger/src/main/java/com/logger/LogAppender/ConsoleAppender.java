package com.logger.LogAppender;

import com.logger.LogMessage;

public class ConsoleAppender implements LogAppender {
    // Appends a log message to the console
    @Override
    public void append(LogMessage logMessage) {
        System.out.println(logMessage); // Print log to console
    }
}
