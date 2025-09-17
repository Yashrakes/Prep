#### Metadata

timestamp: **11:31**  &emsp;  **25-06-2021**
question link: https://practice.geeksforgeeks.org/problems/sum-tree/1#
resource: https://www.geeksforgeeks.org/check-if-a-given-binary-tree-is-sumtree/
tags: #solve_again , #binary_tree 
parent link: [[1. TREE GUIDE]]

---

# Sum Tree

### Question

Given a Binary Tree. Return **1** if, for every node **X** in the tree other than the leaves, its value is equal to the sum of its left subtree's value and its right subtree's value. Else return **0**.

An empty tree is also a Sum Tree as the sum of an empty tree can be considered to be 0. A leaf node is also considered a Sum Tree.

---


### Approach

1) If the node is a leaf node then the sum of the subtree rooted with this node is equal to the value of this node.   
2) If the node is not a leaf node then the sum of the subtree rooted with this node is twice the value of this node (Assuming that the tree rooted with this node is SumTree).

#### Code

``` cpp
int isSumTree(node* node)
{
    int ls; // for sum of nodes in left subtree
    int rs; // for sum of nodes in right subtree
 
    /* If node is NULL or it's a leaf node then
       return true */
    if(node == NULL || isLeaf(node))
        return 1;
 
    if( isSumTree(node->left) && isSumTree(node->right))
    {
       
        // Get the sum of nodes in left subtree
        if(node->left == NULL)
            ls = 0;
        else if(isLeaf(node->left))
            ls = node->left->data;
        else
            ls = 2 * (node->left->data);
 
        // Get the sum of nodes in right subtree
        if(node->right == NULL)
            rs = 0;
        else if(isLeaf(node->right))
            rs = node->right->data;
        else
            rs = 2 * (node->right->data);
 
        /* If root's data is equal to sum of nodes in left
           and right subtrees then return 1 else return 0*/
        return(node->data == ls + rs);
    }
    return 0;
}

```

---


