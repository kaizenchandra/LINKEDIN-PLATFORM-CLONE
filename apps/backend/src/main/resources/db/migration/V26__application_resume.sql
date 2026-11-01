create table application_resume (
 application_id uuid primary key references job_application(id) on delete cascade,
 body text not null check(octet_length(body) between 1 and 20480)
);
