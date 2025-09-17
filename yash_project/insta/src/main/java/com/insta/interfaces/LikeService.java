package com.insta.interfaces;

import com.insta.dto.Likeable;
import com.insta.dto.Profile;

public interface LikeService {
    void like(Likeable likeable, Profile createdBy);
    void unlike(Likeable likeable, Profile createdBy);

}
