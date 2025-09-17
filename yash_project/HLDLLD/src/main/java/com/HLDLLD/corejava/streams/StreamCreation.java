package com.HLDLLD.corejava.streams;


import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamCreation {
    public static void main(String[] args) {
        List<String>  list  = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");

// INTERMIDIATE PROCESS
        // creating stream from list
        Stream<String> depstream = list.stream();
        depstream.forEach(System.out::println);

        Stream<String> dept = Stream.of("a","b");
        dept.forEach(System.out::println);

        Stream<String> depstream1 = list.parallelStream();
        depstream1.forEach(System.out::println);

// create stream from array
        String[] arrayword ={"a","b"};
        Stream<String> streamofword = Arrays.stream(arrayword);

        // infinite stream
        Stream.generate(new Random() :: nextInt).forEach(System.out:: println);

        // stream iterator
        Stream.iterate(1, n ->n+1).forEach(System.out:: println);

// usage of map
        List<String> departmentList = new ArrayList<>();
        departmentList.add("Supply"); departmentList.add("HR");
        departmentList.add("Sales");
        departmentList.add("Marketing");
        departmentList.stream() // Stream creation
                .map(word -> word.toUpperCase()) // Intermediate operation
                .forEach(word->System.out.println(word));

// flatmap
        // extracting each letter from a string array
        // 1st way
        //        flatMap takes a stream of streams and flattens them into a single stream.
        //
        //                So Stream<Stream<String>> becomes Stream<String> containing all the letters.
        //
        //                Resulting stream:
        //["t", "e", "s", "t", "b", "e", "s", "t"]

        String[] strarr = {"test" , "best"};
        Stream<String> stringStream = Arrays.stream(strarr);

        Stream<String[]> streamofletter = stringStream.map(a-> a.split(""));
        streamofletter.flatMap(Arrays::stream).forEach(System.out::println);

        // 2nd way
        Arrays.stream(strarr).flatMap(a->Arrays.stream(a.split(""))).forEach(System.out::println);

// filter

        List<String> temp = Arrays.stream(strarr).filter(a->a.equals("test")).collect(Collectors.toList());
        Arrays.stream(strarr).filter(a->a.equals("test")).forEach(System.out::println);

// limit
        Stream.generate(new Random() :: nextInt).limit(5).forEach(System.out::println);

//skip
        Stream.iterate(1, n -> n+1).skip(5).limit(2).map(x-> x*2).forEach(System.out::println);


// List declaration
        List<Integer> L1 = Arrays.asList(1,2,3,4,5);
        List<Integer> L2 = List.of(1,2,3,4,5);
        List<Integer> L3 = new ArrayList<>();

        L3.add(1);   // works
        L3.set(0, 2); // works

        L1.set(0, 10); // ✅ works
        L2.set(0, 10); // ❌ throws UnsupportedOperationException

        L1.add(3);     // ❌ throws UnsupportedOperationException (fixed size)
        L2.add(3);     // ❌ throws UnsupportedOperationException (immutable)

// TERMINAL OPERATION

//sum/ reduce
        int sum = L2.stream().reduce(0,Integer::sum);

//count
        long count = L2.stream().count();
//min same as max , min and max return optional int , and corrosponding value aacan be obtained by getasint
        int mini =L2.stream().mapToInt(n->n).min().getAsInt();

    }
}
