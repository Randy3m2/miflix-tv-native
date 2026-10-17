-- Run once in Supabase SQL Editor BEFORE installing RC70. Safe to run again.
-- No cron, scheduled cleanup, new reads, or changes to party synchronization.
begin;
alter table public.miflix_presence add column if not exists expires_at timestamptz;
alter table public.miflix_presence add column if not exists event_device text;
update public.miflix_presence set expires_at=updated_at+interval '7 minutes' where expires_at is null;

create table if not exists public.brunio_presence_cursors (
 user_id uuid not null references auth.users(id) on delete cascade,
 device text not null, seq bigint not null, primary key(user_id,device)
);
alter table public.brunio_presence_cursors enable row level security;
revoke all on public.brunio_presence_cursors from public,anon,authenticated;

create or replace function public.brunio_presence_event(
 active boolean,title text,media text,s integer,e integer,room text,device text,seq bigint,expiry timestamptz
) returns void language plpgsql security definer set search_path=public as $$
declare caller uuid:=auth.uid();
begin
 if caller is null then raise exception 'Sign in first'; end if;
 if device is null or length(device) not between 1 and 100 or seq is null or seq<1 then
  raise exception 'Invalid event';
 end if;
 -- Serialize per account, including events arriving concurrently from separate devices.
 perform pg_advisory_xact_lock(hashtextextended(caller::text,70));
 insert into brunio_presence_cursors as c(user_id,device,seq) values(caller,device,seq)
 on conflict on constraint brunio_presence_cursors_pkey do update set seq=excluded.seq where c.seq<excluded.seq;
 if not found then return; end if; -- Retried/older events cannot resurrect a stale title.
 if not coalesce(active,false) then
  update miflix_presence set title='',cloud_id='',season=0,episode=0,room_code=null,
   updated_at=now(),expires_at=now()
   where user_id=caller and (event_device=device or event_device is null);
  return;
 end if;
 if expiry is null or expiry<=now() then return; end if;
 if room is not null and not public.miflix_in_room(room) then raise exception 'Join the room first'; end if;
 insert into miflix_presence(user_id,title,cloud_id,season,episode,room_code,updated_at,expires_at,event_device)
 values(caller,left(coalesce(title,''),200),left(coalesce(media,''),100),coalesce(s,0),coalesce(e,0),
  room,now(),least(expiry,now()+interval '12 hours'),device)
 on conflict(user_id) do update set title=excluded.title,cloud_id=excluded.cloud_id,
  season=excluded.season,episode=excluded.episode,room_code=excluded.room_code,updated_at=now(),
  expires_at=excluded.expires_at,event_device=excluded.event_device;
end $$;
revoke all on function public.brunio_presence_event(boolean,text,text,integer,integer,text,text,bigint,timestamptz) from public,anon;
grant execute on function public.brunio_presence_event(boolean,text,text,integer,integer,text,text,bigint,timestamptz) to authenticated;

-- Keep the legacy writer compatible with older APKs. It retains its old 7-minute lease.
create or replace function public.miflix_set_presence(title text,media text,s integer,e integer,room text)
returns void language plpgsql security definer set search_path=public as $$
begin
 if auth.uid() is null then raise exception 'Sign in first'; end if;
 if room is not null and not public.miflix_in_room(room) then raise exception 'Join the room first'; end if;
 insert into miflix_presence(user_id,title,cloud_id,season,episode,room_code,updated_at,expires_at,event_device)
 values(auth.uid(),left(coalesce(title,''),200),left(coalesce(media,''),100),s,e,room,now(),now()+interval '7 minutes',null)
 on conflict(user_id) do update set title=excluded.title,cloud_id=excluded.cloud_id,season=excluded.season,
 episode=excluded.episode,room_code=excluded.room_code,updated_at=now(),expires_at=excluded.expires_at,event_device=null;
end $$;

-- Preserve each installed RPC's exact return type, security and grants (no DROP FUNCTION).
-- Only replace presence freshness. Party room heartbeat/expiry expressions stay untouched.
do $migration$
declare rpc text; definition text; updated text; rpc_oid oid;
begin
 foreach rpc in array array['miflix_friend_activity','miflix_friend_activity_v30'] loop
  rpc_oid:=to_regprocedure(format('public.%I()',rpc));
  if rpc_oid is null then raise exception 'Missing required RPC: %',rpc; end if;
  definition:=pg_get_functiondef(rpc_oid);
  updated:=regexp_replace(definition,
   'a\.updated_at\s*>\s*now\(\)\s*-\s*interval\s*''(90 seconds|7 minutes)''',
   'a.expires_at > now()','gi');
  if updated=definition and definition !~* 'a\.expires_at\s*>\s*now\(\)' then
   raise exception 'Unexpected presence expression in %; transaction rolled back',rpc;
  end if;
  execute updated;
 end loop;
end $migration$;
notify pgrst,'reload schema';
commit;
