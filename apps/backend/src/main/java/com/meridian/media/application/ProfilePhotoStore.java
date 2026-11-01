package com.meridian.media.application;
import java.util.Optional;
import java.util.UUID;
public interface ProfilePhotoStore extends OwnerPhoto {
 record Photo(byte[] legacy,UUID objectId) {}
 void save(String subject,byte[] bytes);
 void remove(String subject);
 Optional<Photo> visible(String viewer,UUID profileId);
 byte[] read(Photo photo);
}
