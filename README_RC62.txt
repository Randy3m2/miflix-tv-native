BruniO TV RC62 — Manual Party Requests & Social Traffic Reduction

APPLY OVER RC61:
1. Upload the included files to their matching GitHub repository paths.
2. Execute supabase_rc62_presence_window.sql once in Supabase SQL Editor. It is safe to rerun: it replaces two friend-activity functions without changing tables or stored data. Existing permissions and friend checks are retained. No Edge Function deploy or secrets are required.
3. Run your normal signed Android TV build/release and install RC62 on ALL TVs using the account. An older Fire TV installation will continue its old polling until updated.
No Vercel/web update is needed.

CHANGES
- Incoming Watch Party access requests are fetched only through explicit Refresh requests actions. Opening Friends or the player Friends & Party dialog does not query incoming requests.
- Friends has Refresh requests above the requests list. In the player's Friends & Party dialog, Refresh requests fetches the list and opens the existing approve/reject dialog. Repeated taps while the same lookup is running are ignored.
- Presence writes have a hard five-minute minimum between successful writes for the same account, including title/episode/room changes. Initial publication and account changes can publish immediately.
- Avatar/profile PATCH writes have a four-minute minimum for changed avatars. An unchanged avatar is not written repeatedly at all.
- The existing 5-second Compose timer remains a local tick. Repository gates suppress network writes. It still checks the result of the user's OWN pending join request, so an approved guest can enter; that is distinct from querying incoming requests. Active party playback synchronization, chat and membership loops are unchanged.
- Friend-activity RPCs now regard presence as fresh for seven minutes, preventing a five-minute heartbeat from appearing offline between updates. Room heartbeat freshness remains 90 seconds and expiry checks remain intact.
- Version 22060 / 2.0.0-rc62, with bilingual APK changelog.

EXPECTED TRADEOFF
Friends may see your current title/avatar with up to five/four minutes of delay respectively. After closing the app or losing connectivity, a friend may see your last presence for up to seven minutes. Incoming requests will remain unknown until you refresh them. These limits reduce requests rather than disabling Supabase's own logging. Total log-byte savings depend on other traffic, active parties, retries and all installed versions.

VALIDATION
Passed source checks for the single manual incoming-request call site, UI actions, 5/4-minute gate wiring, success-only gate advancement, SQL liveness windows, mirrored sources and RC62 metadata.
JUnit tests added for five-minute throttling across content changes, unchanged presence heartbeats, four-minute avatar changes, unchanged avatar suppression and account switching. These tests were NOT run here; this environment lacks a JDK compiler/Android build toolchain. The migration was reviewed but NOT executed against a database here.
CI command: ./gradlew :app:testDebugUnitTest --tests com.miflix.native2.data.SocialPublishGateTest :app:assembleRelease
Source check: node verification/SocialPollingRC62Check.cjs .

DEVICE CHECK
Create a party, request entry from another device, and confirm the host sees the request only after Refresh requests. Approve/reject from the player and from Friends. Export another fixed 10-minute log window after installing RC62 on every TV: miflix_access_requests should have only manual invocations; unchanged presence should be about two calls per 10 minutes (boundary effects possible), and unchanged avatars should have no repeat PATCH writes.
