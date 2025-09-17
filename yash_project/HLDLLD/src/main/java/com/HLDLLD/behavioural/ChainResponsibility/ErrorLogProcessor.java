package com.HLDLLD.behavioural.ChainResponsibility;

public class ErrorLogProcessor extends LogProcessor{

    ErrorLogProcessor(LogProcessor nexLogProcessor){
        super(nexLogProcessor);
    }

    public void log(int logLevel,String message){

        if(logLevel == ERROR) {
            System.out.println("ERROR: " + message);
        } else{
            System.out.println("trying rubbish");
            //super.log(logLevel, message);
        }

    }
}

