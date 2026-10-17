# RC70 — Event-driven social presence

Apply these replacement files over RC69, keeping the same paths.

1. Run `supabase_rc70_event_presence.sql` in Supabase SQL Editor first. It is repeatable, preserves the existing friend RPC return types and grants, and creates no cron job. Do not rerun the old RC62 presence migration afterward.
2. Upload the patch files to GitHub and build/install RC70 normally. No Vercel redeploy or new secrets are needed. Web friends already read the same RPC; its expiry behavior is updated by this SQL.

Presence publishes on the first video frame, content changes, leaving/backgrounding the player, sign-out and playback completion. Repeated navigation does not resend unchanged presence. Refresh reads the last saved activity; it does not force the other TV to publish. Watch Party control, polling and heartbeat code is unchanged.

Failed sends keep the latest intent in local preferences. Network restoration, the next app event or Friends refresh retries pending work. A later stop replaces an unsent start. Sequence numbers make retries idempotent and prevent old events from restoring activity after a stop. Another device's stop cannot clear this device's active entry. Pending data is sent only with the matching account's credentials.

Crash fallback uses a fixed expiry captured at playback start: remaining duration + 30 minutes, capped at 12 hours. If duration is unknown: 90 minutes for an episode, 3 hours for a movie. Live channels: 4 hours. These are safety leases, not exact online detection: a crash/network outage can leave the last activity visible until expiry; very long pauses or live viewing beyond the limit may expire while the app is still open. No heartbeat means instant crash detection is unavailable. On the next launch the current state replaces stale activity. Friend reads ignore expired activity without deleting rows or running scheduled writes.

Older APKs remain compatible with a 7-minute lease but continue their existing periodic updates. Upgrade each TV to remove those writes. Other app features (history, profile sync, auth, Watch Party, etc.) can still issue requests; this patch removes periodic **presence** writes only.

Validation: executable PostgreSQL/PGlite tests cover migration replay, RPC compatibility, 2-hour visibility without heartbeat, changes/stops, stale-event replay, separate-device stop protection, expiry, legacy writers, friend privacy and party authorization. Source checks cover lifecycle/outbox wiring and mirrored files. Full Android compilation and physical Fire TV testing were unavailable. This patch does not change the AudioTrack error reported separately.
