package com.meridian;

import com.meridian.account.application.*;
import com.meridian.searchhistory.application.*;
import com.meridian.searchhistory.domain.SearchHistory.*;
import com.meridian.shared.application.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HexagonalUseCasesTest {
 private final Transactions transactions=new Transactions(){
  public <T>T write(Supplier<T> work){return work.get();}
  public <T>T snapshot(Supplier<T> work){return work.get();}
 };
 @Test void staleConsentCannotReachPersistenceMutation(){
  var store=mock(SearchHistoryStore.class);when(store.lockOwner("owner")).thenReturn(true);
  when(store.state("owner")).thenReturn(new State(true,4,List.of()));
  var service=new SearchHistoryService(store,transactions);
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.remember("owner",Kind.people,"Java","",3)).reason());
  verify(store,never()).remember(anyString(),any());verify(store,never()).purgeOwnerExpired(anyString());
 }
 @Test void disablingPurgesBeforeReturningConfirmedState(){
  var store=mock(SearchHistoryStore.class);when(store.lockOwner("owner")).thenReturn(true);
  when(store.state("owner")).thenReturn(new State(true,1,List.of()),new State(false,2,List.of()));
  var service=new SearchHistoryService(store,transactions);assertFalse(service.preference("owner",false,1).enabled());
  var order=inOrder(store);order.verify(store).preference("owner",false);order.verify(store).clear("owner");order.verify(store).state("owner");
 }
 @Test void criteriaNormalizationIsIndependentOfFrameworkAndLocale(){
  var criteria=new Criteria(Kind.people,"  JAVA   engineer "," New   York ");
  assertEquals("JAVA engineer",criteria.query());assertEquals("java engineer",criteria.queryKey());assertEquals("New York",criteria.location());
  assertEquals("",new Criteria(Kind.posts,"Java","Unused").location());
  assertThrows(IllegalArgumentException.class,()->new Criteria(Kind.people,"  ",""));
 }
 @Test void unavailableProfilePreservesGuestAndMemberOutcomes(){
  var store=mock(AccountSettingsStore.class);var id=UUID.randomUUID();
  when(store.visibleProfile(eq(id),any())).thenReturn(Optional.empty());
  var service=new AccountSettingsService(store,transactions);
  assertEquals(UseCaseFailure.Reason.UNAUTHENTICATED,assertThrows(UseCaseFailure.class,()->service.view(id,null)).reason());
  assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.view(id,"member")).reason());
 }
}
