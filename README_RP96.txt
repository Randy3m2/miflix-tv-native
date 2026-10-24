BruniO RP96 — Compact watched-friends UI and earlier video prewarm
Version code 22200 / version name 2.0.0-rp96
Apply over RP86–RP95, preserving paths. Includes previous updates. No APK generated.

WATCHED FRIENDS
- Replaces the fixed 232 x 64 dp control with a content-sized, 40 dp high pill, max width 264 dp.
- Idle background is 15%-opaque black, with no outer or avatar borders.
- Up to three overlapping 24 dp avatars, +N, compact label and a simple chevron.
- Only the focused pill fills white; the pill returns to its translucent state when its dialog opens.
- Compact, border-free dark dialog: nickname + 40 dp avatar, small friend subtitle, X in the header.
- Removed the wide bottom Close button. Back/outside dismissal also works.
- First person receives focus on open; closing explicitly restores focus to the original pill.
- Stable friend IDs, one vertically scrolling list with padding; header/X stay outside the list.
- Profile cards are read-only presentation, not invitations or additional social actions.
- No changes to the RP93 lookup, cache, sharing preference, query limits or SQL permissions.

PREWARM
- Begins 250 ms after eligible details metadata is ready (previously 1 second).
  The delay still gives the details screen a chance to appear before speculative work starts.
- Time threshold grows from 5 to 15 seconds. Normal warm allocation threshold: 32 MiB (was 16).
- Low-RAM devices, memoryClass <=128 MB, or unknown memory information retain a 16 MiB threshold.
- Loading stops when either time or allocation threshold is reached. These are soft Media3
  thresholds, not a strict process-memory/network ceiling; segment/allocator overshoot is possible.
- Same paused, silent prepared player is handed to Play. It does not run the movie clock forward,
  change progress, publish watching presence or create an extra playback instance.
- Off-Main setup, serialized construction, Main-only player APIs, source ordering/language/size
  preferences, cancellation and max-five silent fallback attempts remain unchanged.
- Existing per-profile Prepare video in details switch is retained, off by default;
  Autoplay next link still governs whether another warm source is attempted.
- One-minute expiry unchanged. No Live TV, Easy/Kids mode or Watch Party speculative playback.
- More buffering consumes more provider/CDN data. Provider/metadata latency still applies;
  instant playback or a 98%-ready state is not guaranteed.

VALIDATION
- Actual WarmVideoRP91 and WarmConstructionRP92 JUnit suites compiled/run standalone:
  8 tests passed with assertions and coroutine debugging enabled.
- Covers the new 15-second/32-MiB and low-RAM 16-MiB thresholds, earlier delay, handoff freshness,
  owner isolation, responsive construction and cancellation/cleanup.
- New UI, load-control and update-notes Kotlin syntax parsed; changed AppState member parsed
  separately. Screens parser diagnostics unchanged from RP95.
- Source/social cache and off-Main construction engine byte-compared unchanged.
- Full Android/Compose compilation, TV layout/remote focus and actual playback startup timing
  have not been tested here. Native decoder/driver stalls cannot be ruled out without TV testing.

SQL
No NEW SQL. Older RP93/RP88 scripts remain bundled for their previous features;
no rerun if already installed.
