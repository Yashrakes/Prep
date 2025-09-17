package com.library.LibraryManagment.models;

import lombok.Data;

@Data
public abstract class User {
    private String id;
    private String name;
    private String mobile;
    private String gender;
    private String email;
    /*
    User :

    Common behavior for Member and Librarian like :
        -> Can search for books...
        -> Interacts with the library system...
    */
    public User(){

    }
    public User(String id, String name, String mobile, String gender, String email) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.gender = gender;
        this.email = email;
    }
}
