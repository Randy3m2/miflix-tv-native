RC59 Kotlin visibility fix

Apply AFTER the RC59 update. Replace the two AppState.kt files preserving paths.
The playbackLoading property is now internal, matching PlaybackLoading.
The setter remains private and playback behavior is unchanged.
Version remains RC59 because the previous build did not compile.
Commit and rerun the GitHub build. No Supabase or Vercel changes required.
Source mirrors and visibility checked; no full Android compilation locally.
