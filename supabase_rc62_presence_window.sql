begin;
-- Five-minute presence heartbeats need a longer online window. Room liveness stays at 90 seconds.
create or replace function public.miflix_friend_activity() returns table(user_id uuid,nickname text,title text,season integer,episode integer,room_code text,online boolean)
language sql stable security definer set search_path=public as $$
 select p.user_id,p.nickname,case when a.updated_at>now()-interval '7 minutes' then a.title else '' end,
 coalesce(a.season,0),coalesce(a.episode,0),
 case when a.updated_at>now()-interval '7 minutes' and w.updated_at>now()-interval '90 seconds' and w.expires_at>now() then w.room_code end,
 coalesce(a.updated_at>now()-interval '7 minutes',false)
 from miflix_social_profiles p left join miflix_presence a on a.user_id=p.user_id
 left join miflix_watch_parties w on w.room_code=a.room_code
 where public.miflix_friends_with(p.user_id);
$$;
create or replace function public.miflix_friend_activity_v30()
returns table(user_id uuid,nickname text,title text,season integer,episode integer,room_code text,online boolean,avatar_value text,cloud_id text)
language sql stable security definer set search_path=public as $$
 select p.user_id,p.nickname,
 case when a.updated_at>now()-interval '7 minutes' then a.title else '' end,
 coalesce(a.season,0),coalesce(a.episode,0),
 case when a.updated_at>now()-interval '7 minutes' and w.updated_at>now()-interval '90 seconds' and w.expires_at>now() then w.room_code end,
 coalesce(a.updated_at>now()-interval '7 minutes',false),p.avatar_value,
 case when a.updated_at>now()-interval '7 minutes' then a.cloud_id else '' end
 from public.miflix_social_profiles p left join public.miflix_presence a on a.user_id=p.user_id
 left join public.miflix_watch_parties w on w.room_code=a.room_code
 where auth.uid() is not null and public.miflix_friends_with(p.user_id);
$$;
commit;
