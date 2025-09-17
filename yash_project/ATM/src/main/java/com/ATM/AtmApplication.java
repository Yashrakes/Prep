package com.ATM;

import com.ATM.ATMSTATE.IdleState;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@SpringBootApplication
public class AtmApplication {

    User user;

    public static void main(String[] args) {
        //SpringApplication.run(AtmApplication.class, args);
        AtmApplication atmRoom = new AtmApplication();
        ATM atm = ATM.getATMObject();
        atmRoom.initialize();

        atm.printCurrentATMStatus();
        atm.getCurrentATMState().insertCard(atm, atmRoom.user.getCard());
        atm.getCurrentATMState().authenticatePin(atm, atmRoom.user.getCard(), 112211);
        atm.getCurrentATMState().selectOperation(atm, atmRoom.user.getCard(), TransactionType.CASH_WITHDRAWAL);
        atm.getCurrentATMState().cashWithdrawal(atm, atmRoom.user.getCard(), 2700);
        atm.printCurrentATMStatus();
    }

    private void initialize() {
        ATM.getATMObject().setAtmBalance(3500, 1, 2, 5);
        ATM.getATMObject().setCurrentATMState(new IdleState());
        //create User
        this.user = createUser();
    }

    private User createUser() {

        User user = new User();
        user.setCard(createCard());
        return user;
    }

    private Card createCard() {

        Card card = new Card();
        card.setCardNumber(1786);
        card.setCvv(007);
        card.setHolderName("yash");
        card.setExpiryDate(11);
        card.setBankAccount(createBankAccount());
        return card;
    }

    private UserBankAccount createBankAccount() {

        UserBankAccount bankAccount = new UserBankAccount();
        bankAccount.balance = 3000;

        return bankAccount;

    }

}
