# RC28 — 2.0.0-rc28 / 22026

## Player rollback

At your request the player, audio controls, source resolution/selection and HTTP helper are restored to RC25. RC26 automatic stall detection/recreation, lookup cancellation changes and recovery tests are removed. The RC27 guest Party closure notice remains, along with the RC25 Friends list and all other earlier features. Historical RC26 documentation describes changes that no longer apply.

## Updates screen

Settings → Check for Updates opens a separate dialog with a scrollable body and a fixed Close button. Status, errors and installation diagnostics remain visible without expanding the final Settings item beyond the viewport.

Downloads use unique APK filenames and an APK extension during archive validation. Rechecking metadata no longer deletes a previously handed-off APK. Installer launch now uses ACTION_INSTALL_PACKAGE with explicit URI read permission and ClipData, with ACTION_VIEW fallback if no installer handles that action. Existing SHA-256, package, version and signing checks remain enforced.

Installation details show the installed package/version/Android API and public certificate fingerprint. Validation errors report actual installed and incoming versions or fingerprints. This does not bypass Android signing restrictions.

## Update conflict: unresolved on the actual TV

This environment cannot inspect the installed TV APK or its package-manager failure. A signature mismatch requires signing with the original private key to update the same installation without deleting its data. The SHA-256 fingerprint identifies a key; it cannot recreate the private key. Do not replace signing secrets or uninstall as a troubleshooting shortcut. The current workflow already verifies a permanent signing certificate before publishing.

To diagnose the existing installation without uninstalling, provide the exact Android error (photo) and installed app version, ideally the APK used to install that version. Compare its certificate with signing_certificate_sha256.txt using Android SDK apksigner verify --print-certs. If signatures match, the exact installer failure is needed to distinguish version, APK access, storage and device compatibility. No signature mismatch is assumed merely from the generic phrase package error.

Upload the cumulative source and run a NEW TV build. No new SQL. Keep existing signing secrets. A new version code does not solve a mismatched certificate.

## Validation

Compared the restored PlayerScreen, PlayerControls, Http and source selection/resolution functions against RC25 byte-for-byte. Kotlin syntax and ZIP contents verified. Android compilation, installation and remote control testing remain pending in Actions/on the TV.
