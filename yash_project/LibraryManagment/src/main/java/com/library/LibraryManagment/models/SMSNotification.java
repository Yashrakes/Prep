package com.library.LibraryManagment.models;

import com.library.LibraryManagment.interfaces.Notification;
import lombok.Data;

@Data
public class SMSNotification implements Notification {
    @Override
    public void sendNotification(String message, User user) {
        System.out.println("Sending SMS to " + user.getMobile() + ": " + message);
    }
}
