package com.HLDLLD.corejava.Multithreading;

public class array {
    public void chan(Integer t){
        t=11;
    }
    public static void main(String[] args) {
        Integer a = 10;
        array ab = new array();
        ab.chan(a);
        System.out.println(a);

    }
}
