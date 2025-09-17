package com.insta.followRequestState;


import com.insta.dto.FollowRequest;
import com.insta.interfaces.NotificationService;
import com.insta.services.EmailNotificationServiceImpl;

public class HoldState extends State{

    public static HoldState holdState= new HoldState();

//    public HoldState(NotificationService notificationService) {
//        super(notificationService);
//    }
    private HoldState(){
        super(new EmailNotificationServiceImpl());
    }


    @Override
    public void notifyUser(FollowRequest followRequest) {
        notificationService.sendNotification("You have got a friend request: ", followRequest.getReceiver());
    }
}
