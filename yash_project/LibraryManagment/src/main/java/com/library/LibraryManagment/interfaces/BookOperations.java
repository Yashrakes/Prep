package com.library.LibraryManagment.interfaces;

public interface BookOperations {
    void checkoutBook(String barcode);
    void returnBook(String bookId);
    void renewBook(String bookId);
    void reserveBook(String bookId);
}
