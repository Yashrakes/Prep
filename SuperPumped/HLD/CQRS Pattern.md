
# The Problem CQRS Addresses

- Traditional applications often use a single model for both reading and writing data. This creates several problems:
#### Data Access Pattern Mismatch
- In real-world systems, read and write operations often have fundamentally different characteristics:
	- **Reads** are typically much more frequent than writes (often by orders of magnitude)
	- **Reads** usually require optimized data structures for fast querying
	- **Writes** need to enforce business rules and maintain data integrity
	- **Reads** often involve joining multiple entities for display purposes
	- **Writes** focus on individual aggregates and their consistency boundaries
- Forcing a single model to handle both these concerns leads to compromises that can harm both performance and code clarity.

#### Scaling Challenges
- When an application becomes successful and needs to scale, read and write operations often need to scale differently:
	- Read operations typically need horizontal scaling (more servers)
	- Write operations are often constrained by consistency requirements
	- Optimizing for one operation type can degrade performance for the other


#### Complex Domain Logic
- In complex business domains, the rules governing data changes can be intricate and involve multiple validation steps, calculations, and side effects. When this logic is intertwined with query operations, the codebase becomes harder to understand and maintain.

#### Collaboration Conflicts
- In collaborative environments where multiple users update the same data, conflicts become more likely when read and write models are combined. Users might make decisions based on stale data, leading to concurrency issues.

---

# How CQRS Solves These Problems

- **Command Query Responsibility Segregation** (CQRS) addresses these issues by fundamentally separating the responsibility for handling commands (which change system state) from queries (which return system state).
- At its heart, CQRS introduces two distinct models:
	1. **Command Model**: Optimized for writes, focusing on business rules, domain logic, and data consistency. They are imperative, perform validation, and can trigger events.
	2. **Query Model**: Optimized for reads, focusing on fast data retrieval and presentation needs. They are declarative, optimized for performance, and can use a different data model than the one used for writes.
- This separation allows each model to be tailored to its specific responsibilities without compromising the other.

---

# Practical Benefits

#### 1. Optimized Performance
The query side can use database technologies and schemas optimized for read operations:
- Denormalized data structures
- Read-optimized views
- Caching mechanisms
- NoSQL databases where appropriate
For example, a product catalog might use a denormalized view with all product attributes and category information pre-joined to avoid expensive JOINs at query time.

---
#### 2. Independent Scaling
Each side can scale according to its own needs:
- Read replicas can be added for the query side
- Write master can be optimized for consistency and throughput
- Different resources can be allocated based on actual usage patterns
---
#### 3. Simplified Models
Each model becomes simpler by focusing on a single responsibility:
- Command models focus on business rules and invariants
- Query models focus on efficient data retrieval and presentation formats
---
#### 4. Evolution Flexibility
The separation enables more flexible evolution of the system:
- Query models can be modified to support new UI requirements without affecting core business logic
- Command models can evolve with changing business rules without breaking existing queries
- New query models can be added alongside existing ones to support new use cases
---

# Code Example

- Typical structure is as follows, 
	- **CommandHandler**:  make a state change in the db upon receiving a command. Fires a dataChangeEvent. typically each insertion fires a new event;
	- **QueryHandler**: returns data based on the input query, major difference is that the table which the data is being inserted into vs the table/view from which the data is read is different. 
		- QueryHandler is constructed based on the structure of the query input.
		- For instance, if the query demands data from multiple tables, a new view is created that precomputes this data combination.
		- As and when a query input is received, data is directly read from this view and returned, no complex join operations are performed at query time
	- **EventHandler**: To Synchronize data for the query handler
		- Upon receiving events from commandhandler, it updates the corresponding views.

## Ecommerce Example

#### Write Model

``` java
// Command Side (Write Model)
public class PlaceOrderCommand {
    private String customerId;
    private List<OrderItemDto> items;
    private ShippingAddress shippingAddress;
    private PaymentInformation paymentInfo;
}

// Command Handler
public class OrderCommandHandler {
    private final OrderRepository orderRepository;
    private final ProductInventoryRepository inventoryRepository;
    private final EventBus eventBus;
    
    @Transactional
    public void handle(PlaceOrderCommand command) {
        // Validate customer exists
        Customer customer = customerRepository.findById(command.getCustomerId());
        
        // Create order aggregate
        Order order = new Order(customer.getId());
        
        // Add items and check inventory
        for (OrderItemDto item : command.getItems()) {
            Product product = inventoryRepository.findById(item.getProductId());
            
            // Reserve inventory
            product.reserveQuantity(item.getQuantity());
            inventoryRepository.save(product);
            
            // Add to order
            order.addItem(product.getId(), item.getQuantity(), product.getPrice());
        }
        
        // Save order
        orderRepository.save(order);
        
        // Publish event
        eventBus.publish(new OrderPlacedEvent(order.getId(), order.getCustomerId()));
    }
}
```

#### Read Model

``` java
// Query
public class GetOrderDetailsQuery {
    private String orderId;
}

// Query Handler
public class OrderQueryHandler {
    private final OrderSummaryRepository orderSummaryRepository;
    
    public OrderDetailsDto handle(GetOrderDetailsQuery query) {
        // Fetch from read model (denormalized for fast retrieval)
        OrderSummary summary = orderSummaryRepository.findById(query.getOrderId());

        // Map to DTO with all necessary display information
        return new OrderDetailsDto(
            summary.getId(),
            summary.getOrderDate(),
            summary.getCustomerName(),
            summary.getItems(),
            summary.getShippingAddress(),
            summary.getStatus(),
            summary.getTrackingNumber(),
            summary.getTotalAmount()
        );
    }
}

// Order Summary Read Model (denormalized)
public class OrderSummary {
    private String id;
    private LocalDateTime orderDate;
    private String customerId;
    private String customerName;  // Denormalized from Customer
    private List<OrderItemSummary> items;  // Contains product names, prices
    private Address shippingAddress;
    private OrderStatus status;
    private String trackingNumber;
    private BigDecimal totalAmount;
}
```

#### Synchronization Mechanism

``` java
// Event Handler to update read model
public class OrderEventHandler {
    private final OrderSummaryRepository orderSummaryRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    
    @EventListener
    public void on(OrderPlacedEvent event) {
        Order order = orderRepository.findById(event.getOrderId());
        Customer customer = customerRepository.findById(order.getCustomerId());
        
        // Create denormalized read model
        OrderSummary summary = new OrderSummary();
        summary.setId(order.getId());
        summary.setOrderDate(order.getOrderDate());
        summary.setCustomerId(customer.getId());
        summary.setCustomerName(customer.getFullName());  // Denormalize customer name
        
        List<OrderItemSummary> itemSummaries = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId());
            
            OrderItemSummary itemSummary = new OrderItemSummary();
            itemSummary.setProductId(product.getId());
            itemSummary.setProductName(product.getName());  // Denormalize product name
            itemSummary.setProductImageUrl(product.getImageUrl());  // Denormalize image
            itemSummary.setQuantity(item.getQuantity());
            itemSummary.setUnitPrice(item.getUnitPrice());
            itemSummary.setTotalPrice(item.getTotalPrice());
            
            itemSummaries.add(itemSummary);
        }
        
        summary.setItems(itemSummaries);
        summary.setShippingAddress(order.getShippingAddress());
        summary.setStatus(order.getStatus());
        summary.setTotalAmount(order.getTotalAmount());
        
        // Save to read model repository
        orderSummaryRepository.save(summary);
    }
    
    @EventListener
    public void on(OrderShippedEvent event) {
        // Update read model with shipping information
        OrderSummary summary = orderSummaryRepository.findById(event.getOrderId());
        summary.setStatus(OrderStatus.SHIPPED);
        summary.setTrackingNumber(event.getTrackingNumber());
        orderSummaryRepository.save(summary);
    }
}
```