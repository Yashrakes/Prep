#### Metadata

timestamp: **12:36**  &emsp;  **24-06-2021**
question link: https://www.geeksforgeeks.org/print-postorder-from-given-inorder-and-preorder-traversals/
parent link: [[1. TREE GUIDE]]
tags: #doubt

---

# Print Postorder traversal from given Inorder and Preorder traversals

### Question
Given Inorder and Preorder traversals of a binary tree, print Postorder traversal.


---


### Approach


#### Code

``` cpp
#include<bits/stdc++.h>
using namespace std;
 
int preIndex = 0;
void printPost(int in[], int pre[], int inStrt,
               int inEnd, map<int, int> hm)
{
    if (inStrt > inEnd)
        return;        
 
    // Find index of next item in preorder traversal in
    // inorder.
    int inIndex = hm[pre[preIndex++]];
 
    // traverse left tree
    printPost(in, pre, inStrt, inIndex - 1, hm);
 
    // traverse right tree
    printPost(in, pre, inIndex + 1, inEnd, hm);
 
    // print root node at the end of traversal
    cout << in[inIndex] << " ";
}
 
void printPostMain(int in[], int pre[],int n)
{
    map<int,int> hm ;
    for (int i = 0; i < n; i++)
    hm[in[i]] = i;
         
    printPost(in, pre, 0, n - 1, hm);
}
 
// Driver code
int main()
{
    int in[] = { 4, 2, 5, 1, 3, 6 };
    int pre[] = { 1, 2, 4, 5, 3, 6 };
    int n = sizeof(pre)/sizeof(pre[0]);
     
    printPostMain(in, pre, n);
    return 0;
}

```

---


