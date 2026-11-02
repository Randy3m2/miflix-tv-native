begin;
create table if not exists public.brunio_calendar_feeds (
 user_id uuid not null references auth.users(id) on delete cascade,
 profile_id text not null check(length(profile_id) between 1 and 100),
 token_hash text not null unique check(length(token_hash)=64),
 publish_hash text not null unique check(length(publish_hash)=64),
 ics text not null check(octet_length(ics)<=400000),
 updated_at timestamptz not null default now(),
 primary key(user_id,profile_id)
);
alter table public.brunio_calendar_feeds enable row level security;
revoke all on public.brunio_calendar_feeds from anon,authenticated;
grant all on public.brunio_calendar_feeds to service_role;
commit;
