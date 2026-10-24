BruniO RP103 / 22207 / 2.0.0-rp103
Apply these 5 files over RP102, preserving paths. RP102 is required.
Adds Naruto Shippuden (TMDB31910, 500 episodes) to the existing complete-series policy.
Canonical season/episode progress keys remain unchanged; UI, next episode, random play,
metadata cache and empty-result absolute-number source fallback reuse RP102 behavior.
Original Naruto and Shippuden remain separate series. Specials are excluded.
All existing RP102 limitations apply: source availability is not guaranteed and incomplete
metadata is rejected rather than padded. No APK or new SQL.
Validation: 13 standalone Kotlin/JVM JUnit tests using actual episode model/policy;
full Android build, live provider metadata and TV runtime remain untested.
