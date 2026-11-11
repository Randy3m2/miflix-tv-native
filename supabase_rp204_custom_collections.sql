-- RP204 BruniO: private per-account/per-profile custom collections.
-- Run once in Supabase SQL Editor before enabling cross-device sync.
-- Stored as compact JSON; no images or streaming data are uploaded.
create table if not exists public.brunio_custom_collections_rp204 (
    user_id uuid not null references auth.users(id) on delete cascade,
    profile_id text not null,
    collections jsonb not null default '[]'::jsonb,
    updated_at bigint not null default 0,
    primary key (user_id, profile_id),
    constraint brunio_collections_array_rp204 check (jsonb_typeof(collections) = 'array'),
    constraint brunio_collections_limit_rp204 check (jsonb_array_length(collections) <= 100)
);
alter table public.brunio_custom_collections_rp204 enable row level security;
revoke all on public.brunio_custom_collections_rp204 from anon;
grant select, insert, update on public.brunio_custom_collections_rp204 to authenticated;
drop policy if exists "rp204_owner_select" on public.brunio_custom_collections_rp204;
create policy "rp204_owner_select" on public.brunio_custom_collections_rp204
    for select to authenticated using (auth.uid() = user_id);
drop policy if exists "rp204_owner_insert" on public.brunio_custom_collections_rp204;
create policy "rp204_owner_insert" on public.brunio_custom_collections_rp204
    for insert to authenticated with check (auth.uid() = user_id);
drop policy if exists "rp204_owner_update" on public.brunio_custom_collections_rp204;
create policy "rp204_owner_update" on public.brunio_custom_collections_rp204
    for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- Single atomic exchange. Server retains the newer timestamp; other devices
-- never overwrite a more recent revision. Runs with caller's RLS permissions.
create or replace function public.brunio_collections_sync_rp204(
    p_profile text,
    p_collections jsonb default null,
    p_updated_at bigint default 0
) returns jsonb language plpgsql security invoker set search_path=public as $$
declare
    result jsonb;
begin
    if auth.uid() is null then
        raise exception 'Authentication required' using errcode='28000';
    end if;
    if length(p_profile) < 1 or length(p_profile) > 160 then
        raise exception 'Invalid profile';
    end if;
    if p_collections is not null then
        if jsonb_typeof(p_collections) <> 'array' or jsonb_array_length(p_collections) > 100 then
            raise exception 'Invalid collections';
        end if;
        insert into public.brunio_custom_collections_rp204 (user_id,profile_id,collections,updated_at)
        values (auth.uid(),p_profile,p_collections,greatest(0,p_updated_at))
        on conflict(user_id,profile_id) do update
          set collections=excluded.collections,updated_at=excluded.updated_at
          where public.brunio_custom_collections_rp204.updated_at < excluded.updated_at;
    end if;
    select jsonb_build_object('collections',collections,'updatedAt',updated_at)
      into result from public.brunio_custom_collections_rp204
      where user_id=auth.uid() and profile_id=p_profile;
    return coalesce(result,jsonb_build_object('collections','[]'::jsonb,'updatedAt',0));
end $$;
grant execute on function public.brunio_collections_sync_rp204(text,jsonb,bigint) to authenticated;
