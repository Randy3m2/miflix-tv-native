# BruniO Desktop 2.0 — Nuvio UI migration (engineering workspace)

> **Security notice:** The Windows MSI from this workflow is an engineering UI preview, not the integrated BruniO client. Do not enter personal BruniO login credentials in it: upstream authentication and data repositories are not yet all replaced. The Nuvio Sentry and Discord RPC startup paths are disabled. Only install/test with non-sensitive preview data until BruniO auth, profile, playback, and RP1 Party integration has passed cross-device tests.

> **Not a final installer.** This branch is an isolated, reviewed migration workspace. The existing PySide6/VLC BruniO PC and the Android/TV products are unchanged. Do **not** ship a renamed Nuvio binary as BruniO: BruniO backend and Watch Party interoperability must be implemented and tested first.

## Definitive architectural choice

**Upstream UI**: NuvioMedia/NuvioDesktop, branch `Dev`, pinned commit `37ce07d351fc69e17d05a4640ef78d08deba20e2` (reviewed 2026-11-11).
**Application**: Kotlin Multiplatform / Compose Multiplatform, desktop JVM/Skia + native mpv integration.
**BruniO API contract**: current `desktop/brunio-pc/brunio/core.py` and corresponding BruniO Android/TV code and RP1/RP134/RP204/RP205 SQL, **not** Nuvio's Supabase schema.
**Visual fidelity**: preserve upstream Compose components, original page proportions, motion, cursor/hover/focus, typography, desktop density scaling, and themes. Replace brand and services at boundaries, not the visual components. Use BruniO-owned icons/logos. No arbitrary new sidebar, layout, or rewritten hero.

### Detailed Nuvio UI audit

| Surface | Upstream implementation | What must remain unchanged |
|---|---|---|
| Desktop navigation | `core/ui/DesktopNavigationBar.kt`, `core/ui/jelly/JellyNavigationBar.kt` | Floating centered pill, frosted appearance under scroll/hover, icon/label animations, responsive width |
| Featured Home hero | `features/home/components/HomeHeroSection.kt` | Desktop layered gradient art, content at lower left, carousel paging, indicators, stable scroll position |
| Home content | `features/home/HomeScreen.kt`, `HomeRepository.kt`, `HomeCatalogDefinitions.kt` | Lazy shelves, skeleton/loading transitions, card shape, animated imagery; inject BruniO catalogs via adapter |
| Cards/hover | `core/ui/PosterLift.kt`, `PosterOpenMotion.kt`, `CardDepthEffect.kt`, `features/home/components/HomePosterCard.kt` | Same card scaling, hover response, click transitions |
| Continue Watching | `features/home/components/HomeContinueWatchingSection.kt` | Different landscape layout, progress bar, metadata overlay, hover sizing |
| Details | `features/details/MetaDetailsScreen.kt` and `components/DesktopDetailHero.kt` | Backdrop, trailer, casting, season and episode selector |
| Search | `features/search/SearchScreen.kt` | Dynamic grid and discovery, recent searches, skeletons, responsive spacing |
| Library | `features/library/LibraryScreen.kt` | List/filter/collection UX; BruniO user_state and favorites must supply data |
| Profiles | `features/profiles/ProfileSelectionScreen.kt`, `ProfileRepository.kt` | Visual picker; mapping BruniO profile strings to Nuvio integer indices must preserve identity |
| Themes/layout | `core/ui/Theme.kt`, `core/ui/Tokens.kt`, `core/ui/ResponsiveLayout.kt` | Density scaling and dark palette; only replace labels/icons |
| Player | `features/player/PlayerScreenContent.kt`, `desktopMain/features/player` | Native mpv overlay and keyboard shortcuts; adapt BruniO track/stream settings |
| Party | `features/party/PartyScreen.kt`, `PartyRepository.kt`, `PartySync.kt` | Keep the Nuvio Party UI; **replace backend with BruniO RP1 gateway** |
| Downloads | `features/downloads/DownloadsScreen.kt` | Grid/progress UX; preserve existing BruniO downloaded files and scope |
| Settings | `features/settings/SettingsScreen.kt`, appearance and homescreen pages | Same Nuvio modal, layout and animation; BruniO Home Studio and Supabase settings |

### Authentication, data, Watch Party — hard compatibility constraints

1. Nuvio logs into its **own** Supabase configuration and its own tables/RPCs. Merely setting its Supabase URL to BruniO's URL does **not** migrate features. Reuse Supabase Auth identity but route application repositories through BruniO contracts.
2. BruniO stores per-account/profile state in `miflix_user_state` with `__account__` for profiles and per-profile strings, plus events in `rp1_miflix_*`, collections in RP204, Home settings in RP205.
3. Nuvio Watch Party uses `watch_party_create/join/update/heartbeat/send_message/list_messages` and a different media/playback/presence model. BruniO uses `rp1_miflix_watch_parties`, `rp1_miflix_join_room`, `rp1_miflix_request_access`, `rp1_miflix_request_playback`, `rp1_miflix_set_presence` and social RPCs. **These protocols are not interchangeable.** Adapter must preserve host approval, guest authority, room codes, chat/reactions, stream selection per member, synchronized playback and recovery.
4. BruniO PC uses `desktop/brunio-pc/brunio/core.py` (auth, profiles, TMDB, streaming, RP1 Party), `ui.py` (player, chat, profile, Live TV), `desktop_features.py` (downloads, calendar, multiview/sports). Reimplement this access layer in Kotlin; do not attempt to import Python widgets into Compose.
5. Distinguish per-device downloaded files / Live TV channel favorites from shared per-account data. Do not overwrite account preferences or expose add-on private URLs when transitioning.
6. Nuvio Cloud repositories can refresh or watch for events differently than BruniO; use local cache + reads on account/profile open + writes on edits, and an active session for Party only. Do **not** introduce Supabase polling for the Home.
7. For title identity, map `tmdb:movie:<id>` and `tmdb:series:<id>` to Nuvio `MetaPreview(id,type)` using stable IDs. Preserve progress positions/percent semantics in converters.
8. Standard Stremio add-ons may reuse Nuvio's parser and player stream UI, **but** sources from BruniO account settings require account migration and language/order preferences.

### Scope of final application

- Account login, profile selection and switching from existing BruniO account.
- Exact Nuvio Home, search, details, posters, UI animations, collections and nav.
- BruniO Home Studio (row ordering/visibility), local collections and favorite channels.
- Live TV with categories/subcategories, logos, optional favorite shelf, audio/subtitle preferences and removals.
- Sports: NBA, MLB, F1, NFL, fútbol, LIDOM and Live Scores; calendar.
- BruniO Party: cross-platform interoperability with existing TV/Mobile, host admission, six-digit code, friend activity, chat, reactions, stream selection, playback sync, seek/pause/resume, reconnect.
- Native desktop video with stream fallback, external subtitle and manual/autosync when supported, audio tracks, speed, chapters, next episode and resume.
- Downloads and offline playback; up to four live streams in Multiview with one selected audio source.
- Add-ons, provider order and language preference, Trakt if configured, parental profile controls.
- Updates, account logout and data cleanup.
- Windows MSI first; macOS/Linux packaging after functional parity.
- Original app remains usable and no changes to stable `main` until parity tests pass.

### Acceptance gate (required before calling this "final")

- Full Windows CI compiles successfully, passes unit tests, packages MSI and installs alongside the old BruniO PC without uninstalling or overwriting Nuvio.
- Screenshot comparison of Nuvio vs BruniO at 1366x768, 1920x1080, 2560x1440, maximized and windowed: Home, Search, Details, Library, Settings, Player. Intentional changes limited to BruniO branding and added BruniO controls.
- TV user can create a BruniO Party, Desktop joins via six-digit code, host approves, video seeks/pauses/resumes with bounded drift and chat/reactions arrive on TV/Mobile, vice versa.
- Supabase auth and profiles operate with existing account: no new Nuvio-only account, no separate party server, no lost progress or favorites.
- Channels from current BruniO TV import and play through the desktop engine, with correct logos; favorite removal works and respects per-device scope.
- Home Studio, lists, Watch Party and calendar work without new SQL migrations that would damage current BruniO schema.
- Stream requests honor authenticated headers, protected account URLs are not logged, party participants independently resolve playable streams.
- Downloads, subtitles, intro skipping, player source fallback, Multiview and Windows keyboard/mouse behavior tested on a real Windows host.
- GPL-3.0 text and upstream attribution included; use BruniO-owned trademarks/assets, verify third-party asset licenses.

### Implementation boundaries

* Nuvio pinned checkout is the **visual upstream** and must be provided as source under GPL on distribution; independent BruniO adaptations should remain in versioned patches/overlays rather than undocumented edits.
* Do not merge this exploration branch into `main` while any acceptance gate is unmet.
* **Current status:** audited and pinned; no installer or complete BruniO data/Party adapter is claimed in this branch.

## Relevant repositories

- Nuvio: https://github.com/NuvioMedia/NuvioDesktop (GPL-3.0, alpha)
- BruniO: https://github.com/Randy3m2/miflix-tv-native
