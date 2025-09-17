#### Metadata

timestamp: **18:11**  &emsp;  **25-07-2021**
topic tags: #imp, #stack
question link: https://leetcode.com/problems/lru-cache/
resource: [TUF](https://www.youtube.com/watch?v=xDEuM5qa0zg&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=77)
parent link: [[1. Stacks & Queue Guide]]

---

# LRU Cache

### Question

Design a data structure that follows the constraints of a **[Least Recently Used (LRU) cache](https://en.wikipedia.org/wiki/Cache_replacement_policies#LRU)**.

Implement the `LRUCache` class:

-   `LRUCache(int capacity)` Initialize the LRU cache with **positive** size `capacity`.
-   `int get(int key)` Return the value of the `key` if the key exists, otherwise return `-1`.
-   `void put(int key, int value)` Update the value of the `key` if the `key` exists. Otherwise, add the `key-value` pair to the cache. If the number of keys exceeds the `capacity` from this operation, **evict** the least recently used key.

The functions `get` and `put` must each run in `O(1)` average time complexity.

---


### Approach
```
WE use a doubly linked list and a map that stores the key and the address of the node
containing the key and the value. 

In the linked list we maintain two dummy pointers head and tail such that tails's left 
points to the LRU node and head's right points to the newly inserted node. 

Therefore if we traverse the list from head to tail, we visit the nodes in the order of 
recency, with the most recent appearing first and the least recent appearing last.


put:
if the key exists, then we need to update the value of the key and also make this key the most
recent ie.. delete the key from the linked list and insert it again from the front with the update value, 
delete the old instance of this key from the map and insert the new instance.

if capacity is reached, we delete LRU and and insert the node


get:
if the key does not exist 
    return -1
else
    we need to make this key the most recent, hence we delete it from the list and
    insert it again at the front of the list
```


#### Code

``` cpp
class Node{
    public:
    int key;
    int value;
    Node *left;
    Node *right;
    
    Node(int key, int value){
        this->key = key;
        this->value = value;
        left = NULL;
        right = NULL;
    }
};

class LRUCache {
int n;
Node *head, *tail;
unordered_map<int, Node*> map;
    
    void insertKey(int key, int value){
        
        //Insert the key after head
        Node *new_node = new Node(key, value);
        
        new_node->right = head->right;
        new_node->left = head;
        
        (head->right)->left = new_node;
        head->right = new_node;
        
        //insert the new node in the map
        map[key] = new_node;
    }
    
    
    void updateKey(int key, int value){
        //delete the key from the linked list and the map
        Node *del_node = map[key];
        map.erase(key);
        
        (del_node->left)->right = del_node->right;
        (del_node->right)->left = del_node->left;
        delete(del_node);
        
        //Insert the key with the updated value
        insertKey(key, value);
    }
    
    void deleteLRU(){
        //tail->left points to the least recently used node
        Node *del_node = tail->left;
        
        //Remove the lru from the map
        map.erase(del_node->key);
        
        //delete the LRU
        (del_node->left)->right = tail;
        tail->left = del_node->left;
        
        delete(del_node);
    }
    
public:
    LRUCache(int capacity) {
        n = capacity;
        head = new Node(0, 0);
        tail = new Node(0, 0);
        
        head->right = tail;
        tail->left = head;
        
        map.clear();
    }
    
    int get(int key) {
        if(map.find(key) == map.end())
            return -1;
        
        updateKey(key, map[key]->value);
        return map[key]->value;
    }
    
    void put(int key, int value) {
        
        //if key exists, update it
        if(map.find(key) != map.end())
            updateKey(key, value);
        else{
            if(map.size() == n)
                deleteLRU();
            insertKey(key, value);
        }
    }
};

```

---


