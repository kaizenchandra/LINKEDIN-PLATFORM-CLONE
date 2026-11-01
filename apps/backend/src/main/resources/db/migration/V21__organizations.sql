create table organization (
 id uuid primary key,
 owner_subject varchar(255) not null references profile(subject),
 name varchar(160) not null check(length(trim(name))>0),
 industry varchar(120) not null default '',
 location varchar(120) not null default '',
 about varchar(3000) not null default '',
 version bigint not null default 1 check(version>0),
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now()
);
create index organization_owner on organization(owner_subject,id);
create table organization_follow (
 organization_id uuid not null references organization(id),
 subject varchar(255) not null references profile(subject),
 created_at timestamptz not null default now(),
 primary key(organization_id,subject)
);
create index organization_follow_subject on organization_follow(subject,organization_id);
