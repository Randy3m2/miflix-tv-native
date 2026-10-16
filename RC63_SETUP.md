# BruniO TV RC63.1 — Party & cinematic browsing

Apply this cumulative delta over RC62 or RC63. Rerun the included SQL even if you already installed RC63; it also enables playback action events. The ZIP has fewer than 100 files and preserves both Android upload layouts.

## Install

1. Run `supabase_rc63_party_presentation.sql` once in Supabase SQL Editor. It is additive and safe to rerun; it does not change the return types of your existing RPCs.
2. Upload the patch files to their matching GitHub paths, including the new Kotlin files and `docs/party/index.html`.
3. Build/install the TV APK with your usual GitHub workflow. Update host and guest TVs to RC63 for the new options.
4. Publish the updated `docs` GitHub Pages site so the existing party QR opens the new join preview.

No Vercel redeploy, Edge Function deployment, private secrets or server required. The successful direct Cast implementation on the main website is unchanged.

## Included

- Small participant avatars above the video, shown with player controls.
- Brief member pause/resume notices beside the avatars, automatically hidden after 3.5 seconds. A remote-control action sends one small event through the existing party event stream; there is no new periodic query. In host-only mode, guest actions say (locally). Buffering, audio recovery and automatic synchronization do not emit these notices. Old event history is not replayed as notices.
- Right-side Friends & Party sheet with spaced code, members, QR/link and friend invitations.
- Invited friends see Join in Friends after opening/refreshing it. A deliberate host invitation preapproves that friend; QR/code visitors still require host approval.
- Optional host-only party controls. Guests can pause/seek locally and use Watch with the party to resume following the host.
- Explicit Switch party to the current title/episode action using the actual player position. Existing automatic host synchronization is preserved.
- Share my stream starts OFF, asks for consent in the TV interface, and stores private source URLs/headers separately from room metadata. Only admitted members of the active room can fetch a matching, unexpired source.
- Guests see the host source name/file size and a Party source marker when their available source metadata matches. Use party source is available when sharing is enabled.
- Transparent playback information over the movie: source, engine, filename, File Size, video/audio formats, subtitles and buffer. Information rows can receive focus and scroll with the remote.
- Fixed upper backdrop/logo/synopsis area in Home, Collections and catalog/My List screens. Spotlight cards retain the 3-second expansion delay; synopsis no longer occupies a separate block under each rail.
- Coalesced held vertical repeats, faster collapsing cards, debounced backdrop changes and bounded logo caching. Fresh D-pad presses remain immediate.
- RC63 release notes appear in Notifications and Settings → Changelog / Details.

## Important behavior

- Refresh requests remains manual in the player and Friends. Presence stays at 5 minutes and changed avatars at 4 minutes. Invitations are fetched with the directory, not through a new background polling loop.
- Active-party synchronization/chat intervals are unchanged. Source sharing writes once when enabled or when the host source changes, not every heartbeat.
- A shared URL may contain provider/debrid access. Disabling sharing removes it from the room but cannot invalidate a URL a member already copied. Providers may restrict external IPs or simultaneous playback.
- A QR preview exposes only title artwork, synopsis, host nickname and member count for a valid code. It never returns playback URLs, source headers or account IDs. The dedicated party chat page remains separate from the main web app.
- This update does not add a 3-seat limit or remove existing streaming collections/logos.

## Verification

Passed: PostgreSQL/PGlite tests for repeated migration, private source RLS, membership, invitations, expiry, sharing-off cleanup, host controls and safe QR preview; previous Party/access tests; source wiring/mirror checks; Kotlin syntax checks for new components; party-page JavaScript syntax.

No Android SDK/JDK compiler is available in this environment. No APK was generated or compiled here, and real TV focus, animation frame pacing, provider playback and multi-device party behavior still need device testing.

After installing, test: create a party in the player → QR/invite → guest joins → source toggle OFF/ON → host-only controls → guest pause/seek/rejoin → Switch party to current content → Info scroll → rapid horizontal/vertical navigation.
