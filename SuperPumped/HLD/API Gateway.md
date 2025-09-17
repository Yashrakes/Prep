# Index

- [[#API Gateways in System Design]]
- [[#API Gateway vs. Load Balancer]]
- [[#API Gateway Single Point of Failure?]]
- [[#Case Study Netflix]]
- [[#Potential Interview Questions]]
- [[#Real-World Examples of API Gateways]]
---
# API Gateways in System Design

## What is an API Gateway?
- An API gateway is a server that sits between clients and your backend services, acting as a single entry point into your system. Think of it as the front door to your microservices architecture - all requests from clients first go through this gateway before being routed to the appropriate service.

## Core Functions of API Gateways

### 1. API Composition
- API gateways can combine results from multiple backend services into a single response. This is particularly valuable in microservice architectures.
- **Real-life example:** Imagine an e-commerce application where a product page needs:
	- Product details (from the product service)
	- Pricing (from the pricing service)
	- Inventory status (from the inventory service)
	- Reviews (from the review service)
- Without API composition, the client would need to make 4 separate API calls. With an API gateway:
	- The client makes a single request to `/product/123`
	- The gateway orchestrates calls to all required services
	- The gateway composes a unified response and returns it to the client
- This simplifies client-side code and improves performance by reducing network round trips.

---
### 2. Authentication and Authorization
- API gateways centralize authentication logic rather than implementing it in each service.
- **Real-life example:** When you log into Netflix, the API gateway:
	- Validates your credentials
	- Issues a token
	- Passes authenticated requests to internal services with appropriate context
	- Rejects unauthorized requests before they reach your services
- This prevents unauthenticated requests from ever reaching your backend services, providing a security perimeter.

---
### 3. Rate Limiting
- API gateways control how many requests a client can make in a given timeframe.
- **Real-life example:** Consider Twitter's API:
	- Free tier users might be limited to 500 requests per day
	- Premium users might be allowed 10,000 requests per day
- The API gateway tracks each client's usage and:
	- Allows requests within the limit
	- Rejects excess requests with a 429 (Too Many Requests) response
	- Potentially implements more sophisticated strategies like token bucket algorithms
- This protects your backend services from being overwhelmed and ensures fair resource allocation.

---
### 4. Service Discovery
- API gateways maintain a registry of available services and their locations.
- **Real-life example:** In AWS, API Gateway might work with AWS Service Discovery to:
	- Keep track of which services are running
	- Know where each service is located (IP address, port)
	- Route requests to the appropriate service instance
	- Handle cases where service instances come and go dynamically
- This allows your backend services to scale independently without clients needing to know their locations.

---
### 5. Other Important Functions
- **Protocol Translation**: Converting between different protocols (REST, GraphQL, gRPC, SOAP)
- **Request/Response Transformation**: Modifying data formats to meet client or service needs
- **Caching**: Storing frequently accessed data to improve performance
- **Circuit Breaking**: Preventing cascading failures when services are down
- **Logging & Monitoring**: Capturing metrics about API usage and performance
- **Versioning**: Managing different API versions for backward compatibility

---

# API Gateway vs. Load Balancer

- While they may seem similar, they serve different purposes

| Feature                | API Gateway                                                 | Load Balancer                                                        |
| ---------------------- | ----------------------------------------------------------- | -------------------------------------------------------------------- |
| **Primary Purpose**    | API management, request routing, composition                | Distribute traffic evenly across servers                             |
| **Protocol Awareness** | Application layer (HTTP/S) aware, understands API semantics | Can work at different levels (L4/L7) but with less API understanding |
| **Transformation**     | Can transform requests/responses                            | Limited or no transformation capabilities                            |
| **Service Discovery**  | Often includes service discovery                            | May have basic health checks but limited discovery                   |
| **Authentication**     | Commonly handles authentication                             | Generally doesn't handle authentication                              |
| **API Composition**    | Can aggregate multiple service calls                        | Doesn't compose responses                                            |
| **Rate Limiting**      | Typically includes rate limiting                            | Usually doesn't handle rate limiting                                 |

**Analogy**: If your system were a large office building:
- The load balancer would be like the elevator system, making sure people are evenly distributed across floors.
- The API gateway would be the reception desk that checks visitor IDs, tells them where to go, and sometimes handles their requests directly
---
# API Gateway : Single Point of Failure?

- Yes, an API gateway can certainly become a single point of failure (SPOF) if not designed properly. Since all client traffic funnels through this component before reaching your backend services, if the gateway fails, your entire system becomes inaccessible to clients.
- However, in practice, well-architected systems implement several strategies to mitigate this risk:

### High Availability Configuration
- Most production API gateway deployments use multiple redundant instances deployed across different availability zones or even regions. These instances operate behind a load balancer, which itself is typically designed for high availability. If one gateway instance fails, the load balancer redirects traffic to healthy instances.
- For example, when using **Amazon API Gateway**, AWS automatically manages this redundancy for you. The service is designed to be highly available and deployed across multiple availability zones within a region.

### Stateless Design
- Well-designed API gateways are stateless whenever possible. This means any gateway instance can handle any request without needing to maintain session state. 
- This design allows for horizontal scaling (adding more instances) and makes the system resilient when individual instances fail.

### Circuit Breakers and Fallbacks
- Modern API gateways implement circuit breaker patterns that can detect when backend services are failing and provide fallback responses. 
- This prevents cascading failures and maintains partial system functionality even when some components are down.

### Regional Redundancy
- For critical systems, organizations often deploy API gateways across multiple geographic regions. Combined with DNS routing services (like AWS Route 53), traffic can be directed to healthy regions if an entire region experiences an outage.

### Real-world Example
- Netflix's architecture provides a good illustration of this approach. 
- They use their Zuul API gateway but deploy multiple instances across regions. 
- They also use another layer of redundancy with their Global Traffic Management system that can redirect users to different regions if a catastrophic failure occurs in one region.

### Potential Alternative Approaches
Some organizations concerned about the centralized nature of API gateways sometimes implement:
1. Direct client-to-service communication for critical paths with a fallback to the API gateway
2. Multiple specialized gateways instead of a single monolithic gateway (a "gateway mesh")
3. Edge deployments that bring gateway functionality closer to users

### The Trade-off
- The potential single point of failure risk is a recognized trade-off when using API gateways. Organizations accept this risk because the benefits (security, composition, monitoring, etc.) outweigh the potential downsides, especially when proper redundancy is implemented.
- It's worth noting that this same concern applies to many centralized architectural components (load balancers, databases, etc.). The solution is not typically to avoid these components entirely, but rather to design them with appropriate redundancy and fallback mechanisms.
---
# Case Study : Netflix

>This case study helps understand the use of API Gateway and Internal Load Balancer

## Netflix Architecture
- Looking at the Netflix-inspired architecture diagram, we can see several key components arranged in layers:

![[Pasted image 20250520104113.png]]

### Layer 1: Client Devices
- Netflix supports various client devices (mobile, web browsers, smart TVs, etc.) that all need to access the same backend services but might require different responses or formats.

### Layer 2: Global DNS with Traffic Management
- The first point of contact is a global DNS service (similar to AWS Route 53 or Netflix's own Global Traffic Management)
- This service determines which regional deployment should handle the request based on:
    - Geographic proximity to the user
    - Health of each region
    - Current load across regions

### Layer 3: External Load Balancers
- **Positioned before the API gateways**
- These distribute incoming traffic across multiple API gateway instances
- They perform health checks on gateway instances
- If a gateway instance fails, the load balancer stops routing traffic to it
- In cloud environments, these might be AWS Elastic Load Balancers or similar services

### Layer 4: API Gateway Cluster
- Multiple API gateway instances run in each region
- Each gateway instance is identical and can handle any request
- They're deployed across different availability zones within a region
- Netflix uses their custom Zuul gateway (though they're transitioning to a newer system)

### Layer 5: Internal Load Balancers
- **Positioned after the API gateways and before the microservices**
- These distribute requests from API gateways to appropriate service instances
- They handle health checks for backend services
- They may implement more sophisticated routing based on request characteristics

### Layer 6: Microservices
- The actual business logic services
- Each service has multiple instances for redundancy
- Services might include user profiles, content metadata, recommendations, etc.
---
## Load Balancer Placement: Why Both Before and After?

As you noticed, load balancers are positioned at both ends of the API gateway layer. This dual placement serves different purposes:

### External Load Balancers (Before API Gateway)
- **Purpose**: Ensure API gateway high availability and scalability
- **Functions**:
    - Distribute client traffic across multiple gateway instances
    - Perform health checks on gateway instances
    - Remove unhealthy gateway instances from rotation
    - SSL termination (often)
    - Basic DDoS protection
- **Benefits**: Prevents the API gateway layer from becoming a single point of failure

### Internal Load Balancers (After API Gateway)
- **Purpose**: Manage traffic to backend services
- **Functions**:
    - Route requests to appropriate service instances
    - Handle service discovery (finding where services are running)
    - Perform health checks on service instances
    - May implement more advanced routing logic (sticky sessions, etc.)
- **Benefits**: Allows backend services to scale independently and handles service instance failures

---
## How This Prevents Single Points of Failure

This architecture eliminates single points of failure in several ways:
1. **Multiple Gateway Instances**: If one gateway fails, others can handle the traffic
2. **Multiple Regions**: If an entire region fails, traffic can be rerouted to another region
3. **Load Balancer Redundancy**: The load balancers themselves are typically deployed in highly available configurations
4. **Availability Zone Distribution**: Components are spread across isolated failure domains

---
## Netflix's Specific Approach
Netflix's architecture has some additional complexities:
- **Edge Locations**: They use AWS CloudFront and their Open Connect appliances to position content closer to users
- **Fallback Mechanisms**: Their client applications have sophisticated fallback logic to handle partial service outages
- **Circuit Breakers**: They heavily use Hystrix (their circuit breaker library) to isolate failures and provide fallback behaviors
- **Chaos Engineering**: They regularly test failure scenarios using their Chaos Monkey suite of tools to ensure resilience

---
## Real-World Traffic Flow

Let's trace a real-world request through this architecture:
1. A user in Seattle opens the Netflix app on their smart TV
2. The app contacts Netflix's Global DNS service to determine where to send API requests
3. Based on the user's location, the DNS points to the US-West region
4. The request hits an external load balancer in US-West
5. The load balancer forwards the request to one of the available API gateway instances
6. The API gateway authenticates the user and determines which services need to be called
7. The gateway makes requests to various internal services (via internal load balancers)
8. Each internal load balancer routes these requests to healthy service instances
9. The gateway composes the responses and returns a unified response to the client

If the US-West region experiences problems:
- The global DNS would detect this and start directing new sessions to US-East
- Existing sessions might continue in degraded mode with fallbacks

---
## Additional Redundancy Approaches
Beyond what's shown in the diagram, Netflix employs other redundancy strategies:
- **Stateless Design**: API gateways are designed to be stateless, making them easily replaceable
- **Automatic Scaling**: Gateway instances scale based on demand
- **Cross-Region Replication**: Critical data is replicated across regions
- **Active-Active Deployment**: Both regions are actively serving traffic, not just standby

---
# Potential Interview Questions 

### 1. How would you handle API versioning with an API gateway?
**Answer**: Several approaches can be used:
- **URL path versioning**: Including the version in the URL path (e.g., `/v1/products`)
- **Query parameter versioning**: Using a query parameter (e.g., `/products?version=1`)
- **Header-based versioning**: Using a custom header (e.g., `Accept-Version: 1`)
- **Content negotiation**: Using the Accept header (e.g., `Accept: application/vnd.company.v1+json`)

With an API gateway, I'd implement this by:
1. Examining the version indicator in the request
2. Routing to the appropriate backend service version
3. Potentially transforming the request/response if needed

This allows services to evolve independently while maintaining backward compatibility.

---
### 2. How would you handle authentication in a microservice architecture using an API gateway?
**Answer**: I would centralize authentication at the API gateway level:
1. The client authenticates with the gateway (using OAuth, JWT, API keys, etc.)
2. The gateway validates credentials and issues a token
3. For subsequent requests, the gateway:
    - Validates the token
    - Enriches the request with user context (user ID, roles, etc.)
    - Passes this context to internal services
4. Internal services trust the gateway and focus on a**uthorization (what the user can do) rather than authentication (who the user is)**
---
### 3. What issues might arise when implementing API composition, and how would you address them?
**Answer**: Several challenges can arise:
- **Performance concerns**: Calling multiple services sequentially increases latency
    - _Solution_: Use parallel calls where possible and implement timeouts
- **Partial failures**: When some backend services fail while others succeed
    - _Solution_: Implement circuit breakers and graceful degradation (return partial responses)
- **Transactional integrity**: Ensuring operations across services are consistent
    - _Solution_: Implement compensating transactions or saga patterns for rollbacks
- **Different data formats**: Services may use different data models
    - _Solution_: Implement transformation logic in the gateway
---
### 4. How would you implement rate limiting in an API gateway?
**Answer**: I would implement rate limiting using:
1. **Identification**: Determine who the requester is (API key, IP address, user ID)
2. **Policy definition**: Set limits based on user tiers or endpoints
3. **Algorithm selection**:
    - Fixed window counting
    - Sliding window counting
    - Token bucket algorithm (my preference for most scenarios)
    - Leaky bucket algorithm
4. **Storage**: Use a fast data store like Redis to track usage
5. **Response handling**: Return 429 status codes with headers indicating:
    - Current limit
    - Current usage
    - Reset time
6. **Queueing option**: For premium clients, consider queueing rather than rejecting
---
### 5. How would you ensure high availability of an API gateway?
**Answer**: To ensure high availability:
1. **Deploy multiple instances** across different availability zones
2. **Use load balancers** in front of the gateway instances
3. **Implement circuit breakers** to prevent cascading failures
4. **Cache service discovery results** to handle discovery service outages
5. **Design for statelessness** to allow easy scaling
6. **Monitor and alert** on gateway performance and errors
7. **Implement fallback responses** for critical APIs
8. **Consider a multi-region deployment** for disaster recovery
9. **Use blue-green or canary deployments** for updates to avoid downtime
---
### 6. How would an API gateway fit into a service mesh architecture?
**Answer**: In a service mesh architecture:
- **API gateway** handles north-south traffic (external clients to internal services)
- **Service mesh** handles east-west traffic (service-to-service communication)
They complement each other:
1. The API gateway provides a single entry point for external clients
2. The service mesh handles internal communication concerns like:
    - Service discovery
    - Load balancing
    - Circuit breaking
    - Observability
Together they form a comprehensive communication infrastructure, with the gateway focusing on client-facing concerns and the mesh handling internal communication.

---
# Real-World Examples of API Gateways
- **Amazon API Gateway**: Used for AWS Lambda and microservices
- **Kong**: Open-source API gateway built on NGINX
- **Netflix Zuul**: Netflix's API gateway that handles routing, filtering, and more
- **Azure API Management**: Microsoft's API gateway solution
- **Apigee**: Google Cloud's API management platform

These solutions provide the core gateway functionality while offering different levels of additional features and integrations.

---


