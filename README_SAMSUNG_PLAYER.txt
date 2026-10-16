BruniO Web — Samsung / External Player feedback fix
Apply after the preceding External Player Button patch, RC60 + Downloads CDN fix.
Upload matching web/ and verification/ files to GitHub and redeploy Vercel. Cache v=15. No APK, Supabase or SQL update.

LOCAL DOWNLOADED FILES
The old button used Web Share and silently left generic help when it failed. It did not invoke Android's local ACTION_VIEW chooser. This patch corrects the feedback; it does not claim that a website can force the native local-file Open With screen.
Selecting a file is still required. The button now catches canShare exceptions and all share errors, detects insecure/local embedding, shows and scrolls an in-dialog status with a non-secret code, and restores the button after completion/failure/cancellation. Unsupported formats/sizes/browsers get explicit feedback, not a silent click. No file upload or background network operation.
Samsung fallback: My Files > Downloads > selected video > Open with (or use an installed player's local-file browser). iPad: Files > Downloads > Share > player. PC: Finder/File Explorer > Open with. Web Share may offer messaging/share apps instead of players. System defaults, installed apps and browser policy determine what appears. A browser File object does not give the web a transferable native content:// URI.

ONLINE ANDROID OPTION
For a generated TorBox link, Android now shows Open in external player · online. This is a user-tapped HTTPS ACTION_VIEW video intent, without a forced player package. It requests handling by compatible installed apps; Android may choose an existing default instead of showing a chooser, and browsers may block it. Copy link remains available. This opens the online TorBox URL, never the selected downloaded local file.

TESTS
Passed Node syntax and mocked web tests, including unsupported file sharing, thrown canShare, insecure context, cancellation, button restoration and Android intent syntax/URL validation. Physical Samsung browser behavior not verified here; the native OS cannot be forced by web code to show every video player.
Reference: https://developer.chrome.com/docs/android/intents
