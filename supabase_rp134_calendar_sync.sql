begin;
-- Owner RLS is required; this RPC never uses a service-role identity.
do $$ begin
 if not exists(select 1 from pg_class c join pg_namespace n on n.oid=c.relnamespace
 where n.nspname='public' and c.relname='miflix_user_state' and c.relrowsecurity) then
  raise exception 'miflix_user_state must have owner-only RLS enabled';
 end if;
end $$;
create or replace function public.brunio_calendar_sync_rp134(p_profile text,p_changes jsonb)
returns jsonb language plpgsql security invoker set search_path='' as $$
declare me uuid:=auth.uid(); account_state jsonb; merged jsonb; k text; v jsonb; old jsonb; row_id text;
begin
 if me is null then raise exception 'Sign in first'; end if;
 if p_profile is null or length(p_profile)>100 or p_profile='' or p_profile='__account__' then raise exception 'Invalid profile'; end if;
 if jsonb_typeof(p_changes) is distinct from 'object' or octet_length(p_changes::text)>250000 then raise exception 'Invalid subscriptions'; end if;
 select state into account_state from public.miflix_user_state where user_id=me and profile_id='__account__';
 if not exists(select 1 from jsonb_array_elements(coalesce(account_state->'profiles','[]'::jsonb)) p where p->>'id'=p_profile) then raise exception 'Profile does not belong to this account'; end if;
 row_id:='__calendar_rp134__:'||p_profile;
 insert into public.miflix_user_state(user_id,profile_id,state,updated_at)
 values(me,row_id,'{}'::jsonb,now()) on conflict(user_id,profile_id) do nothing;
 select state into merged from public.miflix_user_state where user_id=me and profile_id=row_id for update;
 if merged is null then raise exception 'Calendar inaccessible'; end if;
 for k,v in select key,value from jsonb_each(p_changes) loop
  if length(k)>200 or k='' or jsonb_typeof(v) is distinct from 'object' or
    jsonb_typeof(v->'enabled') is distinct from 'boolean' or
    coalesce(v->>'at','') !~ '^[0-9]{1,16}$' then raise exception 'Invalid subscription entry'; end if;
  old:=merged->k;
  if old is null or (v->>'at')::bigint>coalesce((old->>'at')::bigint,0) or
    ((v->>'at')::bigint=coalesce((old->>'at')::bigint,0) and not (v->>'enabled')::boolean) then
   merged:=jsonb_set(merged,array[k],jsonb_build_object('enabled',(v->>'enabled')::boolean,'at',(v->>'at')::bigint),true);
  end if;
 end loop;
 if octet_length(merged::text)>500000 then raise exception 'Too many subscriptions'; end if;
 update public.miflix_user_state set state=merged,updated_at=now() where user_id=me and profile_id=row_id;
 return merged;
end $$;
revoke all on function public.brunio_calendar_sync_rp134(text,jsonb) from public,anon;
grant execute on function public.brunio_calendar_sync_rp134(text,jsonb) to authenticated;
notify pgrst,'reload schema';
commit;
