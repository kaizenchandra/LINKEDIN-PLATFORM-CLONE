create table post_draft (
 id uuid primary key,
 author varchar(255) not null references profile(subject),
 body varchar(3000) not null,
 audience varchar(16) not null check(audience in ('ONLY_ME','MEMBERS')),
 version bigint not null default 1 check(version>0),
 published boolean not null default false,
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now()
);
create index post_draft_owner on post_draft(author,id) where not published;
