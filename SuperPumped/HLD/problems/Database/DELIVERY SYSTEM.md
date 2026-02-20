
---

## The Core Problem Delivery Systems Solve

On the surface delivery seems simple — customer orders, driver picks up, delivers. But consider the real constraints:

```
Uber Eats / DoorDash reality:
────────────────────────────────────────────────
Active orders:               500,000+ simultaneously
Active drivers:              200,000+ simultaneously  
Driver location updates:     Every 4 seconds per driver
                            = 50,000 updates/second
New order rate:              10,000+ orders/minute (peak)
Driver assignment latency:   <500ms (find nearest driver)
Route recalculation:         Real-time (traffic changes)

Requirements:
→ "Find 10 nearest available drivers within 2 miles of restaurant" (<200ms)
→ "Assign driver optimally considering: distance, rating, vehicle type, current orders"
→ "Recalculate route if driver deviates or traffic changes" (real-time)
→ "Show customer exactly where driver is on map" (4 second updates)
→ "Handle driver going offline mid-delivery gracefully"
→ "Track every status change for audit trail" (legal requirement)
→ "Predict delivery time accurately" (<5 min error)
→ "Scale to 100 cities with regional data isolation"
```

This combination of **geospatial queries at millisecond scale + massive location update throughput + complex routing + regional sharding + event audit trail** is what forces this multi-database architecture.

---

## Why PostgreSQL for Orders (Sharded by Region)?

### What Order Data Actually Needs

```
AN ORDER CONTAINS:
════════════════════════════════════════════════════════
{
  "order_id": "ord_abc123",
  "customer_id": "user_456",
  "restaurant_id": "rest_789",
  "customer_location": {
    "lat": 37.7749,
    "lng": -122.4194,
    "address": "123 Market St, San Francisco, CA"
  },
  "restaurant_location": {
    "lat": 37.7849,
    "lng": -122.4094,
    "address": "456 Mission St, San Francisco, CA"
  },
  "items": [
    {"name": "Burger", "quantity": 2, "price": 12.99},
    {"name": "Fries", "quantity": 1, "price": 4.99}
  ],
  "total": 30.97,
  "status": "searching_driver",
  "assigned_driver_id": null,
  "created_at": "2024-02-26T10:00:00Z",
  "estimated_delivery": "2024-02-26T10:45:00Z",
  "payment_status": "paid",
  "delivery_fee": 3.99,
  "tip": 5.00
}
```

### Why PostgreSQL for This?

```
ORDER DATA CHARACTERISTICS:
════════════════════════════════════════════════════════

1. Complex transactional requirements:
────────────────────────────────────────────────
When customer places order, must atomically:
- Charge customer's card
- Create order record
- Decrement restaurant inventory
- Calculate delivery fee
- Update customer's order history
- Reserve driver capacity (if driver assigned)

BEGIN TRANSACTION;
  -- Charge card
  INSERT INTO payments (order_id, amount, status)
  VALUES ('ord_abc', 30.97, 'processing');
  
  -- Create order
  INSERT INTO orders (order_id, customer_id, total, status)
  VALUES ('ord_abc', 'user_456', 30.97, 'pending');
  
  -- Update inventory
  UPDATE restaurant_inventory
  SET quantity = quantity - 2
  WHERE item_id = 'burger' AND restaurant_id = 'rest_789';
  
  -- Check driver availability
  SELECT driver_id FROM drivers
  WHERE status = 'available' AND region = 'SF'
  FOR UPDATE;  -- lock row
  
COMMIT;

If ANY step fails: ALL rolled back, no half-completed order
This is ACID, only relational databases guarantee this


2. Complex queries for business logic:
────────────────────────────────────────────────
"Show customer all orders in last 30 days with status breakdown"

SELECT 
  DATE(created_at) as order_date,
  COUNT(*) as total_orders,
  SUM(CASE WHEN status = 'delivered' THEN 1 ELSE 0 END) as delivered,
  SUM(CASE WHEN status = 'cancelled' THEN 1 ELSE 0 END) as cancelled,
  AVG(total) as avg_order_value,
  AVG(EXTRACT(EPOCH FROM (delivered_at - created_at))/60) as avg_delivery_time_mins
FROM orders
WHERE customer_id = 'user_456'
AND created_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(created_at)
ORDER BY order_date DESC;

→ Natural SQL
→ Aggregations built-in
→ Date functions native
→ Runs in <50ms with indexes

MongoDB/Redis/Cassandra:
→ Cannot express this query naturally
→ Must do in application code (slow, complex)
→ No aggregation pipeline or limited


3. Referential integrity (data consistency):
────────────────────────────────────────────────
Order references:
- Customer (must exist)
- Restaurant (must exist)
- Driver (must exist, if assigned)
- Payment (must exist)

FOREIGN KEY constraints enforce this:
CREATE TABLE orders (
  order_id UUID PRIMARY KEY,
  customer_id UUID REFERENCES customers(customer_id),
  restaurant_id UUID REFERENCES restaurants(restaurant_id),
  driver_id UUID REFERENCES drivers(driver_id),
  payment_id UUID REFERENCES payments(payment_id)
);

Prevents:
→ Order with non-existent customer
→ Order with deleted restaurant
→ Orphaned records

NoSQL databases: No foreign keys, manual enforcement
```

### Why Shard by Region?

```
GLOBAL ORDERS TABLE (NAIVE):
════════════════════════════════════════════════════════

Single PostgreSQL instance:
orders table: 1 billion rows (all cities worldwide)

Query: "Find active orders in San Francisco"
→ Must scan or index scan ALL cities
→ Even with city index, data scattered globally
→ Slow (100ms+)
→ Single database handles ALL cities = bottleneck


SHARDED BY REGION:
════════════════════════════════════════════════════════

Region: san_francisco
  Database: orders_sf
  Contains: Only SF orders
  
Region: new_york
  Database: orders_ny
  Contains: Only NY orders
  
Region: london
  Database: orders_london
  Contains: Only London orders

Query: "Find active orders in San Francisco"
→ Route to orders_sf database
→ Only contains SF orders (100K rows vs 1B rows)
→ Much faster (5ms)
→ Each region independent
→ Regional failure doesn't affect other regions


SHARDING STRATEGY:
════════════════════════════════════════════════════════

Shard key: restaurant_location.region

Why restaurant not customer?
────────────────────────────────────────────────
Customer in SF orders from SF restaurant:
→ Both in same region
→ Assigned driver also in SF
→ All data stays in SF shard
→ No cross-shard queries

Customer in SF orders from NYC restaurant:
→ Impossible (too far for delivery)
→ App only shows nearby restaurants
→ Natural locality

Result: 99.9% of queries hit single shard
```

---

## Why Redis Geospatial for Driver Locations?

### The Real-Time Location Problem

```
REQUIREMENTS:
════════════════════════════════════════════════════════

200,000 active drivers
Each sends GPS update every 4 seconds
200,000 / 4 = 50,000 location updates PER SECOND

Most critical query:
"Find all available drivers within 2 miles of restaurant X"
Must complete in <200ms (part of driver assignment flow)

This query runs:
- Every time new order is placed (10,000/minute = 167/second)
- Every time assigned driver becomes unavailable (retry assignment)
- Every time customer changes order (re-assign closer driver)
```

### Why PostgreSQL CANNOT Handle This

```
TRYING TO USE POSTGRESQL:
════════════════════════════════════════════════════════

Drivers_Location table:
driver_id │ latitude  │ longitude │ updated_at
──────────────────────────────────────────────────
drv_001   │ 37.7749   │ -122.4194 │ 2024-02-26 10:00:00
drv_002   │ 37.7849   │ -122.4094 │ 2024-02-26 10:00:01
...

Query: "Find drivers within 2 miles of (37.7749, -122.4194)"

SELECT driver_id,
       latitude,
       longitude,
       (
         3959 * acos(
           cos(radians(37.7749)) * 
           cos(radians(latitude)) * 
           cos(radians(longitude) - radians(-122.4194)) + 
           sin(radians(37.7749)) * 
           sin(radians(latitude))
         )
       ) AS distance
FROM drivers_location
WHERE status = 'available'
HAVING distance < 2
ORDER BY distance
LIMIT 10;

Problems:
────────────────────────────────────────────────
→ Must scan ALL 200,000 rows (no efficient spatial index)
→ Haversine formula executed 200,000 times per query
→ Complex trigonometry (slow)
→ Query time: 500ms-2 seconds

At 167 queries/second:
→ 167 × 2 seconds = 334 seconds of DB time per second
→ Need 334 cores just for this query
→ Completely unworkable

PostGIS extension helps but still not fast enough:
→ 50,000 location updates/second overwhelms B-tree index
→ Index maintenance becomes bottleneck
→ Spatial queries still 50-100ms (too slow)
```

### Why Redis Geospatial Is Perfect

```
REDIS GEOSPATIAL:
════════════════════════════════════════════════════════

Redis stores locations using GEOHASH:
GEOADD drivers:locations -122.4194 37.7749 "drv_001"
GEOADD drivers:locations -122.4094 37.7849 "drv_002"

Internally:
Redis converts lat/lng to GEOHASH (base32 string)
37.7749, -122.4194 → "9q8yy"

GEOHASH properties:
────────────────────────────────────────────────
Nearby locations share prefixes:
"9q8yy" (San Francisco)
"9q8yz" (nearby in SF)
"9q8yx" (nearby in SF)
"c2b2q" (New York) ← completely different prefix

Redis stores in sorted set sorted by GEOHASH
Range query on GEOHASH = proximity search!


Query: "Find drivers within 2 miles"
────────────────────────────────────────────────
GEORADIUS drivers:locations 
  -122.4194 37.7749 
  2 mi 
  WITHDIST 
  ASC 
  COUNT 10

Redis internals:
1. Convert center point to GEOHASH: "9q8yy"
2. Calculate GEOHASH range for 2-mile radius: "9q8y" to "9q9z"
3. Range query on sorted set: O(log N + K) where K = results
4. Filter precise distance using haversine (only on K results)
5. Return sorted by distance

Query time: <5ms even at 200,000 drivers

Why so fast?
────────────────────────────────────────────────
✓ In-memory (no disk I/O)
✓ Sorted set optimized for range queries (skip list)
✓ GEOHASH reduces spatial search to 1D range search
✓ Only calculates precise distance for nearby candidates (not all 200K)


Location updates:
────────────────────────────────────────────────
GEOADD drivers:locations -122.4194 37.7749 "drv_001"

Update time: <1ms
50,000 updates/second: handled easily
Redis handles 100K+ commands/second
No index maintenance overhead
```

### Why Not MongoDB Geospatial?

```
MONGODB GEOSPATIAL:
════════════════════════════════════════════════════════

MongoDB has 2dsphere index for geospatial queries:

db.drivers.createIndex({location: "2dsphere"})

db.drivers.find({
  location: {
    $near: {
      $geometry: {type: "Point", coordinates: [-122.4194, 37.7749]},
      $maxDistance: 3218.69  // 2 miles in meters
    }
  }
}).limit(10)

Pros:
✓ Supports geospatial queries
✓ Can include other filters (driver rating, vehicle type)
✓ Persistent storage

Cons:
✗ Query time: 20-50ms (vs Redis 5ms)
✗ Write throughput: ~50K/sec (vs Redis 100K+/sec)
✗ Disk-based (slower than memory)
✗ Complex index maintenance at high update rate
✗ No atomic "update location + filter available" operation

WHEN TO USE MONGODB GEOSPATIAL:
────────────────────────────────────────────────
→ Lower scale (thousands of drivers, not hundreds of thousands)
→ Complex queries (find drivers with rating >4.5 AND sedan AND <2 miles)
→ Need persistence (driver historical location tracking)
→ Can tolerate 50ms latency

WHEN TO USE REDIS GEOSPATIAL:
────────────────────────────────────────────────
→ Ultra high scale (200K+ drivers)
→ Ultra low latency required (<10ms)
→ Simple proximity queries
→ High update frequency (50K+ updates/sec)
→ Ephemeral data (current location, not history)

Production pattern: Use BOTH
Redis: Real-time driver location (current)
MongoDB: Historical driver location (analytics, ML training)
```

---

## Why Graph DB for Route Optimization?

### The Routing Problem

```
BASIC QUESTION:
════════════════════════════════════════════════════════

Driver at point A must:
1. Pick up order at restaurant R
2. Deliver to customer at point C

What's the optimal route considering:
→ Current traffic conditions
→ Road closures
→ Turn restrictions (no left turn at busy intersections)
→ Delivery zones (can't drive through private property)
→ Multi-stop optimization (driver has 3 pending deliveries)
```

### Why Not Just Use Google Maps API?

```
USING GOOGLE MAPS DIRECTLY:
════════════════════════════════════════════════════════

For each driver assignment:
1. Call Google Maps Directions API
   Input: Driver location → Restaurant → Customer
   Output: Route, distance, time
   
Cost per API call: $0.005
At 10,000 orders/minute: 10,000 × $0.005 = $50/minute = $72,000/day
Plus: Google rate limits at 10,000 requests/second
Plus: 100-200ms latency per API call

For route recalculation (traffic changes):
Every 5 minutes for 200K active drivers: 200K × 12/hour × 24 = 57.6M calls/day
Cost: $288,000/day
Total: $360,000/day = $10.8 million/month

Unworkable at scale
```

### Why Graph Database (Neo4j, JanusGraph)?

```
GRAPH DB REPRESENTATION:
════════════════════════════════════════════════════════

Road network as graph:

Nodes: Intersections, addresses
Edges: Road segments

(:Intersection {lat: 37.7749, lng: -122.4194})
-[:ROAD {distance: 0.5, time: 2, traffic: 'heavy'}]->
(:Intersection {lat: 37.7849, lng: -122.4094})

Properties on edges:
- distance: Physical distance (miles)
- base_time: Time without traffic (minutes)
- current_traffic: Real-time traffic multiplier
- restrictions: [no_left_turn, no_trucks, etc.]
- speed_limit: 35 mph


Shortest path query (Dijkstra):
────────────────────────────────────────────────
MATCH (start:Location {id: 'driver_loc'}),
      (end:Location {id: 'restaurant_loc'})
MATCH path = shortestPath(
  (start)-[:ROAD*]-(end)
)
RETURN path, 
       reduce(dist = 0, r in relationships(path) | dist + r.distance) as total_distance,
       reduce(time = 0, r in relationships(path) | time + r.time * r.traffic) as total_time

Query time: 10-50ms (depends on distance)
Cost: Zero (self-hosted)
No rate limits
Full control


Pre-computed delivery zones:
────────────────────────────────────────────────
"Which restaurants can deliver to this address?"

Graph query:
MATCH (customer:Location {id: 'addr_123'}),
      (restaurant:Restaurant)
WHERE exists((restaurant)-[:CAN_DELIVER_TO*1..20]-(customer))
RETURN restaurant

Pre-compute this:
For each customer zone (zip code):
- Find all reachable restaurants within 5 miles / 30 minutes
- Store as materialized relationship
- Update daily (or when traffic patterns change)

Result: Instant lookup (no computation at order time)
```

### Why Not PostgreSQL with pgRouting?

```
POSTGRESQL + pgRouting extension:
════════════════════════════════════════════════════════

Can do routing in PostgreSQL:

SELECT * FROM pgr_dijkstra(
  'SELECT id, source, target, cost FROM edges',
  start_node,
  end_node
);

Pros:
✓ Works for simple routing
✓ One database (no additional system)
✓ ACID guarantees

Cons:
✗ Not optimized for graph traversal (B-tree indexes not ideal)
✗ Complex multi-stop optimization queries are slow
✗ Recursive CTEs for path finding are inefficient
✗ No native graph algorithms (betweenness centrality, etc.)
✗ Updating traffic weights requires table updates (slow)
✗ Cannot handle massive graphs (millions of nodes) efficiently


GRAPH DATABASE:
════════════════════════════════════════════════════════

Pros:
✓ Native graph storage (adjacency lists)
✓ Optimized for traversal (hop to neighbor in O(1))
✓ Rich graph algorithms built-in
✓ Can handle millions of nodes/edges
✓ Real-time weight updates (traffic)
✓ Complex queries (find alternative routes avoiding traffic)

Cons:
✗ Additional system to manage
✗ Learning curve (Cypher query language)
✗ Consistency guarantees weaker than PostgreSQL
✗ Not designed for non-graph data

WHEN TO USE EACH:
────────────────────────────────────────────────
PostgreSQL + pgRouting:
→ Small geographic area (single city)
→ Simple routing (point A to point B)
→ Low query volume (<100/sec)

Graph DB:
→ Large area (entire country)
→ Complex routing (multi-stop, traffic-aware, constraints)
→ High query volume (1000s/sec)
→ Need advanced graph algorithms
```

---

## Complete Schema Architecture

```
POSTGRESQL SCHEMA (sharded by region):
════════════════════════════════════════════════════════

Orders table:
────────────────────────────────────────────────────────────────────────────────
order_id     │ customer_id │ restaurant_id │ driver_id │ status           │ total │ created_at          │ delivered_at
────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
ord_abc123   │ user_456    │ rest_789      │ drv_001   │ delivered        │ 30.97 │ 2024-02-26 10:00:00 │ 2024-02-26 10:42:00
ord_abc124   │ user_457    │ rest_790      │ drv_002   │ in_transit       │ 45.50 │ 2024-02-26 10:05:00 │ NULL
ord_abc125   │ user_458    │ rest_789      │ NULL      │ searching_driver │ 25.00 │ 2024-02-26 10:10:00 │ NULL

Additional columns:
  customer_location:   POINT (PostGIS type)
  restaurant_location: POINT
  delivery_location:   POINT
  pickup_time:         TIMESTAMP
  estimated_delivery:  TIMESTAMP
  actual_delivery:     TIMESTAMP
  distance_miles:      NUMERIC
  delivery_fee:        NUMERIC
  tip:                 NUMERIC
  payment_id:          UUID (foreign key to payments)

Indexes:
  PRIMARY KEY (order_id)
  INDEX (customer_id, created_at DESC)  ← customer order history
  INDEX (driver_id, status)             ← active orders for driver
  INDEX (restaurant_id, status)         ← active orders for restaurant
  INDEX (status, created_at)            ← find pending orders
  GiST INDEX (customer_location)        ← spatial queries (PostGIS)

Partitioning:
  PARTITION BY RANGE (created_at)
  monthly partitions for easy archival


Order_Events table (event sourcing):
────────────────────────────────────────────────────────────────────────────
event_id     │ order_id    │ event_type       │ timestamp           │ data                          │ actor
────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
evt_001      │ ord_abc123  │ ORDER_PLACED     │ 2024-02-26 10:00:00 │ {"items": [...], "total": 30.97} │ customer
evt_002      │ ord_abc123  │ DRIVER_ASSIGNED  │ 2024-02-26 10:02:00 │ {"driver_id": "drv_001"}         │ system
evt_003      │ ord_abc123  │ DRIVER_ARRIVED   │ 2024-02-26 10:15:00 │ {"location": {...}}              │ driver
evt_004      │ ord_abc123  │ ORDER_PICKED_UP  │ 2024-02-26 10:20:00 │ {"pickup_time": "..."}           │ driver
evt_005      │ ord_abc123  │ ORDER_DELIVERED  │ 2024-02-26 10:42:00 │ {"delivered_time": "..."}        │ driver

Purpose:
→ Complete audit trail (legal requirement)
→ Reconstruct order state at any point in time
→ Debugging (what happened when?)
→ Analytics (time between events)


Drivers table:
────────────────────────────────────────────────────────────────────────
driver_id │ name       │ vehicle_type │ rating │ status    │ region │ current_location
────────────────────────────────────────────────────────────────────────────────────────
drv_001   │ John Doe   │ car          │ 4.8    │ busy      │ SF     │ POINT(37.7749, -122.4194)
drv_002   │ Jane Smith │ bike         │ 4.9    │ available │ SF     │ POINT(37.7849, -122.4094)

Note: current_location here is SLOW UPDATE (every 60 seconds)
Real-time location (every 4 seconds) is in Redis


REDIS SCHEMA:
════════════════════════════════════════════════════════

Driver locations (Geospatial):
────────────────────────────────────────────────
Key: drivers:locations:SF  (one per region)
Type: Sorted Set (Geo)

GEOADD drivers:locations:SF -122.4194 37.7749 "drv_001"
GEOADD drivers:locations:SF -122.4094 37.7849 "drv_002"

GEORADIUS drivers:locations:SF -122.4194 37.7749 2 mi WITHDIST


Driver status (fast lookup):
────────────────────────────────────────────────
Key: driver:drv_001:status
Value: "available" | "busy" | "offline"
TTL: 60 seconds (heartbeat)

SET driver:drv_001:status "available" EX 60

If TTL expires: driver auto-marked offline


Driver active orders (prevent over-assignment):
────────────────────────────────────────────────
Key: driver:drv_001:active_orders
Type: SET
Value: [order_id1, order_id2, ...]

SADD driver:drv_001:active_orders "ord_abc123"
SCARD driver:drv_001:active_orders  → current order count

Prevent assigning 5 orders to driver with capacity 2


Order assignment lock (prevent double-assignment):
────────────────────────────────────────────────
Key: order:ord_abc123:assignment_lock
Value: "in_progress"
TTL: 30 seconds

SET order:ord_abc123:assignment_lock "in_progress" NX EX 30

If assignment takes >30s: lock expires, retry allowed


GRAPH DATABASE SCHEMA (Neo4j):
════════════════════════════════════════════════════════

Nodes:
────────────────────────────────────────────────
(:Intersection {
  id: "int_001",
  lat: 37.7749,
  lng: -122.4194,
  name: "Market St & 5th St"
})

(:Restaurant {
  id: "rest_789",
  name: "Best Burgers",
  lat: 37.7849,
  lng: -122.4094,
  delivery_radius_miles: 3
})

(:DeliveryZone {
  id: "zone_downtown_sf",
  name: "Downtown SF",
  polygon: [[...coordinates...]]
})


Relationships:
────────────────────────────────────────────────
(:Intersection)-[:ROAD {
  distance: 0.5,        // miles
  base_time: 2,         // minutes
  traffic_factor: 1.5,  // current traffic (1.0 = no traffic)
  speed_limit: 35,
  restrictions: [],
  road_type: "street",
  updated_at: "2024-02-26 10:00:00"
}]->(:Intersection)

(:Restaurant)-[:CAN_DELIVER_TO {
  travel_time: 15,      // minutes
  distance: 2.5         // miles
}]->(:DeliveryZone)


Queries:
────────────────────────────────────────────────
// Find fastest route with current traffic
MATCH path = shortestPath(
  (start:Intersection {id: $start_id})
  -[:ROAD*]->
  (end:Intersection {id: $end_id})
)
RETURN path,
  reduce(time = 0, r in relationships(path) | 
    time + r.base_time * r.traffic_factor) as total_time

// Check if restaurant can deliver to address
MATCH (r:Restaurant {id: $restaurant_id})
      -[:CAN_DELIVER_TO]->(zone:DeliveryZone)
WHERE point.withinBBox(
  point({latitude: $customer_lat, longitude: $customer_lng}),
  zone.polygon
)
RETURN r
```

---

## Complete Database Flow

```
FLOW 1: Customer Places Order
════════════════════════════════════════════════════════

Customer submits order from app
        │
        ▼
STEP 1: Validate delivery address with Graph DB
────────────────────────────────────────────────
Query Neo4j:
MATCH (r:Restaurant {id: 'rest_789'})
      -[:CAN_DELIVER_TO]->(zone:DeliveryZone)
WHERE point.withinBBox(
  point({latitude: 37.7749, longitude: -122.4194}),
  zone.polygon
)
RETURN r

Result: Restaurant can deliver to this address ✓


STEP 2: Create order in PostgreSQL (transaction)
────────────────────────────────────────────────
BEGIN TRANSACTION;

-- Create order
INSERT INTO orders (order_id, customer_id, restaurant_id, status, ...)
VALUES ('ord_abc123', 'user_456', 'rest_789', 'searching_driver', ...);

-- Create event
INSERT INTO order_events (event_id, order_id, event_type, data, actor)
VALUES ('evt_001', 'ord_abc123', 'ORDER_PLACED', '{"items": [...]}', 'customer');

-- Charge customer
INSERT INTO payments (payment_id, order_id, amount, status)
VALUES ('pay_001', 'ord_abc123', 30.97, 'completed');

COMMIT;


STEP 3: Publish event to Kafka
────────────────────────────────────────────────
Producer.send(
  topic="order_events",
  key=order_id,
  value={
    "event_type": "ORDER_PLACED",
    "order_id": "ord_abc123",
    "restaurant_location": {"lat": 37.7849, "lng": -122.4094},
    "customer_location": {"lat": 37.7749, "lng": -122.4194},
    "timestamp": "2024-02-26T10:00:00Z"
  }
)

Kafka ensures event durability
Multiple consumers can process this event


STEP 4: Driver assignment service (Kafka consumer)
────────────────────────────────────────────────
Consumer receives ORDER_PLACED event
        │
        ▼
A. Acquire assignment lock in Redis
────────────────────────────────────────────────
SET order:ord_abc123:assignment_lock "in_progress" NX EX 30

Result: 1 (lock acquired) → proceed
Result: 0 (already being assigned) → skip


B. Find nearby available drivers (Redis Geospatial)
────────────────────────────────────────────────
GEORADIUS drivers:locations:SF 
  -122.4094 37.7849   ← restaurant location
  5 mi                ← search radius
  WITHDIST            ← include distance
  ASC                 ← nearest first
  COUNT 20            ← get 20 candidates

Returns:
[
  {"driver_id": "drv_001", "distance": 0.5},
  {"driver_id": "drv_002", "distance": 0.8},
  {"driver_id": "drv_003", "distance": 1.2},
  ...
]

Query time: ~3ms


C. Filter available drivers
────────────────────────────────────────────────
For each candidate driver:

GET driver:drv_001:status  → "available" ✓
SCARD driver:drv_001:active_orders → 1 (has capacity) ✓

GET driver:drv_002:status  → "busy" ✗ (skip)

GET driver:drv_003:status  → "available" ✓
SCARD driver:drv_003:active_orders → 0 ✓

Available drivers: [drv_001, drv_003]


D. Score drivers (considering multiple factors)
────────────────────────────────────────────────
SELECT driver_id, rating, vehicle_type, acceptance_rate
FROM drivers
WHERE driver_id IN ('drv_001', 'drv_003')

Score = (proximity_score * 0.5) + (rating * 0.3) + (acceptance_rate * 0.2)

drv_001: (0.5 mi → 10 points) * 0.5 + (4.8 * 2) * 0.3 + (0.95 * 10) * 0.2
       = 5.0 + 2.88 + 1.9 = 9.78

drv_003: (1.2 mi → 8 points) * 0.5 + (4.9 * 2) * 0.3 + (0.90 * 10) * 0.2
       = 4.0 + 2.94 + 1.8 = 8.74

Best match: drv_001


E. Assign driver atomically
────────────────────────────────────────────────
PostgreSQL transaction:
BEGIN;
  UPDATE orders
  SET assigned_driver_id = 'drv_001',
      status = 'driver_assigned'
  WHERE order_id = 'ord_abc123'
  AND status = 'searching_driver';  ← prevent double-assignment

  INSERT INTO order_events (...)
  VALUES (..., 'DRIVER_ASSIGNED', ...);
COMMIT;

Redis updates:
SADD driver:drv_001:active_orders "ord_abc123"
SET driver:drv_001:status "busy" EX 60
DEL order:ord_abc123:assignment_lock

Kafka event:
PUBLISH order_events "DRIVER_ASSIGNED"


F. Notify driver via push notification
────────────────────────────────────────────────
FCM/APNS push to driver's device:
"New order! Best Burgers → 123 Market St. $4.50 expected"
```

```
FLOW 2: Driver Accepts & Navigates
════════════════════════════════════════════════════════

Driver taps "Accept Order"
        │
        ▼
STEP 1: Calculate optimal route (Graph DB)
────────────────────────────────────────────
Driver current location: (37.7749, -122.4194)
Restaurant location:     (37.7849, -122.4094)
Customer location:       (37.7649, -122.4294)

Neo4j query:
MATCH (driver:Intersection {id: 'int_nearest_to_driver'}),
      (restaurant:Intersection {id: 'int_nearest_to_restaurant'}),
      (customer:Intersection {id: 'int_nearest_to_customer'})

// Route: Driver → Restaurant
MATCH path1 = shortestPath(
  (driver)-[:ROAD*]->(restaurant)
)

// Route: Restaurant → Customer
MATCH path2 = shortestPath(
  (restaurant)-[:ROAD*]->(customer)
)

RETURN path1, path2,
  reduce(time = 0, r in relationships(path1) | 
    time + r.base_time * r.traffic_factor) as leg1_time,
  reduce(time = 0, r in relationships(path2) | 
    time + r.base_time * r.traffic_factor) as leg2_time

Returns:
Leg 1 (Driver → Restaurant): 5 minutes
Leg 2 (Restaurant → Customer): 12 minutes
Total ETA: 17 minutes

Send turn-by-turn directions to driver app


STEP 2: Real-time location tracking
────────────────────────────────────────────────
Every 4 seconds, driver app sends GPS update:

POST /api/driver/location
{
  "driver_id": "drv_001",
  "lat": 37.7759,
  "lng": -122.4184,
  "timestamp": 1708945210
}

Backend updates Redis:
GEOADD drivers:locations:SF -122.4184 37.7759 "drv_001"

Set refresh: EX 60 (if driver stops sending, marked offline)


STEP 3: Broadcast location to customer (WebSocket)
────────────────────────────────────────────────
WebSocket connection: Customer ↔ Server

Server: SUBSCRIBE order:ord_abc123:driver_location (Redis Pub/Sub)

Every 4 seconds:
PUBLISH order:ord_abc123:driver_location '{
  "lat": 37.7759,
  "lng": -122.4184,
  "timestamp": 1708945210
}'

Customer's map updates in real-time
Shows driver moving toward restaurant


STEP 4: Driver arrives at restaurant
────────────────────────────────────────────────
Driver taps "Arrived at Restaurant"

PostgreSQL:
UPDATE orders
SET status = 'driver_at_restaurant',
    pickup_time = NOW()
WHERE order_id = 'ord_abc123';

INSERT INTO order_events (...)
VALUES (..., 'DRIVER_ARRIVED_RESTAURANT', ...);

Kafka event: DRIVER_ARRIVED_RESTAURANT

Customer notified: "Driver is picking up your order"


STEP 5: Route recalculation (traffic change)
────────────────────────────────────────────────
Background service polls traffic API every 5 minutes
Updates Graph DB:

MATCH ()-[r:ROAD {id: 'road_123'}]->()
SET r.traffic_factor = 2.0  ← heavy traffic now

Recalculate route for active orders:
Query Neo4j with new traffic_factor
If new route is faster:
→ Send updated directions to driver
→ Update customer ETA
```

```
FLOW 3: Order Delivered
════════════════════════════════════════════════════════

Driver taps "Order Delivered"
        │
        ▼
STEP 1: Update PostgreSQL
────────────────────────────────────────────────
BEGIN TRANSACTION;

UPDATE orders
SET status = 'delivered',
    delivered_at = NOW(),
    actual_delivery_time = NOW() - pickup_time
WHERE order_id = 'ord_abc123';

INSERT INTO order_events (...)
VALUES (..., 'ORDER_DELIVERED', ...);

-- Remove from driver's active orders
-- (happens in Redis, but record here for audit)

COMMIT;


STEP 2: Update Redis
────────────────────────────────────────────────
SREM driver:drv_001:active_orders "ord_abc123"
SET driver:drv_001:status "available" EX 60

Driver now available for new orders


STEP 3: Publish to Kafka
────────────────────────────────────────────────
Producer.send(
  topic="order_events",
  key="ord_abc123",
  value={
    "event_type": "ORDER_DELIVERED",
    "order_id": "ord_abc123",
    "delivered_at": "2024-02-26T10:42:00Z",
    "delivery_time_minutes": 22
  }
)


STEP 4: Trigger downstream systems (Kafka consumers)
────────────────────────────────────────────────
Consumer 1: Payment finalization
→ Release tip to driver
→ Calculate platform fee

Consumer 2: Rating system
→ Send push: "Rate your delivery"

Consumer 3: Analytics
→ Update driver performance metrics
→ Update restaurant fulfillment time

Consumer 4: ML model training
→ Log: predicted ETA vs actual delivery time
→ Improve future predictions
```

```
FLOW 4: Handling Failures
════════════════════════════════════════════════════════

Scenario: Driver becomes unavailable mid-delivery
        │
        ▼
STEP 1: Driver's heartbeat stops
────────────────────────────────────────────────
Last heartbeat: 10:15:00
Current time: 10:16:30
TTL expired (60 seconds)

Redis:
GET driver:drv_001:status → NULL (expired)

Monitoring service detects:
Driver drv_001 offline with active order ord_abc123


STEP 2: Automatic reassignment
────────────────────────────────────────────────
Fetch driver's active orders:
SMEMBERS driver:drv_001:active_orders
→ Returns: ["ord_abc123"]

For each order:
PostgreSQL:
UPDATE orders
SET assigned_driver_id = NULL,
    status = 'searching_driver_again'
WHERE order_id = 'ord_abc123';

Kafka:
PUBLISH order_events "DRIVER_UNAVAILABLE"


STEP 3: Re-trigger assignment flow
────────────────────────────────────────────────
Assignment service receives DRIVER_UNAVAILABLE event
Runs same flow as FLOW 1, STEP 4
Finds new driver
Assigns atomically

Customer notified:
"Your original driver became unavailable.
 New driver assigned: Jane Smith, ETA 8 minutes"
```

---

## Tradeoffs vs Other Databases

```
┌──────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                          │ THIS ARCH    │ MONGO ONLY   │ POSTGRES ALL │ CASSANDRA    │
├──────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Order transactions       │ PostgreSQL✓  │ Limited      │ PostgreSQL✓  │ NO ✗         │
│ Geospatial queries <10ms │ Redis ✓      │ 20-50ms      │ 50-100ms     │ 100ms+       │
│ Location update          │ 50K/sec ✓    │ 50K/sec ✓    │ 10K/sec ✗    │ 100K/sec ✓   │
│ Graph routing            │ Neo4j ✓      │ Manual ✗     │ pgRouting    │ NO ✗         │
│ Complex order queries    │ PostgreSQL✓  │ Aggregation  │ PostgreSQL✓  │ Limited ✗    │
│ Regional sharding        │ Native ✓     │ Native ✓     │ Manual       │ Native ✓     │
│ Event sourcing           │ PostgreSQL✓  │ MongoDB ✓    │ PostgreSQL✓  │ Good ✓       │
│ Operational complexity   │ HIGH         │ MEDIUM       │ LOW          │ HIGH         │
│ Cost at scale            │ MEDIUM       │ HIGH         │ LOW          │ HIGH         │
└──────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> __PostgreSQL stores orders sharded by region because complex transactional requirements (atomically charge customer, create order, update inventory, reserve driver) require ACID guarantees that NoSQL databases cannot provide, and regional sharding ensures 99.9% of queries hit a single shard since customers only order from nearby restaurants naturally isolating data by geography — Redis Geospatial stores real-time driver locations because finding 10 nearest available drivers within 2 miles from 200,000 active drivers must complete in under 10ms to assign drivers within 500ms total, and its GEOHASH-based sorted set gives O(log N + K) range queries while handling 50,000 location updates per second that would overwhelm PostgreSQL's spatial indexes — Graph databases optimize routing because road networks are inherently graphs where finding shortest paths with traffic-aware weights is what graph traversal algorithms (Dijkstra, A_) are designed for, giving 10-50ms route calculations that cost $360K/day via Google Maps API at scale, and pre-computing delivery zones as graph relationships enables instant "can this restaurant deliver here?" lookups without expensive distance calculations — together they enable Uber Eats/DoorDash's magic of assigning the nearest driver in under 500ms, showing real-time driver location updates every 4 seconds, recalculating routes when traffic changes, and maintaining a complete audit trail of every order event for legal compliance and debugging._*