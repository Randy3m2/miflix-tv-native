-- Run once before installing RC73. Repeatable; no cron, polling or video storage.
begin;
create table if not exists public.brunio_shared_moments (
 id uuid primary key,
 sender_id uuid not null references auth.users(id) on delete cascade,
 recipient_id uuid not null references auth.users(id) on delete cascade,
 cloud_id text not null check(cloud_id ~ '^tmdb:(movie|series):[0-9]+$'),
 title text not null check(length(title) between 1 and 200),
 season integer not null default 0, episode integer not null default 0,
 start_ms bigint not null, end_ms bigint not null,
 created_at timestamptz not null default now(),
 check(sender_id<>recipient_id),
 check(start_ms>=0 and end_ms>start_ms and end_ms<=86400000),
 check((cloud_id like 'tmdb:movie:%' and season=0 and episode=0) or
       (cloud_id like 'tmdb:series:%' and season>0 and episode>0))
);
create index if not exists brunio_moments_recipient_date on public.brunio_shared_moments(recipient_id,created_at desc);
alter table public.brunio_shared_moments enable row level security;
revoke all on public.brunio_shared_moments from public,anon,authenticated;
grant select,insert,delete on public.brunio_shared_moments to authenticated;
drop policy if exists moments_send on public.brunio_shared_moments;
create policy moments_send on public.brunio_shared_moments for insert to authenticated
 with check(sender_id=auth.uid() and public.miflix_friends_with(recipient_id));
drop policy if exists moments_read on public.brunio_shared_moments;
create policy moments_read on public.brunio_shared_moments for select to authenticated
 using(sender_id=auth.uid() or (recipient_id=auth.uid() and public.miflix_friends_with(sender_id)));
drop policy if exists moments_delete on public.brunio_shared_moments;
create policy moments_delete on public.brunio_shared_moments for delete to authenticated
 using(recipient_id=auth.uid());
create or replace function public.brunio_received_moments()
returns table(id uuid,sender_id uuid,nickname text,cloud_id text,title text,season integer,episode integer,
 start_ms bigint,end_ms bigint,created_at timestamptz)
language sql stable security invoker set search_path=public as $$
 select m.id,m.sender_id,coalesce(p.nickname,'Friend'),m.cloud_id,m.title,m.season,m.episode,
 m.start_ms,m.end_ms,m.created_at
 from public.brunio_shared_moments m left join public.miflix_social_profiles p on p.user_id=m.sender_id
 where m.recipient_id=auth.uid() order by m.created_at desc,m.id limit 100;
$$;
revoke all on function public.brunio_received_moments() from public,anon;
grant execute on function public.brunio_received_moments() to authenticated;
notify pgrst,'reload schema';
commit;
