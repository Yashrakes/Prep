package com.insta.interfaces;

import com.insta.dto.Profile;

public interface PostService {
    void createPost(Profile profile, String description);
}
