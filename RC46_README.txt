BruniO RC46 · Dark TV visual update + quick reactions

INSTALL
Extract the ZIP and upload all included files to the matching paths in your existing GitHub repository, replacing files when asked. Upload the new SettingsLayout.kt, QuickReactions.kt and QuickReactionGestureGate.java files too. Both app/src/main/java and main/java source layouts are supplied and synchronized. Run your existing Build Native TV APK workflow and install its RC46 APK. There are fewer than 100 files. No SQL, Edge Function or Vercel changes are needed.

VISUAL DESIGN
Near-black backgrounds, charcoal panels, restrained violet focus outlines, consistent corners and margins. The horizontal top menu remains. The original TV logo appears in the header. Settings is grouped into Account, Playback, Language, Appearance, Sources and System, with contextual help and current values. Existing account, pairing, audio, subtitle style, autoplay, size limit, add-on, native source, cache, updates and usage controls remain. English remains the default on fresh installs; existing language preferences are respected.

NAVIGATION
Saveable navigation state is separated by account, profile and screen. Catalog grids preserve their scroll state when returning from details and reset when the catalog title changes. Short focus transitions and smaller scale changes keep the tile layout steady. Social screens use a solid dark surface instead of a full-screen bitmap/color-matrix effect.

PLATFORMS
Netflix, National Geographic, Disney+, HBO Max, Apple TV+, Prime Video, Hulu and Crunchyroll remain available. NatGeo keeps its existing vector wordmark; the other services have cover/focused-GIF URLs. Hulu and Crunchyroll include working catalog routing through their provider IDs. Animated covers activate after 160 ms of sustained focus, reducing decoder churn while moving quickly. Failed animations fall back to covers. Cover content fills the tile with cropping, as before. Existing regional availability/data source behavior remains.

LIVE TV
Guide/EPG buttons and now/next text are removed from the Live TV list and channel extras. Channels, sports, favorites, search and playback remain. The EPG GitHub workflow is now manual only; scheduled runs were removed.

QUICK PARTY REACTIONS
In the player, hold the Reactions button for approximately one second. A compact, focusable top strip appears with five requested reactions: laugh, fire, surprise, popcorn and coffin. Pick a reaction with OK. Selecting one or pressing Back closes the strip and returns player controls. Playback continues and the popup does not dim the full screen. A regular press still opens the normal reaction dialog. If not in a Party, the existing Party setup opens instead. The opening long-press release/repeats are swallowed so no emoji is sent accidentally.

VALIDATION
Passed a compiled Java test for opening gesture repeats/release, the next normal press, pointer-origin opening, arrow navigation and Back. Static checks verified Settings actions, category coverage, platform IDs/GIF URLs, Live TV guide removal, popup routing, paired source layouts and delimiters. Android APK compilation and real remote/focus/performance checks have not run here because this environment has no Android SDK or Gradle. Run the GitHub build before installing, then check Settings tabs, scrolling/back navigation, platform covers, Live TV favorites, and short/long Reactions presses in a Party. These checks do not establish measured FPS on your TV.
