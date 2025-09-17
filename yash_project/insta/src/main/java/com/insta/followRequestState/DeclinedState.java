package com.insta.followRequestState;

import com.insta.dto.FollowRequest;
import com.insta.interfaces.NotificationService;
import com.insta.services.EmailNotificationServiceImpl;

public class DeclinedState extends State{


    public DeclinedState() {
        super(new EmailNotificationServiceImpl());
    }

    @Override
    public void notifyUser(FollowRequest followRequest) {
        notificationService.sendNotification("Your follow request is not accepted, Please stay away!", followRequest.getSender());
    }
}
