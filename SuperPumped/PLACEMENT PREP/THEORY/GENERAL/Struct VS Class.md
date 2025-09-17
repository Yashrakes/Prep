## References
- https://www.geeksforgeeks.org/structure-vs-class-in-cpp/
- https://www.geeksforgeeks.org/difference-c-structures-c-structures/


---
## Concept
- In C++, a structure is the same as a class except for a few differences. The most important of them is security. A Structure is not secure and cannot hide its implementation details from the end-user while a class is secure and can hide its programming and designing details.

- Following are the points that expound on this difference:   
	1) **Members of a class are private by defaul**t and **members of a structure are public by default.**
	2) When deriving a struct from a class/struct, the default access-specifier for a base class/struct is public. And when deriving a class, the default access specifier is private.
	3) Class can have null values but the structure can not have null values.
	4) Memory of structure is allocated in the **stack** while the memory of class is allocated in **heap**.
	5) Class requires constructor and destructor but the structure can not require it.
	6) Classes support polymorphism and also be inherited but the structure cannot be inherited.


---