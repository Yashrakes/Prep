# Proximity-Based Services

## Overview
Pattern for efficiently searching and retrieving entities based on geographical location - essential for location-based services like ride-sharing, food delivery, and location search.

## When to Use
- Design Uber - finding nearby drivers
- Design Gopuff/DoorDash - finding nearby stores/restaurants
- Design Yelp - finding nearby businesses
- Location-based social networks
- Real estate search
- Any service requiring "find nearest X" functionality

## Key Insight
Most proximity-based systems don't require global search - users typically search for entities local to them, not worldwide.

## Technology Options

### Database Extensions

#### PostgreSQL with PostGIS
**Description**: Extension that adds geographic objects to PostgreSQL

**Pros**:
- Built on familiar PostgreSQL
- Rich geospatial functions
- ACID compliance
- Good for moderate scale

**Cons**:
- Additional complexity on top of PostgreSQL
- May need sharding at very high scale

**When to Use**: When you already use PostgreSQL and need geospatial capabilities

#### Redis Geospatial
**Description**: Redis data type for storing and querying geo coordinates

**Pros**:
- Very fast in-memory operations
- Simple API (GEOADD, GEORADIUS)
- Easy to integrate

**Cons**:
- In-memory, higher cost at scale
- Less feature-rich than dedicated solutions

**When to Use**: Fast, simple geo queries with moderate data size

### Dedicated Solutions

#### Elasticsearch with Geo-Queries
**Description**: Full-text search engine with powerful geospatial capabilities

**Pros**:
- Excellent for complex queries (geo + text + filters)
- Distributed, horizontally scalable
- Rich query DSL

**Cons**:
- More complex to operate
- Higher resource requirements
- Overkill for simple geo-only queries

**When to Use**: When you need geo + text search + complex filters

## Core Concepts

### Geospatial Indexing
**Approach**: Divide geographical area into manageable regions and index entities within regions

**Benefits**:
- Quickly exclude irrelevant areas
- Reduce search space significantly
- Enable efficient nearest-neighbor queries

**Common Techniques**:
- **Geohashing**: Encode lat/long into string, nearby locations have similar prefixes
- **Quadtrees**: Recursively divide space into four quadrants
- **R-trees**: Tree data structure for spatial access methods
- **S2 Geometry**: Google's library for spherical geometry

### Search Radius
**Approach**: Query entities within X kilometers/miles of given coordinates

**Considerations**:
- Start with reasonable default radius (5-10 km)
- Expand radius if insufficient results
- Set maximum radius to prevent expensive queries
- Consider population density (smaller radius in cities, larger in rural areas)

## Architecture Patterns

### Simple Architecture (< 100K entities)
**Components**:
- Single database with geospatial extension
- Application server performs radius queries
- Simple caching of popular locations

**When to Use**: MVP, low scale, single-region service

### Regional Sharding (100K - 10M entities)
**Components**:
- Shard data by major geographic regions (cities, states, countries)
- Each shard handles its region
- Request router determines which shard(s) to query

**Benefits**:
- Natural data partitioning
- Most queries hit single shard
- Easy to scale per-region

**Challenges**:
- Border queries may need multiple shards
- Uneven distribution if regions vary in size

### Grid-Based Sharding (10M+ entities)
**Components**:
- Divide world into uniform grid cells
- Each cell has responsible shard
- Geohash prefix determines shard

**Benefits**:
- Even distribution
- Predictable sharding
- Easy to add/remove shards

**Challenges**:
- Border queries span multiple cells
- Grid size optimization is critical

## Important Design Decisions

### When NOT to Use Geospatial Index
**Rule of Thumb**: If searching through < 1,000 items, scan all items rather than build specialized index

**Reasoning**:
- Linear scan is fast for small datasets
- Index overhead (memory, maintenance) not worth it
- Simpler implementation

**Use In-Memory Filtering Instead**:
- Load items into memory
- Calculate distances for all
- Sort and return top N

### When to Use Geospatial Index
- Hundreds of thousands to millions of entities
- Real-time query requirements
- Need efficient range queries
- Frequent updates to entity locations

## Scaling Considerations

### Read Scaling
- **Cache**: Popular location queries (city centers, tourist areas)
- **Read Replicas**: Distribute query load
- **CDN**: Static location data (stores, restaurants)

### Write Scaling
- **Batching**: Update locations in batches (driver locations)
- **Write-Through Cache**: Keep cache consistent
- **Async Updates**: Don't block on location updates

### Data Distribution
- **Active Entities**: Keep frequently queried entities in faster storage
- **Historical Data**: Archive old location data
- **Regional Optimization**: Deploy closer to high-density regions

## Query Optimization

### Bounding Box Queries
**Technique**: Instead of radius, query rectangular bounding box first
**Benefit**: Faster than distance calculations
**Post-Processing**: Calculate exact distance for bounding box results

### Two-Phase Queries
1. **Phase 1**: Broad geospatial filter (fast, approximate)
2. **Phase 2**: Refine with additional filters (rating, price, availability)

### Pre-Computed Distances
For static entities (stores, restaurants), pre-compute distances to major landmarks/grid points

## Common Challenges

### Border Cases
**Problem**: Entities near region/shard boundaries
**Solutions**:
- Query adjacent regions for border queries
- Use overlapping regions
- Accept small inconsistency window

### Moving Entities (Drivers, Delivery Personnel)
**Problem**: Locations change frequently
**Solutions**:
- Update location every N seconds (balance freshness vs load)
- Use separate "active entities" data structure
- In-memory storage for active entities, persistent for inactive

### Hot Spots
**Problem**: Popular locations (downtown, tourist areas) get disproportionate traffic
**Solutions**:
- Cache popular queries
- Dedicated replicas for hot regions
- Rate limiting per location

### Accuracy vs Performance
**Trade-off**: Exact distance calculations (Haversine formula) vs approximate (bounding box)
**Recommendation**: Use approximate for filtering, exact for final ranking

## Interview Tips

- Start simple: Database with geospatial extension unless huge scale
- Don't over-engineer: For < 1,000 entities, just scan all
- Clarify scale: Number of entities, query volume, update frequency
- Discuss regional nature: Most queries are local, not global
- Consider moving vs static entities: Different update patterns
- Think about density: Urban vs rural affects radius and distribution
- Address consistency: How fresh do locations need to be?
- Multi-criteria search: Geo + availability + rating + price

## Example Scenarios

### Design Uber
- **Entities**: Millions of active drivers (moving)
- **Solution**: Grid-based sharding, in-memory for active drivers
- **Update Frequency**: Every 4-5 seconds
- **Query**: Find 20 nearest available drivers within 5km

### Design Gopuff
- **Entities**: Thousands of stores (static)
- **Solution**: PostgreSQL + PostGIS, cache popular queries
- **Update Frequency**: Rare (new stores)
- **Query**: Find nearest store with inventory, within 10km

### Design Yelp
- **Entities**: Millions of businesses (static)
- **Solution**: Elasticsearch for geo + text search + filters
- **Update Frequency**: Daily for business updates
- **Query**: Find "coffee shops" near me, rating > 4, open now
