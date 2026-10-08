# Build MiFlix TV Native 2.0 RC1

## Before building

1. Run `supabase_rc1_setup.sql` once in your Supabase SQL Editor.
2. Confirm GitHub repository secrets still exist:
   - `MIFLIX_TMDB_TOKEN`
   - `MIFLIX_TORRENTIO_MANIFEST`
3. For QR sign-in/add-on pairing, enable GitHub Pages using **GitHub Actions** in Settings -> Pages and run `Deploy MiFlix Pairing Pages` once.

## Build APK

GitHub -> Actions -> **Build MiFlix Native TV APK** -> **Run workflow**.

On success, download either:

- Versioned: `MiFlix-TV-Native-v2.0.0-rc1.apk`
- Permanent latest: `MiFlix-TV-Native.apk`

The permanent asset stays under the `native-latest` release so the same Downloader URL/code can continue to be used.
