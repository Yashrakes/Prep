# Multi-Step Processes Pattern

## Overview
Pattern for coordinating complex business processes involving multiple services, long-running operations, and distributed state. Ensures reliability through failures, retries, and external dependencies.

---

## When to Use This Pattern
- **Order fulfillment** - Inventory check → Payment → Shipment → Notification
- **User onboarding** - Account creation → Email verification → Profile setup → Welcome email
- **Payment processing** - Validate → Authorize → Capture → Receipt → Notification
- **Booking systems** - Check availability → Reserve → Payment → Confirmation
- **Data pipelines** - Extract → Transform → Load → Validate → Notify
- **Approval workflows** - Submit → Review → Approve/Reject → Notify
- **Multi-service transactions** - Coordinating across microservices

---

## Why This is Hard

### Challenges in Distributed Systems

**Challenge 1: Partial Failures**:
```
Order Process:
Step 1: Reserve inventory ✓ Success
Step 2: Charge payment ✗ Failure (card declined)

Now what?
- Inventory reserved but payment failed
- Need to release inventory (compensation)
- System in inconsistent state
```

**Challenge 2: Network Failures**:
```
Step: Send email notification
- Request sent to email service
- Network timeout (did it succeed or fail?)
- Cannot determine state

Retry:
- Risk sending duplicate emails
- Or risk not sending at all
```

**Challenge 3: State Management**:
```
Process spans multiple services:
- Service A holds inventory reservation
- Service B holds payment status
- Service C holds shipment status

Where is overall process state?
Who coordinates?
What if coordinator fails?
```

**Challenge 4: Long-Running Processes**:
```
Workflow takes hours or days:
- Manual approval step (human in loop)
- External API with delays (shipping carrier)
- Background processing (video encoding)

Cannot hold transactions open
Need to persist state
Need to resume after failures
```

---

## Solution Patterns

## Pattern 1: Simple Orchestration (Single Server)

### How It Works

**Concept**:
- Single service orchestrates entire workflow
- Calls other services sequentially
- Handles state and retries locally

**Architecture**:
```
Orchestrator Service
    ├→ Call Inventory Service
    ├→ Call Payment Service
    ├→ Call Shipping Service
    └→ Call Notification Service
```

**Implementation**:

**Orchestrator Code** (pseudocode):
```
function processOrder(order):
    // Step 1: Reserve inventory
    try:
        inventory.reserve(order.items)
    catch error:
        return failure("Inventory unavailable")
    
    // Step 2: Charge payment
    try:
        payment_result = payment.charge(order.payment_method, order.total)
    catch error:
        // Compensate: Release inventory
        inventory.release(order.items)
        return failure("Payment failed")
    
    // Step 3: Create shipment
    try:
        shipment = shipping.create(order.address, order.items)
    catch error:
        // Compensate: Refund payment, release inventory
        payment.refund(payment_result.id)
        inventory.release(order.items)
        return failure("Shipping failed")
    
    // Step 4: Send notification
    try:
        notification.send(order.user_id, "Order confirmed")
    catch error:
        // Non-critical, log and continue
        log("Notification failed, will retry async")
    
    return success(order)
```

**State Storage**:
```
Database table: orders

order_id | status | step | inventory_reserved | payment_id | shipment_id
123      | processing | payment_step | true | null | null

Update after each step:
- Record which step completed
- Store IDs from each service
- Use for retries/compensation
```

---

### When to Use Simple Orchestration

**Good for**:
- Simple workflows (3-5 steps)
- Low volume (< 1000/hour)
- All services synchronous (respond quickly)
- Single team owns all services
- Getting started quickly

**Not good for**:
- Complex workflows (10+ steps, branches, loops)
- High volume (needs horizontal scaling)
- Long-running steps (hours/days)
- Distributed teams/services
- Need audit trail and observability

**Pros**:
- Simple to implement and understand
- Easy to debug (single codebase)
- Can use database transactions for state

**Cons**:
- Single point of failure (orchestrator)
- Hard to scale (stateful)
- Manual retry logic
- Limited observability
- Tight coupling to all services

**Include in Design**:
- "Simple orchestrator for order processing"
- "Handles 3 steps: inventory, payment, shipping"
- "Database tracks progress, enables retries"
- "Compensating actions on failures"

---

## Pattern 2: Workflow Engine (Temporal, Step Functions)

### How It Works

**Concept**:
- Dedicated workflow engine manages state and execution
- Define workflow as code or DSL
- Engine handles retries, timeouts, state persistence
- Guarantees exactly-once execution

**Architecture**:
```
Workflow Engine (Temporal/Step Functions)
    ↓ (stores state)
Persistent Storage
    ↓ (executes tasks)
Worker Processes
    ↓ (call external services)
External Services (Inventory, Payment, etc.)
```

---

### Temporal (Durable Execution)

**Key Concepts**:

**Workflows**:
- Define business logic as code
- Look like normal functions, but durable
- Can run for seconds or months
- Survive crashes and restarts

**Activities**:
- Individual tasks (API calls, database operations)
- Can fail and retry automatically
- Idempotent execution

**Workers**:
- Execute workflow code
- Poll for tasks from Temporal server
- Stateless (can scale horizontally)

**Example Workflow** (Temporal):
```
@workflow
def OrderWorkflow(order_id):
    # Step 1: Reserve inventory
    inventory_id = execute_activity(
        reserve_inventory,
        args=[order_id],
        retry_policy={
            'max_attempts': 3,
            'backoff': 'exponential'
        }
    )
    
    # Step 2: Charge payment
    try:
        payment_id = execute_activity(
            charge_payment,
            args=[order_id],
            retry_policy={'max_attempts': 3}
        )
    except PaymentFailure:
        # Compensate: Release inventory
        execute_activity(release_inventory, args=[inventory_id])
        raise
    
    # Step 3: Create shipment
    try:
        shipment_id = execute_activity(
            create_shipment,
            args=[order_id]
        )
    except ShippingFailure:
        # Compensate: Refund payment, release inventory
        execute_activity(refund_payment, args=[payment_id])
        execute_activity(release_inventory, args=[inventory_id])
        raise
    
    # Step 4: Send notification (fire and forget)
    execute_activity_async(
        send_notification,
        args=[order_id]
    )
    
    return {
        'inventory_id': inventory_id,
        'payment_id': payment_id,
        'shipment_id': shipment_id
    }
```

**How Temporal Guarantees Execution**:
```
1. Workflow started, state persisted
2. Execute activity (reserve inventory)
3. After activity completes, event logged to history
4. Workflow code continues (deterministic replay)
5. If worker crashes:
   - New worker picks up workflow
   - Replays from history
   - Continues from last completed activity
6. Automatic retries with exponential backoff
7. Exactly-once execution guaranteed
```

**State Management**:
```
Temporal stores:
- Workflow history (all events)
- Current state
- Pending activities
- Timer state

You don't manage state - it's automatic
```

**Benefits**:
- Workflows survive crashes (durable)
- Automatic retries
- No manual state management
- Time travel debugging (replay history)
- Built-in observability

**Include in Design**:
- "Use Temporal for order processing workflow"
- "Workflow survives service restarts"
- "Automatic retries with exponential backoff"
- "Built-in compensation on failures"
- "Can handle long-running processes (hours/days)"

---

### AWS Step Functions

**How It Works**:
- Define workflow as JSON state machine (Amazon States Language)
- Managed service, no servers to manage
- Integrates with AWS services natively

**Example State Machine**:
```json
{
  "StartAt": "ReserveInventory",
  "States": {
    "ReserveInventory": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:...:function:ReserveInventory",
      "Retry": [
        {
          "ErrorEquals": ["States.ALL"],
          "MaxAttempts": 3,
          "BackoffRate": 2.0
        }
      ],
      "Catch": [
        {
          "ErrorEquals": ["States.ALL"],
          "Next": "HandleInventoryFailure"
        }
      ],
      "Next": "ChargePayment"
    },
    "ChargePayment": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:...:function:ChargePayment",
      "Retry": [{"ErrorEquals": ["States.ALL"], "MaxAttempts": 3}],
      "Catch": [
        {
          "ErrorEquals": ["PaymentFailure"],
          "Next": "ReleaseInventory"
        }
      ],
      "Next": "CreateShipment"
    },
    "ReleaseInventory": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:...:function:ReleaseInventory",
      "Next": "OrderFailed"
    },
    "CreateShipment": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:...:function:CreateShipment",
      "Next": "SendNotification"
    },
    "SendNotification": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:...:function:SendNotification",
      "End": true
    },
    "HandleInventoryFailure": {
      "Type": "Fail",
      "Error": "InventoryUnavailable"
    },
    "OrderFailed": {
      "Type": "Fail",
      "Error": "OrderProcessingFailed"
    }
  }
}
```

**Features**:
- Visual workflow designer
- Automatic retries and error handling
- Parallel execution
- Wait states (minutes to years)
- AWS service integrations (Lambda, SQS, DynamoDB, etc.)

**Pros**:
- Fully managed, no infrastructure
- Native AWS integration
- Visual designer and monitoring
- Pay per state transition

**Cons**:
- AWS-specific (vendor lock-in)
- JSON DSL (less flexible than code)
- Harder to test locally
- Can get expensive at high volume

**Include in Design**:
- "Use AWS Step Functions for order workflow"
- "JSON state machine with error handling"
- "Integrates with Lambda for each step"
- "Managed service, no ops burden"

---

## Pattern 3: Event-Driven (Saga Pattern)

### How It Works

**Concept**:
- No central orchestrator
- Each service listens for events
- Completing a step emits event for next step
- Choreographed (not orchestrated)

**Architecture**:
```
Order Service → "OrderCreated" event → Event Bus
    ↓
Inventory Service listens → Reserve inventory → "InventoryReserved" event
    ↓
Payment Service listens → Charge payment → "PaymentCharged" event
    ↓
Shipping Service listens → Create shipment → "ShipmentCreated" event
    ↓
Notification Service listens → Send email
```

**Flow**:

**Step 1: Order Created**:
```
Order Service:
- Create order record (status = pending)
- Publish event: {"type": "OrderCreated", "order_id": 123}

Event goes to message bus (Kafka, RabbitMQ, SNS)
```

**Step 2: Inventory Reserved**:
```
Inventory Service:
- Listens to "OrderCreated" events
- Reserve inventory for order
- Publish event: {"type": "InventoryReserved", "order_id": 123, "reservation_id": "xyz"}
```

**Step 3: Payment Charged**:
```
Payment Service:
- Listens to "InventoryReserved" events
- Charge payment
- If success: Publish "PaymentCharged" event
- If failure: Publish "PaymentFailed" event
```

**Step 4a: Shipment Created (Success Path)**:
```
Shipping Service:
- Listens to "PaymentCharged" events
- Create shipment
- Publish "ShipmentCreated" event
```

**Step 4b: Compensation (Failure Path)**:
```
Inventory Service:
- Listens to "PaymentFailed" events
- Release inventory reservation
- Publish "InventoryReleased" event

Order Service:
- Listens to "PaymentFailed" events
- Update order status = "failed"
```

---

### Saga Pattern Variants

**Choreography** (above example):
- No central coordinator
- Services react to events
- Decentralized

**Orchestration** (alternative):
- Saga orchestrator service
- Tells each service what to do
- Centralized control

**Choreography vs Orchestration**:

**Choreography**:
```
Pros:
- Loose coupling
- Services independent
- Easy to add new services (just listen to events)

Cons:
- Hard to understand overall flow (scattered)
- Difficult to debug (event tracing needed)
- No single place to see workflow state
```

**Orchestration**:
```
Pros:
- Clear workflow definition (one place)
- Easy to understand and debug
- Central state management

Cons:
- Tight coupling (orchestrator knows all services)
- Single point of failure
- Can become complex
```

**Recommendation**: Start with orchestration (simpler), move to choreography if needed for scale/decoupling

---

### Compensation in Sagas

**Problem**:
```
Cannot use database transactions across services
Need to undo steps if later step fails
```

**Solution**: Compensating Transactions

**Example**:

**Forward Flow**:
```
1. Reserve inventory → reserveInventory()
2. Charge payment → chargePayment()
3. Create shipment → createShipment()
```

**Compensation (if step 3 fails)**:
```
3. Shipment fails
2. Compensate payment → refundPayment()
1. Compensate inventory → releaseInventory()

Run in reverse order
```

**Each Step Has Compensation**:
```
Reserve inventory → Release inventory
Charge payment → Refund payment
Send email → (No compensation, or send cancellation email)
```

**Idempotency is Critical**:
```
Compensation may run multiple times (retries)
Must be idempotent:

refundPayment(payment_id):
    if payment already refunded:
        return success (do nothing)
    else:
        process refund
        mark as refunded
        return success
```

**Include in Design**:
- "Use Saga pattern for distributed transaction"
- "Each step has compensating action"
- "Events trigger next step or compensation"
- "Eventually consistent (not ACID)"

---

## Pattern 4: Event Sourcing

### How It Works

**Concept**:
- Store all events that happened (immutable log)
- Current state derived from events
- Complete audit trail
- Can replay events to recreate state

**Example: Order Processing**

**Events**:
```
1. OrderCreated {order_id, items, total}
2. InventoryReserved {order_id, reservation_id}
3. PaymentCharged {order_id, payment_id, amount}
4. ShipmentCreated {order_id, shipment_id}
```

**Event Store** (append-only):
```
event_id | order_id | event_type | payload | timestamp
1        | 123      | OrderCreated | {...}  | 2025-01-15 10:00
2        | 123      | InventoryReserved | {...} | 2025-01-15 10:01
3        | 123      | PaymentCharged | {...} | 2025-01-15 10:02
4        | 123      | ShipmentCreated | {...} | 2025-01-15 10:03
```

**Deriving Current State**:
```
function getOrderState(order_id):
    events = event_store.get_events(order_id)
    state = {}
    
    for event in events:
        if event.type == "OrderCreated":
            state.status = "pending"
            state.items = event.payload.items
        elif event.type == "InventoryReserved":
            state.status = "inventory_reserved"
            state.reservation_id = event.payload.reservation_id
        elif event.type == "PaymentCharged":
            state.status = "paid"
            state.payment_id = event.payload.payment_id
        elif event.type == "ShipmentCreated":
            state.status = "shipped"
            state.shipment_id = event.payload.shipment_id
    
    return state
```

**Materialized Views** (for performance):
```
Don't replay events on every read
Maintain current state in read model:

order_state table (updated by events):
order_id | status | payment_id | shipment_id | updated_at
123      | shipped | pay_xyz   | ship_abc   | 2025-01-15 10:03

On event:
1. Append to event store
2. Update materialized view

Reads are fast (no replay)
Events are source of truth
```

**Benefits**:
- Complete audit trail
- Time travel (what was state at time T?)
- Can add new projections retroactively
- Event replay for debugging

**Challenges**:
- More complex than CRUD
- Event schema evolution
- Eventually consistent reads
- Handling mistakes (can't delete events)

**Include in Design**:
- "Use event sourcing for audit requirements"
- "All state changes stored as events"
- "Materialized views for fast reads"
- "Can replay events to debug issues"

---

## Comparison of Patterns

| Pattern | Complexity | Scale | State | Use When |
|---------|-----------|-------|-------|----------|
| Simple Orchestrator | Low | Low-Med | Database | Simple workflow, starting out |
| Workflow Engine | Medium | High | Managed by engine | Complex workflow, needs durability |
| Saga (Choreography) | Medium-High | High | Distributed | Microservices, loose coupling |
| Event Sourcing | High | High | Event log | Audit trail, time travel |

---

## Handling Common Challenges

### Challenge 1: Exactly-Once Execution

**Problem**:
```
Send email notification
Network timeout (did it send?)
Retry → Risk duplicate email
```

**Solution: Idempotency**:
```
function sendEmail(email_id, user_id, message):
    // Check if already sent
    if email_log.exists(email_id):
        return success (already sent, do nothing)
    
    // Send email
    email_service.send(user_id, message)
    
    // Log that it was sent
    email_log.insert(email_id, user_id, timestamp)
    
    return success

Retry safe: Won't send duplicate if already sent
```

**Idempotency Keys**:
```
Include unique ID with each request:
POST /api/charge-payment
Body: {
    "idempotency_key": "order_123_payment_attempt_1",
    "amount": 100
}

Server:
- Check if idempotency_key already processed
- If yes, return previous result
- If no, process and store result with key
```

---

### Challenge 2: Timeouts and Retries

**Problem**:
```
Call external service, timeout after 30s
Did it succeed but response lost?
Or did it fail?
```

**Solution: Retry with Backoff**:
```
retry_policy = {
    'max_attempts': 3,
    'backoff': 'exponential',
    'max_delay': 60
}

Attempt 1: Immediate
Attempt 2: Wait 2s
Attempt 3: Wait 4s

Combined with idempotency, safe to retry
```

**Timeout Best Practices**:
```
- Set reasonable timeout (don't wait forever)
- Use exponential backoff
- Maximum retry attempts
- Different policies per step (critical vs optional)
```

---

### Challenge 3: Monitoring and Observability

**What to Track**:

**Workflow Level**:
```
- Workflows started
- Workflows completed (success/failure)
- Average duration
- Steps completed
- Current active workflows
```

**Step Level**:
```
- Step duration
- Step success/failure rate
- Retry count per step
- Compensation executions
```

**Tracing**:
```
Assign correlation ID to workflow
Pass through all services
Distributed tracing (Jaeger, Zipkin)

Can trace entire workflow across services
```

**Alerting**:
```
- Workflow failure rate > 5%
- Step duration > 2x normal
- Retry rate spike
- Compensation executions (indicates failures)
```

---

## Interview Tips

### Always Mention
1. **Pattern choice**: Justify based on complexity and scale
2. **Compensation**: How to handle failures and rollback
3. **Idempotency**: Critical for retries and reliability
4. **State management**: Where and how workflow state stored

### Deep Dive Topics
- **Saga pattern**: Choreography vs orchestration trade-offs
- **Temporal/Step Functions**: Benefits of workflow engines
- **Event sourcing**: When it makes sense
- **Retry strategies**: Exponential backoff, max attempts

### Red Flags to Avoid
- Using distributed transactions (2PC) without discussing drawbacks
- No compensation logic for failures
- Not mentioning idempotency
- Ignoring observability and monitoring

### Good Answers Include
- "Start with simple orchestrator, move to Temporal if workflow complexity grows"
- "Each step has compensating action for rollback"
- "Use idempotency keys to make retries safe"
- "Workflow engine handles state persistence and retries automatically"
- "Saga pattern with events for loose coupling in microservices"
- "Monitor workflow success rate and step durations"

---

## Example Use Cases

### E-commerce Order
```
Pattern: Temporal workflow
Steps:
1. Validate order
2. Reserve inventory
3. Charge payment (compensation: refund)
4. Create shipment (compensation: cancel shipment, refund, release inventory)
5. Send confirmation email

Why Temporal:
- Multi-step with compensations
- Needs durability (can take minutes)
- Automatic retries on transient failures
```

### User Onboarding
```
Pattern: Simple orchestrator → Step Functions
Steps:
1. Create account
2. Send verification email
3. Wait for verification (hours/days) ← Long wait
4. Setup profile
5. Send welcome email

Why Step Functions:
- Has long wait state (verification)
- Relatively simple flow
- AWS infrastructure
```

### Payment Processing
```
Pattern: Saga (orchestrated)
Steps:
1. Validate payment method
2. Authorize (hold funds)
3. Capture (charge)
4. Send receipt

Why Saga:
- Financial transaction (need compensation)
- Distributed across services
- Critical reliability
```
