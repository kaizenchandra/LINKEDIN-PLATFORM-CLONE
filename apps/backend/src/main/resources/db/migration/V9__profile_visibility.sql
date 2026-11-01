alter table profile add column public_id uuid not null default gen_random_uuid();
alter table profile add constraint profile_public_id_unique unique(public_id);
alter table profile add column member_visible boolean not null default false;
alter table profile add column privacy_version bigint not null default 0 check(privacy_version >= 0);
