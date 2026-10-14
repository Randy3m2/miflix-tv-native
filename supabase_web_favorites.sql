begin;
-- Atomic favorite deltas let web and TV preserve each other's unrelated changes.
create or replace function public.brunio_sync_profile(
 p_profile text, p_adds jsonb default '[]'::jsonb,
 p_removes jsonb default '[]'::jsonb, p_progress jsonb default null
) returns jsonb language plpgsql security invoker set search_path='' as $$
declare
 me uuid:=auth.uid(); previous jsonb; favorites jsonb; entry record;
 merged_progress jsonb; next_state jsonb;
begin
 if me is null then raise exception 'Sign in first'; end if;
 if p_profile is null or length(p_profile)>100 or p_profile in ('','__account__') then raise exception 'Invalid profile'; end if;
 if jsonb_typeof(p_adds) is distinct from 'array' or jsonb_typeof(p_removes) is distinct from 'array' then raise exception 'Invalid favorite deltas'; end if;
 if jsonb_array_length(p_adds)>10000 or jsonb_array_length(p_removes)>10000 then raise exception 'Too many favorites'; end if;
 if exists(select 1 from jsonb_array_elements_text(p_adds || p_removes) as changes(value) where value is null or value !~ '^tmdb:(movie|series):[0-9]+$') then raise exception 'Invalid title'; end if;
 if p_progress is not null and jsonb_typeof(p_progress) is distinct from 'object' then raise exception 'Invalid progress'; end if;
 insert into public.miflix_user_state(user_id,profile_id,state,updated_at)
 values(me,p_profile,'{"favorites":[],"progress":{}}'::jsonb,now())
 on conflict(user_id,profile_id) do nothing;
 select state into previous from public.miflix_user_state where user_id=me and profile_id=p_profile for update;
 if previous is null then raise exception 'Profile inaccessible'; end if;
 select coalesce(jsonb_agg(value order by ordinal),'[]'::jsonb) into favorites from (
  select value,min(ordinal) ordinal from jsonb_array_elements_text(
   p_adds || case when jsonb_typeof(previous->'favorites')='array' then previous->'favorites' else '[]'::jsonb end
  ) with ordinality as all_favorites(value,ordinal)
  where not (p_removes ? value) group by value
 ) kept;
 merged_progress:=case when jsonb_typeof(previous->'progress')='object' then previous->'progress' else '{}'::jsonb end;
 if p_progress is not null then
  for entry in select key,value from jsonb_each(p_progress) loop
   if jsonb_typeof(entry.value)='object' and coalesce((entry.value->>'updatedAt')::numeric,0)>=coalesce((merged_progress->entry.key->>'updatedAt')::numeric,0) then
    merged_progress:=jsonb_set(merged_progress,array[entry.key],entry.value,true);
   end if;
  end loop;
 end if;
 next_state:=previous || jsonb_build_object('favorites',favorites,'progress',merged_progress,'updatedAt',floor(extract(epoch from now())*1000));
 update public.miflix_user_state set state=next_state,updated_at=now() where user_id=me and profile_id=p_profile;
 return next_state;
end $$;
revoke all on function public.brunio_sync_profile(text,jsonb,jsonb,jsonb) from public,anon;
grant execute on function public.brunio_sync_profile(text,jsonb,jsonb,jsonb) to authenticated;
notify pgrst,'reload schema';
commit;
