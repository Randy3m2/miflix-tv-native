BruniO TV RC94 — Trailers, audio & panel navigation
Base RC93. VersionName 2.0.0-rc94 / versionCode 22093.

INSTALL
Replace the 33 included project files over RC93, preserving directories; build through GitHub as usual. No SQL, Edge Function or Vercel deployment.

TRAILER DESIGN
Smaller 38dp pills with larger 24sp accent arrows; no solid contrasting footer panel. Title at the top, four controls below: Previous / Next / Details / Watch Title. Native controls sit outside the YouTube viewport, preserving its content and required branding.
YouTube playback controls disabled through the supported controls=0 parameter, plus disablekb=1. This hides playback buttons/seek bar. The official embed may still show its own branding, pause/title overlays, ads or notices: these cannot all be removed with supported parameters. No cropping, stream extraction or injected modification of YouTube's UI.
Remote behavior preserved: Center pauses/resumes; Left/Right previous/next; Up Details; Down Watch Title; Back closes.

VARIED SELECTIONS
Each explicit mood selection, including clicking the same mood again, starts another selection run. Normal moods advance through pairs of TMDB pages; Halloween and Christmas use their paginated themed catalogs. Existing size/language/playback rules are unaffected.
Local per-profile history puts unseen picks first; previews record a title only when playback starts. The current preview queue is frozen while open, so a Home refresh cannot switch the active trailer unexpectedly.
No repeats are introduced from the available unseen pool; when that pool is exhausted, older entries become eligible again. This is a finite catalog, not an unlimited promise of unique content. Previous intentionally revisits an earlier trailer. History is bounded to 500 IDs per category/profile and adds no Supabase queries or polling.

LONG PRESS
Shared remote button handler changed from 1000 ms to 750 ms. Applies to the TV buttons/cards using tvPlaybackClick: title actions, episodes, seasons, reactions and long-press favorites. Release before the threshold keeps the short action; a long action does not also trigger the short one. Synopsis expansion remains a separate 3-second hover behavior.

AUDIO-COMPATIBLE SOURCE SELECTION
Automatic ranking now applies language priority first, then known compatibility with the TV's registered audio decoders, then the optional network/quality preference.
Recognized codec labels include AAC, Dolby Digital / Plus, TrueHD, DTS / DTS-HD, FLAC, Opus, Vorbis and MP3 when supplied in parsed audio metadata. Unknown codec labels remain eligible as unknown. A dual-codec label is compatible when one advertised codec has a matching decoder; the link metadata cannot establish which language belongs to which audio track.
After opening the actual container, Media3 track support is checked. If no audio track is supported, try the next cached source. Spanish-labelled sources are rejected if every supported track has an explicit language and none is Spanish; existing false-English checks remain. Audio-track initialization/write failures also enter the existing bounded automatic recovery chain.
No video compatibility filter was added. Manual choices remain explicit. Audio preference and the existing Spanish-first / English fallback rules are retained. The bandwidth toggle continues to control network-aware quality, not the basic language/audio ranking.
Decoder MIME labels alone cannot guarantee playback: channel count, codec profiles, passthrough, connected audio output and missing/misleading provider language metadata can differ. Actual track checks and retry are the second line of validation. Unknown track languages are not falsely treated as proof of the chosen language.

CINEMA HUB / SIDE PANELS
Side sheets now own an explicit scroll state and bring-into-view policy, rather than inheriting Home's scroll suppression. They keep focused items inside the panel with a margin. Moving focus back to the fixed Close/header area restores scroll position zero, making the top non-focusable banner visible again.
Movie-night preference/ballot transitions reset scroll and focus to the top on every phase/person change. The fixed title includes person/count during preferences and voting, so the new turn is visible even before scrolling.

VERIFICATION
PASS: JVM audio compatibility tests, including dual audio, unknown labels and DTS-HD versus core DTS distinction.
PASS: preview-clock/pause behavior, source/party/live/moments/discovery regressions, per-turn scroll reset and panel focus restoration wiring, 750ms handler and matching source mirrors.
PASS: syntax parsing of changed UI files.
Android SDK/Gradle compilation and physical TV visual/playback validation were not available here. Please confirm on the TV: small footer appearance, YouTube controls hidden, 0.75-second holds, audio selection, upward panel navigation and the next person's ballot appearing at the top.

Official references
https://developers.google.com/youtube/player_parameters
https://developer.android.com/reference/android/media/MediaCodecList
