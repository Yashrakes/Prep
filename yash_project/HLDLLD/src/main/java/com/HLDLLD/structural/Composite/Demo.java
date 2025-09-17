package com.HLDLLD.structural.Composite;

public class Demo {
    public static void main(String[] args) {
        File file1 = new File("File1.txt");
        File file2 = new File("File2.txt");

        Directory directory1 = new Directory("Directory1");
        directory1.addComponent(file1);
        directory1.showDetails();
        System.out.println("\n");

        Directory directory2 = new Directory("Directory2");
        directory2.addComponent(file2);
        directory2.addComponent(directory1);

        directory2.showDetails();
    }
}

