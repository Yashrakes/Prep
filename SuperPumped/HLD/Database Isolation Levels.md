
Database **Isolation Levels** define how much one transaction is allowed to see the changes made by other concurrent transactions.

The main goal is to balance **consistency** and **performance**.

---

## Problems Isolation Levels Try to Prevent

### 1. Dirty Read

Transaction B reads data modified by Transaction A **before A commits**.

```
T1: UPDATE account SET balance = 500;-- not committedT2: SELECT balance FROM account; -- reads 500
```

If T1 rolls back, T2 has read invalid data.

---

### 2. Non-Repeatable Read

Same row read twice gives different values.

```
T1: SELECT balance FROM account; -- 1000T2: UPDATE account SET balance = 1200;COMMIT;T1: SELECT balance FROM account; -- 1200
```

T1 got different values for the same row.

---

### 3. Phantom Read

Same query executed twice returns different number of rows.

```
T1:SELECT * FROM employees WHERE salary > 50000;-- returns 10 rowsT2:INSERT INTO employees VALUES (...,60000);COMMIT;T1:SELECT * FROM employees WHERE salary > 50000;-- returns 11 rows
```

A new row "appeared" (phantom).

---

# Isolation Levels

|Isolation Level|Dirty Read|Non-Repeatable Read|Phantom Read|
|---|---|---|---|
|Read Uncommitted|❌ Possible|❌ Possible|❌ Possible|
|Read Committed|✅ Prevented|❌ Possible|❌ Possible|
|Repeatable Read|✅ Prevented|✅ Prevented|❌ Possible*|
|Serializable|✅ Prevented|✅ Prevented|✅ Prevented|

---

## 1. Read Uncommitted (Lowest Isolation)

Allows reading uncommitted data.

### Example

```
T1:UPDATE users SET salary = 100000;-- not committedT2:SELECT salary FROM users;
```

T2 can see the uncommitted value.

### Pros

- Fastest
- Very little locking

### Cons

- Can read invalid data

Used rarely.

---

## 2. Read Committed (Most Common)

A transaction can only read committed data.

### Example

```
T1:UPDATE users SET salary = 100000;-- not committedT2:SELECT salary FROM users;
```

T2 sees the old value until T1 commits.

### Prevents

✅ Dirty Read

### Still Allows

❌ Non-Repeatable Read

❌ Phantom Read

### Used By

- Oracle Database (default)
- PostgreSQL (default)

---

## 3. Repeatable Read

If a row is read once, it will remain the same throughout the transaction.

### Example

```
T1:SELECT balance FROM account; -- 1000T2:UPDATE account SET balance = 2000;COMMIT;T1:SELECT balance FROM account; -- still 1000
```

### Prevents

✅ Dirty Read

✅ Non-Repeatable Read

### May Allow

❌ Phantom Read

### Used By

- MySQL default InnoDB isolation

---

## 4. Serializable (Highest Isolation)

Transactions behave as if they run one after another.

### Example

```
T1:SELECT COUNT(*) FROM usersWHERE city='Pune';T2:INSERT INTO users(city)VALUES('Pune');
```

T2 will wait until T1 finishes.

### Prevents

✅ Dirty Read

✅ Non-Repeatable Read

✅ Phantom Read

### Cons

- Most locking
- Lower throughput
- Higher contention

Used when correctness is more important than performance.

---

# Interview Question

## Why is Serializable Slow?

Because the database must ensure:

```
Concurrent execution      ↓Produces same result as      ↓Sequential execution
```

This requires:

- Row locks
- Range locks
- Predicate locks
- Transaction waiting

which reduces concurrency.

---

# Quick Memory Trick

```
Read Uncommitted    ↓Read Committed    ↓Repeatable Read    ↓Serializable
```

As you go down:

```
More Consistency ↑More Locking ↑More Safety ↑Performance ↓Concurrency ↓
```

---

## Real-world Defaults

|Database|Default Isolation|
|---|---|
|Oracle Database|Read Committed|
|PostgreSQL|Read Committed|
|MySQL (InnoDB)|Repeatable Read|
|Microsoft SQL Server|Read Committed|

### SDE-2 Interview One-Liner

- **Read Committed** → prevents dirty reads.
- **Repeatable Read** → prevents dirty + non-repeatable reads.
- **Serializable** → prevents dirty + non-repeatable + phantom reads by making transactions appear sequential.