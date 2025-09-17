#### Metadata

timestamp: **08:46**  &emsp;  **02-08-2021**
topic tags: #trie
question link: https://leetcode.com/problems/implement-trie-prefix-tree/
resource:
parent link: [[1. TRIE GUIDE]]

---

# Implement Trie

### Question

A [**trie**](https://en.wikipedia.org/wiki/Trie) (pronounced as "try") or **prefix tree** is a tree data structure used to efficiently store and retrieve keys in a dataset of strings. There are various applications of this data structure, such as autocomplete and spellchecker.

Implement the Trie class:

-   `Trie()` Initializes the trie object.
-   `void insert(String word)` Inserts the string `word` into the trie.
-   `boolean search(String word)` Returns `true` if the string `word` is in the trie (i.e., was inserted before), and `false` otherwise.
-   `boolean startsWith(String prefix)` Returns `true` if there is a previously inserted string `word` that has the prefix `prefix`, and `false` otherwise.

---
### Applications

1. Auto complete
2. Spell Checker
3. IP routing (Longest Prefix Matching)
4. T9 Predictive text
5. Solving Word Games

---

### Why Trie?

- There are several other data structures, like balanced trees and hash tables, which give us the possibility to search for a word in a dataset of strings. Then why do we need trie? 
- Although hash table has O(1) time complexity for looking for a key, it is not efficient in the following operations :
	-   Finding all keys with a common prefix.
	-   Enumerating a dataset of strings in lexicographical order.
- Another reason why trie outperforms hash table, is that as hash table increases in size, there are lots of hash collisions and the search time complexity could deteriorate to O(n), where n is the number of keys inserted. 
- Trie could use less space compared to Hash Table when storing many keys with the same prefix. In this case using trie has only O(m) time complexity, where mm is the key length. Searching for a key in a balanced tree costs O(mlogn) time complexity.

---

### Approach
#### Complexity Analysis
- `Insertion:` 
	- **Time:** O(m), where m is the key length.
	- **Space:** O(m)

<br>

- `Search:`
	- **Time:** O(m), In each step of the algorithm we search for the next key character. In the worst case the algorithm performs mm operations.
	- **Space:** O(1)

<br>

- `Prefix search:`
	- **Time:** O(m)
	- **Space:** O(1)


#### Code

``` cpp
class Trie {
public:
    
    struct trieNode{
        trieNode* child[26] = {NULL};
        bool isWord = false;
    };
    
    trieNode* root;
    
    Trie() {
        root = new trieNode();
    }
    
    /** Inserts a word into the trie. */
    void insert(string word) {
        
        //Get a copy of the root node
        trieNode* temp = root;
        
        for(char c : word){
            
            //If child is not present then insert
            if(!temp->child[c - 'a']){
                
                //Creating a new node
                temp->child[c - 'a'] = new trieNode();
            }
            
            //Now that the child exists, traverse to that child
            temp = temp->child[c - 'a'];
        }
        
        //We now set the boolean to true as we have successfully inserted the word
        temp->isWord = true;
    }
    
    /** Returns if the word is in the trie. */
    bool search(string word) {
        trieNode* temp = root;
        
        for(char c : word){
            
            if(temp->child[c - 'a'] == NULL)
                return false;
            
            temp = temp->child[c - 'a'];
        }
        //If the control reaches here, than it means either that the word exist 
		//or a prefix equal to the word exists.
        return temp->isWord;
    }
    
    /** Returns if there is any word in the trie that starts with the given prefix. */
    bool startsWith(string prefix) {
        trieNode* temp = root;
        
        for(char c : prefix){
            
            if(temp->child[c - 'a'] == NULL)
                return false;
            
            temp = temp->child[c - 'a'];
        }
        return true;
    }
};

/**
 * Your Trie object will be instantiated and called as such:
 * Trie* obj = new Trie();
 * obj->insert(word);
 * bool param_2 = obj->search(word);
 * bool param_3 = obj->startsWith(prefix);
 */

```

---


