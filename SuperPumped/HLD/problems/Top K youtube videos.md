## Problem Statment

design a service which allows you to query the top k youtube most viewed videos in that time period 
time period are fixed and might be minute second , hour or all time 

https://www.hellointerview.com/learn/system-design/problem-breakdowns/top-k
https://www.youtube.com/watch?v=HjazbLlrWxI&t=51s
https://systemdesignschool.io/problems/topk/solution



## Functional Requirements 
1. Query top k most viewed videos (k = 1000)
2. accept a parameter which is a  time period (minute , hour, day)
3. sliding window
4. no arbitrary starting points

## Non Functional Requirements 
1. <1 min
2. 10- 100 ms latency
3. massive amount of views and videos
4. No approximations (Bloom filters and count min sketch) , for exact we use min heap + hashmap
5. 100 billion views per day 
6. 1 million views per second
7. 1 milliion videos per day -> 3.6 billion videos -> 100 gigbytes

## Core Entity / API 
1. view 
2. video
3. window
i am getting video ids of top 
get/views/video?k=1000&window = x
get a {video id, view}




## Tech used




