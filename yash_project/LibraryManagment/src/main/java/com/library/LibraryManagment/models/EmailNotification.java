package com.library.LibraryManagment.models;

import com.library.LibraryManagment.interfaces.Notification;
import lombok.Data;

@Data
public class EmailNotification implements Notification {
    @Override
    public void sendNotification(String message, User user) {
        System.out.println("Sending Email to " + user.getEmail() + ": " + message);
    }
}
