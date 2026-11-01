create table member_follow (
 follower varchar(255) not null references profile(subject),
 followed varchar(255) not null references profile(subject),
 sequence bigint generated always as identity unique,
 created_at timestamptz not null default now(),
 primary key(follower,followed),
 check(follower<>followed)
);
create index member_follow_listing on member_follow(follower,sequence desc);
create index member_follow_target on member_follow(followed);
