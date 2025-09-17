package com.library.LibraryManagment.models;

import lombok.Data;

import java.time.LocalDate;
import java.util.Optional;

@Data
public class Fine {
    private double amount;
    private boolean isPaid;
    private LocalDate dueDate;
    private LocalDate paymentDate;

    public Fine(double amount, LocalDate dueDate) {
        this.amount = amount;
        this.dueDate = dueDate;
        this.isPaid = false;
    }

    public void payFine() {
        if(Optional.ofNullable(isPaid).orElse(false)){
            System.out.println("Fine is already paid.");
            return;
        }
        this.isPaid = true;
        this.paymentDate = LocalDate.now();
        System.out.println("Fine of $" + amount + " paid successfully.");
    }
}
