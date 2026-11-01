create table community_entry (
 id uuid primary key, owner varchar(255) not null references profile(subject) on delete cascade,
 kind varchar(16) not null check(kind in ('GROUP','EVENT','NEWSLETTER')),
 title varchar(160) not null check(length(trim(title))>0), description varchar(3000) not null,
 starts_at timestamptz, location varchar(240) not null, created_at timestamptz not null default now(),
 check(kind<>'EVENT' or starts_at is not null)
);
create index community_kind_recent on community_entry(kind,created_at desc,id);
create table community_membership (
 entry_id uuid not null references community_entry(id) on delete cascade,
 subject varchar(255) not null references profile(subject) on delete cascade,
 created_at timestamptz not null default now(),primary key(entry_id,subject)
);
create index community_member on community_membership(subject);
