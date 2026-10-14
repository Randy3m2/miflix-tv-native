BruniO TV — RC51 · Compact Navigation + Notify Me
Version 2.0.0-rc51 / code 22049

INSTALL
Extract this ZIP and upload the contents to the existing repository ROOT, replacing matching files in the same paths. Keep other files. Run the existing GitHub Actions APK build and install its output.
Cumulative visual patch over RC45 or newer: includes RC46–RC50, landscape asset, seasonal programming, inline calendar, Live TV favorites, quick reactions and WHITE player timeline. No Supabase SQL, secrets, Vercel or Edge Function changes required.

CHANGES
- Top navigation: Home, search icon, Friends & Party, Live TV, notifications bell. Profile dropdown: My List, Settings, Switch profile.
- Profile and bell backgrounds remain black; focus uses an accent outline.
- All collections shortcut appears in Home's Browse by genre area.
- Marvel/DC franchise tiles appear immediately after For You (the sixth section when Continue Watching and For You are present). All eight original streaming platforms remain.
- Home and Collections media rails use the same expanding card/synopsis behavior. Width transition is 150ms, focus settlement 60ms.
- Details opens immediately with the selected card's title, image and synopsis, with a 120ms graphics-layer entrance. No fixed 150ms wait before initial focus. This same Details screen is used by Home, Collections, My List and Search.
- Twelve recently opened detail records are cached for 15 minutes per TMDB language. Cast/runtime/episodes may still need network time. Old in-flight responses cannot overwrite a newly selected title.
- Coming Soon opens a title with Notify me. Selecting it ADDS to My List (repeated selection never removes it), syncs through the existing favorites path, and primes the existing notification baseline.
- Full TMDB release date is now retained separately from the existing display year. After the release date, a newly opened title shows the usual Play action. Date metadata does not verify playback-link availability.
- Existing notification checks show release notices in the TV app when checks run. This does not add movie email, push notifications or a new scheduled service. The deployed series episode email system continues to use favorites as before.

SEASONAL PROGRAMMING (RC50)
October: Halloween; Nov 20–Jan 6: Christmas; February: romance; Jun–Aug: adventure; other dates: discoveries. Theme follows the TV's local date. Catalog data is reused for the day/language and checked on Home entry/foreground return, with no Supabase request per focus movement.
Profile backdrop is a bundled 1600x900 WebP with slow graphics-layer movement, not a GIF/video. It stops outside the foreground. Generated using the built-in image tool: cinematic blue-hour alpine lake, mountains, forest, mist and violet/teal reflections; no people, logos or text.

VALIDATION / LIMITS
Static checks: source mirroring, nested delimiters, four navigation destinations plus bell, profile routes, Home ordering, Notify me idempotence, full-date handling and stale-details guard.
RC50 Java seasonal policy checks passed across 1,461 dates; previous gesture checks passed.
This environment has no Android SDK/Gradle. Kotlin/Android compilation, actual APK operation and frame pacing have NOT been verified. No APK is included. Fewer menu items alone do not guarantee Netflix-level performance.
Device checks: open/Back profile menu; My List and Settings via dropdown; All collections; Marvel/DC; Notify me twice -> one favorite; open title from Home/Collections/My List; rapidly open/leave titles; switch ES/EN; resume app.
