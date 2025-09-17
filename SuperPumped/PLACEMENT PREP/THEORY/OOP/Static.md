# Static

## Java static variable

If you declare any variable as static, it is known as a static variable.

-   The static variable can be used to refer to the common property of all objects (which is not unique for each object), for example, the company name of employees, college name of students, etc.
-   The static variable** gets memory only once** in the class area at the time of class loading.

---

##  Java static method

If you apply static keyword with any method, it is known as static method.

-   A static method belongs to the class rather than the object of a class.
-   A static method can be invoked without the need for creating an instance of a class.
-   A static method can access static data member and can change the value of it.


#### Restrictions for the static method

1.  The static method can not use non static data member or call non-static method directly.
2.  this and super cannot be used in static context.

---

## Java Static block

-   Is used to initialize the static data member.
-   It is executed before the main method at the time of classloading.

``` java
class A2{  
	static{
		System.out.println("static block is invoked");
	}  
	
	public static void main(String args[]){  
		System.out.println("Hello main");  
	}  
}  


Output:
static block is invoked
Hello main
```

---

## Questions

####  Why is the Java main method static?

Java **main()** method is always static, so that compiler can call it without the creation of an object or before the creation of an object of the class.

-   In any Java program, the **main()** method is the starting point from where compiler starts program execution. So, the compiler needs to call the main() method.
-   If the **main()** is allowed to be non-static, then while calling the **main()** method JVM has to instantiate its class.
-   While instantiating it has to call the constructor of that class, `There will be ambiguity if the constructor of that class takes an argument.`
-   Static method of a class can be called by using the class name only without creating an object of a class.
-   The **main()** method in Java must be declared **public**, **static** and **void**. If any of these are missing, the Java program will compile but a runtime error will be thrown.


---

## Resources
- https://www.geeksforgeeks.org/static-methods-vs-instance-methods-java/

---