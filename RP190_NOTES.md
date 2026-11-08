# RP190 — Details: original background, themed controls

TV 2.0.0-rp190 / Mobile 1.0.8-m9-rp190 — versionCode 22329.

Details is no longer an original-style route in ThemeScreenPolicyRP189. Its existing shared controls therefore inherit the saved theme palette, surfaces, rounding and focus borders. No saved preference is changed.

The existing background exclusions in MiFlixNativeApp, the original dark Details canvas/artwork fade in Screens, and the original mobile Details fade remain intact. No seasonal photo or theme logo is added to Details. My List still excludes the entire theme. Home still excludes only the theme background. Daily rotation is unchanged.

Validation: focused route-policy regression tests; full Android release builds and visual device checks remain pending for this revision.
