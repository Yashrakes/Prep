#### Metadata

timestamp: **20:05**  &emsp;  **11-08-2021**
topic tags: #bst
question link: https://leetcode.com/problems/convert-sorted-list-to-binary-search-tree/
resource: https://www.geeksforgeeks.org/sorted-linked-list-to-balanced-bst/
parent link: [[1. BST GUIDE]]

---

# Convert Sorted List to Binary Search Tree

### Question

Given the `head` of a singly linked list where elements are **sorted in ascending order**, convert it to a height balanced BST.

For this problem, a height-balanced binary tree is defined as a binary tree in which the depth of the two subtrees of _every_ node never differ by more than 1.

---


### Approach

#### Code 1 : Brute Force
- Find mid at each stage and make it the root
- Time: O(N log N)

``` cpp
/**
 * Definition for singly-linked list.
 * struct ListNode {
 *     int val;
 *     ListNode *next;
 *     ListNode() : val(0), next(nullptr) {}
 *     ListNode(int x) : val(x), next(nullptr) {}
 *     ListNode(int x, ListNode *next) : val(x), next(next) {}
 * };
 */
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
    struct Info{
        ListNode *l1;
        ListNode *mid;
        ListNode *l2;
    };

    Info getMiddle(ListNode *head){
        if(!head) return {head, head, head};

        ListNode *slow = head, *fast = head, *prev = NULL;
        while(fast && fast->next && fast->next->next){
            prev = slow;
            slow = slow->next;
            fast = fast->next->next;
        }

        if(prev) prev->next = NULL;
        ListNode *l1 = prev == NULL ? NULL : head;
        return {l1, slow, slow->next};
    }

    TreeNode *construct(ListNode *l1){
        Info obj = getMiddle(l1);

        if(!obj.mid) return NULL;

        TreeNode *root = new TreeNode((obj.mid)->val);
        root->left = construct(obj.l1);
        root->right = construct(obj.l2);

        return root;
    }

    TreeNode* sortedListToBST(ListNode* head) {
        return construct(head);
    }
};

```

---

#### Code 2 : O(N)

- In this method, we construct from leaves to root. 
- The idea is to insert nodes in BST in the same order as they appear in Linked List so that the tree can be constructed in O(n) time complexity. 
- We first count the number of nodes in the given Linked List. Let the count be n. After counting nodes, we take left n/2 nodes and recursively construct the left subtree. After left subtree is constructed, we allocate memory for root and link the left subtree with root. 
- Finally, we recursively construct the right subtree and link it with root.   
- While constructing the BST, we also keep moving the list head pointer to next so that we have the appropriate pointer in each recursive call.

``` cpp
TreeNode* construct(ListNode *&head, int n){
	if(n <= 0)
		return NULL;

	//Recursively construct the left subtree
	TreeNode *left = construct(head, n/2);

	//Allocate memory for root, and link the above constructed left
	//subtree with root
	TreeNode *root = new TreeNode(head->val);
	root->left = left;

	head = head->next;

	/* Recursively construct the right
	subtree and link it with root
	The number of nodes in right subtree
	is total nodes - nodes in
	left subtree - 1 (for root) which is n-n/2-1*/
	root->right = construct(head, n - n/2 - 1);
	return root;
}

int getCount(ListNode* head) {
	if(!head) return 0;
	return 1 + getCount(head->next);
}

TreeNode* sortedListToBST(ListNode* head) {
	int n = getCount(head);
	return construct(head, n);
}
```

---


