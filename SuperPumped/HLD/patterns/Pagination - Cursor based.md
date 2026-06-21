# Cursor-Based Pagination for Infinite Scroll (Chronological Feed)

## Overview

Cursor-based pagination is an efficient and scalable way to implement infinite scrolling for feeds such as news articles or social media posts. Instead of using offset-based pagination, we use a cursor (typically derived from a unique, ordered field) to fetch the next set of results.

In this design, we assume that:

- Article IDs are monotonically increasing

- Higher article ID implies more recent content

- IDs are indexed and controlled by the system


---

## Core Idea

The feed is designed as an append-only downward scroll:

- When a user opens the feed, they see the latest articles (highest IDs)

- As they scroll down, older articles are fetched

- Newly published articles are not injected into the current scroll session

- Users must explicitly refresh to see new content


---

## Pagination Strategy

### Initial Load

Fetch the latest articles:

```sql
SELECT *
FROM articles
ORDER BY article_id DESC
LIMIT 20;
```

---

### Infinite Scroll (Fetching Older Articles)

Use the last seen article ID as the cursor:

```sql
SELECT *
FROM articles
WHERE article_id < :cursor
ORDER BY article_id DESC
LIMIT 20;
```

- cursor = article_id of the last item in the current feed

- Ensures stable and efficient pagination

- Prevents duplication and skipping when used correctly


---

## Two Cursor Model (Important)

To properly handle both old and new data, maintain two cursors:

### 1. Bottom Cursor (for infinite scroll)

Used to fetch older articles:

```sql
WHERE article_id < bottom_cursor
```

### 2. Top Cursor (for refresh / new content)

Used to fetch newly published articles:

```sql
WHERE article_id > top_cursor
```

- top_cursor = highest article_id currently visible to the user


This separation ensures:

- No duplication

- No missing records

- Clean user experience


---

## Key Properties

### 1. Monotonic Ordering

- Article IDs increase with time

- Guarantees:

- Deterministic ordering

- Simple pagination logic

- No need for offsets


---

### 2. No Mid-Stream Insertions

- New articles always have higher IDs

- They appear above the current feed

- Current scroll session remains unaffected


---

### 3. Efficient Querying

- Queries use indexed fields

- Avoids scanning large offsets

- Performs well at scale


---

## Why Cursor-Based Pagination?

Comparison with offset-based pagination:

|Aspect|Offset-Based|Cursor-Based|
|---|---|---|
|Performance|Degrades at scale|Consistent|
|Consistency|Can skip/duplicate|Stable|
|Real-time handling|Weak|Strong|
|Index usage|Inefficient|Efficient|

---

## Assumptions for Correctness

This design works correctly if:

1. Article IDs are strictly increasing

2. ID ordering reflects creation time

3. No delayed inserts or backfills occur

4. Ordering is consistent across reads


---

## Limitations

Even with monotonic IDs, certain scenarios can break assumptions:

### 1. Distributed Systems

- Multiple writers may generate IDs slightly out of real-time order


### 2. Backfills / Delayed Writes

- Older articles inserted later get newer IDs

- Breaks chronological correctness


### 3. Dynamic Ranking Feeds

If feed evolves to:

- Trending

- ML-based ranking

- Personalized ordering


Then:

- ID-based pagination becomes invalid

- Requires different strategies (ranking snapshots, caching, fanout)


---

## Recommended Production Improvement

For stronger guarantees, use a composite cursor:

```sql
SELECT *
FROM articles
WHERE (created_at, article_id) < (?, ?)
ORDER BY created_at DESC, article_id DESC
LIMIT 20;
```

Benefits:

- Handles same timestamp collisions

- Ensures strict ordering

- More robust in distributed systems


---

## Summary

- Cursor-based pagination is ideal for infinite scroll

- Monotonically increasing article IDs can act as a cursor

- Infinite scroll handles only older content

- New content is fetched via a separate top cursor

- This design is simple, scalable, and efficient

- For production-grade systems, consider using (timestamp, id) instead of just ID


---