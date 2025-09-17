package com.HLDLLD.corejava;

import java.util.ArrayList;
import java.util.List;

public class Listt{
    List<String> li = new ArrayList<String>();

//    sort(v.begin() , v.end(), [&](int a , int b ) {
//        int sumA= sums(a);
//        int sumB = sums(b);
//        if(sumA == sumB) return a<b;
//        return sumA<sumB;
//    });
    // Adding elements in List
    public void perfoadd() {

        // Size of ArrayList
        int n = 5;

        // Declaring the List with initial size n
        List<Integer> arrli = new ArrayList<>(n);

        // Appending the new elements
        // at the end of the list
        for (int i = 1; i <= n; i++)
            arrli.add(i);

        // Printing elements
        System.out.println(arrli);



        li.add("Java");
        li.add("Python");
        li.add("DSA");
        li.add("C++");
        // array list class implements-> list interface -> collection
        System.out.println("Elements of List are:");
        // Iterating through the list
        for (String s : li) {
            System.out.println(s);
        }
        li.set(1, "JavaScript");
        System.out.println("Updated List: " + li);

        // Removing elements
        li.remove("C++");
        System.out.println("List After Removing Element: " + li);

        int i = li.indexOf("DSA");
        System.out.println("First Occurrence of DSA is at Index: "+i);

        li.remove(1);
        System.out.println("List After Removing Element: " + li);

        String first = li.get(0);
        String second = li.get(1);
        //String third = li.get(2);

        boolean isPresent = li.contains("Geeks");

    }

    public static void main(String args[]) {
        Listt temp = new Listt();
        temp.perfoadd();
    }
}


