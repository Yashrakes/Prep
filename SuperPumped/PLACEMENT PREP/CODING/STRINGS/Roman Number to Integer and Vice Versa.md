#### Metadata

timestamp: **13:12**  &emsp;  **04-07-2021**
topic tags: #string
question link: https://practice.geeksforgeeks.org/problems/roman-number-to-integer3201/1/?track=md-string&batchId=144#
resource: https://www.geeksforgeeks.org/converting-roman-numerals-decimal-lying-1-3999/
parent link: [[1. STRING GUIDE]]

---

# Roman Number to Integer 

### Question

Given a string in roman no format (s)  your task is to convert it to an integer . Various symbols and their values are given below.  
I 1  
V 5  
X 10  
L 50  
C 100  
D 500  
M 1000

---


### Approach

1.  Split the Roman Numeral string into Roman Symbols (character).
2.  Convert each symbol of Roman Numerals into the value it represents.
3.  Take symbol one by one from starting from the last index : 
    1.  If current value of symbol is greater than or equal to the value of next symbol, then add this value to the running total.
    2.  else subtract this value by adding the value of next symbol to the running total.

#### Code

``` cpp
int romanToDecimal(string &str) {
    unordered_map<char, int> T = { { 'I' , 1 },
                                   { 'V' , 5 },
                                   { 'X' , 10 },
                                   { 'L' , 50 },
                                   { 'C' , 100 },
                                   { 'D' , 500 },
                                   { 'M' , 1000 } };
    
    int n = str.length();
    int sum = T[str[n-1]];
    for(int i = n-2; i >= 0; i--)
        if(T[str[i]] >= T[str[i+1]])
            sum += T[str[i]];
        else
            sum -= T[str[i]];
            
    return sum;
}
```

---



# Integer To Roman Number

### Question

Given a number, find its corresponding Roman numeral.

---


### Approach

Compare given number with base values in the order 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1. Base value which is just smaller or equal to the given number will be the initial base value (largest base value) .Divide the number by its largest base value, the corresponding base symbol will be repeated quotient times, the remainder will then become the number for future division and repetitions.The process will be repeated until the number becomes zero.

#### Code

``` cpp
int printRoman(int number)
{
    int num[] = {1,4,5,9,10,40,50,90,100,400,500,900,1000};
    string sym[] = {"I","IV","V","IX","X","XL","L","XC","C","CD","D","CM","M"};
    int i=12;   
    while(number>0)
    {
      int div = number/num[i];
      number = number%num[i];
      while(div--)
      {
        cout<<sym[i];
      }
      i--;
    }
}

```

---


