package com.HLDLLD.corejava.streams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StreamDemo {
    public static void main(String[] args) {

        //1. Find the  all even numbers in a list of integers.

        List<Integer> list1 = Arrays.asList(1,2,3,4);
        System.out.println("before " + list1);
        list1 = list1.stream()
             .filter((x) ->x%2==0)
             .collect(Collectors.toList());
        System.out.println("after" + list1);


        //2. Find the sum of all even numbers in a list of integers.
        List<Integer> list2 = Arrays.asList(1,2,3,4);
        System.out.println("before " + list2);
        int sum  = list2.stream()
                .filter((x) ->x%2==0)
                .mapToInt(Integer:: intValue)
                .sum();

        System.out.println("after" + sum);

       // 2. Find and print the count of strings that have length greater than 5.

        List<String> list3 = Arrays.asList("apple" , "abcdef" , "abs");
        System.out.println("before " + list3);
        int count = (int) list3.stream().filter(x->x.length()>5).count();
        list3.stream().filter(x->x.length()>5).forEach(System.out :: println);
        System.out.println("after" + count);

        // Implement a function that takes a list of integers as input and returns a new list containing the square of each element.

        List<Integer> list4 = Arrays.asList(1,2,3,4);
        System.out.println("before " + list4);
         list4 = list4.stream().map(x-> x *x).collect(Collectors.toList());
        System.out.println("after" + list4);

        //4. Find the maximum element in a list of integers.
        List<Integer> list5 = Arrays.asList(1,2,3,4);
        System.out.println("before " + list5);
        Optional<Integer> n = list4.stream().max(Comparator.naturalOrder());
        Optional<Integer> n1 = list4.stream().max((x, y) -> x.compareTo(y));
        if(n.isPresent()){
            System.out.println(n.get());
        }
        int nu = list4.stream().mapToInt(Integer::intValue).max().getAsInt();
        System.out.println("after" + nu);

        //5. Concatenate all the strings in a list into a single string.

        List<String> list6 = Arrays.asList("apple" , "abcdef" , "abs");
        System.out.println("before " + list6);
        String concat = list6.stream().collect(Collectors.joining());
        System.out.println("after" + concat);

        //second largest second smallest

        List < Integer > nums1 = Arrays.asList(1, 17, 54, 14, 14, 33, 45, -11);
        System.out.println("List of numbers: " + nums1);
        // Find the second smallest element
        Integer secondSmallest = nums1.stream()
                .distinct()
                .sorted()
                .skip(1)
                .findFirst()
                .orElse(null);

        // Find the second largest element
        Integer secondLargest = nums1.stream()
                .distinct()
                .sorted(Comparator.reverseOrder())
                .skip(1)
                .findFirst()
                .orElse(null);

        System.out.println("\nSecond smallest element: " + secondSmallest);
        System.out.println("\nSecond largest element: " + secondLargest);
    }
}
