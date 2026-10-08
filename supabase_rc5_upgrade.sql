-- Includes RC3/RC4 upgrades; run after RC2 final upgrade.
-- Includes RC3 Friends/approval setup. Run after RC2 final upgrade.
-- Run after supabase_final_upgrade.sql. Existing members keep access.
begin;
create table if not exists public.miflix_presence (
 user_id uuid primary key references auth.users(id) on delete cascade,
 title text not null default '', cloud_id text not null default '',
 season integer not null default 0, episode integer not null default 0,
 room_code text, updated_at timestamptz not null default now()
);
create table if not exists public.miflix_party_access (
 room_code text references public.miflix_watch_parties(room_code) on delete cascade,
 user_id uuid references auth.users(id) on delete cascade,
 status text not null default 'pending' check(status in ('pending','approved','rejected')),
 created_at timestamptz not null default now(), primary key(room_code,user_id)
);
alter table public.miflix_presence enable row level security;
alter table public.miflix_party_access enable row level security;
revoke all on public.miflix_presence,public.miflix_party_access from anon,authenticated;
create or replace function public.miflix_friends_with(other uuid) returns boolean
language sql stable security definer set search_path=public as $$
 select auth.uid() is not null and exists(select 1 from miflix_friendships
 where status='accepted' and ((sender_id=auth.uid() and receiver_id=other) or (receiver_id=auth.uid() and sender_id=other)));
$$;
-- Presence expires after 90 seconds without a heartbeat. Only caller's activity can be written.
create or replace function public.miflix_set_presence(title text,media text,s integer,e integer,room text) returns void
language plpgsql security definer set search_path=public as $$
begin
 if auth.uid() is null then raise exception 'Sign in first'; end if;
 if room is not null and not public.miflix_in_room(room) then raise exception 'Join the room first'; end if;
 insert into miflix_presence(user_id,title,cloud_id,season,episode,room_code,updated_at)
 values(auth.uid(),left(coalesce(title,''),200),left(coalesce(media,''),100),s,e,room,now())
 on conflict(user_id) do update set title=excluded.title,cloud_id=excluded.cloud_id,season=excluded.season,episode=excluded.episode,room_code=excluded.room_code,updated_at=now();
end $$;
drop function if exists public.miflix_friend_activity();
create function public.miflix_friend_activity() returns table(user_id uuid,nickname text,title text,season integer,episode integer,room_code text,online boolean)
language sql stable security definer set search_path=public as $$
 select p.user_id,p.nickname,case when a.updated_at>now()-interval '90 seconds' then a.title else '' end,
 coalesce(a.season,0),coalesce(a.episode,0),
 case when a.updated_at>now()-interval '90 seconds' and w.updated_at>now()-interval '90 seconds' and w.expires_at>now() then w.room_code end,
 coalesce(a.updated_at>now()-interval '90 seconds',false)
 from miflix_social_profiles p left join miflix_presence a on a.user_id=p.user_id
 left join miflix_watch_parties w on w.room_code=a.room_code
 where public.miflix_friends_with(p.user_id);
$$;
-- A room code/QR permits requesting access, never admission without approval.
create or replace function public.miflix_request_access(code text) returns text
language plpgsql security definer set search_path=public as $$
declare decision text;
begin
 if auth.uid() is null then raise exception 'Sign in first'; end if;
 perform 1 from miflix_watch_parties where room_code=code and expires_at>now() for update;
 if not found then raise exception 'Room closed or expired'; end if;
 if public.miflix_in_room(code) then return 'approved'; end if;
 insert into miflix_party_access(room_code,user_id) values(code,auth.uid()) on conflict do nothing;
 select status into decision from miflix_party_access where room_code=code and user_id=auth.uid();
 if decision='approved' then insert into miflix_party_members(room_code,user_id) values(code,auth.uid()) on conflict do nothing; end if;
 return decision;
end $$;
create or replace function public.miflix_join_room(code text) returns void
language plpgsql security definer set search_path=public as $$
begin
 if auth.uid() is null then raise exception 'Sign in first'; end if;
 if not public.miflix_in_room(code) then raise exception 'Request access and wait for host approval'; end if;
 insert into miflix_party_members(room_code,user_id) values(code,auth.uid()) on conflict do nothing;
end $$;
create or replace function public.miflix_access_requests() returns table(room_code text,user_id uuid,nickname text,status text)
language sql stable security definer set search_path=public as $$
 select r.room_code,r.user_id,coalesce(p.nickname,'Guest'),r.status
 from miflix_party_access r join miflix_watch_parties w on w.room_code=r.room_code
 left join miflix_social_profiles p on p.user_id=r.user_id
 where w.host_user_id=auth.uid() and w.expires_at>now() and r.status='pending' order by r.created_at;
$$;
create or replace function public.miflix_decide_access(code text,guest uuid,approve boolean) returns void
language plpgsql security definer set search_path=public as $$
begin
 perform 1 from miflix_watch_parties where room_code=code and host_user_id=auth.uid() and expires_at>now() for update;
 if not found then raise exception 'Only the active host can approve access'; end if;
 update miflix_party_access set status=case when approve then 'approved' else 'rejected' end
 where room_code=code and user_id=guest and status='pending';
 if not found then raise exception 'No pending request'; end if;
 if approve then insert into miflix_party_members(room_code,user_id) values(code,guest) on conflict do nothing; end if;
end $$;
revoke all on function public.miflix_friends_with(uuid),public.miflix_set_presence(text,text,integer,integer,text),public.miflix_friend_activity(),public.miflix_request_access(text),public.miflix_access_requests(),public.miflix_decide_access(text,uuid,boolean),public.miflix_join_room(text) from public;
grant execute on function public.miflix_friends_with(uuid),public.miflix_set_presence(text,text,integer,integer,text),public.miflix_friend_activity(),public.miflix_request_access(text),public.miflix_access_requests(),public.miflix_decide_access(text,uuid,boolean),public.miflix_join_room(text) to authenticated;
notify pgrst,'reload schema';
commit;

-- Guest pause and resume share the same private queue; the newest request wins.
begin;
alter table public.miflix_party_pause_requests add column if not exists playing boolean not null default false;
create or replace function public.miflix_request_playback(code text,media text,s integer,e integer,playing boolean)
returns void language plpgsql security definer set search_path=public as $$
begin
 if auth.uid() is null or not public.miflix_in_room(code) then raise exception 'Join the room first'; end if;
 perform 1 from miflix_watch_parties p where p.room_code=code and p.expires_at>now()
 and coalesce(p.state->>'cloudId','')=media and coalesce((p.state->>'season')::integer,0)=s
 and coalesce((p.state->>'episode')::integer,0)=e for update;
 if not found then raise exception 'Content changed; retry'; end if;
 if playing is null then raise exception 'Playback state required'; end if;
 insert into miflix_party_pause_requests(room_code,user_id,media,season,episode,playing)
 values(code,auth.uid(),media,s,e,playing)
 on conflict(room_code) do update set user_id=excluded.user_id,media=excluded.media,
 season=excluded.season,episode=excluded.episode,playing=excluded.playing,created_at=now();
end $$;
create or replace function public.miflix_request_pause(code text,media text,s integer,e integer)
returns void language sql security definer set search_path=public as $$
 select public.miflix_request_playback(code,media,s,e,false);
$$;
-- NULL means no current request; false is a real pause request.
create or replace function public.miflix_take_playback(code text)
returns boolean language plpgsql security definer set search_path=public as $$
declare request public.miflix_party_pause_requests; room public.miflix_watch_parties;
begin
 select * into room from miflix_watch_parties where room_code=code and host_user_id=auth.uid() and expires_at>now() for update;
 if not found then raise exception 'Only the host can consume playback requests'; end if;
 delete from miflix_party_pause_requests where room_code=code returning * into request;
 if not found then return null; end if;
 if request.created_at>now()-interval '30 seconds'
 and public.miflix_in_room(code) and exists(select 1 from miflix_party_members where room_code=code and user_id=request.user_id)
 and request.media=coalesce(room.state->>'cloudId','')
 and request.season=coalesce((room.state->>'season')::integer,0)
 and request.episode=coalesce((room.state->>'episode')::integer,0)
 then return request.playing; end if;
 return null;
end $$;
-- Old hosts can consume pauses, but must upgrade to handle resume.
create or replace function public.miflix_take_pause(code text)
returns boolean language plpgsql security definer set search_path=public as $$
declare request public.miflix_party_pause_requests; room public.miflix_watch_parties;
begin
 select * into room from miflix_watch_parties where room_code=code and host_user_id=auth.uid() and expires_at>now() for update;
 if not found then raise exception 'Only the host can consume pause requests'; end if;
 delete from miflix_party_pause_requests where room_code=code and playing=false returning * into request;
 if not found then return false; end if;
 return request.created_at>now()-interval '30 seconds'
 and request.media=coalesce(room.state->>'cloudId','')
 and request.season=coalesce((room.state->>'season')::integer,0)
 and request.episode=coalesce((room.state->>'episode')::integer,0);
end $$;
revoke all on function public.miflix_request_playback(text,text,integer,integer,boolean),public.miflix_take_playback(text),public.miflix_request_pause(text,text,integer,integer),public.miflix_take_pause(text) from public;
grant execute on function public.miflix_request_playback(text,text,integer,integer,boolean),public.miflix_take_playback(text),public.miflix_request_pause(text,text,integer,integer),public.miflix_take_pause(text) to authenticated;
notify pgrst,'reload schema';
commit;

begin;
alter table public.miflix_social_profiles add column if not exists avatar_value text;
create table if not exists public.miflix_ratings (
 user_id uuid not null references auth.users(id) on delete cascade,
 profile_id text not null check(length(profile_id) between 1 and 100),
 cloud_id text not null check(cloud_id ~ '^tmdb:(movie|series):[0-9]+$'),
 score integer not null check(score between 1 and 10),
 primary key(user_id,profile_id,cloud_id)
);
alter table public.miflix_ratings enable row level security;
drop policy if exists "ratings own" on public.miflix_ratings;
create policy "ratings own" on public.miflix_ratings for all to authenticated using(user_id=auth.uid()) with check(user_id=auth.uid());
grant select,insert,update,delete on public.miflix_ratings to authenticated;
-- Activity includes the user's selected avatar, visible only to accepted friends.
drop function if exists public.miflix_friend_activity();
create function public.miflix_friend_activity() returns table(user_id uuid,nickname text,title text,season integer,episode integer,room_code text,online boolean,avatar_value text)
language sql stable security definer set search_path=public as $$
 select p.user_id,p.nickname,case when a.updated_at>now()-interval '90 seconds' then a.title else '' end,
 coalesce(a.season,0),coalesce(a.episode,0),
 case when a.updated_at>now()-interval '90 seconds' and w.updated_at>now()-interval '90 seconds' and w.expires_at>now() then w.room_code end,
 coalesce(a.updated_at>now()-interval '90 seconds',false),p.avatar_value
 from miflix_social_profiles p left join miflix_presence a on a.user_id=p.user_id
 left join miflix_watch_parties w on w.room_code=a.room_code where public.miflix_friends_with(p.user_id);
$$;
revoke all on function public.miflix_friend_activity() from public;
grant execute on function public.miflix_friend_activity() to authenticated;
notify pgrst,'reload schema';
commit;
