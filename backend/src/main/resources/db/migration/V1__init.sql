-- Application schema. Supabase auth.users is managed externally.
create table profiles (
 id uuid primary key references auth.users(id) on delete cascade, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 display_name text, age_confirmed_at timestamptz, ai_consent_at timestamptz, goals text[] not null default '{}',
 humor_level integer not null default 50 check(humor_level between 0 and 100), default_tone_id uuid,
 plan text not null default 'FREE' check(plan in ('FREE','PLUS')), plan_expires_at timestamptz,
 timezone text not null default 'UTC', store_images boolean not null default false, analytics_opt_in boolean not null default true,
 timezone_changed_at timestamptz, deleting_at timestamptz
);
alter table profiles enable row level security;

create table tones (
 id uuid primary key default gen_random_uuid(), created_at timestamptz not null default now(), updated_at timestamptz not null default now(), user_id uuid references profiles(id) on delete cascade, name text not null, emoji text,
 description text, prompt_fragment text not null, length_level integer not null default 50 check(length_level between 0 and 100),
 boldness_level integer not null default 50 check(boldness_level between 0 and 100), humor_level integer not null default 50 check(humor_level between 0 and 100),
 sort_order integer not null default 0, is_active boolean not null default true
);
alter table tones enable row level security;

create table contacts (
 id uuid primary key default gen_random_uuid(), created_at timestamptz not null default now(), updated_at timestamptz not null default now(), user_id uuid not null references profiles(id) on delete cascade, name text not null, source text check(source in ('HINGE','TINDER','BUMBLE','INSTAGRAM','IN_PERSON','OTHER')),
 status text not null default 'NEW' check(status in ('NEW','TALKING','DATE_PLANNED','ENDED')),
 interests text[] not null default '{}', notes text, photo_path text
);
alter table contacts enable row level security;

create table threads (
 id uuid primary key default gen_random_uuid(), created_at timestamptz not null default now(), updated_at timestamptz not null default now(), user_id uuid not null references profiles(id) on delete cascade, contact_id uuid references contacts(id) on delete set null, title text, last_activity_at timestamptz not null default now()
);
alter table threads enable row level security;

create table generations (
 id uuid primary key default gen_random_uuid(), created_at timestamptz not null default now(), updated_at timestamptz not null default now(), user_id uuid not null references profiles(id) on delete cascade, thread_id uuid references threads(id) on delete cascade,
 contact_id uuid references contacts(id) on delete set null, type text not null check(type in ('OPENER','REPLY')),
 tone_id uuid references tones(id) on delete set null, user_note text, input_text text, input_image_paths text[] not null default '{}',
 status text not null default 'PENDING' check(status in ('PENDING','SUCCEEDED','FAILED','BLOCKED')),
 model text, tokens_in integer, tokens_out integer, idempotency_key text not null, fingerprint text not null,
 reservation_expires_at timestamptz, unique(user_id,idempotency_key)
);
alter table generations enable row level security;

create table suggestions (
 id uuid primary key default gen_random_uuid(), created_at timestamptz not null default now(), updated_at timestamptz not null default now(), user_id uuid not null references profiles(id) on delete cascade, generation_id uuid not null references generations(id) on delete cascade,
 text text not null check(char_length(text) between 1 and 220), rank integer not null check(rank between 1 and 5),
 is_top_pick boolean not null default false, feedback text check(feedback in ('LIKE','DISLIKE')), feedback_reason text,
 sent_status text not null default 'NOT_SENT' check(sent_status in ('NOT_SENT','SENT')),
 outcome text not null default 'NONE' check(outcome in ('NONE','REPLIED','NO_REPLY')), unique(generation_id,rank)
);
alter table suggestions enable row level security;

create table folders (
 id uuid primary key default gen_random_uuid(), created_at timestamptz not null default now(), updated_at timestamptz not null default now(), user_id uuid not null references profiles(id) on delete cascade, name text not null
);
alter table folders enable row level security;

create table saved_items (
 id uuid primary key default gen_random_uuid(), created_at timestamptz not null default now(), updated_at timestamptz not null default now(), user_id uuid not null references profiles(id) on delete cascade, folder_id uuid references folders(id) on delete set null,
 kind text not null check(kind in ('SUGGESTION','TEMPLATE')), suggestion_id uuid references suggestions(id) on delete set null,
 title text, text text not null, tags text[] not null default '{}' 
);
alter table saved_items enable row level security;

create table style_profiles (
 user_id uuid primary key references profiles(id) on delete cascade,
 created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 sample_messages text[] not null default '{}', summary jsonb, enabled boolean not null default true
);
alter table style_profiles enable row level security;

create table reports (
 id uuid primary key default gen_random_uuid(), created_at timestamptz not null default now(), updated_at timestamptz not null default now(), user_id uuid not null references profiles(id) on delete cascade, suggestion_id uuid references suggestions(id) on delete set null, reason text not null, details text
);
alter table reports enable row level security;

alter table profiles add constraint profiles_default_tone_fk foreign key(default_tone_id) references tones(id) on delete set null;
create index contacts_user_created_idx on contacts(user_id,created_at desc);
create index generations_user_created_idx on generations(user_id,created_at desc);
create index suggestions_user_created_idx on suggestions(user_id,created_at desc);
create index threads_user_created_idx on threads(user_id,created_at desc);
create index folders_user_created_idx on folders(user_id,created_at desc);
create index reports_user_created_idx on reports(user_id,created_at desc);
create index threads_user_activity_idx on threads(user_id,last_activity_at desc);
create index threads_contact_idx on threads(contact_id);
create index generations_thread_idx on generations(thread_id);
create index generations_contact_idx on generations(contact_id);
create index generations_tone_idx on generations(tone_id);
create index generations_pending_idx on generations(reservation_expires_at) where status='PENDING';
create index suggestions_generation_idx on suggestions(generation_id);
create index saved_items_user_folder_idx on saved_items(user_id,folder_id);
create index saved_items_suggestion_idx on saved_items(suggestion_id);
create index reports_suggestion_idx on reports(suggestion_id);
create index tones_user_idx on tones(user_id);
create index profiles_default_tone_idx on profiles(default_tone_id);
