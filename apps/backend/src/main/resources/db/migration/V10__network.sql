create table connection_request (
 id uuid primary key,
 sender varchar(255) not null references profile(subject),
 recipient varchar(255) not null references profile(subject),
 state varchar(16) not null default 'PENDING' check(state in ('PENDING','ACCEPTED','IGNORED','WITHDRAWN','REMOVED')),
 version bigint not null default 0,
 created_at timestamptz not null default now(),
 check(sender <> recipient)
);
create unique index connection_active_pair on connection_request(least(sender,recipient),greatest(sender,recipient)) where state in ('PENDING','ACCEPTED');
create index connection_sender on connection_request(sender,created_at desc);
create index connection_recipient on connection_request(recipient,created_at desc);
