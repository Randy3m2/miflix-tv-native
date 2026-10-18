-- RP1: Run in Supabase SQL Editor BEFORE installing RP1 on every participating TV.
-- Additive migration. Legacy tables/functions remain unchanged for older clients.
-- Social history is copied once to the primary profile; other profiles start independent.
begin;
create table if not exists public.brunio_rp1_actors (
 id uuid primary key,
 account_id uuid not null references auth.users(id) on delete cascade,
 profile_id text,
 primary_profile boolean not null,
 unique(account_id,profile_id)
);
alter table public.brunio_rp1_actors enable row level security;
revoke all on public.brunio_rp1_actors from public,anon,authenticated;
create unique index if not exists brunio_rp1_primary_account on public.brunio_rp1_actors(account_id) where primary_profile;

-- Java UUID.nameUUIDFromBytes / UUID v3, same UTF-8 input on every device.
create or replace function public.brunio_rp1_id(account uuid,profile text) returns uuid
language sql immutable strict set search_path=public as $$
 select (substr(h,1,12)||'3'||substr(h,14,3)||to_hex((('x'||substr(h,17,1))::bit(4)::int & 3) | 8)||substr(h,18))::uuid
 from (select md5('brunio-rp1:'||account::text||':'||profile) h) x;
$$;

create or replace function public.brunio_rp1_profile(p_profile text) returns jsonb
language sql stable security definer set search_path=public as $$
 select p from public.miflix_user_state s,
 lateral jsonb_array_elements(coalesce(s.state->'profiles','[]'::jsonb)) p
 where s.user_id=auth.uid() and s.profile_id='__account__' and p->>'id'=p_profile
 and not coalesce((p->>'isKids')::boolean,false) and p->>'id'<>'kids' limit 1;
$$;
create or replace function public.brunio_rp1_ensure(p_profile text) returns jsonb
language plpgsql security definer set search_path=public as $$
declare p jsonb; actor uuid; main boolean;
begin
 if auth.uid() is null then raise exception 'Sign in first'; end if;
 p:=public.brunio_rp1_profile(p_profile);
 if p is null then raise exception 'Profile is unavailable. Sync your account first.'; end if;
 main:=coalesce((p->>'primary')::boolean,false);
 actor:=case when main then auth.uid() else public.brunio_rp1_id(auth.uid(),p_profile) end;
 insert into public.brunio_rp1_actors(id,account_id,profile_id,primary_profile)
 values(actor,auth.uid(),p_profile,main)
 on conflict(id) do update set profile_id=excluded.profile_id
 where brunio_rp1_actors.account_id=auth.uid() and brunio_rp1_actors.primary_profile=excluded.primary_profile;
 if not found then raise exception 'Profile identity conflict'; end if;
 return jsonb_build_object('actor',actor);
end $$;
create or replace function public.brunio_rp1_actor() returns uuid
language sql stable security definer set search_path=public as $$
 select a.id from public.brunio_rp1_actors a
 where a.account_id=auth.uid()
 and a.profile_id=(nullif(current_setting('request.headers',true),'')::jsonb->>'x-brunio-profile')
 and public.brunio_rp1_profile(a.profile_id) is not null;
$$;
revoke all on function public.brunio_rp1_profile(text),public.brunio_rp1_ensure(text),public.brunio_rp1_actor(),public.brunio_rp1_id(uuid,text) from public,anon;
grant execute on function public.brunio_rp1_ensure(text),public.brunio_rp1_actor() to authenticated;

-- Seed account identities so existing friends, recommendations and rooms belong to Main only.
insert into public.brunio_rp1_actors(id,account_id,profile_id,primary_profile)
 select u.id,u.id,
 (select p->>'id' from public.miflix_user_state s,
 lateral jsonb_array_elements(coalesce(s.state->'profiles','[]'::jsonb)) p
 where s.user_id=u.id and s.profile_id='__account__' and coalesce((p->>'primary')::boolean,false) limit 1),true
 from auth.users u on conflict(id) do nothing;

-- Names are materialized in this transaction, never derived from client input.
create temporary table rp1_names(old text primary key,new text not null) on commit drop;
insert into rp1_names
 select c.relname,'rp1_'||c.relname from pg_class c join pg_namespace n on n.oid=c.relnamespace
 where n.nspname='public' and c.relkind='r' and (
 c.relname in ('miflix_social_profiles','miflix_friendships','miflix_presence','miflix_watch_parties',
 'miflix_calendar_events','miflix_recommendations','brunio_shared_moments','brunio_presence_cursors')
 or c.relname like 'miflix_party_%');

-- Preserve installed schemas, checks, policies and current RPC versions rather than
-- assuming an older release's shape. No legacy identity/auth/user-state tables are cloned.
create temporary table rp1_functions(oid oid primary key) on commit drop;
insert into rp1_functions
 select p.oid from pg_proc p join pg_namespace n on n.oid=p.pronamespace
 where n.nspname='public' and p.prokind='f' and (
 p.proname in ('miflix_friends_with','miflix_in_room','miflix_join_room','miflix_friend_identity_guard',
 'miflix_friend_activity','miflix_friend_activity_v30','miflix_friend_recommendations','miflix_set_presence',
 'miflix_request_access','miflix_access_requests','miflix_decide_access','miflix_request_pause','miflix_take_pause',
 'miflix_request_playback','miflix_take_playback','miflix_invite_party','miflix_calendar_invites_valid',
 'brunio_presence_event','brunio_received_moments') or p.proname like 'miflix_party_%');
-- Include actual trigger functions attached to the cloned tables.
insert into rp1_functions select distinct t.tgfoid from pg_trigger t join pg_class c on c.oid=t.tgrelid
 join rp1_names mapping on mapping.old=c.relname where not t.tgisinternal on conflict do nothing;
insert into rp1_names select distinct p.proname,'rp1_'||p.proname from pg_proc p join rp1_functions f on f.oid=p.oid on conflict do nothing;
-- ON CONFLICT ON CONSTRAINT and row checks must use the copied constraint names.
insert into rp1_names select co.conname,'rp1_'||co.conname from pg_constraint co join pg_class c on c.oid=co.conrelid
 join rp1_names mapping on mapping.old=c.relname where co.contype in ('p','u','c','f','x') on conflict do nothing;

create or replace function pg_temp.rp1_rewrite(source text) returns text language plpgsql as $$
declare result text:=source; m record;
begin
 for m in select * from rp1_names order by length(old) desc loop
  result:=regexp_replace(result,'\m'||m.old||'\M',m.new,'g');
 end loop;
 result:=regexp_replace(result,'auth[.]uid\s*\(\s*\)','public.brunio_rp1_actor()','gi');
 return result;
end $$;

-- Tables and constraints first. LIKE deliberately excludes foreign keys and policies:
-- those are copied below with the new actor reference and scoped identity checks.
do $migration$
declare m record; co record; definition text; fk text;
begin
 for m in select c.relname old,'rp1_'||c.relname new from pg_class c join pg_namespace n on n.oid=c.relnamespace
  join rp1_names names on names.old=c.relname where n.nspname='public' and c.relkind='r' loop
  execute format('create table if not exists public.%I (like public.%I including defaults including generated including identity including storage)',m.new,m.old);
  execute format('alter table public.%I enable row level security',m.new);
  execute format('revoke all on public.%I from public,anon,authenticated',m.new);
 end loop;
 for co in select c.*,r.relname from pg_constraint c join pg_class r on r.oid=c.conrelid
  join pg_namespace n on n.oid=r.relnamespace join rp1_names mapping on mapping.old=r.relname
  where n.nspname='public' and c.contype in ('p','u','c','f','x') order by (c.contype='f') loop
  if not exists(select 1 from pg_constraint where conrelid=to_regclass('public.rp1_'||co.relname) and conname='rp1_'||co.conname) then
   definition:=pg_temp.rp1_rewrite(pg_get_constraintdef(co.oid));
   definition:=replace(definition,'REFERENCES auth.users(', 'REFERENCES public.brunio_rp1_actors(');
   if definition like '%REFERENCES auth.users %' then definition:=replace(definition,'REFERENCES auth.users ', 'REFERENCES public.brunio_rp1_actors '); end if;
   if co.contype='f' and definition not like '%DEFERRABLE%' then definition:=definition||' DEFERRABLE INITIALLY DEFERRED'; end if;
   execute format('alter table public.%I add constraint %I %s','rp1_'||co.relname,'rp1_'||co.conname,definition);
  end if;
 end loop;
end $migration$;

-- Clone functions with deferred body validation because they may refer to one another.
-- Names, RLS and triggers are remapped within this transaction.
set local check_function_bodies=off;
do $migration$
declare f record; definition text;
begin
 for f in select p.*,pg_get_functiondef(p.oid) definition from pg_proc p join rp1_functions r on r.oid=p.oid loop
  definition:=pg_temp.rp1_rewrite(f.definition);
  execute definition;
  execute format('revoke all on function public.%I(%s) from public,anon','rp1_'||f.proname,pg_get_function_identity_arguments(f.oid));
  execute format('grant execute on function public.%I(%s) to authenticated','rp1_'||f.proname,pg_get_function_identity_arguments(f.oid));
 end loop;
end $migration$;
set local check_function_bodies=on;

create table if not exists public.brunio_rp1_imported(table_name text primary key);
alter table public.brunio_rp1_imported enable row level security;
revoke all on public.brunio_rp1_imported from public,anon,authenticated;
-- Copy RLS expressions and triggers, replacing only the scoped identity and object names.
do $migration$
declare m record; policy record; trigger record; role_names text; command text; t text; col record; seq text; max_id bigint; idx record; definition text;
begin
 for m in select c.relname old,'rp1_'||c.relname new from pg_class c join pg_namespace n on n.oid=c.relnamespace
  join rp1_names names on names.old=c.relname where n.nspname='public' and c.relkind='r' loop
  for policy in select * from pg_policies where schemaname='public' and tablename=m.old loop
   execute format('drop policy if exists %I on public.%I',policy.policyname,m.new);
   select string_agg(quote_ident(r),',') into role_names from unnest(policy.roles) r;
   command:=format('create policy %I on public.%I as %s for %s to %s',policy.policyname,m.new,policy.permissive,policy.cmd,role_names);
   if policy.qual is not null then command:=command||' using ('||pg_temp.rp1_rewrite(policy.qual)||')'; end if;
   if policy.with_check is not null then command:=command||' with check ('||pg_temp.rp1_rewrite(policy.with_check)||')'; end if;
   execute command;
  end loop;
  -- Only grant original authenticated table privileges; no new broad grants.
  for t in select privilege_type from information_schema.role_table_grants where table_schema='public' and table_name=m.old and grantee='authenticated' loop
   execute format('grant %s on public.%I to authenticated',t,m.new);
  end loop;
  for trigger in select tgname,pg_get_triggerdef(oid) definition from pg_trigger where tgrelid=to_regclass('public.'||m.old) and not tgisinternal loop
   execute format('drop trigger if exists %I on public.%I',pg_temp.rp1_rewrite(trigger.tgname),m.new);
   execute pg_temp.rp1_rewrite(trigger.definition);
  end loop;
  for idx in select i.relname name,pg_get_indexdef(i.oid) definition from pg_index x join pg_class i on i.oid=x.indexrelid
   where x.indrelid=to_regclass('public.'||m.old) and not exists(select 1 from pg_constraint co where co.conindid=i.oid) loop
   definition:=replace(idx.definition,'INDEX '||quote_ident(idx.name)||' ON','INDEX IF NOT EXISTS '||quote_ident('rp1_'||idx.name)||' ON');
   execute pg_temp.rp1_rewrite(definition);
  end loop;
  -- A completed import marker prevents reruns from resurrecting removed friendships.
  if not exists(select 1 from public.brunio_rp1_imported where table_name=m.old) then
   execute format('insert into public.%I overriding system value select * from public.%I on conflict do nothing',m.new,m.old);
   insert into public.brunio_rp1_imported values(m.old);
  end if;
  for col in select a.attname from pg_attribute a where a.attrelid=to_regclass('public.'||m.new) and a.attnum>0 and not a.attisdropped loop
   seq:=pg_get_serial_sequence('public.'||m.new,col.attname);
   if seq is not null then
    execute format('select max(%I) from public.%I',col.attname,m.new) into max_id;
    perform setval(seq::regclass,greatest(coalesce(max_id,1),1),max_id is not null);
    execute format('grant usage,select on sequence %s to authenticated',seq);
   end if;
  end loop;
 end loop;
end $migration$;

notify pgrst,'reload schema';
commit;
