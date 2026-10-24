BruniO RP91 — Aggressive video preparation
Version code 22195 / version name 2.0.0-rp91
Apply this single ZIP over RP86–RP90, preserving paths. Includes the prior RP88–RP90 updates.

SETTINGS
Settings > Playback > Prepare video in details.
Uses the existing per-profile RP90 switch and preserves its ON/OFF value. Off remains the default for profiles that never enabled it.
The description now states that video preparation consumes data even when the user does not watch.

WHAT CHANGED
- Starts after 1 second in normal-mode details.
- Searches sources, chooses one using current language/provider/size/adaptive settings, and materializes a native TorBox source if needed.
- A valid remembered source is preferred for a resume, matching existing behavior.
- Prepares one ExoPlayer with playWhenReady=false, no visible surface and volume zero. Does not advance playback time while browsing.
- Warm loading stops at a 5-second buffered-duration target or a 16 MiB allocator threshold. These are thresholds, not hard limits on HTTP segment size, total bandwidth or total decoder/process RAM.
- The same player, buffers and subtitle timing are transferred to the visible player after Play. The normal load-control behavior resumes after transfer.
- If Play happens during lookup/materialization, it waits for that existing preparation instead of starting duplicate preparation.
- Start-over, manual source selection, changed headers, a different episode, expired preparation or changed settings use the ordinary playback path when the warm player does not match.
- Preparation expires after 60 seconds, on error, when leaving details or changing identity/settings, when the app backgrounds, or when disabled. The expiry task is cancelled when releasing/transferring its player.
- Does not prepare alongside an active visible single-player lease. Easy/Kids, parties, Live TV, mini-player and upcoming titles stay excluded.

LIMITS
- No 98% readiness or instant-start promise. Servers, network speed, authorization, video format, decoder/surface behavior and device memory still affect startup.
- A failed preparation is silent. Normal Play/retries remain available.
- This is real video traffic to the selected provider/CDN. Native TorBox preparation may also use its API. Abandoning a title can waste those requests/bytes.
- No presence, Continue Watching, watched, Trakt, party event or Supabase write is generated merely by preparing the video. Existing normal Play behavior publishes activity after the user's action.
- No new SQL for this feature. The bundled RP88 party SQL is only for that earlier party feature, and need not be repeated if installed.

VALIDATION
27 pure Java buffer/expiry/resume/lease policy checks passed. Changed Kotlin parsed successfully, with Screens.kt retaining only its inherited parser diagnostic. Android/Media3 runtime compilation and TV playback have not been performed here. CI regression tests for the policies are included.
Media3 API signatures were checked against the official LoadControl and LoadControl.Parameters reference:
https://developer.android.com/reference/androidx/media3/exoplayer/LoadControl
https://developer.android.com/reference/androidx/media3/exoplayer/LoadControl.Parameters

TV CHECKS
- Enable, open a movie, read details for a few seconds, then Play. Verify no audio or progress/presence before pressing Play.
- Compare cold Play versus prepared Play using the same source/network; check embedded audio/subtitles and resume position.
- Press Play early, Back during preparation, choose another episode/source, Start over, change profiles/settings, background the app, and disable the switch.
- Test slow/error/expired links and 1080p/4K on a low-memory TV. Check that only the intended visible player remains and that repeated changes do not crash.
