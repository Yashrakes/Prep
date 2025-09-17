#### Metadata

timestamp: **10:57**  &emsp;  **02-08-2021**
topic tags: #trie
question link: https://www.interviewbit.com/problems/shortest-unique-prefix/
resource: 
parent link: [[1. TRIE GUIDE]]

---

# Shortest Unique Prefix

### Question

Find shortest unique prefix to represent each word in the list.

**Example:**

```
Input: [zebra, dog, duck, dove]
Output: {z, dog, du, dov}
where we can see that
zebra = z
dog = dog
duck = du
dove = dov
```

---


### Approach
- We can use trie to solve this problem.
- With every node in the trie, we associate a counter indicating the no of times that character has been traversed.
- Therefore, we do a standard trie insertion for every word in the input and update the count of each character at the time of insertion.
- Now to get the unique id for a word, we traverse the word in the tree untill we reach a character/node whose count is 1. 

#### Code

``` cpp
struct trieNode {
    trieNode *child[26] = {NULL};
    int counter = 0;
};

trieNode *root;

void insert(string word){
    trieNode *temp = root;
    for(auto c : word){
        if(temp->child[c- 'a'] == NULL)
            temp->child[c - 'a'] = new trieNode();
        
        temp = temp->child[c - 'a'];
        temp->counter++;
    }
}

string getUnique(string word){
    trieNode *temp = root;

    string res = "";
    for(auto c : word){
        res += c;
        temp = temp->child[c - 'a'];

        if(temp->counter == 1)
            break;
    }

    return res;
}

vector<string> Solution::prefix(vector<string> &A) {

    root = new trieNode();
    for(auto word : A)
        insert(word);

    vector<string> res;
    for(auto word : A)
        res.push_back(getUnique(word));

    return res;
}

```

---


