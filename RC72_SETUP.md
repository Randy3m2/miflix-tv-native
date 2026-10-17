# RC72 — Spanish first across all configured add-ons

Apply these replacement files over RC71 and build/install. No new SQL, Vercel deployment, credentials or add-on reset. Keep your working configured Peerflix manifest plus your other add-ons.

Settings → Playback → Audio: choose Español. All configured add-ons are queried and their results combined before automatic selection. The preference is Spanish (Latino/Castilian/Spain) first, English second, then unlabelled results, then other labelled languages. Within a language tier the existing quality/size ordering remains. A labelled Spanish 720p link therefore beats a labelled English 4K link, regardless of provider order. Manual link selection remains under your control.

Audio preference is now passed into the add-on aggregator before URL deduplication, so a poorer-language duplicate cannot hide the Spanish-labelled result before final selection. Automatic playback explicitly uses the same provider-neutral ranking. Native sources, if enabled, remain part of the final merged ranking. Size/year/playability filters and the 3-hour link cache are preserved.

Recognizes additional LAT/España/Spain and Spanish regional tags, stream descriptions, singular audioLanguage metadata and top-level filename fallback. Explicit audio metadata overrides guesses from filenames. VOSE and subtitle-only Spanish labels do not count as Spanish audio. Media3 continues preferring Spanish then English tracks during playback.

The ranking uses the information supplied by each source; it cannot prove a file's actual audio before playback, and a mislabelled or broken link can still need manual replacement. No new provider is installed by this patch. Peerflix's configured results could not be probed here; your reported working configuration is retained, without collecting or copying its credentials.

Validation: executable audio-language cases and aggregation/selection source checks, matching source mirrors, existing Home/presence/party/player regression checks and Kotlin parser baseline. Full Android compilation and physical TV playback remain to be verified in your build/TV.
