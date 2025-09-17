package com.HLDLLD.corejava.functionalProgramming;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

public class BiFunctionDemo {
    public static void main(String[] args) {
        BiPredicate<Integer, Integer> pdemo = (nums1, nums2) -> (nums1 + nums2) % 2 ==0;
        System.out.println(pdemo.test(5,4));

        BiFunction<Integer, Integer, Integer> fdemo = (nums1, nums2)-> nums1+nums2;
        System.out.println( fdemo.apply(5,4));

        BiConsumer<Integer, Integer>  bicodemo = (nums1, nusm2) -> System.out.println("test" + nums1 + "test2" + nusm2);
        bicodemo.accept(5,4);

        BinaryOperator<Integer> binarydemo = (nums1, nums2)-> nums1+nums2;
        System.out.println( binarydemo.apply(5,4));
    }
}
