# Build MiFlix TV Native — Alpha Final

1. Upload/replace this source in the `miflix-tv-native` GitHub repository.
2. Preserve repository secrets `MIFLIX_TMDB_TOKEN` and `MIFLIX_TORRENTIO_MANIFEST`.
3. Make sure `.github/workflows/build-native-tv-apk.yml` is replaced too.
4. Open **Actions → Build MiFlix Native TV APK → Run workflow**.
5. The build produces `MiFlix-TV-Native-v2.0.0-alpha-final.apk` as the versioned artifact/release.
6. The workflow also overwrites `MiFlix-TV-Native.apk` under the fixed `native-latest` release.

Permanent URL:

`https://github.com/Randy3m2/miflix-tv-native/releases/download/native-latest/MiFlix-TV-Native.apk`

You can install the final alpha over the previous Native Alpha without uninstalling it because the application ID remains `com.miflix.native2`.
