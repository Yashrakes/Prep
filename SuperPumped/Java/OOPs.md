# Data Abstraction

### What is Data Abstraction?

Data abstraction is the process of hiding the complex implementation details of a system and exposing only the essential features or behaviors to the user. In object-oriented programming, this principle allows you to focus on what an object does rather than how it does it. By using data abstraction, you can simplify interactions with objects and reduce complexity, which makes your code easier to understand and maintain.

### Real-World Analogy

Imagine driving a car. When you get into a car, you don't need to understand the inner workings of the engine, the transmission, or the intricate electronics. You only interact with the steering wheel, pedals, and gear shifter. The car's complex internal processes are hidden from you, allowing you to focus solely on driving. This is the essence of data abstraction—providing a simple interface while concealing the underlying complexity.

### Code
In this example:
- The `Shape` abstract class defines a contract (the `draw()` method) without detailing the specifics.
- The `Circle` and `Rectangle` classes implement the `draw()` method to provide their specific behaviors.
- This approach hides the details of how each shape is drawn from the user, who only needs to know that each shape has a `draw()` method.
``` java
// Abstract class representing a shape
abstract class Shape {
    // Abstract method to be implemented by subclasses
    abstract void draw();
}

// Concrete implementation of Shape for a Circle
class Circle extends Shape {
    @Override
    void draw() {
        System.out.println("Drawing a circle");
    }
}

// Concrete implementation of Shape for a Rectangle
class Rectangle extends Shape {
    @Override
    void draw() {
        System.out.println("Drawing a rectangle");
    }
}

public class AbstractionDemo {
    public static void main(String[] args) {
        // Using data abstraction to work with shapes
        Shape circle = new Circle();
        circle.draw();  // Output: Drawing a circle

        Shape rectangle = new Rectangle();
        rectangle.draw();  // Output: Drawing a rectangle
    }
}

```

---

# Data Encapsulation / Data Hiding

### What is Data Encapsulation?

Data encapsulation is the concept of bundling the data (attributes) and the methods (functions) that operate on that data into a single unit, typically a class. It restricts direct access to some of an object's components, which helps prevent accidental interference and misuse of the data. This is usually achieved by declaring the data members as `private` and exposing them via public getter and setter methods.

### Real-World Analogy

Think of a bank account as a sealed safe deposit box. The box (object) contains valuable items (data) and has specific controls (methods) such as deposit and withdrawal. Only authorized operations can be performed, and you can’t directly access the contents of the box without going through the proper channels. This ensures that the contents remain secure and that any interaction with the box happens in a controlled manner.

### Java Code Implementation

In this example:
- The `balance` field is declared as `private`, so it cannot be accessed directly from outside the class.
- The methods `deposit()`, `withdraw()`, and `getBalance()` provide controlled access to modify and view the balance.
- This ensures that the balance can only be updated or retrieved in a safe and predictable manner.
``` java
// The Account class encapsulates the balance field, restricting direct access.
public class Account {
    // Private data member - direct access is hidden.
    private double balance;

    // Constructor to initialize the account with a starting balance.
    public Account(double balance) {
        this.balance = balance;
    }

    // Public method to deposit money into the account.
    public void deposit(double amount) {
        if(amount > 0) {
            balance += amount;
        }
    }

    // Public method to withdraw money from the account.
    public void withdraw(double amount) {
        if(amount > 0 && amount <= balance) {
            balance -= amount;
        }
    }

    // Public method to get the current balance.
    public double getBalance() {
        return balance;
    }
}

public class BankDemo {
    public static void main(String[] args) {
        Account myAccount = new Account(1000.0);
        myAccount.deposit(500.0);
        myAccount.withdraw(200.0);
        System.out.println("Current Balance: " + myAccount.getBalance());
    }
}

```

---

# Inheritance


---

# Polymorphism


---
