# RC75 — Player menus on the right + friend avatars when sharing moments

Apply this patch over RC74 using the original paths, then build/install. No new SQL, Vercel deployment, permissions, secrets or add-on changes. Keep the RC73 moments migration already applied.

Player menus now use a shared dark right-side drawer: audio/subtitles, subtitle timing, playback info, episodes/actions, queue, chat/QR/phrases, moment composition/end notice, source details, errors/confirmations and end-of-playback options. Multiview action/source menus also follow this style. Friends & Party retains the existing right-side sheet shown in the reference, with a matching width policy. Compact quick reactions and the skip-intro prompt remain their existing lightweight overlays.

Playback links use the right panel even before playback starts. The header is stacked so Refresh/Close remain accessible; the links list scrolls inside the available height. Technical source badges, language ordering, manual refresh, caching and source selection behavior are preserved. Episode/action rows wrap, subtitle timing and ratings scroll as needed, and end-screen recommendations have an explicitly bounded horizontal rail inside a vertical scroll area.

The new drawer is 52% of the available width, bounded to 360–560 dp and never wider than its viewport, with 14 dp margins, rounded corners, no whole-screen dim/blur and a 160 ms translation-only entrance. Existing Back/dismiss/security policies are passed through the dialog wrapper. Context is scoped to player/multiview (including the mini-player's own subtree); shared dialog functions outside playback keep their existing centered presentation. No video/player lifecycle or request logic is changed by the drawer.

Moment recipients now show a round 48 dp ProfileAvatar, nickname, Share moment label, focus outline and arrow. They reuse the avatar field already returned by the existing explicit friend query. No new friend/presence query or automatic inbox polling is introduced. Image loading still uses the app's normal avatar renderer/cache. Unknown avatars use its usual fallback rather than an invented online status.

Validation: source-wiring checks, mirrored-file checks, Kotlin parser baseline and existing playback/presence/party/moment/source-fallback regressions. Full Android compilation, actual right-panel rendering, TV remote focus, keyboard behavior and scroll bounds require testing on the target TV. This is an implementation patch, not a screenshot-verified APK.
