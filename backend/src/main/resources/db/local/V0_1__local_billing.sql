create table auth.local_entitlements (
 user_id uuid primary key references auth.users(id) on delete cascade,
 active boolean not null, expires_at timestamptz
);
