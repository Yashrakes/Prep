#### Metadata
resources:
https://www.geeksforgeeks.org/introduction-of-dbms-database-management-system-set-1/
https://www.geeksforgeeks.org/advantages-of-database-management-system/
---


A database-management system (DBMS) is a collection of interrelated data and a set of programs to access those data. The collection of data, usually referred to as the database, contains information relevant to an enterprise. The primary goal of a DBMS is to provide a way to store and retrieve database information that is both convenient and efficient.

---

## What is DBMS?

 The software which is used to manage database is called Database Management System (DBMS). For Example, MySQL, Oracle etc. are popular commercial DBMS used in different applications. DBMS allows users the following tasks:

**Data Definition:** It helps in creation, modification and removal of definitions that define the organization of data in database.

**Data Updation:** It helps in insertion, modification and deletion of the actual data in the database.

**Data Retrieval:** It helps in retrieval of data from the database which can be used by applications for various purposes.

**User Administration:** It helps in registering and monitoring users, enforcing data security, monitoring performance, maintaining data integrity, dealing with concurrency control and recovering information corrupted by unexpected failure.

---

## Paradigm Shift from File System to DBMS

-   **Redundancy of data:** Data is said to be redundant if same data is copied at many places. If a student wants to change Phone number, he has to get it updated at various sections. Similarly, old records must be deleted from all sections representing that student.

-   **Inconsistency of Data:** Data is said to be inconsistent if multiple copies of same data does not match with each other. If Phone number is different in Accounts Section and Academics Section, it will be inconsistent. Inconsistency may be because of typing errors or not updating all copies of same data.

-   **Difficult Data Access:** A user should know the exact location of file to access data, so the process is very cumbersome and tedious. If user wants to search student hostel allotment number of a student from 10000 unsorted students’ records, how difficult it can be.

-   **Unauthorized Access:** File System may lead to unauthorized access to data. If a student gets access to file having his marks, he can change it in unauthorized way.

-   **No Concurrent Access:** The access of same data by multiple users at same time is known as concurrency. File system does not allow concurrency as data can be accessed by only one user at a time.

-   **No Backup and Recovery:** File system does not incorporate any backup and recovery of data if a file is lost or corrupted.

These are the main reasons which made a shift from file system to DBMS.

---

## Advantages of DBMS

1.  **Better Data Transferring:**  
    Database management creates a place where users have an advantage of more and better managed data. Thus making it possible for end-users to have a quick look and to respond fast to any changes made in their environment.
    
      
    
2.  **Better Data Security:**  
    As number of users increases data transferring or data sharing rate also increases thus increasing the risk of data security. It is widely used in corporation world where companies invest money, time and effort in large amount to ensure data is secure and is used properly. A Database Management System (DBMS) provide a better platform for data privacy and security policies thus, helping companies to improve Data Security.  
    
      
    
      
    
3.  **Better data integration:**  
    Due to Database Management System we have an access to well managed and synchronized form of data thus it makes data handling very easy and gives integrated view of how a particular organization is working and also helps to keep a track on how one segment of the company affects other segment.
    
      
    
4.  **Minimized Data Inconsistency:**  
    Data inconsistency occurs between files when different versions of the same data appear in different places.  
    For Example, data inconsistency occurs when a student name is saved as “John Wayne” on a main computer of school but on teacher registered system same student name is “William J. Wayne”, or when the price of a product is $86.95 in local system of company and its National sales office system shows the same product price as $84.95.  
    So if a database is properly designed then Data inconsistency can be greatly reduced hence minimizing data inconsistency.
    
      
    
5.  **Faster data Access:**  
    The Data base management system (DBMS) helps to produce quick answers to database queries thus making data accessing faster and more accurate. For example, to read or update the data. For example, end users, when dealing with large amounts of sale data, will have enhanced access to the data, enabling faster sales cycle.  
    Some queries may be like:
    
    -   What is the increase of the sale in last three months?
    -   What is the bonus given to each of the salespeople in last five months?
    -   How many customers have credit score of 850 or more?
    
      
    
6.  **Better decision making:**  
    Due to DBMS now we have Better managed data and Improved data accessing because of which we can generate better quality information hence on this basis better decisions can be made.  
    Better Data quality improves accuracy, validity and time it takes to read data.  
    DBMS does not guarantee data quality, it provides a framework to make it is easy to improve data quality .
    
      
    
7.  **Increased end-user productivity:**  
    The data which is available with the help of combination of tools which transform data into useful information, helps end user to make quick, informative and better decisions that can make difference between success and failure in the global economy.
    
      
    
8.  **Simple:**  
    Data base management system (DBMS) gives simple and clear logical view of data. Many operations like insertion, deletion or creation of file or data are easy to implement.
	
---