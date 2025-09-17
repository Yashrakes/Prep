#### Metadata

timestamp: **18:32**  &emsp;  **10-07-2021**
topic tags: #bst
question link: https://www.geeksforgeeks.org/floor-and-ceil-from-a-bst/
resource:
parent link: [[1. BST GUIDE]]

---

# Floor and Ceil from a BST

### Question
Given a binary tree and a key(node) value, find the floor and ceil value for that particular key value.

**Floor Value Node**: Node with the greatest data lesser than or equal to the key value.   
**Ceil Value Node**: Node with the smallest data larger than or equal to the key value.

**Example:**
```
          8
        /   \    
      4      12
    /  \    /  \
   2    6  10   14

**Key**: 11  **Floor**: 10  **Ceil**: 12
**Key**: 1   **Floor**: -1  **Ceil**: 2
**Key**: 6   **Floor**: 6   **Ceil**: 6
**Key**: 15  **Floor**: 14  **Ceil**: -1
```

---


### Approach
- The given question is equivalent of finding the inorder predecessor and inorder successor.
- If the key is present in the tree, than floor and ceil are equal to key
- If key is not present in the tree, then the situation is same as case 3 of [[Predecessor and Successor]]


#### Code

``` cpp
void floorCeilBSTHelper(Node* root, int key, int& floor, int& ceil)
{

	if(!root) return;
	
	if(root->data == key){
	    floor = ceil = key;
	    return;
	}
	
	if(key < root->data){
	    ceil = root->data;
	    floorCeilBSTHelper(root->left, key, floor, ceil);
	}
	else if(key > root->data){
	    floor = root->data;
	    floorCeilBSTHelper(root->right, key, floor, ceil);
	}
}

// If key is less than the min key in BST, floor will be -1;
// If key is more than the max key in BST, ceil will be -1;
void floorCeilBST(Node* root, int key)
{

	int floor = -1, ceil = -1;
	floorCeilBSTHelper(root, key, floor, ceil);

	cout << key << ' ' << floor << ' ' << ceil << '\n';
}


```

---


