create table profile_language (
 id uuid primary key,
 subject varchar(255) not null references profile(subject),
 name varchar(80) not null,
 name_key varchar(160) not null,
 proficiency varchar(32) not null check(proficiency in ('ELEMENTARY','LIMITED_WORKING','PROFESSIONAL_WORKING','FULL_PROFESSIONAL','NATIVE_BILINGUAL')),
 version bigint not null check (version > 0),
 unique(subject, name_key)
);
