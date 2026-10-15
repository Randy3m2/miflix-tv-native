BRUNIO 2.0.0-rc59 / versionCode 22057

APPLY AFTER RC58 + ITS KOTLIN BUILD FIX
This ZIP is a delta patch, not a standalone project. Upload its files preserving
folder paths, replacing matching files in your existing repository.
Commit both native and web/ changes. Run the normal native GitHub release build;
Vercel must redeploy the updated web/ files for the new TV catalog connection.
No SQL, Edge Function deployment, new secret or additional add-on required.
The previous saveFocusColor JVM setter fix is retained in the included sources.

PLAYER
- Toolbar label is Friends & Party. Opening refreshes the same friends directory
  used by the main screen, once per opening; no additional periodic polling.
- Panel reduced to 570 x max 440 dp, smaller friend avatars and text.
  Removed manual Refresh and Calendar from this player panel.
- Playback loading is artwork only: episode still when available, poster/backdrop
  fallback otherwise. No player controls or Finding the best source text while
  loading. The cover remains until Media3 reports the first rendered video frame.
  Back cancels the pending start. Errors remain accessible above the artwork.
- Seek bar shows target position / duration during remote seeking and pointer
  dragging. Drag previews locally and commits the seek when released, avoiding
  repeated network/video seeks on every drag frame. Timeline remains white.
- Recommendation previews use portrait proportions and Fit to keep covers whole.

NAVIGATION / LIST
- My List is now in the top navigation beside Live TV, also available in Kids.
  Removed its duplicate entry from the profile dropdown.
- Native My List has Last Added and Oldest Added. Ordering happens before the
  25-item page is fetched, including Movies/Series filters.
- Web My List defaults to Last Added; its pages now contain up to 25 titles.
- Added order uses the existing ordered favorites array: the current atomic
  Supabase RPC prepends additions. No invented historical addition dates and no
  database migration. Previously imported batches may have their existing order.

RANDOM EPISODES
- Series details > Random episode, or Player > Episodes > Random.
- Includes confirmed aired regular seasons/episodes; excludes specials, empty
  seasons, unknown air dates and future releases. Avoids immediately repeating
  the current episode when another eligible choice exists.
- Picks seasons weighted by reported episode counts, then an eligible episode.
  Season metadata is cached for six hours; only the chosen season is fetched.
- Starts the selected episode from the beginning. Next chooses another random
  episode while shuffle is active. At the end, Autoplay next episode controls
  whether it continues automatically; queued items retain priority.
- End panel includes Stop shuffle. Choosing a specific episode also stops it.
  Kids checks and Watch Party host restrictions still apply.

CONNECT PHONE / PC — NEW SIMPLE FLOW
1. TV > profile menu > Connect phone / PC.
2. Scan the QR or open the COMPLETE displayed http://IP:8765/catalog/... link.
3. Your actual catalog at https://miflix-tv-native-web.vercel.app opens already
   connected. On a new browser/device, sign in to BruniO normally if necessary.
4. Choose a title > Play on TV. For a series, the details show season/episode
   fields. There is no pairing code or separate IP entry step in this flow.

The TV serves a thin local parent page and embeds the Vercel catalog full screen.
The updated website communicates through an origin/source-checked message bridge;
only the local parent sends same-origin /send requests to the TV. This avoids the
old HTTPS-web-to-HTTP-TV mixed-content connection path. No video plays in the web.
No account password, Supabase token or TV session token is sent through the QR or
message bridge. The QR carries a random, expiring local pairing capability.

Open link validity: 2 minutes. Established connection: up to 30 minutes.
Both devices must reach the same private Wi-Fi subnet; TV must use Wi-Fi and
BruniO must remain foregrounded. Guest isolation/VPNs can prevent connection.
Disconnect, actual Wi-Fi address changes, profile/account changes or backgrounding
close/revoke the receiver. Routine network callback notifications no longer
regenerate the link when the Wi-Fi address has not changed.
Connect remains unavailable in Kids and while receiving commands during a Party.
The displayed full link must be used; the bare IP/root URL is not a pairing link.

IMPORTANT WEB DEPLOYMENT
Deploy web/index.html and web/app.js together before testing the new TV link.
The Vercel production site must remain embeddable: do not add X-Frame-Options DENY
or a frame-ancestors policy that blocks the local TV page. The current production
response was reachable and had neither restriction when checked.
The expected web origin is fixed in the bundled brunio-local-cast.html. Update
that constant if you change the production domain in the future.

CHANGELOG
Notifications previews RC59 changes. Settings > System > Changelog / Details
shows RC59 and retains the earlier RC57–RC58 notes below it.

VERIFICATION / LIMITS
Passed: Java random episode eligibility/weighting; local link capability, expiry,
origin-bound authentication, revocation, subnet checks; Node parent bridge flow,
message origin/source validation, token isolation, send, input rejection and
browser disconnect; endpoint restrictions; web/controller JS syntax; changed
Kotlin UI and isolated new player/random/list syntax; mirrored file equality.
HTTP socket integration was skipped because this runtime has no private IPv4
interface. No Android SDK/Gradle compilation or physical TV/browser interaction
was available. Validate compileReleaseKotlin in GitHub, then test first-frame
loading, Back cancellation, seeking, random autoplay, remote focus and the QR
on your TV and phone/PC. These checks do not certify FPS or device compatibility.
