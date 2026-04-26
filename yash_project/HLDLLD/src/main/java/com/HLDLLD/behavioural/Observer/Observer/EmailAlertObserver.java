package com.HLDLLD.behavioural.Observer.Observer;


import com.HLDLLD.behavioural.Observer.Observable.StockObservable;

public class EmailAlertObserver implements NotAlertObserver{
    String emailId;

    public EmailAlertObserver(String emailId){
        this.emailId = emailId;
    }
    @Override
    public void update() {
            sendmail(emailId, "new product coming");
    }

    private void sendmail(String emailId, String msg) {
        System.out.println("mail send to " + emailId + " with message " + msg);
    }
}
