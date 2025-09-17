#### Metadata

timestamp: **17:50**  &emsp;  **27-06-2021**
topic tags: #greedy , #imp 
question link: https://practice.geeksforgeeks.org/problems/job-sequencing-problem-1587115620/1#
resource: 
https://www.geeksforgeeks.org/job-sequencing-problem/
https://www.geeksforgeeks.org/job-sequencing-using-disjoint-set-union/
parent link: [[1. GREEDY GUIDE]]

---

# Job Sequencing Problem

### Question
Given a set of **N** jobs where each job _i_ has a deadline and profit associated to it. Each job takes _1_ unit of time to complete and only one job can be scheduled at a time. We earn the profit if and only if the job is completed by its deadline. The task is to find the **maximum profit** and the number of jobs done.

**Note:** Jobs will be given in the form (Job id, Deadline, Profit) associated to that Job.

---


### Approach 1 : Greedy

#### Algorithm
1) Sort all jobs in **decreasing** order of profit.   
2) Iterate on jobs in decreasing order of profit.For each job , do the following :
	1.  For each job find an empty time slot **from deadline to 0**. If found empty slot put the job in the slot and mark this slot filled.
#### Complexity Analysis
- **Time Complexity** of the above solution is O(n^2)
#### Code

``` cpp
vector<int> JobScheduling(Job arr[], int n) 
{ 
	sort(arr, arr+n, [](const Job &a, const Job &b){
			return a.profit > b.profit;
	});

	vector<bool> slot(n, false);
	int count = 0, profit = 0;
	for(int i = 0; i < n; i++){

		for(int j = min(n, arr[i].dead)-1; j >= 0; j--){
			if(slot[j] == false){
				slot[j] = true;
				profit += arr[i].profit;
				count++;
				break;
			}
		}
	}

	return {count, profit};
} 

```

---

### Approach 2 : Disjoint Set Union
https://www.geeksforgeeks.org/job-sequencing-using-disjoint-set-union/
#### Algorithm

#### Complexity Analysis
- **Time Complexity** of the above solution is O(n x n log n)
#### Code

``` cpp
class Solution 
{
    struct DisjointSet{
        int *parent;
        
        DisjointSet(int n){
            parent =  new int[n+1];
            
			// Every node is a parent of itself
            for(int i = 0; i <= n; i++)
                parent[i] = i;
        }
        
        int find(int s){
            if(s == parent[s]) return s;
            
            //Path compression
            return parent[s] = find(parent[s]);
        }
        
		// Makes u as parent of v.
        void merge(int u, int v){
            parent[v] = u;
        }
    };
    
    public:
    int findMaxDeadline(Job arr[], int n)
    {
        int ans = INT_MIN;
        for (int i = 0; i < n; i++)
            ans = max(ans, arr[i].dead);
        return ans;
    }
    
    
    vector<int> JobScheduling(Job arr[], int n) 
    { 
        sort(arr, arr+n, [](const Job &a, const Job &b){
                return a.profit > b.profit;
        });
        
		// Find the maximum deadline among all jobs and
   		// create a disjoint set data structure with
    	// maxDeadline disjoint sets initially.
        int max_deadline = findMaxDeadline(arr, n);
        DisjointSet ds(max_deadline);
        
        int count = 0, profit = 0;
        for(int i = 0; i < n; i++){
		
			// Find the maximum available free slot for
        	// this job (corresponding to its deadline)
            int availableSlot = ds.find(arr[i].dead);
			
			// If maximum available free slot is greater
        	// than 0, then free slot available
            if(availableSlot > 0){
			
				// This slot is taken by this job 'i'
				// so we need to update the greatest
				// free slot. Note that, in merge, we
				// make first parameter as parent of
				// second parameter. So future queries
				// for availableSlot will return maximum
				// available slot in set of
				// "availableSlot - 1"
                ds.merge(availableSlot-1, availableSlot);
                profit += arr[i].profit;
                count++;
            }
        }
       return {count, profit};
    } 
};

```

---
