package com.library.LibraryManagment.models;

import lombok.Data;

@Data
public class Author {
    private String authorId;
    private String name;

    public Author(String authorId, String name) {
        this.authorId = authorId;
        this.name = name;
    }

  /*
    Author should not extend User because:

        It has a different purpose (writing books, not borrowing/managing them).
        It would inherit unnecessary behaviors (searching, borrowing, etc.).
        It would violate Liskov Substitution Principle (LSP).
        Composition is a better approach for associating authors with books.
    */

    public void writeBook(String bookTitle) {
        System.out.println("Author " + name + " wrote the book: " + bookTitle);
    }
}
