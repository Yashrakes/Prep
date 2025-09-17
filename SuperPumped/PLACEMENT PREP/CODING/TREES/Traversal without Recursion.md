#### Metadata

timestamp: **13:49**  &emsp;  **24-06-2021**
parent link: [[1. TREE GUIDE]]

---

# Traversal without Recursion

### Inorder


#### Code

``` cpp
void inOrder(struct Node *root)
{
    stack<Node *> s;
    Node *curr = root;
 
    while (curr != NULL || s.empty() == false)
    {
        /* Reach the left most Node of the
           curr Node */
        while (curr !=  NULL)
        {
            /* place pointer to a tree node on
               the stack before traversing
              the node's left subtree */
            s.push(curr);
            curr = curr->left;
        }
 
        /* Current must be NULL at this point */
        curr = s.top();
        s.pop();
 
        cout << curr->data << " ";
 
        /* we have visited the node and its
           left subtree.  Now, it's right
           subtree's turn */
        curr = curr->right;
 
    } /* end of while */
}

```

---
### Preorder



#### Code 1

``` cpp
/**
 * Definition for a binary tree node.
 * struct TreeNode {
 *     int val;
 *     TreeNode *left;
 *     TreeNode *right;
 *     TreeNode() : val(0), left(nullptr), right(nullptr) {}
 *     TreeNode(int x) : val(x), left(nullptr), right(nullptr) {}
 *     TreeNode(int x, TreeNode *left, TreeNode *right) : val(x), left(left), right(right) {}
 * };
 */
class Solution {
public:
    vector<int> preorderTraversal(TreeNode* root) {
        
        if(!root) return {};
        
        vector<int> res;
        stack<TreeNode*> s;
        s.push(root);
        
        while(!s.empty()){
            TreeNode* cur = s.top();
            s.pop();
            res.push_back(cur->val);
            
			//WE push right child first, so that left tree is processed before right
            if(cur->right) s.push(cur->right);
            if(cur->left) s.push(cur->left);
        }
        
        
        return res;
    }
};

```

#### Code2 : Space Optimized

``` cpp

vector<int> preorderTraversal(TreeNode* root) {

	if(!root) return {};

	vector<int> res;
	stack<TreeNode*> s;
	TreeNode* cur = root;

	while(cur || !s.empty()){

		while(cur){
			res.push_back(cur->val);
			if(cur->right) s.push(cur->right);

			cur = cur->left;
		}

		if(!s.empty()){
			cur = s.top();
			s.pop();
		}

	}


	return res;
}
```

---

### Postorder



#### Code1 : Using Two Stacks
- Instead of LRV, we do VRL thereby finding postorder in reverse.
- We use a stack, to store the result.
``` cpp
/**
 * Definition for a binary tree node.
 * struct TreeNode {
 *     int val;
 *     TreeNode *left;
 *     TreeNode *right;
 *     TreeNode() : val(0), left(nullptr), right(nullptr) {}
 *     TreeNode(int x) : val(x), left(nullptr), right(nullptr) {}
 *     TreeNode(int x, TreeNode *left, TreeNode *right) : val(x), left(left), right(right) {}
 * };
 */
class Solution {
public:
    vector<int> postorderTraversal(TreeNode* root) {
        if(!root) return {};
        vector<int> res;
        stack<TreeNode*> s;
        stack<int> ans;
        TreeNode* cur = root;        
        
        while(cur || !s.empty()){
            
            while(cur){
                ans.push(cur->val);
                
                if(cur->left) s.push(cur->left);
                
                cur = cur->right;
            }
            
            if(!s.empty()){
                cur = s.top();
                s.pop();
            }
        }
        
        
        
        while(!ans.empty()){
            res.push_back(ans.top());
            ans.pop();
        }
        return res;
    }
};

```

---

#### Code2 : Using One Stack

The intuition behind the one-stack approach:

1. We need to visit a node's children before the node itself
2. We keep track of the last visited node to know whether we've finished processing the right subtree
3. For each node, we:
    - First try to go as far left as possible
    - Then check if we can go right
    - If we can't go right (either no right child or already visited), process the current node

The key insight is that we need `lastVisited` to help us determine whether:

- We're seeing a node for the first time (go left)
- We've processed the left subtree but not the right (go right)
- We've processed both subtrees (process the node)

``` cpp
vector<int> postorderTraversalOneStack(TreeNode* root) {
    vector<int> result;
    if (!root) return result;
    
    stack<TreeNode*> s;
    TreeNode* current = root;
    TreeNode* lastVisited = nullptr;
    
    while (current || !s.empty()) {
        // Phase 1: Go all the way left
        while (current) {
            s.push(current);
            current = current->left;
        }
        
        // Peek at the top node
        current = s.top();
        
        // If right child exists and hasn't been visited yet
        if (current->right && lastVisited != current->right) {
            // Move to right subtree
            current = current->right;
        } else {
            // Process current node
            result.push_back(current->val);
            lastVisited = current;
            s.pop();
            current = nullptr;  // Force next iteration to pop from stack
        }
    }
    
    return result;
}
```

---

#### Code3 : Using One Stack

- Step1:  Create an empty stack
- Step 2:
	-  Do following while root is not NULL
		- Push root's right child and then root to stack.
		- Set root as root's left child.
	- Pop an item from stack and set it as root.
		- If the popped item has a right child and the right child 
		   is at top of stack, then remove the right child from stack,
		   push the root back and set root as root's right child.
		- Else print root's data and set root as NULL.
	- Repeat steps 2.1 and 2.2 while stack is not empty.

``` cpp
vector<int> postorderTraversal(TreeNode* root) {
	if(!root) return {};

	vector<int> res;
	stack<TreeNode*> s;

	TreeNode* cur = root;

	while(cur || !s.empty()){

		while(cur){
			if(cur->right) s.push(cur->right);
			s.push(cur);
			cur = cur->left;
		}

		cur = s.top();
		s.pop();

		if(cur->right && !s.empty() && cur->right == s.top()){
			s.pop();
			s.push(cur);
			cur = cur->right;
		}
		else{
			res.push_back(cur->val);
			cur = NULL;
		}
	}

	return res;
}

```

---