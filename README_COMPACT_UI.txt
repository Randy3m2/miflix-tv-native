BruniO Web — Compact responsive content + source actions
Apply AFTER BruniO_Web_App_Content_Update.zip (PWA/content release19). Upload these matching paths to GitHub and redeploy Vercel. Web assets/service-worker shell advance to20. No Supabase, SQL or Android APK changes.

TITLE SCREEN
- Visible primary actions: Play or Download, compact My List state, Play on TV, More. Play or Download has primary emphasis; remaining actions are quieter and shorter.
- Share, recommend, event, play from beginning, Rate, watched state, queue and random episode stay available under More. The previous second full button row is removed. Rating controls appear in the content area after selecting Rate.
- More is a native disclosure with keyboard/touch handling, Escape/outside-click dismissal, and an expanding in-flow grid (no floating panel clipped by the dialog). It expands to full row only when requested. Desktop3-column and tablet/mobile2-column secondary actions.
- Manual season/episode inputs are folded; the actual season dropdown and episode grid remain visible below. Shorter paragraph/block spacing without removing synopsis/cast/reviews/related content.

SOURCE CARD (second reference screenshot)
- Compact bordered card with source name, filename, provider and a separate size badge.
- Play in BruniO and Download are grouped together, rather than spread across multiple full-width rows. Other players folds the standalone web-player tab, Android/VLC app links, Copy link and existing help.
- Repeated ready/instruction paragraphs are visually hidden but remain announced to screen readers. Errors/loading remain visible. File names wrap/clamp with their full text retained in accessible DOM/title.
- Copy-link manual fallback keeps Other players open so the selectable text remains visible. No extra source lookup or generation caused by opening these menus.

RESPONSIVE
Desktop: a single compact main action row and metadata/actions side-by-side in source cards.
Tablet: controls wrap; source action row sits below metadata, without horizontal overflow.
Phone: main controls in a2x2 grid, Play/Download side-by-side on each source card, Other players full-width below. Disclosures expand within the card; touch controls remain approximately44px or larger. Minimum widths reset to allow smaller screens. No new animation/dependency/framework.

CHECKS
Node syntax, mocked primary-action grouping, folded source alternatives, generated-download flow, existing content/progress/queue behaviors and PWA/cache exclusions passed; mirrored files/ZIP verified. Actual responsive rendering was not browser/device-tested here. Reference screenshots were inspected locally; no screenshots included or uploaded.
