create table search_history_preference (
 subject varchar(255) primary key references profile(subject),
 enabled boolean not null default false,
 version bigint not null default 0 check(version>=0)
);
create table search_history (
 id uuid primary key,
 subject varchar(255) not null references profile(subject),
 kind varchar(12) not null check(kind in ('people','companies','jobs','posts')),
 query varchar(80) not null check(length(query)>=2),
 location varchar(120) not null default '',
 query_key varchar(160) not null,
 location_key varchar(240) not null,
 searched_at timestamptz not null default now(),
 unique(subject,kind,query_key,location_key)
);
create index search_history_owner_time on search_history(subject,searched_at desc,id);
create index search_history_expiry on search_history(searched_at);
