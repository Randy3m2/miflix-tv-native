BruniO TV RC93 — Cinema Experience
Base: RC92. VersionName 2.0.0-rc93 / versionCode 22092.

INSTALLATION
Copy the 46 included project files over RC92, preserving the folders. Commit/upload and run your regular GitHub Android build. No SQL, Edge Function or Vercel deployment is required.
These are native TV changes. No APK was compiled here.

TRAILER PLAYER
- Full-size fixed-position iframe, explicit YouTube viewport sizing/resizing, native match-parent video container and hardware WebView rendering. This targets the black-image/audio-playing report; physical Fire TV verification is still required.
- Compact four-action bar: Previous, Next, Details, Watch Title.
- Remote shortcuts act directly: Left = Previous; Right = Next; Up = Details; Down = Watch Title; Center/Enter/Play-Pause = Pause/Resume; Back = Close.
- No preliminary focus-selection step is needed for those shortcuts. Repeated held keys do not toggle pause repeatedly.
- Playback still uses the official YouTube player. Preview clock/lifecycle cleanup, next-only metadata warming and one active player are preserved. Provider/device embedding restrictions still apply.

VISUAL DESIGN / MOODS
- Uniform gradient mood cards, colored glyphs, selected indicators, theme-aware focus outlines and stable geometry.
- For me, Christmas, Feel good, A good cry, Documentaries, Marathon mode, Background noise, Plot twist, Cozy night, Date night, Adrenaline rush, Mind-bending, Family time, Nostalgia, Under 30 min, Something meaningful and Halloween.
- For me combines the loaded personal recommendations with time/season selections. Mood labels are discovery categories, not promises that metadata can establish a specific ending or emotional response.
- New Cinema Hub has matching banners, poster cards, metrics, bar charts and ranked itinerary cards.

HOME ORDER
Settings → Appearance → Home row order. Use Up/Down beside a row, or Reset order.
Order is saved per account/profile on this device. The fixed top navigation and hero remain fixed; the catalog shelves are reordered. Returning from details retains the existing keyed focus behavior.

BEST SOURCE SETTING
Settings → Playback → Select the best source, ON/OFF (default OFF).
Language priority and existing size/playability filters remain first. Within the same audio group, rank sources against Media3's estimated throughput with 35% headroom. Use file size and runtime when known; otherwise use a conservative resolution-based bitrate estimate. Over-budget candidates prefer lower estimated bitrate.
The same budget influences automatic fallback candidates. Explicit manual source choices are respected. No separate speed-test download or Supabase request is added. Initial/stale throughput estimates and unknown codecs can make this approximate; it does not transcode a source or adapt its bitrate in the middle of a fixed MP4 stream.

MOVIE NIGHT ROULETTE
Home → My cinema hub → Movie night roulette.
- 2–6 people, on the same TV/device, taking private turns.
- Each person cycles genres through Neutral / Like / Veto, then locks their preferences.
- Any veto is respected. Runtime caps: 90/120/150/180 min. Unknown runtimes are excluded.
- The active profile's watched titles can be excluded automatically; other people can mark a shortlisted title Seen / Exclude during their turn.
- Each person votes for any acceptable shortlist titles. Prior ballots are hidden from the next person. Highest vote count wins; ties are resolved randomly with an animation.
- Uses a bounded local candidate pool and manually requested, cached TMDB metadata; shortlist preparation has a 25-second budget. No server, remote multi-device voting, invitations or polling is introduced.

CINEMA LIFE REPORT
Home → My cinema hub → My cinema life, or Settings → System → My cinema hub / Statistics.
Press Generate / Refresh report. Month / Year / All filters operate locally.
- Playback-time counters, registered titles, favorite genres, recurrent directors when metadata provides them, current rating averages, best-rated decades and monthly viewing bars.
- Real viewing-time counters start in RC93 on this device. They are sampled during active playback; seeking does not add the seek distance. Historical watch time cannot be recovered accurately from old progress records. These are viewing counters, distinct from the older whole-app usage timer.
- Counters are buffered locally, flushed roughly once a minute and when leaving the player. A crash can lose the most recent unflushed slice.
- Retains 24 months, up to 200 title metadata entries per month; monthly time totals continue beyond that title cap. Clearing app data removes the local report. No media, screenshots, frames, Supabase storage or report polling.
- Rating comparisons with friends are manual/offline: show up to 30 of your ratings as text/QR, exchange them yourself, paste a friend's ratings JSON, then Compare now. No automatic reads of private friend ratings. This is not a live Friends integration.

TASTE TUNING / DESCRIPTIVE SEARCH
Movie/series Details → Tune my taste. Select what you liked and Find matches.
- Cast overlap for performances; cinematographer overlap for visuals; shared TMDB keywords as a proxy for plot, pacing and ending preferences.
- Preferences are stored per profile. The latest tuned title can influence For You, using bounded candidates and a 30-minute cache.
- Search example: “quiero algo como Shutter Island, pero con más acción” or “something like Shutter Island but with more action”. Results come from the seed title's recommendations, filtered for action and ranked by the available metadata. Descriptive searches have a longer debounce than ordinary title searches.
- This is a metadata-based recommendation engine, not an LLM. Shared keywords do not measure narrative pace, acting quality, visual beauty or prove an unexpected ending. It may return no verified match rather than invent one. Negative/unsupported modifiers are not represented as understood.

SAGA JOURNEYS
Home → My cinema hub → Saga journeys.
- Initial curated guides: Marvel Infinity Saga, Star Wars core live-action journey, DC DCEU and Game of Thrones / House of the Dragon.
- Release-year order or curated story order, poster cards, saved saga/mode preference per profile, watched/pending movie markers and season/episode checklists using existing progress.
- Series episode guides retain season/episode release order. Some post-credit notes are included, without exact scene timestamps or video jumps.
- These initial guides are not an exhaustive universal catalog: they do not include every current MCU/DC/Star Wars production, interleave individual Clone Wars episodes or claim one official chronology across alternate universes. DC's guide uses DCEU continuity. Later releases/branches need curated guide data.

AI X-RAY
Deferred by your choice in this chat. No scene-recognition engine, paid AI service, video-frame uploads or fake on-screen actor detection has been added.

CHECKS
PASS: JVM tests for bitrate estimates/headroom, descriptive ES/EN parsing and unsupported modifiers, shared runtime/veto/seen policies, tie selection, remote single-press/held-key behavior.
PASS: JavaScript preview clock, paused-time exclusion, single end event, source integration, privacy/local storage bounds, Home order/focus wiring, source/party/live/moments/discovery regressions and mirrored sources.
PASS: syntax parsing of new Kotlin files; unchanged baseline parser-error counts in the large legacy files.
No Android SDK/Gradle compile, physical TV visual QA or actual WebView playback test was available. APK compilation and the black-video fix must be confirmed on your TV. Source/syntax checks are not a substitute for that validation.

DEVICE SMOKE TEST
1. Play a trailer; confirm moving picture and sound. Center pauses/resumes exactly once. Test Left/Right/Up/Down and Back, then close/background: audio must stop.
2. Scroll all moods and verify long labels, spacing and selected state.
3. Change Home order, return to Home, open details and return. Check held navigation remains smooth.
4. Toggle best-source setting and verify chosen language still wins over network quality.
5. Run a 2-person roulette with a Horror veto and a 90-minute cap; confirm no excluded title wins.
6. Play a few minutes, generate the local report, and test the manual rating comparison with a small valid JSON.
7. Open each itinerary, change release/story order and inspect episode watched markers.
8. Test Tune my taste and the Shutter Island query; verify ordinary title search remains responsive.

REFERENCES
https://developers.google.com/youtube/iframe_api_reference
https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose
https://developer.themoviedb.org/reference/discover-movie
https://www.starwars.com/news/star-wars-movies-and-series-guide
https://www.marvel.com/articles/culture-lifestyle/mcu-the-marvel-cinematic-universe-an-official-timeline-available-now
https://www.netflix.com/tudum/articles/mind-bending-movies-tv-shows-on-netflix
