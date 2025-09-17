#### Metadata

timestamp: **18:15**  &emsp;  **25-06-2021**
tags: #imp, #binary_tree 
question link: https://www.geeksforgeeks.org/maximum-sum-nodes-binary-tree-no-two-adjacent/
parent link: [[1. TREE GUIDE]]

---

# Maximum sum of nodes in Binary tree such that no two are adjacent

### Question

Given a binary tree with a value associated with each node, we need to choose a subset of these nodes such that the sum of chosen nodes is maximum under a constraint that no two chosen node in the subset should be directly connected that is, if we have taken a node in our sum then we can’t take any of its children in consideration and vice versa.

---


### Approach
- Return a pair for each node in the binary tree such that the first of the pair indicates maximum sum when the data of a node is included and the second indicates maximum sum when the data of a particular node is not included.

#### Code

``` cpp
pair<int, int> maxSumHelper(Node *root)
{
    if (root==NULL)
    {
        pair<int, int> sum(0, 0);
        return sum;
    }
    pair<int, int> sum1 = maxSumHelper(root->left);
    pair<int, int> sum2 = maxSumHelper(root->right);
    pair<int, int> sum;
 
    // This node is included (Left and right children
    // are not included)
    sum.first = sum1.second + sum2.second + root->data;
 
    // This node is excluded (Either left or right
    // child is included)
    sum.second = max(sum1.first, sum1.second) +
                 max(sum2.first, sum2.second);
 
    return sum;
}
 
int maxSum(Node *root)
{
    pair<int, int> res = maxSumHelper(root);
    return max(res.first, res.second);
}

```

---


