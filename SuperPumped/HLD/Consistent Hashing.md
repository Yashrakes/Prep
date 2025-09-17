
>Reference material: 
>https://arpitbhayani.me/blogs/consistent-hashing/
>http://medium.com/@anil.goyal0057/understanding-consistent-hashing-a-robust-approach-to-data-distribution-in-distributed-systems-0e4a0e770897

# Contents

1. [[#The Problem!]]
2. [[#Consistent Hashing]]
	1. [[#Core Concept]]
	2. [[#Hash Space Size and Virtual Nodes]]
	3. [[#Advantages of Consistent Hashing]]
	4. [[#Drawbacks and Limitations]]
	5. [[#Implementation]]
3.  [[#Interview Questions]]
	
---

# The Problem!

>[Read this to clearly understand the problem with traditional approach](http://medium.com/@anil.goyal0057/understanding-consistent-hashing-a-robust-approach-to-data-distribution-in-distributed-systems-0e4a0e770897)

- Let’s suppose we have a distributed system like Redis Cluster which consists of many shards. Each shard is assigned to a node. The multiple shards (Cluster mode enabled) are used for horizontal scaling i.e. to distribute the data (key-value pairs) to many nodes to balance the load and make the cache more performant during peak load period.
- Traditionally how the data is distributed to all these nodes was using a **hash function**. This hash function takes **data (key) as its input**, **convert** it into number (if it is a string) and then use the modulo operator [% (size)] where size is number of nodes. This results in **transformation of that input to a node.** The hash function used was deterministic i.e. always produce the same hash value for a given input, ensuring that mapping of keys to nodes remain consistent over time for storage and retrieval operations.
- For example, let’s say we have 20 data points (1 to 20) and 5 nodes (Node 1…5). We could hash each data point using a simple hash function like modulo division which distribute these data points equally among all nodes.
- There are two problems with this technique:
	- **Add or remove node** : Lets suppose one of the nodes 5 is removed from the cluster because of any reason like reduce demand, maintenance, or software updates on node, change in usage pattern then distribution of data points is significantly changed, and many data points (16 out of 20) need to be reassigned which could be inefficient and disruptive to the system.
	- **Uneven distribution** : The uneven distribution of data points can happen because of two reasons:
		- **Data Skew**: If the input data itself is not evenly distributed, the resulting hash values may not be evenly distributed either, leading to uneven distribution across nodes. Let’s consider this use case in modulo hash function as shown below. We can see that most of the data points are distributed to node 1 and node 2. This is because of the fixed pattern in input data points which results in same remainder when pass through modulo function.
		- **Poorly chosen hash function** : An example of poor hash function is shown below. In this case, if we use this hash function to distribute keys (strings) across a small number of nodes, it could lead to uneven distribution. For instance, if the keys are words and most of them start with the letter “A,” then all keys starting with “A” would hash to the same node, causing that node to become overloaded while others remain underutilized.

---

# Consistent Hashing

## Core Concept

In traditional hash-based distribution, if you have `n` servers, you might use the formula `hash(key) % n` to assign keys to servers. The problem arises when `n` changes - nearly all keys get reassigned to different servers.

Consistent hashing solves this by:
1. Mapping both keys and servers onto the same abstract circular hash space (often represented as a ring from 0 to 2^m - 1)
2. Assigning each key to the next server encountered when moving clockwise from the key's position on the ring
3. When a server is added or removed, only the keys that were mapped to that server need to be redistributed

---

## Hash Space Size and Virtual Nodes

Two critical design decisions in implementing consistent hashing are:
1. The size of the hash space
2. The number of virtual nodes per physical server

#### Hash Space Size
The hash space size should be:
- Large enough to minimize collisions
- Practical enough for efficient computation and storage

Common choices include:
- **32-bit space** (0 to 2^32 - 1): Offers a good balance between space size and computational efficiency
- **64-bit space**: Provides a much larger space with virtually no collision concerns, at the cost of slightly higher computational overhead

The hash space should be significantly larger than the number of items you'll be hashing to ensure good distribution. A typical rule of thumb is to choose a hash space at least several orders of magnitude larger than the maximum number of objects you expect to have.

---
#### Virtual Nodes
Virtual nodes (sometimes called "replicas") are multiple hash positions for the same physical server. Instead of placing each server at one position on the ring, we place it at multiple positions.

Determining the number of virtual nodes involves balancing:

1. **Load balancing quality**: More virtual nodes leads to better distribution
    - With few virtual nodes, you might have "gaps" in the ring, causing uneven distribution
    - Research shows the standard deviation of load decreases proportionally to 1/√v, where v is the number of virtual nodes
2. **Memory and computational overhead**: More virtual nodes means more memory and slower lookups
    - Each virtual node consumes memory and adds to lookup time
    - The relationship is linear: twice the virtual nodes means twice the memory usage

A common approach is to start with:
- 100-200 virtual nodes per server for smaller systems
- Adjust based on observed load imbalance and performance metrics
- For larger systems with hundreds of servers, you might reduce to 50-100 virtual nodes per server

Many production systems use empirical testing to find the optimal number for their specific use case, measuring load standard deviation against lookup performance.

---

## Advantages of Consistent Hashing

1. **Minimal redistribution**: When a server is added or removed, only a small fraction (1/n where n is the number of servers) of keys need to be remapped
2. **Horizontal scalability**: Makes adding or removing servers simple with minimal disruption
3. **Fault tolerance**: Server failures only affect a limited portion of the keyspace
4. **Predictable performance**: Avoids the "thundering herd" problem where many cache misses happen simultaneously
5. **Locality awareness**: Can be extended to consider network topology by placing servers closer together in the hash space

---

## Drawbacks and Limitations
1. **Uneven distribution**: Without virtual nodes, servers may have significantly different loads
2. **Memory overhead**: Maintaining virtual nodes requires additional memory
3. **Implementation complexity**: More complex than simple modulo-based distribution
4. **Parameter tuning**: Requires careful selection of hash functions and virtual node counts
5. **Weighted distribution challenges**: Additional complexity to handle servers with different capacities

---

## Implementation

#### Base Implementation
- A typical Consistent Hash Ring performs the following actions:
	- **addServer()** : Adds a new node to the hash ring, including virtual nodes
	- **removeServer()**: Removes the given server, including its virtual nodes
	- **getServer()**: Given a dataPoint, identifies which server is going to serve it.
		- Important for this operation to be as fast as possible, below is logarithmic time example
- In a database sharding use case, data migration is also needed for every new server addition/deletion.
- You identify keys to be remapped and perform migration to the new server.

``` cpp
class ConsistentHash {
private:
    // The hash ring - maps positions to server names
    std::map<size_t, std::string> ring;
    
    // Number of virtual nodes per physical server
    unsigned int virtualNodes;
    
    // Hash function
    std::hash<std::string> hasher;
    
    // Maps server names to their virtual node positions for easy removal
    std::unordered_map<std::string, std::vector<size_t>> serverToPositions;

    // Helper method to generate a virtual node name
    std::string getVirtualNodeName(const std::string& server, int i) const {
        return server + "#" + std::to_string(i);
    }
    
    // Get hash of a string
    size_t getHash(const std::string& key) const {
        return hasher(key);
    }

public:
    // Constructor with configurable number of virtual nodes
    ConsistentHash(unsigned int vNodes = 100) : virtualNodes(vNodes) {}
    
    // Add a server to the hash ring
    void addServer(const std::string& server) {
        std::vector<size_t> positions;
        
        // Create virtual nodes for this server
        for (unsigned int i = 0; i < virtualNodes; i++) {
            std::string virtualNodeName = getVirtualNodeName(server, i);
            size_t position = getHash(virtualNodeName);
            
            // Add to the ring
            ring[position] = server;
            positions.push_back(position);
        }
        
        // Store the positions for this server
        serverToPositions[server] = positions;
        
        std::cout << "Added server: " << server << " with " << virtualNodes << " virtual nodes\n";
    }
    
    // Remove a server from the hash ring
    void removeServer(const std::string& server) {
        if (serverToPositions.find(server) == serverToPositions.end()) {
            std::cout << "Server " << server << " not found\n";
            return;
        }
        
        // Get all positions for this server
        const std::vector<size_t>& positions = serverToPositions[server];
        
        // Remove all virtual nodes for this server
        for (const auto& position : positions) {
            ring.erase(position);
        }
        
        // Remove server from our tracking map
        serverToPositions.erase(server);
        
        std::cout << "Removed server: " << server << "\n";
    }
    
    // Get the server responsible for a given key
    std::string getServer(const std::string& key) const {
        if (ring.empty()) {
            throw std::runtime_error("No servers available");
        }
        
        // Get the hash position for this key
        size_t position = getHash(key);
        
        // Find the first server that appears after this position
        auto it = ring.lower_bound(position);
        
        // If we reached the end, wrap around to the first server
        if (it == ring.end()) {
            it = ring.begin();
        }
        
        return it->second;
    }

	// Example usage
	int main() {
	    // Create a consistent hash ring with 150 virtual nodes per server
	    ConsistentHash ring(150);
	    
	    // Add some servers
	    ring.addServer("server1.example.com");
	    ring.addServer("server2.example.com");
	    ring.addServer("server3.example.com");
	    ring.addServer("server4.example.com");
	    
	    // Print the server for some keys
	    std::cout << "\nInitial key assignment:\n";
	    std::cout << "Key 'user123' -> " << ring.getServer("user123") << "\n";
	    std::cout << "Key 'product456' -> " << ring.getServer("product456") << "\n";
	    std::cout << "Key 'session789' -> " << ring.getServer("session789") << "\n";
	    
	    // Analyze the distribution
	    ring.analyzeDistribution(100000);
	    
	    // Remove a server
	    std::cout << "\nRemoving server2.example.com...\n";
	    ring.removeServer("server2.example.com");
	    
	    // Print the new assignments for the same keys
	    std::cout << "\nKey assignment after removal:\n";
	    std::cout << "Key 'user123' -> " << ring.getServer("user123") << "\n";
	    std::cout << "Key 'product456' -> " << ring.getServer("product456") << "\n";
	    std::cout << "Key 'session789' -> " << ring.getServer("session789") << "\n";
	    
	    // Analyze the new distribution
	    ring.analyzeDistribution(100000);
	    
	    // Add a new server
	    std::cout << "\nAdding server5.example.com...\n";
	    ring.addServer("server5.example.com");
	    
	    // Print the new assignments
	    std::cout << "\nKey assignment after addition:\n";
	    std::cout << "Key 'user123' -> " << ring.getServer("user123") << "\n";
	    std::cout << "Key 'product456' -> " << ring.getServer("product456") << "\n";
	    std::cout << "Key 'session789' -> " << ring.getServer("session789") << "\n";
	    
	    // Final distribution analysis
	    ring.analyzeDistribution(100000);
	    
	    return 0;
	}
};
```

---

#### Full End to End implementation
- Showcasing Data Migration

``` cpp
#include <iostream>
#include <string>
#include <unordered_map>
#include <map>
#include <vector>
#include <functional>
#include <memory>
#include <mutex>
#include <thread>
#include <chrono>
#include <cassert>
#include <algorithm>
#include <random>
#include <iomanip>

/**
 * ConsistentHashRing - The core consistent hashing implementation
 * This maintains the mapping of keys to nodes using a ring.
 */
class ConsistentHashRing {
private:
    // The hash ring - maps positions to server names
    std::map<size_t, std::string> ring;
    
    // Number of virtual nodes per physical server
    unsigned int virtualNodesPerServer;
    
    // Hash function
    std::hash<std::string> hasher;
    
    // Maps server names to their virtual node positions
    std::unordered_map<std::string, std::vector<size_t>> serverToPositions;

    // Generate a virtual node name
    std::string getVirtualNodeName(const std::string& server, int i) const {
        return server + "#" + std::to_string(i);
    }
    
    // Get hash of a string
    size_t getHash(const std::string& key) const {
        return hasher(key);
    }

public:
    // Constructor
    ConsistentHashRing(unsigned int virtualNodes = 100) 
        : virtualNodesPerServer(virtualNodes) {
        std::cout << "Created consistent hash ring with " << virtualNodes << " virtual nodes per server\n";
    }
    
    // Add a server to the hash ring
    void addServer(const std::string& server) {
        std::vector<size_t> positions;
        
        // Create virtual nodes for this server
        for (unsigned int i = 0; i < virtualNodesPerServer; i++) {
            std::string virtualNodeName = getVirtualNodeName(server, i);
            size_t position = getHash(virtualNodeName);
            
            ring[position] = server;
            positions.push_back(position);
        }
        
        serverToPositions[server] = positions;
        
        std::cout << "Added server " << server << " to the hash ring with " 
                  << virtualNodesPerServer << " virtual nodes\n";
    }
    
    // Remove a server from the hash ring
    void removeServer(const std::string& server) {
        if (serverToPositions.find(server) == serverToPositions.end()) {
            std::cout << "Server " << server << " not found in the hash ring\n";
            return;
        }
        
        // Get all positions for this server
        const std::vector<size_t>& positions = serverToPositions[server];
        
        // Remove all virtual nodes
        for (const auto& position : positions) {
            ring.erase(position);
        }
        
        serverToPositions.erase(server);
        
        std::cout << "Removed server " << server << " from the hash ring\n";
    }
    
    // Get the server responsible for a key
    std::string getServerForKey(const std::string& key) const {
        if (ring.empty()) {
            throw std::runtime_error("No servers available in the hash ring");
        }
        
        // Get the hash position for this key
        size_t keyHash = getHash(key);
        
        // Find the first server that appears after this position
        auto it = ring.lower_bound(keyHash);
        
        // If we reached the end, wrap around to the beginning
        if (it == ring.end()) {
            it = ring.begin();
        }
        
        return it->second;
    }
    
    // Get all servers in the ring
    std::vector<std::string> getAllServers() const {
        std::vector<std::string> servers;
        servers.reserve(serverToPositions.size());
        
        for (const auto& pair : serverToPositions) {
            servers.push_back(pair.first);
        }
        
        return servers;
    }
    
    // Get the server that comes before a given server in the ring
    // This is used to determine which keys need to be transferred when adding a new server
    std::string getPredecessorServer(const std::string& server) const {
        if (serverToPositions.size() <= 1) {
            return server; // Only one server, so it's its own predecessor
        }
        
        // Get lowest position for this server
        if (serverToPositions.find(server) == serverToPositions.end()) {
            throw std::runtime_error("Server not found in the hash ring");
        }
        
        // Find predecessor for each virtual node and return the closest one
        const std::vector<size_t>& positions = serverToPositions.at(server);
        
        std::unordered_map<std::string, int> predecessorCounts;
        
        for (const auto& position : positions) {
            auto it = ring.find(position);
            if (it == ring.begin()) {
                // Wrap around to the end
                it = std::prev(ring.end());
            } else {
                it = std::prev(it);
            }
            
            // Skip if we landed on the same server
            if (it->second == server) {
                continue;
            }
            
            predecessorCounts[it->second]++;
        }
        
        // Return the most common predecessor
        std::string mostCommonPredecessor;
        int maxCount = 0;
        
        for (const auto& [pred, count] : predecessorCounts) {
            if (count > maxCount) {
                maxCount = count;
                mostCommonPredecessor = pred;
            }
        }
        
        return mostCommonPredecessor;
    }
    
    // Get the range of keys that would now map to a new server
    // Returns a pair of hashes representing the start and end of the range
    std::vector<std::pair<size_t, size_t>> getKeyRangesForNewServer(const std::string& newServer) {
        // Temporarily add the server
        addServer(newServer);
        
        // Get all positions for this server
        const std::vector<size_t>& positions = serverToPositions[newServer];
        
        std::vector<std::pair<size_t, size_t>> ranges;
        
        // For each virtual node position, find the range of keys it's responsible for
        for (const auto& position : positions) {
            // Find predecessor position
            auto it = ring.find(position);
            size_t startPos;
            
            if (it == ring.begin()) {
                // If this is the first position, the predecessor is the last one
                startPos = std::prev(ring.end())->first;
            } else {
                // Otherwise, it's the previous position
                startPos = std::prev(it)->first;
            }
            
            // The range is from the predecessor to this position
            ranges.push_back(std::make_pair(startPos, position));
        }
        
        // Remove the server (since this was just a test)
        removeServer(newServer);
        
        return ranges;
    }
    
    // Debug method to print the ring
    void printRing() const {
        std::cout << "\n=== Hash Ring State ===\n";
        std::cout << "Number of physical servers: " << serverToPositions.size() << "\n";
        std::cout << "Number of virtual nodes: " << ring.size() << "\n\n";
        
        // Print the first few virtual nodes for each server
        for (const auto& [server, positions] : serverToPositions) {
            std::cout << "Server " << server << " positions: ";
            for (size_t i = 0; i < std::min(positions.size(), size_t(3)); ++i) {
                std::cout << positions[i] << " ";
            }
            if (positions.size() > 3) {
                std::cout << "... (and " << positions.size() - 3 << " more)";
            }
            std::cout << "\n";
        }
        std::cout << "=======================\n\n";
    }
};

/**
 * StorageNode - Represents a single server in the distributed system
 * Each node stores data locally and can transfer data to other nodes
 */
class StorageNode {
private:
    std::string nodeId;
    std::unordered_map<std::string, std::string> localData;
    std::mutex dataMutex;
    
public:
    StorageNode(const std::string& id) : nodeId(id) {
        std::cout << "Created storage node " << nodeId << "\n";
    }
    
    // Store a key-value pair locally
    void store(const std::string& key, const std::string& value) {
        std::lock_guard<std::mutex> lock(dataMutex);
        localData[key] = value;
    }
    
    // Retrieve a value by key
    std::string retrieve(const std::string& key) const {
        std::lock_guard<std::mutex> lock(dataMutex);
        auto it = localData.find(key);
        if (it == localData.end()) {
            return ""; // Key not found
        }
        return it->second;
    }
    
    // Check if a key exists
    bool hasKey(const std::string& key) const {
        std::lock_guard<std::mutex> lock(dataMutex);
        return localData.find(key) != localData.end();
    }
    
    // Get all keys stored on this node
    std::vector<std::string> getAllKeys() const {
        std::lock_guard<std::mutex> lock(dataMutex);
        std::vector<std::string> keys;
        keys.reserve(localData.size());
        
        for (const auto& pair : localData) {
            keys.push_back(pair.first);
        }
        
        return keys;
    }
    
    // Transfer data to another node
    void transferData(StorageNode& targetNode, const std::vector<std::string>& keysToTransfer) {
        std::lock_guard<std::mutex> lock(dataMutex);
        for (const auto& key : keysToTransfer) {
            auto it = localData.find(key);
            if (it != localData.end()) {
                std::cout << "Node " << nodeId << " transferring key " << key << " to node " << targetNode.nodeId << "\n";
                targetNode.store(key, it->second);
                
                // In a real system, you would confirm successful transfer before removing
                localData.erase(key);
            }
        }
    }
    
    // Get the node ID
    std::string getId() const {
        return nodeId;
    }
    
    // Get data count
    size_t getDataCount() const {
        std::lock_guard<std::mutex> lock(dataMutex);
        return localData.size();
    }
    
    // Print node status
    void printStatus() const {
        std::lock_guard<std::mutex> lock(dataMutex);
        std::cout << "Node " << nodeId << " status:\n";
        std::cout << "  - Storing " << localData.size() << " key-value pairs\n";
        
        // Print a few sample keys
        if (!localData.empty()) {
            std::cout << "  - Sample keys: ";
            int count = 0;
            for (const auto& pair : localData) {
                if (count++ >= 3) break;
                std::cout << pair.first << " ";
            }
            if (localData.size() > 3) {
                std::cout << "... (and " << localData.size() - 3 << " more)";
            }
            std::cout << "\n";
        }
    }
};

/**
 * DistributedHashTable - The main system that coordinates all nodes
 * This is the central service that clients interact with and that manages the cluster
 */
class DistributedHashTable {
private:
    ConsistentHashRing hashRing;
    std::unordered_map<std::string, std::shared_ptr<StorageNode>> nodes;
    std::mutex nodesMutex;
    
public:
    DistributedHashTable(unsigned int virtualNodesPerServer = 100) 
        : hashRing(virtualNodesPerServer) {
        std::cout << "Created distributed hash table with " << virtualNodesPerServer << " virtual nodes per server\n";
    }
    
    // Add a new node to the cluster
    void addNode(const std::string& nodeId) {
        std::lock_guard<std::mutex> lock(nodesMutex);
        
        // Check if the node already exists
        if (nodes.find(nodeId) != nodes.end()) {
            std::cout << "Node " << nodeId << " already exists in the cluster\n";
            return;
        }
        
        // Create and store the new node
        auto newNode = std::make_shared<StorageNode>(nodeId);
        nodes[nodeId] = newNode;
        
        // Get key ranges that will be assigned to the new node
        auto keyRanges = hashRing.getKeyRangesForNewServer(nodeId);
        
        // Add the server to the hash ring
        hashRing.addServer(nodeId);
        
        // Identify which keys need to be redistributed
        migrateDataForNewNode(nodeId, keyRanges);
        
        std::cout << "Node " << nodeId << " added to the cluster\n";
    }
    
    // Remove a node from the cluster
    void removeNode(const std::string& nodeId) {
        std::lock_guard<std::mutex> lock(nodesMutex);
        
        // Check if the node exists
        if (nodes.find(nodeId) == nodes.end()) {
            std::cout << "Node " << nodeId << " does not exist in the cluster\n";
            return;
        }
        
        // Get all keys from the node that will be removed
        auto nodeToRemove = nodes[nodeId];
        auto keysToRedistribute = nodeToRemove->getAllKeys();
        
        // Remove the node from the hash ring
        hashRing.removeServer(nodeId);
        
        // Redistribute the keys to other nodes
        redistributeKeysFromRemovedNode(nodeToRemove, keysToRedistribute);
        
        // Remove the node from our map
        nodes.erase(nodeId);
        
        std::cout << "Node " << nodeId << " removed from the cluster\n";
    }
    
    // Store a key-value pair
    void put(const std::string& key, const std::string& value) {
        std::lock_guard<std::mutex> lock(nodesMutex);
        
        // Find which node should store this key
        std::string nodeId = hashRing.getServerForKey(key);
        
        // Get the node
        auto nodeIt = nodes.find(nodeId);
        if (nodeIt == nodes.end()) {
            throw std::runtime_error("Node " + nodeId + " not found in the cluster");
        }
        
        // Store the value on the node
        nodeIt->second->store(key, value);
        std::cout << "Stored key '" << key << "' on node " << nodeId << "\n";
    }

	// Get a value by key
    std::string get(const std::string& key) {
        std::lock_guard<std::mutex> lock(nodesMutex);
        
        // Find which node should have this key
        std::string nodeId = hashRing.getServerForKey(key);
        
        // Get the node
        auto nodeIt = nodes.find(nodeId);
        if (nodeIt == nodes.end()) {
            throw std::runtime_error("Node " + nodeId + " not found in the cluster");
        }
        
        // Retrieve the value from the node
        std::string value = nodeIt->second->retrieve(key);
        std::cout << "Retrieved key '" << key << "' from node " << nodeId << "\n";
        
        return value;
    }
    
    // Print the status of the entire cluster
    void printClusterStatus() {
        std::lock_guard<std::mutex> lock(nodesMutex);
        
        std::cout << "\n=== Cluster Status ===\n";
        std::cout << "Number of nodes: " << nodes.size() << "\n\n";
        
        // Print hash ring state
        hashRing.printRing();
        
        // Print each node's status
        for (const auto& [nodeId, node] : nodes) {
            node->printStatus();
        }
        
        std::cout << "=====================\n\n";
    }
    
private:
    // Helper method to migrate data to a new node
    void migrateDataForNewNode(const std::string& newNodeId, const std::vector<std::pair<size_t, size_t>>& keyRanges) {
        std::hash<std::string> hasher;
        auto newNode = nodes[newNodeId];
        
        // For each existing node
        for (const auto& [nodeId, node] : nodes) {
            if (nodeId == newNodeId) continue; // Skip the new node
            
            // Get all keys from this node
            auto keys = node->getAllKeys();
            std::vector<std::string> keysToTransfer;
            
            // Check each key
            for (const auto& key : keys) {
                size_t keyHash = hasher(key);
                
                // Check if this key falls within any of the ranges for the new node
                for (const auto& [start, end] : keyRanges) {
                    if ((start < end && keyHash > start && keyHash <= end) ||
                        (start > end && (keyHash > start || keyHash <= end))) {
                        keysToTransfer.push_back(key);
                        break;
                    }
                }
            }
            
            // Transfer the keys to the new node
            if (!keysToTransfer.empty()) {
                std::cout << "Migrating " << keysToTransfer.size() << " keys from node " 
                          << nodeId << " to new node " << newNodeId << "\n";
                node->transferData(*newNode, keysToTransfer);
            }
        }
    }
    
    // Helper method to redistribute keys from a removed node
    void redistributeKeysFromRemovedNode(std::shared_ptr<StorageNode> nodeToRemove, const std::vector<std::string>& keys) {
        // For each key that was on the removed node
        for (const auto& key : keys) {
            // Find which node should now store this key
            std::string newNodeId = hashRing.getServerForKey(key);
            
            // Get the node
            auto nodeIt = nodes.find(newNodeId);
            if (nodeIt == nodes.end()) {
                std::cout << "Warning: Node " << newNodeId << " not found for key " << key << "\n";
                continue;
            }
            
            // Get the value from the node being removed
            std::string value = nodeToRemove->retrieve(key);
            
            // Store it on the new node
            nodeIt->second->store(key, value);
            
            std::cout << "Redistributed key '" << key << "' from removed node to node " << newNodeId << "\n";
        }
    }
};

// Demo function to show the system in action
void runDemoScenario() {
    std::cout << "=== Running Distributed Hash Table Demo ===\n\n";
    
    // Create a DHT with 100 virtual nodes per server
    DistributedHashTable dht(100);
    
    // Add initial nodes
    dht.addNode("node1");
    dht.addNode("node2");
    dht.addNode("node3");
    
    // Print initial status
    dht.printClusterStatus();
    
    // Store some data
    std::cout << "\n--- Storing initial data ---\n";
    for (int i = 1; i <= 30; i++) {
        std::string key = "key" + std::to_string(i);
        std::string value = "value" + std::to_string(i);
        dht.put(key, value);
    }
    
    // Print status after data is stored
    dht.printClusterStatus();
    
    // Retrieve some data
    std::cout << "\n--- Retrieving some data ---\n";
    for (int i = 1; i <= 5; i++) {
        std::string key = "key" + std::to_string(i);
        std::string value = dht.get(key);
        std::cout << "Retrieved: " << key << " = " << value << "\n";
    }
    
    // Add a new node
    std::cout << "\n--- Adding a new node ---\n";
    dht.addNode("node4");
    
    // Print status after adding a node
    dht.printClusterStatus();
    
    // Retrieve the same data to see if it's still accessible
    std::cout << "\n--- Retrieving same data after adding a node ---\n";
    for (int i = 1; i <= 5; i++) {
        std::string key = "key" + std::to_string(i);
        std::string value = dht.get(key);
        std::cout << "Retrieved: " << key << " = " << value << "\n";
    }
    
    // Remove a node
    std::cout << "\n--- Removing a node ---\n";
    dht.removeNode("node2");
    
    // Print status after removing a node
    dht.printClusterStatus();
    
    // Retrieve the same data again
    std::cout << "\n--- Retrieving same data after removing a node ---\n";
    for (int i = 1; i <= 5; i++) {
        std::string key = "key" + std::to_string(i);
        std::string value = dht.get(key);
        std::cout << "Retrieved: " << key << " = " << value << "\n";
    }
    
    std::cout << "\n=== Demo completed ===\n";
}

int main() {
    runDemoScenario();
    return 0;
}
```
---

# Interview Questions

## 1. What problem does consistent hashing solve?

**Answer**: Consistent hashing solves the problem of minimizing key redistribution in distributed hash tables when servers are added or removed. In traditional hashing schemes using modulo (`hash(key) % n`), changing the number of servers causes almost all keys to be remapped. Consistent hashing ensures that when a server is added or removed, only a small fraction (approximately 1/n) of keys need to be redistributed, making it ideal for dynamic distributed systems like caches, databases, and content delivery networks.

---

## 2. Explain how consistent hashing works.

**Answer**: Consistent hashing works by mapping both servers and keys to points on the same conceptual circle or ring (representing the hash space from 0 to 2^m-1). Each key is assigned to the next server encountered when moving clockwise from the key's position on the ring. When a server is added, it only affects keys that hash to positions between the new server and its predecessor server. When a server is removed, only keys that were assigned to that server need to be reassigned to the next server in the ring. This way, adding or removing a server only affects a small fraction of keys, proportional to 1/n of the total keyspace.

---

## 3. What are virtual nodes in consistent hashing and why are they important?

**Answer**: Virtual nodes (or replicas) represent a single physical server at multiple positions on the hash ring. Without virtual nodes, servers might be unevenly placed around the ring, causing some servers to handle significantly more keys than others. Virtual nodes improve load balancing by distributing each server's responsibility more evenly across the hash space.

The number of virtual nodes directly impacts load distribution quality - more virtual nodes leads to better distribution, with the standard deviation of load decreasing proportionally to 1/√v (where v is the number of virtual nodes). However, more virtual nodes also increase memory usage and lookup overhead. Most production systems use 100-200 virtual nodes per.

Most production systems use 100-200 virtual nodes per server as a starting point, then adjust based on empirical testing. The ideal number balances distribution quality against memory and CPU overhead. In very large systems with thousands of servers, you might use fewer virtual nodes per server to control the total overhead.

---

## 4. How would you implement weighted consistent hashing?

**Answer**: Weighted consistent hashing allows servers with different capacities to take proportional amounts of the keyspace. To implement it:

1. Assign virtual nodes proportionally to server capacity, not equally
2. If Server A has twice the capacity of Server B, give Server A twice as many virtual nodes
3. The implementation simply adjusts the number of virtual nodes per server:

``` cpp
// For a server with relative weight 2.0 (twice the standard capacity)
int virtualNodesForThisServer = baseVirtualNodes * 2.0;
```

This approach means higher-capacity servers will be responsible for more positions on the ring and thus receive proportionally more keys.

---

## 5. Describe how consistent hashing is used in real-world distributed systems.

**Answer**: Consistent hashing has been implemented in numerous production systems:

1. **Amazon Dynamo and DynamoDB**: Uses consistent hashing to partition data across nodes and for replicas
2. **Apache Cassandra**: Employs a consistent hashing variant called "virtual nodes" to distribute data across the ring
3. **Content Delivery Networks (CDNs)**: Akamai and others use consistent hashing to determine which edge server should cache specific content
4. **Redis Cluster**: Uses consistent hashing for sharding data across multiple Redis instances
5. **Memcached clients**: Many memcached client libraries implement consistent hashing to distribute cache entries
6. **Load balancers**: HAProxy and others can use consistent hashing for request distribution with session affinity

Each implementation adapts the concept to its specific requirements, often with customizations for replication, fault tolerance, and load balancing.

---
