alter table direct_conversation
 add column starred_a boolean not null default false,
 add column starred_b boolean not null default false,
 add column archived_a boolean not null default false,
 add column archived_b boolean not null default false,
 add column preference_version_a bigint not null default 0 check(preference_version_a>=0),
 add column preference_version_b bigint not null default 0 check(preference_version_b>=0);
