BruniO Web — Single Play on TV + local downloaded file handoff

Apply AFTER RC60 and the Download Generate Link/CDN Fix. This small patch changes web/ and its test only. No new APK, SQL, Edge Function or secrets.

Upload these matching paths to GitHub and redeploy Vercel; reload the website. JS/CSS cache version is v=13. The existing brunio-downloads function stays as deployed by the preceding CDN fix.

CHANGES
- Removes Send to TV from each title. Keeps one primary Play on TV action, which opens connection instructions if no TV is connected.
- Top connection button is Connect TV / TV connected, with a TV connection panel title. Episode inputs stay in each series profile.
- Download panel adds Open downloaded file: user manually selects the already-downloaded file from the OS picker, then taps Open with… to invoke the native share sheet where supported. User chooses VLC/Infuse/another installed app if that app accepts the selected format. No automatic app selection or confirmation of successful playback.
- The browser cannot locate the downloaded file or detect completion automatically. It never re-downloads it or sends its content to BruniO/Supabase. It holds a File reference and passes it to the OS only on the explicit Open with… tap.
- File Web Share requires browser/policy support, may reject formats or very large files, and may copy/import the file into the destination app instead of opening it in place. No guarantee of zero-copy or instant opening for large movies.
- If unavailable, the UI explains the reliable iPad path: Files > Downloads > video > Share > player. Local HTTP/embedded TV catalog contexts may lack file sharing; the same manual path works. Normal standalone HTTPS website is preferable for trying native share.
- Existing Open in VLC still streams the online TorBox link. Open downloaded file uses your explicitly selected local file. No network requests are added by these local actions.

TESTS
Passed Node syntax and mocked web execution tests: redundant button removed, one Play on TV, Connect TV label, file selection required, selected File object passed to native share, no cloud/upload requests, and manual Files fallback when unsupported. Existing download/cap/VLC/recents/Now Playing flows remain checked. Not tested on physical iPad Safari/Chrome; browser/app/format restrictions still apply.
WebKit reference: https://webkit.org/blog/13862/the-user-activation-api/
