package com.insta.services;

import com.insta.dto.Comment;
import com.insta.dto.Commentable;
import com.insta.dto.Profile;
import com.insta.interfaces.CommentService;

public class CommentServiceImpl implements CommentService {

    public void createComment(String description, Profile createdBy, Commentable commentable){
        Comment comment= new Comment(description, createdBy);
        commentable.getComments().add(comment);
    }
}
