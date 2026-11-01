package com.meridian;
import com.meridian.organizations.application.*;
import com.meridian.organizations.domain.Organizations.*;
import com.meridian.shared.application.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrganizationUseCasesTest {
 private final Transactions transactions=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 @Test void identicalCreationReplaySucceedsWithoutAnUpdate(){
  var store=mock(OrganizationStore.class);var service=new OrganizationService(store,transactions);var id=UUID.randomUUID();
  when(store.initialized("owner")).thenReturn(true);when(store.lockOwner(id)).thenReturn(Optional.of("owner"));
  var existing=new Organization(id,"Company","Industry","City","About",1,true,false,0,true);when(store.get("owner",id)).thenReturn(Optional.of(existing));
  assertEquals(existing,service.save("owner",id,new Write(" Company ","Industry","City","About",0)));verify(store,never()).update(any(),any());
  assertThrows(UseCaseFailure.class,()->service.save("owner",id,new Write("Different","Industry","City","About",0)));
 }
 @Test void foreignCreationConflictAndForeignEditForbiddenRemainDistinct(){
  var store=mock(OrganizationStore.class);var service=new OrganizationService(store,transactions);var id=UUID.randomUUID();when(store.initialized("other")).thenReturn(true);when(store.lockOwner(id)).thenReturn(Optional.of("owner"));
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.save("other",id,new Write("Company","","","",0))).reason());
  assertEquals(UseCaseFailure.Reason.FORBIDDEN,assertThrows(UseCaseFailure.class,()->service.save("other",id,new Write("Company","","","",1))).reason());verify(store,never()).update(any(),any());
 }
 @Test void staleOwnerEditNeverReturnsSuccess(){
  var store=mock(OrganizationStore.class);var service=new OrganizationService(store,transactions);var id=UUID.randomUUID();when(store.initialized("owner")).thenReturn(true);when(store.lockOwner(id)).thenReturn(Optional.of("owner"));
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.save("owner",id,new Write("Company","","","",7))).reason());verify(store,never()).get(anyString(),any());
 }
 @Test void followRequiresInitializedProfileButUnfollowRemainsIdempotent(){
  var store=mock(OrganizationStore.class);var service=new OrganizationService(store,transactions);var id=UUID.randomUUID();
  assertThrows(UseCaseFailure.class,()->service.follow("owner",id));verify(store,never()).follow(anyString(),any());service.unfollow("owner",id);verify(store).unfollow("owner",id);
 }
}
