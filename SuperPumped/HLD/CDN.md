## HOW TO evict cache or dont cache if we goit 100 milllion request in a small windco period 


HTTP Headers The origin server tells the CDN what to do using cache-control headers like: - Cache-Control: no-store → Don't cache at all. - Cache-Control: private → Cache only for one user (e.g., in browser, not shared). - Cache-Control: public, max-age=600 → OK to cache for everyone for 10 minutes. 
2. Cookies CDNs often skip caching if the request has: - Session cookies - Authorization headers Because these suggest the response might be personalized