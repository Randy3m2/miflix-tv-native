-- RP93. Run ONCE in Supabase SQL Editor. Requires RP1 profile social migration.
-- Additive: no progress, friendships or profiles are deleted/rewritten.
-- Reads existing synced progress; no per-playback writes or polling.
begin;
create table if not exists public.brunio_watched_privacy_rp93 (
 actor_id uuid primary key references public.brunio_rp1_actors(id) on delete cascade,
 sharing boolean not null default true
);
alter table public.brunio_watched_privacy_rp93 enable row level security;
revoke all on public.brunio_watched_privacy_rp93 from public,anon,authenticated;
-- Only the narrowly scoped RPC can access this table. No client table grants.
create or replace function public.brunio_watched_rp93(p_cloud text default null,p_share boolean default null)
returns jsonb language plpgsql security definer set search_path=pg_catalog,public as $$
declare me uuid; result jsonb; enabled boolean;
begin
 me:=public.brunio_rp1_actor();
 if auth.uid() is null or me is null then raise exception 'Select a signed-in profile'; end if;
 if p_cloud is not null and (length(p_cloud)>80 or p_cloud !~ '^tmdb:(movie|series):[0-9]+$') then
  raise exception 'Invalid title';
 end if;
 if p_share is not null then
  insert into public.brunio_watched_privacy_rp93(actor_id,sharing) values(me,p_share)
  on conflict(actor_id) do update set sharing=excluded.sharing;
 end if;
 select coalesce((select sharing from public.brunio_watched_privacy_rp93 where actor_id=me),true) into enabled;
 if p_cloud is null then return jsonb_build_object('sharing',enabled,'friends','[]'::jsonb); end if;
 select coalesce(jsonb_agg(jsonb_build_object('id',x.id,'nickname',x.nickname,'avatar',x.avatar_value)
    order by x.nickname,x.id),'[]'::jsonb) into result
 from (
  select distinct a.id,sp.nickname,sp.avatar_value
  from public.rp1_miflix_friendships f
  join public.brunio_rp1_actors a on a.id=case when f.sender_id=me then f.receiver_id else f.sender_id end
  join public.rp1_miflix_social_profiles sp on sp.user_id=a.id
  join public.miflix_user_state us on us.user_id=a.account_id and us.profile_id=a.profile_id
  left join public.brunio_watched_privacy_rp93 privacy on privacy.actor_id=a.id
  where f.status='accepted' and (f.sender_id=me or f.receiver_id=me) and a.id<>me
   and coalesce(privacy.sharing,true)
   -- Exclude deleted/Kids profiles, even if an old actor row remains.
   and exists (
    select 1 from public.miflix_user_state account,
    lateral jsonb_array_elements(coalesce(account.state->'profiles','[]'::jsonb)) profile
    where account.user_id=a.account_id and account.profile_id='__account__'
      and profile->>'id'=a.profile_id and profile->>'id'<>'kids'
      and coalesce(profile->>'isKids','false')<>'true'
   )
   and exists (
    select 1 from jsonb_each(case when jsonb_typeof(us.state->'progress')='object' then us.state->'progress' else '{}'::jsonb end) progress
    where (progress.key=p_cloud or (p_cloud like 'tmdb:series:%' and progress.key like p_cloud||':s%e%'))
      and case when jsonb_typeof(progress.value->'percent')='number' then (progress.value->>'percent')::numeric>=96 else false end
   )
  order by sp.nickname,a.id limit 50
 ) x;
 return jsonb_build_object('sharing',enabled,'friends',result);
end $$;
revoke all on function public.brunio_watched_rp93(text,boolean) from public,anon;
grant execute on function public.brunio_watched_rp93(text,boolean) to authenticated;
commit;
