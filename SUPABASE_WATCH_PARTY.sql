-- MiFlix TV Native 2.0 Beta 1 - Watch Party table
-- Run once in Supabase SQL Editor.

create table if not exists public.miflix_watch_parties (
  room_code text primary key,
  host_user_id uuid not null references auth.users(id) on delete cascade,
  cloud_id text not null,
  media_type text not null,
  season integer not null default 0,
  episode integer not null default 0,
  position_ms bigint not null default 0,
  is_playing boolean not null default false,
  updated_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '4 hours')
);

create index if not exists miflix_watch_parties_expires_idx on public.miflix_watch_parties(expires_at);

alter table public.miflix_watch_parties enable row level security;

drop policy if exists "miflix party read authenticated" on public.miflix_watch_parties;
create policy "miflix party read authenticated"
on public.miflix_watch_parties for select
to authenticated
using (expires_at > now());

drop policy if exists "miflix party host insert" on public.miflix_watch_parties;
create policy "miflix party host insert"
on public.miflix_watch_parties for insert
to authenticated
with check (auth.uid() = host_user_id);

drop policy if exists "miflix party host update" on public.miflix_watch_parties;
create policy "miflix party host update"
on public.miflix_watch_parties for update
to authenticated
using (auth.uid() = host_user_id)
with check (auth.uid() = host_user_id);

drop policy if exists "miflix party host delete" on public.miflix_watch_parties;
create policy "miflix party host delete"
on public.miflix_watch_parties for delete
to authenticated
using (auth.uid() = host_user_id);

grant select, insert, update, delete on public.miflix_watch_parties to authenticated;
