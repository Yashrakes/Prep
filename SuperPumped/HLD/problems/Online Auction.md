

As is the case with all of our common question breakdowns, we'll walk through this problem step by step, using the [Hello Interview System Design Framework](https://www.hellointerview.com/learn/system-design/in-a-hurry/delivery) as our guide. Note that I go into more detail here than would be required or possible in an interview, but I think the added detail is helpful for teaching concepts and deepening understanding.

### [Functional Requirements](https://www.hellointerview.com/learn/system-design/in-a-hurry/delivery#1-functional-requirements)

**Core Requirements**

1. Users should be able to post an item for auction with a starting price and end date.
    
2. Users should be able to bid on an item. Where bids are accepted if they are higher than the current highest bid.
    
3. Users should be able to view an auction, including the current highest bid.
    

**Below the line (out of scope):**

- Users should be able to search for items.
    
- Users should be able to filter items by category.
    
- Users should be able to sort items by price.
    
- Users should be able to view the auction history of an item.
    

### [Non-Functional Requirements](https://www.hellointerview.com/learn/system-design/in-a-hurry/delivery#2-non-functional-requirements)

Before diving into the non-functional requirements, ask your interviewer about the expected scale of the system. Understanding the scale requirements early will help inform key architectural decisions throughout your design.

## The Set Up

### [Defining the Core Entities](https://www.hellointerview.com/learn/system-design/in-a-hurry/delivery#core-entities-2-minutes)

### [API or System Interface](https://www.hellointerview.com/learn/system-design/in-a-hurry/delivery#api-or-system-interface-5-minutes)

## [High-Level Design](https://www.hellointerview.com/learn/system-design/in-a-hurry/delivery#high-level-design-10-15-minutes)

### 1) Users should be able to post an item for auction with a starting price and end date.

### 2) Users should be able to bid on an item. Where bids are accepted if they are higher than the current highest bid.

### 3) Users should be able to view an auction, including the current highest bid.

## [Potential Deep Dives](https://www.hellointerview.com/learn/system-design/in-a-hurry/delivery#deep-dives-10-minutes)

### 1) How can we ensure strong consistency for bids?

### 2) How can we ensure that the system is fault tolerant and durable?

### 3) How can we ensure that the system displays the current highest bid in real-time?

### 4) How can we ensure that the system scales to support 10M concurrent auctions?
