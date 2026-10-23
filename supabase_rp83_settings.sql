begin;
-- Existing miflix_user_state table and its account-owner RLS policies are reused.
-- No table/data is deleted. Run once in Supabase SQL Editor before this patch.
do $$ begin
 if not exists(select 1 from pg_class c join pg_namespace n on n.oid=c.relnamespace
 where n.nspname='public' and c.relname='miflix_user_state' and c.relrowsecurity) then
  raise exception 'miflix_user_state must already have owner-only RLS enabled';
 end if;
end $$;
create or replace function public.brunio_save_settings_rp83(p_profile text,p_settings jsonb,p_expected bigint)
returns bigint language plpgsql security invoker set search_path='' as $$
declare me uuid:=auth.uid(); previous jsonb; revision bigint; account_state jsonb;
begin
 if me is null then raise exception 'Sign in first'; end if;
 if p_profile is null or length(p_profile)>100 or p_profile in ('','__account__') then raise exception 'Invalid profile'; end if;
 if jsonb_typeof(p_settings) is distinct from 'object' or octet_length(p_settings::text)>2500000 then raise exception 'Invalid settings'; end if;
 if (p_settings->>'schema') is distinct from '83' then raise exception 'Unsupported settings schema'; end if;
 select state into account_state from public.miflix_user_state where user_id=me and profile_id='__account__';
 if not exists(select 1 from jsonb_array_elements(coalesce(account_state->'profiles','[]'::jsonb)) p where p->>'id'=p_profile) then raise exception 'Profile does not belong to this account'; end if;
 insert into public.miflix_user_state(user_id,profile_id,state,updated_at)
 values(me,p_profile,'{"favorites":[],"progress":{}}'::jsonb,now()) on conflict(user_id,profile_id) do nothing;
 select state into previous from public.miflix_user_state where user_id=me and profile_id=p_profile for update;
 if previous is null then raise exception 'Profile inaccessible'; end if;
 revision:=coalesce((previous->>'settingsRevisionRP83')::bigint,0);
 if p_expected is null or p_expected<>revision then raise exception 'Settings changed on another device; reload before saving'; end if;
 revision:=revision+1;
 update public.miflix_user_state set state=previous || jsonb_build_object('settingsRP83',p_settings,'settingsRevisionRP83',revision),updated_at=now()
 where user_id=me and profile_id=p_profile;
 -- Atomic field update preserves profiles, avatars and account setup.
 update public.miflix_user_state set state=coalesce(state,'{}'::jsonb) || jsonb_build_object('lastProfileRP83',p_profile),updated_at=now()
 where user_id=me and profile_id='__account__';
 return revision;
end $$;
revoke all on function public.brunio_save_settings_rp83(text,jsonb,bigint) from public,anon;
grant execute on function public.brunio_save_settings_rp83(text,jsonb,bigint) to authenticated;
notify pgrst,'reload schema';
commit;
