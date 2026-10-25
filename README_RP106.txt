BruniO RP106 / 22210 / 2.0.0-rp106
Apply the changed files over RP105, keeping paths. No APK generated.

Stats fixes based on the supplied screenshot and 4.56-second video:
- Replace per-row focus targets with one persistent keyboard-focus target for the table.
  Lazy row recycling no longer detaches the target referenced by fixed header buttons.
- Track the selected player by stable key, independent of ten-second value refreshes.
- Only scroll when the selected row reaches an edge; do not put every selected row at
  the top of the viewport. No competing requestFocus/scrollToItem jobs per arrow press.
- Up/Down changes the selected player; Up from the first returns to the fixed header.
  Left goes to the video-side close action; Right returns to teams. Header Down targets
  the persistent table, even when its first player has scrolled away.
- Stats actions use one focus target and consume both halves of remote OK gestures,
  avoiding duplicate focus/click handlers and a lingering pressed gesture.
- Keep RP105 immediate refresh / ten-second live polling and background cancellation.

Player controls:
- Live favorites lookup is moved off Main so opening controls does not synchronously
  rebuild/normalize the 2660-channel catalog for the heart button.
- Left/Right reveals controls on initial KeyDown and clears a lingering synopsis overlay.
  No timer/debounce was added to visibility. Device frame timing still needs TV testing.

Normal-app navigation:
- Center menu in equal 136dp left/right reserved zones, with a scrollable fallback if
  screen width is insufficient. Profile left; notifications and drawn settings gear right.
- Lightweight translucent glass gradients and rounded tabs; stronger focus/selected states.
  No backdrop blur or repeated image downloads. Existing profile-menu settings remain.
- Continue Watching is pinned first among visible Home rows when nonempty. Preserve
  the other rows' saved relative order and their visibility settings. Home-order settings
  mark Continue Watching Always first and allow the remaining rows to move.
- Easy mode retains its restricted navigation; Kids does not expose the settings gear.

Validation: 8 standalone Kotlin/JVM JUnit tests passed for minimal viewport movement,
100-row down/up navigation, jumps and Home order/deduplication. Updated Kotlin syntax
parsed; existing Screens parser diagnostic unchanged. Symmetric navigation zones,
settings action, retained RP104 batches of 10 and async favorite lookup checked.
No Android SDK/full Android build, emulator or physical TV is available: actual focus,
rendering, frame latency and remote behavior remain unverified. No new SQL/Supabase calls.
