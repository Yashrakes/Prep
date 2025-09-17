package com.HLDLLD.behavioural.Observer;

import com.HLDLLD.behavioural.Observer.Observable.IphoneObservable;
import com.HLDLLD.behavioural.Observer.Observable.StockObservable;
import com.HLDLLD.behavioural.Observer.Observer.EmailAlertObserver;
import com.HLDLLD.behavioural.Observer.Observer.MobileAlertObserver;
import com.HLDLLD.behavioural.Observer.Observer.NotAlertObserver;

import java.util.ArrayList;
public class main {

    public static void main(String args[]) {
        StockObservable iphoneObservable = new IphoneObservable();

        NotAlertObserver user1 = new MobileAlertObserver("a", iphoneObservable);
        NotAlertObserver user2 = new EmailAlertObserver("b", iphoneObservable);
        NotAlertObserver user3 = new MobileAlertObserver("c", iphoneObservable);

        iphoneObservable.addd(user1);
        iphoneObservable.addd(user2);
        iphoneObservable.addd(user3);

        iphoneObservable.setStock(10);
    }
}
