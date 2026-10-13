begin;
create table if not exists public.brunio_episode_mail (
  user_id uuid not null references auth.users(id) on delete cascade,
  episode_id bigint not null,
  sent_at timestamptz not null default now(),
  primary key(user_id, episode_id)
);
alter table public.brunio_episode_mail enable row level security;
revoke all on public.brunio_episode_mail from anon, authenticated;
grant select, insert on public.brunio_episode_mail to service_role;
-- No public access to account emails or other users' favorites.
create or replace function public.brunio_episode_recipients()
returns table(user_id uuid, email text, name text, show_id bigint)
language sql stable security definer set search_path = '' as $$
 select distinct u.id, u.email,
 coalesce(nullif(u.raw_user_meta_data->>'full_name',''),nullif(u.raw_user_meta_data->>'name',''),split_part(u.email,'@',1)),
 split_part(f.value,':',3)::bigint
 from public.miflix_user_state s join auth.users u on u.id=s.user_id
 cross join lateral jsonb_array_elements_text(
 case when jsonb_typeof(s.state->'favorites')='array' then s.state->'favorites' else '[]'::jsonb end
 ) f(value)
 where f.value ~ '^tmdb:series:[0-9]{1,10}$'
 and u.email is not null and u.email_confirmed_at is not null
 and u.deleted_at is null
 and (u.banned_until is null or u.banned_until < now())
$$;
revoke all on function public.brunio_episode_recipients() from public, anon, authenticated;
grant execute on function public.brunio_episode_recipients() to service_role;
commit;
