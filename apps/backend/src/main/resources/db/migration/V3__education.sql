create table education (
 id uuid primary key,
 subject varchar(255) not null references profile(subject),
 degree varchar(120) not null,
 institution varchar(160) not null,
 start_date date not null,
 end_date date,
 description varchar(2600) not null,
 field_of_study varchar(160) not null default '',
 version bigint not null check(version > 0),
 check(end_date is null or end_date >= start_date)
);
create index education_owner on education(subject, start_date desc);
