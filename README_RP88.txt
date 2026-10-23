BruniO RP88 — Unified update
Version code 22192 / version name 2.0.0-rp88

INSTALL
1. Apply this single ZIP over RP86 or RP87, keeping its paths. RP87 is already included.
2. Run supabase_rp88_party_return.sql once in the existing Supabase project before using Bring friend BACK.
   It extends an existing event validation constraint, preserves rows/policies/grants, and keeps RLS enabled.
3. Build through your existing workflow. No APK was generated here.

NORMAL MODE
- Modern Bring friend BACK to the party action in the host's Friends & Party panel.
- A member who remains in the party receives a targeted command to reopen/resynchronize with the host.
- A friend who left receives a normal invitation and must accept it to rejoin.
- Host and recipient need RP88 for the new synchronization command. Old pause/resume actions remain valid.
- Return commands require the real host, the intended recipient, and a fresh timestamp; they are not chat messages.
- Existing party polling intervals stay unchanged. The button writes one event or invitation; its recipient reads the current room once when accepting the command.

PLAYBACK STABILITY
- Repeated activations of the same playback target within 450 ms are ignored; distinct targets remain usable.
- New source lookups cancel older lookup child scopes, and metadata results are checked against the current request ticket.
- Single-player decoder handoff releases the previous player once before building its replacement.
- Late disposal/events from the old player cannot release the new one or drive the new playback state.
- Compressed buffering uses a 48 MiB target and respects the byte threshold instead of requiring a large time buffer first. This is not a cap on total app/decoder memory.
- Normal cleanup/progress synchronization is retained when leaving playback.
- Manual source selection remains first in its retry plan. No promise is made that every 1080p/4K source is supported by every TV.
- One bounded, local-only Java crash diagnostic is retained as app files/last_crash_rp88.txt and logged under BruniOPlayback. No messages/URLs/account tokens are included; no Supabase/network logging was added. Native crashes or OS process termination may not reach this handler.

EASY MODE
- Selecting a channel category moves focus to its first channel once that list has loaded. Late provider metadata keeps a user-activated Easy category and retains both provider catalogs.
- Hover preview in normal mode remains unchanged. Empty categories keep category navigation usable.
- Spanish is the subtitle default when available, separated per profile from normal-mode subtitle preferences and remembered series choices.
- Includes the RP86 launch import fix and all RP87 subtitle/backup changes.

VALIDATION
- 122 Java policy checks passed: decoder lease ordering/exactly-once cleanup, stale disposal, duplicate input boundaries, targeted host return validation, and subtitle mode separation.
- Changed Kotlin declarations and new files passed parser checks. Screens.kt retains its existing parser-only catch diagnostic; AppState's inherited seasonalMutex parser limitation was isolated and all 25 changed declarations parsed separately.
- JUnit regression tests are included for the normal Android CI workflow; they were not run through Gradle here.
- No Android SDK/compiler is installed here. Android compilation, APK execution and real-TV crash reproduction remain unverified. The original crash cause is not confirmed without the TV crash trace.

TV CHECKS
- Play a title, rapidly open/close menus and switch 720p/1080p repeatedly; ensure only the final request survives.
- Press Back and resume to verify progress and decoder cleanup.
- In Easy mode, activate a category with OK, then confirm focus reaches its first channel; check empty/loading categories too.
- Verify Spanish subtitles in Easy mode and that normal-mode English/off selections remain intact.
- With two RP88 devices in a party, test a locally paused guest, a guest browsing elsewhere, and a friend who already left.
