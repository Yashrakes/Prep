package com.HLDLLD.corejava.functionalProgramming;

import java.util.function.Consumer;

public class ConsumerDemo {
    public static void main(String[] args) {
        Consumer<Integer> con = (nums) -> System.out.println(nums);

        con.accept(5);
    }
}
