create table certifications (
 id uuid primary key,
 subject varchar(255) not null references profile(subject),
 name varchar(120) not null,
 issuer varchar(160) not null,
 issue_date date not null,
 expiry_date date,
 credential_url varchar(2048) not null,
 credential_id varchar(160) not null default '',
 version bigint not null check(version > 0),
 check(expiry_date is null or expiry_date >= issue_date)
);
create index certifications_owner on certifications(subject, issue_date desc);
