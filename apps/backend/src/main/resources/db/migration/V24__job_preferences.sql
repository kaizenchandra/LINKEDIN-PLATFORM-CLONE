create table job_preference (
 subject varchar(255) primary key references profile(subject),
 query varchar(80) not null default '',
 location varchar(120) not null default '',
 workplace varchar(10) check(workplace in ('REMOTE','HYBRID','ONSITE')),
 version bigint not null default 1 check(version>0)
);
