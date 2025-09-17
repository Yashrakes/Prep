package com.library.LibraryManagment.models;

import com.library.LibraryManagment.interfaces.Transaction;
import lombok.Data;

@Data
public class CreditCardTransaction implements Transaction {
    private String cardNumber;

    public CreditCardTransaction(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    @Override
    public void processPayment(double amount) {
        System.out.println("Paid $" + amount + " using Credit Card (Card No: " + cardNumber + ").");
    }
}
