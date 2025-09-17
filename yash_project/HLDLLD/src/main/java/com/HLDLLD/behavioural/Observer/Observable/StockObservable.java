package com.HLDLLD.behavioural.Observer.Observable;

import com.HLDLLD.behavioural.Observer.Observer.NotAlertObserver;

public interface StockObservable {
    public void addd(NotAlertObserver notAlertObserver);
    public void remove(NotAlertObserver notAlertObserver);
    public void notifySubscriber();
    public void setStock(int newStock);
    public int getStock();
}
