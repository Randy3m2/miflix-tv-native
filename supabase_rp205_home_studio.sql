-- BruniO RP205: per-user and per-profile Home order and visibility.
-- Run ONCE in the Supabase SQL Editor. No cron, Realtime subscription or polling.
create table if not exists public.brunio_home_studio_rp205 (
    user_id uuid not null references auth.users(id) on delete cascade,
    profile_id text not null,
    home jsonb not null default '{"order":[],"hidden":[]}'::jsonb,
    updated_at bigint not null default 0,
    primary key (user_id, profile_id),
    constraint brunio_home_object_rp205 check (jsonb_typeof(home)='object'),
    constraint brunio_home_size_rp205 check (octet_length(home::text)<=12000)
);
alter table public.brunio_home_studio_rp205 enable row level security;
revoke all on public.brunio_home_studio_rp205 from anon;
grant select,insert,update on public.brunio_home_studio_rp205 to authenticated;
drop policy if exists rp205_home_select on public.brunio_home_studio_rp205;
create policy rp205_home_select on public.brunio_home_studio_rp205
for select to authenticated using (auth.uid()=user_id);
drop policy if exists rp205_home_insert on public.brunio_home_studio_rp205;
create policy rp205_home_insert on public.brunio_home_studio_rp205
for insert to authenticated with check (auth.uid()=user_id);
drop policy if exists rp205_home_update on public.brunio_home_studio_rp205;
create policy rp205_home_update on public.brunio_home_studio_rp205
for update to authenticated using (auth.uid()=user_id) with check (auth.uid()=user_id);

create or replace function public.brunio_home_sync_rp205(
    p_profile text,
    p_home jsonb default null,
    p_updated_at bigint default 0
) returns jsonb
language plpgsql security invoker set search_path=public as $$
declare result jsonb;
begin
    if auth.uid() is null then raise exception 'Authentication required'; end if;
    if length(p_profile) not between 1 and 160 then raise exception 'Invalid profile'; end if;
    if p_home is not null then
        if jsonb_typeof(p_home)<>'object' or octet_length(p_home::text)>12000
            then raise exception 'Invalid Home configuration'; end if;
        insert into public.brunio_home_studio_rp205(user_id,profile_id,home,updated_at)
        values(auth.uid(),p_profile,p_home,greatest(0,p_updated_at))
        on conflict(user_id,profile_id) do update set home=excluded.home,
            updated_at=excluded.updated_at
        where public.brunio_home_studio_rp205.updated_at<excluded.updated_at;
    end if;
    select jsonb_build_object('home',home,'updatedAt',updated_at) into result
    from public.brunio_home_studio_rp205
    where user_id=auth.uid() and profile_id=p_profile;
    return coalesce(result,jsonb_build_object('home','{"order":[],"hidden":[]}'::jsonb,'updatedAt',0));
end $$;
grant execute on function public.brunio_home_sync_rp205(text,jsonb,bigint) to authenticated;
