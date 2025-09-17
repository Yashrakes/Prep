#### Metadata

timestamp: **23:44**  &emsp;  **27-06-2021**
topic tags: #greedy , #imp 
question link: https://www.geeksforgeeks.org/job-sequencing-problem-loss-minimization/
parent link: [[1. GREEDY GUIDE]]

---

# Job Sequencing Problem – Loss Minimization

### Question

We are given N jobs numbered 1 to N. For each activity, let Ti denotes the number of days required to complete the job. For each day of delay before starting to work for job i, a loss of Li is incurred.  
We are required to find a sequence to complete the jobs so that overall loss is minimized. We can only work on one job at a time.

>Input :
>L = {3, 1, 2, 4} and 
>T = {4, 1000, 2, 5}
Output : 
3, 4, 1, 2

---


### Approach

#### Algorithm
Let us consider two extreme cases and we shall deduce the general case solution from them.

1.  All jobs take same time to finish, i.e Ti = k for all i. Since all jobs take same time to finish we should first select jobs which have large Loss (Li). We should select jobs which have the highest losses and finish them as early as possible. Thus this is a greedy algorithm. Sort the jobs in descending order based on Li only.
2.  All jobs have the same penalty. Since all jobs have the same penalty we will do those jobs first which will take less amount of time to finish. This will minimize the total delay, and hence also the total loss incurred.  This is also a greedy algorithm. Sort the jobs in ascending order based on Ti. Or we can also sort in descending order of 1/Ti.
3.  From the above cases, we can easily see that we should sort the jobs not on the basis of Li or Ti alone. Instead, we should sort the jobs according to the ratio Li/Ti, in descending order.
4.  We can get the lexicographically smallest permutation of jobs if we perform a [**stable sort**](https://www.geeksforgeeks.org/stability-in-sorting-algorithms/) on the jobs. An example of a stable sort is [merge sort](https://www.geeksforgeeks.org/merge-sort/).

To get most accurate result avoid dividing Li by Ti. Instead, compare the two ratios like fractions. To compare a/b and c/d, compare ad and bc.
#### Complexity Analysis

#### Code

``` cpp
typedef pair<int, pair<int, int> > job;

bool cmp_pair(job a, job b)
{
	int a_Li, a_Ti, b_Li, b_Ti;
	a_Li = a.second.first;
	a_Ti = a.second.second;
	b_Li = b.second.first;
	b_Ti = b.second.second;

	// To compare a/b and c/d, compare ad and bc
	return (a_Li * b_Ti) > (b_Li * a_Ti);
}

void printOptimal(int L[], int T[], int N)
{
	vector<job> list; // (Job Index, Si, Ti)

	for (int i = 0; i < N; i++) {
		int t = T[i];
		int l = L[i];

		// Each element is: (Job Index, (Li, Ti) )
		list.push_back(make_pair(i + 1, make_pair(l, t)));
	}

	stable_sort(list.begin(), list.end(), cmp_pair);

	// traverse the list and print job numbers
	cout << "Job numbers in optimal sequence are\n";
	for (int i = 0; i < N; i++)
		cout << list[i].first << " ";

}

```

---


