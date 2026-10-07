# Build MiFlix TV Native 2.0 Alpha 1

1. Create a new public or private GitHub repo, recommended: `miflix-tv-native`.
2. Upload the contents of this folder to the repo root.
3. In Settings → Secrets and variables → Actions, add/reuse:
   - `MIFLIX_TMDB_TOKEN`
   - `MIFLIX_TORRENTIO_MANIFEST`
4. Commit.
5. Open Actions → Build MiFlix Native TV APK → Run workflow.
6. After a green build, open Releases → MiFlix TV Native 2.0 Alpha 1.
7. Install `MiFlix-TV-Native-v2.0.0-alpha1.apk` beside your legacy MiFlix build.

The native build uses application ID `com.miflix.native2`, so both versions can remain installed for side-by-side comparison.
