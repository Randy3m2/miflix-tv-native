-- RC30: run once in Supabase SQL Editor, after your existing upgrades.
-- Idempotent; no account settings, add-ons, playback history or parties are removed.
begin;
create table if not exists public.miflix_recommendations (
 owner_id uuid not null references auth.users(id) on delete cascade,
 cloud_id text not null check (cloud_id ~ '^tmdb:(movie|series):[0-9]+$'),
 title text not null check (char_length(title) between 1 and 500),
 poster text check (poster is null or char_length(poster)<=2048),
 created_at timestamptz not null default now(),
 primary key(owner_id,cloud_id)
);
alter table public.miflix_recommendations enable row level security;
revoke all on public.miflix_recommendations from public,anon,authenticated;
grant select,insert,update,delete on public.miflix_recommendations to authenticated;
drop policy if exists recommendations_read on public.miflix_recommendations;
create policy recommendations_read on public.miflix_recommendations for select to authenticated
 using(owner_id=auth.uid() or public.miflix_friends_with(owner_id));
drop policy if exists recommendations_insert on public.miflix_recommendations;
create policy recommendations_insert on public.miflix_recommendations for insert to authenticated with check(owner_id=auth.uid());
drop policy if exists recommendations_update on public.miflix_recommendations;
create policy recommendations_update on public.miflix_recommendations for update to authenticated
 using(owner_id=auth.uid()) with check(owner_id=auth.uid());
drop policy if exists recommendations_delete on public.miflix_recommendations;
create policy recommendations_delete on public.miflix_recommendations for delete to authenticated using(owner_id=auth.uid());

create or replace function public.miflix_friend_recommendations()
returns table(owner_id uuid,nickname text,cloud_id text,title text,poster text,created_at timestamptz)
language sql stable security definer set search_path=public as $$
 select r.owner_id,coalesce(p.nickname,'Friend'),r.cloud_id,r.title,r.poster,r.created_at
 from public.miflix_recommendations r left join public.miflix_social_profiles p on p.user_id=r.owner_id
 where auth.uid() is not null and (r.owner_id=auth.uid() or public.miflix_friends_with(r.owner_id))
 order by r.created_at desc,r.owner_id,r.cloud_id limit 500;
$$;
-- New endpoint preserves the existing RPC for older TV/PC versions.
-- Only accepted friends may see a current content identifier; no playback links are shared.
create or replace function public.miflix_friend_activity_v30()
returns table(user_id uuid,nickname text,title text,season integer,episode integer,room_code text,online boolean,avatar_value text,cloud_id text)
language sql stable security definer set search_path=public as $$
 select p.user_id,p.nickname,
 case when a.updated_at>now()-interval '90 seconds' then a.title else '' end,
 coalesce(a.season,0),coalesce(a.episode,0),
 case when a.updated_at>now()-interval '90 seconds' and w.updated_at>now()-interval '90 seconds' and w.expires_at>now() then w.room_code end,
 coalesce(a.updated_at>now()-interval '90 seconds',false),p.avatar_value,
 case when a.updated_at>now()-interval '90 seconds' then a.cloud_id else '' end
 from public.miflix_social_profiles p left join public.miflix_presence a on a.user_id=p.user_id
 left join public.miflix_watch_parties w on w.room_code=a.room_code
 where auth.uid() is not null and public.miflix_friends_with(p.user_id);
$$;
revoke all on function public.miflix_friend_recommendations(),public.miflix_friend_activity_v30() from public,anon;
grant execute on function public.miflix_friend_recommendations(),public.miflix_friend_activity_v30() to authenticated;
notify pgrst,'reload schema';
commit;
