package com.library.LibraryManagment.services;

import com.library.LibraryManagment.interfaces.Transaction;
import com.library.LibraryManagment.models.Fine;
import com.library.LibraryManagment.models.Member;

import java.util.List;

public class PaymentService {

    public static void processFinePayment(Member member, Transaction transaction) {
        List<Fine> unpaidFines = member.getUnpaidFines();
        if (unpaidFines.isEmpty()) {
            System.out.println("No unpaid fines.");
            return;
        }

        double totalAmount = unpaidFines.stream().mapToDouble(Fine::getAmount).sum();
        transaction.processPayment(totalAmount);
        unpaidFines.forEach(Fine::payFine);
    }
}
