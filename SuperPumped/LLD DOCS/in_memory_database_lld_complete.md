# IN-MEMORY DATABASE SYSTEM - LLD INTERVIEW GUIDE

## SECTION 1: Requirements Clarification (0-5 min)

### Questions to Ask:
1. **Column Types**: What data types? → STRING, INTEGER, DOUBLE, BOOLEAN
2. **Validations**: What constraints? → NOT_NULL, RANGE, REQUIRED, REGEX, CUSTOM
3. **Index Types**: What indexes needed? → PRIMARY (Hash), SECONDARY (Tree), FUZZY (Trie), REVERSE
4. **Query Operators**: What filters? → EQUALS, GT, LT, IN, CONTAINS, BETWEEN
5. **Query Composition**: How to combine filters? → AND, OR, NOT
6. **Concurrency**: Single-threaded or multi-threaded? → Start single, mention thread-safety
7. **Scale**: Rows per table? → Assume 100K-1M rows per table
8. **Transactions**: ACID needed? → Out of scope for LLD, mention for production

### Confirmed Scope:
✅ Schema: Define columns with types + validators
✅ CRUD: Insert, Update, Delete with validation
✅ Query: Filter on any column with multiple criteria
✅ Indexes: Hash (O(1)), Tree (O(log n)), Fuzzy (Trie)
✅ Validation: Chain of validators per column
✅ Error Handling: Report validation failures with details

---

## SECTION 2: Core Classes & Design

### 2.1 Column
```java
public enum ColumnDataType { STRING, INTEGER, DOUBLE, BOOLEAN }

public class Column {
    private final String name;
    private final ColumnDataType dataType;
    private final List<Validator> validators;
    private final boolean required;
    
    public void validate(Object value) throws ValidationException {
        if (required && value == null) {
            throw new ValidationException(name + " is required");
        }
        for (Validator validator : validators) {
            validator.validate(value);
        }
    }
}
```

### 2.2 Validator (Strategy Pattern)
```java
public interface Validator {
    void validate(Object value) throws ValidationException;
}

public class NotNullValidator implements Validator {
    public void validate(Object value) {
        if (value == null) throw new ValidationException("Cannot be null");
    }
}

public class RangeValidator implements Validator {
    private final int min, max;
    public RangeValidator(int min, int max) { this.min = min; this.max = max; }
    
    public void validate(Object value) {
        int val = (Integer) value;
        if (val < min || val > max) {
            throw new ValidationException("Value must be between " + min + " and " + max);
        }
    }
}

public class RegexValidator implements Validator {
    private final Pattern pattern;
    public RegexValidator(String regex) { this.pattern = Pattern.compile(regex); }
    
    public void validate(Object value) {
        if (!pattern.matcher((String) value).matches()) {
            throw new ValidationException("Does not match pattern");
        }
    }
}
```

### 2.3 Schema
```java
public class Schema {
    private final String tableName;
    private final List<Column> columns;
    private final String primaryKeyColumn;
    private final Map<String, Column> columnMap;
    
    public void validate(Row row) throws ValidationException {
        for (Column column : columns) {
            Object value = row.getValue(column.getName());
            column.validate(value);
        }
    }
    
    public static class Builder {
        private String tableName;
        private List<Column> columns = new ArrayList<>();
        private String primaryKey;
        
        public Builder name(String name) { this.tableName = name; return this; }
        public Builder addColumn(Column col) { columns.add(col); return this; }
        public Builder primaryKey(String pk) { this.primaryKey = pk; return this; }
        public Schema build() { return new Schema(tableName, columns, primaryKey); }
    }
}
```

### 2.4 Row
```java
public class Row {
    private final String rowId;
    private final Map<String, Object> data;
    
    public Row(String rowId) {
        this.rowId = rowId;
        this.data = new HashMap<>();
    }
    
    public void setValue(String columnName, Object value) {
        data.put(columnName, value);
    }
    
    public Object getValue(String columnName) {
        return data.get(columnName);
    }
}
```

### 2.5 Index (Strategy Pattern)
```java
public interface Index {
    void insert(Object value, String rowId);
    void delete(Object value, String rowId);
    Set<String> search(Object value);
    Set<String> rangeSearch(Object from, Object to);
}

public class HashMapIndex implements Index {
    private Map<Object, Set<String>> index = new HashMap<>();
    
    public void insert(Object value, String rowId) {
        index.computeIfAbsent(value, k -> new HashSet<>()).add(rowId);
    }
    
    public Set<String> search(Object value) {
        return index.getOrDefault(value, Collections.emptySet());
    }
    // O(1) for equality, doesn't support range
}

public class TreeMapIndex implements Index {
    private TreeMap<Object, Set<String>> index = new TreeMap<>();
    
    public Set<String> rangeSearch(Object from, Object to) {
        return index.subMap(from, true, to, true).values().stream()
            .flatMap(Set::stream).collect(Collectors.toSet());
    }
    // O(log n) for search, supports range queries
}

public class FuzzyIndex implements Index {
    private TrieNode root = new TrieNode();
    // Trie for prefix/fuzzy matching on strings
}
```

### 2.6 Table
```java
public class Table {
    private final Schema schema;
    private final Map<String, Row> rows;
    private final Map<String, Index> indexes;
    private final AtomicLong rowIdGenerator;
    
    public String insert(Row row) throws ValidationException {
        schema.validate(row);
        String rowId = generateRowId();
        row.setRowId(rowId);
        rows.put(rowId, row);
        updateIndexes(row, rowId);
        return rowId;
    }
    
    public void update(String rowId, Row newRow) throws ValidationException {
        schema.validate(newRow);
        Row oldRow = rows.get(rowId);
        removeFromIndexes(oldRow, rowId);
        rows.put(rowId, newRow);
        updateIndexes(newRow, rowId);
    }
    
    public void delete(String rowId) {
        Row row = rows.remove(rowId);
        removeFromIndexes(row, rowId);
    }
    
    public List<Row> query(Query query) {
        Set<String> matchingRowIds = queryExecutor.execute(this, query);
        return matchingRowIds.stream().map(rows::get).collect(Collectors.toList());
    }
}
```

### 2.7 Query & Filter
```java
public class Query {
    private final List<FilterCriteria> filters;
    private final List<String> projectionColumns;
    
    public static class Builder {
        private List<FilterCriteria> filters = new ArrayList<>();
        public Builder filter(String col, Operator op, Object val) {
            filters.add(new FilterCriteria(col, op, val));
            return this;
        }
        public Query build() { return new Query(filters); }
    }
}

public class FilterCriteria {
    private final String columnName;
    private final Operator operator;
    private final Object value;
}

public enum Operator { EQUALS, GT, LT, GTE, LTE, IN, CONTAINS, BETWEEN }
```

---

## SECTION 3: Design Patterns

### 1. Strategy Pattern — Validator
- **Problem**: Different validation rules for different columns
- **Solution**: Validator interface with NotNull, Range, Regex implementations
- **Benefit**: Add new validators without modifying Column class (Open/Closed Principle)

### 2. Strategy Pattern — Index
- **Problem**: Different index types for different query patterns
- **Solution**: Index interface with HashMap (O(1)), TreeMap (O(log n)), Trie (fuzzy)
- **Benefit**: Swap index implementation without changing Table

### 3. Builder Pattern — Schema & Query
- **Problem**: Complex object construction with many optional parameters
- **Solution**: Schema.Builder and Query.Builder for fluent API
- **Benefit**: Readable, self-documenting code

### 4. Chain of Responsibility — Validation
- **Problem**: Multiple validators need to run in sequence
- **Solution**: Each validator checks and passes to next
- **Benefit**: Flexible validation pipeline

### 5. Factory Pattern — ValidatorFactory, IndexFactory
- **Problem**: Creating validators/indexes based on config
- **Solution**: Factory creates correct implementation
- **Benefit**: Centralized creation logic

### 6. Composite Pattern — CompositeFilter
- **Problem**: Combine multiple filters with AND/OR
- **Solution**: CompositeFilter holds list of filters + logical operator
- **Benefit**: Recursive filter composition

---

## SECTION 4: Interview Questions (6 Questions)

### Q1: How do you handle schema validation efficiently?
**A1**: Chain of Responsibility — run all validators, collect errors
**A2**: Fail-fast — stop on first error for performance
**A3**: Parallel validation with CompletableFuture for independent validators
**A4**: Cache validation results if row is immutable

### Q2: How do you choose which index to use for a query?
**A1**: Query planner — analyze filters, pick index with highest selectivity
**A2**: Cost-based — estimate cardinality, choose lowest cost path
**A3**: Heuristic — equality filter > range > full scan
**A4**: User hint — allow query to specify index (like SQL USE INDEX)

### Q3: Multi-column filters (age > 25 AND city = 'NYC')?
**A1**: Index intersection — use both indexes, intersect results
**A2**: Pick most selective, filter rest in-memory
**A3**: Composite index on (city, age) if pattern known
**A4**: Bitmap index for low-cardinality columns

### Q4: How to implement fuzzy search?
**A1**: Trie for prefix matching
**A2**: Levenshtein distance with BK-tree
**A3**: N-gram tokenization + inverted index
**A4**: Soundex/Metaphone for phonetic matching

### Q5: Bulk insert validation failures?
**A1**: Atomic — reject entire batch
**A2**: Best-effort — insert valid rows, return failures
**A3**: Transaction with rollback
**A4**: Two-phase — validate all, then insert all

### Q6: Scale query performance?
**A1**: Add more indexes (trade-off: slower writes)
**A2**: Partition table by key range
**A3**: Parallel query with ForkJoinPool
**A4**: Materialized views for aggregations

---

## SECTION 5: 40-Minute Presentation Flow

**0-5 min**: Requirements
- "Let me clarify column types, validators, index types, and query capabilities"

**5-10 min**: Architecture Overview
- "Core components: Schema validates, Index accelerates, QueryExecutor combines filters"

**10-16 min**: Schema & Validation
- "Column holds validators (Strategy). Schema validates via Chain of Responsibility"

**16-24 min**: Index Design
- "4 index types: HashMap O(1) equality, TreeMap O(log n) range, Trie fuzzy, Reverse suffix"

**24-30 min**: Query Execution
- "QueryExecutor picks best index via selectivity. Index intersection for multi-filter"

**30-36 min**: Insert/Update/Delete
- "Insert: validate → generate rowId → store → update indexes. Delete reverses this"

**36-40 min**: Trade-offs & Extensions
- "Write amplification with many indexes. Discuss transactions, MVCC, thread-safety"

---

## SECTION 6: Critical Success Factors

### ✅ DO SAY:
- "Strategy Pattern for Validator AND Index"
- "Index selection: equality > range > full scan"
- "O(1) HashMap vs O(log n) TreeMap trade-off"
- "Chain of Responsibility for validation pipeline"
- "Thread-safety: ConcurrentHashMap for indexes"
- "Query planner for index selection"

### ❌ AVOID:
- No validation framework (inline if-else)
- Full table scan for every query
- Forgetting to update indexes on insert/delete
- Ignoring thread-safety
- No error handling for validation
- Not explaining time complexity

---

## SECTION 7: Opening Line

**"In-memory database design revolves around three pillars:**
**1. Schema validation for data integrity**
**2. Indexing for query performance**
**3. Query executor that intelligently picks the right index.**

**Let me show you how each component works and the design patterns that make this extensible."**

---

## Complete Usage Example

```java
// 1. Define schema
Schema schema = new Schema.Builder()
    .name("users")
    .addColumn(new Column("id", INTEGER, true))
    .addColumn(new Column("name", STRING, true)
        .addValidator(new NotNullValidator()))
    .addColumn(new Column("age", INTEGER, false)
        .addValidator(new RangeValidator(0, 120)))
    .addColumn(new Column("email", STRING, true)
        .addValidator(new RegexValidator("^[A-Za-z0-9+_.-]+@(.+)$")))
    .primaryKey("id")
    .build();

// 2. Create table
Table users = new Table(schema);

// 3. Add indexes
users.createIndex("age", new TreeMapIndex());  // range queries
users.createIndex("name", new FuzzyIndex());   // fuzzy search

// 4. Insert data
Row row1 = new Row();
row1.setValue("id", 1);
row1.setValue("name", "Alice");
row1.setValue("age", 30);
row1.setValue("email", "alice@example.com");
users.insert(row1);

// 5. Query with filters
Query query = new Query.Builder()
    .filter("age", GT, 25)
    .filter("name", CONTAINS, "Ali")
    .build();
    
List<Row> results = users.query(query);
```

---

END OF DOCUMENT
