create table event_outbox (
 id uuid primary key,
 connection_id uuid not null references connection_request(id),
 created_at timestamptz not null default now(),
 published_at timestamptz,
 attempts integer not null default 0,
 available_at timestamptz not null default now()
);
create index event_outbox_pending on event_outbox(available_at,created_at) where published_at is null;
create table processed_event(id uuid primary key, processed_at timestamptz not null default now());
create table notification (
 id uuid primary key,
 sequence bigint generated always as identity unique,
 connection_id uuid not null references connection_request(id),
 recipient varchar(255) not null references profile(subject),
 actor varchar(255) not null references profile(subject),
 created_at timestamptz not null default now(),
 read_at timestamptz
);
create index notification_recipient on notification(recipient,sequence desc);
create table notification_preference (
 subject varchar(255) primary key references profile(subject),
 connections boolean not null default true,
 version bigint not null default 1
);
