create table profile_media_object (
 id uuid primary key,
 created_at timestamptz not null default now()
);
alter table profile add column photo_object_id uuid references profile_media_object(id);
create index profile_photo_object on profile(photo_object_id) where photo_object_id is not null;
create index profile_media_gc on profile_media_object(created_at);
