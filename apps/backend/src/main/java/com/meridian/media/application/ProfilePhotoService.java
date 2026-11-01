package com.meridian.media.application;
import java.util.UUID;
import com.meridian.shared.application.UseCaseFailure;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
/** Visibility lookup precedes object retrieval, including missing-photo responses. */
public final class ProfilePhotoService implements ProfilePhotoUseCases {
 private final ProfilePhotoStore store;
 public ProfilePhotoService(ProfilePhotoStore store){this.store=store;}
 public void save(String subject,byte[] bytes){store.save(subject,bytes);}
 public void remove(String subject){store.remove(subject);}
 public byte[] owner(String subject){return store.owner(subject);}
 public byte[] shared(String viewer,UUID profileId){
  var photo=store.visible(viewer,profileId).orElseThrow(()->new UseCaseFailure(viewer==null?UNAUTHENTICATED:NOT_FOUND,"Profile unavailable"));
  return store.read(photo);
 }
}
