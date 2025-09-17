### Key Insight: Majority Element Properties

If an element is the majority, it must appear more than n/2n/2n/2 times in an array of size nnn. This means:

- If we start "pairing off" elements by cancelling one instance of the candidate and one instance of any other element, the majority element will still remain because there are simply more instances of it than any other element.

### How the Algorithm Leverages This Insight

1. **Candidate Selection Phase**:
    
    - We initialize a count to zero, and as we iterate through the array:
        - If the count is zero, we set the current element as the candidate.
        - If the element matches the candidate, we increase the count.
        - If it doesn’t, we decrease the count (essentially "cancelling out" the candidate against this different element).
    - The idea here is that elements that don’t have the potential to be the majority will be cancelled out. By the end of the first pass, the count for the actual majority element (if it exists) will have outlasted the cancellations against other elements, so it will emerge as the candidate.
2. **Candidate Validation Phase**:
    
    - After the first pass, the algorithm only suggests a potential majority element, not a guaranteed one.
    - We need a second pass to confirm that this candidate is indeed the majority by counting its occurrences and checking if it appears more than n/2 times.


```
class Solution {
public:
    int majorityElement(vector<int>& nums) {
        int count  = 0, candidate;

        for(int num : nums){
            if(count == 0)
                candidate = num;

            if(num == candidate)
                count++;
            else
                count--;
        }

        return candidate;
    }
};
```