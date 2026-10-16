BruniO Web — Favorites, episode position, subtitles and Cast (shell/player 27)

Upload the web folder over the matching GitHub web files and redeploy Vercel. Apply to the existing configured BruniO Web app. This includes the recent mobile drawer, More styling, persistent login and detail-image fixes. No Supabase deployment, SQL migration or TV APK build is required.

Changes:
- Favorite controls update directly after the existing atomic RPC. No detail reconstruction, season reset or loss of loaded episode pages. Duplicate writes for the same title are prevented; invalid RPC replies show an error instead of wiping the visible favorites set.
- My List is available on each episode and in the embedded-player header. Episode favorites add/remove the parent series, consistent with TV favorites (not a separate favorite per episode).
- Mark watched/unwatched updates the episode row in place. The page, scroll position and expanded spoiler controls are retained instead of fetching episode page 1 again.
- Player Subtitles button: choose exposed caption/subtitle tracks, disable subtitles, or import UTF-8 SRT/VTT files up to 2 MB. SRT timings convert locally to WebVTT; uploaded text stays in a temporary Blob URL, revoked on replacement/cleanup. No automatic online subtitle search is included. Browser/container support determines whether embedded tracks are exposed. Uploaded subtitle files are local and are not forwarded to a remote Cast receiver.
- Player Cast button invokes the browser Remote Playback device picker or Safari's AirPlay picker. The iframe permits remote playback/presentation. Uses built-in browser APIs; no Google Cast SDK, external receiver app registration, Supabase polling or device scanning service was added. This is not universal DLNA, Miracast or a guarantee of Chromecast support on every browser. Compatible receiver, local network visibility, browser permission and receiver-supported codecs are required. Ethernet and Wi-Fi can be the same LAN. Try the standalone player if an embedded browser blocks its picker. Existing Play on TV remains the separate local BruniO TV feature.
- Player and shell versions bumped to 27; fresh assets picked up on reload.

Validation completed:
- Catalog/deep-link/recents checks.
- Experience, progress, queue and no Watch Party checks.
- Service worker shell/cache checks.
- Favorite add/remove regression checks and in-place episode action checks.
- Player source validation, SRT conversion/rejection, subtitle panel controls, Cast/Apple picker dispatch, lifecycle and external-player checks.

Validation limits: Node VM tests with mocked DOM/media APIs. Real Safari/Chrome video subtitle rendering, local-network discovery and receiver playback have not been tested in this environment. No APK was built.

Reviewed primary references:
https://www.w3.org/TR/remote-playback/
https://developer.apple.com/documentation/webkitjs/adding_an_airplay_button_to_your_safari_media_controls
