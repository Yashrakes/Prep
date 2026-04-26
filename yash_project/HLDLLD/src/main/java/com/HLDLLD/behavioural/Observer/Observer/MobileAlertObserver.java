package com.HLDLLD.behavioural.Observer.Observer;

import com.HLDLLD.behavioural.Observer.Observable.StockObservable;


public class MobileAlertObserver implements NotAlertObserver{

    String userName;

    public MobileAlertObserver(String userName){
        this.userName = userName;
    }

    private void sendmsg(String userName, String msg){
        System.out.println("sending username  " + userName +  " this message" + msg);
    }
    @Override
    public void update() {
        sendmsg(userName," new stock coming");
    }
}
