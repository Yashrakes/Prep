package com.HLDLLD.SOLID;

public class Example {
//    Absolutely! The SOLID principles are 5 key principles of object-oriented design that help make software easier to understand, maintain, and extend. Let’s break them down one by one — with simple examples in Java.
//
//🔷 S – Single Responsibility Principle (SRP)
//    A class should have only one reason to change.
//
//            ❌ Bad:
//    java
//            Copy
//    Edit
//    public class User {
//        public void saveToDatabase() { /* saving logic */ }
//        public void sendEmail() { /* email logic */ }
//    }
//✅ Good (Separation of concerns):
//    java
//            Copy
//    Edit
//    public class User {
//        // user fields and methods
//    }
//
//    public class UserRepository {
//        public void save(User user) { /* saving logic */ }
//    }
//
//    public class EmailService {
//        public void sendWelcomeEmail(User user) { /* email logic */ }
//    }
//🔷 O – Open/Closed Principle (OCP)
//    Software entities should be open for extension, but closed for modification.
//
//❌ Bad:
//    java
//            Copy
//    Edit
//    public class Payment {
//        public void pay(String type) {
//            if (type.equals("CREDIT")) {
//                // credit card logic
//            } else if (type.equals("PAYPAL")) {
//                // PayPal logic
//            }
//        }
//    }
//✅ Good (Using polymorphism):
//    java
//            Copy
//    Edit
//    public interface PaymentMethod {
//        void pay();
//    }
//
//    public class CreditCardPayment implements PaymentMethod {
//        public void pay() {
//            System.out.println("Paid with Credit Card");
//        }
//    }
//
//    public class PayPalPayment implements PaymentMethod {
//        public void pay() {
//            System.out.println("Paid with PayPal");
//        }
//    }
//
//    public class PaymentProcessor {
//        public void process(PaymentMethod method) {
//            method.pay();
//        }
//    }
//🔷 L – Liskov Substitution Principle (LSP)
//    Objects of a superclass should be replaceable with objects of a subclass without breaking the app.
//
//            ❌ Bad:
//    java
//            Copy
//    Edit
//    public class Bird {
//        public void fly() {}
//    }
//
//    public class Ostrich extends Bird {
//        public void fly() {
//            throw new UnsupportedOperationException("Can't fly");
//        }
//    }
//✅ Good (Use interfaces properly):
//    java
//            Copy
//    Edit
//    public interface Bird {
//        void layEggs();
//    }
//
//    public interface FlyingBird extends Bird {
//        void fly();
//    }
//
//    public class Sparrow implements FlyingBird {
//        public void fly() {}
//        public void layEggs() {}
//    }
//
//    public class Ostrich implements Bird {
//        public void layEggs() {}
//    }
//🔷 I – Interface Segregation Principle (ISP)
//    Clients shouldn't be forced to depend on interfaces they don't use.
//
//            ❌ Bad:
//    java
//            Copy
//    Edit
//    public interface Worker {
//        void work();
//        void eat();
//    }
//
//    public class Robot implements Worker {
//        public void work() {}
//        public void eat() {
//            throw new UnsupportedOperationException();
//        }
//    }
//✅ Good (Split interfaces):
//    java
//            Copy
//    Edit
//    public interface Workable {
//        void work();
//    }
//
//    public interface Eatable {
//        void eat();
//    }
//
//    public class Human implements Workable, Eatable {
//        public void work() {}
//        public void eat() {}
//    }
//
//    public class Robot implements Workable {
//        public void work() {}
//    }
//🔷 D – Dependency Inversion Principle (DIP)
//    Depend on abstractions, not on concrete classes.
//
//            ❌ Bad:
//    java
//            Copy
//    Edit
//    public class LightBulb {
//        public void turnOn() {}
//        public void turnOff() {}
//    }
//
//    public class Switch {
//        private LightBulb bulb = new LightBulb();
//        public void operate() {
//            bulb.turnOn();
//        }
//    }
//✅ Good:
//    java
//            Copy
//    Edit
//    public interface Switchable {
//        void turnOn();
//        void turnOff();
//    }
//
//    public class LightBulb implements Switchable {
//        public void turnOn() { System.out.println("LightBulb On"); }
//        public void turnOff() { System.out.println("LightBulb Off"); }
//    }
//
//    public class Switch {
//        private Switchable device;
//
//        public Switch(Switchable device) {
//            this.device = device;
//        }
//
//        public void operate() {
//            device.turnOn();
//        }
//    }
//🎯 Summary Table:
//
//    Principle	Name	Key Idea
//    S	Single Responsibility	One class = One responsibility
//    O	Open/Closed	Extend, don’t modify existing code
//    L	Liskov Substitution	Subtypes should substitute their parent types
//    I	Interface Segregation	No fat interfaces; split into smaller ones
//    D	Dependency Inversion	High-level modules should not depend on low-level modules
}
