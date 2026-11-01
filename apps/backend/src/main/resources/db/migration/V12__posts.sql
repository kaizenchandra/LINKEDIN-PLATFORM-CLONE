create table content_post (
 id uuid primary key,
 sequence bigint generated always as identity unique,
 author varchar(255) not null references profile(subject),
 body varchar(3000) not null check(length(trim(body)) > 0),
 audience varchar(16) not null check(audience in ('ONLY_ME','MEMBERS')),
 version bigint not null default 1 check(version > 0),
 created_at timestamptz not null default now(),
 edited boolean not null default false,
 deleted_at timestamptz
);
create index content_post_recent on content_post(sequence desc) where deleted_at is null;
create index content_post_author on content_post(author,sequence desc) where deleted_at is null;
