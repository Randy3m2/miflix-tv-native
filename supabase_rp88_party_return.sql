-- RP88: permit recipient-specific return commands on the existing profile-scoped event table.
-- Run once in Supabase SQL Editor. Keeps all rows, RLS policies, grants and existing actions.
-- Existing profile social schema (RP1 + party presentation) must already be installed.
begin;
do $migration$
begin
  if to_regclass('public.rp1_miflix_party_events') is null then
    raise exception 'Install the existing RP1 profile social schema first';
  end if;
end $migration$;
alter table public.rp1_miflix_party_events enable row level security;
alter table public.rp1_miflix_party_events drop constraint if exists rp1_miflix_party_playback_body;
alter table public.rp1_miflix_party_events drop constraint if exists miflix_party_playback_body;
alter table public.rp1_miflix_party_events drop constraint if exists brunio_rp88_party_playback_body;
alter table public.rp1_miflix_party_events add constraint brunio_rp88_party_playback_body
 check(kind<>'playback' or body in ('paused','resumed','paused-local','resumed-local')
   or body ~ '^return:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$');
notify pgrst,'reload schema';
commit;
