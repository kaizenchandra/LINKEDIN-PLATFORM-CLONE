create table dismissed_job (
 subject varchar(255) not null references profile(subject),
 job_id uuid not null references job(id),
 dismissed_at timestamptz not null default now(),
 primary key(subject,job_id)
);
