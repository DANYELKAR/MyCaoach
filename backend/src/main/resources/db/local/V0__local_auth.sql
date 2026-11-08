-- Local-only identity fixture. This location is NEVER loaded by the Supabase profile.
create schema if not exists auth;
create table auth.users (
 id uuid primary key default gen_random_uuid(),
 email text not null unique,
 password_hash text not null,
 created_at timestamptz not null default now()
);
