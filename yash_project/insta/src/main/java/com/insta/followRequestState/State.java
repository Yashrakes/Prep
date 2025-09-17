package com.insta.followRequestState;

import com.insta.dto.FollowRequest;
import com.insta.interfaces.NotificationService;

public abstract class State {
    protected NotificationService notificationService;

    public State(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public abstract  void notifyUser(FollowRequest followRequest);
}
