BruniO Web — Play in BruniO + existing external players

APPLY AFTER RC60 + Downloads CDN fix + subsequent web/player-button patches (latest: Samsung External Player Fix). Upload all included matching paths to GitHub and redeploy Vercel, root web/. IMPORTANT: upload the NEW web/player.html, too. Cache v=16. No APK, SQL, new secrets or Edge Function change required.

FLOW
Title > Watch / Download > select episode if needed > Generate link > Play in BruniO.
A responsive black/purple dialog opens BruniO's own player page. Native video controls support play/pause/seek/volume and available device fullscreen/PiP functions. iPad/iPhone inline playback is requested. If autoplay is blocked, tap Play in the video/page. No file picker or prior download required.
The generated link remains usable for Download and existing external options: Android video ACTION_VIEW intent, VLC for iPad/iPhone, and Copy link for other players/PC. Offline file picker/share remains available under collapsed Already downloaded? Offline file options, not in the primary streaming path.

FALLBACKS
Open web player in new tab opens a standalone BruniO player page, which may avoid embedded-page restrictions. The player page also provides Open in browser (direct TorBox URL), Android external player, VLC on iPad/iPhone and Copy link. Other third-party web players are not automatically sent private TorBox credentials/links; you can explicitly paste the private link into a trusted player if desired.
Another website cannot guarantee support for a container/codec this browser cannot decode. MKV/HEVC/DTS/etc. compatibility varies by actual file, OS and browser. There is no server transcoding/remuxing, HLS conversion or codec fallback library in this patch. Choose another source or an installed player for unsupported media. No guarantee every TorBox source will play.

PRIVACY / RESOURCES
- The video loads directly from TorBox/CDN, not through Supabase/Vercel. The existing source/link resolver remains the only Supabase interaction for playback; no new playback progress polling/logging or cloud scrobbling is added. Web viewing does not update the TV Recents/progress system in this patch.
- Native video uses metadata preload, not full-file JavaScript buffering. Closing/replacing the dialog or logging out/switching profiles unloads its iframe and stops that stream. Standalone player releases its media source on pagehide; a back/forward-cache return requires a new Play tap.
- Only HTTPS TorBox official domains are accepted. URLs are passed to player.html in a URL fragment (not query/server logs), then removed from that page's history URL. Tokens necessarily exist in the video/iframe DOM/network while used; direct download/media URLs are private and must not be published. Share title links using the separate title Share action.
- No external analytics/scripts/CDN player libraries; player page has a restrictive CSP and no-referrer policy. The file input never runs in the streaming flow. No automatic third-party-player page navigation or source switching.

VALIDATED
Node syntax and mocked parent/standalone player tests: URL/domain spoof rejection, private-fragment handling, dialog open/close cleanup, native controls, codec/autoplay error states, Android/VLC fallbacks, and prior source/cap/download/social/Now Playing flows passed. No real browser rendering/codec playback, live TorBox account or physical Samsung/iPad testing performed here.
References:
https://developer.apple.com/documentation/webkit/delivering-video-content-for-safari
https://html.spec.whatwg.org/multipage/media.html
