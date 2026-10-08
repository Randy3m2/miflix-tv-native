# Live TV

Open Live TV in the left sidebar. TV Channels uses Nauta Live TV, with categories
loaded from its manifest, and optional Load more for catalogs declaring skip.
Select a channel to play; multiple sources open a modal selection dialog.
Long press opens source selection even for a single link.
Live playback uses the existing player and keeps the screen awake. Live channels
are excluded from VOD resume/progress, and Back returns to Live TV.

Live Sports is prepared for SportsFree. Both the supplied /configure URL and
its conventional /manifest.json endpoint returned HTTP 403 in this environment.
The conventional endpoint is a provisional default, not a verified configuration.
Open https://sportsfree-us2.highfly.to/configure in your browser, complete its
configuration, copy the generated manifest.json URL, then paste it into Live TV
> Live Sports > Sports manifest. Categories are taken dynamically from the manifest.
This setting is stored on the device and shared by its profiles.

Verified Nauta manifest, category response and one HTTP HLS stream response.
No video playback or Android compilation was performed in this environment.
Build with GitHub Actions and verify playback and remote navigation on TV.
Includes prior fixes. Permanent APK URL unchanged.
