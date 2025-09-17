package com.HLDLLD.creational.Builder;

public class User {
    // Required fields
    private final String firstName;
    private final String lastName;

    // Optional fields
    private final String email;
    private final String phone;
    private final String address;

    // Private constructor (only Builder can create)
    private User(UserBuilder builder) {
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.email = builder.email;
        this.phone = builder.phone;
        this.address = builder.address;
    }

    // Getters
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }

    @Override
    public String toString() {
        return "User: " + firstName + " " + lastName +
                ", Email: " + email + ", Phone: " + phone + ", Address: " + address;
    }

    // Static nested Builder class
    public static class UserBuilder {
        private final String firstName;
        private final String lastName;

        private String email;
        private String phone;
        private String address;

        public UserBuilder(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public UserBuilder address(String address) {
            this.address = address;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}

