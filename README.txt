BruniO Web — Automatic Subtitle Preload + Cast Icon (shell/player 28)

INSTALL — both steps are required:
1. Upload these files over the matching GitHub paths. Deploy the updated existing Edge Function:
   supabase functions deploy brunio-catalog
2. Redeploy Vercel for the web folder, then reload the website.
No new secrets, SQL migrations or TV APK are needed. Existing TMDB_TOKEN and Supabase runtime auth configuration remain in use. This package extends BruniO_Web_Favorites_Subtitles_Cast.zip and includes the current web source files.

BEHAVIOR
- Opening an identified movie/episode in the web player starts an asynchronous subtitle lookup without blocking playback.
- Uses the same public OpenSubtitles v3 / official fallback add-on endpoints already used by the native app. TMDB external IDs map to IMDb; series requests contain the exact season and episode.
- Up to three subtitle files preload: two options in the preferred UI language plus one fallback. Spanish prioritizes explicit Latino tags, then generic Spanish, then English. English UI prioritizes English. Providers do not always identify regional Spanish variants; generic Spanish is labeled honestly.
- The first online track enables automatically unless the user already chose Off/a manual track or an embedded track is showing. Other preloaded tracks remain selectable under Subtitles. Local SRT/VTT import remains available.
- Authenticated parent-to-player messages validate origin, frame identity and session channel. A standalone player can use the existing saved login on the same website. It does not put account credentials into the player URL.
- Server and embedded-player caches retain successful payloads for up to 30 minutes, bounded to 16 entries. Empty results expire after roughly one minute. Warm server cache still requires an authenticated request. No per-second polling.
- Files/redirects are restricted to HTTPS OpenSubtitles domains; text downloads and decompression are bounded. UTF-8 SRT converts to WebVTT; gzip subtitle bodies are supported. Failed/unavailable subtitles do not interrupt the video.
- The Cast/AirPlay button now includes the familiar display-and-radio-waves SVG icon. Browser Remote Playback/AirPlay capability is unchanged; no Cast SDK was added.

LIMITS
Online provider availability and subtitle timing vary by release. This does not search by movie file hash. Some files may need a different subtitle version. Local/Blob subtitle tracks cannot be transferred to every Cast/AirPlay receiver; no remote subtitle delivery guarantee is added. Casting still depends on browser/device/source support. No public subtitle URLs or tokens are exposed in sharing links.

CHECKS PASSED
Mocked backend tests: auth on cached results, exact episode lookup, language order, three-track bound, SRT/VTT conversion, cache reuse and unsafe host rejection.
Browser logic tests: parent bridge identity/channel checks and cache, subtitle Blob installation/cleanup, subtitles controls, Cast icon/dispatch, favorites/episode regression checks, catalog/experience and service-worker checks.
No live provider download, physical-device playback, subtitle synchronization or actual Cast discovery test was performed here. Backend TypeScript was stripped/evaluated in Node tests, not validated with the Deno deployment toolchain.
