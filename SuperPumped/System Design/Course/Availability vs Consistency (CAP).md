#### Resources
- **Understanding C, A, P in CAP - Part1**: https://www.youtube.com/watch?v=pSoKUfLTe8Y
- **Part 2**:https://www.youtube.com/watch?v=kwCFHLbIhak
- **CAP Theorem explained!!:** http://ksat.me/a-plain-english-introduction-to-cap-theorem
- **CAP faq**: https://github.com/henryr/cap-faq
- **CAP theorem proof**: https://mwhittaker.github.io/blog/an_illustrated_proof_of_the_cap_theorem/
- https://www.infoq.com/articles/cap-twelve-years-later-how-the-rules-have-changed/
---

# Availability vs Consistency
In a distributed computer system, you can only support two of the following guarantees:

-   **Consistency** - Every read receives the most recent write or an error
-   **Availability** - Every request receives a response, without guarantee that it contains the most recent version of the information
-   **Partition Tolerance** - The system continues to operate despite arbitrary partitioning due to network failures

_Networks aren't reliable, so you'll need to support partition tolerance. You'll need to make a software tradeoff between consistency and availability._

#### CP - consistency and partition tolerance

Waiting for a response from the partitioned node might result in a timeout error. CP is a good choice if your business needs require atomic reads and writes.

#### AP - availability and partition tolerance

Responses return the most readily available version of the data available on any node, which might not be the latest. Writes might take some time to propagate when the partition is resolved.

AP is a good choice if the business needs allow for [eventual consistency](https://github.com/donnemartin/system-design-primer#eventual-consistency) or when the system needs to continue working despite external errors.