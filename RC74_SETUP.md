# RC74 — Try Spanish sources first, then English

Apply over RC73, preserve the paths, then build/install. No new SQL, Vercel deployment, secrets or add-on configuration. Keep Audio set to Español and your working Peerflix/Comet/Torrentio manifests.

Automatic independent movie/episode playback builds one provider-neutral attempt list from the links already loaded: up to three Spanish-labelled sources, then up to two English-labelled sources. Five total attempts maximum, including the first source. Each URL appears once; different URLs can still refer to the same underlying file. Existing size/year/playability filters still apply. Unknown-language and other-language sources remain in the manual selector but are not added as automatic retries.

An eligible source I/O, parser or decoder error advances to the next cached candidate. Initial native TorBox materialization failures use the same budget. If no first video frame arrives within 20 seconds while playback is requested, foreground independent playback can also advance. No new add-on search is performed per retry; native sources still need their normal TorBox materialization calls. Offline state, explicit manual selections, Live TV, Watch Party, AudioTrack hardware errors and DRM errors do not use this automatic chain. Watch Party and headphone recovery retain their existing behavior.

Mid-playback failures preserve the current media position, including a backward seek. Initial failures preserve the requested resume point. Pause/play intent and shared-moment boundaries survive the replacement. Same-content source changes retain the existing social-presence lease rather than sending an off/on pair, and skip the old player's exit cloud sync during the replacement. This adds no Supabase polling; normal playback/profile/account activity elsewhere remains.

The Spanish flag can coexist on the same line with a separately labelled subtitle section (`🇪🇸 … | Subtitles: English`); the audio prefix is now retained instead of dropping the whole line. Subtitle-only Spanish labels still do not count as Spanish audio. This is a reproduced parser edge case, not confirmation of the user's specific unprovided examples. Availability and correct audio labels still depend on each provider.

After exhaustion the usual error/Another link controls remain available. English will not be tried if no English-labelled result survived the configured filters. This patch does not override a size cap, download uncached content, add providers or guarantee availability.

Validation: executable Java retry-budget/error-category and language-label tests; source-wiring/regression and Kotlin parser baseline checks. No full APK compilation, real Peerflix protected-stream test or physical TV playback test was available. Test a known broken Spanish source followed by a working Spanish/English source on the target device.
