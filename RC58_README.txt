BruniO 2.0.0-rc58 / versionCode 22056
Small patch AFTER RC57. Upload these files preserving their paths, replacing
matching files. RC57 and all its required source/assets must already be present.

Friend activity menu now contains:
Watch
View Details
Recommendations
Close

View Details opens the friend's current/last reported TMDB title without
starting playback or leaving your Watch Party. Works for movies and series.
It shows an explanatory message when the activity has no supported title ID.
Watch retains independent playback and existing leave-party confirmation.
The redundant Request Access action was removed from this menu as requested.
Other room-join flows remain available elsewhere.
Release notes in Notifications and Settings include this change.
No Supabase or web deployment needed. Run the normal native GitHub build.
Syntax and mirrored sources checked. No APK compiled or TV test in this runtime.
