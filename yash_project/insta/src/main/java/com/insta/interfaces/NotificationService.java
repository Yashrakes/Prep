package com.insta.interfaces;

import com.insta.dto.Profile;

public interface NotificationService {
    void sendNotification(String description, Profile profile);
}
