BruniO RP94 — Coroutine-debug unit-test hotfix
Version code 22198 / version name 2.0.0-rp94
Apply over RP86–RP93, preserving paths. Includes RP93 avatars and preceding updates.

ROOT CAUSE CONFIRMED
With kotlinx.coroutines.debug=on, coroutine debugging decorates a thread's name,
e.g. release-owner @coroutine#4. RP92 tests compared the entire name literally.
- Cancellation test: same correct owner thread, but string comparison failed.
- Responsiveness test: build-thread name assertion failed before its latch signaled,
  so the parent subsequently failed waiting for construction to start.
These failures were reproduced locally against the unmodified RP93 tests.

FIX
Use assertSame on the actual Thread objects captured by each executor's factory.
Retains worker/owner separation, responsiveness, cancellation, exactly-once release,
stale identity and retry-budget assertions. No test disabled and no production
warm-construction behavior changed. Debugging is not disabled to hide the issue.

KOTLIN WARNINGS
- TmdbRepository: remove the redundant second safe call after the first comparison
  already establishes that exclude is non-null.
- UpdateRepository: narrow DEPRECATION suppression on the legacy installer intent;
  preserve TV installer compatibility/fallback behavior.
- HeadphoneAudioSink: narrow OVERRIDE_DEPRECATION suppression on legacy flush();
  keep required delegation to the mixer. No audio behavior changes.
Gradle 9 compatibility notices are separate; this patch does not upgrade Gradle/AGP.

VALIDATION
Downloaded the standalone Kotlin/JUnit tools into temporary storage only.
Compiled the real WarmConstructionRP92 engine and its real four JUnit tests,
using kotlinx-coroutines-core-jvm 1.10.2 and JUnit 4.13.2.
- Before: exactly the two reported failures with coroutine debugging enabled.
- After: all four pass with debug=on, and all four pass with debug=off, assertions enabled.
No APK generated, no full Android Gradle suite run, no real-TV validation.
The Android-specific warning changes need the normal CI Kotlin compilation check.

SQL
No NEW SQL in RP94. If RP93's watched-friends SQL was already run, do not repeat it.
The previous RP93/RP88 scripts remain bundled for installations that still need them.
