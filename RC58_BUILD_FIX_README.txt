BruniO RC58 Kotlin build fix

Apply AFTER the RC57 update and RC58 Friends hotfix.
Replace the four Kotlin files preserving their folder paths.
Renames setFocusColor to saveFocusColor to avoid the generated JVM setter.
Color theme behavior and persisted preferences are unchanged.
Version stays RC58 (2.0.0-rc58, code 22056); the previous build did not compile.
Commit and rerun the GitHub build. No Supabase/Vercel changes needed.
Function/call sites and source mirrors checked; APK not compiled locally.
