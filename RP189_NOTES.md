# RP189 — Original page backgrounds and stable Daily rotation

TV 2.0.0-rp189 / Mobile 1.0.8-m9-rp189 — versionCode 22328.

Home, My List and Details use their original dark backgrounds, including artwork fades. My List and Details also use the original palette, rounding, surfaces and button styling. The shared runtime reads the current route to mask these style values without overwriting the stored theme. Leaving either route restores the selected theme. Home only excludes the theme background; other tabs retain their selected styling.

Daily rotation moves to the fifth content shelf when four other shelves are available (the mobile hero/filter/Now playing status are not counted). Shorter Homes place it last. TV ordering includes saved rows and available shelves; Continue Watching remains first.

The row reserves its artwork height while loading and offers a focusable TV loading target. When the first verified card arrives, focus transfers only if the loading target still owned it. Opening a card no longer clears or restarts the shelf. Verified partial results and checked candidates survive lazy-item cancellation in an account/settings/date-scoped bounded in-memory cache. Resume reuses them, including cancellation after the eighth card. Retry explicitly clears the selection. Cache lifetime remains five minutes, capacity four keys, eight cards and fourteen probes maximum.

Validation: release compilation, unit tests and vital lint for both modules. Physical remote navigation and device screenshots still require device testing; no signed APK is published by this source update.
