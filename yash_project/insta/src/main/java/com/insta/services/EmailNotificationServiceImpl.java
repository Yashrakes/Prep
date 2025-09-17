package com.insta.services;

import com.insta.dto.Notification;
import com.insta.dto.Profile;
import com.insta.interfaces.NotificationService;

public class EmailNotificationServiceImpl implements NotificationService {
    @Override
    public void sendNotification(String description, Profile profile) {
        Notification notification= new Notification(description);
        System.out.println("Hey, in email notification");
    }
}
