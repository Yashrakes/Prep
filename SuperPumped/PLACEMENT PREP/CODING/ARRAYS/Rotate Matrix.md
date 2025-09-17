#### Metadata

timestamp: **21:03**  &emsp;  **28-06-2021**
topic tags: #array , #imp 
question link: https://leetcode.com/problems/rotate-image/
resource: https://www.youtube.com/watch?v=Y72QeX0Efxw&list=PLgUwDviBIf0rPG3Ictpu74YWBQ1CaBkm2&index=12
parent link: [[1. ARRAY GUIDE]]

---

# Rotate Matrix

### Question

You are given an `n x n` 2D `matrix` representing an image, rotate the image by **90** degrees (clockwise).

You have to rotate the image [**in-place**](https://en.wikipedia.org/wiki/In-place_algorithm), which means you have to modify the input 2D matrix directly. **DO NOT** allocate another 2D matrix and do the rotation

---


### Approach


#### Code 1

``` cpp
void rotate(vector<vector<int>>& matrix) {
	int n = matrix.size();
	for (int i = 0; i < (n + 1) / 2; i ++) {
		for (int j = 0; j < n / 2; j++) {
			int temp = matrix[n - 1 - j][i];
			matrix[n - 1 - j][i] = matrix[n - 1 - i][n - j - 1];
			matrix[n - 1 - i][n - j - 1] = matrix[j][n - 1 -i];
			matrix[j][n - 1 - i] = matrix[i][j];
			matrix[i][j] = temp;
		}
	}
}

```


#### Code 2
- Take transpose of the matrix
- reverse all the rows of the matrix

``` cpp
void rotate(vector<vector<int>>& matrix) {
	int n = matrix.size();

	for(int i = 0; i < n; i++)
		for(int j = 0; j < i; j++)
			swap(matrix[i][j], matrix[j][i]);

	for(int i = 0; i < n; i++)
		reverse(matrix[i].begin(), matrix[i].end());
}

```

---


