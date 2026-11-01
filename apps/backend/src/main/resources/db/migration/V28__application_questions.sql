create table job_question_form (
 job_id uuid primary key references job(id),
 version bigint not null check(version>0),
 questions jsonb not null check(jsonb_typeof(questions)='array' and jsonb_array_length(questions)<=5)
);
alter table job_application add column question_version bigint not null default 0 check(question_version>=0);
alter table job_application add column answers jsonb not null default '[]' check(jsonb_typeof(answers)='array' and jsonb_array_length(answers)<=5);
