# Why Both Access Token AND Refresh Token? Why Not Just One?

## Your Question

```
Why do we need two tokens?

Why not just send ONE token (access_token) that:
  - Works for API requests
  - Lasts forever (or very long)
  - Can be used indefinitely

Why add complexity with TWO tokens?
```

**This is an EXCELLENT question because:**
1. It seems like unnecessary complexity
2. One token would be simpler
3. Why not just make it last longer?

The answer reveals the **core security principle of token design**.

---

## The Fundamental Problem: Exposure Risk vs Usability

### The Trade-Off Dilemma

```
Requirement 1: Security
  ├─ Tokens will be stolen sometimes
  ├─ (XSS, network sniffer, malware, etc)
  ├─ Want to minimize damage window
  └─ Solution: Short-lived tokens

Requirement 2: Usability
  ├─ Users don't want to re-login constantly
  ├─ Users want seamless experience
  ├─ Tokens expiring every 5 minutes = annoying
  └─ Solution: Long-lived tokens

These CONFLICT:
  ❌ Short tokens = Secure but annoying
  ❌ Long tokens = Convenient but insecure
  
HOW TO SOLVE BOTH?
  ✓ Two tokens with different lifetimes!
     ├─ Access token: Short-lived (5-15 min)
     ├─ Refresh token: Long-lived (7 days)
     └─ Best of both worlds
```

---

## What Each Token Is For

### Access Token: For Making Requests

```
Purpose: Prove you have permission to access a resource

Characteristics:
  ├─ USED: With every API request
  ├─ EXPOSURE: High (used frequently, in memory)
  ├─ LIFETIME: Short (5-15 minutes)
  ├─ REVOCATION: Can't revoke (expires naturally)
  ├─ SCOPE: Limited permissions
  ├─ STORAGE: Memory (frontend) or session (backend)
  └─ FORMAT: JWT with user claims

Example request:
  GET /api/user
  Authorization: Bearer eyJhbGc...
  
Example payload:
  {
    "user_id": "123",
    "email": "user@gmail.com",
    "scope": "read:photos write:drive",
    "exp": 1234567890,  // Expires in 5 minutes
    "iat": 1234567590
  }

If stolen:
  Attacker has access for: 5-15 minutes
  Damage: Limited by scope and time
  Revocation: Wait for expiry
```

### Refresh Token: For Getting New Access Tokens

```
Purpose: Prove you can get a new access token without re-authenticating

Characteristics:
  ├─ USED: Only when access token expires
  ├─ EXPOSURE: Low (rarely used, stored securely)
  ├─ LIFETIME: Long (7 days, 30 days, 1 year)
  ├─ REVOCATION: Can revoke immediately
  ├─ SCOPE: NOT for API requests, only for token exchange
  ├─ STORAGE: HttpOnly secure cookie (browser) or Keychain (mobile)
  └─ FORMAT: Opaque token (random string or JWT)

Example use:
  Access token expires
        ↓
  Frontend: "My token expired, need a new one"
        ↓
  Backend makes request:
    POST /api/auth/refresh
    {
      refresh_token: "1//0gZ..."
    }
        ↓
  Backend receives:
    {
      access_token: "new_eyJhbGc...",
      expires_in: 900  // 15 minutes
    }
        ↓
  Transparent to user

Example payload:
  {
    "user_id": "123",
    "type": "refresh",
    "exp": 1234567890,  // Expires in 7 days
    "iat": 1234567590
  }

If stolen:
  Attacker can get new access tokens
  BUT: Backend detects abuse (unusual refresh patterns)
  Revocation: User can revoke immediately
```

---

## The Fundamental Design: Exposure vs Lifetime

### The Security Principle

```
┌─────────────────────────────────────────────────┐
│ TOKEN LIFETIME vs EXPOSURE RISK                 │
├─────────────────────────────────────────────────┤
│                                                 │
│ If token is used frequently (many API calls):   │
│   ├─ High exposure to theft                     │
│   ├─ More windows for interception              │
│   ├─ More copies floating around                │
│   └─ Therefore: Must be SHORT-lived             │
│                                                 │
│ If token is used rarely (only on page load):    │
│   ├─ Low exposure to theft                      │
│   ├─ Few windows for interception               │
│   ├─ Fewer copies floating around               │
│   └─ Can be LONGER-lived                        │
│                                                 │
│ DUAL TOKEN STRATEGY:                            │
│   ├─ Access token: High exposure → Short life   │
│   ├─ Refresh token: Low exposure → Long life    │
│   └─ Result: Security + Usability               │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## Why One Long-Lived Token Fails

### Scenario: Single Access Token for Everything

```
Architecture:
  User logs in
        ↓
  Server issues: access_token (lifetime: 7 days)
        ↓
  Token used for all API requests
        ↓
  PROBLEMS:
```

### Problem 1: Extended Exposure Window

```
Single token lasts 7 days

Day 1: User logs in
  ├─ Token created: abc123xyz...
  ├─ Token in memory
  └─ Token in use

Day 2: 
  ├─ Token still valid
  ├─ Attacker steals token (network sniffer)
  └─ Token in attacker's hand

Days 2-7:
  ├─ Attacker uses token
  ├─ Makes requests as user
  ├─ Accesses all data
  ├─ User doesn't notice (no visible activity)
  └─ 5 MORE DAYS of unauthorized access

Day 8: Token expires naturally
  └─ Attack finally stops

DAMAGE WINDOW: 5 days
USER UNAWARE: Entire time
```

### Problem 2: Can't Revoke Without New Login

```
User realizes token is stolen:
  "I need to stop this attacker immediately"
  
Options:
  ❌ Can't revoke token (it's not revokable)
  ❌ Can't revoke without logout
  ❌ Must change password
  
Reality:
  1. User changes password
  2. Attacker still has OLD token
  3. Attacker uses it for 5 more days
  4. Token never talks to server (stateless token)
  5. Server doesn't know token is compromised
  
Result: Can't revoke compromised token
```

### Problem 3: Can't Limit Damage

```
Single token grants:
  ✓ Read all photos
  ✓ Write all documents  
  ✓ Read all emails
  ✓ Delete all data
  ✓ Access everything

If stolen:
  Attacker can:
  ❌ Read all photos
  ❌ Delete all documents
  ❌ Read all emails
  ❌ Delete account
  ❌ Change password
  ❌ Access everything for 7 days

No way to:
  ❌ Limit what attacker can do
  ❌ Detect specific abuse
  ❌ Revoke just part of access
```

### Problem 4: Frequent Exposure Risk

```
Single token used with every request:

Request 1: GET /api/user
  Authorization: Bearer token123
Request 2: GET /api/photos
  Authorization: Bearer token123
Request 3: POST /api/drive
  Authorization: Bearer token123
Request 4: GET /api/emails
  Authorization: Bearer token123
...
Request 100 per day

100 opportunities per day for token to be:
  ├─ Intercepted by MITM
  ├─ Logged by proxy
  ├─ Read by XSS
  ├─ Captured by malware
  ├─ Stolen by extension
  └─ Exposed in error logs

Over 7 days:
  700 opportunities for theft
  
More exposure = Higher probability of theft
Longer lifetime = Longer damage window

RESULT: Very insecure
```

---

## Why One Short-Lived Token Fails

### Scenario: Single Access Token, Short Lifetime

```
Architecture:
  User logs in
        ↓
  Server issues: access_token (lifetime: 15 minutes)
        ↓
  Token used for all API requests
        ↓
  Token expires → USER MUST RE-LOGIN
        ↓
  PROBLEMS:
```

### Problem 1: Terrible User Experience

```
User is browsing app
  ├─ Token obtained at login: 9:00 AM
  ├─ Token expires at: 9:15 AM
  
Timeline:
  9:00 - User logs in
  9:05 - User reads emails (token still valid)
  9:10 - User looks at photos (token still valid)
  9:15 - Token expires ❌
  9:16 - User tries to upload document
        → Error: "You're not authenticated"
        → Redirects to login
  9:16 - User re-enters password
  9:17 - User finally uploads document

This happens EVERY 15 MINUTES

Imagine every app requiring re-login every 15 min:
  ❌ User gets logged out while composing email
  ❌ User gets logged out while editing document
  ❌ User gets logged out while watching video
  ❌ User gets logged out constantly
  ❌ User experience is TERRIBLE

Result: Users hate the app
```

### Problem 2: Can't Handle Offline or Cached Data

```
User has token that expires in 5 minutes
        ↓
Network becomes slow/unstable
        ↓
User loads cached data (works)
        ↓
5 minutes pass, user tries to make request
        ↓
No network connection
        ↓
Can't refresh token (network is down)
        ↓
Token expires
        ↓
User locked out
        ↓
Can't do anything

With refresh token:
  ├─ Can cache refresh_token
  ├─ When network comes back
  ├─ Refresh token still valid (7 days)
  ├─ Can get new access token
  ├─ Back online
```

### Problem 3: Too Many Re-authentications

```
User experience:

9:00 - Login (user enters password)
9:15 - Token expired, login again
9:30 - Token expired, login again
9:45 - Token expired, login again
10:00 - Token expired, login again
10:15 - Token expired, login again
10:30 - Token expired, login again
...

In 1 hour: 4 re-logins required
In 1 day: 96 re-logins required

Users won't tolerate this

They'll:
  ❌ Switch to competitor
  ❌ Use password manager "tricks"
  ❌ Save password on device
  ❌ Click "remember me" (insecure)
  ❌ Leave browser logged in (insecure)
```

---

## The Dual Token Solution

### Access Token + Refresh Token

```
GOAL 1: Security (short exposure window)
  ├─ Access token: 5-15 minutes
  ├─ If stolen, damage window is SHORT
  ├─ Frequent rotation reduces risk
  └─ Attacker can't use it for long

GOAL 2: Usability (seamless experience)
  ├─ Refresh token: 7 days
  ├─ User doesn't re-login
  ├─ Token refresh is automatic
  ├─ User never sees it
  └─ Seamless experience

GOAL 3: Revocation (user control)
  ├─ Refresh token can be revoked
  ├─ Refresh token short-lived (7 days max)
  ├─ If stolen, limited damage
  ├─ User can log out everywhere
  └─ Full user control

RESULT: ✓ SECURE + USABLE
```

---

## How Dual Token Works in Practice

### The Flow

```
┌──────────────────────────────────────────┐
│ USER LOGIN (t=0)                         │
└──────────────────────────────────────────┘
  
User enters credentials
        ↓
Server validates
        ↓
Server issues:
  ├─ access_token: expires in 15 minutes (t=15min)
  ├─ refresh_token: expires in 7 days (t=7d)
  └─ Frontend stores:
     ├─ access_token in memory
     ├─ refresh_token in HttpOnly cookie


┌──────────────────────────────────────────┐
│ NORMAL OPERATION (t=0 to t=15min)       │
└──────────────────────────────────────────┘

User makes requests:
  GET /api/user (access_token valid)
  GET /api/photos (access_token valid)
  POST /api/drive (access_token valid)
  
All requests use access_token
Access_token is SHORT-LIVED (low risk if stolen)


┌──────────────────────────────────────────┐
│ TOKEN EXPIRATION (t=15min)              │
└──────────────────────────────────────────┘

Access token expires
User tries to make request
Backend returns: 401 Unauthorized

Frontend detects 401
        ↓
Frontend uses refresh_token:
  POST /api/auth/refresh
  {
    refresh_token: "1//0gZ..." (from cookie)
  }
        ↓
Backend verifies refresh_token
Backend issues NEW access_token
        ↓
Frontend:
  ├─ Stores new access_token in memory
  ├─ Retry original request
  ├─ User doesn't notice
  └─ Seamless


┌──────────────────────────────────────────┐
│ CONTINUOUS OPERATION (t=15min to t=7d)   │
└──────────────────────────────────────────┘

Every 15 minutes:
  ├─ Access token expires
  ├─ Refresh token automatically gets new access token
  ├─ User continues using app
  ├─ No re-login needed
  └─ No interruption

Over 7 days:
  ├─ Access tokens are rotated 672 times
  ├─ Each token is short-lived
  ├─ Each token is different
  ├─ If one stolen, only 15-min window
  └─ User never has to re-login


┌──────────────────────────────────────────┐
│ REFRESH TOKEN EXPIRES (t=7d)            │
└──────────────────────────────────────────┘

Refresh token expires naturally
User makes request
Access token might be expired
        ↓
Frontend tries to refresh
Backend returns: 401 Unauthorized (refresh token expired)
        ↓
Frontend redirects to login
User must re-authenticate

Result:
  ✓ User had seamless experience for 7 days
  ✓ Token constantly rotated (secure)
  ✓ No access token lasted more than 15 min
  ✓ If any token stolen, limited damage
  ✓ After 7 days, must re-login (reasonable)
```

---

## Attack Scenarios: One Token vs Two Tokens

### Attack Scenario 1: Token Theft

```
SCENARIO: Attacker steals token via network sniffer

WITH SINGLE LONG-LIVED TOKEN (7 days):
  Attacker steals token
        ↓
  Attacker uses token
        ↓
  For 7 DAYS:
    ├─ Attacker has full access
    ├─ User doesn't notice
    ├─ User data is compromised
    ├─ Can't revoke without password change
    └─ Damage is COMPLETE
  
  Timeline: Day 1 theft → Day 7 discovery → 7 days lost
  
  Damage: MASSIVE
  Risk: User's data compromised for a week

WITH DUAL TOKENS (access 15 min, refresh 7 days):
  Attacker steals access token
        ↓
  Attacker can use it
        ↓
  For 15 MINUTES:
    ├─ Attacker has access
    ├─ Can make API calls
    ├─ Can read some data
    └─ Token automatically rotates
  
  After 15 minutes:
    ├─ Token is useless
    ├─ Attacker would need refresh_token
    ├─ Refresh_token is in HttpOnly cookie
    ├─ Can't be stolen by same vector
    └─ Attack stops automatically
  
  Even if attacker gets refresh_token:
    ├─ Can only use 100 times in 7 days
    ├─ Backend detects patterns
    ├─ User can revoke immediately
    ├─ Damage is LIMITED
  
  Timeline: Minute 1 theft → Minute 15 useless
  
  Damage: MINIMAL
  Risk: Limited to 15 minutes
```

### Attack Scenario 2: XSS Vulnerability

```
SCENARIO: Attacker injects JavaScript via XSS

WITH SINGLE LONG-LIVED TOKEN (7 days):
  XSS steals token from memory
        ↓
  XSS sends token to attacker
        ↓
  Attacker has token for 7 days
        ↓
  Attacker can:
    ❌ Make API requests as user
    ❌ Read all data
    ❌ Delete data
    ❌ For 7 DAYS
    ❌ User can't revoke without password change
    ❌ XSS attacker still has token

WITH DUAL TOKENS (access 15 min, refresh 7 days):
  XSS steals access_token from memory
        ↓
  XSS tries to use it
        ↓
  Attacker can make requests for 15 minutes
        ↓
  After 15 minutes:
    ├─ Token expired
    ├─ XSS would need refresh_token
    ├─ Refresh_token is in HttpOnly cookie
    ├─ XSS CANNOT READ HttpOnly cookies
    ├─ XSS CANNOT access refresh_token
    └─ Attack stops
  
  Even if same XSS tries to get refresh_token:
    ├─ document.cookie (doesn't show HttpOnly)
    ├─ Can't read the cookie
    ├─ Can't use it
  
  If XSS tries to make refresh request:
    ├─ Browser adds cookie automatically
    ├─ But backend detects:
    │  ├─ Unusual refresh pattern
    │  ├─ Rapid fire requests
    │  ├─ Possible abuse
    │  └─ Rate limit kicks in
    └─ Attack is mitigated
  
  Damage: Limited to 15 minutes + mitigation
```

### Attack Scenario 3: Malware Reading Process Memory

```
SCENARIO: Malware infects device, reads browser memory

WITH SINGLE LONG-LIVED TOKEN (7 days):
  Malware reads browser process memory
        ↓
  Finds token: "token_abc123xyz..."
        ↓
  Malware can:
    ❌ Use token on attacker's machine
    ❌ Full access to user's account
    ❌ For up to 7 days
    ❌ Can't be revoked without password change
    ❌ Complete compromise
  
  Damage: TOTAL for 7 days

WITH DUAL TOKENS (access 15 min, refresh 7 days):
  Malware reads browser process memory
        ↓
  Finds access_token: "access_abc123xyz..."
        ↓
  Malware can:
    ✓ Use token on attacker's machine
    ✓ For 15 minutes
    ✓ Then it's useless
  
  Malware tries to find refresh_token:
    ❌ Not in memory (it's in HttpOnly cookie)
    ❌ Cookie is not stored in process memory
    ❌ Cookie is in browser's secure storage
    ❌ Malware can't read it
  
  Malware compromises entire device:
    ✓ But can only use stolen token for 15 min
    ✓ After that, needs refresh_token
    ✓ Can't get it from memory (not there)
    ✓ If tries to use browser, browser auto-includes cookie
    ✓ But backend detects: "Same user, different machine"
    ✓ Blocks request
  
  Damage: Limited even in worst case
```

---

## Why Access Token Is Short-Lived

### The Logic

```
Access Token Design Decisions:

Question 1: How long should access_token last?

Factors:
  ❌ Too long (1 day):
     └─ If stolen, damage window is huge
  
  ✓ Reasonable (15 minutes):
     └─ If stolen, damage window is small
     └─ Tokens rotate frequently
     └─ Each token is different
  
  ❌ Too short (30 seconds):
     └─ Too much refresh traffic
     └─ Server overload
     └─ Network issues cause problems

Answer: 5-15 minutes is optimal
  ├─ Security sweet spot
  ├─ Manageable refresh frequency
  ├─ Minimal user impact
  └─ Industry standard

Question 2: Why keep refresh_token longer (7 days)?

Answer: Because refresh_token is NOT used frequently
  ├─ Only used once every 15 minutes
  ├─ Stored in HttpOnly cookie (more secure)
  ├─ Can't be stolen by XSS (different storage)
  ├─ Can be revoked immediately (database-backed)
  ├─ Longer lifetime = better user experience
  └─ Shorter expiry = user must re-login often
```

---

## Why Not Three Tokens? Or More?

### Diminishing Returns

```
Could we have more tokens?

Option A: Just access_token (SIMPLE but INSECURE)
  ├─ 1 token = simple
  ├─ But: Must be long-lived (for usability)
  ├─ Problem: Can't revoke
  ├─ Problem: Exposure window too big
  └─ Result: INSECURE

Option B: Access + Refresh (CURRENT - OPTIMAL)
  ├─ 2 tokens = moderate complexity
  ├─ Access: Short-lived (secure)
  ├─ Refresh: Long-lived (usable)
  ├─ Can revoke refresh_token
  ├─ Limited exposure window
  └─ Result: SECURE + USABLE ✓

Option C: Access + Refresh + Rotate (BETTER SECURITY)
  ├─ 3 tokens = more complex
  ├─ Plus: Every refresh gives NEW refresh_token
  ├─ Plus: Old refresh_token becomes invalid
  ├─ Plus: Detect stolen tokens (can't use old ones)
  ├─ Result: EXCELLENT SECURITY
  ├─ Cost: More complexity, more storage
  └─ Modern systems use this

Option D: Access + Refresh + Short + Medium + Long (OVERKILL)
  ├─ 5 tokens = very complex
  ├─ Benefit: Marginal security improvement
  ├─ Cost: Significant complexity
  ├─ Result: Not worth it
  └─ No system does this

CONCLUSION:
  2 tokens = Good balance ✓
  3 tokens (with rotation) = Better balance ✓
  5+ tokens = Diminishing returns ✗
```

---

## Real-World Examples

### Email Service (Gmail)

```
When you login to Gmail:

Google issues:
  ├─ Access token: 1 hour
  ├─ Refresh token: 6 months
  └─ Can revoke immediately in account settings

If access token stolen:
  ├─ Attacker has 1 hour access
  ├─ Then token is useless
  └─ Damage: Limited

If you revoke:
  ├─ All sessions end immediately
  ├─ New requests fail
  ├─ Refresh token is invalidated
  └─ User control: Complete

User experience:
  ├─ Logs in once every 6 months
  ├─ Seamless experience
  ├─ No constant re-login
  ├─ Full control over access
  └─ Secure and usable
```

### Social Media (Facebook)

```
When you login to Facebook:

Facebook issues:
  ├─ Access token: 2 hours
  ├─ Refresh token: Much longer
  └─ Can revoke in app settings

If access token stolen:
  ├─ Attacker has 2 hours
  ├─ Limited damage
  └─ Token automatically rotates

If you revoke:
  ├─ Immediate session end
  ├─ All devices logged out
  ├─ Token invalidated
  └─ Full control

User experience:
  ├─ Logs in and uses app seamlessly
  ├─ Token refreshes happen in background
  ├─ User never sees token expiration
  ├─ Days of usage on one login
  └─ Can revoke at any time
```

### Cloud Storage (Dropbox)

```
When you install Dropbox:

Dropbox issues:
  ├─ Access token: 4 hours
  ├─ Refresh token: Very long
  └─ Can revoke on web or app

If access token stolen:
  ├─ Attacker has 4 hours max
  ├─ Then must get new token (refresh)
  ├─ But refresh needs refresh_token
  ├─ Which is stored separately
  └─ Limited damage window

If you revoke (on web):
  ├─ Immediately invalidates refresh_token
  ├─ App can't refresh access_token
  ├─ App gets logged out
  ├─ Files stop syncing
  └─ Full control

User experience:
  ├─ Install once
  ├─ Works seamlessly for weeks
  ├─ Can revoke anytime
  ├─ Never re-login manually
  └─ Perfect balance
```

---

## Summary: Why Two Tokens?

```
QUESTION: Why not just one token?

ANSWER: Different purposes need different properties

┌────────────────────────────────────────────┐
│ ACCESS TOKEN                               │
├────────────────────────────────────────────┤
│ Purpose: Make API requests                 │
│ Exposure: High (used every request)        │
│ Lifetime: Short (5-15 minutes)             │
│ Storage: Memory or session                 │
│ Revocation: Wait for expiry                │
│ If stolen: 15-minute damage window         │
│ Benefit: Minimizes exposure risk           │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│ REFRESH TOKEN                              │
├────────────────────────────────────────────┤
│ Purpose: Get new access tokens             │
│ Exposure: Low (used rarely)                │
│ Lifetime: Long (7 days)                    │
│ Storage: HttpOnly cookie                   │
│ Revocation: Immediate (database-backed)    │
│ If stolen: Limited by detection            │
│ Benefit: Seamless user experience          │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│ THE DUAL-TOKEN ADVANTAGE                   │
├────────────────────────────────────────────┤
│ ✓ Security: Access token is short-lived    │
│ ✓ Usability: Seamless experience           │
│ ✓ Revocation: Can revoke immediately       │
│ ✓ Flexibility: Different lifetimes         │
│ ✓ Detection: Can detect abuse patterns     │
│ ✓ Control: User has full control           │
│ ✓ Standard: Industry best practice         │
└────────────────────────────────────────────┘
```

---

## The Design Principle

### Different Risk Profiles = Different Tokens

```
PRINCIPLE: Match token properties to usage pattern

Access Token:
  └─ Used: Every API request (high frequency)
  └─ Therefore: Must be short-lived (minimize damage)

Refresh Token:
  └─ Used: Only on expiration (low frequency)
  └─ Therefore: Can be longer-lived (more convenient)

This is why OAuth 2.0 defines BOTH
This is why all modern auth systems use BOTH
This is why you can't use just one

One token = Impossible to balance security + usability
Two tokens = Perfect balance

✓ Secure (short access token)
✓ Usable (long refresh token)
✓ Revocable (refresh token backed by database)
✓ Rotating (access token changes frequently)
```

---

## Conclusion

**Why two tokens instead of one?**

```
SECURITY + USABILITY CONFLICT:
  ❌ Short token = Secure but annoying (re-login often)
  ❌ Long token = Convenient but insecure (damage window huge)

SOLUTION: TWO TOKENS WITH DIFFERENT LIFETIMES
  ✓ Access token: Short-lived (5-15 min)
     ├─ High exposure (used every request)
     ├─ Therefore: Must be short
     ├─ If stolen: Limited damage (15 min max)
     └─ Automatic rotation every 15 minutes

  ✓ Refresh token: Long-lived (7 days)
     ├─ Low exposure (used rarely)
     ├─ Therefore: Can be long
     ├─ If stolen: Limited by detection
     └─ User can revoke anytime

RESULT:
  ✓ Security: Minimal exposure window
  ✓ Usability: No constant re-login
  ✓ Revocation: Immediate user control
  ✓ Detection: Abuse patterns visible
  ✓ Standard: Industry best practice

This is why OAuth 2.0 defines dual tokens
This is why you can't skip the refresh token
This is optimal system design
```
