#### Metadata

timestamp: **17:17**  &emsp;  **30-06-2021**
topic tags: #two_pointer , #imp 
question link: https://leetcode.com/problems/trapping-rain-water/
resource: [TUF](https://www.youtube.com/watch?v=m18Hntz4go8&list=PLgUwDviBIf0p4ozDR_kJJkONnb1wdx2Ma&index=43)
parent link: [[1. TWO POINTER GUIDE]]
optimal solution approach: https://leetcode.com/problems/trapping-rain-water/solutions/1374608/c-java-python-maxleft-maxright-so-far-with-picture-o-1-space-clean-concise/?envType=study-plan-v2&envId=top-interview-150

---

# Trapping Rain Water

### Question

Given `n` non-negative integers representing an elevation map where the width of each bar is `1`, compute how much water it can trap after raining.

**Example 1:**

![](https://assets.leetcode.com/uploads/2018/10/22/rainwatertrap.png)

**Input:** height = [0,1,0,2,1,0,1,3,2,1,2,1]
**Output:** 6
**Explanation:** The above elevation map (black section) is represented by array [0,1,0,2,1,0,1,3,2,1,2,1]. In this case, 6 units of rain water (blue section) are being trapped.

---


### Approach

- Do a dry run to get the intuition behind the approach.

#### Complexity Analysis
- Time: O(n)
- Space: O(1)

#### Code

``` cpp
int trap(vector<int>& height) {
	int left = 0, right = height.size()-1;
	int leftMax = 0, rightMax = 0, capacity = 0;

	while(left < right)
	{
		if(height[left] < height[right]) {
			//this means that there is a taller bar on the right side of pointer left
			//hence the deciding height is leftMax
			if(height[left] >= leftMax){
				//then no water can be stored and left becomes my left max
				leftMax = height[left];
			} else {
				capacity += leftMax - height[left];
			}
			left++;
		}
		else
		{
			if(height[right] >= rightMax)
				rightMax = height[right];
			else
				capacity += rightMax - height[right];
			right--;
		}
	}
	return capacity;
}

```

---


