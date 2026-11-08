# RP191 — Progressive skip buttons

TV 2.0.0-rp191 / Mobile 1.0.8-m9-rp191 — versionCode 22330.

Skip Intro and Skip Credits share SegmentSkipButtonRP191. White fills the entire interior from left to right according to playback progress through the detected intro/outro interval. No seconds, percentage or timer text is displayed. A dual-tone label remains readable over both the white fill and dark unfilled portion. Existing theme shape and focus outline are retained.

Pressing with the TV remote or touch completes the fill in 140 ms, presents the completed state for a frame and then calls the existing seek action once. It does not automatically skip when the progress reaches the end. Disposing the button cancels pending animations/actions. Progress comes from the existing 250 ms player position samples and is smoothed locally; no new polling, player preparation or network requests are added.

Existing intro dismissal, skip eligibility, guest/live/moment exclusions, and post-credits-scene seek targets are preserved. Credits progress is bounded by the effective episode duration. Marker availability remains required, as before. Daily rotation and theme exclusions are unchanged.

Verification: shared fraction-policy tests cover intro/credits intervals, pauses/buffering, seek direction, invalid markers and bounds. Full Android verification commands are :app:testReleaseUnitTest :mobile:testReleaseUnitTest :app:lintVitalRelease :mobile:lintVitalRelease. Visual animation, focus and touch behavior still require physical device testing. No signed APK is published by this source change.
