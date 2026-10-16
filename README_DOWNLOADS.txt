BruniO Web Downloads — apply AFTER RC60

This is a WEB + EDGE FUNCTION patch. No native APK/build/version change is required.

SETUP
1. Upload the included web/, supabase/ and verification/ files to the matching folders in your existing GitHub repository. Vercel root remains web/. Redeploy/wait for Ready. Web asset cache is v=10.
2. Deploy the NEW Edge Function, named exactly brunio-downloads:
   supabase functions deploy brunio-downloads --no-verify-jwt
   The function verifies the signed-in user with Supabase Auth itself.
   Dashboard alternative: Edge Functions > create a new function brunio-downloads, paste supabase/functions/brunio-downloads/index.ts, disable the gateway legacy JWT verification (auth is checked inside), Deploy.
3. Existing TMDB_TOKEN must be available in Supabase Edge Function Secrets. SUPABASE_URL, SUPABASE_ANON_KEY and SUPABASE_SERVICE_ROLE_KEY are provided by the runtime. Do not put private keys in web files or GitHub.
4. Your own account's Comet/Torrentio TorBox add-on setup must already be saved/synced from BruniO TV. This reuses that setup; no shared/global TorBox account is added.
   Built-in supported hosts: comet.feels.legal, comet.elfhosted.com, torrentio.strem.fun.
   Only if you use a different trusted provider: add DOWNLOAD_ADDON_HOSTS in Edge Function Secrets as comma-separated exact hostnames (no scheme/path). Do not add local/IP hosts. No SQL changes.

USE
Open a movie/series title > Download. For series choose season/episode in the title first. Choose source > Generate link > Download. Refresh sources forces a new provider lookup when links fail. Kids profiles are blocked.
The browser handles download progress, file destination, storage availability and format support. Large files are not buffered in JavaScript. Depending on browser and CDN Content-Disposition, a video link can open in a new tab instead of immediately saving; use the browser's Save/Download action in that case. No web video player is embedded.

BEHAVIOR / PRIVACY
- Resolves only TorBox-labelled ready URLs from the account's configured trusted add-ons; raw magnets and uncached/TB Download entries are omitted. It does not add arbitrary torrents/magnets or manage your TorBox library.
- Lists contain metadata and encrypted expiring tickets, never raw add-on/API URLs. AES-GCM tickets last15min, bound to user/profile/current add-on config and work across cold starts.
- Follows provider redirects on the server to final HTTPS TorBox CDN links. TorBox API requestdl tokens stay server-side; API-key permalinks are not sent to the browser. Provider redirect endpoints must use supported hosts; unsupported redirect/CDN hosts are rejected.
- No authenticated headers are forwarded to add-on/CDN hosts. No console logging of secrets. The website's startup profile query now projects profile names/settings instead of fetching the full private account setup.
- File transfer goes directly from TorBox to the browser, not through Supabase/Vercel. On-demand source listing/link generation does make authenticated Supabase requests and therefore generates some logs. No background polling added. Sources cached up to3h per user/title/episode/config on warm instances; manual Refresh bypasses cache. TorBox account limits/bandwidth still apply.

VALIDATION
Passed Node syntax and mocked backend tests: auth, Kids block, ready TorBox-only filtering, encrypted user-bound tickets, metadata key isolation, source cache, trusted redirect chain, API requestdl-to-CDN resolution, unsafe-host rejection and no CDN file proxy.
Passed web execution checks: title/deep links, pagination, recommendations, Now Playing and download button-to-anchor flow.
Not yet tested against your live TorBox/add-ons or in a real browser; no Supabase function was deployed from this environment.
