package com.HLDLLD.corejava.Multithreading.callable;

import java.util.concurrent.Callable;

public class MyCallable implements Callable<String> {

    private final String name;
    private Integer num;

    public MyCallable(String name ,Integer a){
        this.name = name;
        this.num = a;
    }

    // Implement the call method from Callable interface
    @Override
    public String call() throws Exception{
        StringBuilder result = new StringBuilder();
        for(int i=0;i<5;i++){
            result.append("Callable ").append(name)
                    .append(" is running: ").append(i).append("\n");
            Thread.sleep(500);
            //return String.valueOf(result);
        }
        return result.toString();
    }
}
