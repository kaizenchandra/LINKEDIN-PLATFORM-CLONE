create table hidden_post (
 member varchar(255) not null references profile(subject),
 post_id uuid not null references content_post(id),
 created_at timestamptz not null default now(),
 primary key(member,post_id)
);
