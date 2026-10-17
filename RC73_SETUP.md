# RC73 — Shared moments (timestamps, no video recording)

Apply this patch over RC72. It retains the existing RC70 presence behavior, Home improvements and Spanish-first add-on ranking.

1. Run `supabase_rc73_shared_moments.sql` in Supabase SQL Editor first. It is repeatable and creates one small table plus a read RPC, with private access policies. No cron, bucket, video storage, Edge Function, secret or Realtime subscription.
2. Upload the replacement files using their original paths, then build/install RC73. No Vercel deployment or web changes.

## Mark and send from TV

While watching a seekable movie/episode, open the player controls and focus **Grabar momento / Mark a moment** in the upper actions. Choose **Mark start here**; the panel closes so you can continue watching or seek. Open it again and choose **Mark end here**. Select **Choose friend / Refresh friends**, then a friend to send the range. Both times remain local until you press Share. Drafts reset when playback changes. Nothing records the screen or uploads the video; only title ID, episode and millisecond timestamps are sent. Live channels are excluded.

Sharing uses accepted friends only. Duplicate/retried sends for the same draft and recipient are idempotent. Loading recipients performs explicit friend-directory requests; sharing inserts one small record. No source URL, TorBox key, account token or video bytes are shared.

## Receive and watch

Friends & Party → **Shared moments · manual refresh** → **Refresh moments**. Opening Friends or the inbox does not check for moments. No automatic refresh, badge polling, timer or background moment query is added. A manual refresh loads up to 100 latest received moments; Remove explicitly deletes one. The inbox is account-based and can be accessed from that account's profiles; Kids playback restrictions still apply.

**Play moment** resolves the recipient's own configured sources, uses the existing Spanish-first ranking and seeks to the marked start instead of the recipient's saved resume position. If already in a party, the button explicitly says **Leave party and play**; shared moments do not control or synchronize a party.

At the marked end, a local Media3 playback message pauses and seeks to that end, with **The moment ends here / Aquí termina el momento**. Options: Replay moment, Continue movie/episode, Choose another source, or Close while staying paused. A seek past the end also stops; Replay re-arms the boundary. Changing sources retains the range. Auto-next/queue does not advance while viewing only a moment. Moment-only viewing does not overwrite normal progress or mark the full title watched through Trakt; choosing Continue restores normal progress handling.

Exact timestamps are requested, but differing editions/introductions can shift the actual scene. If the selected file is shorter than the saved range, playback pauses with an incompatible-version notice so the recipient can choose another source. Actual pause/render precision depends on the player/device. No promise of cross-edition scene matching is made.

Validation: executable PostgreSQL/PGlite tests for repeatable migration, friend consent, private access, caller spoofing, idempotence, ranges/episodes, recipient removal and former-friend isolation; executable Java boundary-gate tests; source wiring/regression and Kotlin parser baseline checks. Full Android compilation, TV remote focus and real playback timing still need validation on your TV. Existing playback/auth/profile features may still make their normal requests; this feature adds no automatic inbox checks.
