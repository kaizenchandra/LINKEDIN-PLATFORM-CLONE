create table account_preferences (
 subject varchar(255) primary key references profile(subject),
 theme varchar(8) not null default 'system' check(theme in ('system','light','dark')),
 version bigint not null default 0 check(version >= 0)
);
