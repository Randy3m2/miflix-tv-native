BruniO Web — Persistent Login (shell 22)
Apply after BruniO_Web_Compact_Catalog_Icons.zip. Upload matching web files to GitHub and redeploy Vercel. No SQL, Supabase function deployment or APK build.

Session persists in the same browser and website origin, including new tabs and browser restarts. Existing tab sessions migrate automatically. Password is never stored. Refresh remains on demand; Web Locks coordinate refresh across supported browser tabs. Logout clears persistent and legacy session storage and signs out other open tabs.
Private browsing, cleared website data, another browser/domain or revoked/expired refresh tokens still require sign-in.
Validation: catalog, experience, shell checks and isolated persistence/migration/logout checks passed. Real-device browser QA pending.
