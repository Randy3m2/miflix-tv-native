BruniO Web App — Content Experience Update
APPLY AFTER RC60 and the latest web patches (through Fullscreen Border Fix). This delta replaces the included web files with the current full versions. 16 files total; native Android sources/version are unchanged.

INSTALL / DEPLOY
1. Upload all included files/folders to the matching GitHub paths. Keep Vercel root web/, and redeploy. Do not omit experience.js, manifest.webmanifest, sw.js or the new icons. Cache release is19.
2. Deploy/update brunio-catalog with the included index.ts:
   supabase functions deploy brunio-catalog
   Dashboard: Edge Functions > brunio-catalog > Code > replace index.ts > Deploy updates. Existing manual Auth/JWT configuration remains. No new required secrets or SQL migration.
3. The existing brunio-downloads function (latest CDN fix), miflix-trakt comments function, brunio_sync_profile RPC and miflix_ratings table from the TV project remain prerequisites. This patch does not replace them or add a Watch Party function.
4. Reload the website. Chrome/Edge/compatible Android browsers: Install app or browser install menu. iPad: Safari > Share > Add to Home Screen / Open as Web App. Install from the public HTTPS Vercel website, not the temporary local TV pairing URL. Installation may require a fresh login. Manifest/shell support is implemented, but actual installation/device behavior is not verified here.

CONTENT PROFILES
- Full metadata/backdrop/title logo when available, synopsis/genres/runtime, cast photos in a horizontal row, actor filmography poster grids with Load more25, and related content.
- Season dropdown changes episode list immediately; episode artwork/details, source selection, queue and watched actions. Episode/filmography paging25; unwatched artwork blurred, episode text behind a spoiler disclosure. Future/unknown-release episodes cannot be played from the episode row.
- Random aired regular episode (no Specials), with repeat avoidance where possible. This is a manual random episode/source action; it is not continuous shuffle/autoplay.
- Existing Play on TV, Play or Download, browser/internal player, external Android/VLC/PC-copy options, direct file download, size limit and fullscreen retained.
- Play from beginning, manual source choice, Mark watched/unwatched, Rate1–10, Add to queue, View queue. Movie/episode watched state and ratings use existing TV-compatible tables/RPC formats. Marking a series affects its selected episode, not every episode of the whole series.
- Existing My List, Recommend to friends, events, sharing, Coming Soon and social/favorites functions retained. Coming Soon favorite/reminder behavior remains the existing favorites/reminder system, not a new email job.
- Trakt reviews appear below cast on explicit Load reviews, cached5min; spoiler reviews require disclosure. No trailers added.

PROGRESS / QUEUE / LOGS
- Embedded browser playback sends local same-origin postMessage snapshots about every10s; these are not HTTP/API requests. Only the active iframe/source/channel and valid time ranges are accepted.
- Resume snapshot stored locally by account/profile/title/episode. Cloud writes occur on pause (at most once per60s), ended or closing the embedded player; identical progress is deduplicated. No playback RPC polling or keepalive heartbeat. Modals closing/profile switch/logout stop/unload the iframe. Abrupt browser/OS termination may prevent final cloud sync; local resume is approximate to the latest snapshot.
- Standalone new-tab player can start at the provided resume position but does not continuously write TV progress; it has no authenticated parent. No claim of complete native playback parity: queue is LOCAL to this browser/account/profile (up to100 entries), not the TV queue, with explicit selection/removal; no automatic queue/episode advance. No Trakt playback scrobbling/skip-intro/subtitle-management/transcoding system is added.
- Source listing/generation, content/season metadata, opening reviews, explicit ratings/watched/favorite/event/social actions and the above sparse progress saves DO make Supabase calls/logs. Video bytes still transfer directly from TorBox, not through Supabase/Vercel. App-shell install/update uses Vercel static asset requests. The old statement that this player never saves progress is superseded by this release.

SCOPE / GUARDS
- No Watch Party UI, room calls, realtime subscriptions, background social refresh or party heartbeat added. Friends/recommendations remain user-triggered.
- Live TV intentionally deferred in this release to keep focus on the catalog/player and avoid new refresh loops. Kids profiles remain TV-only on this website; hidden from the web profile selector instead of exposing the adult catalog under a Kids label. This is not a new parental-control/PIN system.
- PWA caches only public app-shell files. Never caches Supabase/auth responses, TorBox media, private-query requests, download tickets or CDN tokens. Offline shell is not an offline movie library; normal catalog/login still needs internet.
- Browser codec/container, autoplay, native sharing and external-app restrictions from earlier patches still apply. No DRM circumvention, browser codec conversion or automatic third-party web-player credential sharing.

VALIDATION
Passed Node syntax and mocked checks for rich catalog/cast/logo, episode and filmography paging25, cached seasons and aired-only random selection, profile queue isolation/deduplication, TV-compatible watched/rating writes, spoiler UI, local resume messages/strict origin+source validation and bounded pause/end RPCs, manifest/icons/service-worker cache exclusions. Existing deep links/download/external player/Now Playing/fullscreen tests retained and passed. Mirror files and ZIP integrity verified.
Not performed: real browser rendering/physical iPad/Samsung/PC playback or install QA, live-account Supabase/TorBox tests, deployment or native APK compilation. No Android build is needed for this web-only patch.
