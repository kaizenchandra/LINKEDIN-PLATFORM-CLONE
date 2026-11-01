package com.meridian;
import com.meridian.media.application.*;
import com.meridian.shared.application.UseCaseFailure;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProfilePhotoUseCasesTest {
 @Test void inaccessibleProfilesNeverReadStorage(){
  var store=mock(ProfilePhotoStore.class);var service=new ProfilePhotoService(store);var id=UUID.randomUUID();
  when(store.visible(null,id)).thenReturn(Optional.empty());
  when(store.visible("member",id)).thenReturn(Optional.empty());
  assertEquals(UseCaseFailure.Reason.UNAUTHENTICATED,assertThrows(UseCaseFailure.class,()->service.shared(null,id)).reason());
  assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.shared("member",id)).reason());
  verify(store,never()).read(any());
 }
 @Test void visiblePhotoIsReadOnlyAfterAuthorization(){
  var store=mock(ProfilePhotoStore.class);var service=new ProfilePhotoService(store);var id=UUID.randomUUID();
  var photo=new ProfilePhotoStore.Photo(null,UUID.randomUUID());var bytes=new byte[]{1,2};
  when(store.visible("member",id)).thenReturn(Optional.of(photo));when(store.read(photo)).thenReturn(bytes);
  assertArrayEquals(bytes,service.shared("member",id));
  var order=inOrder(store);order.verify(store).visible("member",id);order.verify(store).read(photo);
 }
}
