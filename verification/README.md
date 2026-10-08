From the project root, using Node 24+:

```bash
npm --prefix verification install
node verification/test_trakt.cjs
node verification/test_chat_logic.cjs
node verification/test_social_db.cjs
node verification/test_party_access.cjs
```

Tests use simulated APIs/DOM and a local PostgreSQL WASM instance. They never
contact production Supabase or Trakt. Kotlin syntax/SQL parsing checks were
run separately during preparation. Android build still runs in GitHub Actions.
