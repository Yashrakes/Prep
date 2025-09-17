package com.HLDLLD.behavioural.Observer.Observable;


import com.HLDLLD.behavioural.Observer.Observer.NotAlertObserver;

import java.util.ArrayList;
import java.util.List;

public class IphoneObservable implements StockObservable {


    public List<NotAlertObserver> observerList = new ArrayList<>();
    public int stockCount =0;

    @Override
    public void addd(NotAlertObserver notAlertObserver) {
        observerList.add(notAlertObserver);
    }

    @Override
    public void remove(NotAlertObserver notAlertObserver) {
        observerList.remove(notAlertObserver);
    }

    @Override
    public void notifySubscriber() {
        for(NotAlertObserver observer: observerList){
            observer.update();
        }
    }

    @Override
    public void setStock(int newStock) {
        if(stockCount == 0){
            notifySubscriber();
        }
        stockCount =stockCount +newStock;
    }

    @Override
    public int getStock() {
        return stockCount;
    }
}
