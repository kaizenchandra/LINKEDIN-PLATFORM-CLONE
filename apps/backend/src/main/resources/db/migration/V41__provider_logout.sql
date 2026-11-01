-- Independent of spring_session: OAuth2 login registers before its HTTP session commits.
create table oidc_session_binding (
 session_id varchar(128) primary key,
 issuer text not null,
 client_id text not null,
 subject text not null,
 provider_sid text,
 token_issued_at timestamptz not null,
 created_at timestamptz not null default now()
);
create index oidc_binding_provider on oidc_session_binding(issuer,client_id,provider_sid);
create index oidc_binding_subject on oidc_session_binding(issuer,client_id,subject);
create table provider_logout_event (
 issuer text not null,
 client_id text not null,
 token_id text not null,
 subject text,
 provider_sid text,
 issued_at timestamptz not null,
 received_at timestamptz not null default now(),
 revoked_count integer not null default 0,
 primary key(issuer,client_id,token_id),
 check(subject is not null or provider_sid is not null)
);
create index provider_logout_sid on provider_logout_event(issuer,client_id,provider_sid);
create index provider_logout_subject on provider_logout_event(issuer,client_id,subject,issued_at);
