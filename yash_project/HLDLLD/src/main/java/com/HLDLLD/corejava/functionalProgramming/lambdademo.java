package com.HLDLLD.corejava.functionalProgramming;

public class lambdademo {
// need to make obnject of that interface by object o = defination of lambda ;

    public static void main(String[] args) {

// approich 1
        Hello h = () -> System.out.println("hello");
        helloprocess(h);

// arppoch 2
        helloprocess(() -> System.out.println("hello2"));

//anonymous inner hello class
        Hello h1 = new Hello() {
            @Override
            public void sayhello() {

            }
        };
    }
    public static void helloprocess(Hello h){
        h.sayhello();
    }


}
