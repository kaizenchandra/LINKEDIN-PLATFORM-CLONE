alter table profile add column connections_visible boolean not null default false;
alter table profile add column connections_version bigint not null default 0;
create index connection_accepted_sender on connection_request(sender,recipient) where state='ACCEPTED';
create index connection_accepted_recipient on connection_request(recipient,sender) where state='ACCEPTED';
create index member_follow_followed_sequence on member_follow(followed,sequence desc);
