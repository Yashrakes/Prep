
Given a string `s`, return _the longest_ _palindromic_ _substring_ in `s`.

---
# Understanding Manacher's Algorithm Intuitively

## Algorithm
### 1. String Transformation
- We first transform the string by inserting a special character (typically '#') between each character and at the boundaries:
	- Original: "babad"
	- Transformed: "#b#a#b#a#d#"
- This handles both odd and even-length palindromes uniformly.

### 2. Key Variables
- **P[i]**: The radius of the palindrome centered at position i (not counting i itself)
- **C**: Center of the current palindrome that extends furthest right
- **R**: Right boundary of this palindrome

### 3. Core Logic
For each position i:
1. **Mirror Property**: If i is within the right boundary of our current palindrome, we can save time by using the mirror property. The mirror of i around C is (2*C - i).
2. **Expansion**: We try to expand the palindrome centered at position i as far as possible.
3. **Update Center**: If this new palindrome extends beyond our current right boundary R, we update our center and boundary.

### 4. Finding the Answer
After processing the entire string, we locate the position with the maximum palindrome radius and convert it back to the original string's coordinates.

---
## Why Insert Special Characters (#)?

- The special characters serve a critical purpose: **to handle both odd and even length palindromes uniformly**.
- Without this transformation:
	- For odd-length palindromes (like "aba"), the center is a single character
	- For even-length palindromes (like "abba"), the center falls between two characters
- This difference would force us to write two separate expansion routines. By inserting '#' between every character:
- Original: "babad" Transformed: "#b#a#b#a#d#"
- Now **every palindrome has a well-defined center position** in our array. The even-length palindrome "abba" becomes "#a#b#b#a#" with a center at the middle '#'.

---
## The Mirror Property Explained

Let's use that specific example again:

```
Position:   0 1 2 3 4 5 6 7 8 9 10
String:     # a # b # a # b # a #
P[i]:       0 1 0 1 0 5 0 1 0 1 0
```

### Understanding the Current State
- We've processed up to position 5, where P[5] = 5
- This means the palindrome centered at position 5 extends 5 positions in each direction
- So the palindrome spans from position 0 to position 10
- Our current center C = 5 and right boundary R = 10

### The Key Insight
- When we move to position 6, we notice it's within the current palindrome boundary (it's less than R = 10). Here's the crucial insight:
- **Position 6 is a mirror image of position 4 with respect to the center (position 5).**
- This means that the characters surrounding position 6 are exactly the same as those surrounding position 4, but in reverse order - _as long as we stay within the bounds of the larger palindrome_.

### Why This Works: Visual Explanation
Let's look at a section of our string, specifically positions 3 to 7:
```
3 4 5 6 7
b # a # b
```

- This is part of the palindrome centered at position 5. The position 5 is 'a', position 4 is '#', and position 6 is also '#'.
- Because the entire segment from 0 to 10 is a palindrome around center 5:
	- Whatever character is at (5-1) must match the character at (5+1)
	- Whatever character is at (5-2) must match the character at (5+2)
	- And so on...

### The Mathematical Formulation

For any position i within our current palindrome (centered at C):
- Its mirror position is: mirror = 2*C - i
    - This formula gives us the position that's the same distance from C but on the opposite side
- The palindrome centered at i must match the palindrome centered at mirror, up to the boundary of the larger palindrome

### Working With Boundaries: Why Use min(R-i, P[mirror])
There's one caveat: this mirroring only works within the bounds of the larger palindrome. Let's say that at position 4, we had P[4] = 2. This means:
1. The palindrome at position 4 extends 2 positions each way
2. But if the palindrome at position 6 would extend beyond position 10 (our right boundary R), we can't be certain it's still a palindrome
That's why we use:
```
P[i] = min(R - i, P[mirror]);
```
- If P[mirror] is small enough that the palindrome at position i stays within R, we can use P[mirror] directly
- If P[mirror] is too large, we can only be certain up to the boundary (R - i)

### Example Walkthrough for Position 6
1. Position 6 is inside our current palindrome (6 < R = 10)
2. Its mirror is position 4 (2_C - i = 2_5 - 6 = 4)
3. At position 4, we have P[4] = 0
4. We also calculate R - i = 10 - 6 = 4
5. So P[6] = min(4, 0) = 0 to start with
From here, we might still try to expand the palindrome at position 6, but we're starting with some useful information already.

### The Breakthrough

- This is the key efficiency gain: instead of starting from scratch for each position, we're leveraging what we already know about mirrored positions. This transforms what would be an O(n²) approach into O(n).
- In the best case, we might not need to perform any character comparisons at all for a position, if its mirror tells us exactly how large its palindrome radius is. In the worst case, we still have to expand, but we're not redoing work we've already done.

---
## Why Is It O(n)?
The key insight: **each character is compared at most twice.**
- When expanding, we compare a character only when expanding a palindrome
- The right boundary R always moves right (never decreases)
- Each time we expand beyond R, we're discovering new palindromes, not redoing work

---
#### Code: Manacher's Algo - O (n)

``` cpp
class Solution {
public:
    // using Manacher's Algorithm
    string longestPalindrome(string s) {
        string t = "#";
        for (char ch : s) {
            t += ch;
            t += '#';
        }

        int n = t.length();
        vector<int> P(n, 0); // p[i] = length of the radius of the largest palindrome centred at i;
        int R = 0; // R is the index of the rightmost boundary of the largest palindrome centred at C
        int C = 0; // index of the center of the largest palindrom

        for (int i = 0; i < n; i++) {
            // Mirror index of i with respect to C
            int mirror = 2*C - i;

            // If i is within the right boundary, use precomputed values
            if (i < R) {
                P[i] = min(R - i, P[mirror]);
            }

            // Attempt to expand palindrome centered at i
            int left = i - (P[i] + 1);
            int right = i + (P[i] + 1);
            while (left >= 0 && right < n && t[left] == t[right]) {
                P[i]++;
                left--;
                right++;
            }

            // If palindrome centered at i expands past R,
            // adjust center and right boundary
            if (i + P[i] > R) {
                R = i + P[i];
                C = i;
            }
        }

        int maxLen = 0, centerIndex = 0;
        for (int i = 0; i < n; i++) {
            if (P[i] > maxLen) {
                maxLen = P[i];
                centerIndex = i;
            }
        }

        int startIndex = (centerIndex - maxLen)/2;
        return s.substr(startIndex, maxLen);
    }
};
```