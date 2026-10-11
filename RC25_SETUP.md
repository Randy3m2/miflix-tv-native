# RC25 — Player Friends navigation — 2.0.0-rc25 / 22023

Upload the cumulative TV project and start a new build. No additional SQL. Includes all RC24 audio changes and retains Settings' default audio language for newly opened content.

Player → Party → Friends/Parties now sorts people alphabetically with a stable user-ID tie breaker. Every friend row is focusable, including offline people and people without a room. Previously only the nested Request access buttons accepted focus, causing directional navigation to skip rows. Each card is now one focus target; OK requests access when a room is available. A card without a room explains that no Party is available.

Up/Down explicitly selects the previous/next person, composing a row before requesting its focus when it is offscreen. Scrolling occurs only when needed to reveal the target row. The first row returns upward to Friends; the last row moves downward to Close; Up from Close returns to the last friend. Focused cards turn white. Activity refresh remains manual.

Validation: Kotlin syntax and ZIP/version consistency checked. Full Android compilation and physical remote testing remain pending in Actions/on TV. Verify a mixed list of online/offline friends, more entries than fit on screen, both ends of the list and manual refresh. The audio implementation is unchanged from RC24.
