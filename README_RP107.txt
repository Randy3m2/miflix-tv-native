BruniO RP107 / 22211 / 2.0.0-rp107
Apply 6 changed files over RP106 preserving paths. RP102 episode aggregation is required.

Confirmed code issue: RP102 presented a complete episode list but queried the original
season/episode first. Absolute numbering was tried only after an empty source result;
nonempty wrong results could therefore prevent it entirely.

FIX: Dragon Ball Z (series TMDB12971) always maps canonical episode coordinates into
season 1 / absolute episode 1-291 BEFORE any source lookup. No saga-season fallback for
DBZ. For example, in the nine-season test fixture S2:E1 -> S1:E40 and S9:E38 -> S1:E291.
Actual mappings use the complete episode metadata, not hardcoded season-length offsets.
The mapped values reach the existing easy-mode add-on path, normal add-on path, optional
native engine, prewarm, manual source selector and retries through the shared resolver.
Subtitle lookup uses the same provider coordinates. The UI/progress keys remain canonical
to retain existing watched marks and resume positions. Existing S1:absolute coordinates
can be passed through without applying another offset when not present as canonical rows.

DBZ remembered-source keys have a new namespace so URLs saved by the previous rule are
not reused. No profile/history deletion. No change to Naruto or Shippuden policy, which
continues using its installed RP102/RP103 behavior; no global policy file is overwritten.
This fixes request numbering; it cannot create links absent from an add-on or repair an
add-on that mislabels its returned content. No live provider availability claims.

Validation: 7 standalone Kotlin/JVM JUnit tests cover every one of the 291 coordinates,
season boundaries, final episode, scoped matching and invalid data; 3 further checks
compile actual AppState routing methods with fake transport/data adapters, verifying
S1:absolute is the FIRST and ONLY DBZ lookup even if a canonical lookup would return data.
Other series/movies retain their original coordinates. Five modified AppState members
passed syntax checks. Full Android build and playback in TV remain untested.
No APK generated. No new SQL or Supabase calls.
