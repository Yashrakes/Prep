

. In a CDC architecture, there is **a separate service/process** whose only job is to read the database's transaction logs (WAL, binlog, redo log) and publish changes to Kafka.

The most common one is **Debezium**.

### Architecture

```
             INSERT/UPDATE/DELETE
Application ----------------------> Database
                                        |
                                        | WAL / Binlog / Redo Log
                                        |
                                  +-------------+
                                  | Debezium    |   (CDC Service)
                                  +-------------+
                                        |
                                        | Kafka Producer API
                                        |
                                  Kafka Topics
                                        |
                                 Consumer Services
```

### What does Debezium do?

Suppose your application executes:

```
INSERT INTO orders
VALUES (101, 'Laptop', 50000);
```

The database writes this change into its transaction log:

```
LSN: 12345
Table: orders
Operation: INSERT
Row:
{
  id:101,
  product:"Laptop",
  price:50000
}
```

Debezium continuously watches the log and converts it into a Kafka event like:

```
{
  "before": null,
  "after": {
    "id": 101,
    "product": "Laptop",
    "price": 50000
  },
  "op": "c"
}
```

It then publishes that event to a Kafka topic, such as:

```
dbserver.orders
```

---

## How does it know where it left off?

Debezium stores its own **offset** (similar to a Kafka consumer offset).

Example:

```
Last processed WAL position = 12345

Restart

↓

Continue from 12346
```

It does **not** reread the entire log after a restart.

---

## Does it query the database?

No.

That's one of the biggest advantages.

Instead of repeatedly executing queries like:

```
SELECT * FROM orders
WHERE updated_at > last_timestamp;
```

it simply reads the database's transaction log.

That means:

- Very low overhead on the database.
- Near real-time changes.
- No expensive polling.

---

## Is Debezium part of the database?

No.

It is an **external service**.

For example, you might deploy it like this:

```
+----------------------+
| Database             |
+----------------------+

+----------------------+
| Debezium             |
+----------------------+

+----------------------+
| Kafka                |
+----------------------+
```

It connects to the database with the required permissions to read the transaction log.

---

## Is Debezium the only CDC service?

No. There are several CDC tools:

- **Debezium** (most popular open source)
- Oracle GoldenGate (Oracle databases)
- AWS Database Migration Service (AWS DMS)
- SQL Server CDC
- Google Datastream
- Fivetran
- Qlik Replicate

---

### One important clarification

CDC captures **every database change**. If your table receives:

- 1,000 inserts/sec
- 500 updates/sec
- 200 deletes/sec

the CDC service will stream all 1,700 changes/sec to Kafka unless you configure filters.

This is why many microservices combine the **Transactional Outbox** pattern with CDC: the application writes only the business events it wants to publish into an **outbox table**, and the CDC service watches just that table instead of every table in the database. That reduces unnecessary events while still avoiding the dual-write problem.