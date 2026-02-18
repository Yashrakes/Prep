

**

Describe an instance when you dealt with a demanding customer and how you arrived at a solution to resolve a fundamental problem.

Situation:

Regulatory Flag ka bolna hai… Global Asset Class. Ka… 

Idher critical escalation raise Kiya tha user ne Vikas level tak. 

  
While working as a backend developer on the Credit Limit Approval (CLA) project, we received a critical escalation from a senior stakeholder who was facing repeated delays in limit approvals due to incorrect rule executions. This issue was causing reputational risk and operational inefficiency for the business, and the stakeholder was understandably demanding and frustrated.

Task:  
I was tasked with investigating the issue, providing a timely fix, and ensuring no recurrence—while also managing communication with a non-technical but high-stakes customer.

Action:

1. Quick Diagnosis: I coordinated with the operations and business teams to collect logs and examples. I discovered that the rule engine was not correctly applying certain parameters during parallel rule executions for multiple loan limits.  
      
    
2. Root Cause Fix: I proposed and implemented a parameterized execution strategy—ensuring that each limit request would be processed independently with proper isolation of rule data.  
      
    
3. Process Improvement: I added internal validations and updated the rule tracking to provide clearer feedback on rule failures in the UI.  
      
    
4. Stakeholder Handling: I proactively kept the customer informed through daily updates, presented a clear RCA, and offered a demo of the fix in the UAT environment.  
      
    

Result:  
The issue was resolved ahead of the expected deadline, which not only restored the stakeholder's confidence but also improved processing efficiency for future requests.

  

  

  

  

  

Tell us about a challenging personal circumstance and how you dealt with it.

Situation:  
During one of my important work projects, my father had to go through a nose surgery. He was admitted to the hospital and my family needed my support during recovery. At the same time, we were close to delivering the Lite Approval feature. Most of the development was done, and what was left was mainly user testing and coordination with other teams.

Challenge:  
It was a difficult time because I had to manage hospital visits and take care of my father, while also making sure the project moved forward smoothly.

Action:

1. I informed my manager about the situation and asked for flexibility in working hours.  
      
    
2. I adjusted my schedule to work early in the mornings or late evenings, and made sure I was available during important meetings and testing sessions.  
      
    
3. I took help from my teammates for some small tasks and I also kept regular updates going so that there was clear communication with everyone involved.
    

Result:  
We completed the user testing on time, and the Lite Approval feature was delivered successfully. My manager appreciated how I handled everything during a tough personal situation. This experience helped me learn how to stay calm and manage work and personal life together.

  

  

  

  

  

  

  

  

  

How do you analyze risk? Describe a time when you took a risk that paid off.

How I analyze risk:  
When I analyze risk, I look at:

1. Impact – What could break or how badly it will affect users or timelines.  
      
    
2. Likelihood – How likely is the issue to happen?  
      
    
3. Mitigation – Do we have a backup or rollback plan?
    

I also check with team members or seniors, and prefer testing in a lower environment before taking the final call.

---

A time I took a risk that paid off:  
We had to make changes in the NGINX configuration for a new backend route. But there was a major concern — if we didn’t release the changes now, the NGINX release would get delayed by 3 months due to the release cycle. That would block other features too.

So I carefully analyzed the situation:

- I tested the NGINX config locally using syntax checks and dry runs.  
      
    
- I had a rollback plan ready if anything failed.  
      
    
- Then I deployed the changes to the FT environment, not directly to production.
    

I monitored it and also asked the QA team to do a quick test.

Result:  
The deployment on FT was successful, testing passed, and we gained confidence to include it in the next release. Because of this early deployment, we avoided a long delay. My team and manager appreciated the proactive risk analysis and safe execution.

  

  

What would you do if you discovered your manager was going against company policy?

I have handled the situation respectfully and carefully.

Real situation:  
Once, during a past project, my manager asked me to skip one level of mandatory peer code review to speed up the release. According to company policy, every code change had to be reviewed and approved by another developer before merging.

At first, I thought maybe it was an exception. But after checking, I realized that it clearly violated our code quality policy and could cause production issues later.

What I did:

1. I had a private conversation with my manager. I respectfully shared my concern and mentioned that skipping the review might cause problems and go against company rules.  
      
    
2. I offered an alternative: I quickly arranged a peer review with a teammate, so we didn’t lose much time but still followed the policy.  
      
    
3. My manager appreciated the suggestion and agreed.  
      
    

Result:  
We met the deadline without breaking policy. The code went live smoothly and later became part of our standard practice — to plan reviews earlier in time-sensitive projects.

  

  

  

  

  

  

  

  

  

Describe your ideal work environment.

My ideal work environment is one where people are customer-focused, take ownership of their work, and are encouraged to innovate. I enjoy being part of a team where everyone is motivated, supports each other, and is willing to go the extra mile to solve tough problems.

I also value an environment where:

- Ideas are welcomed, regardless of title or level.
    
- There’s a balance of autonomy and collaboration, where I can take responsibility for what I build but also work closely with others to deliver end-to-end solutions.
    
- Data-driven decision making is encouraged — not just opinions.
    
- And where there’s a strong culture of learning and continuous improvement, even if it means failing fast and learning from mistakes.
    

From what I know, Amazon encourages ownership, fast-paced execution, and customer-first thinking — which aligns very well with how I like to work.

  

  

  

Can you share an example of how you have saved money for your previous business?

Most importantly, I introduced .zip compression for the build artifacts before uploading them to Artifactory. This helped compress the final build files significantly.

  

Describe what happened the last time you had to go out of your way for a customer.

Tell me about a time when you took ownership of a problem.

Dono me Lite Approval ka bolna hai…. 

  

  

  

  

  

  

  

  

  

  

  

What is the most interesting project you have worked on that you haven't had the chance to talk about yet?

Risk Tracker -> Application…

What is your greatest professional accomplishment that isn’t on your resume?

  

  

Describe a Time When You Had to Make a Difficult Decision. What makes it difficult?

Isme technical vs business change ka bolna h 

  

Describe a project or idea that was successfully implemented. 

  

  

Are their any of your qualifications that you want to share with us

  

  

Is there a proud moment or accomplishment in your career which you haven't shared yet ?

Customer Obsession Question: “Tell me about a time when you went above and beyond to serve a customer’s needs.”

Situation: While developing the Credit Limit Request/Writeup Angular application, I noticed that credit officers were manually validating loan data a process that was both time‑consuming and error‑prone.

Task: I wanted to streamline this process by automating multiple loan request validations through our Rule Based Approval (RBA) mechanism, ensuring that internal customers (credit officers) could review and approve requests more efficiently and accurately.

Action: I carefully reviewed the existing RBA code and identified opportunities to enhance it to support multiple loan request validations simultaneously. I refactored and optimized the code, integrating robust error handling and comprehensive testing to ensure the new functionality operated seamlessly. This enhancement allowed the system to automatically validate multiple loan requests, reducing the need for manual verification.

Result: The improvements significantly reduced processing time, increased data accuracy, and boosted user confidence in the system. Credit officers were extremely satisfied with the enhanced functionality, which streamlined their workflow  
  

Ownership Question: “Describe a situation where you took responsibility for a project or problem that wasn’t directly in your job description.”  
Situation: We were enhancing our Rule Based Approval (RBA) system by introducing the Lite Approval functionality—a critical business enhancement designed to automatically approve high-risk loan requests, thereby reducing manual workload by 30‑40%.

Task: Although my primary role was backend development, I recognized the strategic importance of this feature. I took ownership of the project by gathering detailed requirements directly from stakeholders, planning the entire enhancement, and coordinating the execution—all without needing to rely on the product owner for every detail.

Action: I initiated requirement gathering sessions with key stakeholders to understand their needs for auto‑approval in high‑risk scenarios. I then led the planning process, breaking down the project into manageable tasks and delegating responsibilities across our team. I also ensured clear communication by providing demos to users and directly addressing their queries during the rollout process.

Result: The Lite Approval functionality was successfully implemented, leading to a 30‑40% reduction in manual work for credit officers. This not only streamlined our loan request processing but also significantly increased stakeholder confidence in our team’s ability to deliver critical business enhancements independently.

  

Invent and Simplify Question: “Can you provide an example of when you simplified a complex process or introduced an innovative solution to solve a problem?”

Answer:

Situation: In designing the Lite Approval functionality for loan requests, the manual review process was cumbersome and error‑prone.

Task: My goal was to simplify the approval workflow and reduce manual intervention.

Action: I designed and implemented a parameterized approach in Java that ran predefined rules twice using external services and JSON data. This automated the setting of the “Lite Approval” flag for high‑risk loans.

Result: The streamlined process not only reduced manual workload but also increased processing accuracy, making loan approvals faster and more reliable for our internal teams.

Are Right, A Lot Question: “Tell me about a decision you made based on your judgment that turned out to be correct. How did you arrive at that decision?”

Situation: After implementing the Lite Approval functionality—a major enhancement for auto-approving high-risk loan requests—we encountered a significant challenge. Our production environment wasn’t available for release, and given the extensive changes, we needed several days of integrated regression testing to cover all scenarios.

Task: Although the environment issue wasn’t originally part of my responsibilities, I recognized that ensuring a stable testing ground was critical. I needed to make a decision that would enable thorough testing without delaying the project.

Action: I decided to deploy the changes to our development environment. I took the initiative to stabilize the dev environment by making necessary configuration changes and managing the deployment process. I also coordinated with other teams to point the dev environment to the required external services, effectively replicating a production-like setup. This allowed us to conduct extensive integrated regression testing across all critical scenarios.

Result: Thanks to this proactive decision, we were able to test and validate the majority of use cases in a stable environment. My judgment not only ensured the functionality’s reliability but also built confidence among stakeholders in our ability to overcome challenges and deliver robust solutions.

Learn and Be Curious Question: “Describe a time when you had to quickly learn something new to address a challenge or seize an opportunity.”

NGINX wala answer dena hai

Hire and Develop the Best Question: “Share an experience where you coached or mentored someone to help them achieve their potential.”

Insist on the Highest Standards Question: “Give an example of when you refused to compromise on quality, even when it was difficult or unpopular.”

Think Big Question: “Tell me about a time when you proposed a bold or innovative idea that challenged the status quo and drove significant impact.”

Caching in service 

  

Bias for Action Question: “Describe a situation where you had to make a quick decision with limited information. What was the outcome?”

FrugalityQuestion: “Tell me about a time when you achieved great results using minimal resources. How did you do it?”

Earn Trust Question: “Provide an example of how you built trust with your team or stakeholders, especially in a challenging situation.”

Dive Deep Question: “Describe a time when you had to dig into the details of a problem to uncover the root cause. What steps did you take?”

Have Backbone; Disagree and Commit Question: “Tell me about a time when you disagreed with a decision. How did you express your viewpoint, and how did you eventually commit to the final decision?”

Deliver Results Question: “Share an example of a project where you overcame significant obstacles to deliver a successful outcome.”

Strive to be Earth’s Best Employer Question: “Describe an instance where you took steps to improve your team’s work environment or promote inclusivity and growth.”

Success and Scale Bring Broad Responsibility Question: “Tell me about a time when you considered the broader impact of your work on your team, community, or organization, and how you addressed that responsibility.”

  

  

  

  

  

  

  

  

  

  

Reference for Leadership principles:

[https://www.kraftshala.com/blog/amazon-interview-questions/](https://www.kraftshala.com/blog/amazon-interview-questions/)

[https://blog.tryexponent.com/how-to-nail-amazons-behavioral-interview-questions/](https://blog.tryexponent.com/how-to-nail-amazons-behavioral-interview-questions/)

[https://www.interviewkickstart.com/interview-questions/amazon-leadership-principles-interview-questions](https://www.interviewkickstart.com/interview-questions/amazon-leadership-principles-interview-questions)

  

  


**