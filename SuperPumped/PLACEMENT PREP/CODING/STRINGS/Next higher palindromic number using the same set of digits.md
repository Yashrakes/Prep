#### Metadata

timestamp: **14:44**  &emsp;  **04-07-2021**
topic tags: #string , #imp 
question link: [gfg](https://practice.geeksforgeeks.org/problems/next-higher-palindromic-number-using-the-same-set-of-digits5859/1/?track=md-string&batchId=144#)
resource: https://www.geeksforgeeks.org/next-higher-palindromic-number-using-set-digits/
parent link: [[1. STRING GUIDE]]

---

# Next higher palindromic number using the same set of digits

### Question

Given a palindromic number **N** in the form of string. The task is to find the smallest palindromic number greater than **N** using the same set of digits as in **N**.
If no such number exists return "-1".

---


### Approach
- We only need to consider the `first half of the input number` and find a number that is just greater than the current. This inturn is the next number in the lexicographic order.
- Therefore, we find the Lexicographically next permutation of the number obtained from the first half of the input string. Code --> [[Next Permutation]]


#### Code

``` cpp
string getNextLex(string s){
	int n = s.length();

	//1.Frist find the point of inflextion  
	int i = n-2;
	while(i >= 0 && (s[i] - '0') >= (s[i+1]- '0')) 
		i--;

	if(i == -1){
		//there cannot be a greater no than the current no, hence return not possible
		return "NP";
	}

	int j = n-1;
	while(j >= 0 && (s[j] - '0') <= (s[i] - '0'))
		j--;

	swap(s[i], s[j]);
	reverse(s.begin() + i + 1, s.end());
	return s;
}

string nextPalin(string str) { 
	int n = str.length();

	string nextLex = getNextLex(str.substr(0, n/2));

	if(nextLex == "NP") 
		return "-1";

	string rev = nextLex;
	reverse(rev.begin(), rev.end());

	string res = "";
	if(n%2 == 0)
		res = nextLex + rev;
	else
		res = nextLex + str[n/2] + rev;

	return res == str ? "-1" : res;
}

```

---


