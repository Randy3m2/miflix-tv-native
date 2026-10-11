# RC26 — Playback recovery — 2.0.0-rc26 / 22024

Upload the cumulative project and start a new TV build. No new SQL. Existing audio preferences, RC24 audio selection and RC25 Friends navigation are retained.

## Prevention

Starting a new source lookup or playback resolution cancels the previous resolution coroutine, in addition to invalidating late results. Unfinished subtitle work is canceled when superseded. HTTP connections disconnect on cancellation and in finally, including when reading/writing throws. Source searches have a 25-second overall timeout and busy state clears in finally. Existing account/profile/generation checks remain in place.

## Automatic recovery

A monotonic-clock monitor observes playback once per second. If playback should be advancing but position does not change for 60 seconds during buffering, or 30 seconds otherwise, the player is recreated with a new playback identity and the same source and position. Live streams reconnect at their live position. Intentional pauses, ended playback and playback suppression reset detection. A position change, including seeking, resets the timer.

Two automatic reconnections are allowed per request chain. After that, the error dialog offers Restart player (a fresh player, not only prepare on the previous instance), Another link and Exit. Manual restart resets that retry allowance. Party remains joined, guests preserve their own source and continue following host playback state. The queue is preserved.

The monitor detects a stalled playback clock, not every frozen decoded image. If audio/time continue while only video freezes, detection cannot reliably infer a freeze from the clock. Bad/offline providers or unsupported codecs may still require another link.

## Validation

Kotlin syntax and ZIP/version metadata checked locally. Added JUnit regression tests for buffering thresholds, paused/suppressed playback and progress/seek resets. The workflow runs these tests before release assembly. This environment has only a Java runtime, no javac, Kotlin compiler or Android SDK, so compilation and those JUnit tests remain pending in Actions.

Physical TV validation required: rapidly request multiple episodes/links and confirm the latest wins; cancel/leave Party during resolution; test a stalled source, pause longer than 60 seconds, resume, and repeated recovery. No crash/freeze stack trace was available, so this fixes identifiable work/cleanup weaknesses and adds recovery but does not prove every reported freeze is resolved.
