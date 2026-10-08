-- RC6: requires the existing account table and RC5 rating migration.
-- Atomic profile removal; the caller can only modify their own account.
create or replace function public.miflix_delete_profile(target_profile text)
returns jsonb language plpgsql security definer set search_path=public
as $$
declare
 me uuid:=auth.uid(); account_state jsonb; kept jsonb; count_left integer;
begin
 if me is null then raise exception 'Sign in first'; end if;
 if target_profile is null or target_profile in ('','__account__') then raise exception 'Invalid profile'; end if;
 select state into account_state from public.miflix_user_state
 where user_id=me and profile_id='__account__' for update;
 if account_state is null then raise exception 'Account not found'; end if;
 if not exists(select 1 from jsonb_array_elements(account_state->'profiles') p where p->>'id'=target_profile) then
   raise exception 'Profile not found';
 end if;
 select coalesce(jsonb_agg(p order by ord),'[]'::jsonb) into kept
 from jsonb_array_elements(account_state->'profiles') with ordinality as x(p,ord)
 where p->>'id'<>target_profile;
 count_left:=jsonb_array_length(kept);
 if count_left<1 then raise exception 'Keep at least one profile'; end if;
 if not exists(select 1 from jsonb_array_elements(kept) p where p->>'primary'='true') then
   kept:=jsonb_set(kept,'{0,primary}','true'::jsonb);
 end if;
 update public.miflix_user_state set state=jsonb_set(account_state,'{profiles}',kept),updated_at=now()
 where user_id=me and profile_id='__account__';
 delete from public.miflix_user_state where user_id=me and profile_id=target_profile;
 delete from public.miflix_ratings where user_id=me and profile_id=target_profile;
 return kept;
end $$;
revoke all on function public.miflix_delete_profile(text) from public,anon;
grant execute on function public.miflix_delete_profile(text) to authenticated;
