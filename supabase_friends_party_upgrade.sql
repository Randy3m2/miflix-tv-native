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
create or replace function public.miflix_friend_activity() returns table(user_id uuid,nickname text,title text,season integer,episode integer,room_code text,online boolean)
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
