BruniO RP97 — Final Live TV catalog, local M3U refresh/QR, LIDOM exceptions and recording sports
Version code 22201 / version name 2.0.0-rp97
Apply over RP86–RP96 preserving paths. Includes all prior patch files. No APK generated.

CATALOG REPLACEMENT
- The supplied merged_playlist_with_logos (1).m3u is bundled unchanged as the initial catalog:
  2,660 entries, 12 categories, 2,438 supplied logos / 222 without a supplied logo.
- Two explicitly requested exceptions: Digital 15 and Telemedios Canal 8 are retained as HLS
  backups from the previous IPTV-country feed, with logos and required Referer/User-Agent.
  Initial total: 2,662 channels. They appear in Republica Dominicana and resolve for LIDOM.
- Backups are omitted if the primary M3U already contains that station, so a future uploaded
  entry takes precedence. URLs are not guaranteed available; Telemedios is marked Not 24/7.
- Retires the old Nauta/curated Tirso/IPTV/IPTV+/Nuvio Test channel catalogs from active browse.
  Old bundled assets/source helpers are not all physically deleted by this patch; they are not
  loaded as active channel catalogs. Sports event provider and VOD movies/series are unchanged.
- New device-local edit/import namespaces retire previous manual/imported channel rows, as requested.
  New personal channels/M3U imports remain possible. New edit records carry a catalog version
  so restoring an older settings backup cannot resurrect the retired channel rows. Old account/friend/profile data is not deleted.
- Categories preserve the provided group-title labels. Republica Dominicana is the initial group,
  All/Todos remains available. Uses the same grids, player, category picker and SplitScreen flows.
- Favorites from retired catalogs map to current channels only on ID or a unique normalized name;
  retired URLs never play from the old favorite record. Sports favorites remain intact.
- Removal is per profile; final channel IDs survive an update when only the playback URL changes.

ARTWORK
- Dragon Ball Z 24/7 uses the EXACT recovered RP53 custom PNG at tirso_logos/044.png.
  Bytes were compared to BruniO_RP53_Dragon_Ball_Z_Custom_Logo_Hotfix.zip.
- Pinky y el Cerebro retains the image identified in the supplied M3U. Its TMDB image-host URL
  is corrected to image.tmdb.org for the same asset path, without substituting another image.
- Existing artwork index is reused by normalized channel name only when a new logo is missing,
  except the explicit DBZ override. A referenced local asset must exist before it is reused.
- Otherwise existing text placeholders remain, including when an image request fails.

GITHUB / LOCAL UPDATE
Settings > System > Main Live TV playlist:
1. Change M3U URL by QR. Scan on the same LAN and paste the HTTPS Raw GitHub URL.
2. Select Update channels. The TV downloads/validates and stores its own local snapshot.
3. Edit/replace that SAME file on GitHub; another Update channels fetches the new version.
   A different URL must be saved through the QR first. No build or Supabase SQL required.
- The URL and snapshot live on this TV, shared by its profiles. No periodic refresh or cloud logs.
- Initially uses the bundled file; thereafter uses the last valid local download.
- Rejects non-M3U, empty, >4-MiB or >10,000-channel payloads before replacing the snapshot.
- AtomicFile preserves the previous file if writing fails. One refresh job at a time; UI ignores
  repeated clicks while busy. Parse/download run off Main; cancelled or superseded URLs cannot
  publish a stale download before commit.
- Disables local HTTP caching and adds a fresh query key only for raw.githubusercontent.com.
  Other hosts' signed query strings are left unchanged. A provider outage/invalid URL retains
  the local list, without reviving the retired catalogs.
- Channel deletion overrides persist across refresh for stable IDs.
- The LAN QR receiver reuses the existing ticket, CSRF/origin, private-network and expiry rules;
  accepts HTTPS URLs only, closes when foreground/profile ownership changes.
- QR save records the URL; the user then selects Update channels. No GitHub repository was
  created or published here, and no URL is preconfigured until you supply your Raw link.

LIDOM
- Existing team/source assignments, favorites, YouTube channels, direct HLS/DASH and last choices
  are retained in the original LIDOM preferences.
- TV sources now resolve by station name/aliases in the final catalog, exact names first.
  No dependence on old channel IDs or retired provider catalogs.
- Digital 15 and Telemedios use the two retained backups when absent from the new primary list.
- Other seeded matches include Teleantillas, Coral 39, Teleuniverso and CDN Deportes.
- A station genuinely absent from both the new list and the explicit backups cannot be invented;
  manual/YouTube/direct sources remain available. No unrelated same-looking channel is substituted.

LIVE REMOTE
- With focus on the video / hidden controls: OK opens Channels.
- Left/right reveal controls; right no longer opens the channel list.
- While controls or another panel are focused, navigation/activation continues normally.
- Up/down channel navigation, timed favorites/options holds and Stats are retained.
- Applies to live TV/sports; VOD/recording seek controls are not repurposed.

RECORDINGS BY SPORT
- Others includes recording categories published under individual sports as well as generic Recaps.
- Local sport chips: All, Basketball, Baseball, American Football, Soccer, F1/Motor, Tennis,
  Hockey, Combat and Other, showing the sports detected in the loaded results.
- Uses event names first, then category metadata/description. Unknowns remain in Other/All.
- Changing chips does not query the provider. Existing Load more still appends provider pages.
- Retains recording metadata and VOD pause/seek. Does not convert live events into recordings.

VALIDATION
- 11 actual Kotlin/JVM JUnit tests passed for the full 2,660-entry file, unique/stable IDs, all groups,
  missing-logo count, Unicode/commas/headers, invalid/DRM/unsafe input, LIDOM name matching,
  backup precedence/deduplication, remote-key behavior, QR HTTPS/origin/token checks and recording groups.
- Java receiver, remote key policy and recording classifier compiled in the JVM harness.
- Modified/new Kotlin files syntax checked; changed AppState declarations parsed separately.
  Screens tree-sitter diagnostics remain the same inherited parser limitation.
- DBZ exact-image bytes verified. No probing of all 2,662 streams; no claim all channels are online.
- Full Android Gradle compilation, actual QR networking/download replacement, TV UI/focus,
  logos loading and real SplitScreen/playback remain to be validated on the TV/CI.
- No APK built. No NEW SQL. RP93/RP88 scripts are retained solely for their earlier features;
  no rerun if already installed.
