package com.meridian.media.application;
import java.util.UUID;
public interface ProfilePhotoUseCases extends OwnerPhoto {
 void save(String subject,byte[] bytes);
 void remove(String subject);
 byte[] shared(String viewer,UUID profileId);
}
