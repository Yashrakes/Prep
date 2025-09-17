
## Single Responsibility Principle (SRP) 

- The SRP states that a class should have only **one reason to change**, meaning it should have only one job or responsibility.

``` java
// Violating SRP
class Employee {
    private String name;
    private double salary;
    
    public void calculateSalary() {
        // Salary calculation logic
    }
    
    public void saveToDatabase() {
        // Database operations
    }
    
    public void generatePayslip() {
        // PDF generation logic
    }
    
    public void sendEmailNotification() {
        // Email sending logic
    }
}
```

- This Employee class is doing too many things: calculating salary, database operations, PDF generation, and email notifications. When any of these aspects need to change (like modifying the email format or updating database operations), we have to modify this class, risking breaking other functionality.
- Here's a better approach following SRP:

``` java
class Employee {
    private String name;
    private double salary;
    
    // Only employee-related data and basic operations
    public double getSalary() {
        return salary;
    }
}

class SalaryCalculator {
    public double calculateSalary(Employee employee) {
        // Salary calculation logic
    }
}

class EmployeeRepository {
    public void save(Employee employee) {
        // Database operations
    }
}

class PayslipGenerator {
    public void generatePayslip(Employee employee) {
        // PDF generation logic
    }
}

class NotificationService {
    public void sendEmail(Employee employee) {
        // Email sending logic
    }
}
```

---

## Open/Closed Principle (OCP) 

- This principle states that software entities should be **open for extension but closed for modification**. In other words, you should be able to add new functionality without changing existing code.

``` java
// Violating OCP
class PaymentProcessor {
    public void processPayment(String paymentType) {
        if (paymentType.equals("CreditCard")) {
            // Process credit card payment
        } else if (paymentType.equals("PayPal")) {
            // Process PayPal payment
        } else if (paymentType.equals("Bitcoin")) {
            // Process Bitcoin payment
        }
    }
}
```

- Every time we need to add a new payment method, we have to modify the existing PaymentProcessor class, risking introducing bugs in the working code.
- Here's the OCP-compliant solution:

``` java
interface PaymentMethod {
    void processPayment(double amount);
}

class CreditCardPayment implements PaymentMethod {
    public void processPayment(double amount) {
        // Process credit card payment
    }
}

class PayPalPayment implements PaymentMethod {
    public void processPayment(double amount) {
        // Process PayPal payment
    }
}

class BitcoinPayment implements PaymentMethod {
    public void processPayment(double amount) {
        // Process Bitcoin payment
    }
}

class PaymentProcessor {
    public void processPayment(PaymentMethod paymentMethod, double amount) {
        paymentMethod.processPayment(amount);
    }
}
```
---

## Liskov Substitution Principle (LSP) 

- The LSP states that **objects of a superclass should be replaceable with objects of its subclasses** without breaking the application. In other words, what works with a base class should work with any of its derived classes.

``` java
// Violating LSP
class Bird {
    public void fly() {
        // Flying implementation
    }
}

class Penguin extends Bird {
    @Override
    public void fly() {
        throw new UnsupportedOperationException("Penguins can't fly!");
    }
}
```

- This violates LSP because you can't use a Penguin wherever a Bird is expected – it will throw an exception when fly() is called.
- Here's a better design:

``` java
interface Bird {
    void move();
}

interface FlyingBird extends Bird {
    void fly();
}

class Sparrow implements FlyingBird {
    public void move() {
        // Basic movement
    }
    
    public void fly() {
        // Flying implementation
    }
}

class Penguin implements Bird {
    public void move() {
        // Walking/swimming implementation
    }
}
```
---

## Interface Segregation Principle (ISP) 

- ISP states that clients should not be forced to depend on interfaces they don't use. In other words, many specific interfaces are better than one general-purpose interface.
- Here's a violation:

``` java
// Violating ISP
interface Worker {
    void work();
    void eat();
    void sleep();
}

class Robot implements Worker {
    public void work() {
        // Working implementation
    }
    
    public void eat() {
        // Robots don't eat!
        throw new UnsupportedOperationException();
    }
    
    public void sleep() {
        // Robots don't sleep!
        throw new UnsupportedOperationException();
    }
}
```

- The Robot class is forced to implement methods it doesn't need. Here's a better approach:
``` java
interface Workable {
    void work();
}

interface Feedable {
    void eat();
}

interface Sleepable {
    void sleep();
}

class Human implements Workable, Feedable, Sleepable {
    public void work() {
        // Working implementation
    }
    
    public void eat() {
        // Eating implementation
    }
    
    public void sleep() {
        // Sleeping implementation
    }
}

class Robot implements Workable {
    public void work() {
        // Working implementation
    }
}
```
---

## Dependency Inversion Principle (DIP) 

- DIP states that high-level modules should not depend on low-level modules; both should depend on abstractions. Abstractions should not depend on details; details should depend on abstractions.
- The principle of dependency inversion refers to the decoupling of software modules. This way, instead of high-level modules depending on low-level modules, both will depend on abstractions.

- To demonstrate this, let’s go old-school and bring to life a Windows 98 computer with code:

```java
public class Windows98Machine {}
```

- But what good is a computer without a monitor and keyboard? Let’s add one of each to our constructor so that every _Windows98Computer_ we instantiate comes prepacked with a _Monitor_ and a _StandardKeyboard_:

```java
public class Windows98Machine {

    private final StandardKeyboard keyboard;
    private final Monitor monitor;

    public Windows98Machine() {
        monitor = new Monitor();
        keyboard = new StandardKeyboard();
    }

}
```

- This code will work, and we’ll be able to use the _StandardKeyboard_ and _Monitor_ freely within our _Windows98Computer_ class.

- Problem solved? Not quite. **By declaring the _StandardKeyboard_ and _Monitor_ with the _new_ keyword, we’ve tightly coupled these three classes together.**

- Not only does this make our _Windows98Computer_ hard to test, but we’ve also lost the ability to switch out our _StandardKeyboard_ class with a different one should the need arise. And we’re stuck with our _Monitor_ class too.

- Let’s decouple our machine from the _StandardKeyboard_ by adding a more general _Keyboard_ interface and using this in our class:

```java
public interface Keyboard { }
```

```java
public class Windows98Machine{

    private final Keyboard keyboard;
    private final Monitor monitor;

    public Windows98Machine(Keyboard keyboard, Monitor monitor) {
        this.keyboard = keyboard;
        this.monitor = monitor;
    }
}
```

- Here, we’re using the dependency injection pattern to facilitate adding the _Keyboard_ dependency into the _Windows98Machine_ class.

- Let’s also modify our _StandardKeyboard_ class to implement the _Keyboard_ interface so that it’s suitable for injecting into the _Windows98Machine_ class:

```java
public class StandardKeyboard implements Keyboard { }
```

- Now our classes are decoupled and communicate through the _Keyboard_ abstraction. If we want, we can easily switch out the type of keyboard in our machine with a different implementation of the interface. We can follow the same principle for the _Monitor_ class.

- Excellent! We’ve decoupled the dependencies and are free to test our _Windows98Machine_ with whichever testing framework we choose.

---
