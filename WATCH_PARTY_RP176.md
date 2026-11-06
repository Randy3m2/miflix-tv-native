# RP176 — Offline and Browser Watch Party

## Offline Watch Party
The host creates a local room over Wi-Fi Direct or a phone hotspot. Playback commands are local WebSocket events; Supabase is not used and media bytes never leave the local network. The host can expose only a downloaded, playable file or an already-authorized local source. DRM content is rejected.

## Browser Watch Party
A temporary room stores only expiry, media metadata and sequenced control events. Guests receive a short-lived link/token and use the browser UI. The browser never receives BruniO account credentials or private source keys. Each guest must use a web-compatible, authorized source.

WatchPartyPolicyRP176 enforces expiry, guest limits, host/guest permissions and DRM rejection. Cleanup should delete expired rooms and events periodically; no video is proxied through Supabase.
