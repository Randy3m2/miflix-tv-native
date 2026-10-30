-- RP162: same-account TV sessions, reusing RP1 event presence. Run once in SQL Editor.
-- Requires the already installed RP1 profile-social migration. No new table/cron/heartbeat.
begin;
create or replace function public.brunio_now_playing_rp162()
returns table(profile_id text,cloud_id text,title text,season integer,episode integer,valid_for_ms bigint)
language sql stable security definer set search_path=public as $$
 select coalesce(a.profile_id,''),p.cloud_id,p.title,p.season,p.episode,
   least(43200000,greatest(0,floor(extract(epoch from (p.expires_at-now()))*1000)))::bigint
 from public.rp1_miflix_presence p
 join public.brunio_rp1_actors a on a.id=p.user_id
 where auth.uid() is not null and a.account_id=auth.uid()
   and p.event_device like 'tv:%' and p.expires_at>now()
   and coalesce(p.title,'')<>'' and coalesce(p.cloud_id,'')<>''
 order by p.updated_at desc limit 12;
$$;
revoke all on function public.brunio_now_playing_rp162() from public,anon;
grant execute on function public.brunio_now_playing_rp162() to authenticated;
notify pgrst, 'reload schema';
commit;
