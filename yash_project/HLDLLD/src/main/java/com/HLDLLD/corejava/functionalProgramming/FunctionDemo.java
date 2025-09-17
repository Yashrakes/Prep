package com.HLDLLD.corejava.functionalProgramming;

import java.util.Locale;
import java.util.function.Function;

public class FunctionDemo {
    public static void main(String[] args) {
        Function<String, String> convstr = (str) -> str.toUpperCase();

        System.out.println(convstr.apply("test"));

        Function<Integer, Integer> addthree = (num) -> num+3;
        Function<Integer, Integer> Doub = (num) -> num*2;

        Function<Integer, Integer> output1 = addthree.andThen(Doub);
        Function<Integer, Integer> output2 = addthree.compose(Doub);

        System.out.println(output1.apply(5)); //16
        System.out.println(output2.apply(5)); //13



    }
}
