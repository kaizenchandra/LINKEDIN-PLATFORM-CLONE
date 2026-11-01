create table post_reaction (
 post_id uuid not null references content_post(id) on delete cascade,
 member varchar(255) not null references profile(subject),
 created_at timestamptz not null default now(),
 primary key(post_id,member)
);
create index post_reaction_member on post_reaction(member,post_id);
