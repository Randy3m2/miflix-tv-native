BruniO RP102 / versionCode 22206 / versionName 2.0.0-rp102
Apply over RP101 preserving paths. Only files changed by this update are included.

Android TV only: Dragon Ball Z (TMDB12971, 291 episodes) and original Naruto
(TMDB46260, 220 episodes) appear as one Complete series episode list.
Naruto Shippuden, Kai, movies, other series and Live TV channels are unchanged.

Episode cards use absolute episode numbers, with original names, synopsis and stills.
The existing canonical season/episode coordinates remain in progress, watched keys,
party synchronization, playback requests and subtitles. No migration or clearing of
existing profile progress is required. Next episode crosses original season boundaries.
Random play, Easy episode resume, episode info, queue/player restoration and batch watched
actions retain the same canonical identities while displaying the combined episode rail.

Metadata: all non-special TMDB seasons are fetched on IO with concurrency capped at 3,
validated for exact expected episode count, unique coordinates and contiguous per-season
numbering, then cached 6 hours per language (max 4 entries per repository).
Partial or invalid metadata fails explicitly and is not cached or padded with invented
episodes. First visit makes additional TMDB requests; no extra Supabase progress writes
or polling were added.

Sources: existing canonical lookup remains first. Only when it returns an empty list,
try S1:absoluteEpisode if different, using the same configured add-on/native settings.
Do not combine source lists for different numbering schemes. Provider errors and sources
that return nonempty but incorrectly labelled episodes cannot be repaired or verified
by this patch. Availability, playback language and correct provider content are not guaranteed.

Validation: 10 Kotlin/JVM JUnit tests using the actual episode model and numbering policy;
8 additional checks compiling the actual repository aggregation/cache methods with fake
transport adapters: bounded concurrency, full list, canonical mapping, cache/locale and
cancellation. Modified TmdbRepository/AppState members and UI syntax checked; Screens
retains its pre-existing tree-sitter diagnostic. Live TMDB/add-on calls, full Android
compilation and TV runtime not tested. No APK generated. No new SQL.

References checked:
https://developer.themoviedb.org/reference/tv-season-details
https://www.themoviedb.org/tv/12971/titles
https://www.themoviedb.org/tv/46260/watch
