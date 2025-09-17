#### Metadata

resource:
https://www.javatpoint.com/final-keyword
https://www.geeksforgeeks.org/final-keyword-java/

---

# Final

## Java final variable

If you make any variable as final, you cannot change the value of final variable(It will be constant).

---
## Java final method

If you make any method as final, you cannot override it.

``` java
class Bike{  
  final void run(){System.out.println("running");}  
}  
     
class Honda extends Bike{  
   void run(){System.out.println("running safely with 100kmph");}  
     
   public static void main(String args[]){  
   Honda honda= new Honda();  
   honda.run();  
   }  
} 

Output: Compile Time Error
```

---
## Java final class

If you make any class as final, you cannot extend it.

---