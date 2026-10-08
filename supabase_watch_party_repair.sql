-- Upgrade the existing Beta Watch Party table without deleting rooms.
begin;
create table if not exists public.miflix_watch_parties (
  room_code text primary key,
  host_user_id uuid not null references auth.users(id) on delete cascade,
  state jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '8 hours')
);
alter table public.miflix_watch_parties add column if not exists state jsonb not null default '{}'::jsonb;
-- Old required columns would reject RC1 inserts, which send the JSON state.
do $$
declare col text;
begin
  foreach col in array array['cloud_id', 'media_type'] loop
    if exists (select 1 from information_schema.columns where table_schema = 'public'
      and table_name = 'miflix_watch_parties' and column_name = col) then
      execute format('alter table public.miflix_watch_parties alter column %I drop not null', col);
    end if;
  end loop;
end $$;
-- Preserve previous rooms by converting the Beta fields to the RC1 JSON shape.
update public.miflix_watch_parties p
set state = jsonb_build_object(
  'cloudId', to_jsonb(p)->>'cloud_id',
  'season', coalesce(to_jsonb(p)->'season', '0'::jsonb),
  'episode', coalesce(to_jsonb(p)->'episode', '0'::jsonb),
  'positionMs', coalesce(to_jsonb(p)->'position_ms', '0'::jsonb),
  'playing', coalesce(to_jsonb(p)->'is_playing', 'false'::jsonb),
  'updatedAt', floor(extract(epoch from p.updated_at) * 1000)::bigint
)
where (state = '{}'::jsonb or state is null) and to_jsonb(p)->>'cloud_id' is not null;
alter table public.miflix_watch_parties enable row level security;

drop policy if exists "watch parties read" on public.miflix_watch_parties;
drop policy if exists "watch parties insert" on public.miflix_watch_parties;
drop policy if exists "watch parties update host" on public.miflix_watch_parties;
drop policy if exists "watch parties delete host" on public.miflix_watch_parties;

create policy "watch parties read" on public.miflix_watch_parties
for select to authenticated using (expires_at > now());

create policy "watch parties insert" on public.miflix_watch_parties
for insert to authenticated with check (host_user_id = auth.uid());

create policy "watch parties update host" on public.miflix_watch_parties
for update to authenticated using (host_user_id = auth.uid()) with check (host_user_id = auth.uid());

create policy "watch parties delete host" on public.miflix_watch_parties
for delete to authenticated using (host_user_id = auth.uid());


grant select, insert, update, delete on public.miflix_watch_parties to authenticated;
notify pgrst, 'reload schema';
commit;
