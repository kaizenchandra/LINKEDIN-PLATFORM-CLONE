alter table profile add column photo bytea;
alter table profile add constraint profile_photo_size check (octet_length(photo) <= 2097152);
