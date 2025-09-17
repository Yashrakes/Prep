package com.insta.interfaces;

import com.insta.dto.Post;
import com.insta.dto.Profile;

import java.util.List;

public interface TimelineService {
    List<Post> fetchTimeline(Profile profile);
    void addToTimeline(Post post, List<Profile> profiles);
}
