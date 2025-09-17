package com.HLDLLD.corejava.functionalProgramming;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;

public class SupplierDemo {
    public static void main(String[] args) {
        Supplier<Integer> sup1 = () -> 5;
        Supplier<String> sup = () -> LocalDateTime.now().getDayOfWeek().name();
        System.out.println(sup.get());

    }
}
