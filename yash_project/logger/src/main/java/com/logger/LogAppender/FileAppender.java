package com.logger.LogAppender;

import com.logger.LogMessage;

import java.io.FileWriter;
import java.io.IOException;

// Appends log messages to a file
public class FileAppender implements LogAppender {
    private final String filePath; // Path to the log file

    // Constructor to set the file path
    public FileAppender(String filePath) {
        this.filePath = filePath;
    }

    // Appends a log message to the file
    @Override
    public void append(LogMessage logMessage) {
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(logMessage.toString() + "n"); // Write log to file
        } catch (IOException e) {
            e.printStackTrace(); // Print error if file writing fails
        }
    }
}
