# MiFlix TV Native 2.0 — Alpha Final

Native Android TV client built with Jetpack Compose for TV-style navigation and Media3/ExoPlayer playback.

## Final-alpha focus

- Fixed compact left sidebar: Home, Search, Collections, My List, Settings + profile shortcut.
- Black UI / white controls and focus states.
- Auto-rotating Top 10 hero from TMDB weekly trending.
- Streaming collection tiles for Netflix, Disney+, Prime Video, Apple TV+ and HBO Max.
- Static collection art at rest; focus GIF is loaded only while the tile is focused.
- Single-press DPAD_CENTER/OK activation on cards, buttons, episodes, trailers and profile tiles.
- Poster-style native catalog rails plus landscape Continue Watching cards.
- Rich movie/series details: cast, trailers, metadata, seasons and episodes.
- Supabase account sync restores profiles, My List, progress, TMDB and private Torrentio/TorBox configuration.
- Native Media3 player with audio/subtitle tracks and progress sync.
- Permanent `native-latest` release channel with stable APK URL.
- Settings → Check for Updates.

## Build secrets

Create these repository secrets in GitHub Actions:

- `MIFLIX_TMDB_TOKEN`
- `MIFLIX_TORRENTIO_MANIFEST`

Supabase URL and publishable key are already compiled into this personal build.

## Permanent APK URL

`https://github.com/Randy3m2/miflix-tv-native/releases/download/native-latest/MiFlix-TV-Native.apk`

Create one Downloader/AFTVnews code for that URL and keep reusing it. Every workflow run replaces the `MiFlix-TV-Native.apk` asset while preserving versioned releases for rollback.

## Collection art

Streaming/genre collection artwork references public community Nuvio collection assets. Focus GIFs are requested only when the corresponding collection tile receives focus.
