# BruniO RP161 · GPT + Gemini (un proveedor activo)

Based on RP160 main commit ec741082. Android app versionCode 22265, versionName 2.0.0-rp161 / Build 161.

## Implementado
- Settings > BruniO Assistant: Local, Gemini or GPT (selection is mutually exclusive).
- Existing Gemini integration retained; GPT uses the same TMDB validation, local app action catalog and existing tools.
- GPT voice input prefers es-DO regardless of app UI language. Voice recognition uses Android RecognizerIntent (not charged by OpenAI).
- GPT uses authenticated Supabase Edge Function; **OpenAI secret never appears in APK or GitHub**.
- Per-account monthly budget: server atomically reserves US$0.03 per request and refuses when reservation reaches US$3 UTC monthly. Up to 100 GPT calls per month. Reservations are deliberately conservative; the actual OpenAI invoice can be lower.
- No GPT fallback to Gemini; on remote failure, local functions remain active.
- Player AI prompt no longer inherits normal search text.
- GPT images are disabled until a verified image-token upper-bound can be enforced.
- Reuses RP160 dictation, navigation fixes and typed action validation.

## Backend setup (REQUIRED before testing GPT)
1. In the Supabase SQL editor, apply `supabase/brunio_ai_budget_rp161.sql`.
2. Set **OPENAI_API_KEY** securely as a Supabase Edge Function secret. Set optional BRUNIO_OPENAI_MODEL=gpt-5-nano. Do **not** commit secrets.
3. Deploy `supabase/functions/brunio-ai/index.ts` as a function named `brunio-ai` with JWT verification enabled. Supabase built-in SUPABASE_URL, SUPABASE_ANON_KEY and SUPABASE_SERVICE_ROLE_KEY must be available in the function environment.
4. Sign into BruniO with a Supabase account. Choose GPT in Settings > Assistant.
5. Set a separate OpenAI organization/project **$3 monthly budget alert** as an additional guard. Project budgets may be notifications rather than immediate provider-side hard stops; the BruniO function is the request gate.

## QA (not yet physically verified)
- Verify provider radio switch is exclusive, including upgrading an installation with Gemini enabled.
- Dictate Spanish (es-DO) actor requests, commands and Itachi storyline lists.
- Verify pause, resume, subtitles/audio in player, channels, franchise order and local TMDB.
- Validate budget via repeated authenticated requests; request 101 in month returns HTTP 402. Verify two simultaneous requests near cap cannot exceed US$3 reserved.
- Verify failed OpenAI calls still consume a conservative budget reservation.
- Verify JWT rejected for anonymous requests and nothing leaks OPENAI_API_KEY.
- Verify AI errors do not freeze player, and Return Home closes overlays.
- Confirm Android release Gradle build through GitHub Actions.

**Important:** This branch alone does not deploy Supabase, set secrets, or produce an installed APK. No live GPT or remote TV test has been run.
