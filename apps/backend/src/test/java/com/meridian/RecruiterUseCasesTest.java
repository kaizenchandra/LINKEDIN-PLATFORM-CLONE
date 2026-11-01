package com.meridian;
import com.meridian.recruiting.application.*;
import com.meridian.recruiting.domain.RecruiterAccess.*;
import com.meridian.shared.application.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RecruiterUseCasesTest {
 private final RecruiterStore store=mock(RecruiterStore.class);
 private final RecruiterSignals signals=mock(RecruiterSignals.class);
 private final Transactions transactions=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 private final RecruiterService service=new RecruiterService(store,transactions,signals);
 private final UUID company=UUID.randomUUID(),id=UUID.randomUUID(),connection=UUID.randomUUID();
 @Test void unauthorizedOwnerCannotInviteOrListCompanyAccess(){
  when(store.lockOrganization(company)).thenReturn(Optional.of("owner"));
  assertEquals(UseCaseFailure.Reason.FORBIDDEN,assertThrows(UseCaseFailure.class,()->service.invite("outsider",company,id,connection)).reason());
  assertEquals(UseCaseFailure.Reason.FORBIDDEN,assertThrows(UseCaseFailure.class,()->service.list("outsider",company,null)).reason());
  verify(store,never()).insert(any(),any(),anyString(),any());verifyNoInteractions(signals);
 }
 @Test void replayDoesNotInsertAuditOrSignal(){
  when(store.lockOrganization(company)).thenReturn(Optional.of("owner"));when(store.invitation(id)).thenReturn(Optional.of(new Invitation(company,connection)));
  service.invite("owner",company,id,connection);
  verify(store).receipt(id,"owner");verify(store,never()).insert(any(),any(),anyString(),any());verify(store,never()).audit(any(),anyLong(),anyString(),any(),anyString());verifyNoInteractions(signals);
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.invite("owner",company,id,UUID.randomUUID())).reason());
 }
 @Test void blockedConnectionAndCapacityPreventInvitation(){
  when(store.lockOrganization(company)).thenReturn(Optional.of("owner"));when(store.connectedMember(connection,"owner")).thenReturn(Optional.of("member"));
  assertThrows(UseCaseFailure.class,()->service.invite("owner",company,id,connection));
  verify(store).lockPair("owner","member");
  when(store.acceptedUnblocked(connection,"owner","member")).thenReturn(true);when(store.occupied(company)).thenReturn(50);
  assertThrows(UseCaseFailure.class,()->service.invite("owner",company,id,connection));verify(store,never()).insert(any(),any(),anyString(),any());verifyNoInteractions(signals);
 }
 @Test void changeChecksActorVersionAndStateBeforeWriting(){
  when(store.accessibleOrganization(eq(id),anyString())).thenReturn(Optional.of(company));when(store.lockOrganization(company)).thenReturn(Optional.of("owner"));when(store.lockMembership(id)).thenReturn(new Membership("member","PENDING",1));
  assertEquals(UseCaseFailure.Reason.FORBIDDEN,assertThrows(UseCaseFailure.class,()->service.change("owner",id,Action.ACCEPT,1)).reason());
  assertEquals(UseCaseFailure.Reason.FORBIDDEN,assertThrows(UseCaseFailure.class,()->service.change("member",id,Action.REVOKE,1)).reason());
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.change("member",id,Action.ACCEPT,2)).reason());
  assertThrows(UseCaseFailure.class,()->service.change("member",id,Action.LEAVE,1));verify(store,never()).change(any(),anyString());verifyNoInteractions(signals);
  service.change("member",id,Action.ACCEPT,1);
  var ordered=inOrder(store,signals);ordered.verify(store).change(id,"ACTIVE");ordered.verify(store).audit(id,2,"member","PENDING","ACTIVE");ordered.verify(signals).changed(Set.of("owner","member"));ordered.verify(store).receipt(id,"member");
 }
 @Test void terminalStatesCannotBeReactivated(){
  for(String state:List.of("DECLINED","LEFT","REVOKED"))for(Action action:Action.values())assertNull(new Membership("member",state,2).next(action));
 }
}
