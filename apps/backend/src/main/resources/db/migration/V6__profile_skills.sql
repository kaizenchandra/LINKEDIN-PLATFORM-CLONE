create table profile_skill (
 id uuid primary key,
 subject varchar(255) not null references profile(subject),
 name varchar(80) not null,
 name_key varchar(160) not null,
 version bigint not null check (version > 0),
 unique(subject, name_key)
);
