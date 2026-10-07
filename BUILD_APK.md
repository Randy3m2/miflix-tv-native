# Build MiFlix TV Native 2.0 Alpha 2

1. Upload/replace the Alpha 2 source files in the `miflix-tv-native` repository.
2. Keep these existing repository Actions secrets:
   - `MIFLIX_TMDB_TOKEN`
   - `MIFLIX_TORRENTIO_MANIFEST`
3. Make sure `.github/workflows/build-native-tv-apk.yml` is replaced by the Alpha 2 version.
4. Commit to `main`.
5. Open **Actions** -> **Build MiFlix Native TV APK** -> **Run workflow**.
6. A successful run creates:
   - Artifact: `MiFlix-TV-Native-v2.0.0-alpha2`
   - APK: `MiFlix-TV-Native-v2.0.0-alpha2.apk`
   - Release tag: `native-v2.0.0-alpha2`

## TV test sequence

After installing over Alpha 1.1:

1. Home: rapidly press Right 8-10 times. The card focus should move instantly while the hero waits until you stop.
2. Move Left/Right repeatedly inside a rail. The rail should no longer make the small vertical jump caused by focus scaling.
3. Enter/leave the sidebar. It should expand over Home without shifting all catalogs.
4. Open Collections and test Action, Science Fiction, Netflix and Disney+ shelves.
5. If signed in, verify Continue Watching and progress bars.
6. Open a movie with progress and confirm Resume appears.
7. Start playback and verify the Media3 controls hide after roughly 2 seconds.

The build remains on compileSdk 36 / AGP 8.13.2 / Gradle 8.13 for compatibility with the known-good Alpha 1.1 toolchain.
