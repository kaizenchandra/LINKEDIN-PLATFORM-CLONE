create table account_export (
 id uuid primary key,
 subject varchar(255) not null references profile(subject),
 status varchar(16) not null default 'QUEUED' check(status in ('QUEUED','PROCESSING','READY','FAILED','EXPIRED')),
 created_at timestamptz not null default now(),
 available_at timestamptz not null default now(),
 lease_until timestamptz,
 attempts int not null default 0 check(attempts between 0 and 3),
 expires_at timestamptz,
 payload bytea,
 failure varchar(32) check(failure in ('TOO_LARGE','GENERATION_FAILED')),
 check(payload is null or octet_length(payload)<=10485760),
 check((status='READY' and payload is not null and expires_at is not null) or (status<>'READY' and payload is null))
);
create index account_export_owner on account_export(subject,created_at desc);
create unique index account_export_active on account_export(subject) where status in ('QUEUED','PROCESSING');
create index account_export_work on account_export(available_at,created_at) where status in ('QUEUED','PROCESSING');
create table account_export_audit (
 id bigint generated always as identity primary key,
 export_id uuid not null references account_export(id),
 action varchar(20) not null check(action in ('REQUESTED','DOWNLOADED','CANCELED')),
 created_at timestamptz not null default now()
);
