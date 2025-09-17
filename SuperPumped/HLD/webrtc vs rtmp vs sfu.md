
## 🔶 1. **WebRTC (Web Real-Time Communication)**

### ✅ What is WebRTC?

**WebRTC** is an open-source framework that enables **real-time, peer-to-peer** communication (audio, video, and data) **directly between browsers or mobile apps** — without plugins.

> 📌 It powers **Zoom, Google Meet, Discord**, etc.

---

### ✅ WebRTC Protocol Stack:

pgsql

CopyEdit

`+------------------------------+ |        Application           | +------------------------------+ |      WebRTC API Layer        | +------------------------------+ |   Signaling (custom layer)   | ← not part of WebRTC (you build this) +------------------------------+ | ICE / STUN / TURN            | ← NAT traversal | SRTP (Secure Real-time TP)   | ← encrypted audio/video | DTLS (Datagram TLS)          | ← key exchange | RTP (Real-time Transport)    | ← media packets +------------------------------+ |          UDP/TCP             | +------------------------------+`

---

### ✅ Key Concepts in WebRTC:

|Concept|Role|
|---|---|
|**SDP** (Session Description Protocol)|Used to negotiate media capabilities between peers|
|**ICE** (Interactive Connectivity Establishment)|Finds best path (host, reflexive, relay)|
|**STUN**|Gets your public IP (used in ICE)|
|**TURN**|Relays traffic if direct P2P fails|
|**DTLS**|Encrypts data (part of SRTP)|
|**SRTP**|Encrypts real-time audio/video|

---

### ✅ Signaling in WebRTC

> WebRTC **doesn't define signaling**. You implement it using **WebSocket/gRPC/HTTP**.

It is used to exchange:

- SDP offers/answers
    
- ICE candidates
    
- Call setup/teardown info
    

---

## 🔶 2. **SFU (Selective Forwarding Unit)**

### ✅ What is SFU?

**SFU** is a media server that **receives media streams from multiple clients** and **forwards them to others** **without decoding**.

> 📌 Ideal for group calls — used in Zoom, Meet, Teams.

---

### ✅ SFU vs MCU vs Mesh

|Type|How it Works|Pros|Cons|
|---|---|---|---|
|**Mesh (P2P)**|Each client sends stream to every other client|Low infra cost|O(n²) streams → doesn't scale|
|**MCU**|Mixes all streams into one and sends back|Simplifies clients|High server CPU, latency|
|**SFU**|Forwards streams without mixing|Scalable, efficient|Clients handle multiple streams|

---

### ✅ How SFU Works:

1. Client sends media stream to SFU.
    
2. SFU selectively **forwards** (not mixes) streams to other participants.
    
3. SFU may perform:
    
    - Bandwidth adaptation (send only one video at lower quality)
        
    - Simulcast handling (forward different resolutions)
        
    - Mute/unmute handling
        

---

### ✅ SFU Popular Implementations

- Jitsi Videobridge
    
- mediasoup
    
- Janus
    
- LiveKit
    
- [Ion SFU](https://github.com/pion/ion)
    

---

## 🔶 3. **RTMP (Real-Time Messaging Protocol)**

### ✅ What is RTMP?

**RTMP** is an old protocol designed by Adobe for **live video streaming**, primarily used to **push streams to a media server (e.g., YouTube, Twitch)**.

> 📌 Used for **1-to-many live streaming**, **not video conferencing**.

---

### ✅ How RTMP Works:

1. Broadcaster (e.g., OBS, Zoom) sends stream to **RTMP server** (e.g., NGINX + RTMP module).
    
2. RTMP server **repackages** stream (e.g., to HLS, DASH) and delivers it to **viewers**.
    
3. Viewers **pull** the stream via HTTP or WebSocket.
    

---

### ✅ RTMP Features

|Feature|Description|
|---|---|
|TCP-based|Stable, but has ~2-5s latency|
|Supports audio, video, metadata|Streams encoded in FLV|
|Push protocol|Good for ingesting stream to CDN|
|Doesn’t support low-latency interactivity|Not meant for meetings/chat|

---

### ✅ Why Zoom uses WebRTC/SFU, not RTMP

|Requirement|Zoom Uses|
|---|---|
|Real-time < 300ms|WebRTC + SFU|
|Bi-directional video/audio|Yes|
|Low-latency interactivity|Needed|
|RTMP good for broadcasting only|Not suitable|

---

## 🔶 Comparison Summary

|Feature|WebRTC|SFU|RTMP|
|---|---|---|---|
|Latency|< 200ms|< 300ms|~2–5 seconds|
|Directionality|Bi-directional|Bi-directional|Uni-directional|
|Use-case|Real-time meeting|Real-time routing|Broadcast/stream push|
|Scalable|No (Mesh) → Yes (SFU)|Yes|Yes (CDN)|
|Protocol|UDP, DTLS, SRTP|Based on WebRTC|TCP|
|Used in|Zoom, Meet, Discord|Zoom (Media Routing)|YouTube Live, Twitch|

---

## 🔶 Real World Usage

|Platform|Uses|
|---|---|
|**Zoom**|WebRTC + SFU, optional RTMP for webinars|
|**Google Meet**|WebRTC + SFU|
|**OBS + YouTube**|RTMP ingest|
|**Twitch**|RTMP ingest + HLS to viewers|
|**Facebook Live**|RTMP ingest|

---