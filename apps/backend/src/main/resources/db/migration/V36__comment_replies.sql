alter table post_comment add column parent_id uuid;
alter table post_comment add constraint comment_post_identity unique(id,post_id);
alter table post_comment add constraint comment_parent_same_post foreign key(parent_id,post_id) references post_comment(id,post_id);
alter table post_comment add constraint comment_not_own_parent check(parent_id is null or parent_id<>id);
create index post_comment_thread on post_comment(post_id,parent_id,sequence desc) where deleted_at is null;
