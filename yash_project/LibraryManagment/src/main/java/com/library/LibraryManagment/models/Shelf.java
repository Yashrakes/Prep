package com.library.LibraryManagment.models;

import lombok.Data;

import java.util.List;
@Data
public class Shelf {
    private String shelfId;
    private String category;
    private List<BookItem> bookItems;

    public Shelf(String shelfId, String category, List<BookItem> bookItems) {
        this.shelfId = shelfId;
        this.category = category;
        this.bookItems = bookItems;
    }
    public String getShelfId() { return shelfId; }
    public String getCategory() { return category; }
    public List<BookItem> getBookItems() { return bookItems; }

    public void addBookItem(BookItem bookItem) {
        bookItems.add(bookItem);
    }

    public void removeBookItem(BookItem bookItem) {
        bookItems.remove(bookItem);
    }

}
