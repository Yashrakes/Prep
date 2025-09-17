#### Metadata

timestamp: **14:36**  &emsp;  **30-06-2021**
topic tags: : #design, #imp 
question link: https://leetcode.com/problems/insert-delete-getrandom-o1/
parent link: [[1. DESIGN GUIDE]]

---

# Insert Delete GetRandom O(1)

### Question

Implement the `RandomizedSet` class:

-   `RandomizedSet()` Initializes the `RandomizedSet` object.
-   `bool insert(int val)` Inserts an item `val` into the set if not present. Returns `true` if the item was not present, `false` otherwise.
-   `bool remove(int val)` Removes an item `val` from the set if present. Returns `true` if the item was present, `false` otherwise.
-   `int getRandom()` Returns a random element from the current set of elements (it's guaranteed that at least one element exists when this method is called). Each element must have the **same probability** of being returned.

You must implement the functions of the class such that each function works in **average** `O(1)` time complexity.

---


### Approach

#### Code

``` cpp
unordered_map<int, int> map;
vector<int> arr;

RandomizedSet() {

}

bool insert(int val) {
	if(map.find(val) == map.end()){
		arr.push_back(val);
		map[val] = arr.size()-1;
		return true;
	}
	return false;
}


/*We perform remove by swapping the last element of the array with the element to be deleted and then updating the map of the swapped element*/
bool remove(int val) {
	if(map.find(val) == map.end()) return false;

	int pos = map[val];
	swap(arr[pos], arr[arr.size()-1]);
	arr.pop_back();
	map[arr[pos]] = pos;
	map.erase(val);

	return true;
}


int getRandom() {
	int i = rand() % (arr.size());
	return arr[i];
}
```

---


