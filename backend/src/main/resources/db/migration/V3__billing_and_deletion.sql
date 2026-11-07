create table billing_events (
 id text primary key, user_id uuid not null references profiles(id) on delete cascade,
 occurred_at timestamptz not null, created_at timestamptz not null default now()
);
alter table billing_events enable row level security;
create index billing_events_user_idx on billing_events(user_id);
-- Tombstones contain only the removed subject id and prevent stale JWT resurrection.
create table deleted_accounts (user_id uuid primary key, created_at timestamptz not null default now());
alter table deleted_accounts enable row level security;
