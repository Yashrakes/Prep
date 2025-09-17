tags: #lower_bound, #binary_search

---
## Definition:
The **lower\_bound()** method in C++ is used to return an iterator pointing to the first element in the range \[first, last) which has a value not less than val. This means that the function returns the index of the next smallest number just greater than or equal to that number. If there are multiple values that are equal to val, lower\_bound() returns the index of the first such value.  
The elements in the range shall already be sorted or at least partitioned with respect to val.

>**Syntax 1:**   
ForwardIterator lower\_bound (ForwardIterator first, ForwardIterator last, const T& val);   
**Syntax 2:**   
ForwardIterator lower\_bound (ForwardIterator first, ForwardIterator last, const T& val, Compare comp);

**Parameters:** The above methods accept the following parameters.  

-   **first, last:** The range used is \[first, last), which contains all the elements between first and last, including the element pointed by first but not the element pointed by last.
-   **val:** Value of the lower bound to be searched for in the range.
-   **comp:** Binary function that accepts two arguments (the first of the type pointed by ForwardIterator, and the second, always val), and returns a value convertible to bool. The function shall not modify any of its arguments. This can either be a function pointer or a function object.

**Return Value:** An iterator to the lower bound of val in the range. If all the elements in the range compare less than val, the function returns last. If all the elements in the range are larger than val, the function returns a pointer to the first element.
<br>
---
## Example
> std::vector<``int``>::iterator low1, low2, low3;
low1 = std::lower_bound(v.begin(), v.end(), 30);
 low2 = std::lower_bound(v.begin(), v.end(), 35);
 low3 = std::lower_bound(v.begin(), v.end(), 55)
 
> OR
 int low1 = std::lower_bound(v.begin(), v.end(), 30) - v.begin();
 
 >Vector contains : 10 20 30 30 30 40 50
lower\_bound for element 30 at position : 2
lower\_bound for element 35 at position : 5
lower\_bound for element 55 at position : 7

---

## Resources 
-	[lower_bound gfg](https://www.geeksforgeeks.org/lower_bound-in-cpp/)
-	[stackoverflow](https://stackoverflow.com/questions/27776362/applying-c-lower-bound-on-an-array-of-char-strings)
-	[docs](https://www.cplusplus.com/reference/algorithm/lower_bound/)


---
