package com.HLDLLD.corejava.Method_Refrence;

import java.util.List;

public class StaticMethodRefrence {
    public static void main(String[] args) {
        Arthop arthop = (a,b) -> {
            int sum = a+b;
            System.out.println("sum" + sum);
            return sum;
        };

        // static method refrence
        Arthop arthop1 = StaticMethodRefrence :: perform;
        arthop1.performoperation(5,3);

        // instnce method refrence
        StaticMethodRefrence staticMethodRefrence = new StaticMethodRefrence();
        Arthop arthop2 = staticMethodRefrence ::perform1;
        arthop2.performoperation(5,3);

        //instance method using class
        var departmentList = List.of("Supply", "HR", "Sales", "Marketing");
        departmentList.forEach(department-> System.out.println(department));
        departmentList.forEach(System.out::println);

        //constructor refrence

//    @FunctionalInterface
//    public interface ProductInterface {
//        Product getProduct(String name, int price);
//    }


//        public class Product {
//            String name;
//            int price;
//
//            public Product(String name, int price) {
//                this.name name;
//                this.price price;
//            }
//        }

//        ProductInterface productInterface = Product::new;
//        Product product = productInterface.getProduct("Apple IPhone", 1500);
//        System.out.println(product);

    }
    public static int perform(int a, int b){
        int sum = a+b;
        System.out.println("sum" + sum);
        return sum;
    }
    public int perform1(int a, int b){
        int sum = a+b;
        System.out.println("sum" + sum);
        return sum;
    }
}
