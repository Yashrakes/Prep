#### Metadata:

- [documentation](https://www.cplusplus.com/reference/sstream/stringstream/)
- [GFG](https://www.geeksforgeeks.org/stringstream-c-applications/)
- Parent: [[1. STRING GUIDE]]


---
## Introduction

A stringstream associates a string object with a stream allowing you to read from the string as if it were a stream (like cin).

Basic methods are –

> clear() — to clear the stream  
> str() — to get and set string object whose content is present in stream.  
> operator << — add a string to the stringstream object.  
> operator >> — read something from the stringstream object,
---

## Applications
#### String to Number

**stringstream() :** This is an easy way to convert strings of digits into ints, floats or doubles. Following is a sample program using a stringstream to convert string to int.

```cpp
#include <iostream>
#include <sstream>
using namespace std;
 
int main()
{
    string s = "12345";
 
    // object from the class stringstream
    stringstream geek(s);
 
    // The object has the value 12345 and stream
    // it to the integer x
    int x = 0;
    geek >> x;
 
    // Now the variable x holds the value 12345
    cout << "Value of x : " << x;
 
    return 0;
}

```

---

#### Split string using delimiter

```cpp
string str = "A bc DEF ghi";
stringstream ss(str);
string token;
char delim = ' ';
while (getline(ss, token, delim))
	cout << token << endl;
```