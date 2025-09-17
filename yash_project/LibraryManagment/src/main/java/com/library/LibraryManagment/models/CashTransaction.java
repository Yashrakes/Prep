package com.library.LibraryManagment.models;

import com.library.LibraryManagment.interfaces.Transaction;
import lombok.Data;

@Data
public class CashTransaction implements Transaction {

    @Override
    public void processPayment(double amount) {
        System.out.println("Paid $" + amount + " using Cash. No receipt generated.");
    }
}
