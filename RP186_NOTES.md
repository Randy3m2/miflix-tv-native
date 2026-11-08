# RP186 — background motifs, continuous page canvas and background sharing

TV and mobile share one full-window theme canvas. Inner AppShell, Live, mobile, social and embedded standings surfaces are transparent when a theme is active. Safe padding and navigation stay intact. Modal sheets keep opaque surfaces for readability; player and Multiview keep black backgrounds. Motifs are static, cached, noninteractive, behind all content, and constrained inside portrait/landscape bounds. No theme symbols were added to controls. Default has no motif.

GSW uses the team logo retrieved from ESPN's team asset endpoint, https://a.espncdn.com/i/teamlogos/nba/500/gs.png, stored locally as theme_gsw_rp186.png. Team marks remain their owners' property; no affiliation is implied. Other motifs are original simple vector silhouettes.

## Casting scope

The native mobile cast button currently publishes the selected media through the Roku Media Player DLNA MediaServer route. It does not implement a general UPnP AVTransport push sender or native Android AirPlay. Its existing foreground service is independent of player/composable lifetime. This change explicitly sets stopWithTask=false, holds Wi-Fi awake during sharing, releases locks on stop, serializes replacement sessions, and checks the selected LAN network rather than stopping merely because the default network changes. URLs and headers remain in memory; no persistent credentials or video downloads are added. Force-stop, process termination, power-off, Wi-Fi loss and expired sources can still interrupt the local-server route; it is not a receiver-independent direct stream.

The web's direct Google Cast sender and Safari AirPlay paths are unchanged. Direct Cast and local DLNA gateway lifetimes are different. Safari's AirPlay needs its media page kept alive; this update cannot guarantee playback after closing it or OS termination. No BruniO TV APK is required for a supported receiver, but Roku still needs Roku Media Player and source compatibility. HLS/Roku codec compatibility and background continuity require real hardware verification; publishing media is not a playback acknowledgment.

Automated coverage includes theme mark bounds/opacity/catalog, existing palette checks, and HTTP media requests continuing after a discovery client disconnects. Physical Roku/TV, locked-phone and Safari AirPlay testing remain pending.
