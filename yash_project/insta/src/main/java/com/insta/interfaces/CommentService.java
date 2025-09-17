package com.insta.interfaces;


import com.insta.dto.Commentable;
import com.insta.dto.Profile;

public interface CommentService {
    void createComment(String description, Profile createdBy, Commentable commentable);
}
