#### Metadata

resource:
https://www.javatpoint.com/runtime-polymorphism-in-java

---
# Polymorphism

## Runtime Polymorphism/ Dynamic Method Dispatch

- Dynamic method dispatch is the mechanism by which a call to an `overridden method is resolved at run time`, rather than compile time.

- When an overridden method is called through a superclass reference, Java determines which version of that method to execute based upon the type of the object being referred to at the time the call occurs. 
- Thus, this determination is made at run time. 
- When different types of objects are referred to, different versions of an overridden method will be called. In other words, it is the type of the object being referred to (not the type of the reference variable) that determines which version of an overridden method will be executed. 
- Therefore, if a superclass contains a method that is overridden by a subclass, then when different types of objects are referred to through a superclass reference variable, different versions of the method are executed.

``` java
// Dynamic Method Dispatch

class A {
	void callme() {
		System.out.println("Inside A's callme method");
	}
}

class B extends A {
	// override callme()
	void callme() {
		System.out.println("Inside B's callme method");
	}
}

class C extends A {
	// override callme()
	void callme() {
		System.out.println("Inside C's callme method");
	}
}

class Dispatch {
	public static void main(String args[]) {
		A a = new A(); // object of type A
		B b = new B(); // object of type B
		C c = new C(); // object of type C
		
		A r; // obtain a reference of type A
		
		r = a; // r refers to an A object
		r.callme(); // calls A's version of callme
		
		r = b; // r refers to a B object
		r.callme(); // calls B's version of callme
		
		r = c; // r refers to a C object
		r.callme(); // calls C's version of callme
	}
}


The output from the program is shown here:
Inside A’s callme method
Inside B’s callme method
Inside C’s callme method
```


####  Java Runtime Polymorphism with Data Member

- A method is overridden, not the data members, so runtime polymorphism can't be achieved by data members.

- In the example given below, both the classes have a data member speedlimit. We are accessing the data member by the reference variable of Parent class which refers to the subclass object. Since we are accessing the data member which is not overridden, hence it will access the data member of the Parent class always.

```java
class Bike{  
	int speedlimit=90;  
}  

class Honda3 extends Bike{  
	int speedlimit=150;  

	public static void main(String args[]){  
		Bike obj=new Honda3();  
		System.out.println(obj.speedlimit);//90  
	}
}  
```

---

## Compile Time Polymorphism

- Achieved through `method overloading in java.`
- Whenever an object is bound with their functionality at the compile-time, this is known as the compile-time polymorphism. 
- At compile-time, java knows which method to call by checking the method signatures. So this is called compile-time polymorphism or static or early binding. 
- Compile-time polymorphism is achieved through [method overloading](https://www.geeksforgeeks.org/overloading-in-java/). Method Overloading says you can have more than one function with the same name in one class having a different prototype. 
- Function overloading is one of the ways to achieve polymorphism but it depends on technology that which type of polymorphism we adopt. In java, we achieve function overloading at compile-Time.

---


## Difference between Compile and Runtime

![[Pasted image 20210807083011.png]]


---