package com.meridian;
import com.meridian.network.application.*;
import com.meridian.network.domain.Network.*;
import com.meridian.shared.application.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class NetworkUseCasesTest {
 private final Transactions transactions=new Transactions(){
  public <T>T write(Supplier<T> work){return work.get();}
  public <T>T snapshot(Supplier<T> work){return work.get();}
 };
 @Test void invitationReplayStillChecksBlockingAndNeverInsertsAnotherRelationship(){
  var store=mock(NetworkStore.class);var signals=mock(NetworkSignals.class);var service=new NetworkService(store,transactions,signals);
  var id=UUID.randomUUID();var recipient=UUID.randomUUID();when(store.target(recipient)).thenReturn(Optional.of("recipient"));
  when(store.invitationExists(id,"sender","recipient")).thenReturn(true);
  service.invite("sender",id,recipient);verify(store,never()).invite(any(),anyString(),any());
  when(store.blocked("sender","recipient")).thenReturn(true);
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.invite("sender",id,recipient)).reason());
  verifyNoInteractions(signals);
 }
 @Test void senderCannotAcceptOrReplayRecipientsAcceptance(){
  var store=mock(NetworkStore.class);var signals=mock(NetworkSignals.class);var service=new NetworkService(store,transactions,signals);
  var id=UUID.randomUUID();when(store.ownedPair(id,"sender")).thenReturn(Optional.of(new Pair("sender","recipient")));
  assertThrows(UseCaseFailure.class,()->service.transition("sender",id,Action.ACCEPT,0));
  verify(store,never()).transitionCompleted(any(),anyString(),any(),anyLong());verify(store,never()).transition(any(),anyString(),any(),anyLong());verifyNoInteractions(signals);
 }
 @Test void successfulAcceptanceEnqueuesOnceAndRetryDoesNotSignalAgain(){
  var store=mock(NetworkStore.class);var signals=mock(NetworkSignals.class);var service=new NetworkService(store,transactions,signals);
  var id=UUID.randomUUID();when(store.ownedPair(id,"recipient")).thenReturn(Optional.of(new Pair("sender","recipient")));
  when(store.transition(id,"recipient",Action.ACCEPT,0)).thenReturn(true);
  service.transition("recipient",id,Action.ACCEPT,0);
  when(store.transitionCompleted(id,"recipient",Action.ACCEPT,1)).thenReturn(true);
  service.transition("recipient",id,Action.ACCEPT,0);
  verify(store,times(1)).transition(id,"recipient",Action.ACCEPT,0);verify(store,times(1)).enqueueAccepted(id);verify(signals,times(1)).changed(Set.of("sender","recipient"));
 }
 @Test void failedTransitionNeverQueuesOutboxOrRealtime(){
  var store=mock(NetworkStore.class);var signals=mock(NetworkSignals.class);var service=new NetworkService(store,transactions,signals);
  var id=UUID.randomUUID();when(store.ownedPair(id,"recipient")).thenReturn(Optional.of(new Pair("sender","recipient")));
  assertThrows(UseCaseFailure.class,()->service.transition("recipient",id,Action.ACCEPT,0));
  verify(store,never()).enqueueAccepted(any());verifyNoInteractions(signals);
 }
 @Test void unfollowingPrivateTargetRemainsAllowedButNewFollowingDoesNot(){
  var store=mock(NetworkStore.class);var service=new NetworkService(store,transactions,mock(NetworkSignals.class));var id=UUID.randomUUID();
  when(store.target(id)).thenReturn(Optional.of("target"));
  assertEquals(new FollowState(false),service.follow("owner",id,false));
  verify(store).follow("owner","target",false);
  assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.follow("owner",id,true)).reason());
  verify(store,never()).follow("owner","target",true);
 }
 @Test void blockSignalsOnlyAfterSuccessfulMutationAndAbsentUnblockIsIdempotent(){
  var store=mock(NetworkStore.class);var signals=mock(NetworkSignals.class);var service=new NetworkService(store,transactions,signals);var id=UUID.randomUUID();
  when(store.blockableTarget("owner",id)).thenReturn(Optional.of("target"));
  service.block("owner",id);var order=inOrder(store,signals);order.verify(store).lockPair("owner","target");order.verify(store).block("owner","target");order.verify(signals).changed(Set.of("owner","target"));
  clearInvocations(store,signals);when(store.target(id)).thenReturn(Optional.empty());service.unblock("owner",id);verify(store,never()).unblock(anyString(),anyString());verifyNoInteractions(signals);
 }
 @Test void discoveryDistinguishesConsentDenialFromAnEmptyAuthorizedList(){
  var store=mock(NetworkDiscoveryStore.class);var service=new NetworkDiscoveryService(store,transactions);var id=UUID.randomUUID();
  when(store.mutual("owner",id,null)).thenReturn(Optional.empty());
  assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.mutual("owner",id,null)).reason());
  when(store.mutual("owner",id,null)).thenReturn(Optional.of(List.of()));
  assertEquals(new Page(List.of(),null),service.mutual("owner",id,null));
 }
}
