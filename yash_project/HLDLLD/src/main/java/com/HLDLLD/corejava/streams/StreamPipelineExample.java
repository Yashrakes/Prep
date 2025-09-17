package com.HLDLLD.corejava.streams;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class StreamPipelineExample {
    public static class Student {
        String name;
        double grade;

        Student(String name, double grade) {
            this.name = name;
            this.grade = grade;
        }

        @Override
        public String toString() {
            return name + " - Grade: " + grade;
        }
    }

    public static void main(String[] args) {
        List<Student> students = Arrays.asList(
                new Student("Alice", 85),
                new Student("Bob", 60),
                new Student("Charlie", 75),
                new Student("David", 90)
        );

        // Predicate: Filter students with grade >= 75
        Predicate<Student> passingGrade = student -> student.grade >= 75;

        // Function: Convert student name to uppercase
        Function<Student, Student> toUpperCaseName = student ->
                new Student(student.name.toUpperCase(), student.grade);

        // Consumer: Print student details
        Consumer<Student> printStudent = System.out::println;

        Supplier<Integer> giverandom = ()-> new Random().nextInt(100);
        List<Integer> ran = Stream.generate(giverandom).limit(5).collect(Collectors.toList());

        System.out.println("Students with Passing Grades:");
        students.stream()
                .filter(passingGrade)       // Filter students with grade >= 75
                .map(toUpperCaseName)        // Convert name to uppercase
                .forEach(printStudent)    ;  // Print the result

        int n = 33;
        System.out.println("Number: " + n);

        Function < Integer, String > convertToBinary = num -> Integer.toBinaryString(num);
        String binaryRepresentation = convertToBinary.apply(n);
    }
}

