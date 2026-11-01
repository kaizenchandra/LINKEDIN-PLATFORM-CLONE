create table job_application (
 id uuid primary key,
 job_id uuid not null references job(id),
 applicant varchar(255) not null references profile(subject),
 applicant_name text not null,
 applicant_headline text not null,
 statement varchar(4000) not null check(length(trim(statement))>0),
 status varchar(16) not null default 'SUBMITTED' check(status in ('SUBMITTED','REVIEWING','ACCEPTED','REJECTED','WITHDRAWN')),
 version bigint not null default 1 check(version>0),
 created_at timestamptz not null default now(),
 unique(job_id,applicant)
);
create index job_application_applicant on job_application(applicant,id);
create index job_application_job on job_application(job_id,id);
create table application_audit (
 application_id uuid not null references job_application(id),
 version bigint not null,
 actor varchar(255) not null references profile(subject),
 from_status varchar(16),
 to_status varchar(16) not null,
 created_at timestamptz not null default now(),
 primary key(application_id,version)
);
