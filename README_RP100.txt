BruniO RP100 — Progressive channel picker
Version code 22204 / version name 2.0.0-rp100
Apply over RP99 preserving paths. Includes preceding source updates.

Keeps the entire channel catalog, categories, favorites, artwork and player behavior.
Prepares the profile's category index off the UI thread; caches only the latest owner,
channel-edit revision and final-playlist revision. Category changes reuse prepared rows
instead of hashing every channel's category again. Search normalization is prepared once
for the local catalog; filtering and favorites reads run off the UI thread.
Local category focus settles for 90 ms so rapid remote repeats cancel obsolete work.

The channel list publishes 20 entries initially, then 40 additional entries per 100 ms.
LazyColumn composes visible rows rather than every published channel. Full metadata is
kept for next/previous navigation. Restoring the currently playing channel may expose
slots through its index immediately, while LazyColumn still composes only visible rows.
Logos use 96x96 decode requests through the existing Coil cache, only for composed rows.
Large images are not decoded at their full original resolution for the 40dp logo slot.
Existing local text fallback remains if a logo fails.

Validation: extracted actual cache implementation compiled using Kotlin/JVM with Android
and data adapters; 7 checks passed: complete 2660-row fixture, group counts, 1000 cached
visits with no new reads/matching, profile isolation, edit revision and catalog revision
invalidation. Kotlin UI syntax parsed. Full Android/Compose build and on-TV performance
remain unverified; no APK generated. No new SQL or periodic network requests.
