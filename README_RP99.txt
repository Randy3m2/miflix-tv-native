BruniO RP99 — Nullable recordings-filter compile fix
Version code 22203 / version name 2.0.0-rp99
Apply over RP86–RP98 preserving paths. Includes preceding updates.

LiveHubMemoryRP46.category returns String? when a profile has no saved recording-sport category.
Calling ifBlank directly caused the reported compileReleaseKotlin error at LiveHubRP41.kt:175.
The initializer now uses .orEmpty().ifBlank {"all"}; existing saved sport choices are retained.
Nearby memory reads were checked for the same unsafe pattern.

Validation: compiled the actual session-memory class with the corrected expression using standalone
Kotlin; 3 checks passed (null -> all, empty -> all, existing baseball -> baseball).
Full Android/Compose Gradle compilation and TV runtime are not available here. No APK generated.
No new SQL, no new network requests, no catalog/provider/player behavior changes.
