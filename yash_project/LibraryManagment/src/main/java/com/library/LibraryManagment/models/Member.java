package com.library.LibraryManagment.models;

import com.library.LibraryManagment.enums.AccountStatus;
import com.library.LibraryManagment.interfaces.BookOperations;
import com.library.LibraryManagment.interfaces.BookSearch;
import com.library.LibraryManagment.interfaces.Notification;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class Member extends User implements BookOperations, BookSearch {
    private List<BookItem> borrowedBooks;
    private Library library;
    private LibraryCard card;
    private List<Fine> fines;
    private Notification notificationService;
    private static final int MAX_BORROW_LIMIT = 10;

    public Member(String id, String name, String mobile, String gender, String email, Library library, LibraryCard card, Notification notificationService) {
        super(id, name, mobile, gender, email);
        this.borrowedBooks = new ArrayList<>();
        this.library=library;
        this.card = card;
        this.fines = new ArrayList<>();
        this.notificationService = notificationService;
    }

    @Override
    public void checkoutBook(String barcode) {
        if (card == null || !(card.getStatus()==AccountStatus.ACTIVE)) {
            System.out.println("Cannot borrow books! Library card is inactive.");
            return;
        }

        if (hasUnpaidFines()) {
            System.out.println("Cannot borrow books! Unpaid fines exist.");
            return;
        }
        BookItem bookItem = library.searchByBarcode(barcode);
        if (bookItem == null || !bookItem.isAvailable()) {
            System.out.println("Book is not available!");
            return;
        }
        bookItem.setAvailable(false);
        bookItem.setDueDate(LocalDate.now().plusDays(14)); // Due in 2 weeks
        borrowedBooks.add(bookItem);
        notificationService.sendNotification("Book checked out: " + bookItem.getTitle() +
                ". Due Date: " + bookItem.getDueDate(), this);

    }

    @Override
    public void returnBook(String bookId) {
        if(borrowedBooks.size()>0){
            BookItem bookItem = library.searchByBarcode(bookId);
            borrowedBooks.remove(bookItem);
            System.out.println("Member returning book: " + bookId);
            notificationService.sendNotification("Book returned : " + bookItem.getTitle() +
                    "Successfully!!!", this);

        }
        else{
            System.out.println("No reserve book present...");
        }
    }

    @Override
    public void renewBook(String bookId) {
        System.out.println("Renewing book with ID: " + bookId);
    }

    @Override
    public void reserveBook(String bookId) {
        System.out.println("Reserving/Borrowing book with ID: " + bookId);
        BookItem item = library.searchByBarcode(bookId);
        if(Objects.nonNull(item)) {
            borrowedBooks.add(item);
        }
    }

    @Override
    public List<Book> searchByTitle(String title) {
        return library.searchByTitle(title);
    }

    @Override
    public List<Book> searchByAuthor(String author) {
        return library.searchByAuthor(author);
    }

    @Override
    public List<Book> searchBySubject(String subject) {
        return library.searchBySubject(subject);
    }

    @Override
    public List<Book> searchByPublishedDate(int year) {
        return library.searchByPublishedDate(year);
    }

    public void deactivateCard() {
        if (card != null) {
            card.setStatus(AccountStatus.INACTIVE);
            System.out.println("Library card has been deactivated.");
        }
    }

    public LibraryCard getCard() {
        return card;
    }

    public void addFine(Fine fine) {
        fines.add(fine);
    }

    public List<Fine> getUnpaidFines() {
        return fines.stream().filter(fine -> !fine.isPaid()).toList();
    }

    public boolean hasUnpaidFines() {
        return getUnpaidFines().size() > 0;
    }
}
