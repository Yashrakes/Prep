package com.library.LibraryManagment.interfaces;

import com.library.LibraryManagment.models.User;

public interface Notification {
    void sendNotification(String message, User user);
}
