#### Metadata

timestamp: **18:34**  &emsp;  **23-08-2021**

---

# GCD

### Question
Find GCD of m, n


---


### Approach

```
Lets say g is gcd(m, n) and m > n.

m = g * m1
n = g * m2

m - n = g * (m1 - m2)

gcd (m, n) = gcd(m-n, n)

           = gcd(m - 2n, n) if m >= 2n
           = gcd(m - 3n, n) if m >= 3n 
             .
             .
             .
           = gcd(m - k*n, n) if m >= k*n
           
       In other words, we keep subtracting n till the result is greater than 0. Ultimately we will end up with m % n.
       
              So gcd(m, n)  = gcd(m % n, n)
```


#### Code

``` cpp
int Solution::gcd(int m, int n) {

	if(m < n)
		   swap(m, n);

	if(n == 0)
		   return m;


	return gcd(m % n, n);
}
```

---


