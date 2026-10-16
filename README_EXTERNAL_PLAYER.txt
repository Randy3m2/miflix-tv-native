BruniO Web — Open in external player
Apply AFTER the Web Play TV / Local File patch (which follows RC60 + Downloads CDN fix).
Upload included web/ and verification/ files to matching GitHub paths and redeploy Vercel. Cache v=14. No Supabase function, APK or SQL update.

Flow: Open downloaded file > select an existing local video > Open in external player.
The button now uses exactly that English label in both UI languages, as requested. On a supported browser/device it requests the native share/app menu with the chosen File object, via navigator.share({files:[file]}), directly from the button tap. The user selects an available installed player.
A website cannot force a video-player-only chooser, enumerate installed players, or invoke the desktop OS Open With dialog universally. A share sheet may show additional destinations or no video player at all. Unsupported browsers, formats, very large files and policy restrictions use the explanatory fallback: open the saved video in Files/Downloads, or Finder/File Explorer on PC, then Open With/Share to the desired player. No automatic download detection or direct local-file path access is claimed.
The website does not upload or re-download the chosen file, nor call Supabase for this action. The OS/browser may copy/import the file into a selected app.
Verified Node syntax plus mocked button text and selected-file share request, no upload/network call and unsupported sharing fallback. Physical mobile/iPad/desktop behavior remains untested.
