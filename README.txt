BruniO Web — Channels, Live TV & Sports (shell/player 29)

INSTALL
Upload the web files over their matching GitHub paths and redeploy Vercel. Includes the current responsive layout, persistent login, favorites and subtitle-preload frontend. No SQL, Edge Function deployment or TV APK build is required for Live TV. Online movie subtitle lookup still requires the brunio-catalog deployment from the previous subtitle patch.

FEATURES
- New Live TV navigation entry, with Channels / Live TV / Sports providers.
- Channels uses the same five IPTV-org country/language lists as TV: Dominicana, Türkiye, México, España and USA. Country/group selection, loaded-channel name/number search, channel logo grid, local per-profile favorites and Load more in batches of 25.
- Live TV defaults to https://stremio-addon-wheat.vercel.app/manifest.json (same native default); Sports defaults to https://sportsfree-us2.highfly.to/manifest.json. Add-on catalogs/genre options and skip pagination are supported. Provider source settings allow pasting a different public manifest.json URL if the TV uses a custom Sports source. Web does not fetch private native settings from Supabase.
- Choose a channel source and play in the same BruniO player, or open that player in a separate tab. No local TV pairing, TV IP, TorBox or phone-to-TV connection is needed for web playback. Sources in the player header lets you pick another link; Retry restarts the selected link.
- Native HLS where supported; otherwise bundled HLS.js 1.7.3 with bounded retry settings. HLS engine is destroyed on close. MP4/WebM/MOV URLs can use native playback.
- Existing Cast icon: Safari AirPlay, Google Cast Web Sender SDK for Chromium Live playback (default media receiver), and native Remote Playback fallback. The receiver receives the direct media URL, not a browser Blob URL. Google SDK loads in Chromium Live players; discovery/permissions and receiver-supported codecs still apply. External player links remain.
- Exposed stream subtitles/manual SRT/VTT remain available. Live channels do not trigger movie subtitle searches.

LOGS / DATA FLOW
The new live.js module has no Supabase requests. Country lists, add-on metadata, sources, logos and video go directly to their providers. Live favorites remain in this browser/profile, not synced with TV or Supabase. Live viewing does not save movie progress or call brunio_sync_profile. Existing website login/profile loading still uses Supabase normally.
Catalog metadata caches in tab memory: playlists up to 6 hours, manifests up to 1 hour, add-on catalogs 5 minutes, stream lists 30 seconds. Refresh sources bypasses cached metadata. Video media and signed URLs are not added to service-worker caches.

BROWSER LIMITATIONS — MATERIAL
A native TV app can load streams the browser cannot. HTTPS and appropriate CORS are required; browser JavaScript cannot forge Referer, Origin, User-Agent, Cookie or Authorization headers. Sources requiring those headers are marked as native-only. For ordinary public HTTP domain URLs, the source selector offers the same host/path over HTTPS as an explicitly described attempt; this works only when that server actually supports HTTPS. IP-only/localhost/private-network URLs and custom HTTP ports are not allowed in this web mode. No video proxy, DRM bypass, iframe scraping or transcoding is included. DASH, raw TS and unsupported codecs may require the native app or an external player.
Sports returned HTTP 403 from this environment, so its full availability in a browser is unverified. Use Provider source for a working public source you have configured in the TV. The same provider catalog does not guarantee every native-TV channel works on the web.

CHECKS
Passed mocked catalog tests: direct non-Supabase fetches, M3U parsing/group/search/cache, local favorites, 25-channel reveal/pagination, add-on paging and source handling.
Passed mocked player tests: native direct-live validation, HLS configuration/load/cleanup and Google Cast LIVE requests containing the original HTTPS URL, plus existing movie playback/subtitle tests.
Existing favorites, episode, progress, subtitle bridge, catalog and service-worker checks passed. UI IDs are unique and both source upload trees match byte-for-byte.
Live HTTP checks: IPTV-org Dominicana playlist and Nauta manifest/catalog responded 200 with Access-Control-Allow-Origin: *. Nauta returned 150 catalog rows; the sampled stream used HTTP, so HTTPS support for that endpoint was not assumed. No real browser/phone/TV media playback or Cast discovery was tested.

OPEN SOURCE
vendor/hls.min.js is the pinned HLS.js 1.7.3 distribution. Its BSD-2-Clause license is included in vendor/LICENSE-hls.txt.
Google Cast SDK loads from its official gstatic URL at runtime for Chromium Live playback.
