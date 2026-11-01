package com.meridian;
import com.meridian.peoplesearch.application.*;
import com.meridian.peoplesearch.domain.People.*;
import com.meridian.jobs.application.*;
import com.meridian.jobs.domain.*;
import com.meridian.shared.application.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DiscoveryPreferenceUseCasesTest {
 private final Transactions transactions=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 @Test void peopleQueryIsValidatedBeforeRepositoryAndNormalizedBeforeSearch(){
  var store=mock(PeopleStore.class);var service=new PeopleService(store,transactions);
  assertThrows(UseCaseFailure.class,()->service.search("owner"," a ","",null));verifyNoInteractions(store);
  when(store.search("owner",new Criteria("Engineer","Remote"),null)).thenReturn(List.of());
  assertEquals(new Page(List.of(),null),service.search("owner"," Engineer "," Remote ",null));verify(store).search("owner",new Criteria("Engineer","Remote"),null);
 }
 @Test void discoveryConflictsCannotProduceAConfirmedState(){
  var store=mock(PeopleStore.class);var service=new PeopleService(store,transactions);
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.discovery("owner",true,7)).reason());verify(store,never()).discovery("owner");
 }
 @Test void jobPreferencesAreOptionalAndPreserveNullableWorkplace(){
  var store=mock(JobPreferenceStore.class);var service=new JobPreferenceService(store,transactions);
  assertEquals(new JobPreferences("","",null,0),service.get("owner"));
  var value=new JobPreferences("Engineer","Remote",null,2);when(store.save("owner",value)).thenReturn(true);when(store.get("owner")).thenReturn(Optional.of(new JobPreferences("Engineer","Remote",null,3)));
  assertEquals(3,service.save("owner",new JobPreferences(" Engineer "," Remote ",null,2)).version());verify(store).save("owner",value);
 }
 @Test void jobPreferenceConflictDoesNotReadOrReturnSuccess(){
  var store=mock(JobPreferenceStore.class);var service=new JobPreferenceService(store,transactions);
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.save("owner",new JobPreferences("","",Workplace.REMOTE,3))).reason());verify(store,never()).get(anyString());
 }
}
