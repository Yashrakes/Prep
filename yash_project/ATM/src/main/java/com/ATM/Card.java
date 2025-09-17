package com.ATM;

import lombok.Data;

@Data
public class Card {
    private int cardNumber;
    private int cvv;
    private int expiryDate;
    private String holderName;
    static int PIN_NUMBER = 112211;
    private UserBankAccount bankAccount;



    public Boolean isCorrectPINEntered(int Pin){
        return Pin == PIN_NUMBER ;
    }
    public int getBankBalance(){
        return bankAccount.balance;

    }
    public void deductBankBalance(int amount){
        bankAccount.withdrawalBalance(amount);
    }
    public void setBankAccount(UserBankAccount bankAccount){
        this.bankAccount  = bankAccount;

    }
}
