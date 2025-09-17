## References
- https://www.geeksforgeeks.org/pointers-vs-references-cpp/


---

## Explanation

- [Pointers](https://www.geeksforgeeks.org/pointers-in-c-and-c-set-1-introduction-arithmetic-and-array/): A pointer is a variable that holds memory address of another variable. A pointer needs to be dereferenced with **\*** operator to access the memory location it points to. 

- [References](https://www.geeksforgeeks.org/references-in-c/) : A reference variable is an alias, that is, another name for an already existing variable. A reference, like a pointer, is also implemented by storing the address of an object.  A reference can be thought of as a constant pointer (not to be confused with a pointer to a constant value!) with automatic indirection, i.e the compiler will apply the **\*** operator for you.

>Example:
int i = 3;  <br>
// A pointer to variable i (or stores address of i)
int \*ptr = &i;  <br>
// A reference (or alias) for i.
int &ref = i;


#### Differences

###### Initialization

``` cpp
Pointer:

int a = 10;        
int *p = &a;    

	 OR 
	 
int *p;
p = &a;

//we can declare and initialize pointer at same step or in multiple line.


References:

int a=10;
int &p=a;  //it is correct

	but
	
int &p;
p=a;//it is incorrect as we should declare and initialize references at single step.
 
```

 **NOTE:** This differences may vary from compiler to compiler.The above differences is with respect to turbo IDE.
 
 
 ###### Reassignment
 - A pointer can be re-assigned. This property is useful for implementation of data structures like linked list, tree, etc.
 ```cpp
int a = 5;
int b = 6;
int \*p;
p =  &a;
p = &b;
```

<br>

- On the other hand, a reference cannot be re-assigned, and must be assigned at initialization.

``` cpp
int a = 5;
int b = 6;
int &p = a;
int &p = b;  //At this line it will show error as "multiple declaration is not allowed".

However it is valid statement,
int &q=p;
```


###### Memory Address

A pointer has its own memory address and size on the stack whereas a reference shares the same memory address (with the original variable) but also takes up some space on the stack.

``` cpp
int &p = a;
cout << &p << endl << &a;   
  ```
  
  
  ###### NULL value
  
  Pointer can be assigned NULL directly, whereas reference cannot. The constraints associated with references (no NULL, no reassignment) ensure that the underlying operations do not run into exception situation.
  
  ###### Indirection
  
  Pointer can be assigned NULL directly, whereas reference cannot. The constraints associated with references (no NULL, no reassignment) ensure that the underlying operations do not run into exception situation.
  ```cpp
  In Pointers,
int a = 10;
int \*p;
int \*\*q;  //it is valid.
p = &a;
q = &p;

Whereas in references,

int &p = a;
int &&q = p; //it is reference to reference, so it is an error.
```
  
   ###### Arithmetic operations
   Various arithmetic operations can be performed on pointers whereas there is no such thing called Reference Arithmetic.(but you can take the address of an object pointed by a reference and do pointer arithmetics on it as in &obj + 5).)
   
   ---