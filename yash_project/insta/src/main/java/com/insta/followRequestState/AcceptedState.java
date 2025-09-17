package com.insta.followRequestState;

import com.insta.dto.FollowRequest;
import com.insta.interfaces.NotificationService;
import com.insta.services.SmsNotificationServiceImpl;

public class AcceptedState extends State{
//    public AcceptedState(NotificationService notificationService) {
//        super(notificationService);
//    }
    public AcceptedState() {
        super(new SmsNotificationServiceImpl());
    }

    @Override
    public void notifyUser(FollowRequest followRequest) {
        notificationService.sendNotification("Your follow request is accepted", followRequest.getSender());
    }
}
