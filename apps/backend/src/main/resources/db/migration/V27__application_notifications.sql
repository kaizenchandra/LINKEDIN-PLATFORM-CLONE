alter table event_outbox alter column connection_id drop not null;
alter table event_outbox add column application_id uuid;
alter table event_outbox add column application_version bigint;
alter table event_outbox add constraint outbox_application_audit foreign key(application_id,application_version) references application_audit(application_id,version);
alter table event_outbox add constraint outbox_source check(
 (connection_id is not null and application_id is null and application_version is null) or
 (connection_id is null and application_id is not null and application_version is not null));
create unique index outbox_application_version on event_outbox(application_id,application_version) where application_id is not null;
alter table notification alter column connection_id drop not null;
alter table notification add column application_id uuid;
alter table notification add column application_version bigint;
alter table notification add constraint notification_application_audit foreign key(application_id,application_version) references application_audit(application_id,version);
alter table notification add constraint notification_source check(
 (connection_id is not null and application_id is null and application_version is null) or
 (connection_id is null and application_id is not null and application_version is not null));
create unique index notification_application_version on notification(application_id,application_version) where application_id is not null;
alter table notification_preference add column jobs boolean not null default true;
