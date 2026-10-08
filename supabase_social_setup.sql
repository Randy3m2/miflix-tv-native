-- Run AFTER supabase_watch_party_repair.sql. No existing rooms/data are removed.
begin;
create table if not exists public.miflix_social_profiles (
 user_id uuid primary key references auth.users(id) on delete cascade,
 nickname text not null unique check(nickname ~ '^[a-z0-9_]{3,24}$'),
 updated_at timestamptz not null default now()
);
create table if not exists public.miflix_party_members (
 room_code text references public.miflix_watch_parties(room_code) on delete cascade,
 user_id uuid references auth.users(id) on delete cascade,
 joined_at timestamptz not null default now(), primary key(room_code,user_id)
);
create table if not exists public.miflix_party_events (
 id bigint generated always as identity primary key,
 room_code text not null references public.miflix_watch_parties(room_code) on delete cascade,
 user_id uuid not null references auth.users(id) on delete cascade,
 kind text not null check(kind in ('emoji','phrase','chat')),
 body text not null check(length(body) between 1 and 400),
 created_at timestamptz not null default now()
);
create index if not exists miflix_party_events_room_idx on public.miflix_party_events(room_code,id);
create table if not exists public.miflix_friendships (
 sender_id uuid references auth.users(id) on delete cascade,
 receiver_id uuid references auth.users(id) on delete cascade,
 status text not null default 'pending' check(status in ('pending','accepted')),
 created_at timestamptz not null default now(), primary key(sender_id,receiver_id),
 check(sender_id <> receiver_id)
);
create unique index if not exists miflix_friends_pair_idx on public.miflix_friendships
 (least(sender_id,receiver_id),greatest(sender_id,receiver_id));
-- Definer helper avoids recursive policies; it can only answer for the caller.
create or replace function public.miflix_in_room(code text) returns boolean
language sql stable security definer set search_path = public as $$
 select exists(select 1 from miflix_watch_parties p where p.room_code=code and p.expires_at>now()
 and (p.host_user_id=auth.uid() or exists(select 1 from miflix_party_members m where m.room_code=code and m.user_id=auth.uid())));
$$;
revoke all on function public.miflix_in_room(text) from public;
grant execute on function public.miflix_in_room(text) to authenticated;
create or replace function public.miflix_join_room(code text) returns void
language plpgsql security definer set search_path = public as $$
begin
 if auth.uid() is null then raise exception 'Sign in first'; end if;
 if not exists(select 1 from miflix_watch_parties where room_code=code and expires_at>now()) then
  raise exception 'Room not found or expired'; end if;
 insert into miflix_party_members(room_code,user_id) values(code,auth.uid()) on conflict do nothing;
end $$;
revoke all on function public.miflix_join_room(text) from public;
grant execute on function public.miflix_join_room(text) to authenticated;
-- Room metadata is visible only after joining by code (or to the host).
drop policy if exists "watch parties read" on public.miflix_watch_parties;
drop policy if exists "miflix party read authenticated" on public.miflix_watch_parties;
create policy "watch parties read" on public.miflix_watch_parties for select to authenticated using(public.miflix_in_room(room_code));
alter table public.miflix_social_profiles enable row level security;
alter table public.miflix_party_members enable row level security;
alter table public.miflix_party_events enable row level security;
alter table public.miflix_friendships enable row level security;
drop policy if exists "social profile read" on public.miflix_social_profiles;
create policy "social profile read" on public.miflix_social_profiles for select to authenticated using(true);
drop policy if exists "social profile insert" on public.miflix_social_profiles;
create policy "social profile insert" on public.miflix_social_profiles for insert to authenticated with check(user_id=auth.uid());
drop policy if exists "social profile update" on public.miflix_social_profiles;
create policy "social profile update" on public.miflix_social_profiles for update to authenticated using(user_id=auth.uid()) with check(user_id=auth.uid());
drop policy if exists "room members read" on public.miflix_party_members;
create policy "room members read" on public.miflix_party_members for select to authenticated using(public.miflix_in_room(room_code));
drop policy if exists "room members leave" on public.miflix_party_members;
create policy "room members leave" on public.miflix_party_members for delete to authenticated using(user_id=auth.uid());
drop policy if exists "room events read" on public.miflix_party_events;
create policy "room events read" on public.miflix_party_events for select to authenticated using(public.miflix_in_room(room_code));
drop policy if exists "room events send" on public.miflix_party_events;
create policy "room events send" on public.miflix_party_events for insert to authenticated with check(user_id=auth.uid() and public.miflix_in_room(room_code));
drop policy if exists "friends read" on public.miflix_friendships;
create policy "friends read" on public.miflix_friendships for select to authenticated using(auth.uid() in (sender_id,receiver_id));
drop policy if exists "friends request" on public.miflix_friendships;
create policy "friends request" on public.miflix_friendships for insert to authenticated with check(sender_id=auth.uid() and status='pending');
drop policy if exists "friends accept" on public.miflix_friendships;
create policy "friends accept" on public.miflix_friendships for update to authenticated using(receiver_id=auth.uid() and status='pending') with check(receiver_id=auth.uid() and status='accepted');
drop policy if exists "friends delete" on public.miflix_friendships;
create policy "friends delete" on public.miflix_friendships for delete to authenticated using(auth.uid() in (sender_id,receiver_id));
-- Prevent a recipient from changing either identity while accepting a request.
create or replace function public.miflix_friend_identity_guard() returns trigger
language plpgsql set search_path = public as $$
begin
 if new.sender_id<>old.sender_id or new.receiver_id<>old.receiver_id then raise exception 'Friend identities cannot change'; end if;
 return new;
end $$;
drop trigger if exists miflix_friend_identity_guard on public.miflix_friendships;
create trigger miflix_friend_identity_guard before update on public.miflix_friendships for each row execute function public.miflix_friend_identity_guard();
grant select,insert,update on public.miflix_social_profiles to authenticated;
grant select,delete on public.miflix_party_members to authenticated;
grant select,insert on public.miflix_party_events to authenticated;
grant usage,select on sequence public.miflix_party_events_id_seq to authenticated;
grant select,insert,update,delete on public.miflix_friendships to authenticated;
-- Trakt credentials are only accessible by the Edge Function's service role.
create table if not exists public.miflix_trakt_tokens (
 user_id uuid primary key references auth.users(id) on delete cascade,
 tokens jsonb not null, updated_at timestamptz not null default now()
);
alter table public.miflix_trakt_tokens enable row level security;
revoke all on public.miflix_trakt_tokens from anon,authenticated;
grant all on public.miflix_trakt_tokens to service_role;
-- OAuth pending codes and idempotency records are server-only.
create table if not exists public.miflix_trakt_pending (
 user_id uuid primary key references auth.users(id) on delete cascade, state jsonb not null
);
create table if not exists public.miflix_trakt_sent (
 user_id uuid references auth.users(id) on delete cascade,event_id text not null,created_at timestamptz default now(),primary key(user_id,event_id)
);
alter table public.miflix_trakt_pending enable row level security;
alter table public.miflix_trakt_sent enable row level security;
revoke all on public.miflix_trakt_pending,public.miflix_trakt_sent from anon,authenticated;
grant all on public.miflix_trakt_pending,public.miflix_trakt_sent to service_role;
notify pgrst,'reload schema';

commit;
