#### Metadata

timestamp: **21:46**  &emsp;  **09-08-2021**
topic tags: #design, #imp
question link: https://leetcode.com/problems/min-stack/
resource:https://www.geeksforgeeks.org/design-a-stack-that-supports-getmin-in-o1-time-and-o1-extra-space/
parent link: [[1. DESIGN GUIDE]]

---

# Min Stack

### Question

Design a stack that supports push, pop, top, and retrieving the minimum element in constant time.

Implement the `MinStack` class:

-   `MinStack()` initializes the stack object.
-   `void push(val)` pushes the element `val` onto the stack.
-   `void pop()` removes the element on the top of the stack.
-   `int top()` gets the top element of the stack.
-   `int getMin()` retrieves the minimum element in the stack.

`Follow Up: Do it in O(1) space
`
---


### Approach
- We define a variable **minEle** that stores the current minimum element in the stack. Now the interesting part is, how to handle the case when minimum element is removed. To handle this, we push “2x – minEle” into the stack instead of x so that previous minimum element can be retrieved using current minEle and its value stored in stack.
- **Push(x)** : Inserts x at the top of stack.   
	-   If stack is empty, insert x into the stack and make minEle equal to x.
	-   If stack is not empty, compare x with minEle. Two cases arise:
		-   If x is greater than or equal to minEle, simply insert x.
		-   If x is less than minEle, insert (2\*x – minEle) into the stack and make minEle equal to x. For example, let previous minEle was 3. Now we want to insert 2. We update minEle as 2 and insert 2\*2 – 3 = 1 into the stack.
- **Pop() :** Removes an element from top of stack.   
	-   Remove element from top. Let the removed element be y. Two cases arise:
		-   If y is greater than or equal to minEle, the minimum element in the stack is still minEle.
		-   If y is less than minEle, the minimum element now becomes (2\*minEle – y), so update (minEle = 2\*minEle – y). This is where we retrieve previous minimum from current minimum and its value in stack. For example, let the element to be removed be 1 and minEle be 2. We remove 1 and update minEle as 2\*2 – 1 = 3.



#### HOW?
- So we need to find a way to encode the new minimum element with the previous minimum element and then update the minimum element with the new one. After we encode the two mins using a special formula, if later a pop operation comes in which this encoded number is at the top, it means we've to remove the new min from stack and as we stored separately, we can get back the previous min from this encoded number. 

- The simplest way you can encode the new min with the previous min is a linear combination of them both, as in, push some "c1 * new_min + c2 * prev_min (where c1 and c2 are some integers). 

- But there's a small catch. We need to ensure that our encoded number is less than the new min, because we need to know as to when do we have to remove the new min and retrieve the previous one. The best way to know is to see if the current stack top has a value smaller than the new minimum. It should technically be impossible that an element in stack is smaller than the new minimum, because if it was, then our new minimum would've been that element instead (I hope it's not too confusing lol).

- So we need to reach this point: 
c1 * new_min + c2 * prev_min < new_min (so the equation to the LHS is our encoding formula)

- If c1 * new_min + c2 * prev_min is our formula for encoding the 2 numbers, what's our decoding formula? As in, when we've to pop this encoded number, how we get the previous minimum? We simply do this: rearrange the equation 
	
```
encoded_number = c1 * new_min + c2 * prev_min

to

prev_min = (encoded number - c1 * min) / c2  or  (c1 * min - encoded number) / - c2   
```


- As a good programmer, you need to understand that even if at a large scale it may not matter, but it's better to assign c2 as -1 (For 2nd decoding formula) or 1 (for 1st decoding formula) so that we don't have to do any extra division while decoding back. We'll see whether c2 becomes -1 or 1 based on the encoding formula we generate, and then accordingly the decoding formula will be known.

- So now we've to only find what is c1 and create a formula which has c2 = 1 or c2 = -1.

- We know that new_min < prev_min.
We can change new_min to 2 * new_min - new_min:
2 \* new_min - new_min < prev_min

- Then we rearrange to get:
2 * new_min - prev_min < new_min    (which is the way we wanted our formula to be)

- So our c1 = 2 and c2 = -1. And since c2 = -1, our decoding formula will be the 2nd one above.

- And that's why we push that (2 * ME - Y) number on stack. I hope this clears up the intuition behind the formula.

- If you don't care about division's time complexity, you can generate your own c1 and c2 like this by playing with the values and starting from new_min < prev_min and getting the inequation of that type which I said above.

#### Code

``` cpp
struct MyStack
{
    stack<int> s;
    int minEle;
 
    // Prints minimum element of MyStack
    void getMin()
    {
        if (s.empty())
            cout << "Stack is empty\n";
 
        // variable minEle stores the minimum element
        // in the stack.
        else
            cout <<"Minimum Element in the stack is: "
                 << minEle << "\n";
    }
 
    // Prints top element of MyStack
    void peek()
    {
        if (s.empty())
        {
            cout << "Stack is empty ";
            return;
        }
 
        int t = s.top(); // Top element.
 
        cout << "Top Most Element is: ";
 
        // If t < minEle means minEle stores
        // value of t.
        (t < minEle)? cout << minEle: cout << t;
    }
 
    // Remove the top element from MyStack
    void pop()
    {
        if (s.empty())
        {
            cout << "Stack is empty\n";
            return;
        }
 
        cout << "Top Most Element Removed: ";
        int t = s.top();
        s.pop();
 
        // Minimum will change as the minimum element
        // of the stack is being removed.
        if (t < minEle)
        {
            cout << minEle << "\n";
            minEle = 2*minEle - t;
        }
 
        else
            cout << t << "\n";
    }
 
    // Removes top element from MyStack
    void push(int x)
    {
        // Insert new number into the stack
        if (s.empty())
        {
            minEle = x;
            s.push(x);
            cout <<  "Number Inserted: " << x << "\n";
            return;
        }
 
        // If new number is less than minEle
        if (x < minEle)
        {
            s.push(2*x - minEle);
            minEle = x;
        }
 
        else
           s.push(x);
 
        cout <<  "Number Inserted: " << x << "\n";
    }
};

```

---


