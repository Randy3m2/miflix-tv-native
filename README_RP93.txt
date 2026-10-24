BruniO RP93 — Friends who watched
Version code 22197 / version name 2.0.0-rp93
Apply over RP86–RP92, preserving every path. Includes preceding RP92 changes.

INSTALL
1. Run supabase_rp93_watched_friends.sql ONCE in Supabase SQL Editor.
   Requires the existing RP1 profile-social schema. RLS is enabled by the script.
   No progress, friendships, catalogs or profile records are deleted/replaced.
2. Apply this source patch and compile through your normal CI workflow.
   supabase_rp88_party_return.sql is included only for that earlier feature; no rerun if already installed.

UI / MEANING
- Normal-mode Details: compact avatar badge in the hero's top-right corner, beside the back button row.
- Same ProfileAvatar renderer and round outline used for party members.
- Shows up to four avatars plus +N, with names on OK. Friends list supports TV focus/scroll.
- Movies: Already watched / Ya la vieron. Series: Watched episodes / Vieron episodios.
- A synced progress record at >=96%, including manual Mark as watched, qualifies.
- For series, at least one completed episode qualifies; this does NOT imply the entire series is finished.
- No badge if logged out, no matching friends, lookup failure, Kids or Easy mode.
- No presence publication, social refresh or focus steal when the badge arrives.

COST / PRIVACY
- Reuses the existing synced per-profile progress. NO extra completion writes, timer, polling or heartbeat.
- One bounded RPC per title/profile/TV on a cache miss; successful/empty results cached 15 minutes.
- Cache holds at most 64 title/profile entries, LRU eviction; failed lookup backs off for one minute.
- First social use may additionally require the existing profile-identity ensure/auth flow.
- Server returns up to 50 accepted friends' social IDs, nicknames and avatars, never their raw progress.
- Exact account/profile mapping and accepted friendship are enforced server-side.
- Settings > Appearance > Share what I watched: persisted server-side per profile; default enabled.
- Preference load is one RPC when the setting is composed; an actual toggle is one write RPC.
- Disabling prevents subsequent server queries from returning this avatar. Another TV's existing cached
  result can remain visible for up to 15 minutes. No forced realtime invalidation/polling.
- Requires existing progress cloud sync. Local-only or removed/overwritten history cannot be inferred.
- No new function that sends requests/messages to friends. No changes to Live TV, movies or series catalogs.

VALIDATION
- 15 Java behavioral checks passed for empty-result caching, account/profile/title isolation,
  15-minute expiry, failure backoff, clock rollback, LRU eviction and clearing.
- Kotlin syntax parsed for new files and changed AppState declarations. Screens parser diagnostics
  compared with RP92 and unchanged. SQL statements and PL/pgSQL body parsed successfully.
- Added Kotlin/JUnit cache regression tests for CI; not run locally.
- Android compilation, actual TV UI/focus and Supabase database execution have NOT been tested here.
- No APK generated. Confirm SQL installation, two accepted friends across profiles, hidden sharing,
  finished movie vs episode, badge appearance and navigation on TV before distributing.
