- https://www.toptal.com/big-data/consistent-hashing
- **What is Consistent Hashing?:** https://www.youtube.com/watch?v=zaRkONvyGr8
- **Code:** https://github.com/coding-parrot/Low-Level-Design/blob/master/service-orchestrator/src/main/java/algorithms/ConsistentHashing.java
- **Best resource:** https://michaelnielsen.org/blog/consistent-hashing/
---
(Gaurav Sen)
- Load Balancing is a key concept to system design. One of the popular ways to balance load in a system is to use the concept of consistent hashing. Consistent Hashing allows requests to be mapped into hash buckets while allowing the system to add and remove nodes flexibly so as to maintain a good load factor on each machine.

- The standard way to hash objects is to map them to a search space, and then transfer the load to the mapped computer. A system using this policy is likely to suffer when new nodes are added or removed from it. 

- Consistent Hashing maps servers to the key space and assigns requests(mapped to relevant buckets, called load) to the next clockwise server. Servers can then store relevant request data in them while allowing the system flexibility and scalability.

- Some terms you would here in system design interviews are Fault Tolerance, in which case a machine crashes. And Scalability, in which case machines need to be added to process more requests. These two principles are allowed by Consistent Hashing, and hence it is an important building block to a system design architect's toolbox.

- Another term used often is request allocation. This means assigning a request to a server. Consistent hashing assigns requests to the servers in a way that the load is balanced are remains close to equal. 

- Server architecture is a subjective concept, and there are outliers for many cases. Don't think of Consistent Hashing as a silver bullet for fault tolerance and scalability, but a useful concept for request allocation.


---