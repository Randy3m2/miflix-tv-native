BruniO TV — RC56 · Genre Paging / Search / Multiview Sources
Version 2.0.0-rc56 / versionCode 22054

INSTALL
Upload all files to the ROOT of the existing repository, replacing matching paths. Keep other files. Run the existing GitHub Actions APK workflow. Cumulative over RC45 or newer, including RC46–RC55, Kids, profile packs and prior focus fixes. No new Supabase SQL, secret, Edge Function or Vercel change.

GENRES
Every genre (including Romance) starts with 25 movie/series results and has Load more · 25 at the end of its catalog. Each click appends up to 25 new, unique titles. Overflow from TMDB's 20-result provider pages is retained, never discarded. Movie/series IDs are distinct; duplicate IDs across pages are excluded. A loading guard/mutex prevents overlapping appends, and generation/language checks discard stale genre responses.
After adding a batch, the view targets its first new title. The final batch can contain fewer than 25; once the provider is exhausted, the footer shows the end-of-catalog message. Failures retain the retryable Load more action.

SEARCH
- Current text appears above the compact keyboard/results area.
- The in-app text box does not open an Android IME or hide the keyboard. OK on the field returns to the on-screen keyboard; hardware/remote Left/Right move its cursor.
- Compact six-column alphabet/number keyboard. Space, backspace, cursor Left/Right, Voice, Clear and View results controls.
- Insertion/deletion works at the cursor, including complete Unicode characters. Up to 100 code points.
- Recent searches appear below the keyboard; if none, seasonal/genre suggestions appear there. Kids gets Kids suggestions and the existing content filter.
- Already-loaded title matches appear immediately. One remote title query starts after a 250ms typing pause, rather than waiting for multiple speculative spelling-variant queries. Existing language/Kids-scoped query caching remains. Network and Kids rating verification can still add latency.
- Voice launches Android's available recognition activity and places its transcription into the field. Voice availability/microphone/recognition quality depend on the TV/remote/service; unsupported TVs show a dismissible error. The selected interface language is passed to the recognizer. No always-listening microphone, separate speech backend or private speech key is added.
Android API reference: https://developer.android.com/reference/android/speech/RecognizerIntent

MULTIVIEW
OK on a tile -> Retry, Listen, Change source, Change channel, Full screen, Remove, Close. Retry has initial focus and recreates only that tile using the current source.
Change source fetches the selected channel's streams from its own provider, marks the current URL, and lets the user select another returned source. Only the selected tile is replaced; stream request headers and its sound selection are preserved. Other players continue. Refresh sources is manual. A provider returning only one/no source cannot supply an alternative the app does not have.
The tile action menu scrolls within bounds. Source requests are tied to the selected tile; no continuous retry or refresh loop.

VALIDATION
Java 17 tests passed: cursor middle insertion/backspace/bounds, Unicode movement/deletion and 100-code-point limit; exact 25-result batches with retained overflow, deduplication, distinct movie/series keys, final partial batch and reset. Existing Kids and repeat-key policy tests passed.
Checked source/resource mirrors, nested source delimiters, no covering NativeTextField in Search, voice result handling, source swap targeting and sound preservation, plus ZIP integrity.
No Android SDK/Gradle or real TV runtime is available here. Android/Kotlin compilation and actual focus/microphone/decoder behavior remain pending. No APK is included; no measured performance claim is made.
Device checks: Romance and another genre -> repeated Load more; type/backspace/edit mid-title; focus the field without keyboard disappearing; Voice -> transcript/cancel/unsupported service; Kids search restriction; Multiview Retry then Change source while another tile has audio; genre row Down still targets its first card.
