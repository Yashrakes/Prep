# How Backend Detects Refresh Token Abuse: Complete Detection Guide

## Your Great Question

```
Attacker steals refresh_token
        ↓
Attacker uses it to get new access_tokens
        ↓
How does backend detect this is abuse?
        ↓
How does it know it's not the legitimate user?

"Backend detects abuse (unusual refresh patterns)"
But HOW exactly?
```

**This is an excellent question because:**
1. Attacker has a VALID refresh token
2. Signature is CORRECT
3. Token hasn't EXPIRED
4. How can backend tell it's stolen?

The answer: **Behavioral analysis and context checking.**

---

## The Detection Problem

### The Challenge

```
Legitimate refresh:
  POST /api/auth/refresh
  {refresh_token: "1//0gZ..."}
  
  Backend checks:
    ✓ Token signature valid
    ✓ Token not expired
    ✓ User exists
    ✓ Token not revoked
    → GRANT new access_token

Stolen refresh (attacker using it):
  POST /api/auth/refresh
  {refresh_token: "1//0gZ..."}
  
  Backend checks:
    ✓ Token signature valid
    ✓ Token not expired
    ✓ User exists
    ✓ Token not revoked
    → SAME CHECKS PASS
    
PROBLEM: Both look identical to backend!
Token signature validates
Token hasn't expired
How to tell them apart?
```

---

## The Solution: Behavioral Context Analysis

### What Backend Tracks

```
For every user, backend maintains:

1. TOKEN METADATA
   ├─ refresh_token_id: unique ID for this token
   ├─ issued_at: when token was created
   ├─ expires_at: when token expires
   ├─ user_id: which user owns it
   ├─ ip_address: where token was issued
   ├─ user_agent: what device/browser
   └─ device_fingerprint: hardware identifier

2. USAGE HISTORY
   ├─ last_refresh_at: when token was last used
   ├─ refresh_count: how many times used
   ├─ refresh_locations: list of all IPs used from
   ├─ refresh_timestamps: timeline of all uses
   ├─ failed_attempts: how many times refresh failed
   └─ success_count: total successful refreshes

3. USER CONTEXT
   ├─ active_sessions: how many logged-in sessions
   ├─ active_devices: phone, laptop, tablet, etc
   ├─ known_locations: home, work, common travel
   ├─ typical_refresh_interval: usually 15 min, sometimes varies
   ├─ time_zone: user's timezone
   └─ normal_usage_time: typically 9-5 or 24/7
```

---

## The Detection Mechanisms

### Mechanism 1: Impossible Travel Detection

```
PRINCIPLE: User can't be in two places at once

Example:

User logs in from:
  Location A: New York (40.7128° N, 74.0060° W)
  Device: iPhone
  Timestamp: 2024-03-03 09:00 AM

15 minutes later:
  Location B: Tokyo (35.6762° N, 139.6503° E)
  Device: Samsung Android
  Timestamp: 2024-03-03 09:15 AM

ANALYSIS:
  Distance: 6,700 km
  Time elapsed: 15 minutes
  Speed required: 26,800 km/hour (impossible - faster than jet!)
  
  Normal human: 0-1000 km/hour max
  This: 26,800 km/hour
  
  VERDICT: IMPOSSIBLE - Likely fraud

Detection code:
```

```python
def detect_impossible_travel(current_request, previous_request, time_delta):
    """
    Calculate if user could have traveled between locations in time_delta
    """
    
    # Calculate distance between coordinates
    distance_km = haversine_distance(
        previous_request.latitude,
        previous_request.longitude,
        current_request.latitude,
        current_request.longitude
    )
    
    # Maximum possible speed (1000 km/hour is jet speed)
    max_possible_speed = 1000  # km/hour
    
    # Time in hours
    time_hours = time_delta.total_seconds() / 3600
    
    # Maximum distance that could be traveled
    max_distance = max_possible_speed * time_hours
    
    # If actual distance > possible distance = FRAUD
    if distance_km > max_distance:
        return {
            'is_fraud': True,
            'distance_traveled': distance_km,
            'max_possible': max_distance,
            'speed_required': distance_km / time_hours,
            'severity': 'CRITICAL'
        }
    
    return {'is_fraud': False}

# Example
last_refresh = {
    'ip': '203.0.113.45',  # NYC
    'latitude': 40.7128,
    'longitude': -74.0060,
    'timestamp': datetime(2024, 3, 3, 9, 0, 0)
}

current_refresh = {
    'ip': '198.51.100.89',  # Tokyo
    'latitude': 35.6762,
    'longitude': 139.6503,
    'timestamp': datetime(2024, 3, 3, 9, 15, 0)
}

result = detect_impossible_travel(current_refresh, last_refresh, timedelta(minutes=15))
# Returns: {'is_fraud': True, 'distance_traveled': 6744, 'max_possible': 250, ...}
# ACTION: Block refresh request, alert user
```

### Mechanism 2: Rate Limit Detection

```
PRINCIPLE: Normal users refresh at predictable intervals

Normal behavior:
  User logs in: 9:00 AM
  Uses app (token being used)
  Token expires after 15 minutes: 9:15 AM
  Refresh happens: 9:15 AM
  Next refresh: 9:30 AM
  Pattern: One refresh every 15 minutes
  Daily refreshes: 4 * 24 = 96 per day

Attacker behavior:
  Attacker gets refresh_token: 9:00 AM
  Attacker starts making requests:
  Refresh 1: 9:00:01 AM
  Refresh 2: 9:00:02 AM
  Refresh 3: 9:00:03 AM
  Refresh 4: 9:00:04 AM
  ...
  Pattern: Rapid-fire refreshes
  In 1 minute: 60 refreshes (should take 15 minutes!)
  
Detection code:
```

```python
def detect_refresh_rate_abuse(user_id, refresh_requests, time_window):
    """
    Detect if refresh rate is abnormal
    """
    
    # Get refresh history in time window
    recent_refreshes = get_refreshes(user_id, time_window)
    
    # Calculate refresh rate
    refresh_count = len(recent_refreshes)
    window_minutes = time_window.total_seconds() / 60
    
    # Normal refresh rate (one per 15 minutes typical)
    normal_rate = window_minutes / 15  # Expected refreshes
    
    # If refreshes are 10x normal rate = suspicious
    abuse_threshold = normal_rate * 10
    
    if refresh_count > abuse_threshold:
        return {
            'is_fraud': True,
            'refresh_count': refresh_count,
            'expected_count': normal_rate,
            'ratio': refresh_count / normal_rate,
            'severity': 'CRITICAL'
        }
    
    return {'is_fraud': False}

# Example: User normally refreshes every 15 minutes
# In 10 minutes, 1 refresh is normal
# In 10 minutes, 10 refreshes = 10x abuse threshold

recent_refreshes = [
    {'timestamp': datetime(2024, 3, 3, 10, 0, 1)},
    {'timestamp': datetime(2024, 3, 3, 10, 0, 2)},
    {'timestamp': datetime(2024, 3, 3, 10, 0, 3)},
    {'timestamp': datetime(2024, 3, 3, 10, 0, 4)},
    {'timestamp': datetime(2024, 3, 3, 10, 0, 5)},
    {'timestamp': datetime(2024, 3, 3, 10, 0, 6)},
    {'timestamp': datetime(2024, 3, 3, 10, 0, 7)},
    {'timestamp': datetime(2024, 3, 3, 10, 0, 8)},
]  # 8 refreshes in 8 seconds

result = detect_refresh_rate_abuse(
    user_id='user123',
    refresh_requests=recent_refreshes,
    time_window=timedelta(minutes=10)
)
# Returns: {'is_fraud': True, 'refresh_count': 8, 'expected_count': 0.67, 'ratio': 12.0}
# ACTION: Block immediately, trigger security alert
```

### Mechanism 3: Device/Browser Mismatch

```
PRINCIPLE: Same user should use same device usually

Normal behavior:
  User logs in from: iPhone, Chrome, Device ID: abc123def
  Uses app
  Refreshes token: Same iPhone, Same Device ID
  Uses app more
  Refreshes token: Same iPhone, Same Device ID
  
Attacker behavior:
  User logs in from: iPhone
  Attacker steals refresh_token
  Attacker uses it from: Linux server, bot, completely different device
  
  Request to /refresh:
    refresh_token: [valid, stolen]
    User-Agent: "Mozilla/5.0 (X11; Linux x86_64)"
    Device: Different from login
    IP: Different from login location
    
Detection code:
```

```python
def detect_device_mismatch(user_id, current_request, previous_login):
    """
    Detect if device/browser changed unexpectedly
    """
    
    current_user_agent = current_request.user_agent
    previous_user_agent = previous_login.user_agent
    
    current_device_id = current_request.device_fingerprint
    previous_device_id = previous_login.device_fingerprint
    
    # Parse user agents to extract device info
    current_device = parse_user_agent(current_user_agent)  # e.g., iPhone, Chrome
    previous_device = parse_user_agent(previous_user_agent)  # e.g., iPhone, Chrome
    
    # Check if device changed
    if current_device != previous_device:
        # New device detected - is this normal?
        user_devices = get_user_known_devices(user_id)
        
        if current_device not in user_devices:
            # User has never used this device before
            # Check if it's within normal behavior
            days_since_account_created = (datetime.now() - user_signup_date).days
            
            if days_since_account_created < 30:
                # New account = probably normal to add devices
                severity = 'LOW'
            elif current_request.ip in user_known_ips:
                # From known location = probably normal
                severity = 'MEDIUM'
            else:
                # New device, new location = suspicious
                severity = 'CRITICAL'
            
            return {
                'device_changed': True,
                'previous_device': previous_device,
                'current_device': current_device,
                'is_known_device': False,
                'severity': severity
            }
    
    return {'device_changed': False}

# Example
login_info = {
    'user_agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)',
    'device_fingerprint': 'iphone_chrome_abc123'
}

refresh_request = {
    'user_agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36',
    'device_fingerprint': 'linux_bot_xyz789'
}

result = detect_device_mismatch('user123', refresh_request, login_info)
# Returns: {'device_changed': True, 'previous_device': 'iPhone', 'current_device': 'Linux', 'severity': 'CRITICAL'}
# ACTION: Block refresh, send alert to user
```

### Mechanism 4: IP Address Anomalies

```
PRINCIPLE: Users typically log in from few consistent locations

Normal behavior:
  User's known IPs:
    ├─ Home: 203.0.113.1 (India, Mumbai)
    ├─ Office: 203.0.113.45 (India, Mumbai)
    └─ Mobile: 192.0.2.89 (India, various)
  
  All refreshes come from these 3 IPs
  All refreshes are from India
  All refreshes happen during 9-5 IST

Attacker behavior:
  Attacker location: 198.51.100.1 (USA, different timezone)
  Attacker tries refresh: 3:00 AM IST (but user never active then)
  From completely new IP: Never seen before
  From USA: User always in India

Detection code:
```

```python
def detect_suspicious_ip(user_id, current_request):
    """
    Detect if refresh request is from suspicious IP
    """
    
    current_ip = current_request.ip_address
    
    # Get user's known IPs
    known_ips = get_user_known_ips(user_id)
    
    # Get user's timezone
    user_timezone = get_user_timezone(user_id)
    
    # Check if IP is in known list
    if current_ip not in known_ips:
        # New IP - get location info
        ip_location = geoip_lookup(current_ip)
        
        # Check if location matches user's timezone
        current_time = datetime.now(timezone.utc)
        user_local_time = current_time.astimezone(user_timezone)
        
        # Is it an unusual time for the user?
        user_active_hours = get_user_active_hours(user_id)  # e.g., 9-5
        
        if user_local_time.hour not in user_active_hours:
            # Active at unusual time = suspicious
            return {
                'suspicious_ip': True,
                'is_known_ip': False,
                'location': ip_location,
                'unusual_time': True,
                'severity': 'HIGH'
            }
        
        # Check distance from last known location
        last_location = get_user_last_login_location(user_id)
        distance = haversine_distance(
            last_location['latitude'],
            last_location['longitude'],
            ip_location['latitude'],
            ip_location['longitude']
        )
        
        # If distance too far for time elapsed = impossible
        # (already checked in impossible_travel)
        
        return {
            'suspicious_ip': True,
            'is_known_ip': False,
            'distance_from_last': distance,
            'severity': 'MEDIUM'
        }
    
    return {'suspicious_ip': False}

# Example
known_ips = {
    '203.0.113.1': {'city': 'Mumbai', 'country': 'India'},
    '203.0.113.45': {'city': 'Mumbai', 'country': 'India'},
    '192.0.2.89': {'city': 'Various', 'country': 'India'}
}

refresh_request = {
    'ip': '198.51.100.1',  # Not in known IPs
    'timestamp': datetime(2024, 3, 3, 3, 0, 0)  # 3 AM IST
}

result = detect_suspicious_ip('user123', refresh_request)
# Returns: {'suspicious_ip': True, 'is_known_ip': False, 'unusual_time': True, 'severity': 'HIGH'}
# ACTION: Block refresh, challenge user
```

### Mechanism 5: Concurrent Session Detection

```
PRINCIPLE: User can't be in two places with different tokens

Normal behavior:
  User logged in from: iPhone at 9:00 AM
  Token refresh: 9:15 AM (same session)
  Token refresh: 9:30 AM (same session)
  User logs out from iPhone: 5:00 PM
  
Attacker behavior:
  User logged in from: iPhone at 9:00 AM (legitimate)
  At 9:05 AM:
    ├─ User is still using app on iPhone (using original access token)
    ├─ Attacker is using stolen refresh_token from different location
    ├─ Both can't be the same person
    └─ FRAUD detected

Detection code:
```

```python
def detect_concurrent_sessions(user_id, current_refresh_request):
    """
    Detect if user is active in multiple places simultaneously
    """
    
    # Get user's active sessions
    active_sessions = get_user_active_sessions(user_id)
    
    # Get last activity from each session
    for session in active_sessions:
        last_activity = session.get('last_activity_timestamp')
        last_activity_ip = session.get('last_activity_ip')
        
        # If session was active in last 2 minutes
        if (datetime.now() - last_activity).total_seconds() < 120:
            # Session is actively being used
            
            # Check if refresh is from different location
            if last_activity_ip != current_refresh_request.ip:
                # User active in two places at once
                distance = haversine_distance(
                    get_ip_location(last_activity_ip),
                    get_ip_location(current_refresh_request.ip)
                )
                
                if distance > 100:  # More than 100 km apart
                    return {
                        'concurrent_fraud': True,
                        'session_active_at': last_activity,
                        'active_from_ip': last_activity_ip,
                        'new_request_from_ip': current_refresh_request.ip,
                        'distance_apart': distance,
                        'severity': 'CRITICAL'
                    }
    
    return {'concurrent_fraud': False}

# Example
active_sessions = [
    {
        'session_id': 'session_abc123',
        'device': 'iPhone',
        'last_activity_timestamp': datetime.now() - timedelta(seconds=30),
        'last_activity_ip': '203.0.113.1'  # Mumbai
    }
]

refresh_request = {
    'ip': '198.51.100.1',  # USA
    'timestamp': datetime.now()
}

result = detect_concurrent_sessions('user123', refresh_request)
# Returns: {'concurrent_fraud': True, 'distance_apart': 9000, 'severity': 'CRITICAL'}
# ACTION: Block refresh, immediately lock account
```

---

## Real-World Response Flow

### When Abuse Is Detected

```
Attacker uses stolen refresh_token
        ↓
POST /api/auth/refresh
Content-Type: application/json
{
  "refresh_token": "1//0gZ..."
}
        ↓
Backend receives request
        ↓
Step 1: Validate token signature
  ✓ Signature is valid
  ✓ Token not expired
  ✓ User exists
        ↓
Step 2: Check context (ABUSE DETECTION)
  ├─ Check 1: Impossible travel?
  │   Location: Tokyo, Time: 5 minutes after NYC
  │   Result: FRAUD DETECTED
  │
  ├─ Check 2: Refresh rate normal?
  │   Rate: 100 refreshes/minute (normal: 1/15min)
  │   Result: FRAUD DETECTED
  │
  ├─ Check 3: Device same?
  │   Last: iPhone Chrome
  │   Now: Linux bot
  │   Result: FRAUD DETECTED
  │
  ├─ Check 4: IP known?
  │   Last: India
  │   Now: USA (unusual time)
  │   Result: FRAUD DETECTED
  │
  └─ Check 5: Concurrent sessions?
       Active on: iPhone in Mumbai
       Refresh from: Bot in USA
       Result: FRAUD DETECTED
        ↓
Step 3: Score the risk
  Fraud score: 95/100 (CRITICAL)
  Action: BLOCK + ALERT
        ↓
Step 4: Response to attacker
  HTTP/1.1 401 Unauthorized
  {
    "error": "unauthorized",
    "error_description": "Suspicious activity detected"
  }
        ↓
Step 5: Action backend takes
  ├─ BLOCK: Invalidate all refresh tokens for user
  ├─ ALERT: Send user email "Your account accessed from unusual location"
  ├─ LOCK: Require user to re-authenticate with 2FA
  ├─ LOG: Record full details of attack attempt
  └─ REVOKE: All active sessions terminated
        ↓
Step 6: User notification
  Email sent:
    Subject: "Suspicious login detected"
    Body: "Your account was accessed from USA at 3 AM IST.
           This doesn't match your normal activity.
           If this was you, click here to confirm.
           Otherwise, change your password immediately."
        ↓
Result: Attacker BLOCKED, user is PROTECTED
```

---

## Real-World Example: Timeline

### Attack Scenario with Detection

```
Timeline of stolen refresh_token attack:

9:00 AM - User logs in normally
  ├─ Location: Mumbai, India
  ├─ Device: iPhone
  ├─ Browser: Chrome
  ├─ IP: 203.0.113.1
  └─ Backend issues: access_token (15 min) + refresh_token (7 days)

9:05 AM - Attacker steals refresh_token
  ├─ XSS vulnerability on website
  ├─ Attacker reads refresh_token from HttpOnly cookie?
  │  No, can't read it
  ├─ Attacker intercepts network traffic?
  │  Encrypted by HTTPS
  ├─ Attacker compromises server?
  │  Possible, gets tokens from database
  └─ Refresh_token: "1//0gZ..."

9:06 AM - Attacker starts using stolen token
  ├─ Attacker's location: USA (198.51.100.1)
  ├─ Time in India: 3 AM (very unusual)
  ├─ Device: Linux server, bot
  ├─ No human behavior (machine automation)
  │
  └─ Attacker Request 1:
       POST /api/auth/refresh
       {refresh_token: "1//0gZ..."}
       
       Backend analysis:
       ├─ Check 1: Impossible travel?
       │  NYC (40.71°N, 74.00°W) to USA (39.74°N, 104.99°W)
       │  Distance: 1,600 km
       │  Time: 1 minute
       │  Speed needed: 96,000 km/hour
       │  Result: ✗ FRAUD
       │
       ├─ Check 2: Refresh rate?
       │  First refresh in 6 minutes (normal = 15 min)
       │  Could be legitimate, monitor
       │
       └─ Response: ALLOW (with monitoring)
           Issue new access_token
           (Might be legitimate - user traveling)

9:07 AM - Attacker Request 2:
     POST /api/auth/refresh
     
     Backend analysis:
     ├─ Check 1: Rate increase
     │  2 refreshes in 1 minute
     │  Expected: 1 per 15 minutes
     │  Result: ⚠ WARNING (but could be rapid API calls)
     │
     ├─ Check 2: Concurrent session?
     │  User still active on iPhone in India (just refreshed 30s ago)
     │  New refresh from USA (same 30s)
     │  User can't be in two places!
     │  Result: ✗ FRAUD
     │
     └─ Response: BLOCK
         HTTP 401
         Action: Invalidate all refresh tokens

9:08 AM - User email notification:
     Subject: "Suspicious activity on your account"
     Body: "Your account was accessed from USA at 3 AM IST.
            Two refresh requests detected from impossible locations.
            Click to verify this was you, or change password."

9:09 AM - Attacker's Request 3:
     POST /api/auth/refresh
     {refresh_token: "1//0gZ..."}
     
     Response: HTTP 401
     Error: "Refresh token has been invalidated"
     
     Attacker: ❌ BLOCKED
     All previously issued tokens from stolen refresh: ❌ REVOKED

9:10 AM - User clicks "This wasn't me"
     ├─ Account locked
     ├─ All sessions terminated
     ├─ Requires password reset + 2FA
     └─ Attacker completely blocked
```

---

## What Happens at Each Detection Stage

### Stage 1: Soft Detection (Monitor)

```
Suspicious but not certain:
  ├─ New device (but user adds devices sometimes)
  ├─ New location (but user travels)
  ├─ Slightly high refresh rate (could be power user)
  
Action:
  ├─ Log the activity
  ├─ Increase monitoring
  ├─ Watch for pattern escalation
  └─ ALLOW request (but track)
```

### Stage 2: Medium Detection (Challenge)

```
Multiple suspicious factors:
  ├─ New device AND new IP
  ├─ Time matches user's sleep time
  ├─ Refresh rate elevated but not extreme
  
Action:
  ├─ Require additional verification
  ├─ Send 2FA code to user's email
  ├─ Ask security questions
  ├─ Block attacker but verify user
  └─ If user confirms: Allow and update known devices
```

### Stage 3: Critical Detection (Block)

```
Definite fraud indicators:
  ├─ Impossible travel (physics violation)
  ├─ Concurrent sessions in different locations
  ├─ Refresh rate 10x+ normal
  ├─ Bot-like patterns detected
  ├─ Multiple fraud signals together
  
Action:
  ├─ Immediate BLOCK
  ├─ Invalidate all refresh tokens
  ├─ Revoke all access tokens
  ├─ Send urgent alert email to user
  ├─ Lock account
  ├─ Require password reset + 2FA
  └─ Full investigation and logging
```

---

## Backend Code Example: Complete Detection

```python
def handle_refresh_token_request(request):
    """
    Complete refresh token endpoint with abuse detection
    """
    
    refresh_token = request.json.get('refresh_token')
    user_ip = request.remote_addr
    user_agent = request.headers.get('User-Agent')
    
    # Step 1: Validate token signature
    try:
        payload = jwt.decode(refresh_token, SECRET_KEY)
    except:
        return error_response(401, "Invalid token")
    
    user_id = payload['user_id']
    
    # Step 2: Check if token is revoked (database lookup)
    if is_token_revoked(refresh_token):
        log_attack(user_id, "Revoked token attempt", user_ip)
        return error_response(401, "Token has been revoked")
    
    # Step 3: Abuse detection
    fraud_score = 0
    fraud_details = []
    
    # Check 1: Impossible travel
    last_location = get_last_request_location(user_id)
    current_location = geoip_lookup(user_ip)
    
    if is_impossible_travel(last_location, current_location, 5):  # 5 minutes
        fraud_score += 40
        fraud_details.append("Impossible travel detected")
        log_suspicious_activity(user_id, "Impossible travel", user_ip)
    
    # Check 2: Refresh rate
    recent_refreshes = get_recent_refreshes(user_id, minutes=10)
    if len(recent_refreshes) > 10:  # More than 10 in 10 minutes
        fraud_score += 30
        fraud_details.append("Abnormal refresh rate")
        log_suspicious_activity(user_id, "High refresh rate", user_ip)
    
    # Check 3: Device mismatch
    current_device = parse_user_agent(user_agent)
    last_device = get_user_last_device(user_id)
    
    if current_device != last_device:
        if not is_known_device(user_id, current_device):
            fraud_score += 20
            fraud_details.append("Unknown device")
            log_suspicious_activity(user_id, "Unknown device", user_ip)
    
    # Check 4: IP anomaly
    if not is_known_ip(user_id, user_ip):
        current_hour = datetime.now().hour
        if not is_user_active_hour(user_id, current_hour):
            fraud_score += 25
            fraud_details.append("Unknown IP at unusual time")
            log_suspicious_activity(user_id, "Unusual IP + time", user_ip)
    
    # Check 5: Concurrent sessions
    active_sessions = get_active_sessions(user_id)
    for session in active_sessions:
        if (datetime.now() - session['last_activity']).seconds < 60:
            # Session active in last minute
            if session['ip'] != user_ip:
                distance = haversine(session['location'], current_location)
                if distance > 100:
                    fraud_score += 50
                    fraud_details.append("Concurrent sessions detected")
                    log_suspicious_activity(user_id, "Concurrent sessions", user_ip)
    
    # Step 4: Make decision
    if fraud_score >= 60:
        # Critical fraud detected
        revoke_all_refresh_tokens(user_id)
        invalidate_all_access_tokens(user_id)
        send_alert_email(user_id, "Critical: Account accessed from unusual location")
        lock_account(user_id)
        
        log_fraud_incident(
            user_id=user_id,
            fraud_score=fraud_score,
            details=fraud_details,
            ip=user_ip
        )
        
        return error_response(401, "Suspicious activity detected - account locked")
    
    elif fraud_score >= 30:
        # Medium fraud detected - challenge user
        send_verification_email(user_id)
        return error_response(401, "Additional verification required")
    
    else:
        # Low fraud score - allow but monitor
        
        # Issue new access token
        new_access_token = generate_access_token(user_id)
        
        # Optionally: Rotate refresh token (new refresh_token with same old one revoked)
        new_refresh_token = generate_refresh_token(user_id)
        revoke_token(refresh_token)  # Invalidate old one
        
        return success_response({
            'access_token': new_access_token,
            'refresh_token': new_refresh_token,
            'expires_in': 900  # 15 minutes
        })
```

---

## Summary: How Detection Works

```
┌───────────────────────────────────────────────────────────┐
│ ATTACKER USES STOLEN REFRESH TOKEN                       │
└───────────────────────────────────────────────────────────┘
                          ↓
┌───────────────────────────────────────────────────────────┐
│ BACKEND CHECKS                                            │
├───────────────────────────────────────────────────────────┤
│ 1. Impossible travel (physics check)                      │
│ 2. Refresh rate (behavior check)                          │
│ 3. Device fingerprint (hardware check)                    │
│ 4. IP address (geolocation check)                         │
│ 5. Concurrent sessions (location check)                   │
│ 6. Time zone match (timezone check)                       │
│ 7. Bot detection (automation check)                       │
└───────────────────────────────────────────────────────────┘
                          ↓
┌───────────────────────────────────────────────────────────┐
│ FRAUD SCORING (0-100)                                     │
│                                                           │
│ Low (0-30): Monitor, log, allow                          │
│ Medium (30-60): Challenge user with 2FA                  │
│ High (60-100): Block, revoke, lock, alert               │
└───────────────────────────────────────────────────────────┘
                          ↓
┌───────────────────────────────────────────────────────────┐
│ ACTION TAKEN                                              │
├───────────────────────────────────────────────────────────┤
│ ✗ Block request                                           │
│ ✗ Revoke all refresh tokens                              │
│ ✗ Invalidate all access tokens                           │
│ ✗ Lock account                                           │
│ ✓ Send alert email                                        │
│ ✓ Require password reset                                  │
│ ✓ Full logging for investigation                         │
└───────────────────────────────────────────────────────────┘
                          ↓
ATTACKER: ❌ BLOCKED & DETECTED
USER: ✓ PROTECTED & ALERTED
```

---

## Key Insight

**Even though the attacker has a VALID, non-expired, correctly-signed refresh token:**

```
Backend doesn't just validate the token signature
Backend also validates the CONTEXT of its usage

If context doesn't match expected user behavior:
  ├─ Impossible physics (too fast travel)
  ├─ Unusual patterns (too many refreshes)
  ├─ Device mismatch (different hardware)
  ├─ Location anomalies (wrong country)
  ├─ Concurrent activity (two places at once)
  
Then: FRAUD DETECTED even with valid token
```

This is why stealing a refresh token doesn't guarantee success. The backend is always watching the **context** of the token usage, not just the token itself.
