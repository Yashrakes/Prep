package com.ATM;

public class User {
    private UserBankAccount userBankAccount;
    private Card card;
    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }
}
