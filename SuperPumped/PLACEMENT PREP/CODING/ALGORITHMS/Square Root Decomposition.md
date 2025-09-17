#### Metadata
Resources:
- https://www.youtube.com/watch?v=ZakhE_eaonY
- https://cp-algorithms.com/data_structures/sqrt_decomposition.html

Similar: [Mo's Algorithm](https://www.geeksforgeeks.org/mos-algorithm-query-square-root-decomposition-set-1-introduction/?ref=rp)
parent: [[PLACEMENT PREP/CODING/ALGORITHMS/ALGO GUIDE]]
# Square Root Decomposition


## Overview

This technique helps us to reduce Time Complexity by a factor of **sqrt(n)**.   

_The key concept of this technique is to decompose given array into small chunks specifically of size sqrt(n)._  

- Let’s say we have an array of n elements and we decompose this array into small chunks of size sqrt(n). We will be having exactly sqrt(n) such chunks provided that n is a perfect square. Therefore, now our array on n elements is decomposed into sqrt(n) blocks, where each block contains sqrt(n) elements (assuming size of array is perfect square).  

- Let’s consider these chunks or blocks as an individual array each of which contains sqrt(n) elements and you have computed your desired answer(according to your problem) individually for all the chunks. Now, you need to answer certain queries asking you the answer for the elements in range l to r(l and r are starting and ending indices of the array) in the original n sized array.  

- The **naive approach** is simply to iterate over each element in range l to r and calculate its corresponding answer. Therefore, the Time Complexity per query will be O(n).  

- **Sqrt Decomposition Trick :** As we have already precomputed the answer for all individual chunks and now we need to answer the queries in range l to r. Now we can simply combine the answers of the chunks that lie in between the range l to r in the original array. So, if we see carefully here we are jumping sqrt(n) steps at a time instead of jumping 1 step at a time as done in naive approach. 


**NOTE**: Watch [this](https://www.youtube.com/watch?v=ZakhE_eaonY) to understand why this method is more efficient.

- See this problem for example : [[Range Sum Query - Mutable]]