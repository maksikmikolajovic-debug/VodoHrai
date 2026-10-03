-- VodoHrai V19 compatibility + cloud state
-- Виконати після вже створених таблиць.

alter table public.profiles add column if not exists name text;
update public.profiles set name = coalesce(name, full_name) where name is null;

create table if not exists public.orders (
  id uuid primary key default gen_random_uuid(),
  car text default '', plate text default '', category text default '',
  worker_names jsonb default '[]'::jsonb,
  worker_share numeric(12,2) default 0,
  total_amount numeric(12,2) default 0,
  services jsonb default '[]'::jsonb,
  status text default 'wait',
  cash_added boolean default false,
  note text default '',
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz default now(), completed_at timestamptz
);

create table if not exists public.carpets (
  id uuid primary key default gen_random_uuid(),
  number text default '', owner text default '', type_id text default '', type_name text default '',
  length numeric(10,2) default 0, width numeric(10,2) default 0, area numeric(12,2) default 0,
  price_per_m2 numeric(12,2) default 0, total_amount numeric(12,2) default 0,
  worker_names jsonb default '[]'::jsonb, worker_share numeric(12,2) default 0,
  admin_name text default '', status text default 'accepted', accepted_at timestamptz,
  due_min timestamptz, due_max timestamptz, note text default '', created_by uuid references auth.users(id) on delete set null,
  cash_added boolean default false, ready_at timestamptz, issued_at timestamptz, recheck_at timestamptz, created_at timestamptz default now()
);

create table if not exists public.app_state (
  id uuid primary key default gen_random_uuid(),
  org_id text unique not null,
  state jsonb not null default '{}'::jsonb,
  updated_by uuid references auth.users(id) on delete set null,
  updated_at timestamptz default now()
);

alter table public.orders enable row level security;
alter table public.carpets enable row level security;
alter table public.app_state enable row level security;

-- Prototype access: authenticated users can work with the shared app data.
-- Role-specific restrictions will be tightened in the production pass.
drop policy if exists orders_authenticated_all on public.orders;
create policy orders_authenticated_all on public.orders for all to authenticated using (true) with check (true);

drop policy if exists carpets_authenticated_all on public.carpets;
create policy carpets_authenticated_all on public.carpets for all to authenticated using (true) with check (true);

drop policy if exists app_state_authenticated_all on public.app_state;
create policy app_state_authenticated_all on public.app_state for all to authenticated using (true) with check (true);

drop policy if exists profiles_authenticated_all on public.profiles;
create policy profiles_authenticated_all on public.profiles for all to authenticated using (true) with check (true);

-- Existing tables created earlier: allow authenticated app access during integration testing.
do $$
declare t text;
begin
  foreach t in array array['workers','admins','car_services','car_orders','car_order_services','car_order_workers','carpet_orders','carpet_order_workers','carpet_rechecks','car_cash','carpet_cash','worker_attendance','admin_attendance','app_updates'] loop
    execute format('drop policy if exists %I on public.%I', t || '_authenticated_all', t);
    execute format('create policy %I on public.%I for all to authenticated using (true) with check (true)', t || '_authenticated_all', t);
  end loop;
end $$;

-- Initial empty shared state. Does not overwrite existing state.
insert into public.app_state(org_id, state)
values ('vodohrai-main', '{"orders":[],"carpets":[],"cash":[],"cashCarpets":[],"users":[],"shiftClosures":[],"shiftNotes":[],"attendance":[],"adminAttendance":[],"schedule":[],"notifications":[]}'::jsonb)
on conflict (org_id) do nothing;
