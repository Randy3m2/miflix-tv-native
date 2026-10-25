BruniO RP105 / versionCode 22209 / versionName 2.0.0-rp105
Apply these changed files over RP104 preserving all paths. No APK generated.

FIX: the player auto-hide effect still ran while Stats was open and requested video
surface focus after three seconds. Stats now participates in that effect's cancellation
keys and guard. The other automatic controls/synopsis/error focus paths also respect the
open Stats panel. Back, X and the existing button on the video side close the panel.
Only Live sports retain access; VOD and recordings stay excluded as before.

DESIGN: compact dark box-score table instead of expanding player cards. Fixed scoreboard,
team and tab selectors, shared metric headers, aligned values and 39dp constant-height
player rows. NBA defaults to PTS/REB/AST/BLK; MLB to AB/H/R/RBI or IP/H/ER/K.
More-columns button exposes the remaining data without changing individual row height.
NBA/MLB player headshots are requested only for visible rows through existing Coil cache,
with small decode sizes and initial fallback if an image fails. Player numbers appear
when provided. No On Court/Off Court status is invented from starter or minutes data.

REMOTE: Up/Down explicitly moves one table row, including past rows 4/5 and back to the
fixed team selector. Left returns focus to the video-side close button; Right returns
to the team selector. Refreshes retain row keys and never request focus or reset scroll.
Team/tab/column changes restart the table at the top. Only the focused action fills white.

LOAD: fixed panel frame renders immediately. Cached box scores display while refreshing.
Opening the panel requests fresh data immediately, while keeping its cached frame visible.
Live updates start every 10 seconds (30s before play), only while open/foreground; final
games stop. Failed requests back off 15-60s. Network/JSON processing stay off Main and
refresh never resets scroll/focus. No extra Supabase logs.
Auto matching can use the first matching league result without waiting for the other
league; manual browsing fetches the chosen league. Only the open-panel polling cadence is increased.
Stats-specific shared OkHttp connections with 3s connect/6s read/7s total timeout and a
2MiB response cap. Closing/backgrounding or changing game cancels the active socket.
Independent request-lock stripes replace one global long-held network lock; cache reads
remain short synchronized accesses. Existing 30s live/5min final cache retained for ordinary callers; the open panel explicitly
requests fresh data. No speculative requests when the panel is closed.

Validation: 24 standalone Kotlin/JVM JUnit tests passed with coroutine debug enabled:
existing NBA/MLB parsers/cache/privacy gate, ordered column mapping, stable row keys across
refresh, 100-row traversal down and back to header, partial/missing data, real local-socket
cancellation and errors, cached-result access while another request waits, and photos.
Production StatsHttp compiled against OkHttp4.12/coroutines1.10.2. New Kotlin syntax parsed;
Screens retains its previous tree-sitter diagnostic and the focus guards were checked.
No Android SDK is available here: full Android/Compose compilation and on-TV layout,
focus traversal and frame timing remain unverified. External API latency/availability
and headshots can vary. NBA/MLB player location on court is not inferred.
No new SQL or Supabase calls. Existing sports streams and channel catalog unchanged.
