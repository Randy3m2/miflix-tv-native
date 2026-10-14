Apply over RC48. Upload all included files to their matching GitHub paths, replacing existing files; run the APK workflow and install RC49. No SQL/Edge Function/Vercel changes.

Details: top navigation, a shorter cinematic header, Play/Continue, My List, Watch Party and More. More retains play from beginning, queue, recommendations and rating. Episodes/season selector stay, with cast and community reviews side by side; spoiler protection and actor filmography remain.

Friends calendar: Calendar becomes an inline tab under the persistent Friends header. A responsive month pane and a separately scrolling event pane fit the remaining viewport. Date-cell height adapts to the panel; Back returns to Friends. The event creation form remains a dialog. Existing calendars elsewhere remain supported.

Settings: removed the oversized section focus group and added top anchoring for oversized bring-into-view targets, addressing clipped content when navigating back upward.

Live TV: lazy adaptive channel grid with number/logo/title/LIVE/favorite state. Short OK plays; hold OK toggles favorites. Favorites dialog also supports hold to remove; the opening/release gate prevents accidental playback of the next row after removal. Channels, sports, regions, search, source selection for multiple links, categories and load-more remain. No guide/EPG added.

Home order: featured, Continue Watching, Streaming, Browse by Genre, For You / Your Ratings, then the remaining existing rows. Empty optional rows are omitted as before. Existing platform tiles and animated logos remain.

Validation: source/route/Home-order and paired-source checks, calendar month geometry for 233–340 dp panels, and compiled Java opening-gesture regression checks passed. APK compilation and physical TV focus/render/performance checks have not run here (no Android SDK/Gradle). Build on GitHub and verify long Settings sections, the inline calendar, long press favorite add/remove, Details More and Home ordering on TV.
