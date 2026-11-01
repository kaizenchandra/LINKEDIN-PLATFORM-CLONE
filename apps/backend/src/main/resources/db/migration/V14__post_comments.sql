create table post_comment (
 id uuid primary key,
 sequence bigint generated always as identity unique,
 post_id uuid not null references content_post(id) on delete cascade,
 author varchar(255) not null references profile(subject),
 body varchar(2000) not null check(length(trim(body)) > 0),
 created_at timestamptz not null default now(),
 deleted_at timestamptz
);
create index post_comment_recent on post_comment(post_id,sequence desc) where deleted_at is null;
