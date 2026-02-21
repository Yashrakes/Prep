
## The Core Problem Ticketmaster Solves

On the surface ticket booking seems simple — sell seats, prevent double booking. But consider the real constraints:

```
Taylor Swift concert announcement:
────────────────────────────────────────────────
Venue capacity:           70,000 seats
Interested fans:          14,000,000+ (200x oversubscribed)
Sale start time:          10:00:00 AM sharp
Concurrent users at 10AM: 2,000,000+ hitting site simultaneously
Request rate:             500,000+ requests/second (peak)
Hold time:                10 minutes (user has 10 min to complete purchase)

Requirements:
→ Never sell same seat to two people (ZERO tolerance)
→ Handle 500K requests/sec without crashing
→ Fair queue (no cutting in line, no bots getting all tickets)
→ Release held seats after 10 minutes if not purchased
→ Show accurate "X seats remaining" in real-time
→ Process payment within hold window
→ Scale from 0 to 2M users in 1 second (flash crowd)
→ Prevent scalpers/bots from buying all tickets
```

This combination of **absolute consistency (no double booking) + massive concurrency spike + timed holds + fairness + bot prevention** is what forces this specific architecture.

---

## Why PostgreSQL with Row-Level Locking?

### The Double Booking Nightmare

```
WITHOUT PROPER LOCKING:
════════════════════════════════════════════════════════

Timeline of disaster:
T+0ms:   User A queries: SELECT * FROM seats WHERE seat_id = 'A1'
         Returns: {seat_id: 'A1', status: 'available'}
         
T+1ms:   User B queries: SELECT * FROM seats WHERE seat_id = 'A1'
         Returns: {seat_id: 'A1', status: 'available'}
         (Both see seat as available!)

T+50ms:  User A executes: UPDATE seats SET status = 'locked' WHERE seat_id = 'A1'
         Seat A1 locked for User A
         
T+51ms:  User B executes: UPDATE seats SET status = 'locked' WHERE seat_id = 'A1'
         Seat A1 locked for User B
         (Overwrites User A's lock!)

T+10min: User A completes payment
T+10min: User B completes payment

RESULT: Seat A1 sold to BOTH User A and User B
        Legal nightmare, refunds, angry customers
        Ticketmaster's reputation destroyed


This is a classic "lost update" problem
Happens with ANY database without proper locking
Not specific to SQL vs NoSQL
```

### PostgreSQL SELECT FOR UPDATE Solution

```
WITH PROPER ROW-LEVEL LOCKING:
════════════════════════════════════════════════════════

User A's transaction:
────────────────────────────────────────────────
BEGIN TRANSACTION;

SELECT * FROM seats
WHERE seat_id = 'A1'
AND status = 'available'
FOR UPDATE;  ← Acquires exclusive row lock

-- Row A1 is now LOCKED at database level
-- No other transaction can read or modify A1 until we COMMIT/ROLLBACK

UPDATE seats
SET status = 'locked',
    locked_by = 'user_A',
    lock_timestamp = NOW()
WHERE seat_id = 'A1';

COMMIT;  ← Lock released


User B's transaction (happens simultaneously):
────────────────────────────────────────────────
BEGIN TRANSACTION;

SELECT * FROM seats
WHERE seat_id = 'A1'
AND status = 'available'
FOR UPDATE;

-- BLOCKS HERE waiting for User A's transaction to complete
-- PostgreSQL queue: User B waits
        │
        ▼ (User A commits)
        
-- Now User B's SELECT executes
-- Returns: NO ROWS (seat_id A1 is now 'locked', not 'available')

ROLLBACK;  -- No seat to lock, abort

User B gets error: "Seat no longer available"
User B tries different seat


RESULT: Only ONE user gets the seat
        No double booking possible
        Database guarantees this
```

### Why This Requires ACID Transactions

```
ACID GUARANTEES NEEDED:
════════════════════════════════════════════════════════

A - Atomicity:
────────────────────────────────────────────────
BEGIN;
  SELECT seat FOR UPDATE;  ← Lock acquired
  UPDATE seat SET locked;  ← Lock applied
  UPDATE inventory count;  ← Remaining seats updated
  INSERT booking record;   ← Booking created
COMMIT;

ALL these operations succeed together
OR ALL fail together
No partial state (seat locked but no booking record)


C - Consistency:
────────────────────────────────────────────────
Database enforces:
- Foreign keys (booking → user_id exists)
- Check constraints (status IN ('available', 'locked', 'sold'))
- Unique constraints (seat_id unique per event)

Cannot violate these even under concurrency


I - Isolation:
────────────────────────────────────────────────
User A's transaction is ISOLATED from User B's
User A sees consistent snapshot
User B sees consistent snapshot
They cannot interfere with each other's work


D - Durability:
────────────────────────────────────────────────
Once COMMIT succeeds, data is PERMANENT
Even if server crashes 1ms later
User's seat lock is GUARANTEED to be recorded
Cannot be lost
```

---

## Why NOT NoSQL Databases Here?

### MongoDB's Weakness: No Row-Level Locking

```
MONGODB ATTEMPT:
════════════════════════════════════════════════════════

MongoDB has document-level locks (since v4.0):

db.seats.findOneAndUpdate(
  {seat_id: 'A1', status: 'available'},
  {$set: {status: 'locked', locked_by: 'user_A'}},
  {returnNewDocument: true}
)

Looks atomic, but problems:
────────────────────────────────────────────────
Problem 1: No SELECT FOR UPDATE equivalent
→ Cannot "lock then check then update"
→ Must update optimistically
→ Race conditions still possible in complex workflows

Problem 2: No multi-document transactions (pre v4.0)
→ Lock seat + update inventory + create booking
→ These were THREE separate operations
→ Could fail between steps → inconsistent state

Problem 3: Weaker isolation guarantees
→ MongoDB default: read uncommitted
→ Can see data mid-transaction
→ Not suitable for financial transactions


When would MongoDB work?
────────────────────────────────────────────────
→ If you only lock ONE seat at a time (single doc)
→ If you tolerate eventual consistency
→ If double booking is recoverable (not in ticketing!)

For Ticketmaster: PostgreSQL ACID is non-negotiable
```

### Cassandra's Weakness: No Locking At All

```
CASSANDRA ATTEMPT:
════════════════════════════════════════════════════════

Cassandra has lightweight transactions (LWT):

UPDATE seats
SET status = 'locked', locked_by = 'user_A'
WHERE seat_id = 'A1'
IF status = 'available';

This uses Paxos consensus (slow!)

Problems:
────────────────────────────────────────────────
Problem 1: Extremely slow under contention
→ 2M users trying to lock same 70K seats
→ Paxos requires multiple rounds of communication
→ Latency: 50-500ms per operation (vs PostgreSQL 1-5ms)
→ At 500K requests/sec, Cassandra overwhelmed

Problem 2: No transactions
→ Lock seat + update inventory + create booking
→ Three separate Paxos operations
→ No atomicity across operations

Problem 3: Tunable consistency is a trap
→ "I'll just use QUORUM for strong consistency"
→ Still no transactions
→ Still no row-level locking
→ Consistency ≠ Correctness


When would Cassandra work?
────────────────────────────────────────────────
→ For inventory that's NOT unique (sellable goods)
→ When eventual consistency is acceptable
→ When you have millions of identical items
→ Not for unique seats with zero double-booking tolerance
```

### DynamoDB's Weakness: Conditional Writes Are Not Enough

```
DYNAMODB ATTEMPT:
════════════════════════════════════════════════════════

DynamoDB has conditional updates:

UpdateItem(
  TableName: 'Seats',
  Key: {seat_id: 'A1'},
  UpdateExpression: 'SET #status = :locked, locked_by = :user_a',
  ConditionExpression: '#status = :available',
  ExpressionAttributeNames: {'#status': 'status'},
  ExpressionAttributeValues: {
    ':available': 'available',
    ':locked': 'locked',
    ':user_a': 'user_A'
  }
)

Works for single-item updates

Problems:
────────────────────────────────────────────────
Problem 1: Transactions limited to 100 items
→ User wants to book 150 seats for company event
→ Cannot do atomically in DynamoDB
→ Must break into chunks → partial failures

Problem 2: No SELECT FOR UPDATE equivalent
→ Cannot "lock and hold" while user enters payment info
→ Must implement external locking (e.g., in Redis)
→ Now you have distributed transaction problem

Problem 3: Cost explosion under contention
→ Conditional write failures still consume capacity units
→ 2M users racing for 70K seats = millions of failures
→ Each failure costs $$$
→ Bill: $10,000+/hour during sale


When would DynamoDB work?
────────────────────────────────────────────────
→ For inventory-based systems (quantity, not unique items)
→ For systems with natural partition keys
→ When AWS-native stack is priority
→ Not for unique seat assignments with ACID requirements
```

---

## Why Redis for Queue Management?

### The Thundering Herd Problem

```
WITHOUT QUEUE:
════════════════════════════════════════════════════════

Sale opens 10:00:00 AM
        │
        ▼
2,000,000 users hit "Buy Tickets" simultaneously
        │
        ▼
All 2M requests hit PostgreSQL:
BEGIN;
SELECT * FROM seats WHERE status = 'available' FOR UPDATE;

PostgreSQL connection limit: 1,000 connections
Remaining 1,999,000 requests:
→ Connection pool exhausted
→ Timeout errors
→ Retry storms (each user retries 5x)
→ 10M requests queued
→ Database crashes
→ Site goes down
→ Tickets never sell
→ Ticketmaster disaster


Even with infinite PostgreSQL capacity:
────────────────────────────────────────────────
2M concurrent SELECT FOR UPDATE on same 70K seats
→ Lock contention
→ Deadlocks (circular wait)
→ Queries timeout after 30s
→ Users get errors: "Database timeout"
→ Awful user experience
```

### Redis Virtual Queue Solution

```
WITH REDIS QUEUE:
════════════════════════════════════════════════════════

STEP 1: Users join virtual queue
────────────────────────────────────────────────
User arrives at 9:59:58 AM (2 seconds before sale)

POST /queue/join
{
  user_id: 'user_001',
  event_id: 'taylor_swift_2024'
}

Backend:
ZADD queue:taylor_swift_2024 
     1708945198000 
     'user_001'
     ^^^^ timestamp (score)
     
User assigned position in queue based on arrival time
Position shown to user: "You are #543,291 in queue"


STEP 2: Randomize at exactly 10:00:00 AM (fairness)
────────────────────────────────────────────────
At 10:00:00.000 AM sharp:

-- Fetch all users who joined before sale start
ZRANGEBYSCORE queue:taylor_swift_2024 0 1708945200000

Returns: 2,000,000 user IDs

-- Shuffle randomly (fair lottery)
random_shuffle(user_ids)

-- Reassign queue positions
For each user in shuffled order:
  ZADD queue:taylor_swift_2024:active
       <new_position_score>
       user_id

Result: Queue order is now RANDOM
No advantage to arriving first (prevents bot advantage)
Everyone has fair chance


STEP 3: Process queue at controlled rate
────────────────────────────────────────────────
Background worker (rate-limited):

Every 1 second:
  -- Admit 100 users from queue to booking system
  users = ZPOPMIN queue:taylor_swift_2024:active 100
  
  For each user:
    -- Create session token
    SET session:user_001 "active" EX 600  (10 min TTL)
    
    -- Send notification
    PUBLISH user:user_001 "Your turn! You have 10 minutes"
    
    -- Redirect to seat selection page

Controlled rate: 100 users/second = 6,000 users/minute
Prevents PostgreSQL overload
Each user has 10 minutes → max 60,000 concurrent users
PostgreSQL can handle 60K concurrent (vs 2M impossible)
```

---

## Complete Schema Architecture

```
POSTGRESQL SCHEMA:
════════════════════════════════════════════════════════

Events table:
────────────────────────────────────────────────────────
event_id   │ name                 │ venue_id │ date                │ total_seats │ seats_available
───────────────────────────────────────────────────────────────────────────────────────────────────
evt_001    │ Taylor Swift 2024    │ venue_1  │ 2024-08-15 19:00:00 │ 70000       │ 70000
evt_002    │ Coldplay 2024        │ venue_2  │ 2024-09-20 20:00:00 │ 55000       │ 55000

Indexes:
  PRIMARY KEY (event_id)
  INDEX (venue_id, date)


Seats table (the critical table):
────────────────────────────────────────────────────────────────────────────────
seat_id    │ event_id │ section │ row │ number │ price │ status    │ locked_by │ lock_timestamp      │ sold_to
───────────────────────────────────────────────────────────────────────────────────────────────────────────────
seat_001   │ evt_001  │ A       │ 1   │ 1      │ 500   │ available │ NULL      │ NULL                │ NULL
seat_002   │ evt_001  │ A       │ 1   │ 2      │ 500   │ locked    │ user_001  │ 2024-02-26 10:05:00 │ NULL
seat_003   │ evt_001  │ A       │ 1   │ 3      │ 500   │ sold      │ NULL      │ NULL                │ user_002

status enum: 'available', 'locked', 'sold'

Indexes:
  PRIMARY KEY (seat_id)
  INDEX (event_id, status)  ← find available seats
  INDEX (locked_by)         ← find user's locked seats
  INDEX (lock_timestamp)    ← cleanup expired locks

Constraints:
  CHECK (status IN ('available', 'locked', 'sold'))
  CHECK (status = 'locked' OR locked_by IS NULL)  ← consistency
  CHECK (status = 'sold' OR sold_to IS NULL)


Bookings table:
────────────────────────────────────────────────────────────────────────────────
booking_id │ user_id  │ event_id │ seat_ids            │ total_price │ status    │ created_at          │ expires_at
───────────────────────────────────────────────────────────────────────────────────────────────────────────────────
book_001   │ user_001 │ evt_001  │ [seat_002, seat_003]│ 1000        │ pending   │ 2024-02-26 10:05:00 │ 2024-02-26 10:15:00
book_002   │ user_002 │ evt_001  │ [seat_004]          │ 500         │ confirmed │ 2024-02-26 10:06:00 │ NULL

status enum: 'pending', 'confirmed', 'cancelled', 'expired'

Indexes:
  PRIMARY KEY (booking_id)
  INDEX (user_id, status)
  INDEX (expires_at) ← cleanup expired bookings
  
Foreign keys:
  FOREIGN KEY (user_id) REFERENCES users(user_id)
  FOREIGN KEY (event_id) REFERENCES events(event_id)


Payments table:
────────────────────────────────────────────────────────────────────────────────
payment_id │ booking_id │ amount │ status    │ payment_method │ transaction_id │ created_at
───────────────────────────────────────────────────────────────────────────────────────────
pay_001    │ book_001   │ 1000   │ completed │ credit_card    │ stripe_xyz123  │ 2024-02-26 10:07:00
pay_002    │ book_002   │ 500    │ completed │ credit_card    │ stripe_abc456  │ 2024-02-26 10:08:00

Foreign key:
  FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)


REDIS SCHEMA:
════════════════════════════════════════════════════════

Virtual queue (before tickets available):
────────────────────────────────────────────────
Key:  queue:evt_001:waiting
Type: Sorted Set
Score: Unix timestamp (microsecond precision for fairness)

ZADD queue:evt_001:waiting 1708945198000000 "user_001"
ZADD queue:evt_001:waiting 1708945198000001 "user_002"
ZADD queue:evt_001:waiting 1708945198000002 "user_003"


Active queue (tickets on sale, being processed):
────────────────────────────────────────────────
Key:  queue:evt_001:active
Type: Sorted Set
Score: Position in queue (after randomization)

ZADD queue:evt_001:active 1 "user_543"
ZADD queue:evt_001:active 2 "user_129"
ZADD queue:evt_001:active 3 "user_891"


User session (10 minute window to complete purchase):
────────────────────────────────────────────────
Key:   session:user_001
Value: "active"
TTL:   600 seconds (10 minutes)

SET session:user_001 "active" EX 600

When TTL expires: session automatically deleted
User kicked out, must rejoin queue


Token bucket (rate limiting per user):
────────────────────────────────────────────────
Key:   ratelimit:user_001:seat_lock_attempts
Value: 5  (max attempts per minute)
TTL:   60 seconds

DECR ratelimit:user_001:seat_lock_attempts

If value reaches 0: reject further attempts
Prevents bot from trying 1000 seats/second


Available seat count (cached for fast display):
────────────────────────────────────────────────
Key:   seats:available:evt_001
Value: 45327

DECR seats:available:evt_001  (when seat locked)
INCR seats:available:evt_001  (when lock expires)

Show to users: "45,327 tickets remaining"
Updated in real-time


User's locked seats (for quick lookup):
────────────────────────────────────────────────
Key:  user:locks:user_001
Type: SET
Value: {seat_002, seat_003}
TTL:  600 seconds

SADD user:locks:user_001 "seat_002"
SMEMBERS user:locks:user_001  → user's current locks

When TTL expires: locks automatically released
```

---

## Complete Database Flow

```
FLOW 1: User Enters Queue (Before Sale Starts)
════════════════════════════════════════════════════════

User visits ticketmaster.com at 9:55 AM (5 min before sale)
        │
        ▼
POST /api/queue/join
{
  user_id: 'user_001',
  event_id: 'evt_001'
}
        │
        ▼
Backend adds to Redis waiting queue:
────────────────────────────────────────────────
ZADD queue:evt_001:waiting 
     1708945198000000 
     'user_001'

Response to user:
{
  position: 543291,
  message: "You are #543,291 in queue. Sale starts at 10:00 AM."
}

User sees: "You are in the waiting room. Position: #543,291"
Page auto-refreshes every 10 seconds to show updated position
```

```
FLOW 2: Sale Starts - Queue Randomization
════════════════════════════════════════════════════════

Clock strikes 10:00:00.000 AM
        │
        ▼
Queue manager service (triggered by cron):
────────────────────────────────────────────────
STEP 1: Fetch all waiting users
ZRANGEBYSCORE queue:evt_001:waiting 0 1708945200000
→ Returns: 2,000,000 user IDs

STEP 2: Shuffle randomly
random.shuffle(user_ids)

STEP 3: Create active queue with random order
For i, user_id in enumerate(shuffled_user_ids):
  ZADD queue:evt_001:active i user_id

STEP 4: Delete waiting queue
DEL queue:evt_001:waiting

STEP 5: Notify users of their final position
For each user:
  PUBLISH notification:user_id "Your position: #12345"

Users see updated positions on their screens
```

```
FLOW 3: User Admitted From Queue
════════════════════════════════════════════════════════

Queue processor (runs continuously):
────────────────────────────────────────────────
Every 1 second:

-- Admit next 100 users
users = ZPOPMIN queue:evt_001:active 100

For each user in users:
  -- Create 10-minute session
  SET session:{user_id} "active" EX 600
  
  -- Create rate limit bucket
  SET ratelimit:{user_id}:seat_lock_attempts 10 EX 60
  
  -- Send push notification
  PUBLISH notification:{user_id} '{
    "type": "YOUR_TURN",
    "message": "Select your seats now! 10 minutes remaining",
    "expires_at": "2024-02-26T10:10:00Z"
  }'
  
  -- Record admission for analytics
  LPUSH admitted:evt_001 {user_id}

User's browser receives WebSocket message
Redirects to seat selection page automatically
Timer starts: "9:59 remaining"
```

```
FLOW 4: User Selects and Locks Seats
════════════════════════════════════════════════════════

User clicks on seat A-1-1 and A-1-2 (wants 2 seats together)
        │
        ▼
POST /api/seats/lock
{
  user_id: 'user_001',
  event_id: 'evt_001',
  seat_ids: ['seat_001', 'seat_002']
}
        │
        ▼
STEP 1: Validate session exists
────────────────────────────────────────────────
GET session:user_001
→ Returns: "active" ✓

If NULL: reject with "Session expired, rejoin queue"


STEP 2: Check rate limit
────────────────────────────────────────────────
DECR ratelimit:user_001:seat_lock_attempts
→ Returns: 9 (still have attempts)

If 0: reject with "Too many attempts, please slow down"


STEP 3: Acquire seats with SELECT FOR UPDATE
────────────────────────────────────────────────
BEGIN TRANSACTION;

-- Lock rows for exclusive access
SELECT seat_id, status
FROM seats
WHERE seat_id IN ('seat_001', 'seat_002')
AND event_id = 'evt_001'
FOR UPDATE;

-- Rows are now LOCKED (no other transaction can touch them)

Result:
seat_001: status = 'available' ✓
seat_002: status = 'available' ✓

If any seat is NOT available:
  ROLLBACK;
  Return error: "One or more seats no longer available"
  
-- Update seat status
UPDATE seats
SET status = 'locked',
    locked_by = 'user_001',
    lock_timestamp = NOW()
WHERE seat_id IN ('seat_001', 'seat_002');

-- Update event inventory count
UPDATE events
SET seats_available = seats_available - 2
WHERE event_id = 'evt_001';

-- Create booking record
INSERT INTO bookings (booking_id, user_id, event_id, seat_ids, status, expires_at)
VALUES ('book_001', 'user_001', 'evt_001', 
        ARRAY['seat_001', 'seat_002'], 
        'pending',
        NOW() + INTERVAL '10 minutes');

COMMIT;  ← All changes permanent, locks released


STEP 4: Update Redis cache
────────────────────────────────────────────────
-- Update available seat count
DECRBY seats:available:evt_001 2

-- Track user's locks
SADD user:locks:user_001 "seat_001"
SADD user:locks:user_001 "seat_002"
EXPIRE user:locks:user_001 600  (10 min TTL)

-- Broadcast to other users
PUBLISH seats:evt_001:updates '{
  "seats_locked": ["seat_001", "seat_002"],
  "seats_remaining": 69998
}'


Response to user:
{
  success: true,
  booking_id: 'book_001',
  seats: ['A-1-1', 'A-1-2'],
  total: 1000,
  expires_at: '2024-02-26T10:15:00Z',
  message: 'Seats held for 10 minutes. Complete payment to confirm.'
}

User redirected to payment page
Timer shows: "9:58 remaining to complete purchase"
```

```
FLOW 5: Payment Processing
════════════════════════════════════════════════════════

User enters credit card, clicks "Pay Now"
        │
        ▼
POST /api/payment/process
{
  booking_id: 'book_001',
  payment_method: 'credit_card',
  card_token: 'tok_xyz123'
}
        │
        ▼
STEP 1: Validate booking still valid
────────────────────────────────────────────────
SELECT booking_id, status, expires_at, seat_ids
FROM bookings
WHERE booking_id = 'book_001'
AND status = 'pending';

Check: expires_at > NOW()?
→ If expired: reject with "Booking expired"


STEP 2: Charge card via Stripe
────────────────────────────────────────────────
stripe.Charge.create(
  amount=1000,
  currency='usd',
  source='tok_xyz123'
)

→ Returns: charge_id = 'ch_abc456'

If payment fails:
  → Return error to user
  → Keep seats locked (user can retry)
  → Seats only released after full 10 min TTL


STEP 3: Update database (ACID transaction)
────────────────────────────────────────────────
BEGIN TRANSACTION;

-- Mark seats as SOLD
UPDATE seats
SET status = 'sold',
    sold_to = 'user_001',
    locked_by = NULL,
    lock_timestamp = NULL
WHERE seat_id IN (
  SELECT unnest(seat_ids) FROM bookings WHERE booking_id = 'book_001'
);

-- Confirm booking
UPDATE bookings
SET status = 'confirmed',
    expires_at = NULL
WHERE booking_id = 'book_001';

-- Record payment
INSERT INTO payments (payment_id, booking_id, amount, status, transaction_id)
VALUES ('pay_001', 'book_001', 1000, 'completed', 'ch_abc456');

COMMIT;


STEP 4: Cleanup Redis
────────────────────────────────────────────────
-- Remove user's session (no longer needed)
DEL session:user_001

-- Remove user's locks
DEL user:locks:user_001

-- Update available count is already decremented (in lock step)
-- Seats remain unavailable (sold)


STEP 5: Send confirmation
────────────────────────────────────────────────
Email to user:
"Your tickets are confirmed!
 Event: Taylor Swift 2024
 Seats: A-1-1, A-1-2
 Total: $1,000
 Ticket PDF attached"

Response to browser:
{
  success: true,
  confirmation_number: 'CONF-XYZ-123',
  tickets_url: 'https://ticketmaster.com/tickets/download/...'
}
```

```
FLOW 6: Lock Expiration (User Doesn't Complete Payment)
════════════════════════════════════════════════════════

User locked seats at 10:05:00
Current time: 10:15:01 (10 minutes 1 second later)
User never completed payment
        │
        ▼
Background job runs every 30 seconds:
────────────────────────────────────────────────
SELECT booking_id, seat_ids
FROM bookings
WHERE status = 'pending'
AND expires_at < NOW();

Finds: book_001 (expired)


BEGIN TRANSACTION;

-- Release seats back to available
UPDATE seats
SET status = 'available',
    locked_by = NULL,
    lock_timestamp = NULL
WHERE seat_id IN (
  SELECT unnest(seat_ids) FROM bookings WHERE booking_id = 'book_001'
);

-- Mark booking as expired
UPDATE bookings
SET status = 'expired'
WHERE booking_id = 'book_001';

-- Restore inventory count
UPDATE events
SET seats_available = seats_available + 2
WHERE event_id = 'evt_001';

COMMIT;


Update Redis:
INCRBY seats:available:evt_001 2

Broadcast to all users:
PUBLISH seats:evt_001:updates '{
  "seats_released": ["seat_001", "seat_002"],
  "seats_remaining": 70000
}'

Other users see: "70,000 tickets remaining" (updated)
Seats A-1-1 and A-1-2 appear available again
Next user can lock them
```

```
FLOW 7: Handling Database Failure Mid-Transaction
════════════════════════════════════════════════════════

User is locking seats
PostgreSQL crashes during UPDATE
        │
        ▼
BEGIN TRANSACTION;

SELECT * FROM seats ... FOR UPDATE;  ✓ succeeds

UPDATE seats SET status = 'locked' ...  ✓ succeeds

-- CRASH HERE (PostgreSQL server dies)

UPDATE events SET seats_available = ...  ✗ never executes

COMMIT;  ✗ never executes


What happens:
────────────────────────────────────────────────
PostgreSQL write-ahead log (WAL) has partial transaction
On restart: PostgreSQL ROLLS BACK incomplete transaction
Result: ALL changes undone
        seats table: unchanged (still 'available')
        events table: unchanged
        No inconsistent state possible

User sees error: "Database error, please try again"
User retries → succeeds on second attempt

This is ACID Durability guarantee
Only PostgreSQL/MySQL/Oracle provide this
NoSQL databases: partial writes can persist
```

---

## Tradeoffs vs Other Databases

```
┌───────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                           │ POSTGRESQL   │ MONGODB      │ DYNAMODB     │ CASSANDRA    │
├───────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Row-level locking         │ Native ✓     │ Limited      │ NO ✗         │ NO ✗         │
│ SELECT FOR UPDATE         │ Yes ✓        │ NO ✗         │ NO ✗         │ NO ✗         │
│ Multi-row transactions    │ Unlimited ✓  │ 16MB limit   │ 100 item max │ NO ✗         │
│ ACID guarantees           │ Full ✓       │ Partial      │ Partial      │ NO ✗         │
│ Double booking prevention │ Guaranteed ✓ │ Risky        │ Manual       │ Impossible   │
│ Complex queries           │ Natural ✓    │ Aggregation  │ Limited      │ Very limited │
│ Write throughput          │ 10K/sec      │ 50K/sec ✓    │ Variable ✓   │ 100K/sec ✓   │
│ Lock timeout handling     │ Built-in ✓   │ Manual       │ Manual       │ N/A          │
│ Fairness (queue)          │ Redis ✓      │ Redis ✓      │ Redis ✓      │ Redis ✓      │
│ Operational complexity    │ LOW ✓        │ MEDIUM       │ LOW (managed)│ HIGH         │
│ Cost at 70K seats         │ LOW ✓        │ MEDIUM       │ HIGH         │ HIGH         │
└───────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **PostgreSQL with row-level locking via SELECT FOR UPDATE is non-negotiable for Ticketmaster because preventing double booking of unique seats requires ACID transactions with exclusive locks that guarantee only one user can lock seat A-1-1 even when 2 million users click it simultaneously within milliseconds — NoSQL databases like MongoDB, DynamoDB, and Cassandra either lack true row-level locking, have limited multi-document transactions (100 items max for DynamoDB), or rely on slow Paxos-based lightweight transactions that cannot handle 500K requests/second, making them unsuitable for unique inventory where double booking has zero tolerance — Redis virtual queue solves the thundering herd problem by admitting users at a controlled rate of 100/second instead of overwhelming PostgreSQL with 2M concurrent SELECT FOR UPDATE queries, randomizing queue order at sale start for fairness so early arrivals get no advantage over bots, and enforcing 10-minute hold TTLs that automatically expire in Redis releasing seats back to availability without manual cleanup jobs, while also providing sub-millisecond updates to "seats remaining" counters that PostgreSQL aggregations would make too slow for real-time display — together they enable Ticketmaster's magic of selling 70,000 seats to 2 million competing buyers in under 10 minutes with absolute zero double bookings, fair lottery-based queue access, automatic lock expiration when users abandon checkout, and graceful handling of payment failures through ACID transaction rollbacks that leave no orphaned seat locks or inconsistent inventory counts.**


