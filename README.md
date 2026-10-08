# MiFlix TV Native 2.0 Beta 1

Native Android TV client built with Jetpack Compose for TV and AndroidX Media3.

## Beta 1 highlights
- Smooth native D-pad UI, fixed compact sidebar, black/white visual system.
- Home Top 10 carousel, streaming collections with focus-only GIFs.
- Platform hubs: Top 10, Movies, Series, Latest, Best Rated and platform-specific genre rails.
- Main genre browser with up to 50 mixed movie/series results per genre.
- Movies by year browser (2026 back to 2000), 50 titles per year.
- QR phone pairing over the local network for account sign-in and adding configured Torrentio/Comet/Stremio manifests.
- Multiple profiles sharing account-level TMDB/add-ons while keeping My List/progress separate.
- Watch Party rooms with QR/code, host play/pause/seek sync, and per-device stream resolution.
- Media3 player, subtitles/audio tracks, progress sync, update channel and permanent APK URL.

## Required GitHub Actions secrets
- `MIFLIX_TMDB_TOKEN`
- `MIFLIX_TORRENTIO_MANIFEST`

## Supabase
The existing `miflix_user_state` table is still used for account/profile state.
Run `SUPABASE_WATCH_PARTY.sql` once to enable Watch Party rooms.

## Private add-ons
Configured add-on manifests can contain personal credentials. They are treated as account-private setup and should never be committed to the public repo. The build-time Torrentio URL comes from a GitHub Actions secret; QR-added manifests are stored in the authenticated Supabase account state.
