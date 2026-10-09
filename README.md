# Última versión: BruniO 2.0.0-rc19

Consulta **RC19_SETUP.md**. Búsqueda con historial y resultados en el mismo scroll; carátulas más pequeñas. Conserva la firma de RC17. No requiere SQL nuevo para este cambio.

# Versión anterior: BruniO 2.0.0-rc18

Consulta **RC18_SETUP.md**. Ejecuta **supabase_rc18_upgrade.sql**, publica Pages y conserva los mismos cuatro secrets de firma de RC17 antes de la build. Incluye filtros de tamaño/año, limpieza de Party, Joined y avatares por QR.

# Versión anterior: BruniO 2.0.0-rc17

**Antes de la build configura los cuatro secrets de firma: RC17_SETUP.md.** El ZIP privado de firma se conserva fuera del repositorio. Esta versión agrega actualizaciones desde la app, firma estable, Home compacto y reproducción aleatoria personalizada. No requiere SQL nuevo.

# Versión anterior: BruniO 2.0.0-rc16

Consulta **RC16_SETUP.md** para los cambios de foco, reproductor, historial y avatar. No requiere SQL nuevo.

## Versión anterior: BruniO 2.0.0-rc15

Consulta `RC15_SETUP.md`. RC15 incluye una limpieza dirigida de manifiestos compartidos: consulta RC15_SETUP.md. Para instalaciones anteriores a RC6, la eliminación de perfiles requiere `supabase_rc6_upgrade.sql` después de RC5.

# MiFlix TV Native 2.0 RC1

Native Android TV client built with Jetpack Compose for TV + AndroidX Media3/ExoPlayer.

## RC1 highlights

- OpenSubtitles v3 is queried for every movie / episode before playback.
- English and Spanish subtitle tracks are normalized and prioritized; stream-embedded subtitles are merged with OpenSubtitles v3 results.
- Series can switch season / episode from inside the player.
- Watch Party rooms with 6-digit code + QR. The host syncs play/pause/seek/episode through Supabase; each participant resolves the stream locally with their own add-ons.
- Streaming service collections (Netflix, Disney+, Prime Video, Apple TV+, HBO Max) open into full platform hubs:
  - Top 10
  - Movies
  - Series
  - Action / Comedy / Horror / Drama / Sci-Fi / Thriller / Animation / Romance
  - Latest + Best Rated rows for each major genre
- Genres in the main sidebar. Each genre loads up to 50 mixed movie/series results.
- Movies by Year (2026 back to 1990), up to 50 titles per year.
- Account-level Torrentio / Comet manifests; profiles share app/add-on setup while keeping My List + playback progress separate.
- Add Profile flow on TV.
- Secure QR device pairing for sign-in and/or sending a Torrentio / Comet manifest from a phone.
- Fixed catalog edge clipping by using a 5-column TV grid + larger end padding.
- Replaced screen-owned coroutine scopes with an application-owned scope for navigation/network actions to eliminate the `rememberCoroutineScope left the composition` runtime error.
- Permanent latest APK channel remains `native-latest / MiFlix-TV-Native.apk`.

## One-time Supabase migration

Run `supabase_rc1_setup.sql` in Supabase SQL Editor before using Watch Party or QR pairing.

## One-time QR pairing page setup

The project contains `docs/pair` and `docs/party`, plus `.github/workflows/deploy-pages.yml`.

In GitHub:

1. Repo -> Settings -> Pages
2. Under **Build and deployment**, choose **GitHub Actions** as Source.
3. Go to Actions -> **Deploy MiFlix Pairing Pages** -> Run workflow once.

The TV QR uses:

- `https://randy3m2.github.io/miflix-tv-native/pair/`
- `https://randy3m2.github.io/miflix-tv-native/party/`

The sign-in/add-on payload is AES-GCM encrypted in the browser using a random key stored only in the QR fragment. The Supabase pairing row stores only ciphertext + IV and expires after ~10 minutes.

## Existing GitHub Actions secrets

Keep these in Repository Settings -> Secrets and variables -> Actions:

- `MIFLIX_TMDB_TOKEN`
- `MIFLIX_TORRENTIO_MANIFEST`

`MIFLIX_TORRENTIO_MANIFEST` remains the bootstrap manifest. After account sync, MiFlix can restore all saved Torrentio / Comet manifests from Supabase.

## Build

Run **Build MiFlix Native TV APK** in GitHub Actions.

Outputs:

- `MiFlix-TV-Native-v2.0.0-rc1.apk`
- permanent latest asset: `MiFlix-TV-Native.apk`

Versioned release tag:

- `native-v2.0.0-rc1`

Permanent release tag:

- `native-latest`


## Watch Party Social + Trakt update

See `SOCIAL_TRAKT_SETUP.md` for installation and the complete feature scope.
Execute `supabase_party_social_upgrade.sql` and deploy the updated Pages site.
Trakt also requires the included `miflix-trakt` Edge Function and your Trakt API
application credentials. All prior RC1 fixes and Live TV are included.
