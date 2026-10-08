-- Apply after supabase_party_social_upgrade.sql. Repeatable; does not delete rooms.
begin;
drop policy if exists "watch parties read" on public.miflix_watch_parties;
create policy "watch parties read" on public.miflix_watch_parties for select to authenticated
using(host_user_id = auth.uid() or public.miflix_in_room(room_code));

-- Private queue: guests can request pause only through the validated RPC.
create table if not exists public.miflix_party_pause_requests (
 room_code text primary key references public.miflix_watch_parties(room_code) on delete cascade,
 user_id uuid not null references auth.users(id) on delete cascade,
 media text not null, season integer not null, episode integer not null,
 created_at timestamptz not null default now()
);
alter table public.miflix_party_pause_requests enable row level security;
revoke all on public.miflix_party_pause_requests from anon,authenticated;

create or replace function public.miflix_request_pause(code text,media text,s integer,e integer)
returns void language plpgsql security definer set search_path=public as $$
begin
 if auth.uid() is null or not public.miflix_in_room(code) then raise exception 'Join the room first'; end if;
 if not exists(select 1 from miflix_watch_parties p where p.room_code=code
   and p.expires_at>now() and coalesce(p.state->>'cloudId','')=media
   and coalesce((p.state->>'season')::integer,0)=s
   and coalesce((p.state->>'episode')::integer,0)=e) then raise exception 'Content changed; retry'; end if;
 insert into miflix_party_pause_requests(room_code,user_id,media,season,episode)
 values(code,auth.uid(),media,s,e)
 on conflict(room_code) do update set user_id=excluded.user_id,media=excluded.media,
 season=excluded.season,episode=excluded.episode,created_at=now();
end $$;

create or replace function public.miflix_take_pause(code text)
returns boolean language plpgsql security definer set search_path=public as $$
declare request public.miflix_party_pause_requests; room public.miflix_watch_parties;
begin
 select * into room from miflix_watch_parties where room_code=code and host_user_id=auth.uid() and expires_at>now();
 if not found then raise exception 'Only the host can consume pause requests'; end if;
 delete from miflix_party_pause_requests where room_code=code returning * into request;
 if not found then return false; end if;
 return request.created_at>now()-interval '30 seconds'
 and request.media=coalesce(room.state->>'cloudId','')
 and request.season=coalesce((room.state->>'season')::integer,0)
 and request.episode=coalesce((room.state->>'episode')::integer,0);
end $$;
revoke all on function public.miflix_request_pause(text,text,integer,integer) from public;
revoke all on function public.miflix_take_pause(text) from public;
grant execute on function public.miflix_request_pause(text,text,integer,integer) to authenticated;
grant execute on function public.miflix_take_pause(text) to authenticated;
notify pgrst,'reload schema';
commit;
