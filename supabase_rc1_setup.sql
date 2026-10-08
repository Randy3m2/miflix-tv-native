-- MiFlix TV Native 2.0 RC1 additions
-- Run once in Supabase SQL Editor.

create table if not exists public.miflix_watch_parties (
  room_code text primary key,
  host_user_id uuid not null references auth.users(id) on delete cascade,
  state jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '8 hours')
);

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

create table if not exists public.miflix_pairing (
  pair_code text primary key,
  status text not null default 'pending',
  payload_enc text,
  iv text,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '10 minutes')
);

alter table public.miflix_pairing enable row level security;

drop policy if exists "pairing insert" on public.miflix_pairing;
drop policy if exists "pairing read" on public.miflix_pairing;
drop policy if exists "pairing update" on public.miflix_pairing;
drop policy if exists "pairing delete" on public.miflix_pairing;

create policy "pairing insert" on public.miflix_pairing
for insert to anon, authenticated with check (expires_at > now());

create policy "pairing read" on public.miflix_pairing
for select to anon, authenticated using (expires_at > now());

create policy "pairing update" on public.miflix_pairing
for update to anon, authenticated
using (expires_at > now() and status = 'pending')
with check (expires_at > now() and status in ('pending','approved'));

create policy "pairing delete" on public.miflix_pairing
for delete to anon, authenticated using (true);

-- Optional cleanup helper. Run occasionally if you want:
-- delete from public.miflix_pairing where expires_at < now();
-- delete from public.miflix_watch_parties where expires_at < now();

grant select, insert, update, delete on table public.miflix_pairing to anon, authenticated;
grant select, insert, update, delete on table public.miflix_watch_parties to authenticated;
