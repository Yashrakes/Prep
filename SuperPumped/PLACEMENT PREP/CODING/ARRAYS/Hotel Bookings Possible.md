#### Metadata

timestamp: **10:17**  &emsp;  **31-07-2021**
topic tags: #array 
question link: https://www.interviewbit.com/problems/hotel-bookings-possible/
resource: https://www.geeksforgeeks.org/find-k-bookings-possible-given-arrival-departure-times/
parent link: [[1. ARRAY GUIDE]]

---

# Hotel Bookings Possible

### Question

A hotel manager has to process **N** advance bookings of rooms for the next season. His hotel has **C** rooms. Bookings contain an arrival date and a departure date. He wants to find out whether there are enough rooms in the hotel to satisfy the demand.

---


### Approach
The idea is store arrival and departure times in an auxiliary array with an additional marker to indicate whether the time is arrival or departure. Now sort the array. Process the sorted array, for every arrival increment active bookings. And for every departure, decrement. Keep track of maximum active bookings. If the count of active bookings at any moment is more than k, then return false. Else return true.

#### Complexity Analysis

#### Code

``` cpp
bool areBookingsPossible(int arrival[],
                         int departure[], int n, int k)
{
    vector<pair<int, int> > ans;
 
    // create a common vector both arrivals
    // and departures.
    for (int i = 0; i < n; i++) {
        ans.push_back(make_pair(arrival[i], 1));
        ans.push_back(make_pair(departure[i], 0));
    }
 
    // sort the vector
    sort(ans.begin(), ans.end());
 
    int curr_active = 0, max_active = 0;
 
    for (int i = 0; i < ans.size(); i++) {
 
        // if new arrival, increment current
        // guests count and update max active
        // guests so far
        if (ans[i].second == 1) {
            curr_active++;
            max_active = max(max_active,
                             curr_active);
        }
 
        // if a guest departs, decrement
        // current guests count.
        else
            curr_active--;
    }
 
    // if max active guests at any instant
    // were more than the available rooms,
    // return false. Else return true.
    return (k >= max_active);
}

```

---


