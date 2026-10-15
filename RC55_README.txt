BruniO TV RC55 / versionCode 22053

Down now explicitly enters the first card of the destination horizontal row rather than the card closest to the expanded artwork above. If that row was previously scrolled to the right, its horizontal position is reset to zero before focus is requested. Left/Right and Up retain their existing behavior. Applied to cinematic movie/series rails, streaming/franchise collection rails and horizontal genre rails in Home/Collections, including Kids cinematic rails.

Upload the contents to the same repository paths and run GitHub Actions. Cumulative over RC45 or newer; no Supabase change.
Source/mirror/delimiter checks passed. Android compilation and actual remote testing remain pending. Device check: expand card 1 or 4, Down -> next row card 1; scroll next row right, go Up then Down -> card 1 again; test held Left/Right and Kids.

Earlier update details:
BruniO TV — RC54 · Kids + Remote Navigation Polish
Version 2.0.0-rc54 / versionCode 22052

INSTALL
Upload the contents to the existing GitHub repository ROOT, replacing matching paths, then build with the existing GitHub Actions workflow. Keep other files. Cumulative visual hotfix over RC45 or newer, including RC46–RC53. No new Supabase migration, secret, Edge Function or Vercel change is required.

DEFAULT KIDS PROFILE
A default Kids profile is added to offline profiles and signed-in account profile JSON. Its ID and isKids flag preserve its type even after renaming or when an older client omits the new flag. The default Kids profile cannot be deleted from the TV UI.
Kids has separate Movies, Series, Continue Watching, Search, My List and release notices. Existing profile-scoped favorites/progress synchronization is retained.
The normal adult Home, broad platform/genre/franchise collections, Live TV, Watch Party, Settings, public comments and external trailers are hidden in Kids. Opening titles and starting playback both verify Kids eligibility. Entering Kids releases any old player/source picker and detaches local party state; old live/party entry paths are blocked.
Switch profile remains available. This is a filtered profile, NOT a PIN-locked parental control. An adult can switch to the normal profile to access full content/settings.

CONTENT POLICY
Uses TMDB US certifications plus a family/animation/kids genre:
- Movies: G or PG only, with animation or family genre.
- Series: TV-Y, TV-Y7, TV-Y7-FV or TV-G, with a kids/family/animation genre.
- Adult flags, mature classifications, missing classifications, conflicting US classifications or unavailable verification are excluded.
PG and TV-Y7 content can still require parental judgment; catalog metadata is not a human review of each episode or a verification of the actual third-party playback file.
The policy applies to Home, search results, My List, Continue Watching, notifications, title opening and playback. Unknown titles are hidden rather than admitted by genre alone.
Initial Kids loading makes TMDB discovery/rating requests (up to four rating checks concurrently); it may take longer the first time. Checks are cached for 24 hours in bounded memory; Kids Home lists are reused for six hours per profile/language. Loading does not block the profile picker. There is no extra Supabase request for each navigation movement.
Sources: https://developer.themoviedb.org/reference/movie-release-dates and https://developer.themoviedb.org/reference/tv-series-content-ratings

HORIZONTAL NAVIGATION
- Focus border responds immediately.
- Artwork expansion waits for 110ms without another selected item, avoiding repeated width changes while holding Left/Right.
- Row scrolling uses its own minimal visibility calculation instead of inheriting Home's vertical row anchor. Cards no longer force themselves to the left edge after expansion.
- Held horizontal key repeats are coalesced to at most one accepted repeat per 90ms. Fresh taps, release, reversing direction, vertical movement and OK remain immediate.
- Applied to cinematic media rails and collection/genre rails; player seek controls are unchanged.

VALIDATION
Java 17 checks passed for G/PG/TV kids allow rules, adult/mature/unrated/conflicting rejection, a 101-event held-key burst reduced to 12 moves, release/fresh press/reversal, vertical/OK and focus reset. Existing quick-reaction gesture checks passed.
Checked profile JSON persistence, mirrors, source delimiters, Kids entry/play/search/list guards, independent row scrolling and ZIP integrity.
No Android SDK/Gradle or TV runtime is available here. Kotlin/Android compilation, real key-repeat behavior on the TV and frame pacing are still unverified. No APK is included; no measured Netflix-equivalent performance is claimed.
Device checks: account/offline startup has Kids; Kids Movies/Series; search a mature title (excluded); My List; restart/rename Kids; switch back to adult; hold Left/Right and reverse midway in Home/Collections; quick separate taps; opening/Back; player seek and party reactions.

Earlier changes, 65 character avatars and source credits remain in the included sources/assets and PROFILE_AVATAR_SOURCES.json.
