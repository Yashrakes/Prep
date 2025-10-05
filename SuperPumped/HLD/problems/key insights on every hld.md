
1. postgress handles 2 k to 4k request per second  , it uses geospatial indexing and quad trees
2. redis handle 100k to 1 m tranasaction per second 
3. redis support geohasing
4. use quad trees when you have uneven density and use geohashing when you have high right frequencies
5. 1 mb = 10 power 6   
6. 1 gb = 10 power 9    ,   1tb = 10 power 12 ,  1pb = 10 power 15 
7. redis pub sub handle 100 gb of data 
8. non func req - > scalbility, consistency , availablilty, security , throughput, fault tolerance