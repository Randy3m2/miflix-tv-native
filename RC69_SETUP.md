# BruniO TV RC69 — Sports → LIDOM

Apply over RC68, preserving paths. This ZIP also carries the TV files containing RC66–68 changes, so those features remain present. Build as usual in GitHub. Version 2.0.0-rc69 / 22068. No SQL, Vercel, Edge Function, new key or relay server is required. The web app is unchanged.

## Use

1. Live TV → Live Sports → ⚾ LIDOM 2026–27.
2. Select one of six teams, or select a published game. Games open the home team's source selector; either club can also be opened independently from its team card.
3. YouTube rows show recent videos when a verified channel ID is configured and its public feed responds. You can also paste an official video URL or open the official channel/page. Licey, Águilas and the Escogido team channel have verified IDs. Abriendo Sports and unconfirmed team YouTube options accept a user-selected video; this release does not discover all six live games automatically.
4. TV rows resolve compatible sources from existing channel catalogs, then show the native source choices. Choose a stream to play in the same Media3 player. HLS/DASH custom URLs use their explicit media type.
5. If playback fails, Try another source returns to the same team's selector. A technical error is not treated as a working source. YouTube embedding errors expose an external YouTube option and another-source action. Nothing bypasses provider restrictions.
6. Add/remove a team favorite from its card; a long press on Choose source also toggles it. The last source that actually started playback is marked Last working. The last successful YouTube video is retained for that team/source.

## Configure

Use LIDOM → Source settings, or Settings → Sources → LIDOM · Source settings.

- Choose the team, then Add source, Edit or Delete.
- HLS/DASH: enter the actual authorized media URL, not an ordinary webpage. Select the correct type. Query strings and original HTTP/HTTPS schemes are preserved.
- YouTube: enter a video or channel URL. A channel URL with a /channel/UC... ID can load recent public feed entries; handle URLs can be opened normally and accept a pasted video URL.
- TV is a named lookup for the preconfigured station; changing it into a direct URL requires choosing HLS, DASH or YouTube.
- The associations are documented in LIDOM_SOURCES_RC69.md. A reference is not a guarantee of this season's coverage, territory, or a live game broadcast.

Favorites, custom URLs and last-source preferences are local to the account/profile on this TV. They are not synced through Supabase. Do not put private stream URLs into the bundled JSON or GitHub; enter them in the TV's settings.

## Data and performance

Schedule requests go straight to MLB's public league 131 / sport 17 endpoint when the module opens, the date changes or Refresh games is pressed. Dates/times use America/Santo_Domingo. No added periodic polling. Failure to load games leaves all six teams available. Logos are bundled, so they do not need downloads at runtime. Source lookup is user-triggered and reuses the existing IPTV cache. Video traffic goes to the chosen provider/YouTube, not Vercel or Supabase; existing app presence/profile behavior remains.

## Verification

- Compiled and ran the Java URL/station policy tests: exact YouTube host validation, supported URL forms, Unicode handles, scheme/credential/control-character rejection, signed HTTP URL preservation and station-name isolation.
- Executed the official-player wrapper against a mocked IFrame API: ready versus playing, success reporting, error 153, autoplay blocked, pause and bounded seek.
- Checked six-team/source/logo mappings, real schedule response fixture, AST date mapping, success-only source memory, owner scoping, native MIME/retry routes and mirrored code.
- Prior party, Return to player, Home double-Back, friend panel, season watched and native channel-renderer regression checks passed.
- Parsed new Kotlin files and integration blocks. No Android build or physical TV/WebView playback test was performed here.

After building, test one team’s YouTube feed/video, a resolved TV station, your own authorized HLS/DASH URLs, a blocked video, another-source recovery, favorites and profile isolation. Availability of every live game is not guaranteed.
