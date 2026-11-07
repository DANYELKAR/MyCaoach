-- Keep non-content generation ledger until account deletion so history deletion cannot refill credits.
alter table generations add column history_deleted_at timestamptz;
alter table generations drop constraint generations_thread_id_fkey;
alter table generations add constraint generations_thread_id_fkey foreign key(thread_id) references threads(id) on delete set null;
