alter table notification add column dismissed_at timestamptz;
-- Retain the event identity so redelivery cannot recreate a dismissed notification.
create index notification_active_recipient on notification(recipient,sequence desc) where dismissed_at is null;
