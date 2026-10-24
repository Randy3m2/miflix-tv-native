BruniO RP92 — Responsive details and bounded warm-source retries
Version code 22196 / version name 2.0.0-rp92
Apply this single ZIP over RP86–RP91, preserving paths. Includes the preceding updates.

PERFORMANCE
- RP91 constructed ExoPlayer/renderers on Main during details prewarm. RP92 performs the builder/platform setup and construction on one low-priority worker.
- Main captures primitive/immutable preference values and the stored track-settings string. Large track-settings JSON is parsed by the warm worker, not on each details recomposition.
- Source add-on JSON parsing/filtering and source sorting/selection are dispatched off Main.
- Player operations, listeners, media setup and PlayerView remain on Main using the explicit application looper. Actual decoding remains Media3's job.
- A serialized construction pipeline cleans up a cancelled or obsolete completed constructor before allowing another warm construction. It never publishes a stale resource.
- System trace sections BruniO.WarmBuild and BruniO.WarmConfigure support local profiling. No new Supabase telemetry or periodic reporting.

SILENT RETRIES
- Uses the already obtained source list, language/provider/size priorities and a single forward pass of at most five unique candidates.
- Honors the existing Autoplay next link toggle: one attempt if disabled, up to five if enabled.
- Retries after source/materialization/decoder preparation errors while still on the matching details screen.
- Releases the previous warm resource before preparing the next; one warm player/constructor at a time.
- Failed warm URLs are excluded from the immediate automatic Play selection for that preparation. Manual selection remains possible.
- Leaving details, disabling the feature, changing profile/settings, opening an active player, or expiry cancels stale preparation/retries. Errors before Play do not display playback dialogs or update watching activity.
- If all warm candidates fail, normal Play/manual selection remain available. Foreground playback keeps its existing fallback behavior after handoff.

UNCHANGED
Settings > Playback > Prepare video in details remains per profile and off by default. Starts after 1 second, uses the same warm-player handoff, a five-second/16-MiB warm-buffer threshold and expiry. Video prewarm consumes provider/CDN data even when abandoned. No new SQL; the bundled RP88 SQL is only for its previously introduced party button.

VALIDATION / LIMITS
32 Java buffer/retry/lease policy checks passed. Modified Kotlin was parsed separately where inherited full-file parser limitations exist. Added CI coroutine tests for owner-loop responsiveness, cancellation cleanup, stale identity rejection and retry limits. Those coroutine/Android tests were not executed here.
No Android compilation/APK, frame-time benchmark or TV crash reproduction was performed. The UI-thread construction issue was identified from code; a device trace is still needed to confirm all contributors to the reported 1–4-second stall. Native decoder/driver stalls and release delays can still occur on some TVs; zero crashes or perfectly instantaneous startup cannot be guaranteed.

TV CHECKS
- Enable preparation; open details and keep scrolling/focusing/clicking while it loads.
- Use an invalid first source and valid subsequent sources; check silent pre-Play retries, then Play.
- Test Autoplay next link off, all sources failing, manual source choice, early Play, Back during construction, another title/profile, app backgrounding and disabling prewarm.
- Verify audio, subtitles, resume/start-over and that no presence/progress is recorded before Play.
- Repeatedly change titles/sources on the affected TV; check for remaining stalls and inspect the two local trace sections if needed.
