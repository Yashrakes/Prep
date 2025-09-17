![[Pasted image 20250413190415.png]]

## Round Robin

- Round Robin distributes requests sequentially to each server in the pool, moving from the first server to the last, then starting over again. Think of it like dealing cards around a table - each server gets a turn in a fixed order.
- This algorithm is simple to implement and ensures an equal distribution of requests among servers. However, it doesn't account for server capacity differences or the varying complexity of requests.

## Sticky Round Robin
- Sticky Round Robin (also called session persistence) routes all requests from a specific client to the same server that handled their initial request. The load balancer remembers which server handled a client's first request and directs subsequent requests from that client to the same server.
- This approach is essential for applications that maintain session state on the server, like shopping carts or authenticated sessions. Without it, users might experience inconsistencies when their requests are handled by different servers that don't share session information.

## Weighted Round Robin
- Weighted Round Robin assigns different "weights" to servers based on their capacity or performance capabilities. Servers with higher weights receive proportionally more requests than those with lower weights.
- For example, if Server A has a weight of 3 and Server B has a weight of 1, Server A will receive three requests for every one that Server B receives. This allows for traffic distribution that respects the different processing capabilities of your infrastructure.

## IP/URL Hash
- IP/URL Hash uses a hash function on either the client's IP address or the requested URL to determine which server should handle the request. The hash function consistently maps the same input (IP or URL) to the same server.
- This creates a form of stickiness without requiring the load balancer to maintain session tables. It works well for content delivery networks where specific content might be cached on particular servers. However, if a server goes down, all its mapped requests get rehashed and redistributed.

## Least Connections
- The Least Connections algorithm directs traffic to the server with the fewest active connections at the time of the request. This helps balance load dynamically based on each server's current capacity rather than using fixed patterns.
- This approach is particularly effective when requests vary significantly in processing time or complexity. It ensures that servers processing longer requests don't continue receiving new connections while others sit idle.

## Least Time
- Least Time is a more sophisticated version of Least Connections. It considers both the number of active connections and the server's response time. The load balancer sends new requests to the server with the lowest combination of current connections and response times.
- This algorithm is particularly effective in environments where server performance can vary due to factors like resource contention or hardware differences. By factoring in actual response times, it can detect and adjust for performance degradation in real-time.
- Each of these algorithms offers different benefits depending on your application needs, server infrastructure, and traffic patterns. Many modern load balancers let you combine these approaches or switch between them based on conditions.
---
