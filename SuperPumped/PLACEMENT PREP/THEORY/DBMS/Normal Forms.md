#### Metadata:


resources: 
[Identify normalization](https://www.youtube.com/watch?v=mzxnbsmIRNw&list=PLmXKhU9FNesR1rSES7oLdJaNFgmuj0SYV&index=51)
[Normal Forms](https://www.geeksforgeeks.org/normal-forms-in-dbms/)

back Link: [[1. DBMS GUIDE]]

---
## Intro

Let there be a functional dependency from A --> B, then
- **Partial Dependency:**  A --> B is Partial if A is a prime attribute and a proper subset of the candidate key and B is a non prime attribute.
- **Transitive Dependency:** A --> B is transitive if both A and B are non prime attributes.


---
## 1NF
- https://www.geeksforgeeks.org/first-normal-form-1nf/


---

## 2NF

- https://www.geeksforgeeks.org/second-normal-form-2nf/
- The schema should be in 1NF
- There should not be any partial dependency. If there exists a dependency such that a proper subset of the candidate key relates to a non prime attribute, then it is known as a partial dependency.

---

## 3NF
- https://www.geeksforgeeks.org/third-normal-form-3nf/
- The schema should be in 2NF.
- There should not be any transitive dependency.

**OR**

 - If the schema has a FD A --> B, then it is said to be in 3NF if and only if
	- A is a super key, OR
	- B is a prime attribute 

---

## BCNF

- https://www.geeksforgeeks.org/boyce-codd-normal-form-bcnf/
- If the schema has a FD of the form A --> B, then it is said to be in BCNF if and only if
	- A is a super key


---