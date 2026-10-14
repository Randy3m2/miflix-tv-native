BruniO TV — RC50 · Cinematic Home
Version 2.0.0-rc50 / code 22048

INSTALL
1. Extract this ZIP and upload its contents to the ROOT of the existing GitHub repository. Replace matching files while preserving paths; do not upload the ZIP itself into app/.
2. Keep all other repository files. This is a cumulative visual hotfix over RC45 or newer, including RC46–RC49 and the WHITE player timeline.
3. Run the existing GitHub Actions APK workflow, then install the resulting APK on the TV.
4. No Supabase SQL, secrets, Edge Function deployment or Vercel changes are needed.

NEW
- Home media rails expand the focused poster into landscape artwork with a short 210ms transition. Metadata appears below; fixed row heights keep neighboring rows stable.
- Vertical focus brings the active rail into view. Expansion completes before the selected artwork is brought fully into horizontal view.
- Hero transitions pause while its buttons have focus.
- Home keeps this order: hero, Continue Watching when present, Streaming, Browse by genre, For You when present, then seasonal picks and the remaining catalogs. Coming Soon appears at the end.
- Search has a remote keyboard on the left and results on the right, including recent searches, recommendations before typing, backspace/space/clear, and an optional system keyboard via the text field. Existing flexible title matching remains.
- Search requests wait 450ms after typing; up to 12 query results are retained for 5 minutes per interface language.
- At startup and after sign-in, a profile picker appears. Rename/avatar/upload/delete/create actions remain under Manage profiles.
- Profile entry uses a bundled landscape image with slow graphics-layer motion, not a GIF or video. Animation stops when the app leaves the foreground and is disposed after leaving the picker.
- Original TV logo, eight streaming brands, Live TV favorites grid, inline Friends calendar, settings fixes, compact reactions and white playback timeline remain.

SEASONAL PICKS
Local device date determines the theme:
- October 1–31: Halloween / horror.
- November 20–January 6: Christmas.
- February: romance.
- June–August: adventure.
- Remaining dates: discoveries / drama.
Halloween and Christmas first resolve the exact TMDB subject keyword and discover movies with that keyword. Halloween can fall back to horror; Christmas is hidden if no matching thematic results exist rather than labeling unrelated family movies as Christmas.
A loaded list is reused for the local calendar day and language, and checked again when entering Home or returning to the foreground. The selected titles rotate their order daily. Failed seasonal requests wait at least five minutes before another foreground/entry attempt. There is no catalog polling or new Supabase request per focus movement.
This is calendar-based programming, not a prediction of an individual user's taste. Existing For You recommendations remain separate.

VALIDATION
- SeasonalProgrammingCheck compiled and ran with Java 17: seasonal boundaries, leap day, New Year and 1,461 dates passed.
- Existing QuickReactionGestureCheck passed.
- Checked source mirroring, nested delimiters, Home row order, eight platform entries and white timeline.
- Android/Kotlin compilation and actual TV frame pacing have NOT been verified here: this environment has no Android SDK or Gradle. GitHub Actions and device testing remain required. No APK is included.
Suggested device checks: cold start -> profile; rapid D-pad across/down rails; back from Details; Search typing/backspace/results; change ES/EN; background/resume; Live TV long press; party quick reactions.

BACKGROUND ASSET
app/src/main/res/drawable-nodpi/brunio_profile_landscape.webp
Generated with the built-in image tool, then resized/compressed for bundling (1600 x 900).
Prompt: Wide cinematic tranquil alpine lake at blue hour, distant jagged mountains, evergreen forest along shore, thin mist above still water, violet twilight and cool teal reflections. Horizon in upper third; dark uncluttered middle/left for profile labels. Photographic detail, restrained exposure, no people/buildings/text/logos/watermarks.

TMDB endpoint reference: https://developer.themoviedb.org/reference/discover-movie
