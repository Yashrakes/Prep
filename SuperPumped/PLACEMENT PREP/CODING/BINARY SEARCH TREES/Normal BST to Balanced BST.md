#### Metadata

timestamp: **10:41**  &emsp;  **10-07-2021**
topic tags: #bst , #imp
question link: https://practice.geeksforgeeks.org/problems/normal-bst-to-balanced-bst/1#
resource: https://www.geeksforgeeks.org/convert-normal-bst-balanced-bst/
parent link: [[1. BST GUIDE]]

---

# Normal BST to Balanced BST

### Question

Given a Binary Search Tree**,** modify the given BST such that itis balanced and has minimum possible height.

---


### Approach

#### Algorithm
1.  Traverse given BST in inorder and store result in an array. This step takes O(n) time. Note that this array would be sorted as inorder traversal of BST always produces sorted sequence.
2.  Build a balanced BST from the above created sorted array using the recursive approach discussed [here](https://www.geeksforgeeks.org/sorted-array-to-balanced-bst/). This step also takes O(n) time as we traverse every element exactly once and processing an element takes O(1) time.


#### Complexity Analysis
- **Time Complexity:** O(N)  
- **Auxiliary Space:** O(N)


#### Code

``` cpp
void storeIn(Node* root, vector<int> &inorder){
    if(!root) return;
    
    storeIn(root->left, inorder);
    inorder.push_back(root->data);
    storeIn(root->right, inorder);
}

Node* build(vector<int> &inorder, int low, int high){
    if(low > high)
        return NULL;
        
    if(low == high)
        return new Node(inorder[low]);
        
    int i = low + (high-low)/2;
    Node *root = new Node(inorder[i]);
    
    root->left = build(inorder, low, i-1);
    root->right = build(inorder, i+1, high);
    
    return root;
}

Node* buildBalancedTree(Node* root)
{
	//First find and store the inorder traversal
	vector<int> inorder;
	storeIn(root, inorder);
	
	return build(inorder, 0, inorder.size()-1);
}

```

---


