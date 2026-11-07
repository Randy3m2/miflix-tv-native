# Roku Media Player sharing — RP182

BruniO Mobile hosts the selected movie, episode, IPTV channel or sports stream as a local DLNA MediaServer. This is a Roku Media Player browse route, not a push-to-play receiver or screen mirroring. It does not send Roku ECP commands.

## Using it

1. Connect the phone and Roku to the same private Wi-Fi network.
2. In the mobile player, press Cast. For an IPTV channel, hold the channel and choose Roku Media Player.
3. Choose Share with Roku Media Player.
4. On Roku, open Roku Media Player → Video → BruniO Mobile → Now Playing.
5. Select the published content. Movies, Episodes, Live TV and Live Sports folders also expose the selected item in its corresponding category.

Only the currently selected content is published. Publishing a source does not prove the Roku has started playback. Playback controls remain in Roku Media Player. Live HLS compatibility must be checked on the actual Roku; a live source is not advertised as verified merely because it can be served over HTTP.

## Server behavior

- SSDP MediaServer announcements and ContentDirectory browsing over the private LAN.
- Session-token media URLs; no arbitrary upstream URL endpoint. Source URLs and authentication headers are not returned in the media catalog.
- MP4 byte ranges and HEAD requests forwarded to the source.
- HLS manifests, segments and key URLs rewritten through the selected-source gateway. Private headers are forwarded only to the original origin. Redirects to another origin do not receive those headers, and cross-origin redirects to private/link-local/loopback addresses are rejected.
- No downloading a full file, decoding or transcoding. Video passes through bounded buffers, so the phone must remain on the network and uses bandwidth for the transfer.
- A connected-device foreground service, ongoing Stop notification, wake lock and Wi-Fi multicast lock keep the server available while the phone screen is locked. Actual device power policies still require testing.
- Changing Wi-Fi stops sharing and asks the user to start again on the matching network. Stop releases HTTP sockets, upstream connections, SSDP and locks.
- Explicit errors for source failures, HTML pages and recognized DRM HLS playlists. The gateway cannot remove DRM or make incompatible codecs playable.

## Verification report

Five JVM HTTP/DLNA contract tests pass: description/Browse, MP4 Range and HEAD, HLS manifest/segment routing and private headers, wrong-token/arbitrary-proxy rejection and resource release, explicit DRM-playlist rejection. Planner tests prevent Roku from being reported as a direct playback destination.

Android TV and Mobile Kotlin/Java release compilation and unit-test suites passed locally (604 tests per module), as did both release-vital lint checks. APK signing and publication remain the repository release workflow's responsibility.

These are server-contract tests with fixture bytes. They are not actual movie playback tests and are not tests on generic DLNA televisions or physical Roku hardware. Roku Media Player discovery, live channel/sports decoding, codec compatibility, screen-lock playback, and Wi-Fi reconnection need a physical Roku acceptance test. No Roku model is marked verified by this release.

Official reference: https://support.roku.com/en-us/article/share-media-over-usb-or-network
