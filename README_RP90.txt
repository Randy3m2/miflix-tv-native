BruniO RP90 — Optional background source prefetch
Version code 22194 / version name 2.0.0-rp90
Apply this single ZIP over RP86–RP89, preserving paths. Includes RP88 and RP89.

ENABLE
Settings > Playback > Preload sources in details.
Off by default; independent per account/profile. Included in the existing RP83 settings backup when installed. No new SQL is needed for prefetch.

BEHAVIOR
- Normal mode only, for movies/series in details after 2 seconds. Excludes Easy, Kids, upcoming titles, parties, background app and mini-player.
- Movie: warms its source list. Series: the episode targeted by the primary Play/Continue button (resume below 96%; otherwise S1 E1).
- Uses the existing profile/language/add-on/size-limit/native-engine cache key and three-hour in-memory source-list cache.
- An early Play shares the in-progress lookup via the cache mutex, then reads the result instead of issuing an identical provider lookup again.
- Playing a different episode cancels the irrelevant warmup. Leaving details, changing profile/settings or disabling the feature cancels speculative work.
- Only successful lists are cached. Speculative errors do not show a playback error; Play may retry normally.
- Existing provider search logic stays unchanged. No stream materialization, source probing, media buffering, video download, progress write or presence publication occurs during prefetch.
- No new Supabase queries or periodic tasks for prefetch. Existing provider requests are moved earlier; opening titles without watching can increase requests to those providers.
- Links still need materialization, connection and a video buffer after Play. Instant playback cannot be guaranteed.

PRIOR PARTY SQL
supabase_rp88_party_return.sql is included for the existing Bring friend BACK feature. Run only if it was not already applied. Not required for this new prefetch toggle.

VALIDATION
24 Java source-target/cache/profile-backup checks passed. Modified Kotlin reviewed and parsed separately where inherited full-file parser limitations exist. No Android build/APK, live provider benchmark or real-TV test performed.

TV CHECKS
Enable for one profile; open a title, wait a few seconds and Play. Test Play before warmup finishes, Back during loading, another episode, language/size/add-on changes, app backgrounding, Easy mode and another profile with the toggle off. Compare start time using the same source/network.
