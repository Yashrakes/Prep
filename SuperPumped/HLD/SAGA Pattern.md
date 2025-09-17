# Index

1. [[#Introduction]]
	- [[#Setting the Foundation Distributed Transactions]]
	- [[#The Challenge Transactions Across Microservices]]
2. [[#The Saga Pattern A Solution for Distributed Transactions]]
3. [[#Types of Saga Implementations]]
	- [[#Orchestration-Based Saga]]
	- [[#Choreography-based Saga]]
		- [[#Code Example]]
		- [[#Dry Run]]
4. [[#Conclusion]]
5. [[#Interview Questions]]

---
# Introduction
## Setting the Foundation: Distributed Transactions

- Imagine we're building an e-commerce system with several microservices:
	- Order Service
	- Payment Service
	- Inventory Service
	- Shipping Service

- When a customer places an order, we need to:
	- Create an order record
	- Process the payment
	- Update inventory
	- Schedule shipping

- In a **monolithic** application, we could wrap all these operations in a single database transaction. If any step fails, we'd roll back the entire transaction, maintaining data consistency.

- Let's see a simplified example of how this would look in a monolithic application:

``` java 
@Transactional
public void processOrder(Order order) {
    // Create order record
    orderRepository.save(order);
    
    // Process payment
    paymentService.processPayment(order.getPaymentDetails(), order.getTotalAmount());
    
    // Update inventory
    for (OrderItem item : order.getItems()) {
        inventoryService.reduceStock(item.getProductId(), item.getQuantity());
    }
    
    // Schedule shipping
    shippingService.scheduleDelivery(order);
}
```

- With the `@Transactional` annotation, if any step fails, the entire transaction is rolled back automatically. But this approach works only when all operations are within a single database.

---

## The Challenge: Transactions Across Microservices

- Now, let's redesign this as microservices:

``` java
public void processOrder(Order order) {
    // Create order record locally
    orderRepository.save(order);
    
    // Call Payment Service API
    paymentServiceClient.processPayment(order.getId(), order.getPaymentDetails(), order.getTotalAmount());
    
    // Call Inventory Service API
    for (OrderItem item : order.getItems()) {
        inventoryServiceClient.reduceStock(item.getProductId(), item.getQuantity());
    }
    
    // Call Shipping Service API
    shippingServiceClient.scheduleDelivery(order.getId(), order.getShippingAddress());
}
```

- Here's where we encounter a serious problem. What happens if the payment succeeds, but the inventory update fails? We've already charged the customer but can't fulfill their order. The payment needs to be refunded, but there's no automatic mechanism to handle this.

- This is the distributed transaction problem. Each service has its own database, and we can't use a simple database transaction across all of them.

- The traditional approach to solve this would be using the **two-phase commit** protocol:
	- **Prepare phase**: A coordinator asks all services if they can commit the transaction
	- **Commit phase**: If all services agree, the coordinator tells them to commit; otherwise, it tells them to abort

- But **two-phase commit** has significant **drawbacks**:
	- It's synchronous and blocking
	- It creates tight coupling between services
	- It's vulnerable to coordinator failures
	- It doesn't scale well in distributed systems

- This is exactly where the Saga pattern comes in.

---

# The Saga Pattern: A Solution for Distributed Transactions

- The Saga pattern breaks down a distributed transaction into a sequence of **local transactions**, where each transaction updates data within a single service. After each local transaction completes, it triggers the next transaction in the sequence.

- Crucially, if a transaction fails, the saga executes **compensating transactions** to undo the changes made by the preceding transactions.

## Example: Order Processing Saga

- Let's redesign our order processing flow using the Saga pattern:
	1. **Order Service**: Create order with status "PENDING"
	2. **Payment Service**: Process payment
	3. **Inventory Service**: Update inventory
	4. **Shipping Service**: Schedule delivery
	5. **Order Service**: Update order status to "COMPLETED".
- If any step fails, we execute compensating transactions:
	- If **Shipping fails**: Cancel shipping, restore inventory, refund payment, update order status to "FAILED"
	- If **Inventory fails**: Refund payment, update order status to "FAILED"
	- If **Payment fails**: Update order status to "FAILED".

---

# Types of Saga Implementations

- Let's apply the Saga pattern to a flight booking system with these services:
	- Booking Service
	- Payment Service
	- Flight Inventory Service
	- Notification Service

### The Scenario
- A customer wants to book a flight, which requires:
	1. Reserving a seat
	2. Processing payment
	3. Issuing a ticket
	4. Sending a confirmation email

---

## Orchestration-Based Saga

- A central coordinator (orchestrator) explicitly manages the saga workflow.
- The orchestrator invokes each service’s local transaction in sequence.
- In case of failure, the orchestrator issues compensating commands to previous services.

``` java
// BookingSaga.java
public class BookingSaga {
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final FlightInventoryService flightInventoryService;
    private final NotificationService notificationService;
    
    public BookingSaga(BookingService bookingService, 
                      PaymentService paymentService,
                      FlightInventoryService flightInventoryService,
                      NotificationService notificationService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.flightInventoryService = flightInventoryService;
        this.notificationService = notificationService;
    }
    
    public void process(BookingRequest request) {
        BookingDTO booking = null;
        PaymentDTO payment = null;
        TicketDTO ticket = null;
        
        try {
            // Step 1: Create booking record
            booking = bookingService.createBooking(request.getCustomerId(), 
                                                 request.getFlightId(),
                                                 request.getSeatClass());
            
            // Step 2: Reserve seat in inventory
            boolean seatReserved = flightInventoryService.reserveSeat(request.getFlightId(), 
                                                                     request.getSeatClass());
            if (!seatReserved) {
                throw new SagaException("Failed to reserve seat");
            }
            
            // Step 3: Process payment
            payment = paymentService.processPayment(request.getCustomerId(),
                                                  booking.getId(),
                                                  booking.getAmount(),
                                                  request.getPaymentDetails());
            
            // Step 4: Issue ticket
            ticket = bookingService.issueTicket(booking.getId(), payment.getId());
            
            // Step 5: Send confirmation
            notificationService.sendBookingConfirmation(booking.getId(), request.getCustomerId());
            
            // Step 6: Update booking status
            bookingService.updateStatus(booking.getId(), BookingStatus.CONFIRMED);
            
        } catch (Exception e) {
            // Execute compensating transactions
            compensate(booking, payment, ticket);
            throw new BookingFailedException("Booking failed: " + e.getMessage());
        }
    }
    
    private void compensate(BookingDTO booking, PaymentDTO payment, TicketDTO ticket) {
        // Compensating transactions in reverse order
        
        // Step 6: No need to compensate for status update
        
        // Step 5: No need to compensate for notification
        
        // Step 4: Cancel ticket if it was issued
        if (ticket != null) {
            bookingService.cancelTicket(ticket.getId());
        }
        
        // Step 3: Refund payment if it was processed
        if (payment != null) {
            paymentService.refundPayment(payment.getId());
        }
        
        // Step 2: Release seat reservation if it was made
        if (booking != null) {
            flightInventoryService.releaseSeat(booking.getFlightId(), booking.getSeatClass());
        }
        
        // Step 1: Update booking status to FAILED
        if (booking != null) {
            bookingService.updateStatus(booking.getId(), BookingStatus.FAILED);
        }
    }
}
```
---

## Choreography-based Saga

- There is no central coordinator.
- Each service listens for events and decides whether to perform its local transaction or to execute a compensating action.
- Services communicate via an event bus or messaging system.
### Code Example

``` java
// BookingService.java
public class BookingService {
    private final BookingRepository bookingRepository;
    private final EventPublisher eventPublisher;
    
    public BookingDTO createBooking(BookingRequest request) {
        BookingDTO booking = new BookingDTO();
        booking.setStatus(BookingStatus.PENDING);        
        // other setters
        bookingRepository.save(booking);
        
        // Publish event to start the saga
        eventPublisher.publish(new BookingCreatedEvent(booking));
        
        return booking;
    }
    
    // Method called when receiving BookingConfirmedEvent
    public void confirmBooking(BookingConfirmedEvent event) {
        BookingDTO booking = bookingRepository.findById(event.getBookingId());
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }
    
    // Method called when receiving BookingFailedEvent
    public void failBooking(BookingFailedEvent event) {
        BookingDTO booking = bookingRepository.findById(event.getBookingId());
        booking.setStatus(BookingStatus.FAILED);
        booking.setFailureReason(event.getReason());
        bookingRepository.save(booking);
    }
    
    // Other methods...
}
```


``` java
// FlightInventoryService.java
public class FlightInventoryService {
    private final InventoryRepository inventoryRepository;
    private final EventPublisher eventPublisher;
    private final EventSubscriber eventSubscriber;
    
    public FlightInventoryService(EventSubscriber eventSubscriber) {
        // Subscribe to events
        eventSubscriber.subscribe(BookingCreatedEvent.class, this::handleBookingCreated);
        eventSubscriber.subscribe(PaymentFailedEvent.class, this::handlePaymentFailed);
    }
    
    private void handleBookingCreated(BookingCreatedEvent event) {
        try {
            boolean reserved = reserveSeat(event.getFlightId(), event.getSeatClass());
            
            if (reserved) {
                // Publish event for the next step
                eventPublisher.publish(new SeatReservedEvent(event.getBookingId(), 
                                                           event.getFlightId(),
                                                           event.getSeatClass()));
            } else {
                // Publish failure event
                eventPublisher.publish(new BookingFailedEvent(event.getBookingId(), 
                                                            "No seats available"));
            }
        } catch (Exception e) {
            eventPublisher.publish(new BookingFailedEvent(event.getBookingId(), 
                                                        "Seat reservation failed: " + e.getMessage()));
        }
    }
    
    private void handlePaymentFailed(PaymentFailedEvent event) {
        // Compensating transaction
        releaseSeat(event.getFlightId(), event.getSeatClass());
        // publish failed event
    }
    
    // Other methods...
}
```


``` java
// PaymentService.java (continued)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;
    private final EventSubscriber eventSubscriber;
    
    public PaymentService(EventSubscriber eventSubscriber) {
        // Subscribe to events
        eventSubscriber.subscribe(SeatReservedEvent.class, this::handleSeatReserved);
        eventSubscriber.subscribe(TicketIssuanceFailedEvent.class, this::handleTicketIssuanceFailed);
    }
    
    private void handleSeatReserved(SeatReservedEvent event) {
        try {
            // Retrieve booking information
            //BookingDTO booking = bookingClient.getbookingdata() ...
            
            // Process payment
            PaymentDTO payment = processPayment(booking.getCustomerId(), 
                                              booking.getId(), 
                                              booking.getAmount());
            
            // Publish success event for next step
            eventPublisher.publish(new PaymentCompletedEvent(event.getBookingId(), 
                                                           payment.getId()));
        } catch (Exception e) {
            // Publish failure event
            eventPublisher.publish(new PaymentFailedEvent(event.getBookingId(), 
                                                       event.getFlightId(),
                                                       event.getSeatClass(),
                                                       "Payment failed: " + e.getMessage()));
        }
    }
    
    private void handleTicketIssuanceFailed(TicketIssuanceFailedEvent event) {
        // Compensating transaction
        refundPayment(event.getPaymentId());
    }
    
    // Other methods...
}
```


``` java
// TicketingService.java
public class TicketingService {
    private final TicketRepository ticketRepository;
    private final EventPublisher eventPublisher;
    private final EventSubscriber eventSubscriber;
    
    public TicketingService(EventSubscriber eventSubscriber) {
        // Subscribe to events
        eventSubscriber.subscribe(PaymentCompletedEvent.class, this::handlePaymentCompleted);
    }
    
    private void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            // Issue ticket
            TicketDTO ticket = issueTicket(event.getBookingId(), event.getPaymentId());
            
            // Publish success event for next step
            eventPublisher.publish(new TicketIssuedEvent(event.getBookingId(), 
                                                       ticket.getId()));
        } catch (Exception e) {
            // Publish failure event
            eventPublisher.publish(new TicketIssuanceFailedEvent(event.getBookingId(),
                                                               event.getPaymentId(),
                                                               "Ticket issuance failed: " + e.getMessage()));
        }
    }
    
    // Other methods...
}
```


``` java
// NotificationService.java
public class NotificationService {
    private final EventSubscriber eventSubscriber;
    
    public NotificationService(EventSubscriber eventSubscriber) {
        // Subscribe to events
        eventSubscriber.subscribe(TicketIssuedEvent.class, this::handleTicketIssued);
        eventSubscriber.subscribe(BookingFailedEvent.class, this::handleBookingFailed);
    }
    
    private void handleTicketIssued(TicketIssuedEvent event) {
        try {
            // Retrieve booking and customer information
            //BookingDTO booking = bookingRepository
            
            // Send confirmation email
            sendBookingConfirmation(booking.getId(), booking.getCustomerId());
            
            // Publish completion event
            eventPublisher.publish(new BookingConfirmedEvent(event.getBookingId()));
        } catch (Exception e) {
            // Even if notification fails, we don't need to roll back previous steps
            // Just log the error and consider the booking still successful
            logger.error("Failed to send notification: " + e.getMessage());
            eventPublisher.publish(new BookingConfirmedEvent(event.getBookingId()));
        }
    }
    
    private void handleBookingFailed(BookingFailedEvent event) {
        // Send failure notification to customer
        BookingDTO booking = bookingRepository.findById(event.getBookingId());
        sendBookingFailureNotification(booking.getId(), booking.getCustomerId(), event.getReason());
    }
    
    // Other methods...
}
```


---

### Dry Run

#### Successful Scenario

1. **Booking Creation**:
    - Customer initiates booking for Flight F123, Economy class
    - BookingService creates a booking record with status "PENDING"
    - BookingService publishes a `BookingCreatedEvent`
2. **Seat Reservation**:
    - FlightInventoryService receives the `BookingCreatedEvent`
    - FlightInventoryService reserves a seat on Flight F123
    - FlightInventoryService publishes a `SeatReservedEvent`
3. **Payment Processing**:
    - PaymentService receives the `SeatReservedEvent`
    - PaymentService processes the payment ($200)
    - PaymentService publishes a `PaymentCompletedEvent`
4. **Ticket Issuance**:
    - TicketingService receives the `PaymentCompletedEvent`
    - TicketingService issues a ticket
    - TicketingService publishes a `TicketIssuedEvent`
5. **Notification**:
    - NotificationService receives the `TicketIssuedEvent`
    - NotificationService sends a confirmation email
    - NotificationService publishes a `BookingConfirmedEvent`
6. **Booking Confirmation**:
    - BookingService receives the `BookingConfirmedEvent`
    - BookingService updates the booking status to "CONFIRMED"

#### Failure Scenario: Payment Failure

1. **Booking Creation**:
    - Customer initiates booking for Flight F123, Economy class
    - BookingService creates a booking record with status "PENDING"
    - BookingService publishes a `BookingCreatedEvent`
2. **Seat Reservation**:
    - FlightInventoryService receives the `BookingCreatedEvent`
    - FlightInventoryService reserves a seat on Flight F123
    - FlightInventoryService publishes a `SeatReservedEvent`
3. **Payment Processing**:
    - PaymentService receives the `SeatReservedEvent`
    - PaymentService attempts to process payment but fails (insufficient funds)
    - PaymentService publishes a `PaymentFailedEvent`
4. **Compensating Transactions**:
    - FlightInventoryService receives the `PaymentFailedEvent`
    - FlightInventoryService releases the reserved seat
    - BookingService receives the `BookingFailedEvent`
    - BookingService updates the booking status to "FAILED"
5. **Failure Notification**:
    - NotificationService receives the `BookingFailedEvent`
    - NotificationService sends a failure notification email

---

# Conclusion

### **Why Was Saga Needed?**

- **Traditional Distributed Transactions (e.g., 2PC):**  
    Were impractical in loosely coupled microservices due to complexity, performance overhead, and blocking behavior.
- **Saga Provides:**  
    A non-blocking, asynchronous way to achieve eventual consistency across services, handling failures gracefully with compensating transactions.

### **Summary:**

- **Purpose:**  
    Saga addresses the challenge of coordinating distributed transactions in a microservices architecture.
- **Approach:**  
    It breaks a distributed transaction into a series of local transactions with compensations in case of failures.
- **Types:**
    - **Orchestration-Based:** Centralized control.
    - **Choreography-Based:** Decentralized, event-driven.
---

# Interview Questions

### **Q1: What is the Saga pattern, and why is it needed in microservices?**

**Model Answer:**  
The Saga pattern is a design approach for managing distributed transactions in microservices. Instead of using a single, monolithic ACID transaction—which isn’t practical across multiple autonomous services—a saga breaks the workflow into a sequence of local transactions. Each local transaction updates its service and publishes an event to trigger the next transaction. If a step fails, compensating transactions are executed to roll back the previous actions. This pattern is essential in microservices to avoid the pitfalls of distributed transactions, such as the complexity and performance overhead of two-phase commit (2PC), while still ensuring eventual consistency.

---

### **Q2: Can you explain the two primary types of Saga patterns and how they differ?**

**Model Answer:**  
There are two common types of Saga patterns:

- **Orchestration-Based Saga:**  
    A central coordinator (or orchestrator) manages the saga’s workflow. It explicitly invokes each local transaction in sequence and, if a step fails, instructs the necessary compensating actions.  
    _Example:_ In an order processing workflow, the orchestrator calls the Inventory Service to reserve items, then the Payment Service to process the charge, and finally the Shipping Service to schedule delivery. If payment fails, it issues commands to cancel the inventory reservation and order.
    
- **Choreography-Based Saga:**  
    There is no central coordinator. Instead, services communicate by emitting and listening to events. Each service decides when to execute its local transaction or a compensating transaction based on the events it receives.  
    _Example:_ The Order Service publishes an “Order Created” event, which triggers the Inventory Service to reserve stock. When inventory is reserved, it publishes an “Inventory Reserved” event that triggers the Payment Service. If payment fails, a “Payment Failed” event is broadcast, and the Inventory Service listens for that event to release the reserved stock.
    

The key difference lies in control—centralized in orchestration versus decentralized event-driven coordination in choreography.

---

### **Q3: How does compensation work in a Saga pattern, and can you provide an example?**

**Model Answer:**  
Compensation in a Saga is the process of undoing the effects of a local transaction when a subsequent transaction fails. Instead of rolling back a global transaction, each service performs its own compensating action.

_Example:_ In an e-commerce order saga, suppose the Payment Service fails after the Inventory Service has reserved items. The compensating transaction in the Inventory Service would release the reserved stock. Similarly, the Order Service might mark the order as canceled. Compensation ensures that, even if a part of the workflow fails, the overall system eventually returns to a consistent state.

---

### **Q4: What are some challenges you might face when implementing the Saga pattern?**

**Model Answer:**  
Implementing the Saga pattern can present several challenges:

- **Complexity in Coordination:**  
    Managing the sequence of local transactions and ensuring proper compensations can become complex, especially in choreography-based sagas.
- **Eventual Consistency:**  
    The system is only eventually consistent, which might be unsuitable for scenarios requiring immediate consistency.
- **Error Handling and Retries:**  
    Determining how and when to retry failed transactions or trigger compensations without causing cascading failures requires careful design.
- **Data Integrity and Side Effects:**  
    Ensuring that compensating transactions fully reverse the effects of a transaction, particularly when side effects (such as sending notifications) are involved.
- **Monitoring and Observability:**  
    Tracking and debugging a saga across multiple services necessitates robust logging, distributed tracing, and clear error reporting.

---

### **Q5: How do you ensure eventual consistency using the Saga pattern?**

**Model Answer:**  
Eventual consistency in the Saga pattern is achieved by ensuring that all local transactions and compensating actions eventually complete. Each service publishes events upon completing its transaction, which triggers the next step. Even if failures occur, compensating transactions are executed, and the system gradually converges to a consistent state. Tools like message brokers or event buses help maintain the flow of events, while distributed tracing and monitoring ensure that any delays or failures are quickly detected and resolved.

---

### **Q6: Can you compare the Saga pattern with Two-Phase Commit (2PC) in distributed transactions?**

**Model Answer:**  
The Saga pattern and 2PC both address distributed transactions but differ significantly:

- **2PC:**  
    Provides a strong ACID guarantee by locking resources across multiple services until the transaction commits, but it is blocking, can lead to performance bottlenecks, and is hard to scale in distributed systems.
- **Saga Pattern:**  
    Uses a series of local transactions with compensating actions, thereby avoiding resource locking. It offers eventual consistency rather than immediate consistency, which is more aligned with the asynchronous nature of microservices. The Saga pattern is non-blocking and more scalable, though it requires handling potential inconsistencies until the saga completes.

---

### **Q7: Walk me through a real-life scenario where you would apply the Saga pattern, including a dry run of the workflow.**

**Model Answer:**  
Consider an e-commerce order processing system:

1. **Step 1 – Order Creation:**  
    The Order Service creates an order and initiates the saga by sending an event or notifying the orchestrator.
2. **Step 2 – Inventory Reservation:**  
    The Inventory Service reserves the ordered items. If successful, it sends a confirmation event.
3. **Step 3 – Payment Processing:**  
    The Payment Service processes the customer’s payment. If the payment is successful, it publishes an event.
4. **Step 4 – Shipping Scheduling:**  
    The Shipping Service schedules delivery and confirms shipment.

**Dry Run – Failure Scenario:**

- If the Payment Service fails to process the payment:
    - A “Payment Failed” event is generated.
    - The Inventory Service receives the event and executes its compensating transaction to release the reserved items.
    - The Order Service cancels the order.
- **Outcome:**  
    Even though the individual services completed their local transactions, the compensating transactions ensure that the overall system state remains consistent.

---

### **Q8: How would you handle retries and error recovery in a Saga pattern?**

**Model Answer:**  
Handling retries involves designing the saga to be resilient:

- **Idempotency:**  
    Ensure that local transactions and compensating actions are idempotent, so retries do not cause duplicate operations.
- **Exponential Backoff:**  
    Use retry strategies with exponential backoff for transient errors.
- **Timeouts and Dead Letter Queues:**  
    Set timeouts for each transaction step. If a service doesn’t respond in time, move the message to a dead letter queue for further inspection or manual intervention.
- **Monitoring and Alerts:**  
    Implement comprehensive logging and monitoring to quickly detect failures and trigger compensating transactions if necessary.