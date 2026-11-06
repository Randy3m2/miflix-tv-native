begin;
create table if not exists public.brunio_tirso_schedule_rp174 (
 channel_id text not null default 'tirso-247', schedule_date date not null,
 schedule_version bigint not null default 1, items jsonb not null,
 updated_at timestamptz not null default now(), primary key(channel_id,schedule_date)
);
alter table public.brunio_tirso_schedule_rp174 enable row level security;
drop policy if exists tirso_schedule_read_authenticated on public.brunio_tirso_schedule_rp174;
create policy tirso_schedule_read_authenticated on public.brunio_tirso_schedule_rp174 for select to authenticated using (true);
revoke insert,update,delete on public.brunio_tirso_schedule_rp174 from anon,authenticated;
grant select on public.brunio_tirso_schedule_rp174 to authenticated;
commit;
