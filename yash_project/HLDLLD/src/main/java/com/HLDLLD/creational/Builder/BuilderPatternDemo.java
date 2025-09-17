package com.HLDLLD.creational.Builder;

public class BuilderPatternDemo {
    public static void main(String[] args) {
        User user1 = new User.UserBuilder("John", "Doe")
                .email("john.doe@example.com")
                .phone("123-456-7890")
                .build();

        User user2 = new User.UserBuilder("Jane", "Smith")
                .address("123 Main St")
                .build();

        System.out.println(user1);
        System.out.println(user2);
    }
}

