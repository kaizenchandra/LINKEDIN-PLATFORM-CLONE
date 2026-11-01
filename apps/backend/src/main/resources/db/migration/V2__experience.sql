create table experience (
 id uuid primary key,
 subject varchar(255) not null references profile(subject),
 title varchar(120) not null,
 organization varchar(160) not null,
 start_date date not null,
 end_date date,
 description varchar(2600) not null,
 version bigint not null check(version > 0),
 check(end_date is null or end_date >= start_date)
);
create index experience_owner on experience(subject, start_date desc);
