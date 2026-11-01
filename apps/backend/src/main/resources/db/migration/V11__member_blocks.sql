create table member_block (
 blocker varchar(255) not null references profile(subject),
 blocked varchar(255) not null references profile(subject),
 created_at timestamptz not null default now(),
 primary key(blocker,blocked),
 check(blocker <> blocked)
);
create index member_block_target on member_block(blocked,blocker);
