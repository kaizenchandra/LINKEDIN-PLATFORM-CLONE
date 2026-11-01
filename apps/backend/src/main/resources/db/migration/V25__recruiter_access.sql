create table recruiter_access (
 id uuid primary key,
 organization_id uuid not null references organization(id),
 member varchar(255) not null references profile(subject),
 connection_id uuid not null references connection_request(id),
 state varchar(12) not null default 'PENDING' check(state in ('PENDING','ACTIVE','DECLINED','REVOKED','LEFT')),
 version bigint not null default 1 check(version>0)
);
create unique index recruiter_access_current on recruiter_access(organization_id,member) where state in ('PENDING','ACTIVE');
create index recruiter_access_member on recruiter_access(member,id);
create index recruiter_access_company on recruiter_access(organization_id,id);
create table recruiter_access_audit (
 access_id uuid not null references recruiter_access(id),
 version bigint not null,
 actor varchar(255) not null references profile(subject),
 from_state varchar(12),
 to_state varchar(12) not null,
 created_at timestamptz not null default now(),
 primary key(access_id,version)
);
