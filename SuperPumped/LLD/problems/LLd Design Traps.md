

# LLD Interviews for SDE-2: Where Candidates Get Stuck (and How a Pro Thinks)

> The SDE-1 question is _"can you turn nouns into classes?"_ The SDE-2 question is _"can you make defensible engineering decisions under ambiguity, survive concurrency, and evolve the design without rewriting it?"_
> 
> Drawing classes is table stakes. You are graded on **judgment**.

---

## Part 1 — The Mindset Shift

|Dimension|SDE-1 expectation|SDE-2 expectation (what's actually scored)|
|---|---|---|
|Modeling|Maps entities to classes|Picks the _right_ abstraction boundary, justifies inheritance vs composition|
|Concurrency|"It works" (single-threaded)|Identifies race conditions _before being asked_, picks lock granularity, knows CAS vs locks|
|Extensibility|Hardcodes behavior|Open/Closed: new requirement = new class, not an `if/else` edit|
|Consistency|Mutates state freely|Knows what must be atomic, what can be eventual, where idempotency is needed|
|Scale|Assumes one JVM|Knows _exactly_ which assumptions break when you add a 2nd server/DB|
|Tradeoffs|"I chose X"|"I chose X over Y because of [throughput/consistency/complexity], and here's when I'd switch"|

The single biggest signal: **you name the tradeoff before the interviewer pokes it.** That's the whole game.

---

## Part 2 — The 8 Traps (the places people stall)

These recur in _every_ LLD problem. Memorize the trap, not the answer.

### Trap 1 — The Scoping / Requirements Trap

Candidates start coding immediately. SDE-2s spend 3–4 minutes locking scope, because the design _depends_ on the answers.

For Parking Lot, the answers that change your design:

- Multiple floors? Multiple entry/exit gates? **(→ concurrency + distributed)**
- Vehicle→spot mapping: can a bike take a car spot? **(→ allocation strategy)**
- Pricing: flat / hourly / per-vehicle-type / dynamic surge? **(→ Strategy pattern)**
- Single building or multi-location chain? **(→ in-memory vs DB vs distributed)**
- Real-time availability display? **(→ Observer + counter consistency)**

> Pro move: _"Before I model classes, let me pin 4 things, because each one flips a design decision…"_ — and then state which decision each flips. This alone separates you from 70% of candidates.

### Trap 2 — Abstraction & Class-Relationship Trap (IS-A vs HAS-A)

The classic mistake: modeling `CarSpot`, `BikeSpot`, `TruckSpot` as subclasses with behavior. There's no behavioral difference — only data (size). That's **composition over inheritance**.

- **Inheritance (IS-A):** use only when subtypes have _different behavior_ that benefits from polymorphism.
- **Composition (HAS-A):** a spot _has a_ `SpotType`; a ticket _has a_ vehicle, spot, timestamps.

Interviewer probe: _"Add a fee that depends on vehicle type."_ If you put fee logic inside `Car`/`Bike` subclasses, you've coupled pricing to the vehicle hierarchy and you'll edit those classes forever. Pull pricing into a separate `PricingStrategy`. (Trap 5.)

### Trap 3 — Concurrency / Thread-Safety Trap ⭐ (the #1 SDE-2 killer)

The moment you have ≥2 gates, "find a free spot then occupy it" is a **TOCTOU** (time-of-check to time-of-use) race. Two threads read the same spot as `FREE`, both assign it. Detailed below with code.

### Trap 4 — State Machine Trap

Candidates treat `Spot.occupied = true/false` as a flag. But real systems have **illegal transitions**. A gate _reserves_ a spot before the car physically parks; you can't occupy an already-reserved spot, can't release a free spot. Model it as an explicit state machine — interviewers love asking "what if the car never shows up?" (→ reservation timeout).

### Trap 5 — Extensibility / Open-Closed Trap

"Add dynamic pricing." "Add an EV-charging spot type." "Allocate the nearest spot instead of any spot." If each of these makes you _edit_ existing classes, you fail OCP. Each should be a _new class_ plugged into a strategy/factory.

### Trap 6 — Consistency & Atomicity Trap

What _must_ be atomic (spot claim, payment), what can be **eventually consistent** (the "342 spots free" display board)? Candidates over-lock (kill throughput) or under-lock (corrupt state). Knowing the difference is senior signal.

### Trap 7 — Distributed / Scale Trap

`AtomicBoolean` and `synchronized` **only work inside one JVM**. With multiple gate servers, your in-memory lock is meaningless. You need a DB row lock, optimistic versioning, or a distributed lock (Redis). Knowing precisely _which line of code breaks_ when you go multi-node is the strongest senior signal there is.

### Trap 8 — Database Modeling Trap

Where does spot status live — memory, DB, or both? How do you atomically decrement "available count" without two payments overselling the last spot? Idempotent payments? This is where "LLD" quietly becomes "HLD."

---

## Part 3 — Parking Lot, Worked Through Every Trap

### 3.1 Core model (composition, not inheritance)

```java
enum VehicleType { BIKE, CAR, TRUCK, EV }
enum SpotType    { SMALL, MEDIUM, LARGE, EV_CHARGING }

// HAS-A everywhere. No CarSpot/BikeSpot subclasses — they'd only differ in data.
class Vehicle {
    final String plate;
    final VehicleType type;
    Vehicle(String plate, VehicleType type) { this.plate = plate; this.type = type; }
}
```

Spot as a **state machine**, not a boolean (Trap 4):

```java
enum SpotState { FREE, RESERVED, OCCUPIED }

class ParkingSpot {
    final String id;
    final SpotType type;
    final int distanceFromGate;                 // for "nearest" allocation
    private final AtomicReference<SpotState> state =
            new AtomicReference<>(SpotState.FREE);

    ParkingSpot(String id, SpotType type, int distance) {
        this.id = id; this.type = type; this.distanceFromGate = distance;
    }

    // Atomic claim — the heart of thread safety (Trap 3).
    boolean tryReserve() {
        return state.compareAndSet(SpotState.FREE, SpotState.RESERVED);
    }
    boolean confirmOccupied() {
        return state.compareAndSet(SpotState.RESERVED, SpotState.OCCUPIED);
    }
    void release() { state.set(SpotState.FREE); }   // OCCUPIED/RESERVED -> FREE
    SpotState state() { return state.get(); }
}
```

> **Why `AtomicReference<SpotState>` and not a boolean + `synchronized`?** CAS gives me lock-free, per-spot atomic claiming. Two threads racing for the same spot: exactly one `compareAndSet` returns `true`, the loser moves on. No lock, no contention between _different_ spots. Tradeoff covered below.

---

### 3.2 The concurrency trap, shown as an evolution

This is the part interviewers dig into. Show the **wrong** version first — it proves you understand _why_ the right version exists.

#### ❌ Naive (TOCTOU race — two cars, one spot)

```java
ParkingSpot assign(VehicleType type) {
    for (ParkingSpot s : spots) {
        if (s.state() == SpotState.FREE) {   // CHECK
            s.occupy();                       // USE  <-- gap! another thread slipped in
            return s;
        }
    }
    return null;
}
```

Thread A and Thread B both read spot #42 as `FREE`, both occupy it. Two cars, one spot. **This is the bug they want you to find yourself.**

#### Option A — Coarse lock (`synchronized`)

```java
synchronized ParkingSpot assign(VehicleType type) { ... }   // whole method locked
```

- ✅ Correct, dead simple, easy to reason about.
- ❌ **Serializes every gate.** 8 gates, but only 1 can assign at a time. Throughput collapses under load. Fine for a 50-spot lot, wrong for an airport.

#### Option B — CAS on a concurrent free-list (lock-free) ⭐ recommended for single JVM

```java
class SpotAllocator {
    // One concurrent free-list per SpotType.
    private final Map<SpotType, ConcurrentLinkedQueue<ParkingSpot>> free =
            new EnumMap<>(SpotType.class);

    ParkingSpot allocate(SpotType type) {
        ParkingSpot s;
        while ((s = free.get(type).poll()) != null) {  // poll() is atomic
            if (s.tryReserve()) return s;               // CAS — exactly one winner
            // lost the race or stale entry; loop to next
        }
        return null; // genuinely full
    }
    void freeUp(ParkingSpot s) { s.release(); free.get(s.type).offer(s); }
}
```

- ✅ Different spots claimed fully in parallel — high throughput.
- ✅ No global lock; contention only on the rare true tie for the _same_ spot.
- ❌ `ConcurrentLinkedQueue` is **FIFO, not "nearest."** If "nearest spot" is a requirement, FIFO doesn't honor it.

#### Option C — Nearest-spot under concurrency (the follow-up they love)

"Nearest" needs _ordering_, which fights _concurrency_. Tradeoff:

```java
// PriorityBlockingQueue ordered by distance; still CAS-claim to resolve ties.
PriorityBlockingQueue<ParkingSpot> pq =
    new PriorityBlockingQueue<>(64, Comparator.comparingInt(s -> s.distanceFromGate));

ParkingSpot allocateNearest() {
    ParkingSpot s;
    while ((s = pq.poll()) != null) {       // poll is O(log n) and atomic
        if (s.tryReserve()) return s;        // CAS still guards the actual claim
    }
    return null;
}
```

- ✅ Gives nearest _and_ stays thread-safe (the PQ serializes the pop; CAS guards the claim).
- ❌ The PQ's internal lock is now a **contention point** — you've traded some throughput back for ordering. _Say this out loud._ "If nearest-spot matters I accept the PQ as a hotspot; if it doesn't, I use the lock-free queue for max throughput." **That sentence is the senior signal.**

> **The meta-lesson:** ordering ↔ throughput is a fundamental tension. You don't 'solve' it; you pick a side and name the cost.

---

### 3.3 Reservation timeout (Trap 4 follow-up: "car never shows up")

A `RESERVED` spot that's never confirmed leaks capacity. Senior answer: reservations expire.

```java
// On reserve, schedule a release if not confirmed within N seconds.
scheduler.schedule(() -> {
    if (spot.state() == SpotState.RESERVED) spot.release();  // CAS-safe inside release path
}, 120, TimeUnit.SECONDS);
```

Real systems use a TTL on a Redis key or a sweeper job. Mentioning _timeout/leak_ unprompted is a strong signal.

---

### 3.4 Extensibility via Strategy + Factory (Traps 2 & 5)

**Pricing — Strategy** (so "add surge pricing" = new class, zero edits):

```java
interface PricingStrategy { double price(Ticket t, Instant exit); }

class HourlyPricing implements PricingStrategy {
    public double price(Ticket t, Instant exit) {
        long h = Math.max(1, Duration.between(t.entry, exit).toHours());
        return h * rateFor(t.spot.type);
    }
}
class SurgePricing implements PricingStrategy { /* multiplies by demand */ }
class FlatDayPricing implements PricingStrategy { /* day pass */ }
```

Adding a strategy never touches `Ticket` or the gate logic. **That's OCP made concrete.**

**Allocation — also a Strategy** (nearest / random / level-balanced are swappable):

```java
interface AllocationStrategy { ParkingSpot pick(SpotType type); }
```

**Spot creation — Factory** (new `EV_CHARGING` type = one factory branch, not edits scattered everywhere).

**Availability board — Observer** (decouples display from allocation):

```java
interface AvailabilityListener { void onChange(SpotType type, int available); }
```

> Note the board can be **eventually consistent** (Trap 6) — a 200ms-stale "free count" on a sign is fine. You do _not_ lock the allocation path to keep a display perfectly in sync. Saying this shows you separate correctness-critical paths from cosmetic ones.

---

### 3.5 What must be atomic vs eventual (Trap 6, summarized)

|Operation|Requirement|Mechanism|
|---|---|---|
|Claim a spot|**Strictly atomic** (no double-assign)|CAS / DB row lock|
|Take payment|**Atomic + idempotent** (no double charge)|idempotency key + transaction|
|"Spots free" sign|Eventually consistent|async counter, no lock|
|Analytics / occupancy history|Eventually consistent|event stream|

Over-locking the cosmetic stuff is the most common over-engineering mistake.

---

### 3.6 The Distributed Trap — exactly which line breaks (Trap 7)

> Interviewer: _"Now there are 5 entry gates, each its own service. Does your design still work?"_

**No — and you should know precisely why.** `spot.tryReserve()` uses an in-JVM `AtomicReference`. Gate-server A and gate-server B have **separate** copies of that object (or separate JVMs entirely). CAS guarantees nothing across the network. The single-node thread-safety evaporates.

Three ways to fix, with tradeoffs:

**(1) DB optimistic claim — atomic conditional UPDATE** (simplest, usually best)

```sql
UPDATE spot
   SET state = 'RESERVED', version = version + 1
 WHERE id = :id AND state = 'FREE';
-- success iff rowsAffected == 1. The DB row is the single source of truth.
```

- ✅ One source of truth, no extra infra, naturally idempotent-ish via `version`.
- ❌ The DB becomes the throughput ceiling; hot rows contend.

**(2) `SELECT ... FOR UPDATE` (pessimistic row lock)**

```sql
BEGIN;
SELECT state FROM spot WHERE id = :id FOR UPDATE;   -- locks the row
-- check + update
COMMIT;
```

- ✅ Strong guarantee, intuitive.
- ❌ Holds a lock for the txn duration → lower throughput, deadlock risk if lock ordering is sloppy.

**(3) Distributed lock (Redis `SET key val NX PX ttl` / Redlock)**

- ✅ Fast, TTL gives automatic reservation-expiry for free.
- ❌ Another moving part; correctness under network partition / clock skew is genuinely hard (the famous Redlock debate). Use when DB contention is the proven bottleneck.

**The oversell problem (distributed counter):** the "available count per level" can't be a decrement in app memory — two servers both decrement the last unit and oversell.

```sql
UPDATE level SET available = available - 1
 WHERE level_id = :id AND available > 0;   -- guard in the WHERE, check rowsAffected
```

The `AND available > 0` is the entire trick — the DB enforces "never below zero" atomically. Tradeoff: that one row is a **write hotspot**; at extreme scale you shard the counter (N sub-counters, sum on read) and accept approximate reads.

> The sentence that lands the senior level: _"Single JVM, I'd use CAS for lock-free claiming. The instant we have multiple gate servers, the in-memory CAS is worthless and the source of truth must move to the DB row — `UPDATE … WHERE state='FREE'` with a rowsAffected check, or a Redis lock if DB contention becomes the bottleneck."_

---

### 3.7 Database model (Trap 8)

```
parking_lot(id, name, ...)
level(id, lot_id, available_count)          -- the contended counter
spot(id, level_id, type, state, version)    -- version = optimistic concurrency
ticket(id, spot_id, vehicle_plate, entry_ts, exit_ts, status)
payment(id, ticket_id, amount, idempotency_key UNIQUE, status)
```

- `spot.version` → optimistic locking.
- `payment.idempotency_key UNIQUE` → a retried "pay" request can't double-charge; the unique constraint makes the DB reject the second insert.
- **Memory vs DB:** for a single lot, in-memory state + async DB persistence is fine. For a multi-location chain, the **DB is the source of truth** and memory is just a cache. Which one you pick _is_ the answer to "does this scale."

---

## Part 4 — The Transferable Framework (use on any LLD)

Run this checklist out loud in every interview:

1. **Clarify scope** → name which design decision each requirement flips.
2. **Model with composition first**; reach for inheritance only for real behavioral polymorphism.
3. **Find the shared-mutable-state race** _before they ask_ → state it, then pick lock granularity (coarse lock → CAS → distributed).
4. **Make the lifecycle a state machine**; ask "what if a step never completes?" (timeouts, leaks).
5. **Isolate every "it varies" axis behind a Strategy/Factory** (pricing, allocation, matching, notification).
6. **Classify each operation:** atomic-critical vs eventually-consistent. Don't lock the cosmetic ones.
7. **Stress-test for distribution:** "which exact line assumes one JVM?" → move source of truth to DB/Redis.
8. **Sketch the schema** with the atomicity guard (`WHERE count > 0`, unique idempotency key, version column).

---

## Part 5 — Question Bank: the hidden trap in each

These are the same 8 traps wearing costumes. Once you see that, every "new" problem is familiar.

|Problem|The trap they're really testing|
|---|---|
|**Parking Lot**|Spot-claim race (TOCTOU) + nearest-vs-throughput + distributed counter oversell|
|**Elevator System**|Scheduling **strategy** (SCAN/LOOK) swappable + request queue concurrency + state machine (doors)|
|**BookMyShow / Ticketmaster**|Seat-hold race + **reservation timeout** + oversell + idempotent payment (this is Parking Lot in disguise)|
|**Splitwise**|Consistency of balances, atomic multi-party update, avoiding float errors, simplify-debts algorithm|
|**Rate Limiter**|Concurrency on the counter, **atomic increment**, token-bucket vs sliding-window tradeoff, distributed (Redis)|
|**Vending Machine**|State machine (the canonical one), illegal transitions, payment atomicity|
|**LRU / LFU Cache**|Concurrency (`ConcurrentHashMap` + locking the eviction), O(1) invariant, thread-safe doubly-linked list|
|**Logging Framework**|Async + backpressure, Strategy (appenders), thread-safe buffer, ordering guarantees|
|**Notification System**|Strategy (channels) + Observer + at-least-once delivery + idempotency|
|**Tic-Tac-Toe / Chess**|Pure abstraction & extensibility (pieces, rules as strategy) — almost no concurrency; tests OCP|
|**Distributed Job Scheduler**|Leader election, at-least-once vs exactly-once, idempotency, the _full_ distributed trap|

**Notice:** BookMyShow, Parking Lot, and Rate Limiter are the _same_ concurrency+oversell+idempotency trap. Master one deeply and you've handled a whole cluster.

---

## The One-Line Summary

> An SDE-2 LLD round is won by **naming tradeoffs before you're poked**, **finding the race before you're asked**, and **knowing the exact line of code that breaks when you add a second server.** Classes and patterns are just the vocabulary you use to say those three things.




# LLD Trap Analysis — The Other 8 Systems

> Same 8-trap framework as before: **(1)** Scoping · **(2)** Abstraction (IS-A vs HAS-A) · **(3)** Concurrency · **(4)** State Machine · **(5)** Extensibility/OCP · **(6)** Consistency/Atomicity · **(7)** Distributed · **(8)** Database
> 
> The skill is recognizing **which traps dominate** each problem. Walking in with the wrong trap in mind is how strong candidates still fail.

---

## 1. Elevator System

**Trap fingerprint:** State Machine ⭐ · Extensibility (scheduling) ⭐ · Concurrency · _(NOT a claim-race problem)_

### Killer trap A — confusing the two request types

The mistake that ends interviews: treating every request as "go to floor X." There are **two fundamentally different requests**:

- **Hall call** (external, pressed _outside_): has a floor **and a direction** ("3rd floor, going UP").
- **Cabin call** (internal, pressed _inside_): just a target floor, no direction.

Modeling them as one type means you can't do proper scheduling. Model them separately.

```java
enum Direction { UP, DOWN, IDLE }
enum ElevatorState { MOVING, STOPPED, DOORS_OPEN, MAINTENANCE }

sealed interface Request permits HallCall, CabinCall {}
record HallCall(int floor, Direction dir) implements Request {}   // from outside
record CabinCall(int floor) implements Request {}                 // from inside
```

### Killer trap B — FCFS scheduling (the naive answer)

Candidates serve requests first-come-first-served. Real elevators use **SCAN / LOOK** (keep going in one direction, serving everything en route, then reverse). Make it a **Strategy** so you can swap algorithms.

```java
interface SchedulingStrategy {
    Integer nextStop(int current, Direction dir,
                     NavigableSet<Integer> up, NavigableSet<Integer> down);
}

// LOOK: serve all stops above while going UP, then flip.
class LookStrategy implements SchedulingStrategy {
    public Integer nextStop(int cur, Direction dir,
                            NavigableSet<Integer> up, NavigableSet<Integer> down) {
        if (dir == Direction.UP) {
            Integer above = up.ceiling(cur);     // next stop at/above current
            return above != null ? above : down.floor(cur); // none above -> flip
        } else {
            Integer below = down.floor(cur);
            return below != null ? below : up.ceiling(cur);
        }
    }
}
```

- **FCFS:** simple, but a request to floor 1 right after floor 9 makes the cabin yo-yo. Terrible average wait.
- **LOOK/SCAN:** minimizes travel; standard. Tradeoff: a request _just behind_ the direction of travel waits a full sweep (starvation risk → add aging).

### The follow-up that breaks people — _"N elevators"_

Now it's a **dispatcher** problem. A hall call must be assigned to _one_ elevator. This is the only place concurrency bites (multiple hall calls + shared elevator pool).

```java
// Assign to the elevator that can serve it soonest (estimated time / nearest in-direction).
Elevator pick(HallCall c) {
    return elevators.stream()
        .min(Comparator.comparingInt(e -> e.estimatedTimeTo(c)))
        .orElseThrow();
}
```

Senior note: the dispatcher's request set is shared mutable state — guard the assignment so two dispatchers don't both hand the same call to different cabins.

---

## 2. Splitwise

**Trap fingerprint:** Consistency/Atomicity ⭐ · Money precision ⭐ · Extensibility (split types) · Algorithm (debt simplification)

### Killer trap A — using `double` for money 💀

The single most common Splitwise failure. `0.1 + 0.2 != 0.3` in floating point. Penny errors compound across thousands of expenses. **Use `BigDecimal` or store integer minor units (paise/cents).**

```java
// WRONG: double share = amount / n;   // rounding drift, money silently lost
// RIGHT:
BigDecimal[] split(BigDecimal total, int n) {
    BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
    BigDecimal[] r = new BigDecimal[n];
    Arrays.fill(r, base);
    BigDecimal remainder = total.subtract(base.multiply(BigDecimal.valueOf(n)));
    r[0] = r[0].add(remainder);   // assign leftover paise to one person — never lose money
    return r;
}
```

_That remainder line is the senior signal_ — proving you know `total` must equal the sum of shares to the paise.

### Killer trap B — split types + validation (Extensibility)

EQUAL / EXACT / PERCENT split as a **Strategy**, each with **validation** candidates forget:

```java
interface SplitStrategy { Map<User,BigDecimal> compute(BigDecimal total, ...); }
// EXACT: shares must sum to total (else reject)
// PERCENT: percents must sum to 100 (else reject)
```

Interviewers deliberately feed percents summing to 99 — if you don't validate, you fail.

### Killer trap C — concurrency on balances

Two expenses added to the same group simultaneously → both read balance, both write → lost update. Optimistic locking:

```sql
UPDATE balance SET amount = :new, version = version + 1
 WHERE user_a = :a AND user_b = :b AND version = :expected;  -- retry if 0 rows
```

### The follow-up — _"minimize transactions to settle up"_

Greedy min-cash-flow: net everyone, then repeatedly match the biggest creditor with the biggest debtor (two heaps). Brings N people from up to N² transfers down toward N−1.

---

## 3. Vending Machine

**Trap fingerprint:** State Machine ⭐⭐ (the _canonical_ one) · Atomicity (payment/change) · _(little concurrency — it's one physical user)_

### Killer trap — if/else state soup vs the **State pattern**

Candidates write one giant `switch(state)`. The interviewer is literally testing whether you know the **State design pattern**: each state is a class that handles events and returns the next state. This prevents illegal transitions structurally.

```java
interface VendingState {
    VendingState insertCoin(Machine m, Coin c);
    VendingState selectProduct(Machine m, String code);
    VendingState dispense(Machine m);
}

class IdleState implements VendingState {                 // no money yet
    public VendingState insertCoin(Machine m, Coin c){ m.add(c); return new HasMoneyState(); }
    public VendingState selectProduct(Machine m,String code){ throw new IllegalStateException("Insert money first"); }
    public VendingState dispense(Machine m){ throw new IllegalStateException(); }
}
// HasMoneyState, DispensingState, OutOfStockState ...
```

The illegal transitions (select before pay, dispense before select) are now _impossible to call wrong_ — that's the whole point.

### The follow-up — _"give change"_ and _"dispense fails mid-way"_

- **Change-making:** greedy works only for canonical coin systems; mention DP for arbitrary denominations.
- **Atomicity:** if dispense fails after payment, you must **refund** — payment + dispense is a tiny transaction. Candidates forget the failure path entirely.

---

## 4. LRU / LFU Cache

**Trap fingerprint:** Data-structure invariant (O(1)) ⭐ · Concurrency ⭐⭐ (subtler than it looks) · Distributed (as a follow-up)

### Killer trap A — O(1) eviction needs a doubly-linked list

`get`/`put` must be O(1) _including_ eviction. HashMap alone can't find the least-recently-used in O(1). Answer: **HashMap + doubly-linked list** (map value = DLL node; move-to-front on access; evict tail).

```java
class LRUCache<K,V> {
    private final int cap;
    private final Map<K,Node<K,V>> map = new HashMap<>();
    private final Node<K,V> head, tail;          // sentinels
    V get(K k){ Node<K,V> n = map.get(k); if(n==null) return null; moveToFront(n); return n.v; }
    void put(K k,V v){ /* upsert, moveToFront, if size>cap evict tail.prev */ }
}
```

### Killer trap B — the concurrency misconception 💀

> "Just use `ConcurrentHashMap`, done." **No.**

The recency ordering (the DLL) is **shared mutable state**. Even a `get` _mutates_ the list (move-to-front), so reads need write-locking. `ConcurrentHashMap` protects the map, **not the ordering**. This catches almost everyone.

Tradeoffs:

- **`synchronized` everything:** correct, but every read serializes — kills a read-heavy cache.
- **Real-world (Caffeine/Guava):** _approximate_ LRU — buffer reads, apply ordering asynchronously in batches, sample for eviction (TinyLFU). Exact-LRU-under-concurrency is too expensive. **Mentioning approximate LRU here is a strong senior signal.**

### The follow-up — _"distributed cache"_

Single JVM → multi-node: consistent hashing to shard keys, eviction handled per-node (or Redis with `maxmemory-policy allkeys-lru`). The in-memory DLL doesn't cross nodes — same "which line assumes one JVM" lesson as Parking Lot.

---

## 5. Logging Framework

**Trap fingerprint:** Concurrency + Async/Backpressure ⭐⭐ · Extensibility (appenders) ⭐ · Chain of Responsibility (levels)

### Killer trap A — synchronous logging serializes your app

Naive logger `synchronized`-writes to a file → every app thread blocks on I/O. Real frameworks log **asynchronously**: producer threads enqueue, a dedicated writer thread drains.

```java
class AsyncLogger {
    private final BlockingQueue<LogEvent> q = new ArrayBlockingQueue<>(10_000); // BOUNDED
    AsyncLogger(){ Thread w = new Thread(this::drain); w.setDaemon(true); w.start(); }
    void log(LogEvent e){ /* enqueue per backpressure policy below */ }
    private void drain(){ while(true){ try { write(q.take()); } catch(Exception ex){} } }
}
```

### Killer trap B — backpressure (the follow-up that breaks people)

> _"The queue fills up. Now what?"_

This is the senior question. A **bounded** queue forces a choice — name the tradeoff:

- **Block** (`q.put()`): no logs lost, but logging now stalls the app — the disease you tried to cure.
- **Drop** (`q.offer()`): app never blocks, but you lose logs (acceptable for DEBUG, dangerous for AUDIT).
- **Unbounded queue:** never blocks, never drops — until OOM. (The trap answer.)

The right reply: _"Depends on log level — drop DEBUG under pressure, block/never-drop AUDIT."_

### Extensibility

- **Appenders** = Strategy (console/file/network/Kafka), pluggable.
- **Log levels** = Chain of Responsibility (each handler decides handle-or-pass).
- Logger = Singleton → mention enum singleton or double-checked locking (don't get the DCL `volatile` wrong, they check).

---

## 6. Notification System

**Trap fingerprint:** Extensibility (channels) ⭐ · Delivery guarantees + Idempotency ⭐⭐ · Distributed (queue/retry)

### Killer trap A — channels as if/else

Email/SMS/Push as a **Strategy + Factory**, plus **Observer** for event→subscriber fan-out. New channel = new class.

```java
interface NotificationChannel { void send(Notification n); }
class EmailChannel implements NotificationChannel { ... }
```

### Killer trap B — "at-least-once" means duplicates → idempotency 💀

The follow-up that stalls people: _"The user got the same SMS twice."_ Retries (for reliability) cause duplicates. You need a **dedup key**:

```java
boolean firstTime = dedupStore.putIfAbsent(n.idempotencyKey(), TTL); // e.g. Redis SETNX
if (firstTime) channel.send(n);   // suppress duplicate within the TTL window
```

- **At-least-once** (retry till ack): never lost, may duplicate → _requires_ idempotency.
- **At-most-once** (fire once): no dupes, may lose → unacceptable for OTPs.
- **Exactly-once:** practically unachievable end-to-end; you _approximate_ it with at-least-once + dedup ("effectively once"). Saying this plainly is senior signal.

### Distributed shape

Producer → **queue (Kafka/SQS)** → worker pool → channel, with **retry + exponential backoff + Dead Letter Queue** for poison messages. Plus user preferences / quiet-hours / rate limits per user.

---

## 7. Tic-Tac-Toe / Chess

**Trap fingerprint:** Abstraction & Extensibility ⭐⭐ · State Machine (game status) · **Almost ZERO concurrency** — and that's the lesson.

### The meta-lesson

This problem tests _clean OOP_, not threads. **Over-engineering with concurrency here is itself a red flag.** Read the problem's fingerprint before reaching for locks.

### Killer trap A — hardcoding 3×3 and scanning the whole board

Generalize to **N×N, K-in-a-row**, and make win-detection **O(1) per move** instead of O(N²) board scans.

```java
// Track running counts per row/col/diag; update only the affected lines on each move.
int[] rows, cols; int diag, anti;
boolean place(int r,int c,int player){     // player = +1 or -1
    int v = player;
    rows[r]+=v; cols[c]+=v;
    if(r==c) diag+=v;
    if(r+c==n-1) anti+=v;
    return Math.abs(rows[r])==n || Math.abs(cols[c])==n
        || Math.abs(diag)==n   || Math.abs(anti)==n;     // O(1) win check
}
```

### Killer trap B (Chess) — piece movement as Strategy

Each piece's move rules = its own class (polymorphism), not a giant `switch(pieceType)`. Adding a fairy-chess piece = new class, zero edits. This is the clean **IS-A** case (genuine behavioral difference), unlike Parking Lot spots.

```java
interface MovementStrategy { boolean isValid(Cell from, Cell to, Board b); }
class KnightMovement implements MovementStrategy { ... }
class BishopMovement implements MovementStrategy { ... }
```

Game status (IN_PROGRESS / WIN / DRAW / STALEMATE) is the only state machine; no atomicity, no distribution.

---

## 8. Distributed Job Scheduler

**Trap fingerprint:** **ALL the distributed traps** ⭐⭐⭐ — this is the graduation problem. Leader election · At-least-once + Idempotency · Fault tolerance · DB optimistic claim.

### Killer trap A — the single scheduler is a SPOF

Candidates draw one scheduler. _"It crashes — now what?"_ You need multiple schedulers, but then **two might run the same job**. Resolve with **leader election** (ZooKeeper/etcd) _or_ per-job optimistic claim — which is the _exact same `UPDATE … WHERE` pattern from Parking Lot_:

```sql
UPDATE job
   SET status='RUNNING', owner=:worker, lease_until=now()+interval '30s', version=version+1
 WHERE id=:id AND status='READY';     -- exactly one worker wins (rowsAffected==1)
```

### Killer trap B — worker dies mid-job (the lease + heartbeat)

If a worker crashes after claiming, the job is stuck `RUNNING` forever. Solution: **lease with TTL + heartbeat** (SQS-style visibility timeout).

```
worker claims job -> sets lease_until -> heartbeats every 10s to extend lease
if worker dies -> lease expires -> a sweeper resets status='READY' -> job re-runs
```

Consequence: a job **can run twice** (worker was slow, not dead) → **jobs must be idempotent**. This chain — _lease → re-run → idempotency_ — is the senior insight.

### Killer trap C — delivery semantics

- **At-least-once** (lease + retry): default, requires idempotent jobs.
- **Exactly-once:** practically impossible; achieve **effectively-once** via idempotency keys / dedup table.

### Scale

- Scheduling at scale: a **hashed timing wheel** beats a global `PriorityQueue` for millions of timers (O(1) tick vs O(log n) per op).
- Components: job store (DB) · ready-queue (Kafka/Redis) · worker pool · sweeper for expired leases · cron parser for recurring jobs.

---

## The Trap-Intensity Matrix (study this, not the individual problems)

|System|Scope|Abstraction|Concurrency|State Machine|Extensibility|Consistency|Distributed|DB|
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
|Parking Lot / BMS / Rate Limiter|●●|●|●●●|●●|●●|●●●|●●●|●●|
|Elevator|●●|●|●●|●●●|●●●|–|●|–|
|Splitwise|●|●●|●●|–|●●|●●●|●●|●●|
|Vending Machine|●|●|●|●●●|●●|●●|–|–|
|LRU / LFU Cache|–|●|●●●|–|●|●●|●●|–|
|Logging Framework|●|●|●●●|–|●●●|●●|●|–|
|Notification System|●|●|●●|–|●●●|●●●|●●●|●|
|Tic-Tac-Toe / Chess|●|●●●|–|●●|●●●|–|–|–|
|Distributed Job Scheduler|●●|●|●●|●●|●●|●●●|●●●|●●●|

_●●● = the problem is fundamentally about this · ● = present · – = essentially absent_

### The three lessons in this matrix

1. **Read the fingerprint first.** Don't hunt for a TOCTOU race in Tic-Tac-Toe or a clever data structure in a Job Scheduler.
2. **The same `UPDATE … WHERE status='X'` optimistic-claim pattern** powers Parking Lot, BookMyShow, _and_ the Job Scheduler. One pattern, three problems.
3. **The same lease → retry → idempotency chain** appears in Notifications and the Job Scheduler. Master it once.

> Over-engineering is graded as harshly as under-engineering. Adding distributed locks to Tic-Tac-Toe signals you can't read a problem. Match the design to the fingerprint.





# Class Responsibility & Relationships — How Classes Should Connect

Two questions decide whether a class diagram looks junior or senior:

1. **What is each class responsible for?** (cohesion / single reason to change)
2. **How do classes point at each other?** (dependency → association → aggregation → composition → inheritance)

This guide covers both with real-world examples and the decision rules.

---

## Part A — A Class's Responsibility

### The Single Responsibility Principle (SRP)

> A class should have **one reason to change** — one axis of responsibility.

"Reason to change" = a **stakeholder/actor**. If pricing analysts, DBAs, and the marketing team would each _independently_ force you to edit the same class, that class has three responsibilities. Split it.

### The classic violation — the bloated `Order`

```java
// ❌ Four reasons to change → four responsibilities crammed into one class
class Order {
    List<Item> items;
    Money total()              { ... }   // domain rule    — changes when pricing changes
    void saveToDB()            { ... }   // persistence    — changes when DB schema changes
    void sendConfirmationMail(){ ... }   // notification   — changes when email changes
    byte[] toInvoicePdf()      { ... }   // presentation   — changes when layout changes
}
```

Split by responsibility (and by _who_ asks for the change):

```java
class Order          { List<LineItem> items; Money total(){...} }  // domain only
class OrderRepository{ void save(Order o){...} }                   // persistence
class EmailService   { void sendConfirmation(Order o){...} }       // notification
class InvoiceRenderer{ byte[] toPdf(Order o){...} }                // presentation
```

Now a change to the PDF layout touches exactly one class, and `Order` stays about _being an order_.

### Three principles that decide WHO owns a responsibility (GRASP)

**Information Expert** — give a responsibility to the class that holds the data needed for it.

> Who computes the order total? `Order` — it holds the line items. Not some external `TotalCalculator` that has to reach into the order's guts.

**Creator** — the class that contains / aggregates / closely uses an object should create it.

> `Order` creates its `LineItem`s (this is composition — see Part B).

**Tell, Don't Ask** — don't pull an object's data out to make decisions _about_ it; tell it to do the thing, so the invariant lives where the data lives.

```java
// ❌ Ask: extract state, decide externally — the rule leaks out of Account
if (account.getBalance() >= amt) account.setBalance(account.getBalance() - amt);

// ✅ Tell: the object guards its own invariant
account.withdraw(amt);   // throws InsufficientFundsException internally
```

**The litmus for cohesion:** can you describe the class's job in one sentence without "and"? "An `Order` represents a customer's order _and_ saves itself _and_ emails the customer" → three jobs → split.

---

## Part B — How Classes Connect (weakest → strongest coupling)

Same six relationships as the diagram, now with code and the _real-world intuition_ for each.

### 1. Dependency — "uses-a" (transient)

One class uses another momentarily: as a method parameter, local variable, or return type. **No stored reference.** Weakest coupling.

```java
class ReportService {
    void export(Report r, Printer printer) {   // Printer used, not held
        printer.print(r.render());
    }
}
```

> Real world: you _use_ a printer to print a page — you don't _own_ it. Swap the printer and `ReportService` barely notices.

### 2. Association — "knows-a" (independent peers)

A persistent reference between independent objects. No whole-part feeling; often bidirectional.

```java
class Doctor  { private List<Patient> patients; }
class Patient { private List<Doctor>  doctors;  }   // peers; each exists without the other
```

> Real world: doctors and patients. Both exist independently; they just reference each other.

### 3. Aggregation — "has-a" (shared part, **independent lifecycle**)

A whole-part relationship where the **part lives on its own and can be shared** across wholes. The whole does _not_ own the part's lifecycle. In code, the part is typically **injected from outside**.

```java
class Team {
    private final List<Player> roster = new ArrayList<>();
    void sign(Player p) { roster.add(p); }   // p created elsewhere, owned elsewhere
}
// The same Player can also be on the national team.
// Disband the Team → the Players still exist.
```

> Real world: Team↔Player, Playlist↔Song, University↔Professor, Library↔Book. **Litmus:** does the part outlive the whole? can it be shared? → **aggregation** (hollow diamond ◇).

### 4. Composition — "has-a" (exclusive part, **bound lifecycle**)

A whole-part relationship where the **part cannot exist without the whole, isn't shared, and is destroyed with it.** In code, the whole typically **creates the part internally** and holds the only reference.

```java
class Order {
    private final List<LineItem> items = new ArrayList<>();
    void addLine(Product p, int qty) {
        items.add(new LineItem(p, qty));   // Order creates AND solely owns the line
    }
}
// A LineItem has no meaning outside its Order; not shared across orders.
// Delete the Order → its LineItems vanish.
```

> Real world: House◆Room, Order◆LineItem, Human◆Heart, Book◆Page. **Litmus:** destroy the whole ⇒ the part must die too? part never shared? → **composition** (filled diamond ◆).

### 5. Inheritance — "is-a" (specialization)

A subclass IS-A superclass and is fully substitutable for it (Liskov Substitution).

```java
abstract class Account { abstract Money interest(); }
class SavingsAccount extends Account { Money interest(){...} }  // SavingsAccount IS-AN Account
```

> Real world: a savings account _is an_ account; a dog _is an_ animal. **Use sparingly** — only when subtypes have genuinely different behavior worth polymorphism. For pure code reuse, prefer composition (see the trap below).

### 6. Realization — "can-do / behaves-as" (fulfills a contract)

A class implements an interface — it plays a role.

```java
interface PaymentMethod { void pay(Money m); }
class UpiPayment        implements PaymentMethod { public void pay(Money m){...} }
class CreditCardPayment implements PaymentMethod { public void pay(Money m){...} }
```

> Real world: a credit card and UPI both _can do_ payment. The role is shared; the implementations differ.

---

## The Two Confusions Interviewers Probe

### Aggregation vs Composition

In code they look **identical** — both are just fields holding references. The difference is **semantic: lifecycle ownership + exclusivity.** Ask two questions:

1. _If I destroy the whole, must the part die too?_ → yes = composition
2. _Can the part be shared by multiple wholes / exist on its own?_ → yes = aggregation

**Code heuristic:**

- The whole **`new`s the part inside itself** and keeps the only reference → **composition** (`Order` makes its `LineItem`s).
- The part is **passed in** via constructor/setter, created and managed elsewhere → **aggregation** (`Team` is handed `Player`s).

> In an interview, say it out loud: _"I'm modeling this as composition because a line item has no independent lifecycle — it's created with the order and meaningless without it."_ That sentence is the senior signal.

### Inheritance vs Composition ("favor composition over inheritance")

Inheritance is rigid: **compile-time-fixed, single-parent (in Java), exposes parent internals, and is fragile** (a base-class change silently breaks every subclass). Composition is **runtime-swappable, flexible, and hides internals.**

The textbook mistake:

```java
// ❌ A Stack is NOT substitutable for an ArrayList — it inherits add(index, e),
//    get(index), etc., which violate stack semantics (LSP violation).
class Stack<E> extends ArrayList<E> { ... }

// ✅ A Stack HAS-A list; it exposes only push/pop/peek.
class Stack<E> {
    private final List<E> elements = new ArrayList<>();
    void push(E e){ elements.add(e); }
    E pop(){ return elements.remove(elements.size()-1); }
}
```

> Rule of thumb: reach for inheritance only when the answer to "is every subclass _truly_ an X, usable anywhere an X is expected?" is an unqualified yes. Otherwise compose.

---

## Part C — One Domain, Every Relationship (E-commerce Order)

Watch how a single realistic domain uses _all_ of them. This is exactly the kind of model an interviewer wants to see you reason toward.

|Connection|Relationship|Why|
|---|---|---|
|`Order` → `Customer`|Association|the order references its customer; the customer exists independently|
|`PremiumCustomer` ▷ `Customer`|Inheritance|a premium customer _is a_ customer with extra behavior|
|`Order` ◆ `LineItem`|Composition|line items are created by, owned by, and die with the order|
|`LineItem` → `Product`|Association|references a catalog product; the product is shared across many orders|
|`Order` ⇢ `PaymentGateway`|Dependency|used transiently at checkout, never stored|
|`CreditCard`, `Upi` ⊳ `PaymentMethod`|Realization|each fulfills the payment-method contract|

```java
class Order {                                  // RESPONSIBILITY: represent an order, enforce its rules
    private final Customer customer;                       // association
    private final List<LineItem> lines = new ArrayList<>();// composition (Order owns the lines)

    Order(Customer customer) { this.customer = customer; }

    void addLine(Product p, int qty) {                     // Creator: Order builds its own parts
        lines.add(new LineItem(p, qty));
    }

    Money total() {                                        // Information Expert: Order has the lines
        return lines.stream()
                    .map(LineItem::subtotal)
                    .reduce(Money.ZERO, Money::add);
    }

    void checkout(PaymentGateway gateway) {                // dependency: used, not held
        gateway.charge(total());
    }
}

class LineItem {                               // RESPONSIBILITY: one line = qty × product price
    private final Product product;             // association — the line does NOT own the product
    private final int qty;

    LineItem(Product product, int qty) { this.product = product; this.qty = qty; }
    Money subtotal() { return product.price().times(qty); }  // Tell, Don't Ask
}

// Persistence and presentation are SEPARATE responsibilities (SRP) — not on Order.
class OrderRepository { void save(Order o) { ... } }
class InvoiceRenderer { byte[] toPdf(Order o) { ... } }
```

Notice: `Order` _owns_ its `LineItem`s (composition — they're an internal `new`), but only _references_ `Customer` and `Product` (association — they live independently and are shared). That distinction is the whole lesson.

---

## Part D — The Decision Flowchart (run this every time)

**Picking the relationship:**

1. Is it a true **IS-A** that's substitutable anywhere the parent is used? → **Inheritance** _(then sanity-check: would composition be more flexible?)_
2. Otherwise it's **HAS-A** — which kind?
    - Part's life is bound to the whole **and** never shared → **Composition** (the whole `new`s it).
    - Part is independent / shareable → **Aggregation** (the part is injected).
    - Independent peers, no whole-part feeling → **Association**.
    - Used only momentarily (parameter / local) → **Dependency**.
3. Is it a **role / contract** the class fulfills? → **Interface (Realization)**.

**Assigning responsibility:**

- Give each class **one reason to change** (one stakeholder).
- Put each responsibility on the class that **owns the data** (Information Expert).
- Let the **container create its owned parts** (Creator).
- **Tell objects what to do**; don't extract their data to decide for them (Tell, Don't Ask).

> The senior habit isn't memorizing these — it's _narrating the choice_: "This is composition, not aggregation, because the part's lifecycle is bound to the whole." Saying the reason out loud is what separates a strong design round from a passable one.