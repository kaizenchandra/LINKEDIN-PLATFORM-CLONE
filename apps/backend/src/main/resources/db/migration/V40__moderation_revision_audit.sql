-- Historical decisions predate revision tracking and retain NULL.
alter table moderation_audit add column post_version bigint check(post_version>0);
