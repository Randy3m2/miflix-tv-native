BruniO TV — RC53 · Profile Character Gallery
Version 2.0.0-rc53 / code 22051

INSTALL
Upload this ZIP's contents to the existing GitHub repository ROOT, replacing the same paths. Keep other files. This is cumulative over RC45 or newer, including RC46–RC52. Run the existing APK build. No new Supabase SQL, keys, Edge Functions or Vercel changes.

GALLERY
65 internet-sourced portraits: five in each of Game of Thrones, Lanterns, Justice League/Snyder Cut, Avengers, The Last of Us, Gran Turismo, WWE, God of War, Ghost of Tsushima, Red Dead Redemption 2, DCU, Power and Death Stranding.
Gran Turismo uses people from the film (with an actor portrait for Matty Davis), not car icons. The gallery mixes promotional stills, game portraits and some fan artwork; these are not an official uniform illustration pack.
BruniO keeps the existing avatars. Custom uses the existing phone QR upload, shows the current uploaded photo, and offers Remove picture. For a new profile, create the profile before using Custom upload.
Open profile dropdown -> Edit picture -> category -> Choose (or Right) -> select an avatar. Also accessible from Manage profiles -> Edit picture.
Existing Hide/Restore actions remain. New profile creation can choose pack avatars as well.

STORAGE / PERFORMANCE
One five-portrait WebP atlas per category: 13 images, about 0.8 MB combined. Atlas cells are 256x256. Images are bundled and render offline, with a small shared decode cache; browsing the gallery makes no Supabase or external image request. Saving a selection uses existing profile sync. Custom QR uploads retain their existing network/storage behavior.
Stable keys pack:<category>:<index> are saved to the profile and social avatar field. Other TVs need RC53 or newer to render these new keys; older apps display their existing fallback avatar.

SOURCES
PROFILE_AVATAR_SOURCES.json records each original image URL, source page, saved key and crop. Copyright remains with the respective owners. Alpha Coders explicitly labels its images for private/personal use; other promotional/fan images are not supplied with a commercial license. Review source permissions before public/commercial redistribution.

VALIDATION
Verified 13 categories x 5 unique keys, image decoding and 1280x256 atlas dimensions, all tile bounds, mirrored resources/sources, nested source delimiters, avatar whitelist and edit/custom routes. Visually inspected all 65 final crops; replaced an expired source returning a logo.
Android/Kotlin compilation and TV interaction testing remain pending: this environment has no Android SDK/Gradle. No APK is included.
Device checks: profile dropdown Edit picture; navigate all categories and select each pack; restart; another updated TV/friend card; Custom QR; BruniO Hide/Restore; create profile with a character avatar; errors still close after five seconds.

Previous release details:
BruniO TV RC52 / versionCode 22050

Errors: global errors now use a native dialog with initial focus on Dismiss, above the player and existing dialogs. Each message closes after five seconds; changed messages restart the timer. Closing does not navigate out of the current screen. Player/link errors keep Retry, Another link and Exit, focus Retry automatically, and close after five seconds without exiting playback. Dismissing an error does not resolve or automatically retry the failed action.

Cumulative hotfix: upload all files to the same repository paths and run GitHub Actions. No Supabase changes. Mirroring/delimiter checks passed; Android compilation and TV testing remain pending.

Previous changes:
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
