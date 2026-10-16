BruniO RC60 — TV headphones + web catalog / local network

APPLY AFTER RC59 + RC59 Visibility Build Fix. This delta also includes that visibility fix in AppState.kt.
Upload the included folders/files to their matching locations in the existing repository. Keep both app/ and main/ mirrors. No APK is included.

1. GitHub: upload these files, then run your Android release workflow.
2. Vercel: keep the existing web/ root; redeploy the GitHub update. JS/CSS cache version is now v=9.
3. Supabase: deploy the updated catalog function for backdrop artwork/runtime metadata:
   supabase functions deploy brunio-catalog
   Existing TMDB_TOKEN and auth settings stay in use. No SQL migration or new secrets.
4. TV: Profile → Connect phone / PC. TV may use Ethernet and phone Wi-Fi on the same private subnet/router. Scan a fresh QR/link with this APK. Isolated guest Wi-Fi/VLANs cannot reach the TV. TV must remain foregrounded. Kids and Party restrictions remain.

FEATURES
- TV headphone connection detection (wired, Bluetooth A2DP/BLE, USB), stereo track preference while connected, original channel limit restored after disconnection. Media usage/movie audio attributes, audio focus and noisy-output pause. Media3/AudioTrack manages the playback clock; no guessed Bluetooth offset or reduced network buffer. Bluetooth/TV hardware latency cannot be measured or eliminated automatically by this patch.
- Now Playing card in the connected catalog: title, state, episode and position/duration. Authenticated local requests every 5 seconds only while the page is visible and signed in; one in-flight request at a time. No Supabase polling for this feature. Snapshot includes no media URLs, request headers or credentials.
- Short share URLs: https://miflix-tv-native-web.vercel.app/#mfa (movie 550 example), or #s + base36 TV-series TMDB ID. Share sheet / clipboard / selectable-link fallback. Shared links open the title after normal BruniO login and never include the TV pairing capability/token/IP.
- Recents: the selected profile's synced TV history, newest first, Load more in groups of 25. Does not mean every locally opened web detail is watched; progress removed/zeroed on TV is not included. Uses Supabase when opened/refreshed, like My List.
- Larger responsive title profiles with top artwork, synopsis, release/rating/genre/runtime when available; Play on TV, My List, share, recommend to accepted friends and create event. Series season/episode inputs. No web video playback.
- Recommendations use the existing social recommendation table/RLS and appear to accepted friends (same behavior as TV).
- Updated in-app changelog; RC57–RC59 notes retained.

VERIFIED
Node JS syntax; mock execution of short/deep links/login continuation, 25-title batching, 51-title recents pagination, recommendation upsert headers, local-only status and visibility/disconnection behavior. Parent bridge origin/source checks, pairing-token isolation, local status/auth forwarding and invalid title rejection. Kotlin parser checks for modified components and PlayerScreen. Mirrored files and ZIP integrity.
NOT VERIFIED HERE
Android/Kotlin/JVM compilation (SDK/Gradle/javac unavailable), real browser rendering, physical Ethernet/Wi-Fi interoperability and actual headphone routing/lip synchronization. Run the GitHub release build, then test on the TV. Added Java socket status checks require a JDK and a private network interface.
