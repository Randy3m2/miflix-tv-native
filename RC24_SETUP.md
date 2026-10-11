# RC24 — Audio selection — 2.0.0-rc24 / 22022

Upload and start a new TV build. No new SQL migration; RC23 calendar migration still applies if not already installed.

Audio selection now resolves the selected group against the player's current track list, clears previous overrides and the automatic language preference, then applies the explicit track override. The menu stays open and checks the player's actual selected tracks for up to eight seconds. A message reports selection confirmation or failure to confirm; selection confirmation is not a guarantee of audible output from the TV's decoder.

Every detected track remains listed with an individual number. Tracks reported as exceeding capabilities can be attempted and carry a limited-compatibility label. Tracks for which the TV reports no decoder produce an explanatory message rather than silently ignoring the click. This update does not bundle software codecs or transcode unsupported audio.

Validated Kotlin syntax and archive/version consistency. No Android compiler or physical TV is available locally; compilation is performed by Actions. On TV, test two supported languages with different spoken audio, a limited-capability track, an unsupported codec and the same selection while paused/in a Party. Audio selection remains local to each participant and does not change the room's shared playback position.
