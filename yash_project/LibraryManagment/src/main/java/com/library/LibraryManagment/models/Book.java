package com.library.LibraryManagment.models;

import lombok.Data;

@Data
public class Book {
    private String title;
    private String author;
    private String subject;
    private int publishedYear;

    public Book(){

    }

    public Book(String title, String author, String subject, int publishedYear) {
        this.title = title;
        this.author = author;
        this.subject = subject;
        this.publishedYear = publishedYear;
    }

}
