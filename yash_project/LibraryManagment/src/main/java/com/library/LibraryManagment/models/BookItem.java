package com.library.LibraryManagment.models;

import lombok.Data;

import java.time.LocalDate;
@Data
public class BookItem extends Book{
    private String barcode;
    private boolean isAvailable;
    private LocalDate dueDate;
    private Shelf shelf; // New association

    public BookItem(String title, String author, String subject, int publishedYear, String barcode, Shelf shelf) {
        super(title, author, subject, publishedYear);
        this.barcode = barcode;
        this.isAvailable = true;
        this.shelf = shelf;
        this.dueDate=null;
    }

}
