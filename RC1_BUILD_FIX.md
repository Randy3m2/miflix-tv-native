# RC1 compilation fix

Overwrite the matching files in the existing repository, including Qr.kt,
PairingServer.kt and Catalogs.kt. These three retired source files are intentionally
empty so old definitions do not remain after an upload.

Fixes: duplicated QR composable, obsolete models, missing Watch Party deep-link
argument, and suspend HTTP calls inside a Sequence callback.

The APK workflow and permanent native-latest/MiFlix-TV-Native.apk URL are unchanged.

Validation: source/reference checks against the supplied compiler log. An Android
build was not run locally because Gradle and the Android SDK are unavailable.
Run Build MiFlix Native TV APK on main after committing these changes.
