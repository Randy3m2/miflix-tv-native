# Channels and Watch Party update

Channels: search by name (case/accent insensitive) or assigned number within the
selected category. Catalog search is requested from the add-on when supported.
Logos come from the catalog; a channel initial remains as fallback.
Numbers are allocated once per channel/provider and saved on this device. They
stay the same between categories and searches, but are not official TV numbers
and may differ between devices. Pagination remains available.

Watch Party: execute supabase_watch_party_repair.sql in Supabase SQL Editor.
It adds state, migrates Beta room fields, permits RC1 inserts without the old
cloud_id/media_type fields, preserves data and applies host-only write policies.
It notifies PostgREST to reload its schema cache. The main RC1 setup SQL now also
upgrades existing Beta tables.

Then build and install the APK using the unchanged workflow/latest URL.
Includes all previous fixes. Android build, TV playback and the SQL migration
against your actual Supabase project still need to be verified.
