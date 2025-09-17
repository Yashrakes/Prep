package com.ATM;

public class UserBankAccount {
    int balance;

    public void withdrawalBalance(int amount) {
        this.balance = this.balance - amount;
    }

}
