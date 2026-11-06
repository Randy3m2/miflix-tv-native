# BruniO RP173 — Universal Cast + Theme Studio

## Universal Cast

`UniversalCastRP173` is the capability gate used by the mobile destination picker. It separates BruniO TV, a native DLNA renderer, Roku Media Player and screen mirroring. A Roku target is never reported as direct push playback: it is planned as a DLNA MediaServer browse flow, and HLS is rejected unless the target advertises verified HLS support.

The planner rejects DRM and unsafe authenticated URLs before a local gateway is selected. Live content is only planned for receivers advertising live support; the model also carries range, seek and Go Live capability for the player controls.

The existing authenticated, private-LAN `LocalCastServer` remains unchanged in this RP. The next integration step is to expose its selected media through a scoped DLNA MediaServer endpoint and SSDP advertisement. That endpoint must be tested on a real Roku Media Player before being labelled verified.

## Theme Studio

`ThemeStudioRP173` provides the 12 requested presets and a stable palette model. The Appearance settings now expose every preset without replacing the current layout, focus system or user-defined color. Applying a preset currently maps its cinematic focus color into the existing global focus/accent pipeline; artwork, textures and motion remain optional metadata so devices without the assets stay lightweight.

## Verification status

| Path | Status |
|---|---|
| Native DLNA renderer | Planner implemented; physical renderer test pending |
| Roku Media Player | Capability-gated plan only; real-device test pending |
| Live HLS / live sports | Rejected when unsupported; gateway integration pending |
| DRM / private credentials | Explicitly rejected by planner |
| Theme presets | Model + Appearance picker implemented |

The repository snapshot does not include a Gradle wrapper, so CI/Android compilation must be run by the existing GitHub Actions workflow.
