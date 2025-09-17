#### Metadata

timestamp: **10:50**  &emsp;  **04-07-2021**
topic tags: #string, #imp
question link: https://leetcode.com/problems/valid-parenthesis-string/
resource: 
parent link: [[1. STRING GUIDE]]

---

# Valid Parenthesis String

### Question

Given a string `s` containing only three types of characters: `'('`, `')'` and `'*'`, return `true` _if_ `s` _is **valid**_.

The following rules define a **valid** string:

-   Any left parenthesis `'('` must have a corresponding right parenthesis `')'`.
-   Any right parenthesis `')'` must have a corresponding left parenthesis `'('`.
-   Left parenthesis `'('` must go before the corresponding right parenthesis `')'`.
-   `'*'` could be treated as a single right parenthesis `')'` or a single left parenthesis `'('` or an empty string `""`.

---


### Approach

- `lCount:`  It is the count of the no of outstanding parenthesis (left - right)
    
- `occupiedStarC:` It is the count of all stars which have been greedily assigned a closing parenthesis. If we have an outstanding opening parenthesis, we can greedily assign the star as a closing parenthesis.

- `unoccStarC:` Count of all stars which have not yet been assigned.- 

#### Code

``` cpp
bool checkValidString(string s) {
	int n = s.size(), i, lCount = 0, occupiedStarC = 0, unoccStarC = 0;

	for(int i = 0; i < n; i++){
		if(s[i] == '(') lCount++;
		else if(s[i] == '*') {

			//Greedily satisfy an opening parenthesis
			if(lCount - occupiedStarC > 0)
				occupiedStarC++;
			else
				unoccStarC++;
		}
		//if s[i] is ')'
		else {

		//if there are any outstanding openinig bracket, then close it with the current char
			if(lCount - occupiedStarC > 0)
				lCount--;

			//if no of outstanding left <= occupiedStarC, which means that more than required
			//stars have been assigned a closing bracket, we can match the left with the 
			//current closing bracket and transfer the star assignment to unoccupied
			//in summary, we free an occupied star if a left match is possible
			else if(occupiedStarC > 0){
				occupiedStarC--;
				unoccStarC++;
				lCount--;
			}
			//we assign a star as openinig bracket for this closing bracket
			else if(unoccStarC > 0)
				unoccStarC--;
			else 
				return false;
		}
	}

	return lCount - occupiedStarC == 0;
}

```

---


