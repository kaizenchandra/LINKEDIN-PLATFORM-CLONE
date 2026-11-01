alter table profile add column discoverable boolean not null default false;
alter table profile add column discovery_version bigint not null default 0 check(discovery_version>=0);
create index profile_discovery_cursor on profile(public_id) where member_visible and discoverable;
