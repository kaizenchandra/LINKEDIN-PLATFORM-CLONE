create table saved_post (
 post_id uuid not null references content_post(id) on delete cascade,
 member varchar(255) not null references profile(subject),
 created_at timestamptz not null default now(),
 primary key(post_id,member)
);
create index saved_post_member on saved_post(member,post_id);
