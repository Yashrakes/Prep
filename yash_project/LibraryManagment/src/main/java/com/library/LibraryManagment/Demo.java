package com.library.LibraryManagment;

import com.library.LibraryManagment.enums.PaymentMethod;
import com.library.LibraryManagment.models.*;

import java.util.List;

public class Demo {
   // public static void main(String args []) {
        List<BookItem> bookItems;
        List<Shelf> shelves;
        Shelf shelf1 = new Shelf("S001", "Programming", bookItems);
        Shelf shelf2 = new Shelf("S002", "Fiction", bookItems);
        Library library = new Library(bookItems, shelves);

        // Create a librarian
        Librarian librarian = new Librarian("L001", "Alice Smith", "1234567890", "Male", "alice@library.com", library);

        // Add shelves
//    librarian.addShelf(shelf1);
//    librarian.addShelf(shelf2);

        // Issue Book to Member
        public void issueBook (Member member, String barcode){
            BookItem bookItem = library.searchByBarcode(barcode);
            if (bookItem != null && bookItem.isAvailable()) {
                member.checkoutBook(bookItem.getBarcode());
                System.out.println("Book issued to " + member.getName());
            } else {
                System.out.println("Book issue failed. Book not available.");
            }
        }
        // Return Book
        public void returnBook (Member member, String barcode){
            BookItem bookItem = library.searchByBarcode(barcode);
            if (bookItem != null) {
                member.returnBook(bookItem.getBarcode());
                System.out.println("Book returned by " + member.getName());
            } else {
                System.out.println("Return failed. Book not found.");
            }
        }

        // Pay Fine
        public void payFine (Member member, PaymentMethod method){
//        PaymentService.processFinePayment(member, method);
        }

        // Display Library
        public void displayLibrary () {
            library.displayLibraryLayout();
        }
  //  }

}


