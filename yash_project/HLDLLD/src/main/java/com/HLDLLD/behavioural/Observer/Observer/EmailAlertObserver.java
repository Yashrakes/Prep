package com.HLDLLD.behavioural.Observer.Observer;


import com.HLDLLD.behavioural.Observer.Observable.StockObservable;

public class EmailAlertObserver implements NotAlertObserver{
    String emailId;
    StockObservable observable;

    public EmailAlertObserver(String emailId, StockObservable stockObservable){
        this.emailId = emailId;
        this.observable = stockObservable;
    }
    @Override
    public void update() {
            sendmail(emailId, "new product coming");
    }

    private void sendmail(String emailId, String msg) {
        System.out.println("mail send to " + emailId + " with message " + msg);
    }
}
