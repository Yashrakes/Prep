package com.HLDLLD.corejava.functionalProgramming;

import java.util.function.Predicate;

public class PredicateDemo {
    public static void main(String[] args) {
        Predicate<Integer> isEven = (num) -> num%2==0;
        System.out.println(isEven.test(5)); // false

        Predicate<Integer> isGreater = (num) -> num >=10;

        System.out.println(isEven.and(isGreater).test(14)); // true

        Predicate<String> checkEquality = Predicate.isEqual("iseasy");
        checkEquality.test("by"); // false

    }
}


