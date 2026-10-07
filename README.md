# MiFlix TV Native 2.0 — Alpha 1

Parallel Android TV client built natively with Jetpack Compose for TV + Media3/ExoPlayer. It uses a different application ID (`com.miflix.native2`), so it installs beside the legacy WebView build.

## Alpha 1 goal

Performance first. This build is intended to benchmark D-pad feel, rails, details navigation, and native playback before porting every legacy feature.

Included:
- Native splash screen
- Native sidebar with intentional focus-only expansion
- Native LazyColumn/LazyRow catalog browser
- TMDB Trending / Popular Movies / Popular Series / Top Rated
- Search
- Full-screen native Details screen (not an HTML modal)
- Native series episode rail; clicking an episode immediately plays it
- Torrentio/TorBox stream resolution
- Native Media3/ExoPlayer playback, seek, audio tracks and subtitle button
- Supabase login using the same project as PC
- Pulls profiles + private TMDB/Torrentio setup after login
- My List + progress sync using the existing `miflix_user_state` table

Not ported yet:
- Watch Party
- Collections/streaming-service GIF folders
- advanced appearance settings
- skip-intro metadata
- manual stream picker

Those should be ported only after this shell feels smooth on the TV.

## Recommended repo
Create a second GitHub repo named `miflix-tv-native` and upload this project there. Do not overwrite the legacy repo.

Reuse these existing GitHub Actions secrets:
- `MIFLIX_TMDB_TOKEN`
- `MIFLIX_TORRENTIO_MANIFEST`

They are optional after login because the app can pull private setup from your Supabase account, but the TMDB secret lets Home render before login.

## Build
Actions → **Build MiFlix Native TV APK** → Run workflow.

Release artifact:
`MiFlix-TV-Native-v2.0.0-alpha1.apk`

Release tag:
`native-v2.0.0-alpha1`
