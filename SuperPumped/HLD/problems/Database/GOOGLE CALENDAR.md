# Why This Architecture for Calendar (Google Calendar / Outlook)?

Let me build your intuition from the ground up — starting with what makes a calendar system brutally hard at scale.

---

## The Core Problem Calendar Systems Solve

On the surface a calendar seems simple — store events with start/end times. But consider the real constraints:

```
Calendar scale reality:
────────────────────────────────────────────────
Active users:              2 billion+ (Google Calendar)
Events per user:           100-500 events average
Total events:              200+ billion events
Events created:            10 million+ per day
Queries per second:        500K+ (viewing calendars)

Requirements:
→ Find overlapping events (<10ms for conflict detection)
→ Recurring events (daily standup for next 2 years = 730 instances)
→ Time zone conversions (event in "America/Los_Angeles" shown to user in "Asia/Tokyo")
→ Multi-user events (invite 50 people, track RSVP status)
→ Free/busy queries ("When is Alice available next week?")
→ Reminders (send notification 15 minutes before event)
→ All-day events (no specific time, date-based)
→ Complex recurrence (every Monday and Wednesday, 2nd Tuesday of month)
→ Exception handling (recurring event, skip one occurrence)
→ Search ("Find all meetings with Bob in Q4 2023")
```

This combination of **time-based queries + recurring event complexity + conflict detection + multi-user coordination + timezone handling** is what forces this specific architecture.

---

## Why PostgreSQL for Events?

### The Time-Based Query Problem

```
CORE QUERIES:
════════════════════════════════════════════════════════

Query 1: "Show me my calendar for this week"
SELECT * FROM events
WHERE user_id = 'user_001'
AND start_time >= '2024-02-26 00:00:00'
AND start_time < '2024-03-04 00:00:00'
ORDER BY start_time ASC;

Query 2: "Find overlapping events" (conflict detection)
SELECT * FROM events
WHERE user_id = 'user_001'
AND start_time < '2024-02-26 15:00:00'  -- new event end
AND end_time > '2024-02-26 14:00:00'    -- new event start
AND event_id != 'new_event_id';

Query 3: "When is Alice free next week?"
SELECT start_time, end_time FROM events
WHERE user_id = 'alice_id'
AND start_time >= '2024-02-26 00:00:00'
AND start_time < '2024-03-04 00:00:00'
ORDER BY start_time ASC;
-- Find gaps between events

These are INTERVAL OVERLAP queries
Perfect for B-tree indexes on timestamps
```

### PostgreSQL Schema

```
POSTGRESQL SCHEMA:
════════════════════════════════════════════════════════

Events table:
────────────────────────────────────────────────────────────────────────────────
event_id │ user_id  │ title          │ start_time          │ end_time            │ timezone        │ all_day
───────────────────────────────────────────────────────────────────────────────────────────────────────────────────
evt_001  │ user_001 │ Team Meeting   │ 2024-02-26 14:00:00│ 2024-02-26 15:00:00 │ America/LA     │ false
evt_002  │ user_001 │ Doctor Appt    │ 2024-02-27 10:00:00│ 2024-02-27 10:30:00 │ America/LA     │ false
evt_003  │ user_001 │ Birthday Party │ 2024-02-28 00:00:00│ 2024-02-28 23:59:59 │ America/LA     │ true

Additional columns:
  description: TEXT
  location: VARCHAR
  color: VARCHAR
  visibility: ENUM('public', 'private', 'default')
  created_at: TIMESTAMP WITH TIME ZONE
  updated_at: TIMESTAMP WITH TIME ZONE
  creator_id: UUID (who created it)

Indexes:
  PRIMARY KEY (event_id)
  INDEX (user_id, start_time)  ← Critical for calendar view
  INDEX (user_id, end_time)    ← Critical for conflict detection
  BTREE INDEX ON (start_time, end_time) USING GIST  ← Interval overlap


Recurring_Events table:
────────────────────────────────────────────────────────────────────────
event_id │ recurrence_rule                          │ recurrence_end
────────────────────────────────────────────────────────────────────────────────
evt_004  │ FREQ=DAILY;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR│ 2024-12-31
evt_005  │ FREQ=WEEKLY;INTERVAL=1;BYDAY=MO          │ 2025-01-31
evt_006  │ FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=15    │ NULL (forever)

RRULE format (iCalendar standard RFC 5545):
FREQ=DAILY: Repeats daily
INTERVAL=2: Every 2nd occurrence
BYDAY=MO,WE: Only Monday and Wednesday
BYMONTHDAY=15: 15th of each month
COUNT=10: Stop after 10 occurrences
UNTIL=2024-12-31: Stop at this date


Recurring_Event_Instances table (materialized instances):
────────────────────────────────────────────────────────────────────────
instance_id │ event_id │ occurrence_date     │ is_exception │ exception_reason
───────────────────────────────────────────────────────────────────────────────────────
inst_001    │ evt_004  │ 2024-02-26 09:00:00 │ false        │ NULL
inst_002    │ evt_004  │ 2024-02-27 09:00:00 │ false        │ NULL
inst_003    │ evt_004  │ 2024-02-28 09:00:00 │ true         │ "Cancelled"
inst_004    │ evt_004  │ 2024-02-29 09:00:00 │ false        │ NULL

Why materialize instances:
- Fast queries (no runtime RRULE expansion)
- Easy exception handling (mark specific instance)
- Efficient conflict detection (treat like regular events)

Generate instances:
- On event creation: Generate next 90 days
- Background job: Generate 90 days ahead rolling window


Attendees table:
────────────────────────────────────────────────────────────────────────
event_id │ user_id  │ email             │ response_status │ is_organizer
────────────────────────────────────────────────────────────────────────────────
evt_001  │ user_001 │ john@company.com  │ accepted        │ true
evt_001  │ user_002 │ alice@company.com │ tentative       │ false
evt_001  │ user_003 │ bob@company.com   │ declined        │ false

response_status: ENUM('accepted', 'tentative', 'declined', 'needsAction')

Indexes:
  PRIMARY KEY (event_id, user_id)
  INDEX (user_id, response_status)  ← "Show my accepted events"


Reminders table:
────────────────────────────────────────────────────────────────────────
reminder_id │ event_id │ user_id  │ remind_at           │ method │ sent
────────────────────────────────────────────────────────────────────────────────
rem_001     │ evt_001  │ user_001 │ 2024-02-26 13:45:00 │ email  │ false
rem_002     │ evt_001  │ user_001 │ 2024-02-26 13:55:00 │ push   │ false

method: ENUM('email', 'push', 'sms')

Background job polls:
SELECT * FROM reminders
WHERE remind_at <= NOW()
AND sent = false
ORDER BY remind_at ASC
LIMIT 1000;
```

### Why B-tree Indexes Are Perfect for Time Ranges

```
B-TREE INDEX ON START_TIME:
════════════════════════════════════════════════════════

Index structure (simplified):
────────────────────────────────────────────────

                    [2024-02-26 12:00]
                    /               \
        [2024-02-26 10:00]      [2024-02-26 16:00]
        /           \           /             \
    [09:00]     [11:00]    [14:00]        [18:00]
     |           |          |              |
   evt_002    evt_003    evt_001        evt_004


Query: "Events between 14:00 and 16:00"
────────────────────────────────────────────────
WHERE start_time >= '2024-02-26 14:00:00'
AND start_time < '2024-02-26 16:00:00'

B-tree traversal:
1. Start at root: 12:00
2. Go right (14:00 > 12:00)
3. Reach 16:00 node
4. Go left (14:00 < 16:00)
5. Find 14:00 leaf
6. Sequential scan: [14:00, 14:05, 14:30, ...]
7. Stop at 16:00

Query time: O(log N + K) where K = results
On 100K events: log₂(100K) + K ≈ 17 + K comparisons
Time: <5ms


GIST Index for Interval Overlap:
────────────────────────────────────────────────
CREATE INDEX idx_event_interval ON events 
USING GIST (tsrange(start_time, end_time));

Query: "Find overlapping events"
WHERE tsrange(start_time, end_time) && 
      tsrange('2024-02-26 14:00:00', '2024-02-26 15:00:00')

GIST (Generalized Search Tree):
- Optimized for interval queries
- Finds overlapping ranges efficiently
- Used for: geometry, IP ranges, time ranges

Query time: <10ms even on millions of events
```

### Complex Query Examples

```
QUERY 1: Free/Busy Lookup
════════════════════════════════════════════════════════

"When is Alice free between 9 AM - 5 PM next week?"

WITH business_hours AS (
  SELECT generate_series(
    '2024-02-26 09:00:00'::timestamp,
    '2024-03-01 17:00:00'::timestamp,
    '1 hour'::interval
  ) AS hour
),
busy_times AS (
  SELECT start_time, end_time
  FROM events
  WHERE user_id = 'alice_id'
  AND start_time >= '2024-02-26 09:00:00'
  AND end_time <= '2024-03-01 17:00:00'
)
SELECT bh.hour
FROM business_hours bh
LEFT JOIN busy_times bt 
  ON bh.hour >= bt.start_time 
  AND bh.hour < bt.end_time
WHERE bt.start_time IS NULL  -- No overlap = free
ORDER BY bh.hour;

Returns available time slots


QUERY 2: Find Common Free Time (Meeting Scheduling)
════════════════════════════════════════════════════════

"When are Alice, Bob, and Charlie all free next week?"

WITH all_events AS (
  SELECT start_time, end_time
  FROM events
  WHERE user_id IN ('alice', 'bob', 'charlie')
  AND start_time >= '2024-02-26 00:00:00'
  AND start_time < '2024-03-04 00:00:00'
),
merged_busy_times AS (
  -- Merge overlapping intervals
  SELECT 
    MIN(start_time) as start_time,
    MAX(end_time) as end_time
  FROM (
    SELECT start_time, end_time,
           SUM(new_interval) OVER (ORDER BY start_time, end_time) as interval_group
    FROM (
      SELECT start_time, end_time,
             CASE WHEN start_time <= LAG(end_time) OVER (ORDER BY start_time)
                  THEN 0 ELSE 1 END as new_interval
      FROM all_events
    ) t
  ) t2
  GROUP BY interval_group
)
-- Find gaps between busy times
SELECT 
  LAG(end_time) OVER (ORDER BY start_time) as free_start,
  start_time as free_end
FROM merged_busy_times
WHERE LAG(end_time) OVER (ORDER BY start_time) < start_time;

Returns time slots when all three are free


QUERY 3: Search Events
════════════════════════════════════════════════════════

"Find all meetings with 'budget' in title or description in Q4 2023"

SELECT event_id, title, start_time, description
FROM events e
WHERE user_id = 'user_001'
AND (title ILIKE '%budget%' OR description ILIKE '%budget%')
AND start_time >= '2023-10-01'
AND start_time < '2024-01-01'
ORDER BY start_time DESC;

Full-text search optimization:
CREATE INDEX idx_events_title_fts ON events 
USING GIN (to_tsvector('english', title || ' ' || description));

Then query:
WHERE to_tsvector('english', title || ' ' || description) @@ 
      to_tsquery('english', 'budget');

Much faster than ILIKE on large datasets
```

---

## Why Redis for Reminders?

### The Reminder Problem

```
REMINDER REQUIREMENTS:
════════════════════════════════════════════════════════

User creates event for 2 PM
Wants reminder at 1:45 PM (15 minutes before)

Naive PostgreSQL approach:
────────────────────────────────────────────────
Cron job every minute:
SELECT * FROM reminders
WHERE remind_at <= NOW()
AND sent = false
LIMIT 1000;

Problems:
→ Poll every minute (even when no reminders due)
→ 1 billion events × 2 reminders avg = 2 billion rows
→ Query scans 2 billion rows every minute
→ Index on remind_at helps but still expensive
→ Database load constantly high


Real-world scale:
────────────────────────────────────────────────
2 billion users × 100 events × 2 reminders = 400 billion reminders
Polling 400 billion rows every minute is impossible
```

### Redis TTL-Based Solution

```
REDIS REMINDER ARCHITECTURE:
════════════════════════════════════════════════════════

When event created at 2 PM with 15-min reminder:
────────────────────────────────────────────────

Event time: 2024-02-26 14:00:00
Reminder time: 2024-02-26 13:45:00
Current time: 2024-02-26 10:00:00

TTL calculation:
ttl = reminder_time - current_time
    = 13:45:00 - 10:00:00
    = 3 hours 45 minutes
    = 13,500 seconds

Redis command:
SETEX reminder:evt_001:user_001 13500 '{
  "event_id": "evt_001",
  "user_id": "user_001",
  "event_title": "Team Meeting",
  "event_start": "2024-02-26T14:00:00Z",
  "reminder_type": "push"
}'

After 13,500 seconds (at 13:45:00):
→ Key expires
→ Redis publishes to keyspace notification channel
→ Reminder service listens and sends notification


Keyspace notifications:
────────────────────────────────────────────────
CONFIG SET notify-keyspace-events Ex
# E = keyspace events, x = expired events

SUBSCRIBE __keyevent@0__:expired

Listener receives:
"reminder:evt_001:user_001"

Fetch value (before it's deleted):
GET reminder:evt_001:user_001
→ Returns event details
→ Send push notification
→ Send email


Benefits:
────────────────────────────────────────────────
✓ Zero database polling
✓ Automatic trigger at exact time
✓ Scales to billions of reminders
✓ Memory efficient (expired keys deleted)
✓ Sub-second accuracy
```

### Alternative: PostgreSQL + Background Worker

```
POSTGRESQL APPROACH (ALSO VIABLE):
════════════════════════════════════════════════════════

Reminders table:
reminder_id │ event_id │ remind_at           │ sent
────────────────────────────────────────────────────────
rem_001     │ evt_001  │ 2024-02-26 13:45:00 │ false
rem_002     │ evt_002  │ 2024-02-26 15:55:00 │ false

Index on (remind_at) WHERE sent = false


Background worker (runs every 10 seconds):
────────────────────────────────────────────────
SELECT reminder_id, event_id, user_id
FROM reminders
WHERE remind_at <= NOW()
AND sent = false
ORDER BY remind_at ASC
FOR UPDATE SKIP LOCKED  -- Avoid lock contention
LIMIT 100;

For each reminder:
1. Send notification
2. UPDATE reminders SET sent = true WHERE reminder_id = X


Pros:
────────────────────────────────────────────────
✓ Single source of truth (PostgreSQL)
✓ Durable (survives Redis restart)
✓ Easier to query ("Show all pending reminders")
✓ Transaction safety


Cons:
────────────────────────────────────────────────
✗ Polling overhead (even if no reminders)
✗ 10-second granularity (vs Redis sub-second)
✗ Database load (constant queries)
✗ Scaling challenge at billions of reminders


WHEN TO USE EACH:
────────────────────────────────────────────────

PostgreSQL approach:
→ Small-medium scale (<10M reminders)
→ Need durability guarantee
→ Already using PostgreSQL heavily

Redis TTL approach:
→ Large scale (>100M reminders)
→ Need exact timing (<1 second)
→ Want zero polling overhead
→ Acceptable to lose reminders on Redis crash (rare)

Hybrid (best of both):
→ Store reminders in PostgreSQL (durability)
→ Load upcoming reminders (next hour) into Redis
→ Use Redis TTL for exact triggering
→ Background sync keeps Redis fresh
```

---

## Recurring Events Deep Dive

### RRULE Parsing and Expansion

```
RECURRING EVENT EXAMPLE:
════════════════════════════════════════════════════════

"Team standup every weekday (Mon-Fri) at 9 AM, forever"

RRULE: FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=9

Expansion algorithm:
────────────────────────────────────────────────
START: 2024-02-26 (Monday)
FREQ: DAILY

Iterate:
- 2024-02-26 (Mon) → Include ✓
- 2024-02-27 (Tue) → Include ✓
- 2024-02-28 (Wed) → Include ✓
- 2024-02-29 (Thu) → Include ✓
- 2024-03-01 (Fri) → Include ✓
- 2024-03-02 (Sat) → Skip (not in BYDAY)
- 2024-03-03 (Sun) → Skip (not in BYDAY)
- 2024-03-04 (Mon) → Include ✓
...

Generate instances:
INSERT INTO recurring_event_instances (event_id, occurrence_date)
VALUES
  ('evt_004', '2024-02-26 09:00:00'),
  ('evt_004', '2024-02-27 09:00:00'),
  ('evt_004', '2024-02-28 09:00:00'),
  ...
  (next 90 days);


Complex RRULE: "2nd Tuesday of every month"
────────────────────────────────────────────────
RRULE: FREQ=MONTHLY;BYDAY=2TU

Expansion:
- February 2024: 2nd Tuesday = Feb 13
- March 2024: 2nd Tuesday = Mar 12
- April 2024: 2nd Tuesday = Apr 9
...

BYDAY format:
- MO = Monday
- TU = Tuesday
- 2TU = 2nd Tuesday
- -1FR = Last Friday (negative = from end)
```

### Exception Handling

```
EXCEPTION SCENARIOS:
════════════════════════════════════════════════════════

Scenario 1: Cancel one occurrence
────────────────────────────────────────────────
User: "Cancel Feb 28 standup only"

UPDATE recurring_event_instances
SET is_exception = true,
    exception_reason = 'cancelled'
WHERE event_id = 'evt_004'
AND occurrence_date = '2024-02-28 09:00:00';

Query: "Show my calendar"
SELECT * FROM recurring_event_instances
WHERE user_id = 'user_001'
AND occurrence_date >= '2024-02-26'
AND occurrence_date < '2024-03-04'
AND is_exception = false;  -- Exclude cancelled

Result: Shows all standups except Feb 28


Scenario 2: Reschedule one occurrence
────────────────────────────────────────────────
User: "Move Feb 28 standup to 10 AM"

-- Mark original as exception
UPDATE recurring_event_instances
SET is_exception = true,
    exception_reason = 'rescheduled'
WHERE event_id = 'evt_004'
AND occurrence_date = '2024-02-28 09:00:00';

-- Create new event for rescheduled time
INSERT INTO events (event_id, user_id, start_time, end_time, title, is_exception_of)
VALUES ('evt_new', 'user_001', '2024-02-28 10:00:00', '2024-02-28 10:15:00', 
        'Team Standup', 'evt_004');


Scenario 3: "Edit all future occurrences"
────────────────────────────────────────────────
User: "Change standup to 10 AM starting March 1"

-- Update recurrence end date
UPDATE recurring_events
SET recurrence_end = '2024-02-29'
WHERE event_id = 'evt_004';

-- Create new recurring event (starting March 1)
INSERT INTO events (...) VALUES (..., start_time = '10:00:00', ...);
INSERT INTO recurring_events (...) VALUES (..., start_date = '2024-03-01', ...);

-- Generate instances for new series
...
```

---

## Time Zone Handling

### Store UTC, Display Local

```
TIME ZONE ARCHITECTURE:
════════════════════════════════════════════════════════

Event created by user in Los Angeles:
────────────────────────────────────────────────
User input: "Meeting on Feb 26, 2024 at 2 PM PST"

Store in PostgreSQL (UTC):
INSERT INTO events (event_id, start_time, end_time, timezone)
VALUES ('evt_001', 
        '2024-02-26 22:00:00+00',  -- 2 PM PST = 10 PM UTC
        '2024-02-26 23:00:00+00',  -- 3 PM PST = 11 PM UTC
        'America/Los_Angeles');

Why store timezone separately:
- For displaying "2 PM PST" to user
- For handling DST transitions
- For "show in my timezone" feature


User in Tokyo views calendar:
────────────────────────────────────────────────
User timezone: Asia/Tokyo (UTC+9)

Query:
SELECT event_id, 
       start_time AT TIME ZONE 'Asia/Tokyo' as local_start,
       end_time AT TIME ZONE 'Asia/Tokyo' as local_end
FROM events
WHERE user_id = 'user_001';

Result:
local_start: 2024-02-27 07:00:00  (22:00 UTC + 9 hours)
local_end:   2024-02-27 08:00:00

UI displays: "Feb 27, 2024 at 7 AM JST"


DST (Daylight Saving Time) handling:
────────────────────────────────────────────────
Event: "Weekly meeting every Monday at 2 PM PST"

On March 10, 2024: DST starts in US (spring forward)
- Before DST: 2 PM PST = UTC 22:00
- After DST: 2 PM PDT = UTC 21:00 (1 hour shift)

PostgreSQL handles this automatically:
'2024-03-10 14:00:00 America/Los_Angeles' 
  converts to correct UTC based on DST rules

Without timezone-aware storage:
- Would need manual DST adjustments
- Complex business logic
- Error-prone
```

### All-Day Events

```
ALL-DAY EVENT CHALLENGE:
════════════════════════════════════════════════════════

Event: "Birthday Party on Feb 28, 2024"
No specific time, entire day

Naive approach:
start_time: 2024-02-28 00:00:00
end_time: 2024-02-28 23:59:59

Problems:
→ Timezone confusion (midnight in which timezone?)
→ Spans two days in other timezones
→ User in Tokyo sees "Feb 28 9 AM - Feb 29 8:59 AM" (weird!)


Correct approach:
────────────────────────────────────────────────
Store all-day events differently:

all_day: true
event_date: 2024-02-28 (DATE type, not TIMESTAMP)
start_time: NULL
end_time: NULL

UI rendering:
- Show at top of calendar day
- Display as "All day" badge
- No time shown
- Doesn't block other events (no conflict)

Query for all-day events:
SELECT * FROM events
WHERE user_id = 'user_001'
AND event_date = '2024-02-28'
AND all_day = true;


Multi-day all-day events:
────────────────────────────────────────────────
Event: "Vacation Feb 28 - Mar 3"

all_day: true
event_date_start: 2024-02-28
event_date_end: 2024-03-03

Query: "Is user on vacation on March 1?"
SELECT * FROM events
WHERE user_id = 'user_001'
AND all_day = true
AND event_date_start <= '2024-03-01'
AND event_date_end >= '2024-03-01';
```

---

## Complete Flow

```
FLOW 1: Create Event
════════════════════════════════════════════════════════

User: "Create meeting tomorrow 2-3 PM, invite Alice and Bob"
        │
        ▼
POST /api/events
{
  title: "Project Sync",
  start_time: "2024-02-27T14:00:00",
  end_time: "2024-02-27T15:00:00",
  timezone: "America/Los_Angeles",
  attendees: ["alice@co.com", "bob@co.com"],
  reminders: [
    {minutes_before: 15, method: "push"},
    {minutes_before: 30, method: "email"}
  ]
}
        │
        ▼
STEP 1: Check for conflicts (PostgreSQL)
────────────────────────────────────────────────
SELECT event_id, title, start_time, end_time
FROM events
WHERE user_id = 'user_001'
AND start_time < '2024-02-27 15:00:00'  -- new end
AND end_time > '2024-02-27 14:00:00'    -- new start
AND event_id != 'new_event_id';

Returns: evt_conflict (Doctor appointment 2:30-3:00 PM)

Response: "Conflict detected: Doctor appointment"
User: "Create anyway"


STEP 2: Insert event (PostgreSQL transaction)
────────────────────────────────────────────────
BEGIN TRANSACTION;

-- Create event
INSERT INTO events (event_id, user_id, title, start_time, end_time, timezone, created_at)
VALUES ('evt_new', 'user_001', 'Project Sync', 
        '2024-02-27 22:00:00+00',  -- converted to UTC
        '2024-02-27 23:00:00+00', 
        'America/Los_Angeles',
        NOW());

-- Add attendees
INSERT INTO attendees (event_id, user_id, email, response_status, is_organizer)
VALUES 
  ('evt_new', 'user_001', 'john@co.com', 'accepted', true),
  ('evt_new', 'alice_id', 'alice@co.com', 'needsAction', false),
  ('evt_new', 'bob_id', 'bob@co.com', 'needsAction', false);

-- Create reminders in PostgreSQL (for durability)
INSERT INTO reminders (reminder_id, event_id, user_id, remind_at, method, sent)
VALUES
  ('rem_001', 'evt_new', 'user_001', '2024-02-27 21:45:00+00', 'push', false),
  ('rem_002', 'evt_new', 'user_001', '2024-02-27 21:30:00+00', 'email', false);

COMMIT;

Time: <20ms


STEP 3: Schedule reminders (Redis)
────────────────────────────────────────────────
Current time: 2024-02-27 10:00:00

15-min reminder at 21:45:00:
ttl = 21:45:00 - 10:00:00 = 11h 45m = 42,300 seconds

SETEX reminder:evt_new:user_001:push 42300 '{
  "event_id": "evt_new",
  "user_id": "user_001",
  "title": "Project Sync",
  "start_time": "2024-02-27T14:00:00-08:00",
  "method": "push"
}'

30-min reminder at 21:30:00:
ttl = 21:30:00 - 10:00:00 = 11h 30m = 41,400 seconds

SETEX reminder:evt_new:user_001:email 41400 '{...}'


STEP 4: Send invitations (async)
────────────────────────────────────────────────
Email to alice@co.com:
"John invited you to Project Sync
 When: Tomorrow, Feb 27 at 2:00 PM PST
 [Accept] [Tentative] [Decline]"

Calendar invite attachment (iCal format):
BEGIN:VCALENDAR
VERSION:2.0
BEGIN:VEVENT
UID:evt_new@calendar.app
DTSTART:20240227T220000Z
DTEND:20240227T230000Z
SUMMARY:Project Sync
ORGANIZER:john@co.com
ATTENDEE:alice@co.com
END:VEVENT
END:VCALENDAR
```

```
FLOW 2: View Calendar
════════════════════════════════════════════════════════

User: Opens calendar app, views this week
        │
        ▼
GET /api/calendar?start=2024-02-26&end=2024-03-03&timezone=America/Los_Angeles
        │
        ▼
STEP 1: Fetch events (PostgreSQL)
────────────────────────────────────────────────
SELECT 
  e.event_id,
  e.title,
  e.start_time AT TIME ZONE 'America/Los_Angeles' as local_start,
  e.end_time AT TIME ZONE 'America/Los_Angeles' as local_end,
  e.all_day,
  e.color,
  a.response_status,
  COUNT(att.user_id) as attendee_count
FROM events e
LEFT JOIN attendees a ON e.event_id = a.event_id AND a.user_id = 'user_001'
LEFT JOIN attendees att ON e.event_id = att.event_id
WHERE e.user_id = 'user_001'
  OR a.user_id = 'user_001'  -- Events I'm invited to
AND e.start_time >= '2024-02-26 00:00:00+00'
AND e.start_time < '2024-03-04 00:00:00+00'
GROUP BY e.event_id, a.response_status
ORDER BY e.start_time ASC;

Query time: <10ms (indexed on user_id, start_time)


STEP 2: Fetch recurring event instances
────────────────────────────────────────────────
SELECT 
  ri.instance_id,
  e.title,
  ri.occurrence_date AT TIME ZONE 'America/Los_Angeles' as local_start,
  e.duration
FROM recurring_event_instances ri
JOIN events e ON ri.event_id = e.event_id
WHERE e.user_id = 'user_001'
AND ri.occurrence_date >= '2024-02-26 00:00:00+00'
AND ri.occurrence_date < '2024-03-04 00:00:00+00'
AND ri.is_exception = false
ORDER BY ri.occurrence_date ASC;


STEP 3: Merge and return
────────────────────────────────────────────────
Response:
{
  "events": [
    {
      "id": "evt_001",
      "title": "Team Meeting",
      "start": "2024-02-26T14:00:00-08:00",
      "end": "2024-02-26T15:00:00-08:00",
      "attendees": 5,
      "status": "accepted",
      "color": "blue"
    },
    {
      "id": "evt_004_inst_001",
      "title": "Daily Standup",
      "start": "2024-02-26T09:00:00-08:00",
      "end": "2024-02-26T09:15:00-08:00",
      "recurring": true,
      "status": "accepted"
    },
    ...
  ]
}

Total latency: <50ms
```

```
FLOW 3: Respond to Invitation
════════════════════════════════════════════════════════

Alice clicks "Accept" in email invitation
        │
        ▼
POST /api/events/evt_new/respond
{
  user_id: "alice_id",
  response: "accepted"
}
        │
        ▼
STEP 1: Update attendee status (PostgreSQL)
────────────────────────────────────────────────
UPDATE attendees
SET response_status = 'accepted',
    responded_at = NOW()
WHERE event_id = 'evt_new'
AND user_id = 'alice_id';


STEP 2: Add event to Alice's calendar
────────────────────────────────────────────────
-- Event already exists in events table
-- Just need to ensure Alice sees it

-- Alice can now query her events:
SELECT * FROM events e
JOIN attendees a ON e.event_id = a.event_id
WHERE a.user_id = 'alice_id'
AND a.response_status != 'declined';


STEP 3: Notify organizer
────────────────────────────────────────────────
Email to john@co.com:
"Alice accepted your invitation to Project Sync"


STEP 4: Check if all attendees responded
────────────────────────────────────────────────
SELECT 
  COUNT(*) as total,
  SUM(CASE WHEN response_status = 'accepted' THEN 1 ELSE 0 END) as accepted,
  SUM(CASE WHEN response_status = 'declined' THEN 1 ELSE 0 END) as declined,
  SUM(CASE WHEN response_status = 'needsAction' THEN 1 ELSE 0 END) as pending
FROM attendees
WHERE event_id = 'evt_new';

Result: 2 accepted, 0 declined, 1 pending (Bob hasn't responded)
```

```
FLOW 4: Reminder Triggers
════════════════════════════════════════════════════════

Time reaches 21:45:00 UTC (1:45 PM PST, 15 min before event)
        │
        ▼
STEP 1: Redis key expires
────────────────────────────────────────────────
Key: reminder:evt_new:user_001:push
TTL reaches 0


STEP 2: Keyspace notification fires
────────────────────────────────────────────────
Redis publishes to: __keyevent@0__:expired
Message: "reminder:evt_new:user_001:push"


STEP 3: Reminder service listens
────────────────────────────────────────────────
Service subscribed to expired events

Receives: "reminder:evt_new:user_001:push"

Parse:
- event_id: evt_new
- user_id: user_001
- method: push


STEP 4: Fetch event details (PostgreSQL)
────────────────────────────────────────────────
SELECT title, start_time, location
FROM events
WHERE event_id = 'evt_new';

Returns: "Project Sync" at 2:00 PM


STEP 5: Send push notification
────────────────────────────────────────────────
FCM/APNS push to user_001's device:
{
  "title": "Upcoming: Project Sync",
  "body": "Starts in 15 minutes at 2:00 PM",
  "action": "open_event",
  "event_id": "evt_new"
}

User's phone vibrates
Notification appears


STEP 6: Mark as sent (PostgreSQL)
────────────────────────────────────────────────
UPDATE reminders
SET sent = true,
    sent_at = NOW()
WHERE event_id = 'evt_new'
AND user_id = 'user_001'
AND method = 'push';
```

```
FLOW 5: Find Available Meeting Time
════════════════════════════════════════════════════════

User: "Schedule meeting with Alice and Bob next week, 1 hour duration"
        │
        ▼
POST /api/calendar/find-available
{
  attendees: ["user_001", "alice_id", "bob_id"],
  duration_minutes: 60,
  start_date: "2024-02-26",
  end_date: "2024-03-01",
  business_hours: {start: "09:00", end: "17:00"}
}
        │
        ▼
STEP 1: Fetch all attendees' events (PostgreSQL)
────────────────────────────────────────────────
SELECT user_id, start_time, end_time
FROM events
WHERE user_id IN ('user_001', 'alice_id', 'bob_id')
AND start_time >= '2024-02-26 00:00:00'
AND start_time < '2024-03-02 00:00:00'
ORDER BY start_time ASC;

Returns all busy times for 3 people


STEP 2: Build busy time matrix (in-memory)
────────────────────────────────────────────────
Day-by-day analysis:

Feb 26:
- 09:00-10:00: All free
- 10:00-11:00: Alice busy
- 11:00-12:00: All free ✓ (1-hour slot)
- 14:00-15:00: All free ✓ (1-hour slot)
- 15:00-16:00: Bob busy

Feb 27:
- 09:00-10:00: John busy
- 10:00-11:00: All free ✓
- ...


STEP 3: Rank suggestions
────────────────────────────────────────────────
Algorithm considers:
- Earliest available (prefer morning)
- Fewest conflicts
- Working hours preference

Top suggestions:
1. Feb 26, 11:00 AM - 12:00 PM (all free, morning)
2. Feb 26, 2:00 PM - 3:00 PM (all free, afternoon)
3. Feb 27, 10:00 AM - 11:00 AM (all free, morning)


STEP 4: Return suggestions
────────────────────────────────────────────────
Response:
{
  "suggestions": [
    {
      "start": "2024-02-26T11:00:00-08:00",
      "end": "2024-02-26T12:00:00-08:00",
      "confidence": "high"
    },
    ...
  ]
}

User selects first option → creates event
```

---

## System Architecture Diagram

```
                ┌──────────────────┐
                │  User Browsers   │
                │  (Calendar UI)   │
                └────────┬─────────┘
                         │
                ┌────────▼─────────┐
                │   API Gateway    │
                └────────┬─────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
  ┌────────────┐  ┌────────────┐  ┌────────────┐
  │PostgreSQL  │  │   Redis    │  │Background  │
  │            │  │            │  │  Workers   │
  │ -Events    │  │ -Reminders │  │            │
  │ -Attendees │  │  (TTL)     │  │ -RRULE     │
  │ -Recurring │  │ -Cache     │  │  expansion │
  │ -Reminders │  │            │  │ -Email     │
  │  (durable) │  │            │  │  sender    │
  └────────────┘  └────────────┘  └────────────┘
         │               │               │
         └───────────────┼───────────────┘
                         │
                    ┌────▼──────┐
                    │ Message   │
                    │  Queue    │
                    │ (Kafka)   │
                    └───────────┘
```

---

## Tradeoffs vs Other Databases

```
┌──────────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                              │ THIS ARCH    │ MONGO ALL    │ CASSANDRA ALL│ MYSQL ALL    │
├──────────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Time range queries           │ PostgreSQL✓  │ Indexed ✓    │ Limited ✗    │ MySQL ✓      │
│ Interval overlap (conflict)  │ GIST ✓       │ Manual       │ Manual       │ Manual       │
│ Recurring event logic        │ RRULE ✓      │ Manual       │ Manual       │ Manual       │
│ Multi-user JOINs             │ Native ✓     │ Lookup ✗     │ Denorm ✗     │ Native ✓     │
│ Timezone-aware storage       │ Native ✓     │ Manual       │ Manual       │ Limited      │
│ TTL-based reminders          │ Redis ✓      │ Native ✓     │ Native ✓     │ Manual ✗     │
│ Free/busy complex queries    │ SQL ✓        │ Aggregation  │ NO ✗         │ SQL ✓        │
│ ACID transactions            │ Yes ✓        │ Limited      │ NO ✗         │ Yes ✓        │
│ Full-text search             │ GIN ✓        │ Text Index✓  │ Manual       │ Full-text ✓  │
│ Operational complexity       │ MEDIUM       │ MEDIUM       │ HIGH         │ LOW          │
│ Cost at scale                │ MEDIUM       │ MEDIUM       │ HIGH         │ LOW          │
└──────────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **PostgreSQL stores events with B-tree indexes on (user_id, start_time) enabling <10ms queries for "show my calendar this week WHERE start_time >= '2024-02-26' AND start_time < '2024-03-04'" and GIST indexes on tsrange(start_time, end_time) enabling efficient interval overlap detection for conflict checking WHERE tsrange overlaps with new event returning collisions in O(log N + K) time, while timezone-aware TIMESTAMP WITH TIME ZONE columns store events in UTC but preserve original timezone metadata (America/Los_Angeles) enabling automatic DST handling and correct "show in my timezone" conversions via AT TIME ZONE operator — recurring events use materialized instances table where RRULE (FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR) expands into 90 days of concrete instances generated by background workers enabling recurring events to be queried identically to one-time events with exception handling through is_exception boolean flag that cancels or reschedules individual occurrences without affecting the entire series — Redis stores reminders with TTL-based expiration where SETEX reminder:evt_X:user_Y ttl_seconds event_json sets keys that automatically expire at reminder time triggering keyspace notifications (CONFIG SET notify-keyspace-events Ex) that reminder service subscribes to (**keyevent@0**:expired channel) receiving expired keys as triggers to send push notifications or emails, eliminating database polling overhead and scaling to billions of reminders with sub-second accuracy versus PostgreSQL's polling approach requiring SELECT FROM reminders WHERE remind_at <= NOW() queries every minute scanning millions of rows — multi-user event coordination uses attendees junction table storing (event_id, user_id, response_status) enabling queries like "show events WHERE I'm invited AND response_status != 'declined'" with foreign keys ensuring referential integrity preventing orphaned attendees when events are deleted — free/busy lookups use recursive CTEs with window functions to merge overlapping busy intervals across multiple users' calendars then find gaps between merged intervals representing available time slots, with queries like "generate_series business_hours LEFT JOIN busy_times WHERE no overlap" returning common free times for scheduling meetings across 3+ participants' calendars.**
