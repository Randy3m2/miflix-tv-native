BRUNIO 2.0.0-rc57 — versionCode 22055

INSTALLATION
This is a DELTA PATCH OVER RC56, not a standalone repository. Upload/extract all
included folders into your existing miflix-tv-native repository, preserving paths
and replacing matching files. Do not replace the repository with only this ZIP.
The existing RC56 source/resources, app logo and web assets remain required.

GitHub: commit the patch and run your normal native release workflow.
Vercel: the web/ changes redeploy through your existing connected project.
No SQL, Supabase Edge deployment, new secrets or playback on the website.
No APK was built in this workspace; validate compileReleaseKotlin in GitHub.

NEW IN RC57
- Send to TV over the local Wi-Fi subnet. Phone/PC never plays video.
- Cast filmography now uses posters. Favorite Live TV channels use logo tiles.
- Global focus/outline themes: White, Purple, Blue, Green, Amber, Rose and hex.
- Pill buttons; focused rail cards and synopsis wait 3 seconds before expanding.
- Playback startup shows backdrop and title logo when TMDB provides one.
- My List: All / Movies / Series, filtered before fetching, Load more in batches
  of 25; genre pagination from RC56 retains its 25-title batches.
- Focus a season for 240 ms to select it. Same season rail in the player; no arrows.
- Reactions: short OK opens compact emojis; long OK opens phrases.
- Long Play: Play from beginning, Add to queue, View queue, Recommend to friends,
  Play manually, Mark as watched. Rate is separate; More and Trailers removed.
- Long episode opens actions including manual sources and Mark as watched.
- Continue Watching long OK: Go to details, Play manually, Start from beginning,
  Remove. Removing preserves episode history; it writes a timestamped zero
  progress row so cloud merges do not immediately restore the title.
- Full-width cast row with community reviews below it.
- Notifications: Mark as read and local What's new card, with all changes in
  its details. Settings > System > Changelog / Details opens the same notes.
  Reading state is per profile. Release notes are bundled, not fetched from DB.
- Formula 1 avatars: Hamilton, Verstappen, Alonso, Leclerc and Norris.
- Settings > Account > Remember last profile (off until explicitly enabled).
- Movie Top 10 hero includes its rank. Menu screensaver after 5 idle minutes,
  using Top 10 backdrops and a bundled landscape fallback. No screensaver during
  playback, multiview, pairing or source/loading overlays. Any key wakes it.
- Friends has Friends / Requests / Calendar. Removed redundant upper
  Recommendations, Parties and Manage profile buttons; recommendations below
  remain. Manage profile is now in the profile menu.
- Player top X and maximize/restore-size toolbar buttons removed. Back still exits.
- Comet/Torrentio links: one clean heading, separated video/audio/source/size,
  small vector/text Dolby Vision, Atmos and HDR variants, only when reported in
  metadata. No inference that 4K/HEVC guarantees HDR/Atmos or TV compatibility.
  Hold OK on a source to read the original description and filename.

LOCAL SEND TO TV
1. TV: Profile menu > Connect phone / PC. TV must use Wi-Fi.
2. Phone/PC on the same local subnet: scan the QR, or enter the displayed
   http://PRIVATE_IP:8765 and six-digit code in the website's Send to TV dialog.
3. On the website choose a title > Send to TV > Open details on TV / Play on TV.
   For a series, choose season/episode. No stream URLs are accepted from clients.
4. If the browser blocks HTTPS-to-local HTTP, use Open local controller (or QR).
   It opens the TV-hosted control page in a new tab. The title is passed in the
   URL fragment; choose Connect, then send. The local page is in English.
5. Keep BruniO foregrounded. Pair code: 2 minutes. Session: up to 30 minutes.
   Backgrounding the app, switching profiles or a Wi-Fi network change closes
   the receiver/revokes sessions. Open pairing again for a fresh code.
6. Close hides the pairing window while keeping the receiver active. Disconnect
   stops it. New code revokes previous sessions. No background service or router
   port forwarding. Wi-Fi guest isolation/VPNs/different subnets can block access.
   Not available in Kids; incoming commands are ignored during a Watch Party.

This feature checks the TV's Wi-Fi interface, RFC1918 peer and subnet, pairing
code and a short-lived origin-bound token. It does not prove a physical house
or identical SSID. It is a local BruniO receiver, not Chromecast/AirPlay discovery.
Browser direct-access support varies; local controller is the fallback.

SCREENSHOT MAPPING
01-03 Friends buttons/tabs; 04 player X; 05 color settings; 06 season pills;
07 reactions; 08 player episode selector; 09 Continue Watching menu;
10 episode actions; 11 cast/reviews.
053309: old repeated source text. 053330: compact organized source reference.

VERIFICATION
Passed Java checks: LocalCastAccess/subnet policies, StreamPresentation,
QuickReactionGesture, Kids/navigation, SearchTextEditor and GenrePageBuffer.
Passed Node check: local cast endpoint restrictions; syntax checks for web JS
and the embedded local controller JS; HTML IDs match cast controls.
New/shared Kotlin files parsed for syntax; matching main/ source mirrors checked.
No Android SDK/Gradle compiler in this runtime, so this is not an APK compilation
or device/performance certification. Socket transport integration was skipped
because the runtime has no private IPv4 interface. Test on TV + phone/PC:
pairing, permission/fallback, background revocation, remote gestures, theme,
season focus, source badges, actual playback, update notes and 5-minute idle.

Avatar source URLs are recorded in PROFILE_AVATAR_SOURCES.json. Existing assets
and source attribution are retained. Driver portraits come from their F1 pages.
