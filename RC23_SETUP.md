# BruniO TV RC23 — 2.0.0-rc23 / 22021

## Install

1. If RC22 calendar SQL already ran, run only SUPABASE_RC23_CALENDAR.sql in Supabase SQL Editor. Otherwise run RC22 then RC23. This repeatable migration preserves existing events and friend privacy. Previous Game events remain visible/deletable; creating new Game events is removed.
2. Upload these TV source files to the existing repository, preserving desktop and the PC workflow.
3. Start a NEW Build MiFlix Native TV APK run on the updated main commit. Keep the existing signing secrets. APK update URL remains unchanged.

## Changes and locations

- Details remembers the screen it was opened from. Back returns to My List when opened there, including after removing a favorite; other catalog origins are preserved too.
- Queue: each entry has Play, up/down and Remove. Play works from details or the player. It consumes only the chosen entry after resolving its stream and preserves the rest. Failed resolution keeps the entry. In Party only the host selects the next title; guests follow using their own source configuration.
- Party transitions: each playback request has a separate player composition/lifecycle. After network calls, canceled/stale loops stop before touching the previous player. Guest source failures are reported instead of escaping the player coroutine. Host publication/heartbeat requests are serialized. Concurrent queue starts are blocked. Detached focus targets do not crash playback-control timers.
- Audio menu: scroll through every detected track. Unsupported tracks remain visible with an explicit compatibility label; they cannot be selected. This does not add decoders or tracks absent from the source.
- Player → Party: friend avatars, online status, current title and episode; Refresh remains manual.
- Friends/Party → Calendar: a star marks dates containing visible events. Movie/series search results show poster cards in three columns with padding around focus outlines.
- Create Event: Movie, Series, Live TV, Live Sports. Browse live categories, search and Load more when the add-on supports pagination. Live events store a channel ID/type and provider kind, never a playback URL or private add-on token. View title resolves fresh links at viewing time. Provider availability can change; future sports listings depend on the add-on publishing them.
- Streaming → National Geographic: TMDB network catalogs and films associated with matching production companies. This is a catalog, not a subscription or entitlement to a stream.
- Collections → Series by Year and Documentaries by Year. Series use first-air year. Documentaries use documentary films and their release year. 50 results per year maximum, as with the movie catalog.

## Validation and limits

Kotlin syntax checked. Calendar migration tested twice locally against PostgreSQL-compatible PGlite, including upgrade from RC22, live/sports insertion, malformed content rejection, refusal of new Game events, preservation of existing Game events, accepted-friend invitations and guest/outsider isolation.

No Android SDK/Gradle compiler or physical TV is available in this environment. Full compilation runs in Actions. The reported Party crash was not reproduced on hardware and no crash stack trace was supplied; this release addresses concrete lifecycle/error/publication risks found in that path. Verify host and guest switching between a movie and episodes from different series, queue Play and automatic countdown, unavailable guest links, leave/rejoin and rapid double presses. Confirm all detected audio tracks scroll into view and calendar focus margins on the target TV.

PC is unchanged. Audio-to-phone remains pending.

TMDB references: https://developer.themoviedb.org/reference/discover-tv , https://developer.themoviedb.org/reference/discover-movie , https://www.themoviedb.org/network/43 .
