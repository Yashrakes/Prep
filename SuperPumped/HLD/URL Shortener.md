https://www.geeksforgeeks.org/system-design-url-shortening-service/
https://www.geeksforgeeks.org/how-to-design-a-tiny-url-or-url-shortener/
https://medium.com/@sandeep4.verma/system-design-scalable-url-shortener-service-like-tinyurl-106f30f23a82



first we will take a generalized approach, then solve for a specific use case (HLD perspective)
- explain what a url shorterner is
- explain how you can design a system for url shorterner. 
- in this explanation, proceed incrementally wherein we start with a basic solution, identify the gaps, and propose a better design that solves this issue and repeat this process until we reach  the right approach.  the idea is to build the solution incrementally at the same time understand what blockers one may face when designing and how can it be resolved. is this clear?
- Once the foundation is set, explain how this is designed in real life use cases
- Finally as an example: d

- design a url shorterner such that it can handle 20M url reads per day, the capacity of this service is to be able to generate 20M * 356 unique urls. the url shorterner can use lowercase characters, uppercase characters, and numbers 0 - 9.  the length of each generated url must be same and the smallest possible