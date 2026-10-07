# MiFlix TV Native 2.0 — Alpha Final.1 Patch

Replace these files in the existing Native repo:
- app/src/main/java/com/miflix/native2/ui/Components.kt
- app/build.gradle
- .github/workflows/build-native-tv-apk.yml

This fixes the Kotlin compile failure caused by `nativeKeyEvent` and keeps the permanent `native-latest` APK release channel.
