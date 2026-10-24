BruniO RP95 — NBA / MLB Live Stats
Version code 22199 / version name 2.0.0-rp95
Apply over RP86–RP94 preserving paths. Includes preceding updates. No APK generated.

USE
- Normal-mode live sports player: open its existing controls, select the bar-chart Stats icon.
- Available for live Sports provider events and sports channels recognized from their title/description
  (NBA, MLB, ESPN, Sports/Deportes, baseball/basketball, etc.). Generic non-sports channels are excluded.
- Never displayed for movies/series VOD or sports recaps/recordings marked as VOD.
- Supports NBA and MLB data only in this revision. Other sports require another data provider/parser.
- Tries to identify a unique game from BOTH teams in the stream title and the selected day's games.
  Ambiguous, generic or missing titles open the manual selector instead of guessing the first game.
- Manual NBA/MLB league tabs, previous/next date, daily games and Change game action.
- Video keeps playing on the left 64%; stats use the right 36%. Same PlayerView/player remains alive.
- Team selector A/B filters the same table locally. NBA: Players / Teams. MLB: Batting / Pitching / Teams.
- Select a player row to expand all supplied columns, including shooting/turnovers etc. for NBA.
- Close with the X, Back, the Stats × button on the video side, or a touch on the video.
  Left from X focuses the video-side button; returning to the main video restores the normal controls.

DESIGN / FOCUS
- Dark charcoal panel, team logos/scores, compact cards, white fill only on the focused action.
- Fixed title/X, one LazyColumn per mode, including score/headers/rows in the stats view.
- No competing nested vertical scrolls; top/bottom padding protects focused cards and outlines.
- Stable item keys and team IDs; switching a team/tab does not discard the focused selector.
- Panel intercepts no playback commands itself; while open, the normal channel-zapping shortcuts
  are bypassed so navigation through the table does not switch the channel accidentally.

NETWORK / PERFORMANCE
- Public ESPN NBA scoreboard/summary and MLB Stats API, queried directly by the TV via existing Http.
- No added Supabase RPCs, SQL, presence writes, telemetry, watch progress or social refreshes.
- ESPN endpoint is not a contracted API; availability/schema can change. Missing data is nonfatal.
- Only the open, foreground panel polls its selected game: 30 seconds live, 60 seconds pregame,
  stop after a confirmed final. On failure: bounded 60–240-second backoff, retaining last good data.
- Closing/backgrounding cancels the coroutine and prevents stale publication/further polling.
  An already executing blocking HTTP operation may take up to its timeout to finish/cancel.
- Changing team or tab makes no network request. Manual game selector fetches daily NBA/MLB lists,
  each cached for 30 seconds; no automatic game-list polling while selecting.
- Stats cache max 8 games, 30-second live TTL / 5-minute final TTL, shared in-flight work via mutex.
- MLB field selection excludes play-by-play/media payload. Real probe: 35,386 vs 842,707 bytes
  for the sampled completed game. Actual response sizes vary.
- Network, JSON parsing and automatic matching run off the UI thread. No decoder/player rebuild
  is caused by opening/closing/changing stats. No new dependency or native library.
- These stats follow live time and can lead a delayed broadcast; no video synchronization is claimed.

VALIDATION
- Compiled actual SportsStatsRP95 + existing LiveScoresRP11 in a standalone Kotlin/JVM harness
  using injected HTTP, kotlinx-coroutines 1.10.2, org.json and JUnit 4.13.2.
- 8 tests passed: VOD/recording exclusion, sports scope, unique two-team matching, NBA columns/team IDs,
  wrong-game rejection, MLB batting/pitching parsing, simultaneous-request cache/TTL,
  cancellation without cached publication, final cache and recovery after a fetch failure.
- Offline fixtures from actual provider responses included under app/src/test/resources only;
  they are not APK assets. Live MLB filtered-feed response separately verified.
- Kotlin syntax checks passed for new UI/data and modified PlayerControls/UpdateNotes. Screens
  diagnostics match the preceding file's known tree-sitter limitation.
- Full Android compilation/Gradle suite, TV visuals/remote focus/scroll, resizing on actual TV decoders
  have NOT been validated here. No zero-crash guarantee. Verify those before distributing.

SQL
No new SQL. Existing RP93 / RP88 SQL files are retained only for those earlier features;
there is no need to rerun either if already installed.
