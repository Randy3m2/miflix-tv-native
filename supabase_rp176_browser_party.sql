begin;
create table if not exists public.brunio_browser_party_rooms_rp176 (
  room_id text primary key, host_user_id uuid not null references auth.users(id) on delete cascade,
  expires_at timestamptz not null, media jsonb, allow_guest_control boolean not null default false,
  created_at timestamptz not null default now()
);
create table if not exists public.brunio_browser_party_events_rp176 (
  room_id text not null references public.brunio_browser_party_rooms_rp176(room_id) on delete cascade,
  sequence bigint not null, sender_id text not null, command text not null,
  position_ms bigint not null default 0, payload text not null default '',
  created_at timestamptz not null default now(), primary key(room_id,sequence)
);
alter table public.brunio_browser_party_rooms_rp176 enable row level security;
alter table public.brunio_browser_party_events_rp176 enable row level security;
create policy browser_party_room_host on public.brunio_browser_party_rooms_rp176
  for all to authenticated using (host_user_id=auth.uid()) with check (host_user_id=auth.uid());
create policy browser_party_room_read on public.brunio_browser_party_rooms_rp176
  for select to anon,authenticated using (expires_at>now());
create policy browser_party_event_room on public.brunio_browser_party_events_rp176
  for select to anon,authenticated using (exists(select 1 from public.brunio_browser_party_rooms_rp176 r where r.room_id=room_id and r.expires_at>now()));
revoke insert,update,delete on public.brunio_browser_party_events_rp176 from anon,authenticated;
grant select on public.brunio_browser_party_rooms_rp176,public.brunio_browser_party_events_rp176 to anon,authenticated;
commit;
