package com.HLDLLD.corejava.Multithreading.INCREMENT_SHARED_COUNTER;

public class IncrementThread extends Thread{
    private Counter counter;
    public Integer incrementsPerThread;

    public IncrementThread(Integer num, Counter counter){
        this.incrementsPerThread = num;
        this.counter = counter;
    }

    @Override
    public void run(){
        for(int i =0;i<incrementsPerThread;i++){
            counter.increment();
        }
    }
}
