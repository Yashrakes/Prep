#### Metadata

timestamp: **12:29**  &emsp;  **26-06-2021**
tags: #imp, #binary_tree , #solve_again 
question link: https://leetcode.com/problems/flatten-binary-tree-to-linked-list/
resource: https://www.geeksforgeeks.org/flatten-a-binary-tree-into-linked-list/
parent link: [[1. TREE GUIDE]]

---

# Flatten a binary tree into linked list

### Question

Given the `root` of a binary tree, flatten the tree into a "linked list":

-   The "linked list" should use the same `TreeNode` class where the `right` child pointer points to the next node in the list and the `left` child pointer is always `null`.
-   The "linked list" should be in the same order as a [**pre-order** **traversal**](https://en.wikipedia.org/wiki/Tree_traversal#Pre-order,_NLR) of the binary tree.

---


### Approach


#### Code1 : Recursion



``` cpp
class Solution {
private:
    // Helper function that takes a reference to lastNode as a parameter
    // This allows us to keep track of the last processed node without global state
    void flattenHelper(TreeNode* root, TreeNode*& lastNode) {
        // Base case: if root is null, nothing to flatten
        if (root == nullptr) return;
        
        // Store original children since we'll modify the pointers
        TreeNode* rightChild = root->right;
        TreeNode* leftChild = root->left;
        
        // If we have processed a node before, connect it to current node
        if (lastNode != nullptr) {
            lastNode->right = root;
            lastNode->left = nullptr;
        }
        
        // Update lastNode to current node
        lastNode = root;
        
        // Recursively process left and right subtrees
        // The order matters here to maintain pre-order traversal
        flattenHelper(leftChild, lastNode);
        flattenHelper(rightChild, lastNode);
        
        // Clear left pointer as per requirement
        root->left = nullptr;
    }
    
public:
    void flatten(TreeNode* root) {
        // Initialize lastNode as nullptr and pass it by reference
        TreeNode* lastNode = nullptr;
        flattenHelper(root, lastNode);
    }
};
```

---

#### Code2 : Without extra space

- The key insight of this solution is that instead of using recursion or a stack, we can cleverly rearrange the tree by finding connection points.

``` cpp
class Solution {
public:
    void flatten(TreeNode* root) {
        if (!root) return;

        TreeNode* node = root;
        while (node) {

            // Attatches the right sub-tree to the rightmost leaf of the left sub-tree:
            if (node->left) {

                TreeNode *rightMost = node->left;
                while (rightMost->right) {

                    rightMost = rightMost->right;
                }
                rightMost->right = node->right;

                // Makes the left sub-tree to the right sub-tree:
                node->right = node->left;
                node->left = NULL;
            }

            // Flatten the rest of the tree:
            node = node->right;
        } 
    }
};
```

- First, let's understand the main loop structure. This loop traverses each node in the tree from top to bottom. For each node, we look at whether it has a left child that needs to be flattened.
- The process continues until all left children are processed.
- The most interesting part is how it handles the left subtree. Let's look at that section in detail:

``` cpp
if (node->left) {
    TreeNode *rightMost = node->left;
    while (rightMost->right) {
        rightMost = rightMost->right;
    }
    rightMost->right = node->right;
    node->right = node->left;
    node->left = NULL;
}
```

This code segment performs three crucial operations:

1. It finds the rightmost node in the left subtree (the node that should connect to the current node's right child)
2. It connects that rightmost node to the current node's right subtree
3. It moves the entire left subtree to become the right child and nullifies the left pointer


---
#### Code3 : Using iterative inorder traversal

``` cpp
void flatten(TreeNode* root) {

	stack<TreeNode*> s;
	TreeNode *cur = root;
	TreeNode *dummy = new TreeNode(), *l1 = dummy;

	while(cur || !s.empty()){

		while(cur){
			if(cur->right)
				s.push(cur->right);

			l1->right = cur;
			l1 = l1->right;

			cur = cur->left;
			l1->left = NULL;
		}

		if(!s.empty()){
			cur = s.top();
			s.pop();
		}

	}
}

```

---

