alter table content_post add column moderated_at timestamptz;
create table moderation_operator (
 subject varchar(255) primary key references profile(subject),
 granted_at timestamptz not null default now()
);
create table content_report (
 id uuid primary key,
 reporter varchar(255) not null references profile(subject),
 post_id uuid not null references content_post(id),
 category varchar(20) not null check(category in ('SPAM','HARASSMENT','MISINFORMATION','OTHER')),
 details varchar(2000) not null,
 state varchar(16) not null default 'OPEN' check(state in ('OPEN','REMOVED','DISMISSED')),
 version bigint not null default 1,
 created_at timestamptz not null default now(),
 unique(reporter,post_id)
);
create index report_queue on content_report(created_at,id) where state='OPEN';
create index report_owner on content_report(reporter,created_at desc);
create table moderation_audit (
 id bigint generated always as identity primary key,
 report_id uuid not null references content_report(id),
 reviewer varchar(255) not null references profile(subject),
 decision varchar(16) not null check(decision in ('REMOVED','DISMISSED')),
 reason varchar(1000) not null check(length(trim(reason))>0),
 created_at timestamptz not null default now(),
 unique(report_id)
);
