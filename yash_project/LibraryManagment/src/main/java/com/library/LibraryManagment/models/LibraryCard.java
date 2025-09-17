package com.library.LibraryManagment.models;

import com.library.LibraryManagment.enums.AccountStatus;
import lombok.Data;

@Data
public class LibraryCard {
    private String cardNumber;
    private AccountStatus status;

    public LibraryCard(String cardNumber, AccountStatus status) {
        this.cardNumber = cardNumber;
        this.status = status;
    }
}
