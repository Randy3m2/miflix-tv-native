# MiFlix TV Native 2.0 — Alpha 2

Parallel Android TV client built with Jetpack Compose UI and Media3/ExoPlayer.

## Alpha 2 focus

This build deliberately prioritizes motion quality and D-pad behavior before adding every legacy feature.

### Motion / focus polish
- Media cards use a small 1.035x graphics-layer focus scale with a 100 ms non-spring tween.
- Every card has a fixed outer footprint, so scaling does not change LazyRow/LazyColumn measurements.
- Rails have fixed heights to avoid vertical correction while moving left/right.
- Hero/backdrop changes are delayed 420 ms. Rapid D-pad movement updates only focus; the expensive hero image is changed only after focus settles.
- Hero transition is a short crossfade.
- Sidebar expansion overlays content instead of resizing the Home layout.
- Sidebar collapses after focus leaves it and no longer shifts the full screen.
- Episode/profile focus animations use the same no-bounce motion language.

### Alpha 2 features
- Continue Watching shelf with progress bars.
- Resume movie / continue series actions in Details.
- Collections screen.
- Action and Science Fiction collections.
- Netflix and Disney+ discovery shelves through TMDB watch-provider metadata (US discovery fallback).
- Debounced Search.
- Automatic authenticated private setup restore for TMDB and Torrentio/TorBox.
- If the private setup is missing from Supabase, the GitHub build secrets bootstrap it into the authenticated account row.
- Player controller auto-hides after 2 seconds.
- Playback completion is written to progress and synchronized.

## Architecture

- UI: Jetpack Compose + TV Material
- Images: Coil 3.5
- Metadata: TMDB
- Streams: Stremio-compatible Torrentio/TorBox manifest
- Player: AndroidX Media3 / ExoPlayer
- Sync/Auth: Supabase REST/Auth

No WebView is used for the main TV UI.

## Build secrets

Repository Settings -> Secrets and variables -> Actions:

- `MIFLIX_TMDB_TOKEN`
- `MIFLIX_TORRENTIO_MANIFEST`

Never commit the private Torrentio/TorBox URL to a public repository.

## Package

`com.miflix.native2`

This intentionally stays separate from the legacy MiFlix TV package, while Alpha 2 installs over previous Native 2.0 alpha builds.
