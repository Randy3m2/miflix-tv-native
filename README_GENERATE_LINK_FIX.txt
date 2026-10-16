BruniO Web Downloads — CDN / Generate Link Fix

Includes and supersedes the earlier Downloads and VLC/Size Limit web patches. Apply after RC60. No Android files, APK build or SQL migration.

1. Upload the included folders/files to the matching GitHub paths. Redeploy Vercel (web/ remains the root; assets v=12).
2. Update/deploy the full brunio-downloads/index.ts in Supabase:
   supabase functions deploy brunio-downloads --no-verify-jwt
   Dashboard: Edge Functions > brunio-downloads > Code > replace index.ts > Deploy updates. Authentication is verified inside the function with Supabase Auth.
3. Reload the website, open Download, press Refresh sources, then Generate link.
4. If it still fails, send only the new error CODE (e.g. PROVIDER_HTTP_403, TORBOX_NOT_READY, LINK_HOST_UNSUPPORTED). Do not send full download/add-on URLs or tokens.

FIXES
- The original host check allowed only *.torbox.app. It rejected official TorBox CDN hosts *.tb-cdn.cx/io/pw/sh/st/to/earth. Allowlisting now matches these exact domain boundaries on both backend and web, including VLC links; similarly named attacker domains remain blocked.
- Handles TorBox requestdl HTTP redirects even when redirect=false was requested, as well as JSON responses.
- Provider resolution cancels the response body as soon as redirect headers arrive; no file bytes are forwarded or buffered. Returns typed non-secret error codes for host, provider HTTP/network, TorBox HTTP/not-ready, ticket, and generic resolution failures. No secret-bearing upstream errors/URLs are logged or returned as diagnostics.
- Keeps the default editable5GB cap (0 unlimited), size parsing and unknown-size filtering; iPad/iPhone Open in VLC, browser Download, Copy link and manual-copy fallback.

PRIVATE LINK CORRECTION
Source-list responses expose encrypted tickets and display metadata only. Raw account add-on URLs are never returned or hardcoded in the website. The generated direct CDN URL is intentionally returned only to its authenticated owner. TorBox CDN URL authentication tokens may be private account credentials, not necessarily harmless short-lived signatures. These direct download/VLC links are private: do not publish or share them. Use the title-profile Share button to share a movie/series instead. No TorBox API requestdl permalink is returned to the browser. The prior claim that an API key can never appear in any generated CDN URL was too broad; this patch documents the provider's actual behavior rather than promising that guarantee. Keeping all CDN credentials out of the browser would require proxying the file, which this implementation does not do.

REQUIREMENTS / NOTES
TV need not be on or connected. Sign into the account with its Comet/Torrentio TorBox setup synced from TV. Existing TMDB_TOKEN runtime secret required. Default supported add-on hosts: comet.feels.legal, comet.elfhosted.com, torrentio.strem.fun; custom trusted hosts may use DOWNLOAD_ADDON_HOSTS. Kids downloads are blocked. No new required secrets.
Download/stream transfer is direct TorBox-to-device. On-demand source/link calls still generate Supabase requests/logs; opening the generated link in VLC does not add another Supabase request. Offline playback requires downloading first and opening the saved file from Files/Downloads in VLC/another local player.

TESTS
Node syntax and mocked backend/frontend checks passed, now including real-format tb-cdn.io links with token query parameters, domain-spoof rejection and VLC encoding. Auth, user-bound encrypted tickets, cap defaults/overrides, unknown-size/GiB handling, link generation flow and existing web flows retained. No live account/Safari/Chrome-on-iPad tests or deployment performed here.
Official CDN reference: https://support.torbox.app/en/articles/12667085-torbox-is-being-blocked-by-my-anti-virus-software
