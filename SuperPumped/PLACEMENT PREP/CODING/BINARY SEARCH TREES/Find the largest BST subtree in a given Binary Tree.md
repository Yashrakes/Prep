#### Metadata

timestamp: **16:51**  &emsp;  **10-07-2021**
topic tags: #bst, #imp
question link: https://practice.geeksforgeeks.org/problems/largest-bst/1#
resource: [Apna College](https://www.youtube.com/watch?v=YgC-IXiMrRM)
parent link: [[1. BST GUIDE]]

---

# Find the largest BST subtree in a given Binary Tree

### Question

Given a Binary Tree, write a function that returns the size of the largest subtree which is also a Binary Search Tree (BST). If the complete Binary Tree is BST, then return the size of whole tree.

---


### Approach

If we traverse the tree in bottom-up manner, then we can pass information about subtrees to the parent. The passed information can be used by the parent to do BST test (for parent node) only in constant time (or O(1) time). A left subtree need to tell the parent whether it is BST or not and also needs to pass maximum value in it. So that we can compare the maximum value with the parent’s data to check the BST property. Similarly, the right subtree need to pass the minimum value up the tree. The subtrees need to pass the following information up the tree for the finding the largest BST.

**NOTE: See video in the header to understand the solution**

#### Code

``` cpp
struct Info{
    int size;   //Size of the current tree
    int max;    //Max element in the current tree including the root
    int min;    //Min element in the current tree including the root
    int ans;    //Max size of a valid BST uptill now
    bool isBST; //Is the tree rooted at current node a valid BST
};

//Bottom up, traverse from the leaf to the root
Info largestBstInBT(Node* root){
    //Base case 1
    if(root == NULL)
        return {0, INT_MIN, INT_MAX, 0, true};
        
    //base case 2: if root is a leaf
    if(!root->left && !root->right)
        return {1, root->data, root->data, 1, true};
        
    Info leftInfo = largestBstInBT(root->left);
    Info rightInfo = largestBstInBT(root->right);
    Info curInfo;
    
    curInfo.size = 1 + leftInfo.size + rightInfo.size;
    
    //Check if tree rooted at the current node ie root, is BST or not
    if(leftInfo.isBST && rightInfo.isBST && root->data > leftInfo.max && root->data < rightInfo.min){
        curInfo.max = max({root->data, leftInfo.max, rightInfo.max});
        curInfo.min = min({root->data, leftInfo.min, rightInfo.min});
        curInfo.ans = curInfo.size;
        curInfo.isBST = true;
    }
    //If not a valid BST
    else{
        curInfo.ans = max(leftInfo.ans, rightInfo.ans);
        curInfo.isBST = false;
    }
    
    return curInfo;
}

int largestBst(Node *root){
	return largestBstInBT(root).ans;
}


```

---


