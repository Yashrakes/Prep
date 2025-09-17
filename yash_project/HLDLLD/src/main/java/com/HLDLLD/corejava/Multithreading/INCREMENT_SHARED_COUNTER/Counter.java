package com.HLDLLD.corejava.Multithreading.INCREMENT_SHARED_COUNTER;

public class Counter {
    public  Integer num =0;
    public synchronized void increment(){
        num++;
    }
    public int getcount(){
        return this.num;
    }
}
