-- Run once in the Supabase SQL editor. Private ledger; only service_role can reserve.
create table if not exists public.brunio_ai_budget_rp161 (
  account_id uuid not null,
  month text not null,
  reserved_usd numeric(10,6) not null default 0,
  request_count integer not null default 0,
  primary key (account_id, month)
);
alter table public.brunio_ai_budget_rp161 enable row level security;
revoke all on public.brunio_ai_budget_rp161 from anon, authenticated;
create or replace function public.brunio_ai_reserve_rp161(p_account_id uuid, p_month text, p_reserved numeric)
returns boolean language plpgsql security definer set search_path=public as $$
declare new_total numeric;
begin
  if auth.role() <> 'service_role' then raise exception 'service_role required'; end if;
  if p_account_id is null or p_month !~ '^[0-9]{4}-[0-9]{2}$' or p_reserved<=0 or p_reserved>0.03 then return false; end if;
  insert into public.brunio_ai_budget_rp161(account_id,month,reserved_usd,request_count)
    values(p_account_id,p_month,0,0) on conflict do nothing;
  update public.brunio_ai_budget_rp161 set reserved_usd=reserved_usd+p_reserved,
    request_count=request_count+1
    where account_id=p_account_id and month=p_month and reserved_usd+p_reserved<=3.00
    returning reserved_usd into new_total;
  return new_total is not null;
end $$;
revoke all on function public.brunio_ai_reserve_rp161(uuid,text,numeric) from public, anon, authenticated;
grant execute on function public.brunio_ai_reserve_rp161(uuid,text,numeric) to service_role;
