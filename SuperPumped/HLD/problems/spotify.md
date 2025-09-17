https://www.youtube.com/watch?v=_K-eupuDVEc&t=241s
https://blog.algomaster.io/p/design-spotify-system-design-interview

A. Client-Side Ad Insertion (most common for Spotify)
Playlist continues as normal.

At the ad break point, the client pauses music playback.

Client streams the ad directly from CDN (like a song).

After ad finishes, client resumes the next song.

✅ Pros: Simple, works offline if ad is pre-cached.
❌ Cons: Easier to block with ad-blockers on desktop/web.

B. Server-Side Ad Stitching
Spotify’s streaming backend dynamically merges ad audio chunks into the music stream.

Client plays it as if it’s just another song chunk.

User can’t skip unless allowed.

✅ Pros: Harder to block, works seamlessly.
❌ Cons: More processing cost, needs tight CDN integration.