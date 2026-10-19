BruniO TV - RP13 unit test compilation hotfix

Apply over RP13, preserving paths and replacing both mirrored test files.

Replaces the test-only com.sun.net.httpserver.HttpServer dependency with a
loopback java.net.ServerSocket fixture. No dependencies or production code change.
Retains coverage of HTTP failure handling and the transport call deadline.

Validation: isolated Kotlin compilation passed. All 31 related JVM tests passed
with only java.base and java.logging modules available, excluding jdk.httpserver.
Android Gradle compilation and APK generation were not run.

The reported w: warnings are separate from the test compilation failure and
are not modified by this hotfix. P2P remains paused. No SQL or Vercel changes.
