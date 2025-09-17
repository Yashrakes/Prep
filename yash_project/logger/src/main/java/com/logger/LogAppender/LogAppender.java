package com.logger.LogAppender;

import com.logger.LogMessage;

public interface LogAppender {
    void append(LogMessage logMessage);
}
