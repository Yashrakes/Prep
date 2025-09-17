# Content

- [[#Clustered Indexes: The Physical Organization of Data]]
- [[#Non-Clustered Indexes Separate Lookup Structures]]
- [[#How They Work Together Lookup Process]]
- [[#Practical Example with Query Execution]]
- [[#Performance Implications and Tradeoffs]]
- [[#How Databases Handle Multiple Non-Clustered Indexes in a Single Query]]
- [[#Why B-Tree Clustered Indexes Store Data Only at the Leaf Level]]
- [[#Interview Questions]]

---
# Clustered Indexes: The Physical Organization of Data

- A clustered index determines the actual physical order of the data rows in a table. Think of it like how a dictionary organizes words in alphabetical order—the physical pages of the dictionary are arranged so that words appear in A-Z sequence.

### Key Properties of Clustered Indexes:
1. **One per table**: A table can have only one clustered index because the data rows themselves can only be stored in one physical order.
2. **Data and index are together**: The leaf level of a clustered index IS the data. There's no separate structure pointing to data—the data pages themselves are the leaf level of the index.
3. **Defines the physical sort order**: All rows in the table are stored in the order of the clustered index key.
4. **Often on primary key**: By default, many database systems automatically create a clustered index on the primary key.

### How a Clustered Index Works
- Let's say we have an `employees` table with a clustered index on the `employee_id` column:
![[Pasted image 20250426150546.png]]

### Real-World Analogy
- Think of a clustered index like a physical phone book where people are arranged alphabetically by last name. The book itself is organized in the same order as its index (alphabetical order). To find "Smith, John," you use the alphabetical order to navigate directly to where all the Smiths are physically located in the book.

---
# Non-Clustered Indexes: Separate Lookup Structures

- A non-clustered index is a separate structure from the data itself. It contains copies of indexed columns along with pointers to the actual data rows.

### Key Properties of Non-Clustered Indexes:
1. **Multiple per table**: A table can have many non-clustered indexes (though each one adds overhead).
2. **Separate from data**: Non-clustered indexes exist as separate structures apart from the actual data pages.
3. **Contains pointers**: The leaf level of a non-clustered index contains pointers (row locators) to the actual data rows, not the data itself.
4. **Additional storage**: Each non-clustered index requires additional storage space beyond the table data.

### How a Non-Clustered Index Works
- Continuing with our `employees` table example, let's say we add a non-clustered index on the `last_name` column:

![[Pasted image 20250426150701.png]]
### Real-World Analogy
A non-clustered index is like the index at the back of a textbook. The index lists topics alphabetically with page numbers, but the actual content remains organized by chapter (not alphabetically). To find information about "Photosynthesis," you first look it up in the index to find the page number, then jump to that page.

---
# How They Work Together: Lookup Process

### Finding Data with a Clustered Index
When you search for data using a clustered index:
1. The database navigates the B-tree structure to find the correct data page
2. Since the leaf level of the index IS the data, once you reach the leaf level, you have the complete row

```sql
-- Using the clustered index (employee_id)
SELECT * FROM employees WHERE employee_id = 245;
```

- This query can go directly to the correct data page containing employee #245.

### Finding Data with a Non-Clustered Index
When you search for data using a non-clustered index:
1. The database navigates the non-clustered index B-tree structure
2. At the leaf level, it gets a pointer to the actual data row
3. It then performs a **lookup operation** to fetch the complete row from the data pages

```sql
-- Using the non-clustered index (last_name)
SELECT * FROM employees WHERE last_name = 'Smith';
```

This query requires:
- Finding 'Smith' in the non-clustered index
- Reading the row pointer(s)
- Additional lookup(s) to fetch the actual data

### Important Detail: Row Locator Type
The way non-clustered indexes point to data depends on whether the table has a clustered index:
- **With clustered index**: Non-clustered indexes use the clustered index key as the row locator (not a direct physical pointer)
- **Without clustered index** (heap table): Non-clustered indexes use a physical row identifier (RID) pointing to the exact location
This is why clustered index design impacts non-clustered index performance!

---
# Practical Example with Query Execution

- Let's walk through a concrete example with a table of 1 million customer records:

```sql
CREATE TABLE customers (
    customer_id INT PRIMARY KEY,  -- This becomes the clustered index by default
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100),
    signup_date DATE
);

-- Add a non-clustered index on last_name
CREATE INDEX idx_lastname ON customers(last_name);
```

### Scenario 1: Query Using Clustered Index

```sql
SELECT * FROM customers WHERE customer_id = 500000;
```
- Execution process:
	1. Database uses the clustered index to navigate directly to the correct page
	2. Single lookup operation to get the entire row
	3. Very efficient (typically Log(n) page reads)

### Scenario 2: Query Using Non-Clustered Index

```sql
SELECT * FROM customers WHERE last_name = 'Johnson';
```
- Execution process:
	1. Database uses the non-clustered index on last_name to find all 'Johnson' entries
	2. For each matching 'Johnson', it reads the corresponding customer_id (clustered key)
	3. It then makes an additional lookup using that customer_id to fetch the full row data
	4. Overall slower than using the clustered index directly

---
# Performance Implications and Tradeoffs

### Clustered Index Considerations:
1. **Choose carefully**: You get only one, so pick a column that:
    - Is frequently used in range queries and sorting operations
    - Has high cardinality (many unique values)
    - Rarely or never changes value (updates are expensive)
    - Is narrow (small data types use less space in non-clustered indexes)
2. **Sequential vs. random values**: Sequential values (like identity/auto-increment columns) minimize page splits and fragmentation
3. **Impact on non-clustered indexes**: All non-clustered indexes store the clustered index key as their row locator, so a wide clustered key increases the size of all non-clustered indexes

### Non-Clustered Index Considerations:
1. **Extra lookup cost**: Unless it's a covering index, there's always an extra lookup step
2. **Index size vs. query performance**: More indexes improve read performance but increase storage and harm write performance
3. **Column order matters**: In composite indexes, the order of columns dramatically affects which queries can use the index effectively

---
# How Databases Handle Multiple Non-Clustered Indexes in a Single Query

- When you have multiple non-clustered indexes on a table and your WHERE clause references multiple columns, the database has several possible approaches to handle the query. Let's explore what actually happens behind the scenes.

## Index Intersection vs. Single Index Selection
- When your query includes multiple conditions on different columns, the database optimizer has a few main options:
### Option 1: Choose a Single "Best" Index
- The query optimizer will evaluate all available indexes and typically select just one index that it believes will be most efficient. It considers:
	- Which index can eliminate the most rows (highest selectivity)
	- The estimated number of rows that will be returned
	- The cost of index lookups vs. data access
	- Whether any index covers all needed columns
- For example, if you have:
```sql
SELECT * FROM customers 
WHERE last_name = 'Smith' AND city = 'Chicago';
```

- And you have separate indexes on `last_name` and `city`, the optimizer might determine that:
	- The `last_name` index might match 10,000 rows (not very selective)
	- The `city` index might match 500 rows (more selective)
- In this case, it would likely choose the `city` index as the access path, then filter those 500 rows for the last_name condition.

### Option 2: Index Intersection (Multiple Index Usage)
- Some advanced database systems can use multiple indexes simultaneously through a technique called "index intersection" or "index combining." Here's how it works:
	1. The database uses the first index to find matching row IDs
	2. It uses the second index to find another set of matching row IDs
	3. It performs an intersection operation to find row IDs that appear in both sets
	4. It retrieves only the rows with those IDs
- This can be very efficient when each individual condition is highly selective but no single index covers all conditions.

```sql
SELECT * FROM customers 
WHERE last_name = 'Smith' AND city = 'Chicago';
```

- Index intersection would:
	1. Use the `last_name` index to find all row IDs for 'Smith' (10,000 IDs)
	2. Use the `city` index to find all row IDs for 'Chicago' (500 IDs)
	3. Compute the intersection of these two sets (perhaps 20 IDs)
	4. Fetch only those 20 rows

### Option 3: Composite Index Match
- If you have a composite index that includes both columns (like `CREATE INDEX idx_name_city ON customers(last_name, city)`), this would be the most efficient option. The database would use this single index to directly locate the rows matching both conditions without any intersection operations.

---
## Real-World Example: Step by Step

Let's walk through a concrete example to see how this works:
```sql
-- Our table with multiple indexes
CREATE TABLE products (
    product_id INT PRIMARY KEY,  -- Clustered index
    category_id INT,
    brand_id INT,
    price DECIMAL(10,2),
    stock_quantity INT,
    name VARCHAR(100)
);

-- Individual indexes on different columns
CREATE INDEX idx_category ON products(category_id);
CREATE INDEX idx_brand ON products(brand_id);
CREATE INDEX idx_price ON products(price);

-- Our query uses multiple conditions
SELECT * FROM products 
WHERE category_id = 5 AND brand_id = 12 AND price < 50;
```

### What Happens During Query Execution?
1. **Query Parsing**: The SQL is parsed into a logical plan
2. **Query Optimization**: The optimizer evaluates all possible execution plans:
    - Use idx_category, then filter by brand and price
    - Use idx_brand, then filter by category and price
    - Use idx_price for range scan, then filter by category and brand
    - Use index intersection between multiple indexes
3. **Statistics & Cost Estimation**: The optimizer checks statistics:
    - Category 5 has 5,000 products (out of 1 million)
    - Brand 12 has 2,000 products (out of 1 million)
    - Products under $50 are 400,000 (out of 1 million)
4. **Plan Selection**: Based on these statistics, using idx_brand is most selective, so the optimizer might choose:
    - Seek into idx_brand to find all brand_id = 12 rows (2,000 rows)
    - For each matching row, check if category_id = 5 and price < 50
    - This might leave us with perhaps 100 final rows
5. **Execution**: The database executes this plan, retrieving only the rows that match all conditions

### Index Intersection (When Available)
If the database supports index intersection and determines it's more efficient:
1. Use idx_brand to get row IDs where brand_id = 12 (2,000 IDs)
2. Use idx_category to get row IDs where category_id = 5 (5,000 IDs)
3. Compute the intersection (approximately 10 IDs assuming even distribution)
4. Check the price condition on these 10 rows
5. Return the final matching rows

---
## Key Factors That Influence Index Selection

The database considers several factors when deciding which index(es) to use:
1. **Selectivity of each condition**: How many rows will each condition filter out?
2. **Index type and structure**: B-tree indexes are great for equality and range conditions.
3. **Join operations**: If the query involves joins, that affects index selection.
4. **Data distribution**: The optimizer uses statistics about how data is distributed.
5. **Available memory**: More memory allows for more complex operations like index intersection.
6. **Query complexity**: More complex queries might favor simpler execution plans.

---
# Why B-Tree Clustered Indexes Store Data Only at the Leaf Level

- That's an excellent question! The design decision to store data only at leaf nodes in B-tree clustered indexes might seem counterintuitive at first glance. Let's explore why databases are built this way and why storing data in non-leaf nodes would actually create more problems than it solves.

## The Physical Structure Constraint
B-trees are fundamentally designed as balanced tree structures where:
1. Each non-leaf node contains keys and pointers to child nodes
2. Each leaf node contains the actual data (for clustered indexes) or pointers to data (for non-clustered indexes)

The key insight is that **non-leaf nodes are navigational aids**, not storage containers. Their purpose is to guide the search process efficiently down to the correct leaf node.

## Why Not Store Data in Non-Leaf Nodes?
Your intuition that storing matching data in non-leaf nodes could speed up retrieval seems logical, but several technical challenges make this impractical:

### 1. Variable-Length Data and Page Structure
Database pages have fixed sizes (typically 8KB or 16KB). Non-leaf nodes in B-trees are optimized to store as many keys as possible to minimize tree depth. If we stored complete data rows in these nodes:
- Each non-leaf node could hold far fewer keys due to the space consumed by data rows
- This would increase the tree height dramatically
- More I/O operations would be needed to traverse the tree
For example, if keys are 8 bytes but complete rows are 400 bytes, a non-leaf node that could hold 500 keys would now only hold 10 complete rows with data.

### 2. Data Duplication Issues
Consider a B-tree for employee data with a clustered index on employee_id. If we stored complete data rows in non-leaf nodes:
- The same employee data would exist in both leaf and non-leaf nodes
- This redundancy would waste storage space
- Updates would need to modify multiple copies, introducing consistency challenges

### 3. Search Algorithm Complexity
The standard B-tree search algorithm is designed to follow a path from root to leaf:

```
search(key):
    start at root
    while current node is not a leaf:
        find appropriate child node based on key
        move to child node
    search for key in leaf node
```

If data could be found at any level, the algorithm would need to check every node it visits for matching data, complicating the search logic substantially.

### 4. Maintenance Complexity
B-trees maintain balance through node splitting and merging operations. These operations would become far more complex if complete data rows were stored in internal nodes:
- Node splits would need to handle redistributing data rows
- Tree rebalancing would be more expensive
- Page utilization would be less efficient

---
# Interview Questions

## Question: How would you index a products table to optimize a query that filters by category, brand, and price range?

- "I'd analyze the query patterns and data distribution first. If these three columns frequently appear together in queries, I'd create a composite index. Since category and brand are equality conditions while price is a range condition, I'd order them:

```sql
CREATE INDEX idx_category_brand_price ON products(category_id, brand_id, price);
```

- This index would efficiently handle queries like:

```sql
SELECT * FROM products 
WHERE category_id = 5 AND brand_id = 12 AND price BETWEEN 10 AND 50;
```

- If these columns also appear in different combinations in other queries, I might need separate indexes for those patterns. I'd monitor query performance and execution plans to verify the indexes are being used effectively and adjust as needed."
---
## How would you determine which columns need indexes in a large production database?

1. First, I'd identify the most frequently executed and high-impact queries using performance monitoring tools.
2. For each critical query, I'd examine execution plans to see where full table scans or inefficient index usage occurs.
3. I'd analyze the selectivity of columns in WHERE clauses by checking the data distribution. Highly selective columns (those that narrow down results significantly) are good index candidates.
4. For example, if we have a users table where:
    - gender has 2 distinct values (low selectivity)
    - city has 1,000 distinct values (medium selectivity)
    - email has unique values (high selectivity)
    - I'd focus on indexing high and medium selectivity columns that appear in frequent queries.
5. I'd validate my analysis by creating test indexes and comparing execution plans before and after, looking for improvements in estimated rows examined and overall cost.
6. Finally, I'd consider the write/read ratio of the application, since indexes that speed up queries also slow down writes. For heavy write tables, I'd be more selective about adding indexes.
---
