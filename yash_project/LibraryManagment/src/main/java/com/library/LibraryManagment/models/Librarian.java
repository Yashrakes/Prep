package com.library.LibraryManagment.models;

import com.library.LibraryManagment.enums.AccountStatus;
import com.library.LibraryManagment.interfaces.BookOperations;
import com.library.LibraryManagment.interfaces.BookSearch;
import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
public class Librarian extends User implements BookOperations, BookSearch {

    private Library library;

    public Librarian(String id, String name, String mobile, String gender, String email, Library library) {
        super(id, name, mobile, gender, email);
        this.library = library;
    }


    @Override
    public void checkoutBook(String bookId) {
        System.out.println("Librarian checking out book on behalf of member: " + bookId);
    }

    @Override
    public void returnBook(String bookId) {
        System.out.println("Librarian processing return for book: " + bookId);
    }

    @Override
    public void renewBook(String bookId) {
        System.out.println("Librarian renewing book on behalf of member: " + bookId);
    }

    @Override
    public void reserveBook(String bookId) {
        System.out.println("Librarian reserving book for member: " + bookId);
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

    public void activateMemberCard(Member member){
        if(Objects.nonNull(member)){
            member.getCard().setStatus(AccountStatus.ACTIVE);
            System.out.println("Library card activated for " + member.getName());
        }
    }

    public void deactivateMemberCard(Member member) {
        if (member.getCard() != null) {
            member.getCard().setStatus(AccountStatus.INACTIVE);
            System.out.println("Library card deactivated for " + member.getName());
        }
    }

    // Add Shelf
    public void addShelf(Shelf shelf1) {
        library.addShelf(shelf1);
    }
}
