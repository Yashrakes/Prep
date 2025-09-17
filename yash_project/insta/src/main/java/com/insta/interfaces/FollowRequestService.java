package com.insta.interfaces;

import com.insta.dto.FollowRequest;
import com.insta.dto.Profile;

import java.util.List;

public interface FollowRequestService {
    void createFollowRequest(Profile sender, Profile receiver) throws Exception;
    List<FollowRequest> fetchAllRequest(Profile profile);
    void acceptFollowRequest(FollowRequest followRequest);
    void rejectFollowRequest(FollowRequest followRequest);

}
