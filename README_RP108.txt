BruniO RP108 / 22212 / 2.0.0-rp108
Apply over RP107 preserving paths. Only changed source files are included.

LIVE HUB
Exactly six cards in a 3x2 grid: LIVE TV, NBA, MLB, F1, LIDOM, OTHERS.
Remove the unused Live title/back row from the main hub. Enlarge cards and increase gaps.
Reuse original atlas crops and aspect-fit artwork without stretching, recompressing,
replacing logos or modifying stream quality. NFL/football and soccer categories remain
available inside Others. Live scores / pin / unpin opens from Others using the existing UI.
The normal top menu remains centered with its settings gear.

SPORTS FAIL-SAFE
On an error for a live sports-provider stream, retry the SAME MediaItem once after 250ms,
retaining URL, request headers and playback parameters. Budget is scoped to playbackId,
never reset by refresh, successful frames or recomposition. Subsequent failure uses the
existing next-source/error flow. Disable the extra HLS-parser retry for sports to avoid a
third same-source attempt. No new retry for VOD, recordings, ordinary Live TV or LIDOM.
Leaving the screen cancels the pending job; backgrounding keeps an actionable error.

STATS TAB FIX
The supplied video was clarified to show inaccessible Batting/Pitching/Teams controls.
Previously team-button Down pointed directly to the table, skipping the tabs entirely.
Now explicit focus links are teams -> tabs -> persistent player table, Up reverses this,
and Left/Right traverses every tab including More Columns. Team requesters are stable,
not reattached only to the selected team. Player table Left goes to video; Right to teams;
Up from its first row returns to the active stats tab. Keep RP106 scroll stabilization and
RP105 ten-second polling, cache and cancellable background requests.

GLASS ACTION DESIGN
Shared cached lightweight gradient fills replace opaque white focused action fills.
White text/icons remain legible; selection/focus is a stronger translucent fill and edge.
Includes common FocusButton/Back/ModernButton (dialogs, authentication, menus, details),
player controls, settings tiles, profiles/add-profile, watch-friends actions, search keys,
Live channels/source selectors, sports tabs, LIDOM actions, custom channels, filters,
local-list creation, social actions, calendar, moods/Load More and Easy controls.
Poster art, logos, progress bars, switch thumbs and semantic badges retain their purpose.
No backdrop blur or extra network work. The top profile trigger is a circular photo,
without the old rectangular container/caret; the existing profile menu still opens.

HEADER FOCUS
Home claims focus at initial profile entry. An explicit top-menu navigation hands focus
to that destination once (including Friends & Party), not the first Home tab. Polling and
recomposition never reclaim it. Preserve ordinary details Play-button focus and restricted
Easy/Kids behavior. Remove competing first-tile autofocus from the Live hub.

Validation: 12 standalone Kotlin/JVM JUnit tests passed for header handoff/profile scope
and exactly-once sports retry/exclusions. Thirty Kotlin source/test files were parsed;
pre-existing parser diagnostics in inherited source files were compared with their
originals. Static regression checks cover the stats header graph, six-card mapping,
Others categories/scores, profile trigger and retry-before-fallback/same MediaItem.
Full Android/Compose compilation, physical remote traversal, all-screen visual QA and
frame latency remain unverified here. No APK or new SQL/Supabase calls.
