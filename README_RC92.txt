BruniO TV RC92 — Mood & Dazzle me
Base: RC91. VersionName 2.0.0-rc92 / versionCode 22091.

INSTALL
Copy the included files over RC91 at the repository root, preserving directories.
Upload these changes to GitHub and run the normal Android build.
No SQL, Edge Function deployment or Vercel deployment is required.

CHANGES
1. Home: four mood modes — Quick watch (TMDB episode-runtime filter: 1–30 min), Marathon mode, Something meaningful and Background noise. The selected picks move ahead of the regular shelves. First use fetches metadata; cached selections are reused for 30 minutes.
2. For me: picks change with the local time, weekends, Halloween and Christmas. Uses existing seasonal catalogs when applicable; refreshed on foreground entry, with no periodic catalog polling.
3. Surprise me: chooses from the current mood/context selection and starts playback preparation directly. Prefers titles not completed or rated poorly, where possible. A series starts at S1E1; no random spoiler clip.
4. EXCLUDED as requested: no pseudo-live thematic TV channels.
5. Dazzle me: official YouTube trailers/teasers inside BruniO. Horizontal navigation only, not a vertical feed. Right/left on the focused video or Next/Previous; a horizontal swipe strip is available below the video. OK toggles play/pause. Up accesses Details, Watch title and Close. Previews advance after up to 60 seconds of playback or when they end; paused time is excluded. Only one player exists. Only the next title's trailer metadata is warmed.
6. Search accepts bilingual mood phrases alongside ordinary title search, e.g. “something to feel good after a hard day”, “un thriller con final inesperado”, “algo que mi mamá y yo podamos ver juntas”. This uses local intent/genre rules and existing profile preferences, not an LLM or a guarantee about a plot ending.
7. Changelog included in Notifications / Settings. RC91 scrolling and focus behavior retained.

NETWORK / COMPATIBILITY
Uses the existing TMDB credential to find trailer IDs and the official YouTube IFrame player to play them; no new YouTube Data API key is needed.
Mood and trailer discovery add TMDB/YouTube traffic, not social polling or new Supabase tables. Normal playback retains its existing activity/progress behavior; this does not promise zero total app logs.
YouTube can restrict embedding, autoplay, age/region availability or device support. Blocked videos show a Next trailer option. Missing trailers show Retry/Next. No stream extraction or protection bypass.
Kids Home remains separate; mood searches still pass through Kids filtering. Family mood uses the same conservative certification filter. Runtime limits depend on TMDB metadata and are not a guarantee for every episode of a series.
A new selection may need a short metadata load on first use. The finite preview selection contains up to 40 titles and closes at its end.

VERIFICATION
PASS: JVM bilingual intent/title-protection/time-boundary tests.
PASS: JavaScript preview clock, pause exclusion and single end-event tests.
PASS: Kotlin syntax checks for the new files/functions and source integration/regression checks (live renderer, party, source recovery, moments, Home layout and mirrors).
No Android SDK/Gradle build or physical TV playback test was available here. APK compilation, Fire TV WebView/YouTube compatibility and actual navigation smoothness still require device testing.

SMOKE TEST
- Change each mood, return from details and check row/focus restoration.
- Search the example phrases in English and Spanish; check Kids separately.
- Open Dazzle me, use Right/Left, pause for a while, then resume; check Next, Details and Watch title.
- Close / background the app: trailer audio must stop. Screensaver must stay off during previews.
- Confirm Live TV, Watch Party and RC91 held-remote navigation still work.

Official integration references:
https://developers.google.com/youtube/iframe_api_reference
https://developers.google.com/youtube/terms/required-minimum-functionality
https://developer.themoviedb.org/reference/discover-tv
