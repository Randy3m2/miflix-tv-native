# Alpha 1.1 compatibility patch

Replace these files in the existing `miflix-tv-native` repository:

- `app/build.gradle`
- `.github/workflows/build-native-tv-apk.yml`

The patch pins Compose to 1.11.4 and Coil to 3.5.0 so the project can remain on `compileSdk 36` + AGP 8.13.2 instead of requiring Android API 37 / AGP 9.1.
