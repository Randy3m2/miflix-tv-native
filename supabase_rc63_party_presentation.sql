-- RC63: private shared sources, friend invitations, safe QR preview, host controls.
-- Additive and idempotent. Run after the existing Party/Friends setup.
begin;
create table if not exists public.miflix_party_sources (
 room_code text primary key references public.miflix_watch_parties(room_code) on delete cascade,
 cloud_id text not null, season integer not null default 0, episode integer not null default 0,
 source_key text not null, url text not null check(length(url)<=12000 and url ~ '^https?://'),
 request_headers jsonb not null default '{}'::jsonb check(jsonb_typeof(request_headers)='object' and octet_length(request_headers::text)<=16000),
 expires_at timestamptz not null
);
alter table public.miflix_party_sources enable row level security;
revoke all on public.miflix_party_sources from anon,authenticated;
grant select,insert,update,delete on public.miflix_party_sources to authenticated;
drop policy if exists "party source members read" on public.miflix_party_sources;
create policy "party source members read" on public.miflix_party_sources for select to authenticated
 using(expires_at>now() and public.miflix_in_room(room_code) and exists(
 select 1 from public.miflix_watch_parties w where w.room_code=miflix_party_sources.room_code
 and w.expires_at>now() and w.state->>'shareStream'='true'
 and w.state->>'cloudId'=miflix_party_sources.cloud_id
 and w.state->>'sourceKey'=miflix_party_sources.source_key
 and coalesce((w.state->>'season')::integer,0)=miflix_party_sources.season
 and coalesce((w.state->>'episode')::integer,0)=miflix_party_sources.episode));
drop policy if exists "party source host write" on public.miflix_party_sources;
create policy "party source host write" on public.miflix_party_sources for all to authenticated
 using(exists(select 1 from public.miflix_watch_parties w where w.room_code=miflix_party_sources.room_code and w.host_user_id=auth.uid() and w.expires_at>now()))
 with check(exists(select 1 from public.miflix_watch_parties w where w.room_code=miflix_party_sources.room_code and w.host_user_id=auth.uid() and w.expires_at>now()));

create table if not exists public.miflix_party_invites (
 room_code text references public.miflix_watch_parties(room_code) on delete cascade,
 recipient_id uuid references auth.users(id) on delete cascade,
 host_name text not null, title text not null, created_at timestamptz not null default now(),
 primary key(room_code,recipient_id)
);
alter table public.miflix_party_invites enable row level security;
revoke all on public.miflix_party_invites from anon,authenticated;
grant select on public.miflix_party_invites to authenticated;
drop policy if exists "party invites recipient read" on public.miflix_party_invites;
create policy "party invites recipient read" on public.miflix_party_invites for select to authenticated using(recipient_id=auth.uid() and created_at>now()-interval '8 hours');
create or replace function public.miflix_invite_party(code text,friend uuid) returns void
language plpgsql security definer set search_path=public as $$
declare w public.miflix_watch_parties; nick text;
begin
 select * into w from miflix_watch_parties p where p.room_code=$1 and p.host_user_id=auth.uid() and p.expires_at>now() for update;
 if not found then raise exception 'Only the active host can invite'; end if;
 if not public.miflix_friends_with($2) then raise exception 'Choose an accepted friend'; end if;
 select nickname into nick from miflix_social_profiles where user_id=auth.uid();
 insert into miflix_party_invites(room_code,recipient_id,host_name,title)
 values($1,$2,coalesce(nick,'Host'),left(coalesce(nullif(w.state->>'title',''),'Watch Party'),200))
 on conflict(room_code,recipient_id) do update set created_at=now(),title=excluded.title,host_name=excluded.host_name;
 -- A deliberate host invitation preapproves this friend, but they still choose Join.
 insert into miflix_party_access(room_code,user_id,status) values($1,$2,'approved')
 on conflict(room_code,user_id) do update set status='approved';
end $$;
revoke all on function public.miflix_invite_party(text,uuid) from public;
grant execute on function public.miflix_invite_party(text,uuid) to authenticated;

-- Code-only landing reveals presentation fields, never stream URLs, headers or account IDs.
create or replace function public.miflix_party_preview(code text) returns jsonb
language sql stable security definer set search_path=public as $$
 select coalesce((select jsonb_build_object('title',left(coalesce(nullif(w.state->>'title',''),'Watch Party'),200),
 'overview',left(coalesce(w.state->>'overview',''),800),'poster',case when w.state->>'poster' like 'https://image.tmdb.org/%' then w.state->>'poster' end,
 'logo',case when w.state->>'logo' like 'https://image.tmdb.org/%' then w.state->>'logo' end,
 'season',w.state->'season','episode',w.state->'episode','host',coalesce(p.nickname,'Host'),
 'members',(select count(*) from miflix_party_members m where m.room_code=w.room_code))
 from miflix_watch_parties w left join miflix_social_profiles p on p.user_id=w.host_user_id
 where $1 ~ '^[0-9]{6}$' and w.room_code=$1 and w.expires_at>now()),'{}'::jsonb);
$$;
revoke all on function public.miflix_party_preview(text) from public;
grant execute on function public.miflix_party_preview(text) to anon,authenticated;

create or replace function public.miflix_rc63_playback_guard() returns trigger
language plpgsql security definer set search_path=public as $$
begin
 if exists(select 1 from miflix_watch_parties w where w.room_code=new.room_code
 and w.host_user_id<>auth.uid() and w.state->>'hostOnlyControls'='true') then
  raise exception 'Only the host controls party playback';
 end if;
 return new;
end $$;
drop trigger if exists miflix_rc63_playback_guard on public.miflix_party_pause_requests;
create trigger miflix_rc63_playback_guard before insert or update on public.miflix_party_pause_requests
for each row execute function public.miflix_rc63_playback_guard();
create or replace function public.miflix_rc63_room_options() returns trigger
language plpgsql security definer set search_path=public as $$
begin
 if new.state->>'hostOnlyControls'='true' and old.state->>'hostOnlyControls' is distinct from 'true' then
  delete from miflix_party_pause_requests where room_code=new.room_code;
 end if;
 if new.state->>'shareStream' is distinct from 'true' and old.state->>'shareStream'='true' then
  delete from miflix_party_sources where room_code=new.room_code;
 end if;
 return new;
end $$;
drop trigger if exists miflix_rc63_room_options on public.miflix_watch_parties;
create trigger miflix_rc63_room_options after update of state on public.miflix_watch_parties
for each row execute function public.miflix_rc63_room_options();
revoke all on function public.miflix_rc63_playback_guard(),public.miflix_rc63_room_options() from public;
notify pgrst,'reload schema';
commit;
