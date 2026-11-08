# RP187 — resilient verified update checks

The reported HTTP 503 occurred during GitHub discovery, before installation. The previous code displayed the entire remote HTML response through its generic error handler. Both latest-channel releases were inspected and contained their RP186 APK and update.json; a tag's original published_at is not the version of its current assets.

The updater now reads the public channel update.json directly before consulting the release API. If that fails, it discovers the metadata asset through the API and makes one metadata retry after a short delay. There are at most three metadata requests. Requests bypass caches, have connection/read timeouts, bound response bodies to 256 KiB, restrict redirect hosts, and forward no account credentials. Cancellation propagates. Failure does not claim the app is up to date or expose remote HTML/URLs through the update dialog.

Validation requires a positive version code, version text, a matching platform release tag, trusted release URL, SHA-256 and size. Application ID is checked when present; legacy TV manifests omit it and remain supported through their native release tag and mandatory APK package validation. Downloads still verify size, checksum, package, increasing version and the installed signing certificate before installation.

These changes only take effect after installing an APK built with RP187 (build 22326). Pushing source code does not modify installed RP185/RP186 clients or create signed APKs. Existing releases can be installed over the matching package without uninstalling if downloaded successfully; do not uninstall to work around a transient HTTP error.

Regression fixtures cover direct metadata with no API call, 503 fallback/retry, sanitized failures, cancellation, malformed/untrusted metadata, TV/mobile separation, unchanged-version behavior and legacy TV manifests. Real signed-APK installation on devices remains a separate verification step.
