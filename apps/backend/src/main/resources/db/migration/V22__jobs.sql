create table job (
 id uuid primary key,
 organization_id uuid not null references organization(id),
 title varchar(160) not null check(length(trim(title))>0),
 location varchar(120) not null check(length(trim(location))>0),
 description varchar(10000) not null check(length(trim(description))>0),
 workplace varchar(10) not null check(workplace in ('REMOTE','HYBRID','ONSITE')),
 status varchar(10) not null check(status in ('OPEN','CLOSED')),
 version bigint not null default 1 check(version>0),
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now()
);
create index job_organization on job(organization_id,id);
create index job_open on job(id) where status='OPEN';
create table saved_job (
 job_id uuid not null references job(id),
 subject varchar(255) not null references profile(subject),
 primary key(job_id,subject)
);
create index saved_job_subject on saved_job(subject,job_id);
