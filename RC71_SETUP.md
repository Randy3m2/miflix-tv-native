# RC71 — Familiar Home and broader Spanish searches

Apply these replacement files over RC70, preserving their paths, then build/install normally. No new SQL, secrets or Vercel deployment. Keep the RC70 event-presence migration already applied.

## Home

- New Romance, K-dramas and Timeless classics rows, fetched directly from TMDB. K-dramas use Korean original language plus Drama; that classification does not guarantee a Spanish dub.
- For you uses up to two recently watched titles with at least five minutes of progress, plus existing ratings. Disliked titles are not seeds, negatively rated recommendations retain their negative weight, and completed/explicitly rated titles are excluded from the suggestions. Profiles and interface languages have separate result guards.
- Discovery and recommendation results use a bounded in-memory 3-hour cache. Three catalog requests are made on a cold Home load; no periodic catalog polling or additional Supabase presence/profile reads are introduced. Each request still consumes the respective provider's normal quota, and image loading uses its CDN/cache.
- Settings → Appearance → Simple Home enables a local per-account/profile preference. Home has larger buttons/headings, readable titles on fixed cards, fewer secondary rows and Search / My List / Full view shortcuts. Normal Home remains available; streaming platforms, genres, Kids Home and other screens are retained. This is a Home presentation option, not a full-app accessibility overhaul.

## Spanish source search

- Playback → Audio: select Spanish. Spanish remains first and English stays available as fallback; nothing enforces Spanish-only playback.
- The broader keyword search requires the existing **BruniO Native engine enabled in Sources and a configured TorBox key**. Add-on manifests continue their existing ID-based resolution and Spanish-first ordering. This patch does not add a new provider, a Kodi runtime or a third-party scraper.
- Native search uses the original title and a Spanish translated title when TMDB has one, retaining the normal queries and adding at most two searches (`latino`, `castellano`). A translated-title lookup is cached locally. Searches remain bounded by the existing 25-second native resolution timeout, cached index pages and 3-hour playback-link cache.
- Spanish-labelled candidate releases are sorted before the 80-candidate verification cap. Eligible files are filtered by size before choosing the best file, so a larger file no longer hides a smaller eligible one in the same cached release.
- Language labels are evidence, not proof of a working Spanish track. MULTI/subtitles alone do not establish Spanish audio. Media3 continues preferring Spanish and then English. Availability depends on the configured sources and cached files; no coverage percentage or extra playable title count is promised.

## Kodi add-ons

Alfa/Balandro are candidates for a separate compatibility study, not functional providers included in this patch. Kodi Python/platform dependencies, each source's interface, code license, playback authorization and maintenance need checking before reusing components. Public source code is not a drop-in BruniO/Stremio manifest. Palantir/Black Ghost were not verified as reusable APIs. No Kodi add-on code or resolvers are shipped here.

Validation: executable Java search-budget/fallback tests and existing audio-language cases passed. Source regression checks passed for mirrored files, Home/profile/language guards, native filtering, TV channels, navigation, Watch Party, seasons and event-driven presence. Kotlin syntax was compared to the existing parser baseline. Full Android compilation and physical TV/UI testing were unavailable; the build and remote-control layout still need checking on the target TV.
