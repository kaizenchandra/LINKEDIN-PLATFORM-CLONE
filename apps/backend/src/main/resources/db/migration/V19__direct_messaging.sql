create table direct_conversation (
 id uuid primary key,
 sequence bigint generated always as identity unique,
 member_a varchar(255) not null references profile(subject),
 member_b varchar(255) not null references profile(subject),
 last_sequence bigint not null default 0 check(last_sequence>=0),
 read_a bigint not null default 0 check(read_a>=0),
 read_b bigint not null default 0 check(read_b>=0),
 created_at timestamptz not null default now(),
 unique(member_a,member_b),
 check(member_a<member_b),
 check(read_a<=last_sequence and read_b<=last_sequence)
);
create index direct_conversation_a on direct_conversation(member_a,sequence desc);
create index direct_conversation_b on direct_conversation(member_b,sequence desc);
create table direct_message (
 id uuid primary key,
 conversation_id uuid not null references direct_conversation(id),
 sequence bigint not null check(sequence>0),
 sender varchar(255) not null references profile(subject),
 body varchar(3000) not null check(length(trim(body))>0),
 created_at timestamptz not null default now(),
 unique(conversation_id,sequence)
);
