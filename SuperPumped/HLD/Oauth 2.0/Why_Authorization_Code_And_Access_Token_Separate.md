# Why Authorization Code and Access Token Are Separate

## Quick Answer

**Authorization code and access token serve DIFFERENT PURPOSES and CANNOT be sent together because:**

1. **Authorization code** = Proof you authenticated (issued by auth server after you log in)
2. **Access token** = Permission to access resources (issued by server after code is validated)

If they were sent together, anyone could use that combined token repeatedly without authentication.

---

## The Core Problem: Separation of Concerns

### What Happens if You Try to Combine Them

**Bad Approach (What NOT to do):**

```
User clicks "Sign in with Google"
        ↓
Google authenticates user
        ↓
Google creates both:
  - Code: abc123
  - Access Token: eyJhbGc...
        ↓
Google redirects with BOTH:
https://myapp.com/callback?code=abc123&access_token=eyJhbGc...&expires_in=3600
```

**Why This Is Insecure:**

```
1. Authorization code is VISIBLE in the URL
   https://myapp.com/callback?code=abc123...
   
2. If you also included access_token in the URL:
   https://myapp.com/callback?code=abc123&access_token=eyJhbGc...
   
3. ANYONE who sees this URL can:
   - Copy the access token
   - Use it to access your data indefinitely
   - No need to exchange the code
   - Browser history stores it
   - Proxy logs store it
   - ISP could intercept it

4. Token is permanent (until expiration)
   Unlike code (single-use, 10 minutes)
```

---

## The Actual Flow: Why It's Designed This Way

### Current Secure Flow

```
STEP 1: Authorization Request (User's browser, visible in URL)
┌─────────────────────────────────────────────────────┐
User clicks "Sign in with Google"
        ↓
Browser redirects to:
https://accounts.google.com/oauth/authorize?
  client_id=123456
  &redirect_uri=https://myapp.com/callback
  &response_type=code
  &scope=email%20profile

User logs in and grants permission
        ↓
Google verifies user and checks scopes
└─────────────────────────────────────────────────────┘

STEP 2: Authorization Code Sent (Still visible in URL, but temporary)
┌─────────────────────────────────────────────────────┐
Google redirects back to:
https://myapp.com/callback?code=AUTH_CODE_ABC123

⚠️ Code is visible in URL BUT:
  ✓ Single-use only (10 minutes)
  ✓ Can only be exchanged with client_secret
  ✓ No sensitive data in code itself
  ✓ Limited damage window

Browser/frontend receives code
└─────────────────────────────────────────────────────┘

STEP 3: Code Exchange (Server-to-server, HTTPS, SECURE)
┌─────────────────────────────────────────────────────┐
Frontend sends code to BFF backend:
POST https://myapp.com/api/auth/callback
{
  code: "AUTH_CODE_ABC123"
}

BFF Backend exchanges code for tokens:
POST https://accounts.google.com/token (server-to-server)
{
  code: AUTH_CODE_ABC123
  client_id: 123456
  client_secret: SECRET_KEY ← Never exposed to browser
  grant_type: authorization_code
}

✓ client_secret never seen by browser
✓ HTTPS encryption
✓ Secure server-to-server communication
✓ Code can only be used ONCE

Google validates and returns:
{
  access_token: "eyJhbGc...",
  refresh_token: "1//0gZ...",
  expires_in: 3600
}

BFF stores:
  - access_token in memory
  - refresh_token in HttpOnly cookie
  - returns short-lived copy to frontend
└─────────────────────────────────────────────────────┘

STEP 4: Tokens Used for Requests
┌─────────────────────────────────────────────────────┐
Frontend uses access_token for API calls:
GET /api/user
Authorization: Bearer eyJhbGc...

BFF validates token and proxies to resource server
└─────────────────────────────────────────────────────┘
```

---

## Why Not Combine Them? Security Analysis

### Attack Scenario 1: Token Exposed in URL

**If we sent access_token in URL redirect:**

```
Attacker's goals:
  1. Intercept the OAuth redirect URL
  2. Extract the access token
  3. Use it to access user's data

How attacker could intercept:
  ├─ Network sniffer (if not HTTPS)
  ├─ Browser history (stored by browser)
  ├─ Browser plugins/extensions
  ├─ Proxy logs (corporate networks)
  ├─ ISP logs
  ├─ Server logs
  └─ Referrer headers (if clicking external link)

Example:
Attacker proxies your network traffic and sees:
https://myapp.com/callback?code=ABC&access_token=eyJhbGc...&expires_in=3600

Attacker extracts: eyJhbGc...

Attacker can now:
POST /api/user
Authorization: Bearer eyJhbGc...
// Access user's data as if they were the user

Duration of attack: Until token expires (1 hour)
// If refresh_token was also in URL, they could get new tokens forever
```

---

### Attack Scenario 2: Browser History

```
If access token sent in URL:

User visits:
https://myapp.com/callback?code=ABC&access_token=eyJhbGc...

Browser stores in history:
  ├─ History file (searchable)
  ├─ Autocomplete suggestions
  ├─ Back button
  └─ Session recovery

Attacker with device access:
  1. Presses Ctrl+H (History)
  2. Searches "callback" or "myapp"
  3. Sees full URL with access token
  4. Copies token
  5. Uses it to access data

Attacker didn't need to intercept anything!
Just physical access to the computer.

With refresh_token in URL:
  Attacker gets indefinite access
```

---

### Attack Scenario 3: Browser Extensions

```
Malicious browser extension monitors all traffic:

If access_token in URL:
  1. Extension sees redirect URL
  2. Extracts access_token
  3. Sends to attacker's server
  4. Attacker has user's access token

Real-world examples:
  - Fake ad-blocker that is actually malware
  - Cryptocurrency miner
  - Search hijacker
  - Price comparison tool
  - Theme extension

Users click "Add to Chrome" without reading permissions
Extension has access to all network traffic
```

---

### Attack Scenario 4: Referrer Header

```
If access token in URL and user clicks external link:

Page with token:
https://myapp.com/callback?access_token=eyJhbGc...

User clicks link to external site:
<a href="https://attacker.com">Check this out</a>

Browser sends:
GET /page
Referer: https://myapp.com/callback?access_token=eyJhbGc...

Attacker's server logs:
Referer header contains the access token!
```

---

## Why Authorization Code Works Better

### Authorization Code Properties

```
Authorization Code = abc123def456...

Properties:
  ✓ Single-use only (CRITICAL)
  ✓ Short-lived (10 minutes)
  ✓ Can only be exchanged with client_secret
  ✓ Contains NO user data
  ✓ Contains NO permissions data
  ✓ Is just a reference/pointer

Even if intercepted:
  Attacker gets: abc123def456...
  
Attacker tries to use it:
  POST /token
    code: abc123def456
    client_id: 123456
    client_secret: ??? (attacker doesn't have this)
    
Server rejects: Invalid request (missing/wrong client_secret)

If attacker tries again with same code:
  Server rejects: Code already used

Damage: ZERO
Time window: 10 minutes before code expires
```

---

## Comparison: Authorization Code vs Access Token

| Property | Authorization Code | Access Token |
|----------|---|---|
| **Reusability** | Single-use ✓ | Reusable (until expiry) |
| **Lifetime** | 10 minutes | 1 hour (or longer) |
| **Contains Data** | No, just a reference | Yes, claims about user |
| **Requires Secret** | Yes (client_secret) | No, just a token |
| **Safe in URL** | ✓ Yes (single-use) | ❌ No (reusable) |
| **If Stolen** | Limited damage | Full damage |
| **Use Case** | Prove authentication | Access resources |

---

## What If You Sent Access Token Directly?

### Insecure Flow (What NOT to do)

```
User authenticates with Google
        ↓
Google redirects:
https://myapp.com/callback?access_token=eyJhbGc...&expires_in=3600

Problems:
1. Token visible in browser history ❌
2. Token visible in server logs ❌
3. Token in browser URL = vulnerable to XSS ❌
4. Token in referrer header ❌
5. Token in browser extensions ❌
6. Token in network proxies ❌
7. No way to verify it wasn't intercepted ❌

Attacker scenario:
  1. Sees token in browser history
  2. Uses token to access API
  3. Accesses user's data
  4. Makes unauthorized purchases
  5. Reads private emails
  6. Modifies profile
  
Duration: 1 hour (token lifetime)
All because token was sent in URL
```

---

## The Real Reason: Trust Boundaries

### Implicit Flow (Deprecated - Access Token in URL)

```
┌──────────────────────────────────────────┐
│ Browser (UNTRUSTED)                      │
│                                          │
│ ❌ RECEIVES ACCESS TOKEN                 │
│ ❌ STORES IN localStorage                │
│ ❌ USED FOR API CALLS                    │
│ ❌ VULNERABLE TO XSS                     │
│                                          │
└──────────────────────────────────────────┘
        
        ↓ (visible in URL)

https://myapp.com/callback?access_token=eyJhbGc...

Attack surface:
  - Browser history
  - Network interception
  - Browser extensions
  - Proxy logs
  - Referrer headers
```

### Authorization Code Flow (Secure - Current Standard)

```
┌──────────────────────────────────────────┐
│ Browser (UNTRUSTED)                      │
│                                          │
│ ✓ RECEIVES CODE (single-use, 10 min)    │
│ ✓ CODE SENT TO BACKEND                  │
│ ✓ CODE CANNOT BE USED BY ATTACKER       │
│   (needs client_secret)                  │
│                                          │
└──────────────────────────────────────────┘
        
        ↓ (visible in URL, but safe)

https://myapp.com/callback?code=abc123

        ↓

┌──────────────────────────────────────────┐
│ Backend (TRUSTED)                        │
│                                          │
│ ✓ STORES client_secret (secure)         │
│ ✓ EXCHANGES CODE FOR TOKEN               │
│ ✓ VALIDATES TOKEN                       │
│ ✓ STORES REFRESH TOKEN (secure)         │
│ ✓ PROXIES API CALLS                     │
│                                          │
└──────────────────────────────────────────┘

Attack surface: MINIMAL
Trust boundary respected
```

---

## Why Not Skip Authorization Code?

### Hypothetical: Direct Token Request

**What if we did:**

```javascript
// Frontend requests token directly from Google
POST https://accounts.google.com/token
{
  username: user@gmail.com
  password: PASSWORD (user enters)
  client_id: 123456
  client_secret: SECRET
}
```

**Problems:**

```
1. Frontend must have client_secret
   → Exposed in source code
   → Everyone can see it
   → Attacker can use it
   
2. Frontend must have user's password
   → Direct password in frontend
   → Stored in JavaScript
   → Phishing-friendly
   → Violates OAuth principle
   
3. No user consent screen
   → User doesn't know what scopes are requested
   → No granular permission control
   
4. Can't revoke without password
   → User can't easily disconnect app
   
5. Password stored in memory
   → XSS can steal password
```

**This is why OAuth exists:**
- Separate authentication from authorization
- Users don't give password to third-party apps
- Apps don't need to handle passwords
- Granular permissions via scopes
- Easy revocation
```

---

## The Security Chain

### Step-by-Step Trust Building

```
STEP 1: USER AUTHENTICATES
┌──────────────┐
│ Google Login │  ← User enters password
│              │  ← Only Google knows password
└──────────────┘
       ↓
      (Only user can authenticate)

STEP 2: AUTHORIZATION CODE ISSUED
┌──────────────────────────┐
│ Authorization Code: ABC  │
│ - Single-use             │
│ - 10 minutes             │
│ - No user data inside    │
│ - Just a reference       │
└──────────────────────────┘
       ↓
      (Safe to send to browser)

STEP 3: CODE SENT TO BACKEND
┌────────────────────────────────┐
│ Frontend → Backend: code=ABC    │
│ (visible in URL, but safe)      │
└────────────────────────────────┘
       ↓
      (Backend receives code)

STEP 4: BACKEND EXCHANGES CODE
┌──────────────────────────────────────┐
│ Backend → Google:                    │
│ {                                    │
│   code: ABC,                         │
│   client_secret: HIDDEN_IN_BACKEND   │
│ }                                    │
│ (Only backend knows client_secret)   │
└──────────────────────────────────────┘
       ↓
      (Google verifies client_secret)

STEP 5: ACCESS TOKEN ISSUED
┌──────────────────────────────┐
│ Access Token: eyJhbGc...     │
│ - Valid for 1 hour           │
│ - Contains user info         │
│ - Signed by Google           │
│ - Backend keeps it secure    │
└──────────────────────────────┘
       ↓
      (Only backend stores it)

RESULT: ✅ SECURE END-TO-END
        ✅ User authenticated
        ✅ App authorized
        ✅ Token protected
```

---

## Real Attack: Without Separation

### Imagine If They Weren't Separate

```
User logs in → Google issues access_token → Sends in URL → Browser

https://myapp.com/callback?access_token=eyJhbGc...

ATTACKER SCENARIO:
1. Network sniffer captures URL
2. Extracts access_token
3. Makes requests as user

GET /api/profile
Authorization: Bearer eyJhbGc...

Response: 
{
  "id": "user123",
  "email": "victim@gmail.com",
  "name": "Victim User",
  "phone": "+1234567890",
  "address": "123 Main St"
}

4. More API calls as user

GET /api/billing
Response: Credit card details, billing history

POST /api/settings
Body: {email: attacker@evil.com}
Updates account email to attacker's email

5. Account takeover complete

Duration: 1 hour (token lifetime)
Damage: TOTAL compromise
```

---

## With Current Architecture (Secure)

### Authorization Code + Backend Validation

```
Network sniffer captures:
https://myapp.com/callback?code=abc123...

Attacker extracts: abc123

Attacker tries to use code:
POST /api/auth/callback
{code: abc123}

Backend exchanges code:
POST https://accounts.google.com/token
{
  code: abc123,
  client_secret: SECRET (attacker doesn't have)
}

Google rejects: Invalid client_secret ✓

Attacker tries with different codes:
POST /api/auth/callback
{code: xyz789}

Google verifies:
Code xyz789 doesn't exist
Code already used (if it was)
Code expired (if > 10 min ago)

All attempts fail ✓

Even if attacker somehow got a valid code:
Code is single-use, can only be used once
Code expires in 10 minutes

Damage: ZERO
Attack window: 10 minutes, and still fails
```

---

## Summary: Why They're Separate

| Aspect | Authorization Code | Access Token |
|--------|---|---|
| **Purpose** | Prove user authenticated | Prove app authorized |
| **Lifetime** | 10 minutes | 1 hour+ |
| **Reusability** | Single-use only | Reusable |
| **Requires Secret** | Yes, client_secret | No |
| **Safe in URL** | ✓ Yes | ❌ No |
| **If Stolen** | Attacker can't use it (no secret) | Attacker gets full access |
| **Exchange Location** | Backend (secure) | N/A |
| **Contains User Data** | No | Yes |

---

## Why Not Send Code and Token Together?

```
Hypothetical: 
https://myapp.com/callback?code=abc123&access_token=eyJhbGc...

Attacker sees:
  code: abc123 (can't use, single-use)
  access_token: eyJhbGc... (CAN USE, reusable)

Attacker ignores the code and uses token:
GET /api/profile
Authorization: Bearer eyJhbGc...
// Instant access to user data

Damage: MAXIMUM
Why? Because we gave attacker the access token directly
The code was irrelevant
```

---

## Conclusion

**Authorization code and access token MUST BE SEPARATE because:**

1. **Authorization code** = Proof of authentication
   - Single-use
   - Can't be used without client_secret
   - Safe to show in URL
   - Limited damage if intercepted

2. **Access token** = Sensitive credential
   - Reusable (until expiry)
   - Grants access to resources
   - MUST be kept secret
   - Should not be in URLs

3. **Trust boundaries matter**
   - Browser can't be trusted with access token
   - Only backend can be trusted with client_secret
   - Separation prevents complete compromise

4. **Single-use property is key**
   - Even if attacker gets code from URL
   - Can't use without client_secret
   - Code expires in 10 minutes
   - Code can only be used once

**This is why OAuth 2.0 is the industry standard and why modern secure applications use this architecture.**
